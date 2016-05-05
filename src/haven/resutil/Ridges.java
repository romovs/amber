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

import static haven.MCache.tilesz;
import static haven.Utils.clip;

import java.util.Arrays;
import java.util.Random;

import haven.Coord;
import haven.Coord3f;
import haven.GLState;
import haven.Light;
import haven.MCache;
import haven.MapMesh;
import haven.MapMesh.Model;
import haven.MapMesh.Scan;
import haven.Material;
import haven.MeshBuf;
import haven.States;
import haven.Surface;
import haven.Surface.MeshVertex;
import haven.Surface.Vertex;
import haven.Tiler;
import haven.Tiler.MPart;
import haven.Utils;

public class Ridges extends MapMesh.Hooks {
    public static final MapMesh.DataID<Ridges> id = MapMesh.makeid(Ridges.class);
    public static final int segh = 8;
    public final MapMesh m;
    private final MapMesh.MapSurface ms;
    private final boolean[] breaks;
    private Vertex[][] edges, edgec;
    private float[] edgeo;
    private final MPart[] gnd, ridge;

    public interface RidgeTile {
        public int breakz();
    }

    public static class RPart extends MPart {
        public float[] rcx, rcy;
        public int[] rn;
        public float[] rh;

        public RPart(Coord lc, Coord gc, Surface.Vertex[] v, float[] tcx, float[] tcy, int[] f, float[] rcx, float[] rcy, int[] rn, float[] rh) {
            super(lc, gc, v, tcx, tcy, f);
            this.rcx = rcx;
            this.rcy = rcy;
            this.rn = rn;
            this.rh = rh;
        }

        public RPart(RPart... parts) {
            super(parts);
        }

        private void mapridges(RPart[] parts, int[][] vmap) {
            int nir = 0;
            int[][] pmap = new int[parts.length][];
            int[][] pvmap = new int[parts.length][];
            for (int i = 0; i < parts.length; i++) {
                pmap[i] = new int[parts[i].rh.length];
                for (int o = 0; o < parts[i].rh.length; o++)
                    pmap[i][o] = nir++;
                pvmap[i] = new int[parts[i].v.length];
                for (int o = 0; o < parts[i].v.length; o++)
                    pvmap[i][o] = pmap[i][parts[i].rn[o]];
            }
            int nor = 0;
            int[] nids = new int[nir];
            for (int i = 0; i < nir; i++)
                nids[i] = -1;
            for (int i = 0; i < parts.length; i++) {
                for (int o = 0; o < pvmap[i].length; o++) {
                    if (nids[pvmap[i][o]] < 0)
                        nids[pvmap[i][o]] = nor++;
                }
            }
            this.rn = new int[v.length];
            this.rh = new float[nor];
            for (int i = 0; i < parts.length; i++) {
                for (int o = 0; o < parts[i].v.length; o++) {
                    this.rn[vmap[i][o]] = nids[pmap[i][parts[i].rn[o]]];
                }
            }
            for (int i = 0; i < parts.length; i++) {
                for (int o = 0; o < parts[i].rh.length; o++) {
                    int nid = nids[pmap[i][o]];
                    if (nid < 0)
                        continue;
                    this.rh[nid] = parts[i].rh[o];
                }
            }
        }

        protected void mapvertices(MPart[] mparts, int[][] vmap) {
            super.mapvertices(mparts, vmap);
            RPart[] parts = (RPart[]) mparts;
            rcx = new float[v.length];
            rcy = new float[v.length];
            for (int i = 0; i < parts.length; i++) {
                for (int o = 0; o < parts[i].v.length; o++) {
                    rcx[vmap[i][o]] = parts[i].rcx[o];
                    rcy[vmap[i][o]] = parts[i].rcy[o];
                }
            }
            mapridges(parts, vmap);
        }
    }

    private int eo(int x, int y, int e) {
        int s = m.sz.x + 1;
        int b = (x + (y * s)) * 2;
        switch (e) {
            case 0: /* N */
                return (b + 1);
            case 1: /* E */
                return (b + 2);
            case 2: /* S */
                return (b + (s * 2) + 1);
            case 3: /* W */
                return (b);
        }
        throw (new Error());
    }

