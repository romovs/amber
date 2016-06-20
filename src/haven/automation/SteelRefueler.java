package haven.automation;

import haven.*;
import haven.Button;
import haven.Label;
import haven.Window;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;


public class SteelRefueler extends Window implements GobSelectCallback {
    private static final Text.Foundry infof = new Text.Foundry(Text.sans, Config.fontsizeglobal).aa(true);
    private static final Text.Foundry countf = new Text.Foundry(Text.sans.deriveFont(Font.BOLD), Config.fontsizeglobal * 12 / 10).aa(true);
    private List<Gob> crucibles = new ArrayList<>();
    private List<Gob> stockpiles = new ArrayList<>();
    private final Label lblc, lbls;
    public boolean terminate = false;
    private Button clearbtn, runbtn, stopbtn;
    private static final int TIMEOUT = 2000;
    private static final int HAND_DELAY = 8;
    private static final int SLEEP = 30 * 60 * 1000; // 30 min
    private Thread runner;

    public SteelRefueler() {
        super(new Coord(270, 180), "Steel Refueler");

        final Label lbl = new Label("Alt + Click to select crucibles and stockpiles.", infof);
        add(lbl, new Coord(30, 20));

        Label lblctxt = new Label("Crucibles Selected:", infof);
        add(lblctxt, new Coord(15, 60));
        lblc = new Label("0", countf, true);
        add(lblc, new Coord(110, 58));

        Label lblstxt = new Label("Stockpiles Selected:", infof);
        add(lblstxt, new Coord(135, 60));
        lbls = new Label("0", countf, true);
        add(lbls, new Coord(235, 58));

        clearbtn = new Button(140, "Clear Selection") {
            @Override
            public void click() {
                crucibles.clear();
                stockpiles.clear();
                lblc.settext(crucibles.size() + "");
                lbls.settext(stockpiles.size() + "");
            }
        };
        add(clearbtn, new Coord(65, 90));

        runbtn = new Button(140, "Run") {
            @Override
            public void click() {
                if (crucibles.size() == 0) {
                    gameui().error("No crucibles selected.");
                    return;
                } else if (stockpiles.size() == 0) {
                    gameui().error("No stockpiles selected.");
                    return;
                }

                this.hide();
                cbtn.hide();
                clearbtn.hide();
                stopbtn.show();
                terminate = false;

                runner = new Thread(new Runner(), "Steel Refueler");
                runner.start();
            }
        };
        add(runbtn, new Coord(65, 140));

        stopbtn = new Button(140, "Stop") {
            @Override
            public void click() {
                terminate = true;
                // TODO: terminate PF
                this.hide();
                runbtn.show();
                clearbtn.show();
                cbtn.show();
            }
        };
        stopbtn.hide();
        add(stopbtn, new Coord(65, 140));
    }

    private class Runner implements Runnable {
        @Override
        public void run() {
            GameUI gui = gameui();
            while (!terminate) {
                cloop:
                for (Gob c : crucibles) {
                    // take fuel from stockpiles if we don't have enough in the inventory
                    int availableFuelCoal = gui.maininv.getItemPartialCount("Coal");
                    int availableFuelBlock = gui.maininv.getItemPartialCount("Block");
                    int availableFuelBranch = gui.maininv.getItemPartialCount("Branch");
                    if (availableFuelCoal < 9 && availableFuelBlock < 3 && availableFuelBranch < 18)
                        getfuel();

                    // find one piece of fuel in the inventory
                    WItem fuel = gui.maininv.getItemPartial("Coal");
                    if (fuel == null)
                        fuel = gui.maininv.getItemPartial("Block");
                    if (fuel == null)
                        fuel = gui.maininv.getItemPartial("Branch");
                    if (fuel == null)
                        continue;

                    int fuelticks;
                    if (fuel.item.getname().contains("Block"))
                        fuelticks = 27;
                    else if (fuel.item.getname().contains("Coal"))
                        fuelticks = 11;
                    else
                        fuelticks = 5; // branch

                    // navigate to crucible
                    gui.map.pfRightClick(c, -1, 3, 1, null);
                    try {
                        gui.map.pfthread.join();
                    } catch (InterruptedException e) {
                        return;
                    }

                    if (terminate)
                        return;

                    // get crucible fuel status
                    // wait for the window. really ugly but oh well...
                    try {
                        Thread.sleep(TIMEOUT);
                    } catch (InterruptedException e) {
                        return;
                    }
                    Window cwnd = gui.getwnd("Steelbox");
                    if (cwnd == null)
                        continue;
                    VMeter vm = cwnd.getchild(VMeter.class);
                    if (vm == null)
                        continue;

                    if (vm.amount > (100 - fuelticks))
                        continue;

                    int fueltoload = (100 - vm.amount) / fuelticks;

                    // take fuel
                    fuel.item.wdgmsg("take", new Coord(fuel.item.sz.x / 2, fuel.item.sz.y / 2));
                    int timeout = 0;
                    while (gui.hand.isEmpty()) {
                        timeout += HAND_DELAY;
                        if (timeout >= TIMEOUT)
                            continue cloop;
                        try {
                            Thread.sleep(HAND_DELAY);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }

                    for (; fueltoload > 0; fueltoload--) {
                        if (terminate)
                            return;

                        gui.map.wdgmsg("itemact", Coord.z, c.rc, fueltoload == 1 ? 0 : 1, 0, (int) c.id, c.rc, 0, -1);
                        timeout = 0;
                        while (true) {
                            WItem newfuel = gui.vhand;
                            if (newfuel != fuel) {
                                fuel = newfuel;
                                break;
                            }

                            timeout += HAND_DELAY;
                            if (timeout >= TIMEOUT)
                                break;
                            try {
                                Thread.sleep(HAND_DELAY);
                            } catch (InterruptedException e) {
                                return;
                            }
                        }
                    }

                    WItem hand = gui.vhand;
                    // if the crucible is full already we'll end up with a stockpile on the cursor
                    if (hand != null) {
                        gui.map.wdgmsg("place", Coord.z, 0, 3, 0);
                        gui.map.wdgmsg("drop", hand.c.add(Inventory.sqsz.div(2)).div(Inventory.invsq.sz()));
                    }
                }

                try {
                    Thread.sleep(SLEEP);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    private void getfuel() {
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

            // navigate to the stockpile and load up on fuel
            gameui().map.pfRightClick(s, -1, 3, 1, null);
            try {
                gui.map.pfthread.join();
            } catch (InterruptedException e) {
                return;
            }

            // return if got enough fuel
            int availableFuelCoal = gui.maininv.getItemPartialCount("Coal");
            int availableFuelBlock = gui.maininv.getItemPartialCount("Block");
            int availableFuelBranch = gui.maininv.getItemPartialCount("Branch");
            if (availableFuelCoal >= 9 && availableFuelBlock >= 3 && availableFuelBranch >= 18)
                return;
        }
    }

    public void gobselect(Gob gob) {
        Resource res = gob.getres();
        if (res != null) {
            if (res.name.equals("gfx/terobjs/steelcrucible")) {
                if (!crucibles.contains(gob)) {
                    crucibles.add(gob);
                    lblc.settext(crucibles.size() + "");
                }
            } else if (res.name.equals("gfx/terobjs/stockpile-coal") || res.name.equals("gfx/terobjs/stockpile-wblock") || res.name.equals("gfx/terobjs/stockpile-branch")) {
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
        this.destroy();
    }
}
