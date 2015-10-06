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
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.font.TextAttribute;
import java.util.*;

import static haven.Window.wbox;
import static haven.PUtils.*;

import haven.resutil.FoodInfo;

public class CharWnd extends Window {
    public static final RichText.Foundry ifnd = new RichText.Foundry(Resource.remote(), java.awt.font.TextAttribute.FAMILY, "SansSerif", java.awt.font.TextAttribute.SIZE, 9).aa(true);
    public static final Text.Furnace catf = new BlurFurn(new TexFurn(new Text.Foundry(Text.sans, 20).aa(true), Window.ctex), 2, 2, new Color(96, 48, 0));
    public static final Text.Foundry attrf = new Text.Foundry(Text.sansb, 14).aa(true);
    public static final Color debuff = new Color(255, 128, 128);
    public static final Color buff = new Color(128, 255, 128);
    public static final Color tbuff = new Color(128, 128, 255);
    public static final Color every = new Color(255, 255, 255, 16), other = new Color(255, 255, 255, 32);
    public final Collection<Attr> base;
    public final Collection<SAttr> skill;
    public final FoodMeter feps;
    public final GlutMeter glut;
    public final Constipations cons;
    public final SkillList csk, nsk;
    public final ExperienceList exps;
    public final Widget woundbox;
    public final WoundList wounds;
    public Wound.Info wound;
    public int exp, enc;
    private int scost;
    private final Tabs.Tab sattr, fgt;

    public static class FoodMeter extends Widget {
        public static final Tex frame = Resource.loadtex("gfx/hud/chr/foodm");
        public static final Coord marg = new Coord(5, 5), trmg = new Coord(10, 10);
        public double cap;
        public List<El> els = new LinkedList<El>();
        private List<El> enew = null, etr = null;
        private Indir<Resource> trev = null;
        private Tex trol;
        private long trtm = 0;

        @Resource.LayerName("foodev")
        public static class Event extends Resource.Layer {
            public final Color col;
            public final String nm;
            public final int sort;

            public Event(Resource res, Message buf) {
                res.super();
                int ver = buf.uint8();
                if (ver == 1) {
                    col = new Color(buf.uint8(), buf.uint8(), buf.uint8(), buf.uint8());
                    nm = buf.string();
                    sort = buf.int16();
                } else {
                    throw (new Resource.LoadException("unknown foodev version: " + ver, res));
                }
            }

            public void init() {
            }
        }

        public static class El {
            public final Indir<Resource> res;
            public double a;

            public El(Indir<Resource> res, double a) {
                this.res = res;
                this.a = a;
            }

            private Event ev = null;

            public Event ev() {
                if (ev == null)
                    ev = res.get().layer(Event.class);
                return (ev);
            }
        }

        public static final Comparator<El> dcmp = new Comparator<El>() {
            public int compare(El a, El b) {
                int c;
                if ((c = (a.ev().sort - b.ev().sort)) != 0)
                    return (c);
                return (a.ev().nm.compareTo(b.ev().nm));
            }
        };

        public FoodMeter() {
            super(frame.sz());
        }

        private BufferedImage mktrol(List<El> els, Indir<Resource> trev) {
            BufferedImage buf = TexI.mkbuf(sz.add(trmg.mul(2)));
            Coord marg2 = marg.add(trmg);
            Graphics g = buf.getGraphics();
            double x = 0;
            int w = sz.x - (marg.x * 2);
            for (El el : els) {
                int l = (int) Math.floor((x / cap) * w);
                int r = (int) Math.floor(((x += el.a) / cap) * w);
                if (el.res == trev) {
                    g.setColor(Utils.blendcol(el.ev().col, Color.WHITE, 0.5));
                    g.fillRect(marg2.x - (trmg.x / 2) + l, marg2.y - (trmg.y / 2), r - l + trmg.x, sz.y - (marg.y * 2) + trmg.y);
                }
            }
            imgblur(buf.getRaster(), trmg.x, trmg.y);
            return (buf);
        }

        private void drawels(GOut g, List<El> els, int alpha) {
            double x = 0;
            int w = sz.x - (marg.x * 2);
            for (El el : els) {
                int l = (int) Math.floor((x / cap) * w);
                int r = (int) Math.floor(((x += el.a) / cap) * w);
                try {
                    Color col = el.ev().col;
                    g.chcolor(new Color(col.getRed(), col.getGreen(), col.getBlue(), alpha));
                    g.frect(new Coord(marg.x + l, marg.y), new Coord(r - l, sz.y - (marg.y * 2)));
                } catch (Loading e) {
                }
            }
        }

        public void tick(double dt) {
            if (enew != null) {
                try {
                    Collections.sort(enew, dcmp);
                    els = enew;
                    rtip = null;
                } catch (Loading l) {
                }
                enew = null;
            }
            if (trev != null) {
                try {
                    Collections.sort(etr, dcmp);
                    trol = new TexI(mktrol(etr, trev));
                    trtm = System.currentTimeMillis();
                    trev = null;
                } catch (Loading l) {
                }
            }
        }

        public void draw(GOut g) {
            int d = (trtm > 0) ? ((int) (System.currentTimeMillis() - trtm)) : Integer.MAX_VALUE;
            g.chcolor(0, 0, 0, 255);
            g.frect(marg, sz.sub(marg.mul(2)));
            drawels(g, els, 255);
            if (d < 1000)
                drawels(g, etr, 255 - ((d * 255) / 1000));
            g.chcolor();
            g.image(frame, Coord.z);
            if (d < 2500) {
                GOut g2 = g.reclipl(trmg.inv(), sz.add(trmg.mul(2)));
                g2.chcolor(255, 255, 255, 255 - ((d * 255) / 2500));
                g2.image(trol, Coord.z);
            } else {
                trtm = 0;
            }
        }

        public void update(Object... args) {
            int n = 0;
            this.cap = (Float) args[n++];
            List<El> enew = new LinkedList<El>();
            while (n < args.length) {
                Indir<Resource> res = ui.sess.getres((Integer) args[n++]);
                double a = (Float) args[n++];
                enew.add(new El(res, a));
            }
            this.enew = enew;
        }

        public void trig(Indir<Resource> ev) {
            etr = (enew != null) ? enew : els;
            trev = ev;
        }

        private Tex rtip = null;

