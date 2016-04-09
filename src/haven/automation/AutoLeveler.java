package haven.automation;

import haven.*;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.*;


public class AutoLeveler extends Window implements GobSelectCallback, ErrorSysMsgCallback {
    private static final Text.Foundry infof = new Text.Foundry(Text.sans, 10).aa(true);
    private static final Text.Foundry countf = new Text.Foundry(Text.sansb, 12).aa(true);
    private List<Gob> stockpiles = new ArrayList<>();
    private final Label lbls;
    public boolean terminate = false;
    private Button clearbtn, runbtn, stopbtn;
    private static final int TIMEOUT = 6000;
    private static final int DIG_DELAY = 4;
    private static final int HAND_DELAY = 10;
    private static final int INACTIVTY_TIMEOUT = 5 * 60 * 1000;
    private Thread runner;
    private Gob survey;
    public boolean running;
    private String lasteerrmsg;
    private Set<Gob> fullstockpiles = new HashSet<Gob>();

    public AutoLeveler() {
        super(new Coord(270, 180), "Auto Leveler");

        Widget inf = add(new Widget(new Coord(245, 55)) {
            public void draw(GOut g) {
                g.chcolor(0, 0, 0, 128);
                g.frect(Coord.z, sz);
                g.chcolor();
                super.draw(g);
            }

        }, new Coord(10, 10).add(wbox.btloff()));
        Frame.around(this, Collections.singletonList(inf));
        inf.add(new RichTextBox(new Coord(245, 55),
                Resource.getLocString(Resource.l10nLabel, "Alt + Click to select soil stockpiles for storing or taking the soil from, or leave empty to use all stockpiles in the area.\n\n" +
                        "Nearest survey flag will be used for leveling.\n\n" +
                        "Put flasks/waterskins in inventory and hotkey one of them for auto-drinking\n"), CharWnd.ifnd));

        Label lblstxt = new Label("Stockpiles Selected:", infof);
        add(lblstxt, new Coord(15, 90));
        lbls = new Label("0", countf, true);
        add(lbls, new Coord(120, 88));

        clearbtn = new Button(140, "Clear Selection") {
            @Override
            public void click() {
                stockpiles.clear();
                fullstockpiles.clear();
                lbls.settext(stockpiles.size() + "");
            }
        };
        add(clearbtn, new Coord(65, 115));

        runbtn = new Button(140, "Run") {
            @Override
            public void click() {
                if (stockpiles.size() == 0) {

                    OCache oc = ui.sess.glob.oc;
                    synchronized (oc) {
                        for (Gob gob : oc) {
                            try {
                                Resource res = gob.getres();
                                if (res != null && res.name.contains("gfx/terobjs/stockpile-soil")) {
                                    stockpiles.add(gob);
                                }
                            } catch (Loading l) {
                            }
                        }
                    }

                    if (stockpiles.size() == 0) {
                        gameui().error("No stockpiles selected or found.");
                        return;
                    } else {
                        lbls.settext(stockpiles.size() + "");
                    }
                }

                this.hide();
                cbtn.hide();
                clearbtn.hide();
                stopbtn.show();
                terminate = false;

                runner = new Thread(new Runner(), "Auto Leveler");
                runner.start();
            }
        };
        add(runbtn, new Coord(65, 150));

        stopbtn = new Button(140, "Stop") {
            @Override
            public void click() {
                running = false;
                terminate = true;
                if (gameui().map.pfthread != null)
                    gameui().map.pfthread.interrupt();
                if (runner != null)
                    runner.interrupt();
                try {
                    if (running)
                        gameui().map.wdgmsg("click", Coord.z, gameui().map.player().rc, 1, 0);
                } catch (Exception e) { // ignored
                }
                this.hide();
                runbtn.show();
                clearbtn.show();
                cbtn.show();
                stockpiles.clear();
                fullstockpiles.clear();
                lbls.settext(stockpiles.size() + "");
            }
        };
        stopbtn.hide();
        add(stopbtn, new Coord(65, 150));
    }

    @Override
    public void notifyErrMsg(String msg) {
        this.lasteerrmsg = msg;
    }

