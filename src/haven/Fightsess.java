/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.util.*;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;

public class Fightsess extends Widget {
    public static final Tex lframe = Resource.loadtex("gfx/hud/combat/lframe");
    public static final int actpitch = 50;
    public final Indir<Resource>[] actions;
    public final boolean[] dyn;
    public int use = -1;
    public Coord pcc;
    public int pho;
    private final Fightview fv;

    @RName("fsess")
    public static class $_ implements Factory {
        public Widget create(Widget parent, Object[] args) {
            int nact = (Integer) args[0];
            return (new Fightsess(nact, parent.getparent(GameUI.class).fv));
        }
    }

    @SuppressWarnings("unchecked")
    public Fightsess(int nact, Fightview fv) {
        this.fv = fv;
        pho = -40;
        this.actions = (Indir<Resource>[]) new Indir[nact];
        this.dyn = new boolean[nact];
    }

    public void presize() {
        resize(parent.sz);
        pcc = sz.div(2);
    }

    protected void added() {
        presize();
    }

    private void updatepos() {
        MapView map;
        Gob pl;
        if (((map = getparent(GameUI.class).map) == null) || ((pl = map.player()) == null) || (pl.sc == null))
            return;
        pcc = pl.sc;
        pho = (int) (pl.sczu.mul(20f).y) - 20;
    }

    private static final Text.Furnace ipf = new PUtils.BlurFurn(new Text.Foundry(Text.serif, 18, new Color(128, 128, 255)).aa(true), 1, 1, new Color(48, 48, 96));
    private final Text.UText<?> ip = new Text.UText<Integer>(ipf) {
        public String text(Integer v) {
            return ("IP: " + v);
        }

        public Integer value() {
            return (fv.current.ip);
        }
    };
    private final Text.UText<?> oip = new Text.UText<Integer>(ipf) {
        public String text(Integer v) {
            return ("IP: " + v);
        }

        public Integer value() {
            return (fv.current.oip);
        }
    };

    public void draw(GOut g) {
        updatepos();
        double now = System.currentTimeMillis() / 1000.0;

        for (Buff buff : fv.buffs.children(Buff.class))
            buff.draw(g.reclip(pcc.add(-buff.c.x - Buff.cframe.sz().x - 20, buff.c.y + pho - Buff.cframe.sz().y), buff.sz));
        if (fv.current != null) {
            for (Buff buff : fv.current.buffs.children(Buff.class))
                buff.draw(g.reclip(pcc.add(buff.c.x + 20, buff.c.y + pho - Buff.cframe.sz().y), buff.sz));

            g.aimage(ip.get().tex(), pcc.add(-75, 0), 1, 0.5);
            g.aimage(oip.get().tex(), pcc.add(75, 0), 0, 0.5);
        }

        if (now < fv.atkct) {
            int w = (int) ((fv.atkct - now) * 20);
            g.chcolor(255, 0, 128, 255);
            g.frect(pcc.add(-w, 20), new Coord(w * 2, 15));
            g.chcolor();
            double cc = (fv.atkct - now);
            DecimalFormat df = new DecimalFormat("#.#");
            g.astext(df.format(cc) + "s", pcc.add(11, 35), 1, 1, Color.WHITE, Color.BLACK);
        }
        Coord ca = pcc.add(-(actions.length * actpitch) / 2, 45);
        for (int i = 0; i < actions.length; i++) {
            Indir<Resource> act = actions[i];
            try {
                if (act != null) {
                    Tex img = act.get().layer(Resource.imgc).tex();
                    g.image(img, ca);
                    g.image(dyn[i] ? lframe : Buff.frame, ca.sub(Buff.imgoff));
                    String hotkeyText;
                    hotkeyText = ""+(i+1);
                    g.astext(hotkeyText, ca.add( img.sz().x/2 + 5, img.sz().y + 1), 1, 1, Color.WHITE, Color.BLACK);
                    if (i == use) {
                        g.chcolor(255, 0, 128, 255);
                        Coord cc = ca.add(img.sz().x / 2, img.sz().y + 5);
                        g.frect(cc.sub(2, 2), new Coord(5, 5));
                        g.chcolor();
                    }
                }
            } catch (Loading l) {
            }
            ca.x += actpitch;
        }
    }

    private Widget prevtt = null;

    public Object tooltip(Coord c, Widget prev) {
        for (Buff buff : fv.buffs.children(Buff.class)) {
            Coord dc = pcc.add(-buff.c.x - Buff.cframe.sz().x - 20, buff.c.y + pho - Buff.cframe.sz().y);
            if (c.isect(dc, buff.sz)) {
                Object ret = buff.tooltip(c.sub(dc), prevtt);
                if (ret != null) {
                    prevtt = buff;
                    return (ret);
                }
            }
        }
        if (fv.current != null) {
            for (Buff buff : fv.current.buffs.children(Buff.class)) {
                Coord dc = pcc.add(buff.c.x + 20, buff.c.y + pho - Buff.cframe.sz().y);
                if (c.isect(dc, buff.sz)) {
                    Object ret = buff.tooltip(c.sub(dc), prevtt);
                    if (ret != null) {
                        prevtt = buff;
                        return (ret);
                    }
                }
            }
        }
        Coord ca = pcc.add(-(actions.length * actpitch) / 2, 45);
        for (int i = 0; i < actions.length; i++) {
            Indir<Resource> act = actions[i];
            try {
                if (act != null) {
                    Tex img = act.get().layer(Resource.imgc).tex();
                    if (c.isect(ca, img.sz())) {
                        if (dyn[i])
                            return ("Combat discovery");
                        return (act.get().layer(Resource.tooltip).t);
                    }
                }
            } catch (Loading l) {
            }
            ca.x += actpitch;
        }
        return (null);
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "act") {
            int n = (Integer) args[0];
            if (args.length > 1) {
                Indir<Resource> res = ui.sess.getres((Integer) args[1]);
                actions[n] = res;
                dyn[n] = ((Integer) args[2]) != 0;
            } else {
                actions[n] = null;
            }
        } else if (msg == "use") {
            this.use = (Integer) args[0];
        } else if (msg == "used") {
        } else if (msg == "dropped") {
        } else {
            super.uimsg(msg, args);
        }
    }

    public boolean globtype(char key, KeyEvent ev) {
        int c = ev.getKeyChar();
        if((key == 0) && (c >= KeyEvent.VK_1) && (c < KeyEvent.VK_1 + actions.length)) {
            int n = c - KeyEvent.VK_1;
            if ((ev.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0)
                wdgmsg("drop", n);
            else
                wdgmsg("use", n);
            return (true);
        }
        return (super.globtype(key, ev));
    }
}