        public Object tooltip(Coord c, Widget prev) {
            if (rtip == null) {
                List<El> els = this.els;
                BufferedImage cur = null;
                double sum = 0.0;
                for (El el : els) {
                    Event ev = el.res.get().layer(Event.class);
                    Color col = Utils.blendcol(ev.col, Color.WHITE, 0.5);
                    BufferedImage ln = Text.render(String.format("%s: %s", ev.nm, Utils.odformat2(el.a, 2)), col).img;
                    Resource.Image icon = el.res.get().layer(Resource.imgc);
                    if (icon != null)
                        ln = ItemInfo.catimgsh(5, icon.img, ln);
                    cur = ItemInfo.catimgs(0, cur, ln);
                    sum += el.a;
                }
                cur = ItemInfo.catimgs(0, cur, Text.render(String.format("Total: %s/%s", Utils.odformat2(sum, 2), Utils.odformat(cap, 2))).img);
                rtip = new TexI(cur);
            }
            return (rtip);
        }
    }

    public static class GlutMeter extends Widget {
        public static final Tex frame = Resource.loadtex("gfx/hud/chr/glutm");
        public static final Coord marg = new Coord(5, 5);
        public Color fg, bg;
        public double glut, lglut, gmod;
        public String lbl;

        public GlutMeter() {
            super(frame.sz());
        }

        public void draw(GOut g) {
            Coord isz = sz.sub(marg.mul(2));
            g.chcolor(bg);
            g.frect(marg, isz);
            g.chcolor(fg);
            g.frect(marg, new Coord((int) Math.round(isz.x * (glut - Math.floor(glut))), isz.y));
            g.chcolor();
            g.image(frame, Coord.z);
        }

        public void update(Object... args) {
            int a = 0;
            this.glut = ((Number) args[a++]).doubleValue();
            this.lglut = ((Number) args[a++]).doubleValue();
            this.gmod = ((Number) args[a++]).doubleValue();
            this.lbl = (String) args[a++];
            this.bg = (Color) args[a++];
            this.fg = (Color) args[a++];
            rtip = null;
        }

        private Tex rtip = null;

        public Object tooltip(Coord c, Widget prev) {
            if (rtip == null) {
                rtip = RichText.render(String.format("%s: %d%%\nFood efficacy: %d%%", lbl, Math.round((lglut) * 100), Math.round(gmod * 100)), -1).tex();
            }
            return (rtip);
        }
    }

    public static class Constipations extends Listbox<Constipations.El> {
        public static final Color hilit = new Color(255, 255, 0, 48);
        public static final Text.Foundry elf = attrf;
        public static final Convolution tflt = new Hanning(1);
        public static final Color full = new Color(250, 230, 64), none = new Color(250, 19, 43);
        public final List<El> els = new ArrayList<El>();
        private Integer[] order = {};

        public static class El {
            public static final int h = elf.height() + 2;
            public final Indir<Resource> t;
            public double a;
            private Tex tt, at;
            private boolean hl;

            public El(Indir<Resource> t, double a) {
                this.t = t;
                this.a = a;
            }

            public void update(double a) {
                this.a = a;
                at = null;
            }

            public Tex tt() {
                if (tt == null) {
                    BufferedImage img = t.get().layer(Resource.imgc).img;
                    String nm = t.get().layer(Resource.tooltip).t;
                    Text rnm = elf.render(nm);
                    BufferedImage buf = TexI.mkbuf(new Coord(El.h + 5 + rnm.sz().x, h));
                    Graphics g = buf.getGraphics();
                    g.drawImage(convolvedown(img, new Coord(h, h), tflt), 0, 0, null);
                    g.drawImage(rnm.img, h + 5, ((h - rnm.sz().y) / 2) + 1, null);
                    g.dispose();
                    tt = new TexI(buf);
                }
                return (tt);
            }

            public Tex at() {
                if (at == null)
                    at = elf.render(String.format("%d%%", (int) Math.floor(a * 100)), Utils.blendcol(none, full, a)).tex();
                return (at);
            }
        }

        private WItem.ItemTip lasttip = null;

        public void draw(GOut g) {
            WItem.ItemTip tip = null;
            if (ui.lasttip instanceof WItem.ItemTip)
                tip = (WItem.ItemTip) ui.lasttip;
            if (tip != lasttip) {
                for (El el : els)
                    el.hl = false;
                FoodInfo finf = (tip == null) ? null : ItemInfo.find(FoodInfo.class, tip.item().info());
                if (finf != null) {
                    for (int i = 0; i < els.size(); i++) {
                        El el = els.get(i);
                        for (int o = 0; o < finf.types.length; o++) {
                            if (finf.types[o] == i) {
                                el.hl = true;
                                break;
                            }
                        }
                    }
                }
                lasttip = tip;
            }
            super.draw(g);
        }

        public static final Comparator<El> ecmp = new Comparator<El>() {
            public int compare(El a, El b) {
                if (a.a < b.a)
                    return (-1);
                else if (a.a > b.a)
                    return (1);
                return (0);
            }
        };

        public Constipations(int w, int h) {
            super(w, h, El.h);
        }

        protected void drawbg(GOut g) {
        }

        protected El listitem(int i) {
            return (els.get(order[i]));
        }

        protected int listitems() {
            return (order.length);
        }

        protected void drawitem(GOut g, El el, int idx) {
            g.chcolor(el.hl ? hilit : (((idx % 2) == 0) ? every : other));
            g.frect(Coord.z, g.sz);
            g.chcolor();
            try {
                g.image(el.tt(), Coord.z);
            } catch (Loading e) {
            }
            Tex at = el.at();
            g.image(at, new Coord(sz.x - at.sz().x - sb.sz.x, (El.h - at.sz().y) / 2));
        }

        private void order() {
            int n = els.size();
            order = new Integer[n];
            for (int i = 0; i < n; i++)
                order[i] = i;
            Arrays.sort(order, new Comparator<Integer>() {
                public int compare(Integer a, Integer b) {
                    return (ecmp.compare(els.get(a), els.get(b)));
                }
            });
        }

        public void update(Indir<Resource> t, double a) {
            prev:
            {
                for (Iterator<El> i = els.iterator(); i.hasNext(); ) {
                    El el = i.next();
                    if (el.t != t)
                        continue;
                    if (a == 1.0)
                        i.remove();
                    else
                        el.update(a);
                    break prev;
                }
                els.add(new El(t, a));
            }
            order();
        }

