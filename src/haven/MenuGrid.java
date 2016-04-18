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
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;

import haven.Glob.Pagina;
import haven.Resource.AButton;
import haven.automation.AddBranchesToOven;
import haven.automation.AddCoalToSmelter;
import haven.automation.AutoLeveler;
import haven.automation.ErrorSysMsgCallback;
import haven.automation.GobSelectCallback;
import haven.automation.LightWithTorch;
import haven.automation.SteelRefueler;
import haven.util.ObservableCollection;
import purus.CarrotFarmer;
import purus.DragonflyCollector;
import purus.MusselPicker;


public class MenuGrid extends Widget {
    public final static Tex bg = Resource.loadtex("gfx/hud/invsq");
    public final static Coord bgsz = bg.sz().add(-1, -1);
    public final static Pagina next = new Glob.Pagina(Resource.local().loadwait("gfx/hud/sc-next").indir());
    public final static Pagina bk = new Glob.Pagina(Resource.local().loadwait("gfx/hud/sc-back").indir());
    public final static RichText.Foundry ttfnd = new RichText.Foundry(TextAttribute.FAMILY, "SansSerif", TextAttribute.SIZE, 10);
    private static Coord gsz = new Coord(4, 4);
    private Pagina cur, pressed, dragging, layout[][] = new Pagina[gsz.x][gsz.y];
    private UI.Grab grab;
    private int curoff = 0;
    private int pagseq = 0;
    private boolean loading = true;
    private Map<Character, Pagina> hotmap = new TreeMap<Character, Pagina>();
    public GameUI gameui;
    private haven.Widget w;
    private haven.Inventory i;

    @RName("scm")
    public static class $_ implements Factory {
        public Widget create(Widget parent, Object[] args) {
            return (new MenuGrid());
        }
    }

    @SuppressWarnings("serial")
	public class PaginaException extends RuntimeException {
        public Pagina pag;

        public PaginaException(Pagina p) {
            super("Invalid pagina: " + p.res);
            pag = p;
        }
    }

    private boolean cons(Pagina p, Collection<Pagina> buf) {
        Collection<Pagina> open, close = new HashSet<Pagina>();
        synchronized (ui.sess.glob.paginae) {
            open = new LinkedList<Pagina>();
            for (Pagina pag : ui.sess.glob.paginae) {
                if (pag.newp == 2) {
                    pag.newp = 0;
                    pag.fstart = 0;
                }
                open.add(pag);
            }
            for (Pagina pag : ui.sess.glob.pmap.values()) {
                if (pag.newp == 2) {
                    pag.newp = 0;
                    pag.fstart = 0;
                }
            }
        }
        boolean ret = true;
        while (!open.isEmpty()) {
            Iterator<Pagina> iter = open.iterator();
            Pagina pag = iter.next();
            iter.remove();
            try {
                AButton ad = pag.act();
                if (ad == null)
                    throw (new PaginaException(pag));
                Pagina parent = paginafor(ad.parent);
                if ((pag.newp != 0) && (parent != null) && (parent.newp == 0)) {
                    parent.newp = 2;
                    parent.fstart = (parent.fstart == 0) ? pag.fstart : Math.min(parent.fstart, pag.fstart);
                }
                if (parent == p)
                    buf.add(pag);
                else if ((parent != null) && !close.contains(parent) && !open.contains(parent))
                    open.add(parent);
                close.add(pag);
            } catch (Loading e) {
                ret = false;
            }
        }
        return (ret);
    }

    public MenuGrid() {
        super(bgsz.mul(gsz).add(1, 1));
    }
    
