package haven;


import java.awt.*;
import java.util.List;

public class TimerWdg extends Widget {
    public static final Text.Foundry foundry = new Text.Foundry(Text.sans.deriveFont(Font.BOLD), Config.fontsizeglobal * 12 / 10).aa(true);
    private static final Resource timersfx = Resource.local().loadwait("sfx/timer");
    public final static int height = 31;
    private final static int txty = 8;
    public String name;
    public long start, duration, elapsed;
    public boolean active = false;
    private Label lbltime, lblname;
    private Button btnstart, btnstop, btndel, btnedit;

    public TimerWdg(String name, long duration, long start) {
        this.name = name;
        this.duration = duration;

        sz = new Coord(420, height);
        lblname = new Label(name, foundry, true);
        add(lblname, new Coord(3, txty));
        lbltime = new Label(timeFormat(duration), foundry, true);

        add(lbltime, new Coord(190, txty));

        btnstart = new Button(50, "Start") {
            @Override
            public void click() {
                start();
            }
        };
        btnstop = new Button(50, "Stop") {
            @Override
            public void click() {
                stop();
            }
        };
        btnstop.hide();
        btndel = new Button(20, "X") {
            @Override
            public void click() {
                delete();
            }
        };
        btnedit = new Button(50, "Edit") {
            @Override
            public void click() {
                edit();
            }
        };

        add(btnstart, new Coord(270, 3));
        add(btnstop, new Coord(270, 3));
        add(btnedit, new Coord(334, 3));
        add(btndel, new Coord(395, 3));

        if (start != 0)
            start(start);
    }

    @Override
    public void draw(GOut g) {
        g.chcolor(0, 0, 0, 128);
        g.line(new Coord(0, 0), new Coord(sz.x, 0), 1);
        g.line(new Coord(0, sz.y), new Coord(sz.x, sz.y), 1);
        g.chcolor();
        draw(g, true);
    }

    public void updateRemaining() {
        lbltime.settext(timeFormat(duration - elapsed));
    }

    public void updateDuration() {
        lbltime.settext(timeFormat(duration));
    }


    public void updateName() {
        lblname.settext(name.length() > 21 ? name.substring(0, 20) : name);
    }

    private String timeFormat(long time) {
        long ts = time / 1000;
        return String.format("%02d:%02d.%02d", (int) (ts / 3600), (int) ((ts % 3600) / 60), (int) (ts % 60));
    }

    public void start() {
        start = Glob.timersThread.globtime() / 3;
        btnstart.hide();
        btnstop.show();
        active = true;
        Glob.timersThread.save();
    }

    public void start(long start) {
        this.start = start;
        btnstart.hide();
        btnstop.show();
        active = true;
    }

    public void delete() {
        active = false;
        Glob.timersThread.remove(this);
        reqdestroy();
        int y = this.c.y;
        List<TimerWdg> timers = Glob.timersThread.getall();
        for (TimerWdg timer : timers) {
            if (timer.c.y > y)
                timer.c.y -= height;
        }
        parent.resize(TimersWnd.width, timers.size() * TimerWdg.height + 60);
        Glob.timersThread.save();
    }

    public void stop() {
        active = false;
        btnstart.show();
        btnstop.hide();
        updateDuration();
    }

    public void done() {
        stop();
        GameUI gui = ((TimersWnd) parent).gui;
        gui.add(new TimerDoneWindow(name), new Coord(gui.sz.x / 2 - 150, gui.sz.y / 2 - 75));
        if (Config.timersalarm)
            Audio.play(timersfx, Config.timersalarmvol);
        Glob.timersThread.save();
    }

    public void edit() {
        GameUI gui = ((TimersWnd) parent).gui;
        gui.add(new TimerEditWnd("Edit Timer", gui, name, duration, this), new Coord(gui.sz.x / 2 - 200, gui.sz.y / 2 - 200));
    }

    private class TimerDoneWindow extends Window {
        public TimerDoneWindow(String timername) {
            super(new Coord(300, 130), "Hooray!");

            Label lbltimer = new Label(timername, foundry);
            add(lbltimer, new Coord(300 / 2 - lbltimer.sz.x / 2, 20));

            Label lblinf = new Label("has finished running");
            add(lblinf, new Coord(300 / 2 - lblinf.sz.x / 2, 50));

            add(new Button(60, "Close") {
                @Override
                public void click() {
                    parent.reqdestroy();
                }
            }, new Coord(300 / 2 - 60 / 2, 90));
        }

        @Override
        public void wdgmsg(Widget sender, String msg, Object... args) {
            if (sender == cbtn)
                reqdestroy();
            else
                super.wdgmsg(sender, msg, args);
        }

        @Override
        public boolean type(char key, java.awt.event.KeyEvent ev) {
            if (key == 27) {
                reqdestroy();
                return true;
            }
            return super.type(key, ev);
        }
    }
}