        public boolean mousedown(Coord c, int button) {
            return (false);
        }
    }

    public static final int attrw = FoodMeter.frame.sz().x - wbox.bisz().x;

    public class Attr extends Widget {
        public final String nm;
        public final Text rnm;
        public final Glob.CAttr attr;
        public final Tex img;
        public final Color bg;
        private double lvlt = 0.0;
        private Text ct;
        private int cbv, ccv;

        private Attr(Glob glob, String attr, Color bg) {
            super(new Coord(attrw, attrf.height() + 2));
            Resource res = Resource.local().loadwait("gfx/hud/chr/" + attr);
            this.nm = attr;
            this.img = res.layer(Resource.imgc).tex();
            String tooltip = res.layer(Resource.tooltip).t;
            this.rnm = attrf.render(tooltip);
            this.attr = glob.cattr.get(attr);
            this.bg = bg;
            this.attr.addObserver(new ChangeObserver(tooltip));
        }

        public void tick(double dt) {
            if ((attr.base != cbv) || (attr.comp != ccv)) {
                cbv = attr.base;
                ccv = attr.comp;
                Color c = Color.WHITE;
                if (ccv > cbv) {
                    c = buff;
                    tooltip = Text.render(String.format("%d + %d", cbv, ccv - cbv));
                } else if (ccv < cbv) {
                    c = debuff;
                    tooltip = Text.render(String.format("%d - %d", cbv, cbv - ccv));
                } else {
                    tooltip = null;
                }
                ct = attrf.render(Integer.toString(ccv), c);
            }
            if ((lvlt > 0.0) && ((lvlt -= dt) < 0))
                lvlt = 0.0;
        }

        public void draw(GOut g) {
            if (lvlt != 0.0)
                g.chcolor(Utils.blendcol(bg, new Color(128, 255, 128, 128), lvlt));
            else
                g.chcolor(bg);
            g.frect(Coord.z, sz);
            g.chcolor();
            Coord cn = new Coord(0, sz.y / 2);
            g.aimage(img, cn.add(5, 0), 0, 0.5);
            g.aimage(rnm.tex(), cn.add(img.sz().x + 10, 1), 0, 0.5);
            g.aimage(ct.tex(), cn.add(sz.x - 7, 1), 1, 0.5);
        }

        public void lvlup() {
            lvlt = 1.0;
        }

        private class ChangeObserver implements Observer {
            private String name;
            public ChangeObserver(String name) {
                this.name = name;
            }
            @Override
            public void update(Observable o, Object arg) {
                if (arg != null) {
                    int basediff = (Integer) arg;
                    if (basediff != 0) {
                        String msg = name + (basediff > 0 ? " +" + basediff : " " + basediff);
                        gameui().syslog.append(msg, Color.LIGHT_GRAY);
                    }
                }
            }
        }
    }

    public class SAttr extends Widget {
        public final String nm;
        public final Text rnm;
        public final Glob.CAttr attr;
        public final Tex img;
        public final Color bg;
        public int tbv, tcv, cost;
        private Text ct;
        private int cbv, ccv;

        private SAttr(Glob glob, String attr, Color bg) {
            super(new Coord(attrw, attrf.height() + 2));
            Resource res = Resource.local().loadwait("gfx/hud/chr/" + attr);
            this.nm = attr;
            this.img = res.layer(Resource.imgc).tex();
            this.rnm = attrf.render(res.layer(Resource.tooltip).t);
            this.attr = glob.cattr.get(attr);
            this.bg = bg;
            adda(new IButton("gfx/hud/buttons/add", "u", "d", null) {
                public void click() {
                    adj(1);
                }
            }, sz.x - 5, sz.y / 2, 1, 0.5);
            adda(new IButton("gfx/hud/buttons/sub", "u", "d", null) {
                public void click() {
                    adj(-1);
                }

                public boolean mousewheel(Coord c, int a) {
                    adj(-a);
                    return (true);
                }
            }, sz.x - 20, sz.y / 2, 1, 0.5);
        }

        public void tick(double dt) {
            if ((attr.base != cbv) || (attr.comp != ccv)) {
                cbv = attr.base;
                ccv = attr.comp;
                if (tbv <= cbv) {
                    tbv = cbv;
                    tcv = ccv;
                    updcost();
                }
                Color c = Color.WHITE;
                if (ccv > cbv) {
                    c = buff;
                    tooltip = Text.render(String.format("%d + %d", cbv, ccv - cbv));
                } else if (ccv < cbv) {
                    c = debuff;
                    tooltip = Text.render(String.format("%d - %d", cbv, cbv - ccv));
                } else {
                    tooltip = null;
                }
                if (tcv > ccv)
                    c = tbuff;
                ct = attrf.render(Integer.toString(tcv), c);
            }
        }

        public void draw(GOut g) {
            g.chcolor(bg);
            g.frect(Coord.z, sz);
            g.chcolor();
            super.draw(g);
            Coord cn = new Coord(0, sz.y / 2);
            g.aimage(img, cn.add(5, 0), 0, 0.5);
            g.aimage(rnm.tex(), cn.add(img.sz().x + 10, 1), 0, 0.5);
            g.aimage(ct.tex(), cn.add(sz.x - 40, 1), 1, 0.5);
        }

        private void updcost() {
            int cost = 100 * ((tbv + (tbv * tbv)) - (attr.base + (attr.base * attr.base))) / 2;
            scost += cost - this.cost;
            this.cost = cost;
        }

        public void adj(int a) {
            if (tbv + a < attr.base) a = attr.base - tbv;
            tbv += a;
            tcv += a;
            cbv = ccv = 0;
            updcost();
        }

        public void reset() {
            tbv = attr.base;
            tcv = attr.comp;
            cbv = ccv = 0;
            updcost();
        }

        public boolean mousewheel(Coord c, int a) {
            adj(-a);
            return (true);
        }
    }

    public static class RLabel extends Label {
        private Coord oc;

        public RLabel(Coord oc, String text) {
            super(text);
            this.oc = oc;
        }

        protected void added() {
            this.c = oc.add(-sz.x, 0);
        }

        public void settext(String text) {
            super.settext(text);
            this.c = oc.add(-sz.x, 0);
        }
    }

    public class ExpLabel extends RLabel {
        private int cexp;

