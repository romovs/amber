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

import java.awt.*;
import java.util.*;

public class Glob {
    public long time, epoch = System.currentTimeMillis();
    public Astronomy ast;
    public OCache oc = new OCache(this);
    public MCache map;
    public Session sess;
    public Party party;
    public Set<Pagina> paginae = new HashSet<Pagina>();
    public int pagseq = 0;
    public Map<Resource.Named, Pagina> pmap = new WeakHashMap<Resource.Named, Pagina>();
    public Map<String, CAttr> cattr = new HashMap<String, CAttr>();
    public Color lightamb = null, lightdif = null, lightspc = null;
    public Color olightamb = null, olightdif = null, olightspc = null;
    public Color tlightamb = null, tlightdif = null, tlightspc = null;
    public static Color dlightamb = new Color(200, 200, 200);
    public static Color dlightspc = new Color(255, 255, 255);
    public double lightang = 0.0, lightelev = 0.0;
    public double olightang = 0.0, olightelev = 0.0;
    public double tlightang = 0.0, tlightelev = 0.0;
    public long lchange = -1;
    public Indir<Resource> sky1 = null, sky2 = null;
    public double skyblend = 0.0;
    private Map<Indir<Resource>, Object> wmap = new HashMap<Indir<Resource>, Object>();
    public static TimersThread timersThread;
    public String servertime;
    public Tex servertimetex;

    static {
        timersThread = new TimersThread();
        timersThread.start();
    }

    public Glob(Session sess) {
        this.sess = sess;
        map = new MCache(sess);
        party = new Party(this);
    }

    @Resource.PublishedCode(name = "wtr")
    public static interface Weather {
        public void gsetup(RenderList rl);

        public void update(Object... args);

        public boolean tick(int dt);
    }

    public static class CAttr extends Observable {
        public static final Text.Foundry capval = new Text.Foundry(Text.sans.deriveFont(Font.BOLD), Config.fontsizeglobal * 12 / 10).aa(true);
        String nm;
        int base, comp;
        public Tex comptex;

        public CAttr(String nm, int base, int comp) {
            this.nm = nm.intern();
            this.base = base;
            this.comp = comp;
            this.comptex = Text.renderstroked(comp + "", Color.WHITE, Color.BLACK, capval).tex();
        }

        public void update(int base, int comp) {
            if ((base == this.base) && (comp == this.comp))
                return;
            this.base = base;
            this.comp = comp;
            setChanged();
            notifyObservers(null);
            this.comptex = Text.renderstroked(comp + "", Color.WHITE, Color.BLACK, capval).tex();
        }
    }

    public static class Pagina implements java.io.Serializable {
        public final Indir<Resource> res;
        public State st;
        public int meter, dtime;
        public long gettime;
        public Image img;
        public int newp;
        public long fstart; /* XXX: ABUSAN!!! */

        public interface Image {
            public Tex tex();
        }

        public static enum State {
            ENABLED, DISABLED {
                public Image img(final Pagina pag) {
                    return (new Image() {
                        private Tex c = null;

                        public Tex tex() {
                            if (pag.res() == null)
                                return (null);
                            if (c == null)
                                c = new TexI(PUtils.monochromize(pag.res().layer(Resource.imgc).img, Color.LIGHT_GRAY));
                            return (c);
                        }
                    });
                }
            };

            public Image img(final Pagina pag) {
                return (new Image() {
                    public Tex tex() {
                        if (pag.res() == null)
                            return (null);
                        return (pag.res().layer(Resource.imgc).tex());
                    }
                });
            }
        }

        public Pagina(Indir<Resource> res) {
            this.res = res;
            state(State.ENABLED);
        }

        public Resource res() {
            return (res.get());
        }

        public Resource.AButton act() {
            return (res().layer(Resource.action));
        }

        public void state(State st) {
            this.st = st;
            this.img = st.img(this);
        }
    }

    private static Color colstep(Color o, Color t, double a) {
        int or = o.getRed(), og = o.getGreen(), ob = o.getBlue(), oa = o.getAlpha();
        int tr = t.getRed(), tg = t.getGreen(), tb = t.getBlue(), ta = t.getAlpha();
        return (new Color(or + (int) ((tr - or) * a),
                og + (int) ((tg - og) * a),
                ob + (int) ((tb - ob) * a),
                oa + (int) ((ta - oa) * a)));
    }

