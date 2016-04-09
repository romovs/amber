package haven;

import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class MinimapWnd extends Widget {
    private static final Tex bg = Resource.loadtex("gfx/hud/wnd/lg/bg");
    private static final Tex cl = Resource.loadtex("gfx/hud/wndmap/lg/cl");
    private static final Tex tm = Resource.loadtex("gfx/hud/wnd/lg/tm");
    private static final Tex tr = Resource.loadtex("gfx/hud/wndmap/lg/tr");
    private static final Tex lm = Resource.loadtex("gfx/hud/wndmap/lg/lm");
    private static final Tex rm = Resource.loadtex("gfx/hud/wnd/lg/rm");
    private static final Tex bl = Resource.loadtex("gfx/hud/wndmap/lg/bl");
    private static final Tex bm = Resource.loadtex("gfx/hud/wnd/lg/bm");
    private static final Tex br = Resource.loadtex("gfx/hud/wndmap/lg/br");
    private static final Coord tlm = new Coord(18, 30), brm = new Coord(13, 22);
    private final Widget mmap;
    private final MapView map;
    private IButton center, viewdist, grid;
    private ToggleButton pclaim, vclaim, lock;
    private boolean minimized;
    private Coord szr;
    private boolean resizing;
    private Coord doff;
    private static final Coord minsz = new Coord(215, 130);
    private static final BufferedImage[] cbtni = new BufferedImage[]{
            Resource.loadimg("gfx/hud/wndmap/lg/cbtnu"),
            Resource.loadimg("gfx/hud/wndmap/lg/cbtnh")};
    private final Coord tlo, rbo, mrgn;
    private final IButton cbtn;
    private Coord wsz, ctl, csz, atl, asz;
    private UI.Grab dm = null;

    public MinimapWnd(Coord sz, MapView _map) {
        this.tlo = Coord.z;
        this.rbo = Coord.z;
        this.mrgn = Coord.z;
        cbtn = add(new IButton(cbtni[0], cbtni[1]));
        resize(sz);
        setfocustab(true);
        this.mmap = new LocalMiniMap(Utils.getprefc("mmapsz", new Coord(290, 271)), _map);
        this.map = _map;
        this.c = Coord.z;

        if (Utils.getprefb("showpclaim", false))
            map.enol(0, 1);
        if (Utils.getprefb("showvclaim", false))
            map.enol(2, 3);

        pclaim = new ToggleButton("gfx/hud/wndmap/btns/claim", "gfx/hud/wndmap/btns/claim-d", map.visol(0)) {
            {
                tooltip = Text.render(Resource.getLocString(Resource.l10nLabel, "Display personal claims"));
            }

            public void click() {
                if ((map != null) && !map.visol(0)) {
                    map.enol(0, 1);
                    Utils.setprefb("showpclaim", true);
                } else {
                    map.disol(0, 1);
                    Utils.setprefb("showpclaim", false);
                }
            }
        };
        vclaim = new ToggleButton("gfx/hud/wndmap/btns/vil", "gfx/hud/wndmap/btns/vil-d", map.visol(2)) {
            {
                tooltip = Text.render(Resource.getLocString(Resource.l10nLabel, "Display village claims"));
            }

            public void click() {
                if ((map != null) && !map.visol(2)) {
                    map.enol(2, 3);
                    Utils.setprefb("showvclaim", true);
                } else {
                    map.disol(2, 3);
                    Utils.setprefb("showvclaim", false);
                }
            }
        };
        center = new IButton("gfx/hud/wndmap/btns/center", "", "", "") {
            {
                tooltip = Text.render(Resource.getLocString(Resource.l10nLabel, "Center the map on player"));
            }

            public void click() {
                ((LocalMiniMap) mmap).center();
            }
        };
        lock = new ToggleButton("gfx/hud/wndmap/btns/lock-d", "gfx/hud/wndmap/btns/lock", Config.maplocked) {
            {
                tooltip = Text.render(Resource.getLocString(Resource.l10nLabel, "Lock map dragging"));
            }

            public void click() {
                Config.maplocked = !Config.maplocked;
                Utils.setprefb("maplocked", Config.maplocked);
            }
        };
        viewdist = new IButton("gfx/hud/wndmap/btns/viewdist", "", "", "") {
            {
                tooltip = Text.render(Resource.getLocString(Resource.l10nLabel, "Show view distance box"));
            }

            public void click() {
                Config.mapshowviewdist = !Config.mapshowviewdist;
                Utils.setprefb("mapshowviewdist", Config.mapshowviewdist);
            }
        };
        grid = new IButton("gfx/hud/wndmap/btns/grid", "", "", "") {
            {
                tooltip = Text.render(Resource.getLocString(Resource.l10nLabel, "Show map grid"));
            }

            public void click() {
                Config.mapshowgrid = !Config.mapshowgrid;
                Utils.setprefb("mapshowgrid", Config.mapshowgrid);
            }
        };

        add(mmap, 1, 27);
        add(pclaim, 5, 3);
        add(vclaim, 29, 3);
        add(center, 53, 3);
        add(lock, 77, 3);
        add(viewdist, 101, 3);
        add(grid, 125, 3);
        pack();
    }

    @Override
    protected void added() {
        parent.setfocus(this);
    }

    private void drawframe(GOut g) {
        Coord mdo, cbr;

        g.image(cl, tlo);
        mdo = tlo.add(cl.sz().x, 0);
        cbr = tlo.add(wsz.add(-tr.sz().x, tm.sz().y));
        for (; mdo.x < cbr.x; mdo.x += tm.sz().x)
            g.image(tm, mdo, Coord.z, cbr);
        g.image(tr, tlo.add(wsz.x - tr.sz().x, 0));

        mdo = tlo.add(0, cl.sz().y);
        cbr = tlo.add(lm.sz().x, wsz.y - bl.sz().y);
        for (; mdo.y < cbr.y; mdo.y += lm.sz().y)
            g.image(lm, mdo, Coord.z, cbr);

        mdo = tlo.add(wsz.x - rm.sz().x, tr.sz().y);
        cbr = tlo.add(wsz.x, wsz.y - br.sz().y);
        for (; mdo.y < cbr.y; mdo.y += rm.sz().y)
            g.image(rm, mdo, Coord.z, cbr);

        g.image(bl, tlo.add(0, wsz.y - bl.sz().y));

        mdo = tlo.add(bl.sz().x, wsz.y - bm.sz().y);
        cbr = tlo.add(wsz.x - br.sz().x, wsz.y);
        for (; mdo.x < cbr.x; mdo.x += bm.sz().x)
            g.image(bm, mdo, Coord.z, cbr);
        g.image(br, tlo.add(wsz.sub(br.sz())));
    }

    @Override
    public void draw(GOut g) {
        Coord bgc = new Coord();
        for (bgc.y = ctl.y; bgc.y < ctl.y + csz.y; bgc.y += bg.sz().y) {
            for (bgc.x = ctl.x; bgc.x < ctl.x + csz.x; bgc.x += bg.sz().x)
                g.image(bg, bgc, ctl, csz);
        }
        drawframe(g);
        super.draw(g);
    }

    @Override
    public Coord contentsz() {
        Coord max = new Coord(0, 0);
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg == cbtn)
                continue;
            if (!wdg.visible)
                continue;
            Coord br = wdg.c.add(wdg.sz);
            if (br.x > max.x)
                max.x = br.x;
            if (br.y > max.y)
                max.y = br.y;
        }
        return (max);
    }

    @Override
    public void resize(Coord sz) {
        asz = sz;
        csz = asz.add(mrgn.mul(2));
        wsz = csz.add(tlm).add(brm);
        this.sz = wsz.add(tlo).add(rbo);
        ctl = tlo.add(tlm);
        atl = ctl.add(mrgn);
        cbtn.c = xlate(tlo.add(wsz.x - cbtn.sz.x, 0), false);
        for (Widget ch = child; ch != null; ch = ch.next)
            ch.presize();
    }

    @Override
    public void uimsg(String msg, Object... args) {
        if (msg == "pack") {
            pack();
        } else if (msg == "dt") {
            return;
        } else if (msg == "cap") {
            return;
        } else {
            super.uimsg(msg, args);
        }
    }

    @Override
    public Coord xlate(Coord c, boolean in) {
        if (in)
            return (c.add(atl));
        else
            return (c.sub(atl));
    }

    @Override
    public boolean mousedown(Coord c, int button) {
        if (!minimized && c.x > sz.x - 40 && c.y > sz.y - 30) {
            doff = c;
            dm = ui.grabmouse(this);
            resizing = true;
            return true;
        }

        if (!minimized) {
            parent.setfocus(this);
            raise();
        }

        if (super.mousedown(c, button)) {
            parent.setfocus(this);
            raise();
            return true;
        }

        if (c.isect(ctl, csz)) {
            if (button == 1) {
                dm = ui.grabmouse(this);
                doff = c;
            }
            parent.setfocus(this);
            raise();
            return true;
        }
        return false;
    }

    @Override
    public void mousemove(Coord c) {
        if (resizing && dm != null) {
            Coord d = c.sub(doff);
            doff = c;
            mmap.sz.x = Math.max(mmap.sz.x + d.x, minsz.x);
            mmap.sz.y = Math.max(mmap.sz.y + d.y, minsz.y);
            pack();
            Utils.setprefc("mmapwndsz", sz);
            Utils.setprefc("mmapsz", mmap.sz);
        } else {
            if (dm != null)
                this.c = this.c.add(c.add(doff.inv()));
            else
                super.mousemove(c);
        }
    }

    @Override
    public boolean mouseup(Coord c, int button) {
        resizing = false;

        if (dm != null) {
            Utils.setprefc("mmapc", this.c);
            dm.remove();
            dm = null;
        } else {
            return super.mouseup(c, button);
        }
        return true;
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == cbtn) {
            minimize();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    @Override
    public boolean type(char key, KeyEvent ev) {
        if (key == KeyEvent.VK_ESCAPE) {
            wdgmsg(cbtn, "click");
            return (true);
        }
        return (super.type(key, ev));
    }

    private void minimize() {
        minimized = !minimized;
        if (minimized) {
            mmap.hide();
            pclaim.hide();
            vclaim.hide();
            center.hide();
            lock.hide();
            viewdist.hide();
            grid.hide();
        } else {
            mmap.show();
            pclaim.show();
            vclaim.show();
            center.show();
            lock.show();
            viewdist.show();
            grid.show();
        }

        if (minimized) {
            szr = asz;
            resize(new Coord(asz.x, 24));
        } else {
            resize(szr);
        }
    }
}