    private int eo(Coord c, int e) {
        return (eo(c.x, c.y, e));
    }

    private boolean[] breaks() {
        Scan ts = new Scan(new Coord(-1, -1), m.sz.add(2, 2));
        int[] bz = new int[ts.l];
        Coord c = new Coord();
        for (c.y = ts.ul.y; c.y < ts.br.y; c.y++) {
            for (c.x = ts.ul.x; c.x < ts.br.x; c.x++) {
                MCache map = m.map;
                Tiler t = map.tiler(map.gettile(m.ul.add(c)));
                if (t instanceof RidgeTile)
                    bz[ts.o(c)] = ((RidgeTile) t).breakz();
                else
                    bz[ts.o(c)] = Integer.MAX_VALUE;
            }
        }
        boolean[] breaks = new boolean[(m.sz.x + 1) * (m.sz.y + 1) * 2];
        for (c.y = 0; c.y <= m.sz.y; c.y++) {
            for (c.x = 0; c.x <= m.sz.x; c.x++) {
                Coord tc = m.ul.add(c);
                int ul = m.map.getz(tc);
                int xd = Math.abs(ul - m.map.getz(tc.add(1, 0)));
                if ((xd > bz[ts.o(c.x, c.y)]) && (xd > bz[ts.o(c.x, c.y - 1)]))
                    breaks[eo(c, 0)] = true;
                int yd = Math.abs(ul - m.map.getz(tc.add(0, 1)));
                if ((yd > bz[ts.o(c.x, c.y)]) && (yd > bz[ts.o(c.x - 1, c.y)]))
                    breaks[eo(c, 3)] = true;
            }
        }
        return (breaks);
    }

    private static Coord3f dc(float m, int d) {
        if ((d % 2) == 0)
            return (new Coord3f(m, 0, 0));
        else
            return (new Coord3f(0, m, 0));
    }

    private static final Coord[] tecs = {new Coord(0, -1), new Coord(1, 0), new Coord(0, 1), new Coord(-1, 0)};
    private static final Coord[] tccs = {new Coord(0, 0), new Coord(1, 0), new Coord(1, 1), new Coord(0, 1)};

    private boolean edgelc(Coord tc, int e) {
        Coord gc = tc.add(m.ul);
        return (m.map.getz(gc.add(tccs[e])) < m.map.getz(gc.add(tccs[(e + 1) % 4])));
    }

    private Vertex[] makeedge(Coord tc, int e) {
        if (e == 1)
            return (makeedge(tc.add(1, 0), 3));
        if (e == 2)
            return (makeedge(tc.add(0, 1), 0));
        float eds = edgelc(tc, e) ? 1 : -1;
        int lo, hi;
        {
            Coord gc = tc.add(m.ul);
            int z1 = m.map.getz(gc.add(tccs[e])), z2 = m.map.getz(gc.add(tccs[(e + 1) % 4]));
            lo = Math.min(z1, z2);
            hi = Math.max(z1, z2);
        }
        int nseg = Math.max((hi - lo + (segh / 2)) / segh, 2) - 1;
        Vertex[] ret = new Vertex[nseg + 1];
        Coord3f base = new Coord3f(tc.add(tccs[e]).add(tc.add(tccs[(e + 1) % 4])).mul(tilesz).mul(1, -1)).div(2);
        base.z = lo;
        float segi = (float) (hi - lo) / (float) nseg;
        Random rnd = MapMesh.grnd(m.ul.add(tc));
        rnd.setSeed(rnd.nextInt() + e);
        float bb = (rnd.nextFloat() - 0.5f) * 3.5f;
        float cfac = -eds * Math.min((hi - lo) * (5.0f / 37.0f), 5.5f);
        for (int v = 0; v <= nseg; v++) {
            float z = v * segi;
            float zp = (z / (hi - lo));
            float cd = (4 * zp * zp) - (4 * zp) + 0.5f;
            cd *= cfac;
            ret[v] = ms.new Vertex(base.add(dc(bb + ((rnd.nextFloat() - 0.5f) * 2.0f) + cd, e)).add(0, 0, z));
            if ((v > 0) && (v < nseg))
                ret[v].z += (rnd.nextFloat() - 0.5f) * segi * 0.5f;
        }
        return (ret);
    }