    private void ticklight(int dt) {
        if (lchange >= 0) {
            lchange += dt;
            if (lchange > 2000) {
                lchange = -1;
                lightamb = tlightamb;
                lightdif = tlightdif;
                lightspc = tlightspc;
                lightang = tlightang;
                lightelev = tlightelev;
            } else {
                double a = lchange / 2000.0;
                lightamb = colstep(olightamb, tlightamb, a);
                lightdif = colstep(olightdif, tlightdif, a);
                lightspc = colstep(olightspc, tlightspc, a);
                lightang = olightang + a * Utils.cangle(tlightang - olightang);
                lightelev = olightelev + a * Utils.cangle(tlightelev - olightelev);
            }
        }
    }

    private long lastctick = 0;

    public void ctick() {
        long now = System.currentTimeMillis();
        int dt;
        if (lastctick == 0)
            dt = 0;
        else
            dt = (int) (now - lastctick);
        dt = Math.max(dt, 0);

        synchronized (this) {
            ticklight(dt);
            for (Object o : wmap.values()) {
                if (o instanceof Weather)
                    ((Weather) o).tick(dt);
            }
        }

        oc.ctick(dt);
        map.ctick(dt);

        lastctick = now;
    }

    private static double defix(int i) {
        return (((double) i) / 1e9);
    }

    private long lastrep = 0;
    private long rgtime = 0;

    public long globtime() {
        long now = System.currentTimeMillis();
        long raw = ((now - epoch) * 3) + (time * 1000);
        if (lastrep == 0) {
            rgtime = raw;
        } else {
            long gd = (now - lastrep) * 3;
            rgtime += gd;
            if (Math.abs(rgtime + gd - raw) > 1000)
                rgtime = rgtime + (long) ((raw - rgtime) * (1.0 - Math.pow(10.0, -(now - lastrep) / 1000.0)));
        }
        lastrep = now;
        return (rgtime);
    }

    private static final long secinday = 60 * 60 * 24;
    private static final long dewyladysmantletimemin = 4 * 60 * 60 + 45 * 60;
    private static final long dewyladysmantletimemax = 7 * 60 * 60 + 15 * 60;

    private void servertimecalc() {
        long secs = globtime() / 1000;
        long day = secs / secinday;
        long secintoday = secs % secinday;
        long hours = secintoday / 3600;
        long mins = (secintoday % 3600) / 60;
        servertime = String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Day %d, %02d:%02d"), day, hours, mins);
        if (secintoday >= dewyladysmantletimemin && secintoday <= dewyladysmantletimemax)
            servertime += Resource.getLocString(Resource.BUNDLE_LABEL, " (Dewy Lady's Mantle)");
        servertimetex = Text.render(servertime).tex();
    }

    public void blob(Message msg) {
        boolean inc = msg.uint8() != 0;
        while (!msg.eom()) {
            String t = msg.string().intern();
            Object[] a = msg.list();
            int n = 0;
            if (t == "tm") {
                time = ((Number) a[n++]).intValue();
                epoch = System.currentTimeMillis();
                if (!inc)
                    lastrep = 0;
                timersThread.tick(time, epoch);
                servertimecalc();
            } else if (t == "astro") {
                double dt = ((Number) a[n++]).doubleValue();
                double mp = ((Number) a[n++]).doubleValue();
                double yt = ((Number) a[n++]).doubleValue();
                boolean night = (Integer) a[n++] != 0;
                Color mc = (Color) a[n++];
                ast = new Astronomy(dt, mp, yt, night, mc);
            } else if (t == "light") {
                synchronized (this) {
                    tlightamb = (Color) a[n++];
                    tlightdif = (Color) a[n++];
                    tlightspc = (Color) a[n++];
                    tlightang = ((Number) a[n++]).doubleValue();
                    tlightelev = ((Number) a[n++]).doubleValue();
                    if (inc) {
                        olightamb = lightamb;
                        olightdif = lightdif;
                        olightspc = lightspc;
                        olightang = lightang;
                        olightelev = lightelev;
                        lchange = 0;
                    } else {
                        lightamb = tlightamb;
                        lightdif = tlightdif;
                        lightspc = tlightspc;
                        lightang = tlightang;
                        lightelev = tlightelev;
                        lchange = -1;
                    }
                }
            } else if (t == "sky") {
                synchronized (this) {
                    if (a.length < 1) {
                        sky1 = sky2 = null;
                        skyblend = 0.0;
                    } else {
                        sky1 = sess.getres(((Number) a[n++]).intValue());
                        if (a.length < 2) {
                            sky2 = null;
                            skyblend = 0.0;
                        } else {
                            sky2 = sess.getres(((Number) a[n++]).intValue());
                            skyblend = ((Number) a[n++]).doubleValue();
                        }
                    }
                }
            } else if (t == "wth") {
                synchronized (this) {
                    if (!inc)
                        wmap.clear();
                    Collection<Object> old = new LinkedList<Object>(wmap.keySet());
                    while (n < a.length) {
                        Indir<Resource> res = sess.getres(((Number) a[n++]).intValue());
                        Object[] args = (Object[]) a[n++];
                        Object curv = wmap.get(res);
                        if (curv instanceof Weather) {
                            Weather cur = (Weather) curv;
                            cur.update(args);
                        } else {
                            wmap.put(res, args);
                        }
                        old.remove(res);
                    }
                    for (Object p : old)
                        wmap.remove(p);
                }
            } else {
                System.err.println("Unknown globlob type: " + t);
            }
        }
    }