        public ExpLabel(Coord oc) {
            super(oc, "0");
            setcolor(new Color(192, 192, 255));
        }

        public void draw(GOut g) {
            super.draw(g);
            if (exp != cexp)
                settext(Utils.thformat(cexp = exp));
        }
    }

    public class EncLabel extends RLabel {
        private int cenc;

        public EncLabel(Coord oc) {
            super(oc, "0");
            setcolor(new Color(255, 255, 192));
        }

        public void draw(GOut g) {
            super.draw(g);
            if (enc != cenc)
                settext(Utils.thformat(cenc = enc));
        }
    }

    public class StudyInfo extends Widget {
        public Widget study;
        public int texp, tw, tenc;
        private final Text.UText<?> texpt = new Text.UText<Integer>(Text.std) {
            public Integer value() {
                return (texp);
            }

            public String text(Integer v) {
                return (Utils.thformat(v));
            }
        };
        private final Text.UText<?> twt = new Text.UText<String>(Text.std) {
            public String value() {
                return (tw + "/" + ui.sess.glob.cattr.get("int").comp);
            }
        };
        private final Text.UText<?> tenct = new Text.UText<Integer>(Text.std) {
            public Integer value() {
                return (tenc);
            }

            public String text(Integer v) {
                return (Integer.toString(tenc));
            }
        };

        private StudyInfo(Coord sz, Widget study) {
            super(sz);
            this.study = study;
            add(new Label("Attention:"), 2, 2);
            add(new Label("Experience cost:"), 2, 32);
            add(new Label("Learning points:"), 2, sz.y - 32);
        }

        private void upd() {
            int texp = 0, tw = 0, tenc = 0;
            for (GItem item : study.children(GItem.class)) {
                try {
                    Curiosity ci = ItemInfo.find(Curiosity.class, item.info());
                    if (ci != null) {
                        texp += ci.exp;
                        tw += ci.mw;
                        tenc += ci.enc;
                    }
                } catch (Loading l) {
                }
            }
            this.texp = texp;
            this.tw = tw;
            this.tenc = tenc;
        }

        public void draw(GOut g) {
            upd();
            super.draw(g);
            g.chcolor(255, 192, 255, 255);
            g.aimage(twt.get().tex(), new Coord(sz.x - 4, 17), 1.0, 0.0);
            g.chcolor(255, 255, 192, 255);
            g.aimage(tenct.get().tex(), new Coord(sz.x - 4, 47), 1.0, 0.0);
            g.chcolor(192, 192, 255, 255);
            g.aimage(texpt.get().tex(), sz.add(-4, -15), 1.0, 0.0);
        }
    }

    public static class LoadingTextBox extends RichTextBox {
        private Indir<String> text = null;

        public LoadingTextBox(Coord sz, String text, RichText.Foundry fnd) {
            super(sz, text, fnd);
        }

        public LoadingTextBox(Coord sz, String text, Object... attrs) {
            super(sz, text, attrs);
        }

        public void settext(Indir<String> text) {
            this.text = text;
        }

        public void draw(GOut g) {
            if (text != null) {
                try {
                    settext(text.get());
                    text = null;
                } catch (Loading l) {
                }
            }
            super.draw(g);
        }
    }

    public static final PUtils.Convolution iconfilter = new PUtils.Lanczos(3);

    public class Skill {
        public final String nm;
        public final Indir<Resource> res;
        public final int cost;
        private String sortkey;
        private Tex small;
        private final Text.UText<?> rnm = new Text.UText<String>(attrf) {
            public String value() {
                try {
                    return (res.get().layer(Resource.tooltip).t);
                } catch (Loading l) {
                    return ("...");
                }
            }
        };

        private Skill(String nm, Indir<Resource> res, int cost) {
            this.nm = nm;
            this.res = res;
            this.cost = cost;
            this.sortkey = nm;
        }

        public String rendertext() {
            StringBuilder buf = new StringBuilder();
            Resource res = this.res.get();
            buf.append("$img[" + res.name + "]\n\n");
            buf.append("$b{$font[serif,16]{" + res.layer(Resource.tooltip).t + "}}\n\n\n");
            buf.append("Cost: " + cost + "\n\n");
            buf.append(res.layer(Resource.pagina).text);
            return (buf.toString());
        }
    }

    public class Experience {
        public final Indir<Resource> res;
        public final int mtime, score;
        private String sortkey = "\uffff";
        private Tex small;
        private final Text.UText<?> rnm = new Text.UText<String>(attrf) {
            public String value() {
                try {
                    return (res.get().layer(Resource.tooltip).t);
                } catch (Loading l) {
                    return ("...");
                }
            }
        };

        private Experience(Indir<Resource> res, int mtime, int score) {
            this.res = res;
            this.mtime = mtime;
            this.score = score;
        }

        public String rendertext() {
            StringBuilder buf = new StringBuilder();
            Resource res = this.res.get();
            buf.append("$img[" + res.name + "]\n\n");
            buf.append("$b{$font[serif,16]{" + res.layer(Resource.tooltip).t + "}}\n\n\n");
            if (score > 0)
                buf.append("Experience points: " + Utils.thformat(score) + "\n\n");
            buf.append(res.layer(Resource.pagina).text);
            return (buf.toString());
        }
    }

    public static class Wound {
        public final int id;
        public Indir<Resource> res;
        public Object qdata;
        private String sortkey = "\uffff";
        private Tex small;
        private final Text.UText<?> rnm = new Text.UText<String>(attrf) {
            public String value() {
                try {
                    return (res.get().layer(Resource.tooltip).t);
                } catch (Loading l) {
                    return ("...");
                }
            }
        };
        private final Text.UText<?> rqd = new Text.UText<Object>(attrf) {
            public Object value() {
                return (qdata);
            }
        };

        private Wound(int id, Indir<Resource> res, Object qdata) {
            this.id = id;
            this.res = res;
            this.qdata = qdata;
        }

        public static class Box extends LoadingTextBox implements Info {
            public final int id;
            public final Indir<Resource> res;

            public Box(int id, Indir<Resource> res) {
                super(Coord.z, "", ifnd);
                bg = null;
                this.id = id;
                this.res = res;
                settext(new Indir<String>() {
                    public String get() {
                        return (rendertext());
                    }
                });
            }

            protected void added() {
                resize(parent.sz);
            }