    @Override
    protected void attach(UI ui) {
    	super.attach(ui);
    	Glob glob = ui.sess.glob;
    	ObservableCollection<Pagina> p = glob.paginae;
    	// Amber Stuff
        if (!Config.hidexmenu) {
            p.add(glob.paginafor(Resource.local().load("paginae/amber/coal11")));
            p.add(glob.paginafor(Resource.local().load("paginae/amber/coal12")));
            p.add(glob.paginafor(Resource.local().load("paginae/amber/branchoven")));
            p.add(glob.paginafor(Resource.local().load("paginae/amber/steel")));
            p.add(glob.paginafor(Resource.local().load("paginae/amber/autosurvey")));
            p.add(glob.paginafor(Resource.local().load("paginae/amber/torch")));
        }
    	// Purus Cor Stuff
    	p.add(glob.paginafor(Resource.local().load("paginae/custom/timer")));
    	p.add(glob.paginafor(Resource.local().load("paginae/custom/study")));
    	p.add(glob.paginafor(Resource.local().load("paginae/custom/mussel")));
    	p.add(glob.paginafor(Resource.local().load("paginae/custom/carrotfarm")));
    	p.add(glob.paginafor(Resource.local().load("paginae/custom/flycollect")));
    	// Disable this for now because amber has one
    	//p.add(glob.paginafor(Resource.local().load("paginae/custom/fillsmelter")));
    	//p.add(glob.paginafor(Resource.local().load("paginae/custom/oven")));
    }


    private static Comparator<Pagina> sorter = new Comparator<Pagina>() {
        public int compare(Pagina a, Pagina b) {
            AButton aa = a.act(), ab = b.act();
            if ((aa.ad.length == 0) && (ab.ad.length > 0))
                return (-1);
            if ((aa.ad.length > 0) && (ab.ad.length == 0))
                return (1);
            return (aa.origName.compareTo(ab.origName));
        }
    };

    private void updlayout() {
        synchronized (ui.sess.glob.paginae) {
            List<Pagina> cur = new ArrayList<Pagina>();
            loading = !cons(this.cur, cur);
            Collections.sort(cur, sorter);
            int i = curoff;
            hotmap.clear();
            for (int y = 0; y < gsz.y; y++) {
                for (int x = 0; x < gsz.x; x++) {
                    Pagina btn = null;
                    if ((this.cur != null) && (x == gsz.x - 1) && (y == gsz.y - 1)) {
                        btn = bk;
                    } else if ((cur.size() > ((gsz.x * gsz.y) - 1)) && (x == gsz.x - 2) && (y == gsz.y - 1)) {
                        btn = next;
                    } else if (i < cur.size()) {
                        Resource.AButton ad = cur.get(i).act();
                        if (ad.hk != 0)
                            hotmap.put(Character.toUpperCase(ad.hk), cur.get(i));
                        btn = cur.get(i++);
                    }
                    layout[x][y] = btn;
                }
            }
            pagseq = ui.sess.glob.pagseq;
        }
    }

    private static Text rendertt(Resource res, boolean withpg) {
        Resource.AButton ad = res.layer(Resource.action);
        Resource.Pagina pg = res.layer(Resource.pagina);
        String tt = ad.name;
        int pos = tt.toUpperCase().indexOf(Character.toUpperCase(ad.hk));
        if (pos >= 0)
            tt = tt.substring(0, pos) + "$b{$col[255,128,0]{" + tt.charAt(pos) + "}}" + tt.substring(pos + 1);
        else if (ad.hk != 0)
            tt += " [" + ad.hk + "]";
        if (withpg && (pg != null)) {
            tt += "\n\n" + pg.text;
        }
        return (ttfnd.render(tt, 300));
    }

    private static Map<Pagina, Tex> glowmasks = new WeakHashMap<Pagina, Tex>();

    private Tex glowmask(Pagina pag) {
        Tex ret = glowmasks.get(pag);
        if (ret == null) {
            ret = new TexI(PUtils.glowmask(PUtils.glowmask(pag.res().layer(Resource.imgc).img.getRaster()), 4, new Color(32, 255, 32)));
            glowmasks.put(pag, ret);
        }
        return (ret);
    }