    private Vertex[] ensureedge(Coord tc, int e) {
        int o = eo(tc, e);
        Vertex[] ret;
        if ((ret = edges[o]) == null) {
            ret = edges[o] = makeedge(tc, e);
            edgec[o] = new Vertex[]{
                    ms.new Vertex(ret[0]),
                    ms.new Vertex(ret[ret.length - 1]),
            };
        }
        return (ret);
    }

    public Ridges(MapMesh m) {
        this.m = m;
        this.ms = m.data(MapMesh.gnd);
        this.breaks = breaks();
        this.edges = new Vertex[(m.sz.x + 1) * (m.sz.y + 1) * 2][];
        this.edgec = new Vertex[(m.sz.x + 1) * (m.sz.y + 1) * 2][];
        this.gnd = new MPart[ms.ts.l];
        this.ridge = new MPart[ms.ts.l];
    }

    public boolean[] breaks(Coord tc) {
        return (new boolean[]{
                breaks[eo(tc, 0)],
                breaks[eo(tc, 1)],
                breaks[eo(tc, 2)],
                breaks[eo(tc, 3)]
        });
    }

    private int[] tczs(Coord tc) {
        int[] ret = new int[4];
        for (int i = 0; i < 4; i++)
            ret[i] = m.map.getz(tc.add(m.ul).add(tccs[i]));
        return (ret);
    }

    private static int isend(boolean[] b) {
        for (int i = 0; i < 4; i++) {
            if (b[i] && !b[(i + 1) % 4] && !b[(i + 2) % 4] && !b[(i + 3) % 4])
                return (i);
        }
        return (-1);
    }

    private static int isdiag(boolean[] b) {
        for (int i = 0; i < 4; i++) {
            if (b[i] && b[(i + 1) % 4] && !b[(i + 2) % 4] && !b[(i + 3) % 4])
                return (i);
        }
        return (-1);
    }

    private int isdiag2(Coord tc, boolean[] b) {
        if (b[0] && b[1] && b[2] && b[3]) {
            Coord gc = tc.add(m.ul);
            int bz = ((RidgeTile) m.map.tiler(m.map.gettile(gc))).breakz();
            if (Math.abs(m.map.getz(gc) - m.map.getz(gc.add(1, 1))) <= bz)
                return (0);
            if (Math.abs(m.map.getz(gc.add(0, 1)) - m.map.getz(gc.add(1, 0))) <= bz)
                return (1);
        }
        return (-1);
    }

    private void mkfaces(Vertex[] va, int[] fa) {
        for (int i = 0; i < fa.length; i += 3)
            ms.new Face(va[fa[i]], va[fa[i + 1]], va[fa[i + 2]]);
    }

    private RPart connect(Coord tc, Vertex[] l, Vertex[] r) {
        Coord pc = tc.mul(tilesz).mul(1, -1);
        int n = l.length, m = r.length;
        Vertex[] va = new Vertex[n + m];
        float[] tcx = new float[n + m], tcy = new float[n + m];
        float[] rcx = new float[n + m], rcy = new float[n + m];
        int[] rn = new int[n + m];
        float lh = l[n - 1].z - l[0].z, rh = r[m - 1].z - r[0].z;
        float[] rhs = {lh, rh};
        for (int i = 0; i < n; i++) {
            va[i] = l[i];
            tcx[i] = clip((l[i].x - pc.x) / tilesz.x, 0, 1);
            tcy[i] = clip(-(l[i].y - pc.y) / tilesz.y, 0, 1);
            rcx[i] = 0;
            rcy[i] = (lh == 0) ? 0 : ((l[i].z - l[0].z) / lh);
            rn[i] = 0;
        }
        for (int i = 0; i < m; i++) {
            va[i + n] = r[i];
            tcx[i + n] = clip((r[i].x - pc.x) / tilesz.x, 0, 1);
            tcy[i + n] = clip(-(r[i].y - pc.y) / tilesz.y, 0, 1);
            rcx[i + n] = 1;
            rcy[i + n] = (rh == 0) ? 0 : ((r[i].z - r[0].z) / rh);
            rn[i + n] = 1;
        }
        int[] fa = new int[(n + m - 2) * 3];
        int fi = 0;
        int a = 0, b = 0;
        float E = 1.0f / n, F = 1.0f / m, e = E, f = F;
        while ((a < n - 1) || (b < m - 1)) {
            if (e < f) {
                fa[fi++] = a++;
                fa[fi++] = b + n;
                fa[fi++] = a;
                e += E;
            } else {
                fa[fi++] = a;
                fa[fi++] = b++ + n;
                fa[fi++] = b + n;
                f += F;
            }
        }
        mkfaces(va, fa);
        return (new RPart(tc, tc.add(this.m.ul), va, tcx, tcy, fa, rcx, rcy, rn, rhs));
    }