            public String rendertext() {
                StringBuilder buf = new StringBuilder();
                Resource res = this.res.get();
                buf.append("$img[" + res.name + "]\n\n");
                buf.append("$b{$font[serif,16]{" + res.layer(Resource.tooltip).t + "}}\n\n\n");
                buf.append(res.layer(Resource.pagina).text);
                return (buf.toString());
            }

            public int woundid() {
                return (id);
            }
        }

        @RName("wound")
        public static class $wound implements Factory {
            public Widget create(Widget parent, Object[] args) {
                int id = (Integer) args[0];
                Indir<Resource> res = parent.ui.sess.getres((Integer) args[1]);
                return (new Box(id, res));
            }
        }

        public interface Info {
            public int woundid();
        }
    }

    public class SkillList extends Listbox<Skill> {
        public Skill[] skills = new Skill[0];
        public boolean dav = false;
        private boolean loading = false;
        private final Comparator<Skill> skcomp = new Comparator<Skill>() {
            public int compare(Skill a, Skill b) {
                return (a.sortkey.compareTo(b.sortkey));
            }
        };

        public SkillList(int w, int h) {
            super(w, h, attrf.height() + 2);
        }

        public void tick(double dt) {
            if (loading) {
                loading = false;
                for (Skill sk : skills) {
                    try {
                        sk.sortkey = sk.res.get().layer(Resource.tooltip).t;
                    } catch (Loading l) {
                        sk.sortkey = sk.nm;
                        loading = true;
                    }
                }
                Arrays.sort(skills, skcomp);
            }
        }

        protected Skill listitem(int idx) {
            return (skills[idx]);
        }

        protected int listitems() {
            return (skills.length);
        }

        protected void drawbg(GOut g) {
        }

        protected void drawitem(GOut g, Skill sk, int idx) {
            g.chcolor((idx % 2 == 0) ? every : other);
            g.frect(Coord.z, g.sz);
            g.chcolor();
            try {
                if (sk.small == null)
                    sk.small = new TexI(PUtils.convolvedown(sk.res.get().layer(Resource.imgc).img, new Coord(itemh, itemh), iconfilter));
                g.image(sk.small, Coord.z);
            } catch (Loading e) {
                g.image(WItem.missing.layer(Resource.imgc).tex(), Coord.z, new Coord(itemh, itemh));
            }
            if (dav && (sk.cost > exp))
                g.chcolor(255, 192, 192, 255);
            g.aimage(sk.rnm.get().tex(), new Coord(itemh + 5, itemh / 2), 0, 0.5);
        }

        public void pop(Collection<Skill> nsk) {
            Skill[] skills = nsk.toArray(new Skill[0]);
            sb.val = 0;
            sb.max = skills.length - h;
            Skill psel = sel;
            sel = null;
            this.skills = skills;
            if (psel != null) {
                for (Skill sk : skills) {
                    if (sk.nm.equals(psel.nm)) {
                        sel = sk;
                        break;
                    }
                }
            }
            loading = true;
        }
    }

    public static class ExperienceList extends Listbox<Experience> {
        public Experience[] exps = new Experience[0];
        private boolean loading = false;
        private final Comparator<Experience> comp = new Comparator<Experience>() {
            public int compare(Experience a, Experience b) {
                return (a.sortkey.compareTo(b.sortkey));
            }
        };

        public ExperienceList(int w, int h) {
            super(w, h, attrf.height() + 2);
        }

        public void tick(double dt) {
            if (loading) {
                loading = false;
                for (Experience exp : exps) {
                    try {
                        exp.sortkey = exp.res.get().layer(Resource.tooltip).t;
                    } catch (Loading l) {
                        exp.sortkey = "\uffff";
                        loading = true;
                    }
                }
                Arrays.sort(exps, comp);
            }
        }

        protected Experience listitem(int idx) {
            return (exps[idx]);
        }

        protected int listitems() {
            return (exps.length);
        }

        protected void drawbg(GOut g) {
        }

        protected void drawitem(GOut g, Experience exp, int idx) {
            g.chcolor((idx % 2 == 0) ? every : other);
            g.frect(Coord.z, g.sz);
            g.chcolor();
            try {
                if (exp.small == null)
                    exp.small = new TexI(PUtils.convolvedown(exp.res.get().layer(Resource.imgc).img, new Coord(itemh, itemh), iconfilter));
                g.image(exp.small, Coord.z);
            } catch (Loading e) {
                g.image(WItem.missing.layer(Resource.imgc).tex(), Coord.z, new Coord(itemh, itemh));
            }
            g.aimage(exp.rnm.get().tex(), new Coord(itemh + 5, itemh / 2), 0, 0.5);
        }

        public void pop(Collection<Experience> nl) {
            Experience[] exps = nl.toArray(new Experience[0]);
            sb.val = 0;
            sb.max = exps.length - h;
            sel = null;
            this.exps = exps;
            loading = true;
        }
    }

    public class WoundList extends Listbox<Wound> implements DTarget {
        public List<Wound> wounds = new ArrayList<Wound>();
        private boolean loading = false;
        private final Comparator<Wound> wcomp = new Comparator<Wound>() {
            public int compare(Wound a, Wound b) {
                return (a.sortkey.compareTo(b.sortkey));
            }
        };

        private WoundList(int w, int h) {
            super(w, h, attrf.height() + 2);
        }

        public void tick(double dt) {
            if (loading) {
                loading = false;
                for (Wound w : wounds) {
                    try {
                        w.sortkey = w.res.get().layer(Resource.tooltip).t;
                    } catch (Loading l) {
                        w.sortkey = "\uffff";
                        loading = true;
                    }
                }
                Collections.sort(wounds, wcomp);
            }
        }

        protected Wound listitem(int idx) {
            return (wounds.get(idx));
        }

        protected int listitems() {
            return (wounds.size());
        }

        protected void drawbg(GOut g) {
        }

        protected void drawitem(GOut g, Wound w, int idx) {
            if ((wound != null) && (wound.woundid() == w.id))
                drawsel(g);
            g.chcolor((idx % 2 == 0) ? every : other);
            g.frect(Coord.z, g.sz);
            g.chcolor();
            try {
                if (w.small == null)
                    w.small = new TexI(PUtils.convolvedown(w.res.get().layer(Resource.imgc).img, new Coord(itemh, itemh), iconfilter));
                g.image(w.small, Coord.z);
            } catch (Loading e) {
                g.image(WItem.missing.layer(Resource.imgc).tex(), Coord.z, new Coord(itemh, itemh));
            }
            g.aimage(w.rnm.get().tex(), new Coord(itemh + 5, itemh / 2), 0, 0.5);
            Text qd = w.rqd.get();
            if (qd != null)
                g.aimage(qd.tex(), new Coord(sz.x - 15, itemh / 2), 1.0, 0.5);
        }