    public final Iterable<Weather> weather = new Iterable<Weather>() {
        public Iterator<Weather> iterator() {
            return (new Iterator<Weather>() {
                Iterator<Map.Entry<Indir<Resource>, Object>> bk = wmap.entrySet().iterator();
                Weather n = null;

                public boolean hasNext() {
                    if (n == null) {
                        while (true) {
                            if (!bk.hasNext())
                                return (false);
                            Map.Entry<Indir<Resource>, Object> cur = bk.next();
                            Object v = cur.getValue();
                            if (v instanceof Weather) {
                                n = (Weather) v;
                                break;
                            }
                            Class<? extends Weather> cl = cur.getKey().get().layer(Resource.CodeEntry.class).getcl(Weather.class);
                            Weather w;
                            try {
                                w = Utils.construct(cl.getConstructor(Object[].class), new Object[]{v});
                            } catch (NoSuchMethodException e) {
                                throw (new RuntimeException(e));
                            }
                            cur.setValue(n = w);
                        }
                    }
                    return (true);
                }

                public Weather next() {
                    if (!hasNext())
                        throw (new NoSuchElementException());
                    Weather ret = n;
                    n = null;
                    return (ret);
                }

                public void remove() {
                    throw (new UnsupportedOperationException());
                }
            });
        }
    };

    /* XXX: This is actually quite ugly and there should be a better
     * way, but until I can think of such a way, have this as a known
     * entry-point to be forwards-compatible with compiled
     * resources. */
    public static DirLight amblight(RenderList rl) {
        return (((MapView) ((PView.WidgetContext) rl.state().get(PView.ctx)).widget()).amb);
    }

    public Pagina paginafor(Resource.Named res) {
        if (res == null)
            return (null);
        synchronized (pmap) {
            Pagina p = pmap.get(res);
            if (p == null)
                pmap.put(res, p = new Pagina(res));
            return (p);
        }
    }

    public void paginae(Message msg) {
        synchronized (paginae) {
            while (!msg.eom()) {
                int act = msg.uint8();
                if (act == '+') {
                    String nm = msg.string();
                    int ver = msg.uint16();
                    Pagina pag = paginafor(Resource.remote().load(nm, ver));
                    paginae.add(pag);
                    pag.state(Pagina.State.ENABLED);
                    pag.meter = 0;
                    int t;
                    while ((t = msg.uint8()) != 0) {
                        if (t == '!') {
                            pag.state(Pagina.State.DISABLED);
                        } else if (t == '*') {
                            pag.meter = msg.int32();
                            pag.gettime = System.currentTimeMillis();
                            pag.dtime = msg.int32();
                        } else if (t == '^') {
                            pag.newp = 1;
                        }
                    }
                } else if (act == '-') {
                    String nm = msg.string();
                    int ver = msg.uint16();
                    paginae.remove(paginafor(Resource.remote().load(nm, ver)));
                }
            }
            pagseq++;
        }
    }

    public void cattr(Message msg) {
        synchronized (cattr) {
            while (!msg.eom()) {
                String nm = msg.string();
                int base = msg.int32();
                int comp = msg.int32();
                CAttr a = cattr.get(nm);
                if (a == null) {
                    a = new CAttr(nm, base, comp);
                    cattr.put(nm, a);
                } else {
                    a.update(base, comp);
                }
            }
        }
    }
}
