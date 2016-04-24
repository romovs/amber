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

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.imageio.ImageIO;

public class MiniMap extends Widget {
    static Map<String, Tex> grids = new WeakHashMap<String, Tex>();
    static Set<String> loading = new HashSet<String>();
    static Loader loader = new Loader();
    public static final Tex bg = Resource.loadtex("gfx/hud/mmap/ptex");
    public static final Tex nomap = Resource.loadtex("gfx/hud/mmap/nomap");
    public static final Resource plx = Resource.local().loadwait("gfx/hud/mmap/x");
    MapView mv;

    static class Loader implements Runnable {
        Thread me = null;

        private InputStream getreal(String nm) throws IOException {
            URL url = new URL(Config.mapurl, nm + ".png");
            URLConnection c = url.openConnection();
            c.addRequestProperty("User-Agent", "Haven/1.0");
            InputStream s = c.getInputStream();
        /*
         * I've commented this out, since it seems that the JNLP
	     * PersistenceService (or at least Sun's implementation of
	     * it) is SLOWER THAN SNAILS, so this caused more problems
	     * than it solved.
	     *
	    if(ResCache.global != null) {
		StreamTee tee = new StreamTee(s);
		tee.setncwe();
		tee.attach(ResCache.global.store("mm/" + nm));
		s = tee;
	    }
	    */
            return (s);
        }

        private InputStream getcached(String nm) throws IOException {
            if (ResCache.global == null)
                throw (new FileNotFoundException("No resource cache installed"));
            return (ResCache.global.fetch("mm/" + nm));
        }

        public void run() {
            try {
                while (true) {
                    String grid;
                    synchronized (grids) {
                        grid = null;
                        for (String cg : loading) {
                            grid = cg;
                            break;
                        }
                    }
                    if (grid == null)
                        break;
                    try {
                        InputStream in;
                        try {
                            in = getcached(grid);
                        } catch (FileNotFoundException e) {
                            in = getreal(grid);
                        }
                        BufferedImage img;
                        try {
                            img = ImageIO.read(in);
                        } finally {
                            Utils.readtileof(in);
                            in.close();
                        }
                        Tex tex = new TexI(img);
                        synchronized (grids) {
                            grids.put(grid, tex);
                            loading.remove(grid);
                        }
                    } catch (IOException e) {
                        synchronized (grids) {
                            grids.put(grid, null);
                            loading.remove(grid);
                        }
                    }
                }
            } finally {
                synchronized (this) {
                    me = null;
                }
            }
        }

        void start() {
            synchronized (this) {
                if (me == null) {
                    me = new HackThread(this, "Minimap loader");
                    me.setDaemon(true);
                    me.start();
                }
            }
        }

        void req(String nm) {
            synchronized (grids) {
                if (loading.contains(nm))
                    return;
                loading.add(nm);
                start();
            }
        }
    }

    public MiniMap(Coord sz, MapView mv) {
        super(sz);
        this.mv = mv;
    }

    public static Tex getgrid(final String nm) {
        return (AccessController.doPrivileged(new PrivilegedAction<Tex>() {
            public Tex run() {
                synchronized (grids) {
                    if (grids.containsKey(nm)) {
                        return (grids.get(nm));
                    } else {
                        loader.req(nm);
                        return (null);
                    }
                }
            }
        }));
    }

    public void draw(GOut g) {
        Coord tc = mv.cc.div(tilesz);
        Coord ulg = tc.div(cmaps);
        while ((ulg.x * cmaps.x) - tc.x + (sz.x / 2) > 0)
            ulg.x--;
        while ((ulg.y * cmaps.y) - tc.y + (sz.y / 2) > 0)
            ulg.y--;
        boolean missing = false;
        g.image(bg, Coord.z);
        outer:
        for (int y = ulg.y; (y * cmaps.y) - tc.y + (sz.y / 2) < sz.y; y++) {
            for (int x = ulg.x; (x * cmaps.x) - tc.x + (sz.x / 2) < sz.x; x++) {
                Coord cg = new Coord(x, y);
                MCache.Grid grid;
                synchronized (ui.sess.glob.map.req) {
                    synchronized (ui.sess.glob.map.grids) {
                        grid = ui.sess.glob.map.grids.get(cg);
                        if (grid == null)
                            ui.sess.glob.map.request(cg);
                    }
                }
                if (grid == null)
                    continue;
                if (grid.mnm == null) {
                    missing = true;
                    break outer;
                }
                Tex tex = getgrid(grid.mnm);
                if (tex == null)
                    continue;
                g.image(tex, cg.mul(cmaps).add(tc.inv()).add(sz.div(2)));
            }
        }
        if (missing) {
            g.image(nomap, Coord.z);
        } else {
            synchronized (ui.sess.glob.party.memb) {
                for (Party.Member m : ui.sess.glob.party.memb.values()) {
                    Coord ptc;
                    try {
                        ptc = m.getc();
                    } catch (MCache.LoadingMap e) {
                        ptc = null;
                    }
                    if (ptc == null)
                        continue;
                    ptc = ptc.div(tilesz).add(tc.inv()).add(sz.div(2));
                    g.chcolor(m.col.getRed(), m.col.getGreen(), m.col.getBlue(), 128);
                    g.image(plx.layer(Resource.imgc).tex(), ptc.add(plx.layer(Resource.negc).cc.inv()));
                    g.chcolor();
                }
            }
        }
        super.draw(g);
    }
}