    public void draw(GOut g) {
        long now = System.currentTimeMillis();
        for (int y = 0; y < gsz.y; y++) {
            for (int x = 0; x < gsz.x; x++) {
                Coord p = bgsz.mul(new Coord(x, y));
                g.image(bg, p);
                Pagina btn = layout[x][y];
                if (btn != null) {
                    Tex btex = btn.img.tex();
                    g.image(btex, p.add(1, 1));
                    if (btn.meter > 0) {
                        double m = btn.meter / 1000.0;
                        if (btn.dtime > 0)
                            m += (1 - m) * (double) (now - btn.gettime) / (double) btn.dtime;
                        m = Utils.clip(m, 0, 1);
                        g.chcolor(255, 255, 255, 128);
                        g.fellipse(p.add(bgsz.div(2)), bgsz.div(2), 90, (int) (90 + (360 * m)));
                        g.chcolor();
                    }
                    if (btn.newp != 0) {
                        if (btn.fstart == 0) {
                            btn.fstart = now;
                        } else {
                            double ph = ((now - btn.fstart) / 1000.0) - (((x + (y * gsz.x)) * 0.15) % 1.0);
                            if (ph < 1.25) {
                                g.chcolor(255, 255, 255, (int) (255 * ((Math.cos(ph * Math.PI * 2) * -0.5) + 0.5)));
                                g.image(glowmask(btn), p.sub(4, 4));
                                g.chcolor();
                            } else {
                                g.chcolor(255, 255, 255, 128);
                                g.image(glowmask(btn), p.sub(4, 4));
                                g.chcolor();
                            }
                        }
                    }
                    if (btn == pressed) {
                        g.chcolor(new Color(0, 0, 0, 128));
                        g.frect(p.add(1, 1), btex.sz());
                        g.chcolor();
                    }
                }
            }
        }
        super.draw(g);
        if (dragging != null) {
            final Tex dt = dragging.img.tex();
            ui.drawafter(new UI.AfterDraw() {
                public void draw(GOut g) {
                    g.image(dt, ui.mc.add(dt.sz().div(2).inv()));
                }
            });
        }
    }

    private Pagina curttp = null;
    private boolean curttl = false;
    private Text curtt = null;
    private long hoverstart;

    public Object tooltip(Coord c, Widget prev) {
        Pagina pag = bhit(c);
        long now = System.currentTimeMillis();
        if ((pag != null) && (pag.act() != null)) {
            if (prev != this)
                hoverstart = now;
            boolean ttl = (now - hoverstart) > 500;
            if ((pag != curttp) || (ttl != curttl)) {
                curtt = rendertt(pag.res(), ttl);
                curttp = pag;
                curttl = ttl;
            }
            return (curtt);
        } else {
            hoverstart = now;
            return ("");
        }
    }

    private Pagina bhit(Coord c) {
        Coord bc = c.div(bgsz);
        if ((bc.x >= 0) && (bc.y >= 0) && (bc.x < gsz.x) && (bc.y < gsz.y))
            return (layout[bc.x][bc.y]);
        else
            return (null);
    }

    public boolean mousedown(Coord c, int button) {
        Pagina h = bhit(c);
        if ((button == 1) && (h != null)) {
            pressed = h;
            grab = ui.grabmouse(this);
        }
        return (true);
    }

    public void mousemove(Coord c) {
        if ((dragging == null) && (pressed != null)) {
            Pagina h = bhit(c);
            if (h != pressed)
                dragging = pressed;
        }
    }

    private Pagina paginafor(Resource.Named res) {
        return (ui.sess.glob.paginafor(res));
    }

    public void use(String[] ad) {
        GameUI gui = gameui();
        if (gui == null)
            return;
        if (ad[1].equals("coal")) {
            Thread t = new Thread(new AddCoalToSmelter(gui, Integer.parseInt(ad[2])), "AddCoalToSmelter");
            t.start();
        } else if (ad[1].equals("branchoven")) {
            Thread t = new Thread(new AddBranchesToOven(gui, Integer.parseInt(ad[2])), "AddBranchesToOven");
            t.start();
        } else if (ad[1].equals("steel")) {
            if (gui.getwnd("Steel Refueler") == null) {
                SteelRefueler sw = new SteelRefueler();
                gui.map.steelrefueler = sw;
                gui.add(sw, new Coord(gui.sz.x / 2 - sw.sz.x / 2, gui.sz.y / 2 - sw.sz.y / 2 - 200));
                synchronized (GobSelectCallback.class) {
                    gui.map.registerGobSelect(sw);
                }
            }
        } else if (ad[1].equals("level")) {
            if (gui.getwnd("Auto Leveler") == null) {
                AutoLeveler lw = new AutoLeveler();
                gui.map.autoleveler = lw;
                gui.add(lw, new Coord(gui.sz.x / 2 - lw.sz.x / 2, gui.sz.y / 2 - lw.sz.y / 2 - 200));
                synchronized (GobSelectCallback.class) {
                    gui.map.registerGobSelect(lw);
                }
                synchronized (ErrorSysMsgCallback.class) {
                    gui.registerErrMsg(lw);
                }
            }
        } else if (ad[1].equals("torch")) {
            Thread t = new Thread(new LightWithTorch(gui), "LightWithTorch");
            t.start();
        }
    }