        protected void itemclick(Wound item, int button) {
            if (button == 3) {
                CharWnd.this.wdgmsg("wclick", item.id, button, ui.modflags());
            } else {
                super.itemclick(item, button);
            }
        }

        public boolean drop(Coord cc, Coord ul) {
            return (false);
        }

        public boolean iteminteract(Coord cc, Coord ul) {
            Wound w = itemat(cc);
            if (w != null)
                CharWnd.this.wdgmsg("wiact", w.id, ui.modflags());
            return (true);
        }

        public void change(Wound w) {
            if (w == null)
                CharWnd.this.wdgmsg("wsel", (Object) null);
            else
                CharWnd.this.wdgmsg("wsel", w.id);
        }

        public Wound get(int id) {
            for (Wound w : wounds) {
                if (w.id == id)
                    return (w);
            }
            return (null);
        }

        public void add(Wound w) {
            wounds.add(w);
        }

        public Wound remove(int id) {
            for (Iterator<Wound> i = wounds.iterator(); i.hasNext(); ) {
                Wound w = i.next();
                if (w.id == id) {
                    i.remove();
                    return (w);
                }
            }
            return (null);
        }
    }

    @RName("chr")
    public static class $_ implements Factory {
        public Widget create(Widget parent, Object[] args) {
            return (new CharWnd(parent.ui.sess.glob));
        }
    }

