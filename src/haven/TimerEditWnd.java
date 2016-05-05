package haven;


import java.awt.event.KeyEvent;
import java.util.List;

public class TimerEditWnd extends Window {

    public TimerEditWnd(String cap, final GameUI gui, String timername, long duration, final TimerWdg timer) {
        super(new Coord(355, 100), cap);

        add(new Label("Name"), new Coord(15, 10));
        final TextEntry txtname = new TextEntry(200, timername != null ? timername : "");
        add(txtname, new Coord(15, 30));

        long ds = duration / 1000;
        int hours = (int) (ds / 3600);
        int minutes = (int) ((ds % 3600) / 60);

        add(new Label("Hours"), new Coord(225, 10));
        final TextEntry txthours = new TextEntry(50, hours != 0 ? hours + "" : "") {
            @Override
            public boolean type(char c, KeyEvent ev) {
                if (c == 0x8 || c == 0x7f || c == 0x09 || (c >= 0x30 && c <= 0x39 && text.length() <= 2))
                    return super.type(c, ev);
                return true;
            }
        };
        add(txthours, new Coord(225, 30));

        add(new Label("Minutes"), new Coord(285, 10));
        final TextEntry txtminutes = new TextEntry(50, minutes != 0 ? minutes + "" : "") {
            @Override
            public boolean type(char c, KeyEvent ev) {
                if (c == 0x8 || c == 0x7f || c == 0x09 || (c >= 0x30 && c <= 0x39 && text.length() <= 1))
                    return super.type(c, ev);
                return true;
            }
        };
        add(txtminutes, new Coord(285, 30));

        Button add;
        if (timer != null) {
            add = new Button(60, "Save") {
                @Override
                public void click() {
                    if (timer.active)
                        timer.stop();
                    timer.name = txtname.text;
                    int hours = Integer.parseInt(txthours.text.equals("") ? "0" : txthours.text);
                    int minutes = Integer.parseInt(txtminutes.text.equals("") ? "0" : txtminutes.text);
                    timer.duration = (60 * hours + minutes) * 60 * 1000;
                    timer.updateName();
                    timer.updateDuration();
                    Glob.timersThread.save();
                    parent.reqdestroy();
                }
            };
        } else {
            add = new Button(60, "Add") {
                @Override
                public void click() {
                    long hours = Long.parseLong(txthours.text.equals("") ? "0" : txthours.text);
                    long minutes = Long.parseLong(txtminutes.text.equals("") ? "0" : txtminutes.text);
                    long duration = (60 * hours + minutes) * 60 * 1000;
                    int y = 0;
                    List<TimerWdg> timers = Glob.timersThread.getall();
                    for (TimerWdg timer : timers) {
                        if (timer.c.y > y)
                            y = timer.c.y;
                    }
                    if (y == 0)
                        y = 20;

                    GameUI.timerswnd.add(Glob.timersThread.add(txtname.text, duration, 0), new Coord(20, y + TimerWdg.height));
                    Glob.timersThread.save();
                    GameUI.timerswnd.resize(TimersWnd.width, timers.size() * TimerWdg.height + 60);
                    parent.reqdestroy();
                }
            };
        }
        add(add, new Coord(15, 70));

        Button cancel = new Button(60, "Cancel") {
            @Override
            public void click() {
                parent.reqdestroy();
            }
        };
        add(cancel, new Coord(275, 70));
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

    public TimerEditWnd(String cap, final GameUI gui) {
        this(cap, gui, null, 0, null);
    }
}