    private void modelcap(Coord tc, int dir) {
        ensureedge(tc, dir);
        Coord3f close = ms.fortile(tc.add(tccs[(dir + 2) % 4]))
                .add(ms.fortile(tc.add(tccs[(dir + 3) % 4])))
                .div(2);
        Vertex[] gv = {
                ms.fortile(tc.add(tccs[dir])),
                ms.fortile(tc.add(tccs[(dir + 3) % 4])),
                ms.new Vertex(close),
                edgec[eo(tc, dir)][edgelc(tc, dir) ? 0 : 1],

                edgec[eo(tc, dir)][edgelc(tc, dir) ? 1 : 0],
                ms.new Vertex(close),
                ms.fortile(tc.add(tccs[(dir + 2) % 4])),
                ms.fortile(tc.add(tccs[(dir + 1) % 4])),
        };
        float[] tcx = new float[8], tcy = new float[8];
        Coord pc = tc.mul(tilesz).mul(1, -1);
        for (int i = 0; i < 8; i++) {
            tcx[i] = clip((gv[i].x - pc.x) / tilesz.x, 0, 1);
            tcy[i] = clip(-(gv[i].y - pc.y) / tilesz.y, 0, 1);
        }
        mkfaces(gv, srfi);
        gnd[ms.ts.o(tc)] = new MPart(tc, tc.add(m.ul), gv, tcx, tcy, srfi);

        Vertex[] cls = new Vertex[]{ms.new Vertex(close)};
        if (edgelc(tc, dir))
            ridge[ms.ts.o(tc)] = connect(tc, edges[eo(tc, dir)], cls);
        else
            ridge[ms.ts.o(tc)] = connect(tc, cls, edges[eo(tc, dir)]);
    }

    private static final int[] srfi = {0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7};

    private void modelstraight(Coord tc, int dir) {
        ensureedge(tc, dir);
        ensureedge(tc, dir + 2);
        Vertex[] gv = {
                ms.fortile(tc.add(tccs[dir])),
                ms.fortile(tc.add(tccs[(dir + 3) % 4])),
                edgec[eo(tc, dir + 2)][edgelc(tc, dir + 2) ? 1 : 0],
                edgec[eo(tc, dir)][edgelc(tc, dir) ? 0 : 1],

                edgec[eo(tc, dir)][edgelc(tc, dir) ? 1 : 0],
                edgec[eo(tc, dir + 2)][edgelc(tc, dir + 2) ? 0 : 1],
                ms.fortile(tc.add(tccs[(dir + 2) % 4])),
                ms.fortile(tc.add(tccs[(dir + 1) % 4])),
        };
        float[] tcx = new float[8], tcy = new float[8];
        Coord pc = tc.mul(tilesz).mul(1, -1);
        for (int i = 0; i < 8; i++) {
            tcx[i] = clip((gv[i].x - pc.x) / tilesz.x, 0, 1);
            tcy[i] = clip(-(gv[i].y - pc.y) / tilesz.y, 0, 1);
        }
        mkfaces(gv, srfi);
        gnd[ms.ts.o(tc)] = new MPart(tc, tc.add(m.ul), gv, tcx, tcy, srfi);

        if (edgelc(tc, dir))
            ridge[ms.ts.o(tc)] = connect(tc, edges[eo(tc, dir)], edges[eo(tc, dir + 2)]);
        else
            ridge[ms.ts.o(tc)] = connect(tc, edges[eo(tc, dir + 2)], edges[eo(tc, dir)]);
    }

