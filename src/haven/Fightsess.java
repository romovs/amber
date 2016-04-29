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

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Fightsess extends Widget {
    private static final Text.Foundry cdfndr = new Text.Foundry(Text.serif, 18).aa(true);
    private static final Color cdclrpos = new Color(128, 128, 255);
    private static final Color cdclrneg = new Color(239, 41, 41);
    public static final Tex lframe = Resource.loadtex("gfx/hud/combat/lframe");
    public static final int actpitch = 50;
    public final Indir<Resource>[] actions;
    public final boolean[] dyn;
    public int use = -1;
    public Coord pcc;
    public int pho;
    private final Fightview fv;
    private static final DecimalFormat cdfmt = new DecimalFormat("#.#");
    private static final Map<Long, Tex> cdvalues = new HashMap<Long, Tex>(7);

    private static final Map<String, Integer> atkcds = new HashMap<String, Integer>(9){{
        put("Chop", 50);
        put("Cleave", 80);
        put("Haymaker", 60);
        put("Kick", 45);
        put("Knock Its Teeth Out", 35);
        put("Left Hook", 40);
        put("Low Blow", 30);
        put("Punch", 30);
        put("Sting", 35);
    }};
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

    private static final Resource tgtfx = Resource.local().loadwait("gfx/hud/combat/trgtarw");
    private final Map<Pair<Long, Resource>, Sprite> cfx = new CacheMap<Pair<Long, Resource>, Sprite>();
    private final Collection<Sprite> curfx = new ArrayList<Sprite>();

    private void fxon(long gobid, Resource fx) {
	MapView map = getparent(GameUI.class).map;
	Gob gob = ui.sess.glob.oc.getgob(gobid);
	if((map == null) || (gob == null))
	    return;
	Pair<Long, Resource> id = new Pair<Long, Resource>(gobid, fx);
	Sprite spr = cfx.get(id);
	if(spr == null)
	    cfx.put(id, spr = Sprite.create(null, fx, Message.nil));
	map.drawadd(gob.loc.apply(spr));
	curfx.add(spr);
    }

    public void tick(double dt) {
	for(Sprite spr : curfx)
	    spr.tick((int)(dt * 1000));
	curfx.clear();
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

        DefBar my = new DefBar();
        for(Buff buff : fv.buffs.children(Buff.class)) {
            Coord c = pcc.add(-buff.c.x - Buff.cframe.sz().x - 20, buff.c.y + pho - Buff.cframe.sz().y);
    		buff.draw(g.reclip(c, buff.sz));
            my.addBuff(buff);
    	}
        my.draw(g, pcc.add(-Buff.cframe.sz().x - 20,pho - Buff.cframe.sz().y));
    	if(fv.current != null) {
            DefBar opp = new DefBar();
    	    for(Buff buff : fv.current.buffs.children(Buff.class)) {
                buff.draw(g.reclip(pcc.add(buff.c.x + 20, buff.c.y + pho - Buff.cframe.sz().y), buff.sz));
                opp.addBuff(buff);
            }
            opp.draw(g, pcc.add(20, pho - Buff.cframe.sz().y));

            g.aimage(ip.get().tex(), pcc.add(-75, 0), 1, 0.5);
            g.aimage(oip.get().tex(), pcc.add(75, 0), 0, 0.5);

	        if(fv.lsrel.size() > 1)
		        fxon(fv.current.gobid, tgtfx);

            if (Config.showcddelta && fv.current != null) {
                Tex cdtex = cdvalues.get(fv.current.gobid);
                if (cdtex != null)
                    g.aimage(cdtex, pcc.add(0, 110), 0.5, 1);
            }
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
                    if (Config.smallicon) { 
                    g.image(img, ca, new Coord((img.sz().x/2), img.sz().y/2));
                    g.image(dyn[i] ? lframe : Buff.frame, ca.sub(Buff.imgoff), new Coord(lframe.sz().x/2, lframe.sz().y/2));
                    String hotkeyText;
                    hotkeyText = ""+(i+1);
                    g.astext(hotkeyText, ca.add( img.sz().x/4 + 5, img.sz().y/2 + 10), 1, 1, Color.WHITE, Color.BLACK);
                    } else {
                        g.image(img, ca);
                        g.image(dyn[i] ? lframe : Buff.frame, ca.sub(Buff.imgoff));
                         String hotkeyText;
                         hotkeyText = ""+(i+1);
                         g.astext(hotkeyText, ca.add( img.sz().x/2 + 5, img.sz().y + 1), 1, 1, Color.WHITE, Color.BLACK);
                    }
                    if (i == use) {
                        g.chcolor(255, 0, 128, 255);
                        g.frect(ca.add(0, img.sz().y + 3), new Coord(img.sz().x, 5));
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
            Indir<Resource> act = actions[(Integer) args[0]];
            try {
                if (act != null) {
                    Resource.Tooltip tt = act.get().layer(Resource.Tooltip.class);
                    if (tt != null) {
                        Integer cd = atkcds.get(tt.t);
                        if (cd != null && fv.current != null) {
                            double inc = fv.atkcd - cd;
                            int cddelta = -(int) (inc / (double) cd * 100);
                            cdvalues.put(fv.current.gobid, Text.renderstroked(cddelta + " %", cddelta < 0 ? cdclrneg : cdclrpos, Color.BLACK, cdfndr).tex());
                        }
                    }
                }
            } catch (Loading l) {
            }
        } else if (msg == "dropped") {
        } else {
            super.uimsg(msg, args);
        }
    }

    public boolean globtype(char key, KeyEvent ev) {
        int c = ev.getKeyChar();
        if (Config.userazerty)
            c = Utils.azerty2qwerty((char) c);

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
    private static class DefBar {
        private static final Coord bsz = new Coord(20, 10);

        private final Map<AttackType, List<Buff>> defs = new HashMap<AttackType, List<Buff>>();

        public void addBuff(Buff buff) {
            Resource.Image img = buff.getImage();
            if (img != null) {
                int attackType = CombatHelper.getAttackType(img.img);
                for (AttackType type : AttackType.All) {
                    if ((type.value & attackType) != 0) {
                        List<Buff> buffs = defs.get(type);
                        if (buffs == null) {
                            buffs = new ArrayList<Buff>();
                            defs.put(type, buffs);
                        }
                        buffs.add(buff);
                    }
                }
            }
        }

        public void draw(GOut g, Coord c) {
            int y = 15;
            for (AttackType type : defs.keySet()) {
                int x = 0;
                for (Buff buff : defs.get(type)) {
                    g.chcolor(type.color, 128);
                    g.frect(c.add(x, -y), new Coord((buff.ameter * bsz.x) / 100, bsz.y));
                    g.chcolor(Color.LIGHT_GRAY);
                    g.rect(c.add(x, -y), bsz);
                    g.chcolor();
                    x += bsz.x + 5;
                }
                y += bsz.y + 5;
            }
        }
    }
}
