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

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

import haven.resutil.Ridges;

public class LocalMiniMap extends Widget {
    private static final Tex resize = Resource.loadtex("gfx/hud/wndmap/lg/resize");
    private static final Tex gridblue = Resource.loadtex("gfx/hud/mmap/gridblue");
    private static final Tex gridred = Resource.loadtex("gfx/hud/mmap/gridred");
    public static final Text.Foundry bushf = new Text.Foundry(Text.sansb, 12);
    private static final Text.Foundry partyf = bushf;
    public final MapView mv;
    private Coord cc = null;
    private MapTile cur = null;
    private UI.Grab dragging;
    private Coord doff = Coord.z;
    private Coord delta = Coord.z;
	private static final Resource alarmplayersfx = Resource.local().loadwait("sfx/alarmplayer");
    private static final Resource foragablesfx = Resource.local().loadwait("sfx/awwyeah");
    private static final Resource bearsfx = Resource.local().loadwait("sfx/bear");
    private static final Resource trollsfx = Resource.local().loadwait("sfx/troll");
    private static final Resource mammothsfx = Resource.local().loadwait("sfx/mammoth");
    private static final Resource doomedsfx = Resource.local().loadwait("sfx/doomed");
	private final HashSet<Long> sgobs = new HashSet<Long>();
    private final HashMap<Coord, BufferedImage> maptiles = new HashMap<Coord, BufferedImage>(28, 0.75f);
    private final Map<Pair<MCache.Grid, Integer>, Defer.Future<MapTile>> cache = new LinkedHashMap<Pair<MCache.Grid, Integer>, Defer.Future<MapTile>>(7, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<Pair<MCache.Grid, Integer>, Defer.Future<MapTile>> eldest) {
            return size() > 7;
        }
    };
    private final static Tex bushicn = Text.renderstroked("\u22C6", Color.CYAN, Color.BLACK, bushf).tex();
    private final static Tex treeicn = Text.renderstroked("\u25B2", Color.CYAN, Color.BLACK, bushf).tex();
    private Map<Color, Tex> xmap = new HashMap<Color, Tex>(6);
    public static Coord plcrel = null;
    public final static double partymemberarrowsize = 14;

    private static class MapTile {
        public MCache.Grid grid;
        public int seq;

        public MapTile(MCache.Grid grid, int seq) {
            this.grid = grid;
            this.seq = seq;
        }
    }

    private BufferedImage tileimg(int t, BufferedImage[] texes) {
        BufferedImage img = texes[t];
        if (img == null) {
            Resource r = ui.sess.glob.map.tilesetr(t);
            if (r == null)
                return (null);
            Resource.Image ir = r.layer(Resource.imgc);
            if (ir == null)
                return (null);
            img = ir.img;
            texes[t] = img;
        }
        return (img);
    }

    public BufferedImage drawmap(Coord ul, Coord sz) {
        BufferedImage[] texes = new BufferedImage[256];
        MCache m = ui.sess.glob.map;
        BufferedImage buf = TexI.mkbuf(sz);
        Coord c = new Coord();
        for (c.y = 0; c.y < sz.y; c.y++) {
            for (c.x = 0; c.x < sz.x; c.x++) {
                int t = m.gettile(ul.add(c));
                BufferedImage tex = tileimg(t, texes);
                int rgb = 0;
                if (tex != null)
                    rgb = tex.getRGB(Utils.floormod(c.x + ul.x, tex.getWidth()),
                            Utils.floormod(c.y + ul.y, tex.getHeight()));
                buf.setRGB(c.x, c.y, rgb);

                try {
                    if ((m.gettile(ul.add(c).add(-1, 0)) > t) ||
                            (m.gettile(ul.add(c).add(1, 0)) > t) ||
                            (m.gettile(ul.add(c).add(0, -1)) > t) ||
                            (m.gettile(ul.add(c).add(0, 1)) > t))
                        buf.setRGB(c.x, c.y, Color.BLACK.getRGB());
                } catch (Exception e) {
                }
            }
        }

        for (c.y = 1; c.y < sz.y - 1; c.y++) {
            for (c.x = 1; c.x < sz.x - 1; c.x++) {
                try {
                    int t = m.gettile(ul.add(c));
                    Tiler tl = m.tiler(t);
                    if (tl instanceof Ridges.RidgeTile) {
                        if (Ridges.brokenp(m, ul.add(c))) {
                            for (int y = c.y - 1; y <= c.y + 1; y++) {
                                for (int x = c.x - 1; x <= c.x + 1; x++) {
                                    Color cc = new Color(buf.getRGB(x, y));
                                    buf.setRGB(x, y, Utils.blendcol(cc, Color.BLACK, ((x == c.x) && (y == c.y)) ? 1 : 0.1).getRGB());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                }
            }
        }

        return (buf);
    }

    public LocalMiniMap(Coord sz, MapView mv) {
        super(sz);
        this.mv = mv;
    }

    public Coord p2c(Coord pc) {
        return (pc.div(tilesz).sub(cc).add(sz.div(2)));
    }

    public Coord c2p(Coord c) {
        return (c.sub(sz.div(2)).add(cc).mul(tilesz).add(tilesz.div(2)));
    }

    public void drawicons(GOut g) {
        OCache oc = ui.sess.glob.oc;
        List<Gob> dangergobs = new ArrayList<Gob>();
        synchronized (oc) {
            for (Gob gob : oc) {
                try {
                    GobIcon icon = gob.getattr(GobIcon.class);
                    Resource res = gob.getres();
                    if (res != null && (icon != null || Config.additonalicons.containsKey(res.name))) {
                        if (Config.dangerousgobres.contains(res.name)) {
                            dangergobs.add(gob);
                            continue;
                        }

                        boolean ignore = false;
                        if (Config.iconssel != null) {
                            for (String name : Config.iconssel) {
                                if (res.basename().equals(name)) {
                                    ignore = true;
                                    break;
                                }
                            }
                        }
                        if (!ignore) {
                            Coord gc = p2c(gob.rc);
                            Tex tex = icon != null ? icon.tex() : Config.additonalicons.get(res.name);
                            g.image(tex, gc.sub(tex.sz().div(2)).add(delta));
                        }
                    } else if (res != null) {
                        String basename = res.basename();
                        if (res.name.startsWith("gfx/terobjs/bumlings")) {
                            boolean recognized = false;

                            if (Config.boulderssel != null) {
                                for (String name : Config.boulderssel) {
                                    if (basename.startsWith(name)) {
                                        Coord pc = p2c(gob.rc).add(delta).sub(3, 3);
                                        g.chcolor(Color.BLACK);
                                        g.frect(pc, new Coord(6, 6));
                                        g.chcolor(Color.CYAN);
                                        g.frect(pc.add(1, 1), new Coord(4, 4));
                                        g.chcolor();
                                        recognized = true;
                                        break;
                                    }
                                }
                            }

                            if (!recognized) {
                                for (String name : Config.boulders) {
                                    if (basename.startsWith(name)) {
                                        recognized = true;
                                        break;
                                    }
                                }
                                if (!recognized) {
                                    Coord pc = p2c(gob.rc).add(delta).sub(3, 3);
                                    g.chcolor(Color.BLACK);
                                    g.frect(pc, new Coord(6, 6));
                                    g.chcolor(Color.RED);
                                    g.frect(pc.add(1, 1), new Coord(4, 4));
                                    g.chcolor();
                                }
                            }
                        } else if (res.name.startsWith("gfx/terobjs/bushes")) {
                            boolean recognized = false;

                            if (Config.bushessel != null) {
                                for (String name : Config.bushessel) {
                                    if (basename.startsWith(name)) {
                                        g.image(bushicn, p2c(gob.rc).add(delta).sub(3, 3));
                                        recognized = true;
                                        break;
                                    }
                                }
                            }

                            if (!recognized) {
                                for (String name : Config.bushes) {
                                    if (basename.startsWith(name)) {
                                        recognized = true;
                                        break;
                                    }
                                }
                                if (!recognized) {
                                    Coord pc = p2c(gob.rc).add(delta).sub(3, 3);
                                    g.atextstroked("\u22C6", pc, Color.RED, Color.BLACK, bushf);
                                }
                            }
                        } else if (res.name.startsWith("gfx/terobjs/trees")) {
                            boolean recognized = false;

                            if (Config.treessel != null) {
                                for (String name : Config.treessel) {
                                    if (basename.equals(name)) {
                                        g.image(treeicn, p2c(gob.rc).add(delta).sub(3, 3));
                                        recognized = true;
                                        break;
                                    }
                                }
                            }

                            if (!recognized) {
                                for (String name : Config.trees) {
                                    if (basename.equals(name)) {
                                        recognized = true;
                                        break;
                                    }
                                }
                                if (!recognized && !basename.endsWith("log") &&
                                        !basename.endsWith("fall") &&
                                        !basename.endsWith("stump") &&
                                        !basename.equals("oldtrunk")) {
                                    Coord pc = p2c(gob.rc).add(delta).sub(3, 3);
                                    g.atextstroked("\u25B2", pc, Color.RED, Color.BLACK);
                                }
                            }
                        }
                    }
                } catch (Loading l) {
                }
            }

            for (Gob gob : dangergobs) {
                try {
                    GobIcon icon = gob.getattr(GobIcon.class);
                    if (icon != null) {
                        Coord gc = p2c(gob.rc);
                        Tex tex = icon.tex();
                        g.image(tex, gc.sub(tex.sz().div(2)).add(delta));
                    }
                } catch (Loading l) {
                }
            }
            
            for (Gob gob : oc) {
                try {
                    Resource res = gob.getres();
                    if (res == null)
                        continue;

                    if ("body".equals(res.basename()) && gob.id != mv.player().id) {
                        boolean ispartymember = false;
                        synchronized (ui.sess.glob.party.memb) {
                            ispartymember = ui.sess.glob.party.memb.containsKey(gob.id);
                        }

                        Coord pc = p2c(gob.rc).add(delta);
                        if (!ispartymember) {
                            KinInfo kininfo = gob.getattr(KinInfo.class);
                            if (pc.x >= 0 && pc.x <= sz.x && pc.y >= 0 && pc.y < sz.y) {
                                g.chcolor(Color.BLACK);
                                g.fellipse(pc, new Coord(5, 5));
                                g.chcolor(kininfo != null ? BuddyWnd.gc[kininfo.group] : Color.WHITE);
                                g.fellipse(pc, new Coord(4, 4));
                                g.chcolor();
                            }

                            if ((Config.alarmunknown || Config.autohearth) && kininfo == null) {
                                if (!sgobs.contains(gob.id)) {
                                    sgobs.add(gob.id);
                                    Audio.play(alarmplayersfx, Config.alarmunknownvol);
                                    if (Config.autohearth)
                                        gameui().menu.wdgmsg("act", new Object[]{"travel", "hearth"});
                                }
                            } else if (Config.alarmred && kininfo != null && kininfo.group == 2) {
                                if (!sgobs.contains(gob.id)) {
                                    sgobs.add(gob.id);
                                    Audio.play(alarmplayersfx, Config.alarmredvol);
                                }
                            }
                        }
                    } else if (Config.foragables.contains(res.name)) {
                        if (Config.alarmonforagables && !sgobs.contains(gob.id)) {
                            sgobs.add(gob.id);
                            Audio.play(foragablesfx, Config.alarmonforagablesvol);
                        }
                    } else if (res.name.equals("gfx/kritter/lynx/lynx") || res.name.equals("gfx/kritter/bear/bear")) {
                        if (Config.alarmbears && !sgobs.contains(gob.id)) {
                            sgobs.add(gob.id);
                            GAttrib drw = gob.getattr(Drawable.class);
                            if (drw != null && drw instanceof Composite) {
                                Composite cpst = (Composite) drw;
                                if (cpst.nposes != null && cpst.nposes.size() > 0) {
                                    for (ResData resdata : cpst.nposes) {
                                        Resource posres = resdata.res.get();
                                        if (posres == null || !posres.name.endsWith("/knock")) {
                                            Audio.play(bearsfx, Config.alarmbearsvol);
                                            break;
                                        }
                                    }
                                } else {
                                    Audio.play(bearsfx, Config.alarmbearsvol);
                                }
                            }
                        }
                    } else if (res.name.equals("gfx/kritter/troll/troll")) {
                        if (mv.areamine != null)
                            mv.areamine.terminate();
                        if (Config.alarmtroll && !sgobs.contains(gob.id)) {
                            sgobs.add(gob.id);
                            Audio.play(trollsfx, Config.alarmtrollvol);
                        }
                    } else if (res.name.equals("gfx/kritter/mammoth/mammoth")) {
                        if (Config.alarmmammoth && !sgobs.contains(gob.id)) {
                            sgobs.add(gob.id);
                            GAttrib drw = gob.getattr(Drawable.class);
                            if (drw != null && drw instanceof Composite) {
                                Composite cpst = (Composite) drw;
                                if (cpst.nposes != null && cpst.nposes.size() > 0) {
                                    for (ResData resdata : cpst.nposes) {
                                        Resource posres = resdata.res.get();
                                        if (posres == null || !posres.name.endsWith("/knock")) {
                                            Audio.play(mammothsfx, Config.alarmmammothvol);
                                            break;
                                        }
                                    }
                                } else {
                                    Audio.play(mammothsfx, Config.alarmmammothvol);
                                }
                            }
                        }
                    } else if (res.name.equals("gfx/terobjs/vehicle/bram") || res.name.equals("gfx/terobjs/vehicle/catapult")) {
                        if (Config.alarmbram && !sgobs.contains(gob.id)) {
                            sgobs.add(gob.id);
                            Audio.play(doomedsfx, Config.alarmbramvol);
                        }
                    }
                } catch (Exception e) { // fail silently
                }
            }
        }
    }

    public Gob findicongob(Coord c) {
        OCache oc = ui.sess.glob.oc;
        synchronized (oc) {
            for (Gob gob : oc) {
                try {
                    GobIcon icon = gob.getattr(GobIcon.class);
                    if (icon != null) {
                        Coord gc = p2c(gob.rc);
                        Coord sz = icon.tex().sz();
                        if (c.isect(gc.sub(sz.div(2)), sz)) {
                            boolean ignore = false;
                            if (Config.iconssel != null) {
                                Resource res = icon.res.get();
                                if (res != null) {
                                    for (String name : Config.iconssel) {
                                        if (res.basename().equals(name)) {
                                            ignore = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (!ignore)
                                return (gob);
                        }
                    } else { // custom icons
                        Coord gc = p2c(gob.rc);
                        Coord sz = new Coord(18, 18);
                        if (c.isect(gc.sub(sz.div(2)), sz)) {
                            boolean ignore = false;
                            Resource res = gob.getres();
                            if (res != null && Config.additonalicons.containsKey(res.name)) {
                                if (Config.iconssel != null) {
                                    for (String name : Config.iconssel) {
                                        if (res.basename().equals(name)) {
                                            ignore = true;
                                            break;
                                        }
                                    }
                                }
                            } else {
                                ignore = true;
                            }
                            if (!ignore)
                                return (gob);
                        }
                    }
                } catch (Loading l) {
                }
            }
        }
        return (null);
    }

    public void tick(double dt) {
        Gob pl = ui.sess.glob.oc.getgob(mv.plgob);
        if(pl == null)
            this.cc = mv.cc.div(tilesz);
        else
            this.cc = pl.rc.div(tilesz);

        if (Config.playerposfile != null && MapGridSave.gul != null) {
            try {
                // instead of synchronizing MapGridSave.gul we just handle NPE
                plcrel = pl.rc.sub((MapGridSave.gul.x + 50) * tilesz.x, (MapGridSave.gul.y + 50) * tilesz.y);
            } catch (NullPointerException npe) {
            }
        }
    }

    public void draw(GOut g) {
        if (cc == null)
            return;

        map:
        {
            final MCache.Grid plg;
            try {
                plg = ui.sess.glob.map.getgrid(cc.div(cmaps));
            } catch (Loading l) {
                break map;
            }
            final int seq = plg.seq;

            if (cur == null || plg != cur.grid || seq != cur.seq) {
                Defer.Future<MapTile> f;
                synchronized (cache) {
                    f = cache.get(new Pair<MCache.Grid, Integer>(plg, seq));
                    if (f == null) {
                        f = Defer.later(new Defer.Callable<MapTile>() {
                            public MapTile call() {
                                boolean gczero = plg.gc.equals(Coord.z);
                                if (gczero && cur == null || cur != null && gczero && cur.grid != plg)
                                    maptiles.clear();
                                Coord ul = plg.ul;
                                Coord gc = plg.gc;
                                maptiles.put(gc.add(-1, -1), drawmap(ul.add(-100, -100), cmaps));
                                maptiles.put(gc.add(0, -1), drawmap(ul.add(0, -100), cmaps));
                                maptiles.put(gc.add(1, -1), drawmap(ul.add(100, -100), cmaps));
                                maptiles.put(gc.add(-1, 0), drawmap(ul.add(-100, 0), cmaps));
                                maptiles.put(gc, drawmap(ul, cmaps));
                                maptiles.put(gc.add(1, 0), drawmap(ul.add(100, 0), cmaps));
                                maptiles.put(gc.add(-1, 1), drawmap(ul.add(-100, 100), cmaps));
                                maptiles.put(gc.add(0, 1), drawmap(ul.add(0, 100), cmaps));
                                maptiles.put(gc.add(1, 1), drawmap(ul.add(100, 100), cmaps));
                                return new MapTile(plg, seq);
                            }
                        });
                        cache.put(new Pair<MCache.Grid, Integer>(plg, seq), f);
                    }
                }
                if (f.done())
                    cur = f.get();
            }
        }
        if (cur != null) {
            int hhalf = sz.x / 2;
            int vhalf = sz.y / 2;

            int ht = (hhalf / 100) + 2;
            int vt = (vhalf / 100) + 2;

            int pox = cur.grid.gc.x * 100 - cc.x + hhalf + delta.x;
            int poy = cur.grid.gc.y * 100 - cc.y + vhalf + delta.y;

            int tox = pox / 100 - 1;
            int toy = poy / 100 - 1;

            if (maptiles.size() >= 9) {
                for (int x = -ht; x < ht + ht; x++) {
                    for (int y = -vt; y < vt + vt; y++) {
                        BufferedImage mt = maptiles.get(cur.grid.gc.add(x - tox, y - toy));
                        if (mt != null) {
                            int mtcx = (x - tox) * 100 + pox;
                            int mtcy = (y - toy) * 100 + poy;
                            if (mtcx + 100 < 0 || mtcx > sz.x || mtcy + 100 < 0 || mtcy > sz.y)
                                continue;
                            Coord mtc = new Coord(mtcx, mtcy);
                            g.image(mt, mtc);
                            if (Config.mapshowgrid)
                                g.image(gridred, mtc);
                        }
                    }
                }
            }

            g.image(resize, sz.sub(resize.sz()));

            if (Config.mapshowviewdist) {
                Gob player = mv.player();
                if (player != null)
                    g.image(gridblue, p2c(player.rc).add(delta).sub(44, 44));
            }

            try {
                synchronized (ui.sess.glob.party.memb) {
                    Collection<Party.Member> members = ui.sess.glob.party.memb.values();
                    for (Party.Member m : members) {
                        Coord ptc;
                        double angle;
                        try {
                            ptc = m.getc();
                            angle = m.geta();
                        } catch (MCache.LoadingMap e) {
                            continue;
                        }
                        try {
                            ptc = p2c(ptc);
                            drawpartymember(g, ptc, angle);
                        } catch (NullPointerException npe) { // in case chars are in different words
                        }
                    }
                }
            } catch (Loading l) {
            }
        }
        drawicons(g);
    }

    public void center() {
        delta = Coord.z;
    }

    public boolean mousedown(Coord c, int button) {
        if (Config.alternmapctrls) {
            if (button != 2) {
                if (cc == null)
                    return false;
                Gob gob = findicongob(c.sub(delta));
                if (gob == null)
                    mv.wdgmsg("click", rootpos().add(c.sub(delta)), c2p(c.sub(delta)), button, ui.modflags());
                else
                    mv.wdgmsg("click", rootpos().add(c.sub(delta)), c2p(c.sub(delta)), button, ui.modflags(), 0, (int) gob.id, gob.rc, 0, -1);
            } else if (button == 2 && !Config.maplocked) {
                doff = c;
                dragging = ui.grabmouse(this);
            }
        } else {
            if (button == 3) {
                if (cc == null)
                    return false;
                Gob gob = findicongob(c.sub(delta));
                if (gob == null)
                    mv.wdgmsg("click", rootpos().add(c.sub(delta)), c2p(c.sub(delta)), 1, ui.modflags());
                else
                    mv.wdgmsg("click", rootpos().add(c.sub(delta)), c2p(c.sub(delta)), button, ui.modflags(), 0, (int) gob.id, gob.rc, 0, -1);
            } else if (button == 1 && !Config.maplocked) {
                doff = c;
                dragging = ui.grabmouse(this);
            }
        }
        return true;
    }

    public void mousemove(Coord c) {
        if (dragging != null) {
            delta = delta.add(c.sub(doff));
            doff = c;
        }
    }

    public boolean mouseup(Coord c, int button) {
        if (dragging != null) {
            dragging.remove();
            dragging = null;
        }
        return (true);
    }

    private void drawpartymember(GOut g, Coord position, double angle) {
        final Coord top = position.add(Coord.sc(0, partymemberarrowsize / 2).rotate(angle));
        final Coord left = position.add(Coord.sc(Math.PI / 4, -partymemberarrowsize / 2).rotate(angle));
        final Coord right = position.add(Coord.sc(-Math.PI / 4, -partymemberarrowsize / 2).rotate(angle));
        g.polygon(new Color(255, 0, 0), top, left, right);
        g.polyline(new Color(0, 0, 0), top, left, right);
    }
}