    private static final int[] d1rfi = {0, 1, 2, 3, 4, 7, 7, 4, 6, 6, 4, 5};

    private void modeldiag1(Coord tc, int dir) {
        ensureedge(tc, dir);
        ensureedge(tc, (dir + 1) % 4);
        Vertex[] gv = {
                ms.fortile(tc.add(tccs[(dir + 1) % 4])),
                edgec[eo(tc, dir)][edgelc(tc, dir) ? 1 : 0],
                edgec[eo(tc, (dir + 1) % 4)][edgelc(tc, dir) ? 1 : 0],

                ms.fortile(tc.add(tccs[dir])),
                ms.fortile(tc.add(tccs[(dir + 3) % 4])),
                ms.fortile(tc.add(tccs[(dir + 2) % 4])),
                edgec[eo(tc, (dir + 1) % 4)][edgelc(tc, dir) ? 0 : 1],
                edgec[eo(tc, dir)][edgelc(tc, dir) ? 0 : 1],
        };
        float[] tcx = new float[8], tcy = new float[8];
        Coord pc = tc.mul(tilesz).mul(1, -1);
        for (int i = 0; i < 8; i++) {
            tcx[i] = clip((gv[i].x - pc.x) / tilesz.x, 0, 1);
            tcy[i] = clip(-(gv[i].y - pc.y) / tilesz.y, 0, 1);
        }
        mkfaces(gv, d1rfi);
        gnd[ms.ts.o(tc)] = new MPart(tc, tc.add(m.ul), gv, tcx, tcy, d1rfi);

        if (edgelc(tc, dir))
            ridge[ms.ts.o(tc)] = connect(tc, edges[eo(tc, dir)], edges[eo(tc, (dir + 1) % 4)]);
        else
            ridge[ms.ts.o(tc)] = connect(tc, edges[eo(tc, (dir + 1) % 4)], edges[eo(tc, dir)]);
    }

    private static final int[] d2rfi = {0, 1, 2, 3, 4, 5, 6, 7, 11, 7, 8, 11, 11, 8, 10, 8, 9, 10};

    private void modeldiag2(Coord tc, int dir) {
        for (int i = 0; i < 4; i++) ensureedge(tc, i);
        Vertex[] gv = {
                ms.fortile(tc.add(tccs[dir + 1])),
                edgec[eo(tc, dir)][edgelc(tc, dir) ? 1 : 0],
                edgec[eo(tc, dir + 1)][edgelc(tc, dir) ? 1 : 0],

                ms.fortile(tc.add(tccs[(dir + 3) % 4])),
                edgec[eo(tc, dir + 2)][edgelc(tc, dir + 2) ? 1 : 0],
                edgec[eo(tc, (dir + 3) % 4)][edgelc(tc, dir + 2) ? 1 : 0],

                ms.fortile(tc.add(tccs[dir])),
                edgec[eo(tc, (dir + 3) % 4)][edgelc(tc, dir + 2) ? 0 : 1],
                edgec[eo(tc, dir + 2)][edgelc(tc, dir + 2) ? 0 : 1],
                ms.fortile(tc.add(tccs[dir + 2])),
                edgec[eo(tc, dir + 1)][edgelc(tc, dir) ? 0 : 1],
                edgec[eo(tc, dir)][edgelc(tc, dir) ? 0 : 1],
        };
        float[] tcx = new float[12], tcy = new float[12];
        Coord pc = tc.mul(tilesz).mul(1, -1);
        for (int i = 0; i < 12; i++) {
            tcx[i] = clip((gv[i].x - pc.x) / tilesz.x, 0, 1);
            tcy[i] = clip(-(gv[i].y - pc.y) / tilesz.y, 0, 1);
        }
        mkfaces(gv, d2rfi);
        gnd[ms.ts.o(tc)] = new MPart(tc, tc.add(m.ul), gv, tcx, tcy, d2rfi);

        RPart r1, r2;
        if (edgelc(tc, dir))
            r1 = connect(tc, edges[eo(tc, dir)], edges[eo(tc, dir + 1)]);
        else
            r1 = connect(tc, edges[eo(tc, dir + 1)], edges[eo(tc, dir)]);
        if (edgelc(tc, dir + 2))
            r2 = connect(tc, edges[eo(tc, dir + 2)], edges[eo(tc, (dir + 3) % 4)]);
        else
            r2 = connect(tc, edges[eo(tc, (dir + 3) % 4)], edges[eo(tc, dir + 2)]);
        ridge[ms.ts.o(tc)] = new RPart(r1, r2);
    }