    public CharWnd(Glob glob) {
        super(new Coord(300, 290), "Character Sheet");

        final Tabs tabs = new Tabs(new Coord(15, 10), Coord.z, this);
        Tabs.Tab battr;
        {
            int x = 5, y = 0;

            battr = tabs.add();
            battr.add(new Img(catf.render("Base Attributes").tex()), new Coord(x - 2, y));
            y += 35;
            base = new ArrayList<Attr>();
            Attr aw;
            base.add(aw = battr.add(new Attr(glob, "str", every), wbox.btloff().add(x, y)));
            y += aw.sz.y;
            base.add(aw = battr.add(new Attr(glob, "agi", other), wbox.btloff().add(x, y)));
            y += aw.sz.y;
            base.add(aw = battr.add(new Attr(glob, "int", every), wbox.btloff().add(x, y)));
            y += aw.sz.y;
            base.add(aw = battr.add(new Attr(glob, "con", other), wbox.btloff().add(x, y)));
            y += aw.sz.y;
            base.add(aw = battr.add(new Attr(glob, "prc", every), wbox.btloff().add(x, y)));
            y += aw.sz.y;
            base.add(aw = battr.add(new Attr(glob, "csm", other), wbox.btloff().add(x, y)));
            y += aw.sz.y;
            base.add(aw = battr.add(new Attr(glob, "dex", every), wbox.btloff().add(x, y)));
            y += aw.sz.y;
            base.add(aw = battr.add(new Attr(glob, "psy", other), wbox.btloff().add(x, y)));
            y += aw.sz.y;
            Frame.around(battr, base);
            y += 24;
            battr.add(new Img(catf.render("Food Event Points").tex()), new Coord(x - 2, y));
            y += 35;
            feps = battr.add(new FoodMeter(), new Coord(x, y));

            x = 260;
            y = 0;
            battr.add(new Img(catf.render("Food Satiations").tex()), new Coord(x - 2, y));
            y += 35;
            cons = battr.add(new Constipations(attrw, base.size()), wbox.btloff().add(x, y));
            y += cons.sz.y;
            Frame.around(battr, Collections.singletonList(cons));
            y += 24;
            battr.add(new Img(catf.render("Hunger Level").tex()), new Coord(x - 2, y));
            y += 35;
            glut = battr.add(new GlutMeter(), new Coord(x, y));
        }

        {
            int x = 5, y = 0;

            sattr = tabs.addStudy();
            sattr.add(new Img(catf.render("Abilities").tex()), new Coord(x - 2, y));
            y += 35;
            skill = new ArrayList<SAttr>();
            SAttr aw;
            skill.add(aw = sattr.add(new SAttr(glob, "unarmed", every), wbox.btloff().add(x, y)));
            y += aw.sz.y;
            skill.add(aw = sattr.add(new SAttr(glob, "melee", other), wbox.btloff().add(x, y)));
            y += aw.sz.y;
            skill.add(aw = sattr.add(new SAttr(glob, "ranged", every), wbox.btloff().add(x, y)));
            y += aw.sz.y;
            skill.add(aw = sattr.add(new SAttr(glob, "explore", other), wbox.btloff().add(x, y)));
            y += aw.sz.y;
            skill.add(aw = sattr.add(new SAttr(glob, "stealth", every), wbox.btloff().add(x, y)));
            y += aw.sz.y;
            skill.add(aw = sattr.add(new SAttr(glob, "sewing", other), wbox.btloff().add(x, y)));
            y += aw.sz.y;
            skill.add(aw = sattr.add(new SAttr(glob, "smithing", every), wbox.btloff().add(x, y)));
            y += aw.sz.y;
            skill.add(aw = sattr.add(new SAttr(glob, "carpentry", other), wbox.btloff().add(x, y)));
            y += aw.sz.y;
            skill.add(aw = sattr.add(new SAttr(glob, "cooking", every), wbox.btloff().add(x, y)));
            y += aw.sz.y;
            skill.add(aw = sattr.add(new SAttr(glob, "farming", other), wbox.btloff().add(x, y)));
            y += aw.sz.y;
            skill.add(aw = sattr.add(new SAttr(glob, "survive", every), wbox.btloff().add(x, y)));
            y += aw.sz.y;
            Frame.around(sattr, skill);

            x = 260;
            y = 0;
            sattr.add(new Img(catf.render("Study Report").tex()), new Coord(x - 2, y));
            y += 35;
            y += 151;
            int rx = x + attrw - 10;
            Frame.around(sattr, Area.sized(new Coord(x, y).add(wbox.btloff()), new Coord(attrw, 80)));
            sattr.add(new Label("Experience points:"), new Coord(x + 15, y + 10));
            sattr.add(new EncLabel(new Coord(rx, y + 10)));
            sattr.add(new Label("Learning points:"), new Coord(x + 15, y + 25));
            sattr.add(new ExpLabel(new Coord(rx, y + 25)));
            sattr.add(new Label("Learning cost:"), new Coord(x + 15, y + 40));
            sattr.add(new RLabel(new Coord(rx, y + 40), "0") {
                int cc;

                public void draw(GOut g) {
                    if (cc > exp)
                        g.chcolor(debuff);
                    super.draw(g);
                    if (cc != scost)
                        settext(Utils.thformat(cc = scost));
                }
            });
            sattr.add(new Button(75, "Buy") {
                public void click() {
                    ArrayList<Object> args = new ArrayList<Object>();
                    for (SAttr attr : skill) {
                        if (attr.tbv > attr.attr.base) {
                            args.add(attr.attr.nm);
                            args.add(attr.tbv);
                        }
                    }
                    CharWnd.this.wdgmsg("sattr", args.toArray(new Object[0]));
                }
            }, new Coord(rx - 75, y + 55));
            sattr.add(new Button(75, "Reset") {
                public void click() {
                    for (SAttr attr : skill)
                        attr.reset();
                }
            }, new Coord(rx - 160, y + 55));
        }

        Tabs.Tab skills;
        {
            int x = 5, y = 0;

            skills = tabs.add();
            skills.add(new Img(catf.render("Lore & Skills").tex()), new Coord(x - 2, y));
            y += 35;
            final LoadingTextBox info = skills.add(new LoadingTextBox(new Coord(attrw, 260), "", ifnd), new Coord(x, y).add(wbox.btloff()));
            info.bg = new Color(0, 0, 0, 128);
            Frame.around(skills, Collections.singletonList(info));

            x = 260;
            y = 0;
            skills.add(new Img(catf.render("Entries").tex()), new Coord(x - 2, y));
            y += 35;
            Tabs lists = new Tabs(new Coord(x, y), new Coord(attrw + wbox.bisz().x, 0), skills);
            Tabs.Tab nsk = lists.add();
            {
                this.nsk = nsk.add(new SkillList(lists.sz.x - wbox.bisz().x, 7) {
                    public void change(final Skill sk) {
                        Skill p = sel;
                        super.change(sk);
                        CharWnd.this.csk.sel = null;
                        CharWnd.this.exps.sel = null;
                        if (sk != null)
                            info.settext(new Indir<String>() {
                                public String get() {
                                    return (sk.rendertext());
                                }
                            });
                        else if (p != null)
                            info.settext("");
                    }
                }, wbox.btloff());
                this.nsk.dav = true;
                y = Frame.around(nsk, Collections.singletonList(this.nsk)).sz.y + 5;
                int rx = attrw - 10;
                Frame.around(nsk, Area.sized(new Coord(0, y).add(wbox.btloff()), new Coord(attrw, 69)));
                nsk.add(new Label("Learning points:"), new Coord(15, y + 10));
                nsk.add(new ExpLabel(new Coord(rx, y + 10)));
                nsk.add(new Label("Cost:"), new Coord(15, y + 25));
                nsk.add(new RLabel(new Coord(rx, y + 25), "N/A") {
                    Integer cc = null;

                    public void draw(GOut g) {
                        if ((cc != null) && (cc > exp))
                            g.chcolor(debuff);
                        super.draw(g);
                        Skill sel = CharWnd.this.nsk.sel;
                        if ((sel == null) && (cc != null)) {
                            settext("N/A");
                            cc = null;
                        } else if ((sel != null) && ((cc == null) || (cc != sel.cost))) {
                            settext(Utils.thformat(cc = sel.cost));
                        }
                    }
                });
                nsk.add(new Button(75, "Buy") {
                    public void click() {
                        if (CharWnd.this.nsk.sel != null)
                            CharWnd.this.wdgmsg("buy", CharWnd.this.nsk.sel.nm);
                    }
                }, new Coord(rx - 75, y + 44));
            }
            Tabs.Tab csk = lists.add();
            {
                this.csk = csk.add(new SkillList(lists.sz.x - wbox.bisz().x, 11) {
                    public void change(final Skill sk) {
                        Skill p = sel;
                        super.change(sk);
                        CharWnd.this.nsk.sel = null;
                        CharWnd.this.exps.sel = null;
                        if (sk != null)
                            info.settext(new Indir<String>() {
                                public String get() {
                                    return (sk.rendertext());
                                }
                            });
                        else if (p != null)
                            info.settext("");
                    }
                }, wbox.btloff());
                Frame.around(csk, Collections.singletonList(this.csk));
            }
            Tabs.Tab exps = lists.add();
            {
                this.exps = exps.add(new ExperienceList(lists.sz.x - wbox.bisz().x, 11) {
                    public void change(final Experience exp) {
                        Experience p = sel;
                        super.change(exp);
                        CharWnd.this.nsk.sel = null;
                        CharWnd.this.csk.sel = null;
                        if (exp != null)
                            info.settext(new Indir<String>() {
                                public String get() {
                                    return (exp.rendertext());
                                }
                            });
                        else if (p != null)
                            info.settext("");
                    }
                }, wbox.btloff());
                Frame.around(exps, Collections.singletonList(this.exps));
            }
            lists.pack();
            int bw = (lists.sz.x + 5) / 3;
            x = lists.c.x;
            y = lists.c.y + lists.sz.y + 5;
            skills.add(lists.new TabButton(bw - 5, "Available", nsk), new Coord(x, y));
            skills.add(lists.new TabButton(bw - 5, "Known", csk), new Coord(x + bw, y));
            skills.add(lists.new TabButton(bw - 5, "Lore", exps), new Coord(x + bw * 2, y));
        }

        Tabs.Tab wounds;
        {
            wounds = tabs.add();
            wounds.add(new Img(catf.render("Health & Wounds").tex()), new Coord(0, 0));
            this.wounds = wounds.add(new WoundList(attrw, 12), new Coord(260, 35).add(wbox.btloff()));
            Frame.around(wounds, Collections.singletonList(this.wounds));
            woundbox = wounds.add(new Widget(new Coord(attrw, this.wounds.sz.y)) {
                public void draw(GOut g) {
                    g.chcolor(0, 0, 0, 128);
                    g.frect(Coord.z, sz);
                    g.chcolor();
                    super.draw(g);
                }

                public void cdestroy(Widget w) {
                    if (w == wound)
                        wound = null;
                }
            }, new Coord(5, 35).add(wbox.btloff()));
            Frame.around(wounds, Collections.singletonList(woundbox));
        }

        {
            Widget prev;

            class TB extends IButton {
                final Tabs.Tab tab;

                TB(String nm, Tabs.Tab tab) {
                    super(Resource.loadimg("gfx/hud/chr/" + nm + "u"), Resource.loadimg("gfx/hud/chr/" + nm + "d"));
                    this.tab = tab;
                }

                public void click() {
                    tabs.showtab(tab);
                }

                protected void depress() {
                    Audio.play(Button.lbtdown.stream());
                }

                protected void unpress() {
                    Audio.play(Button.lbtup.stream());
                }
            }

            tabs.pack();

            fgt = tabs.add();

            prev = add(new TB("battr", battr), new Coord(tabs.c.x + 5, tabs.c.y + tabs.sz.y + 10));
            prev.settip("Base Attributes");
            prev = add(new TB("sattr", sattr), new Coord(prev.c.x + prev.sz.x + 10, prev.c.y));
            prev.settip("Abilities");
            prev = add(new TB("skill", skills), new Coord(prev.c.x + prev.sz.x + 10, prev.c.y));
            prev.settip("Lore & Skills");
            prev = add(new TB("fgt", fgt), new Coord(prev.c.x + prev.sz.x + 7, prev.c.y));
            prev.settip("Martial Arts & Combat Schools");
            prev = add(new TB("wound", wounds), new Coord(prev.c.x + prev.sz.x + 10, prev.c.y));
            prev.settip("Health & Wounds");
        }

        resize(contentsz().add(15, 10));
    }

