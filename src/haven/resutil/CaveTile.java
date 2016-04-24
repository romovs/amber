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

package haven.resutil;

import java.util.Random;

import haven.Coord;
import haven.Coord3f;
import haven.MapMesh;
import haven.MapMesh.Scan;
import haven.Material;
import haven.MeshBuf;
import haven.Resource;
import haven.Surface;
import haven.Surface.Vertex;
import haven.Tiler;

public class CaveTile extends Tiler {
    public static final float h = 16;
    public final Material wtex;

    public static class Walls {
        public final MapMesh m;
        public final Scan cs;
        public final Vertex[][] wv;
        private MapMesh.MapSurface ms;

        public Walls(MapMesh m) {
            this.m = m;
            this.ms = m.data(MapMesh.gnd);
            cs = new Scan(Coord.z, m.sz.add(1, 1));
            wv = new Vertex[cs.l][];
        }

        public Vertex[] fortile(Coord tc) {
            if (wv[cs.o(tc)] == null) {
                Random rnd = MapMesh.grnd(tc.add(m.ul));
                Vertex[] buf = wv[cs.o(tc)] = new Vertex[4];
                buf[0] = ms.new Vertex(ms.fortile(tc));
                for (int i = 1; i < buf.length; i++) {
                    buf[i] = ms.new Vertex(buf[0].x, buf[0].y, buf[0].z + (i * h / (buf.length - 1)));
                    buf[i].x += (rnd.nextFloat() - 0.5f) * 3.0f;
                    buf[i].y += (rnd.nextFloat() - 0.5f) * 3.0f;
                    buf[i].z += (rnd.nextFloat() - 0.5f) * 3.5f;
                }
            }
            return (wv[cs.o(tc)]);
        }
    }

    public static final MapMesh.DataID<Walls> walls = MapMesh.makeid(Walls.class);

    @ResName("cave")
    public static class Factory implements Tiler.Factory {
        public Tiler create(int id, Resource.Tileset set) {
            Material wtex = null;
            for (Object rdesc : set.ta) {
                Object[] desc = (Object[]) rdesc;
                String p = (String) desc[0];
                if (p.equals("wmat")) {
                    wtex = set.getres().layer(Material.Res.class, (Integer) desc[1]).get();
                }
            }
            return (new CaveTile(id, set, wtex));
        }
    }

    public CaveTile(int id, Resource.Tileset set, Material wtex) {
        super(id);
        this.wtex = wtex;
    }

    private static final Coord[] tces = {new Coord(0, -1), new Coord(1, 0), new Coord(0, 1), new Coord(-1, 0)};
    private static final Coord[] tccs = {new Coord(0, 0), new Coord(1, 0), new Coord(1, 1), new Coord(0, 1)};

    private void modelwall(Walls w, Coord ltc, Coord rtc) {
        Vertex[] lw = w.fortile(ltc), rw = w.fortile(rtc);
        for (int i = 0; i < lw.length - 1; i++) {
            w.ms.new Face(lw[i + 1], lw[i], rw[i + 1]);
            w.ms.new Face(lw[i], rw[i], rw[i + 1]);
        }
    }

    public void model(MapMesh m, Random rnd, Coord lc, Coord gc) {
        super.model(m, rnd, lc, gc);
        Walls w = null;
        for (int i = 0; i < 4; i++) {
            int cid = m.map.gettile(gc.add(tces[i]));
            if (cid <= id || (m.map.tiler(cid) instanceof CaveTile))
                continue;
            if (w == null) w = m.data(walls);
            modelwall(w, lc.add(tccs[(i + 1) % 4]), lc.add(tccs[i]));
        }
    }

    private void mkwall(MapMesh m, Walls w, Coord ltc, Coord rtc) {
        Vertex[] lw = w.fortile(ltc), rw = w.fortile(rtc);
        MapMesh.Model mod = MapMesh.Model.get(m, wtex);
        MeshBuf.Vertex[] lv = new MeshBuf.Vertex[lw.length], rv = new MeshBuf.Vertex[rw.length];
        MeshBuf.Tex tex = mod.layer(MeshBuf.tex);
        for (int i = 0; i < lv.length; i++) {
            float ty = (float) i / (float) (lv.length - 1);
            lv[i] = new Surface.MeshVertex(mod, lw[i]);
            tex.set(lv[i], new Coord3f(0, ty, 0));
            rv[i] = new Surface.MeshVertex(mod, rw[i]);
            tex.set(rv[i], new Coord3f(1, ty, 0));
        }
        for (int i = 0; i < lv.length - 1; i++) {
            mod.new Face(lv[i + 1], lv[i], rv[i + 1]);
            mod.new Face(lv[i], rv[i], rv[i + 1]);
        }
    }

    public void lay(MapMesh m, Random rnd, Coord lc, Coord gc) {
        Walls w = null;
        for (int i = 0; i < 4; i++) {
            int cid = m.map.gettile(gc.add(tces[i]));
            if (cid <= id || (m.map.tiler(cid) instanceof CaveTile))
                continue;
            if (w == null) w = m.data(walls);
            mkwall(m, w, lc.add(tccs[(i + 1) % 4]), lc.add(tccs[i]));
        }
    }

    public void trans(MapMesh m, Random rnd, Tiler gt, Coord lc, Coord gc, int z, int bmask, int cmask) {
    }
}