    private static Coord3f zmatch(Coord3f[] cl, float z) {
        Coord3f ret = cl[0];
        float md = Math.abs(cl[0].z - z);
        for (int i = 1; i < cl.length; i++) {
            float zd = Math.abs(cl[i].z - z);
            if (zd < md) {
                ret = cl[i];
                md = zd;
            }
        }
        return (ret);
    }

    private Vertex[] colzmatch(Coord3f[] cl, float lo, float hi) {
        int i, l, h;
        float md;
        for (i = 1, l = 0, md = Math.abs(cl[0].z - lo); i < cl.length; i++) {
            float zd = Math.abs(cl[i].z - lo);
            if (zd < md) {
                l = i;
                md = zd;
            }
        }
        for (i = 1, h = 0, md = Math.abs(cl[0].z - hi); i < cl.length; i++) {
            float zd = Math.abs(cl[i].z - hi);
            if (zd < md) {
                h = i;
                md = zd;
            }
        }
        Vertex[] ret = new Vertex[1 + h - l];
        for (i = 0; i < ret.length; i++)
            ret[i] = ms.new Vertex(cl[i + l]);
        return (ret);
    }

    private static float[] mktcx(Vertex[] v, Coord pc) {
        float[] ret = new float[v.length];
        for (int i = 0; i < v.length; i++)
            ret[i] = clip((v[i].x - pc.x) / tilesz.x, 0, 1);
        return (ret);
    }

    private static float[] mktcy(Vertex[] v, Coord pc) {
        float[] ret = new float[v.length];
        for (int i = 0; i < v.length; i++)
            ret[i] = clip(-(v[i].y - pc.y) / tilesz.y, 0, 1);
        return (ret);
    }

    private static final int[] cg1rfi = {0, 1, 2, 0, 2, 3};
    private static final int[] cg2rfi = {0, 1, 4, 4, 1, 2, 4, 2, 3};