    private class Runner implements Runnable {
        @Override
        public void run() {
            GameUI gui = gameui();
            OCache oc = ui.sess.glob.oc;
            running = true;
            boolean needsoil = false;

            lvl:
            while (!terminate) {
                gui.maininv.drinkTillFull(90);

                try {
                    if (survey == null) {
                        // find closest survey flag
                        double closest = Double.MAX_VALUE;

                        synchronized (oc) {
                            for (Gob gob : oc) {
                                try {
                                    Resource res = gob.getres();
                                    if (res != null && res.name.contains("gfx/terobjs/survobj")) {
                                        double dist = gui.map.player().rc.dist(gob.rc);
                                        if (dist < closest) {
                                            closest = dist;
                                            survey = gob;
                                        }
                                    }
                                } catch (Loading l) {
                                }
                            }
                        }
                    }

                    if (survey == null) {
                        gameui().error("No survey flag found.");
                        return;
                    }

                    // nagivate to the flag
                    gui.map.pfRightClick(survey, -1, 3, 1, null);
                    try {
                        gui.map.pfthread.join();
                    } catch (InterruptedException e) {
                        return;
                    }

                    if (terminate)
                        return;

                    // invoke "make level" button
                    Window survwnd = gui.waitfForWnd("Land survey", 60 * 1000);
                    if (survwnd == null)
                        continue;

                    Field tzf = survwnd.getClass().getDeclaredField("tz");   //throw NPE if user clicks midst wlaking
                    tzf.setAccessible(true);
                    Integer tz = (Integer) tzf.get(survwnd);

                    survwnd.wdgmsg("lvl", new Object[]{tz});

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        return;
                    }

                    // close survey window
                    //try {
                    //    survwnd.cbtn.click();
                    //} catch (UI.UIException uie) { // ignored
                    //} // no need to close the window


                    int timeout = 0;
                    while (!terminate) {
                        timeout = 0;
                        // break if not enough soil
                        if ("You need soil to fill this up.".equals(lasteerrmsg)) {
                            needsoil = true;
                            lasteerrmsg = null;
                            break;
                        }

                        // drop tubbers
                        List<WItem> tubers = gui.maininv.getItemsPartial("Odd Tuber");
                        for (WItem tuber : tubers)
                            tuber.item.wdgmsg("drop", Coord.z);


                        IMeter.Meter stam = gameui().getmeter("stam", 0);
                        if (stam != null && stam.a < 30)
                            continue lvl;

                        timeout += DIG_DELAY;
                        if (timeout > INACTIVTY_TIMEOUT) {
                            terminate();
                            return;
                        }

                        try {
                            Thread.sleep(2 * DIG_DELAY);
                        } catch (InterruptedException e) {
                            return;
                        }
                        if (gui.maininv.getFreeSpace() < 5 && needsoil == false) {
                            if ("You need soil to fill this up.".equals(lasteerrmsg)) {
                                needsoil = true;
                            }

                            break;
                        }


                    }
                    //there was a issue here with needsoil = null when it shouldn't be
                    //as a result once you use the survey to put soil down the script 
                    //must be restarted to use other functionality.
                    if (needsoil)
                        getsoil();
                    if (!needsoil)
                        storesoil();
                } catch (Exception e) {
                    if (terminate)
                        return;
                    continue lvl;
                }
            }
        }
    }

    private void storesoil() {
        GameUI gui = gameui();
        Glob glob = gui.map.glob;
        stockpiles:
        for (Gob s : stockpiles) {
            if (terminate)
                return;

            if (fullstockpiles.contains(s))
                continue;

            // drop any soil we got on hands
            if (!gui.hand.isEmpty()) {
                gui.map.wdgmsg("drop", Coord.z, gui.map.player().rc, 0);
                int timeout = 0;
                while (gui.hand.isEmpty()) {
                    timeout += HAND_DELAY;
                    if (timeout >= TIMEOUT)
                        return;
                    try {
                        Thread.sleep(HAND_DELAY);
                    } catch (InterruptedException e) {
                        terminate = true;
                        return;
                    }
                }
            }

            WItem soil = gui.maininv.getItemPartial("Soil");
            if (soil == null)
                soil = gui.maininv.getItemPartial("Earthworm");
            if (soil == null)
                return;

            // make sure stockpile still exists
            synchronized (glob.oc) {
                if (glob.oc.getgob(s.id) == null)
                    continue;
            }

            // navigate to the stockpile
            gui.map.pfRightClick(s, -1, 3, 0, null);
            try {
                gui.map.pfthread.join();
            } catch (InterruptedException e) {
                return;
            }

            // check whtether stockpile is full
            Window spwnd = gui.waitfForWnd("Stockpile", 1500);
            if (spwnd == null)
                continue;

            boolean nospace = true;
            for (Widget w = spwnd.lchild; w != null; w = w.prev) {
                if (w instanceof ISBox) {
                    ISBox isb = (ISBox) w;
                    int freespace = isb.getfreespace();
                    if (freespace > 0) {
                        // we use boolean instead incase the ISBox didn't load yet
                        nospace = false;
                        break;
                    } else {
                        fullstockpiles.add(s);
                        break;
                    }
                }
            }

            if (nospace)
                continue stockpiles;

            if (terminate)
                return;

            // take one soil piece
            soil.item.wdgmsg("take", new Coord(soil.item.sz.x / 2, soil.item.sz.y / 2));

            int timeout = 0;
            while (gui.hand.isEmpty()) {
                timeout += HAND_DELAY;
                if (timeout >= TIMEOUT)
                    return;
                try {
                    Thread.sleep(HAND_DELAY);
                } catch (InterruptedException e) {
                    terminate = true;
                    return;
                }
            }

            // put into stockpile
            gui.map.wdgmsg("itemact", Coord.z, s.rc, 1, 0, (int) s.id, s.rc, 0, -1);
            timeout = 0;
            while (!gui.hand.isEmpty()) {
                timeout += HAND_DELAY;
                if (timeout >= TIMEOUT)
                    return;
                try {
                    Thread.sleep(HAND_DELAY);
                } catch (InterruptedException e) {
                    terminate = true;
                    return;
                }
            }

            List<WItem> soilinv = gui.maininv.getItemsPartial("Soil", "Earthworm");
            if (soilinv.size() == 0)
                return;
        }

        List<WItem> soilinv = gui.maininv.getItemsPartial("Soil", "Earthworm");
        if (soilinv.size() > 0) {
            gameui().error("Nowhere to place soil. All stockpiles are full. ");
            terminate();
        }
    }

    private void getsoil() {
        GameUI gui = gameui();
        Glob glob = gui.map.glob;
        for (Gob s : stockpiles) {
            if (terminate)
                return;

            // make sure stockpile still exists
            synchronized (glob.oc) {
                if (glob.oc.getgob(s.id) == null)
                    continue;
            }

            // navigate to the stockpile
            gameui().map.pfRightClick(s, -1, 3, 1, null);
            try {
                gui.map.pfthread.join();
            } catch (InterruptedException e) {
                return;
            }

            if (fullstockpiles.contains(s))
                fullstockpiles.remove(s);

            if (gui.maininv.getFreeSpace() < 2)
                break;
        }
    }

    public void gobselect(Gob gob) {
        Resource res = gob.getres();
        if (res != null) {
            if (res.name.equals("gfx/terobjs/stockpile-soil")) {
                if (!stockpiles.contains(gob)) {
                    stockpiles.add(gob);
                    lbls.settext(stockpiles.size() + "");
                }
            }
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == cbtn)
            reqdestroy();
        else
            super.wdgmsg(sender, msg, args);
    }

    @Override
    public boolean type(char key, KeyEvent ev) {
        if (key == 27) {
            if (cbtn.visible)
                reqdestroy();
            return true;
        }
        return super.type(key, ev);
    }

    public void terminate() {
        terminate = true;
        if (runner != null)
            runner.interrupt();
        try {
            if (running)
                gameui().map.wdgmsg("click", Coord.z, gameui().map.player().rc, 1, 0);
        } catch (Exception e) { // ignored
        }
        if (gameui().map.pfthread != null) {
            gameui().map.pfthread.interrupt();
        }
        this.destroy();
    }
}