    private void use(Pagina r, boolean reset) {
        Collection<Pagina> sub = new LinkedList<Pagina>(),
                cur = new LinkedList<Pagina>();
        cons(r, sub);
        cons(this.cur, cur);
        if (sub.size() > 0) {
            this.cur = r;
            curoff = 0;
        } else if (r == bk) {
            this.cur = paginafor(this.cur.act().parent);
            curoff = 0;
        } else if (r == next) {
            if ((curoff + 14) >= cur.size())
                curoff = 0;
            else
                curoff += 14;
        } else {
            r.newp = 0;
            use(r);
            String[] ad = r.act().ad;
            if(ad[0].equals("@")) {
                use(ad);
            } else {
                if (ad.length > 0 && (ad[0].equals("craft") || ad[0].equals("bp")))
                    gameui().histbelt.push(r);
                wdgmsg("act", (Object[]) ad);
            }
            if (reset)
                this.cur = null;
            curoff = 0;
        }
        updlayout();
    }
    
    public boolean use(Pagina r) {
        String [] ad = r.act().ad;
        if((ad == null) || (ad.length < 1)){
            return false;
        }
        if(ad[0].equals("@")) {
            usecustom(ad);
        } else {
        	// Disable to prevent double toggling
           // wdgmsg("act", (Object[])ad);
        }
        return true;
    }

    public void usecustom(String[] ad) {
        if(ad[1].equals("timer")) {
        	GameUI.AvaaTimer();
        } else if (ad[1].equals("study")) {
    		if(ui.gui!=null){
		    ui.gui.toggleStudy();
    		}
        } else if (ad[1].equals("mussel")) {
        	new MusselPicker(ui, w, i).Run(); 
        } else if (ad[1].equals("carrotfarmer")) {
        	new CarrotFarmer(ui, w, i).Run();
        } else if (ad[1].equals("flycollect")) {
        	new DragonflyCollector(ui, w, i).Run();
        }
    }

    public void tick(double dt) {
        if (loading || (pagseq != ui.sess.glob.pagseq))
            updlayout();
    }

    public boolean mouseup(Coord c, int button) {
        Pagina h = bhit(c);
        if ((button == 1) && (grab != null)) {
            if (dragging != null) {
                ui.dropthing(ui.root, ui.mc, dragging.res());
                dragging = pressed = null;
            } else if (pressed != null) {
                if (pressed == h)
                    use(h, false);
                pressed = null;
            }
            grab.remove();
            grab = null;
        }
        return (true);
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "goto") {
            String resnm = (String) args[0];
            if (resnm.equals("")) {
                cur = null;
            } else {
                Resource.Named res = Resource.remote().load(resnm, (Integer) args[1]);
                cur = paginafor(res);
            }
            curoff = 0;
            updlayout();
        }
    }

    public boolean globtype(char k, KeyEvent ev) {
        if (ev.isShiftDown()) {
            return false;
        } else if ((k == 27) && (this.cur != null)) {
            this.cur = null;
            curoff = 0;
            updlayout();
            return (true);
        } else if ((k == 8) && (this.cur != null)) {
            this.cur = paginafor(this.cur.act().parent);
            curoff = 0;
            updlayout();
            return (true);
        } else if ((k == 'N') && (layout[gsz.x - 2][gsz.y - 1] == next)) {
            use(next, false);
            return (true);
        }
        Pagina r = hotmap.get(Character.toUpperCase(k));
        if (r != null) {
            use(r, true);
            return (true);
        }
        return (false);
    }
}