    private void modelcomplex(Coord tc, boolean[] breaks) {
        Coord gc = tc.add(m.ul), pc = tc.mul(tilesz).mul(1, -1);
        int[] tczs = tczs(tc);
        int s;
        for (s = 0; !breaks[s] || !breaks[(s + 3) % 4]; s++) ;
        Coord3f[] col;
        {
            int n = 0;
            float[] zs = new float[4];
            for (int i = 0, d = s; i < 4; i++) {
                if (breaks[d]) {
                    zs[n++] = tczs[d];
                    d = (d + 1) % 4;
                } else {
                    zs[n++] = (tczs[d] + tczs[(d + 1) % 4]) / 2;
                    i++;
                    d = (d + 2) % 4;
                }
            }
            zs = Utils.splice(zs, 0, n);
            Arrays.sort(zs);
            col = new Coord3f[n];
            float tcx = tc.x * tilesz.x + (tilesz.x / 2.0f), tcy = -(tc.y * tilesz.y + (tilesz.y / 2.0f));
            Random rnd = m.rnd(tc);
            for (int i = 0; i < n; i++) {
                col[i] = new Coord3f(tcx + ((rnd.nextFloat() - 0.5f) * 5.0f),
                        tcy + ((rnd.nextFloat() - 0.5f) * 5.0f),
                        zs[i]);
            }
        }

        MPart[] gnd = new MPart[4];
        RPart[] rdg = new RPart[4];
        int n = 0;
        for (int i = 0, d = s; i < 4; i++) {
            if (breaks[d]) {
                ensureedge(tc, (d + 3) % 4);
                ensureedge(tc, d);
                Vertex[] gv = {
                        ms.fortile(tc.add(tccs[d])),
                        edgec[eo(tc, (d + 3) % 4)][edgelc(tc, (d + 3) % 4) ? 1 : 0],
                        ms.new Vertex(zmatch(col, tczs[d])),
                        edgec[eo(tc, d)][edgelc(tc, d) ? 0 : 1],
                };
                mkfaces(gv, cg1rfi);
                gnd[n] = new MPart(tc, gc, gv, mktcx(gv, pc), mktcy(gv, pc), cg1rfi);
                if (edgelc(tc, d))
                    rdg[n] = connect(tc, edges[eo(tc, d)], colzmatch(col, tczs[d], tczs[(d + 1) % 4]));
                else
                    rdg[n] = connect(tc, colzmatch(col, tczs[(d + 1) % 4], tczs[d]), edges[eo(tc, d)]);
                n++;
                d = (d + 1) % 4;
            } else {
                assert (breaks[(d + 1) % 4]);
                ensureedge(tc, (d + 3) % 4);
                ensureedge(tc, (d + 1) % 4);
                float mz = (tczs[d] + tczs[(d + 1) % 4]) / 2.0f;
                Vertex[] gv = {
                        ms.fortile(tc.add(tccs[(d + 1) % 4])),
                        ms.fortile(tc.add(tccs[d])),
                        edgec[eo(tc, (d + 3) % 4)][edgelc(tc, (d + 3) % 4) ? 1 : 0],
                        ms.new Vertex(zmatch(col, mz)),
                        edgec[eo(tc, (d + 1) % 4)][edgelc(tc, (d + 1) % 4) ? 0 : 1],
                };
                mkfaces(gv, cg2rfi);
                gnd[n] = new MPart(tc, gc, gv, mktcx(gv, pc), mktcy(gv, pc), cg2rfi);
                if (edgelc(tc, (d + 1) % 4))
                    rdg[n] = connect(tc, edges[eo(tc, (d + 1) % 4)], colzmatch(col, mz, tczs[(d + 2) % 4]));
                else
                    rdg[n] = connect(tc, colzmatch(col, tczs[(d + 2) % 4], mz), edges[eo(tc, (d + 1) % 4)]);
                n++;
                i++;
                d = (d + 2) % 4;
            }
        }
        gnd = Utils.splice(gnd, 0, n);
        rdg = Utils.splice(rdg, 0, n);
        this.gnd[ms.ts.o(tc)] = new MPart(gnd);
        this.ridge[ms.ts.o(tc)] = new RPart(rdg);
    }

    public boolean model(Coord tc) {
        tc = new Coord(tc);
        boolean[] b = breaks(tc);
        int d;
        if (!b[0] && !b[1] && !b[2] && !b[3]) {
            return (false);
        } else if ((d = isend(b)) >= 0) {
            modelcap(tc, d);
            return (true);
        } else if (b[0] && !b[1] && b[2] && !b[3]) {
            modelstraight(tc, 0);
            return (true);
        } else if (!b[0] && b[1] && !b[2] && b[3]) {
            modelstraight(tc, 1);
            return (true);
        } else if ((d = isdiag(b)) >= 0) {
            modeldiag1(tc, d);
            return (true);
        } else if ((d = isdiag2(tc, b)) >= 0) {
            modeldiag2(tc, d);
            return (true);
        } else {
	    try {
		modelcomplex(tc, b);
	    } catch(ArrayIndexOutOfBoundsException e) {
		/* XXX: Just ignore for now, until I can find the
		 * cause of this. */
	    } catch(NegativeArraySizeException e) {
	    }
            return (true);
        }
    }

    static final Tiler.MCons testcons = new Tiler.MCons() {
        GLState mat = GLState.compose(new Material.Colors(new java.awt.Color(255, 255, 255)), States.vertexcolor, Light.deflight);

        public void faces(MapMesh m, MPart mdesc) {
            RPart desc = (RPart) mdesc;
            Model mod = Model.get(m, mat);
            MeshBuf.Col col = mod.layer(MeshBuf.col);
            MeshVertex[] v = new MeshVertex[desc.v.length];
            for (int i = 0; i < desc.v.length; i++) {
                v[i] = new MeshVertex(mod, desc.v[i]);
                col.set(v[i], new java.awt.Color((int) (255 * desc.rcx[i]), (int) (255 * desc.rcy[i]), 0));
            }
            int[] f = desc.f;
            for (int i = 0; i < f.length; i += 3)
                mod.new Face(v[f[i]], v[f[i + 1]], v[f[i + 2]]);
        }
    };

