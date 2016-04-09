package haven;

import java.awt.event.KeyEvent;

import static haven.Inventory.invsq;

public class FBelt extends Widget implements DTarget, DropTarget {
    private final int beltkeys[] = {KeyEvent.VK_F1, KeyEvent.VK_F2, KeyEvent.VK_F3, KeyEvent.VK_F4,
            KeyEvent.VK_F5, KeyEvent.VK_F6, KeyEvent.VK_F7, KeyEvent.VK_F8,
            KeyEvent.VK_F9, KeyEvent.VK_F10, KeyEvent.VK_F11, KeyEvent.VK_F12};
    @SuppressWarnings("unchecked")
    private Indir<Resource>[] belt = new Indir[12];
    private UI.Grab dragging;
    private Coord dc;
    private static final Indir<Resource> waterskin = Resource.remote().load("gfx/invobjs/small/waterskin");
    private static final Coord vsz = new Coord(34, 450);
    private static final Coord hsz = new Coord(450, 34);
    private boolean vertical;
    private String chrid;

    public FBelt(String chrid, boolean vertical) {
        super(vertical ? vsz : hsz);
        this.chrid = chrid;
        this.vertical = vertical;
    }

    public void load() {
        if (chrid != "") {
            String[] resnames = Utils.getprefsa("fbelt_" + chrid, null);
            if (resnames != null) {
                for (int i = 0; i < 12; i++) {
                    String resname = resnames[i];
                    if (!resname.equals("null"))
                        belt[i] = Resource.remote().load(resnames[i]);
                }
            }
        }
    }

    private void save() {
        String chrid = gameui().chrid;
        if (chrid != "") {
            String[] resnames = new String[12];
            for (int i = 0; i < 12; i++) {
                Indir<Resource> res = belt[i];
                if (res != null)
                    resnames[i] = res.get().name;
            }
            Utils.setprefsa("fbelt_" + chrid, resnames);
        }
    }

    private Coord beltc(int i) {
        if (vertical)
            return new Coord(0, ((invsq.sz().x + 2) * i) + (10 * (i / 4)));
        return new Coord(((invsq.sz().x + 2) * i) + (10 * (i / 4)), 0);
    }

    private int beltslot(Coord c) {
        for (int i = 0; i < 12; i++) {
            if (c.isect(beltc(i), invsq.sz()))
                return i;
        }
        return -1;
    }

    @Override
    public void draw(GOut g) {
        for (int i = 0; i < 12; i++) {
            int slot = i;
            Coord c = beltc(i);
            g.image(invsq, c);
            try {
                if (belt[slot] != null)
                    g.image(belt[slot].get().layer(Resource.imgc).tex(), c.add(1, 1));
            } catch (Loading e) {
            }
            g.chcolor(156, 180, 158, 255);
            FastText.aprintf(g, c.add(invsq.sz().sub(2, 0)), 1, 1, "F%d", i + 1);
            g.chcolor();
        }
    }

    @Override
    public boolean mousedown(Coord c, int button) {
        int slot = beltslot(c);
        if (slot != -1) {
            if (belt[slot] == null) {
                if (ui.modshift) {
                    if (vertical) {
                        sz = hsz;
                        vertical = false;
                    } else {
                        sz = vsz;
                        vertical = true;
                    }
                    Utils.setprefb("fbelt_vertical", vertical);
                } else {
                    dragging = ui.grabmouse(this);
                    dc = c;
                }
                return true;
            }

            if (button == 1) {
                use(slot);
            } else if (button == 3) {
                Resource res = belt[slot].get();
                if (res.name.equals("gfx/invobjs/small/waterskin") || res.name.equals("gfx/invobjs/waterflask"))
                    gameui().wdgmsg("setbelt", getServerSlot(slot), 1);
                belt[slot] = null;
                save();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseup(Coord c, int button) {
        if (dragging != null) {
            dragging.remove();
            dragging = null;
            Utils.setprefc("fbelt_c", this.c);
            return true;
        }
        return super.mouseup(c, button);
    }

    @Override
    public void mousemove(Coord c) {
        if (dragging != null) {
            this.c = this.c.add(c.x, c.y).sub(dc);
            return;
        }
        super.mousemove(c);
    }

    @Override
    public boolean globtype(char key, KeyEvent ev) {
        if (key != 0)
            return false;
        for (int i = 0; i < beltkeys.length; i++) {
            if (ev.getKeyCode() == beltkeys[i]) {
                if (belt[i] != null)
                    use(i);
                return true;
            }
        }
        return false;
    }

    public boolean drop(Coord c, Coord ul) {
        int slot = beltslot(c);
        if (slot != -1) {

            WItem item = gameui().vhand;
            if (item != null && item.item != null) {
                Resource res = item.item.getres();
                if (res.name.equals("gfx/invobjs/waterskin"))
                    belt[slot] = waterskin;
                else if (res.name.equals("gfx/invobjs/waterflask"))
                    belt[slot] = item.item.getres().indir();
                else
                    return true;
                gameui().wdgmsg("setbelt", getServerSlot(slot), 0);
                save();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean iteminteract(Coord c, Coord ul) {
        return false;
    }

    public boolean dropthing(Coord c, Object thing) {
        int slot = beltslot(c);
        if (slot != -1) {
            if (thing instanceof Resource) {
                Resource res = (Resource) thing;
                if (res.layer(Resource.action) != null) {
                    belt[slot] = res.indir();
                    save();
                    return true;
                }
            }
        }
        return false;
    }

    private void use(int slot) {
        Resource res = belt[slot].get();
        if (res.name.equals("gfx/invobjs/small/waterskin") || res.name.equals("gfx/invobjs/waterflask")) {
            gameui().wdgmsg("belt", getServerSlot(slot), 1, ui.modflags());
        } else {
            Resource.AButton act = res.layer(Resource.action);
            if (act == null)
                return;

            if (res.name.startsWith("paginae/amber"))
                gameui().menu.use(act.ad);
            else
                gameui().menu.wdgmsg("act", (Object[]) act.ad);
        }
    }

    // can't (?) drink without activating the flower menu unless the container is on the default belt
    // so we store it server-side in the 11th and 12th slots which are not used anyway
    private int getServerSlot(int slot) {
        return slot < 6 ? (slot + 1) * 10 : (slot + 1) * 11;
    }
}