package haven;

public class ObjNameEditWnd extends Window {

    public ObjNameEditWnd(Coord c, final long gobid) {
        super(new Coord(215, 100), "Edit object name");
        this.c = c;

        add(new Label("Name"), new Coord(15, 10));
        final TextEntry txtname = new TextEntry(200, Utils.getpref(String.format("gobname.%s", gobid), ""));
        add(txtname, new Coord(15, 30));

        Button ok = new Button(60, "Ok") {
            @Override
            public void click() {
                Utils.setpref(String.format("gobname.%s", gobid), txtname.text);
                parent.reqdestroy();
            }
        };
        add(ok, new Coord(15, 70));

        Button cancel = new Button(60, "Cancel") {
            @Override
            public void click() {
                parent.reqdestroy();
            }
        };
        add(cancel, new Coord(155, 70));
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