    public static class TexCons implements Tiler.MCons {
        public final GLState mat;
        public final float texh;

        public TexCons(GLState mat, float texh) {
            this.mat = mat;
            this.texh = texh;
        }

        public void faces(MapMesh m, MPart mdesc) {
            RPart desc = (RPart) mdesc;
            Model mod = Model.get(m, mat);
            MeshBuf.Tex tex = mod.layer(MeshBuf.tex);
            int[] trn = new int[desc.rh.length];
            float zf = 1.0f / texh;
            for (int i = 0; i < trn.length; i++)
                trn[i] = Math.max((int) ((desc.rh[i] + (texh * 0.5f)) * zf), 1);
            MeshVertex[] v = new MeshVertex[desc.v.length];
            for (int i = 0; i < desc.v.length; i++) {
                v[i] = new MeshVertex(mod, desc.v[i]);
        /* tex.set(v[i], new Coord3f(desc.rcx[i], desc.v[i].z * zf, 0)); */
                tex.set(v[i], new Coord3f(desc.rcx[i], desc.rcy[i] * trn[desc.rn[i]], 0));
            }
            int[] f = desc.f;
            for (int i = 0; i < f.length; i += 3)
                mod.new Face(v[f[i]], v[f[i + 1]], v[f[i + 2]]);
        }
    }

    public boolean laygnd(Coord tc, Tiler.MCons cons) {
        MPart gnd = this.gnd[ms.ts.o(tc)];
        if (gnd == null)
            return (false);
        cons.faces(m, gnd);
        return (true);
    }

    public boolean layridge(Coord tc, Tiler.MCons cons) {
        MPart ridge = this.ridge[ms.ts.o(tc)];
        if (ridge == null)
            return (false);
        cons.faces(m, ridge);
        return (true);
    }

    public boolean clean() {
        edgeo = new float[edgec.length * 2];
        for (int i = 0; i < edgec.length; i++) {
            if (edgec[i] != null) {
                for (int o = 0; o < 2; o++) {
                    if ((i % 2) == 0)
                        edgeo[i * 2 + o] = (edgec[i][o].y % 11) + 5.5f;
                    else
                        edgeo[i * 2 + o] = (edgec[i][o].x % 11) - 5.5f;
                }
            }
        }
        edges = null;
        edgec = null;
        return (true);
    }

    public static boolean brokenp(MCache map, Coord tc) {
        Tiler t = map.tiler(map.gettile(tc));
        if (!(t instanceof RidgeTile))
            return (false);
        int bz = ((RidgeTile) t).breakz();
        for (Coord ec : tecs) {
            t = map.tiler(map.gettile(tc.add(ec)));
            if (t instanceof RidgeTile)
                bz = Math.min(bz, ((RidgeTile) t).breakz());
        }
        for (int i = 0; i < 4; i++) {
            if (Math.abs(map.getz(tc.add(tccs[(i + 1) % 4])) - map.getz(tc.add(tccs[i]))) > bz)
                return (true);
        }
        return (false);
    }

    public static boolean brokenp(MCache map, Coord tc, MCache.Grid g) {
        Tiler t = map.tiler(g.gettile(tc));
        if (!(t instanceof RidgeTile))
            return (false);
        int bz = ((RidgeTile) t).breakz();
        for (Coord ec : tecs) {
            t = map.tiler(g.gettile(tc.add(ec)));
            if (t instanceof RidgeTile)
                bz = Math.min(bz, ((RidgeTile) t).breakz());
        }
        for (int i = 0; i < 4; i++) {
            if (Math.abs(g.getz(tc.add(tccs[(i + 1) % 4])) - g.getz(tc.add(tccs[i]))) > bz)
                return (true);
        }
        return (false);
    }

    public static float edgeoff(MCache map, Coord tc, int edge, boolean hi) {
        Ridges r = map.getcut(tc.div(MCache.cutsz)).data(id);
        tc = tc.mod(MCache.cutsz);
        return (r.edgeo[(2 * r.eo(tc, edge)) + (hi ? 1 : 0)]);
    }
}