    public void addchild(Widget child, Object... args) {
        String place = (args[0] instanceof String) ? (((String) args[0]).intern()) : null;
        if (place == "study") {
            sattr.add(child, new Coord(260, 35).add(wbox.btloff()));
            Frame.around(sattr, Collections.singletonList(child));
            Widget inf = sattr.add(new StudyInfo(new Coord(attrw - 150, child.sz.y), child), new Coord(260 + 150, child.c.y).add(wbox.btloff().x, 0));
            sattr.add(new CheckBox("Lock") {
                {
                    a = Config.studylock;
                }

                public void set(boolean val) {
                    Utils.setprefb("studylock", val);
                    Config.studylock = val;
                    a = val;
                }
            }, new Coord(412, 10));
            sattr.add(new CheckBox("Auto") {
                {
                    a = Config.autostudy;
                }

                public void set(boolean val) {
                    Utils.setprefb("autostudy", val);
                    Config.autostudy = val;
                    a = val;
                }
            }, new Coord(460, 10));
            Frame.around(sattr, Collections.singletonList(inf));
        } else if (place == "fmg") {
            fgt.add(child, 0, 0);
        } else if (place == "wound") {
            this.wound = (Wound.Info) child;
            woundbox.add(child, Coord.z);
        } else {
            super.addchild(child, args);
        }
    }

    private void decsklist(Collection<Skill> buf, Object[] args, int a) {
        while (a < args.length) {
            String nm = (String) args[a++];
            Indir<Resource> res = ui.sess.getres((Integer) args[a++]);
            int cost = ((Number) args[a++]).intValue();
            buf.add(new Skill(nm, res, cost));
        }
    }

    private void decexplist(Collection<Experience> buf, Object[] args, int a) {
        while (a < args.length) {
            Indir<Resource> res = ui.sess.getres((Integer) args[a++]);
            int mtime = ((Number) args[a++]).intValue();
            int score = ((Number) args[a++]).intValue();
            buf.add(new Experience(res, mtime, score));
        }
    }

    public void uimsg(String nm, Object... args) {
        if (nm == "exp") {
            exp = ((Number) args[0]).intValue();
        } else if (nm == "enc") {
            enc = ((Number) args[0]).intValue();
        } else if (nm == "food") {
            feps.update(args);
        } else if (nm == "glut") {
            glut.update(args);
        } else if (nm == "glut") {
        } else if (nm == "ftrig") {
            feps.trig(ui.sess.getres((Integer) args[0]));
        } else if (nm == "lvl") {
            for (Attr aw : base) {
                if (aw.nm.equals(args[0]))
                    aw.lvlup();
            }
        } else if (nm == "const") {
            int a = 0;
            while (a < args.length) {
                Indir<Resource> t = ui.sess.getres((Integer) args[a++]);
                double m = ((Number) args[a++]).doubleValue();
                cons.update(t, m);
            }
        } else if (nm == "csk") {
        /* One *could* argue that rmessages should have some
         * built-in fragmentation scheme. ^^ */
            boolean rst = ((Integer) args[0]) != 0;
            Collection<Skill> buf = rst ? new ArrayList<Skill>() : new ArrayList<Skill>(Arrays.asList(csk.skills));
            decsklist(buf, args, 1);
            csk.pop(buf);
        } else if (nm == "nsk") {
            boolean rst = ((Integer) args[0]) != 0;
            Collection<Skill> buf = rst ? new ArrayList<Skill>() : new ArrayList<Skill>(Arrays.asList(nsk.skills));
            decsklist(buf, args, 1);
            nsk.pop(buf);
        } else if (nm == "exps") {
            boolean rst = ((Integer) args[0]) != 0;
            Collection<Experience> buf = rst ? new ArrayList<Experience>() : new ArrayList<Experience>(Arrays.asList(exps.exps));
            decexplist(buf, args, 1);
            exps.pop(buf);
        } else if (nm == "wounds") {
            for (int i = 0; i < args.length; i += 3) {
                int id = (Integer) args[i];
                Indir<Resource> res = (args[i + 1] == null) ? null : ui.sess.getres((Integer) args[i + 1]);
                Object qdata = args[i + 2];
                if (res != null) {
                    Wound w = wounds.get(id);
                    if (w == null) {
                        wounds.add(new Wound(id, res, qdata));
                    } else {
                        w.res = res;
                        w.qdata = qdata;
                    }
                    wounds.loading = true;
                } else {
                    wounds.remove(id);
                }
            }
        } else {
            super.uimsg(nm, args);
        }
    }
}
