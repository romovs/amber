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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import haven.Surface.MeshVertex;
import haven.Surface.Vertex;

public abstract class Tiler {
    public final int id;

    public Tiler(int id) {
        this.id = id;
    }

    public static class MPart {
        public Coord lc, gc;
        public Surface.Vertex[] v;
        public float[] tcx, tcy;
        public int[] f;
        public GLState mat = null;

        public MPart(Coord lc, Coord gc, Surface.Vertex[] v, float[] tcx, float[] tcy, int[] f) {
            this.lc = lc;
            this.gc = gc;
            this.v = v;
            this.tcx = tcx;
            this.tcy = tcy;
            this.f = f;
        }

        public MPart(MPart... parts) {
            this.lc = parts[0].lc;
            this.gc = parts[0].gc;
            int[][] vmap = new int[parts.length][];
            int vbn = 0;
            for (int i = 0; i < parts.length; i++) {
                vbn += parts[i].v.length;
                vmap[i] = new int[parts[i].v.length];
            }
            Vertex[] vbuf = new Vertex[vbn];
            int vn = 0;
            for (int i = 0; i < parts.length; i++) {
                int cvn = vn;
                Vertex[] cv = parts[i].v;
                for (int o = 0; o < cv.length; o++) {
                    found:
                    {
                        for (int u = 0; u < cvn; u++) {
                            if (cv[o] == vbuf[u]) {
                                vmap[i][o] = u;
                                break found;
                            }
                        }
                        vbuf[vmap[i][o] = vn++] = cv[o];
                    }
                }
            }
            this.v = Utils.splice(vbuf, 0, vn);
            int fn = 0;
            for (MPart p : parts)
                fn += p.f.length;
            this.f = new int[fn];
            fn = 0;
            for (int i = 0; i < parts.length; i++) {
                for (int o = 0; o < parts[i].f.length; o++)
                    this.f[fn++] = vmap[i][parts[i].f[o]];
            }
            mapvertices(parts, vmap);
        }

        protected void mapvertices(MPart[] parts, int[][] vmap) {
            tcx = new float[v.length];
            tcy = new float[v.length];
            for (int i = 0; i < parts.length; i++) {
                for (int o = 0; o < parts[i].v.length; o++) {
                    tcx[vmap[i][o]] = parts[i].tcx[o];
                    tcy[vmap[i][o]] = parts[i].tcy[o];
                }
            }
        }

        public GLState mcomb(GLState mat) {
            return ((this.mat == null) ? mat : (GLState.compose(mat, this.mat)));
        }

        public static final float[] ctcx = {0, 0, 1, 1}, ctcy = {0, 1, 1, 0};
        public static final int[] rdiag = {0, 1, 2, 0, 2, 3}, ldiag = {0, 1, 3, 1, 2, 3};

        public static MPart splitquad(Coord lc, Coord gc, Surface.Vertex[] corners, boolean diag) {
            return (new MPart(lc, gc, corners, ctcx, ctcy, diag ? rdiag : ldiag));
        }
    }

    public static interface MCons {
        public void faces(MapMesh m, MPart desc);

        public static final MCons nil = new MCons() {
            public void faces(MapMesh m, MPart desc) {
            }
        };
    }

    public static interface CTrans {
        public MCons tcons(int z, int bmask, int cmask);
    }

    public static interface VertFactory {
        public MeshVertex make(MeshBuf buf, MPart d, int i);

        public static final VertFactory id = new VertFactory() {
            public MeshVertex make(MeshBuf buf, MPart d, int i) {
                return (new MeshVertex(buf, d.v[i]));
            }
        };
    }

    public static class SModel extends MapMesh.Model {
        private final VertFactory f;
        private final MeshVertex[] map;

        public SModel(MapMesh m, GLState mat, VertFactory f) {
            super(m, mat);
            this.f = f;
            this.map = new MeshVertex[m.data(MapMesh.gnd).vl.length];
        }

        public MeshVertex get(MPart d, int i) {
            MeshVertex ret;
            if ((ret = map[d.v[i].vi]) == null)
                ret = map[d.v[i].vi] = f.make(this, d, i);
            return (ret);
        }

        public MeshVertex[] get(MPart d) {
            MeshVertex[] ret = new MeshVertex[d.v.length];
            for (int i = 0; i < d.v.length; i++)
                ret[i] = get(d, i);
            return (ret);
        }

        public static class Key implements MapMesh.DataID<SModel> {
            public final GLState mat;
            public final VertFactory f;
            private final int hash;

            public Key(GLState mat, VertFactory f) {
                this.mat = mat;
                this.f = f;
                this.hash = (mat.hashCode() * 31) + f.hashCode();
            }

            public int hashCode() {
                return (hash);
            }

            public boolean equals(Object x) {
                return ((x instanceof Key) && mat.equals(((Key) x).mat) && f.equals(((Key) x).f));
            }

            public SModel make(MapMesh m) {
                return (new SModel(m, mat, f));
            }
        }

        public static SModel get(MapMesh m, GLState mat, VertFactory f) {
            return (m.data(new Key(mat, f)));
        }
    }

    public static void flatmodel(MapMesh m, Coord lc) {
        MapMesh.MapSurface s = m.data(MapMesh.gnd);
        if (s.split[s.ts.o(lc)]) {
            s.new Face(s.surf[s.vs.o(lc.x, lc.y)],
                    s.surf[s.vs.o(lc.x, lc.y + 1)],
                    s.surf[s.vs.o(lc.x + 1, lc.y + 1)]);
            s.new Face(s.surf[s.vs.o(lc.x, lc.y)],
                    s.surf[s.vs.o(lc.x + 1, lc.y + 1)],
                    s.surf[s.vs.o(lc.x + 1, lc.y)]);
        } else {
            s.new Face(s.surf[s.vs.o(lc.x, lc.y)],
                    s.surf[s.vs.o(lc.x, lc.y + 1)],
                    s.surf[s.vs.o(lc.x + 1, lc.y)]);
            s.new Face(s.surf[s.vs.o(lc.x, lc.y + 1)],
                    s.surf[s.vs.o(lc.x + 1, lc.y + 1)],
                    s.surf[s.vs.o(lc.x + 1, lc.y)]);
        }
    }

    public void model(MapMesh m, Random rnd, Coord lc, Coord gc) {
        flatmodel(m, lc);
    }

    public void lay(MapMesh m, Coord lc, Coord gc, MCons cons, boolean cover) {
        MapMesh.MapSurface s = m.data(MapMesh.gnd);
        cons.faces(m, MPart.splitquad(lc, gc, s.fortilea(lc), s.split[s.ts.o(lc)]));
    }

    public abstract void lay(MapMesh m, Random rnd, Coord lc, Coord gc);

    public abstract void trans(MapMesh m, Random rnd, Tiler gt, Coord lc, Coord gc, int z, int bmask, int cmask);

    public GLState drawstate(Glob glob, GLConfig cfg, Coord3f c) {
        return (null);
    }

    public static class FactMaker implements Resource.PublishedCode.Instancer {
        public Factory make(Class<?> cl) throws InstantiationException, IllegalAccessException {
            if (Factory.class.isAssignableFrom(cl)) {
                return (cl.asSubclass(Factory.class).newInstance());
            } else if (Tiler.class.isAssignableFrom(cl)) {
                Class<? extends Tiler> tcl = cl.asSubclass(Tiler.class);
                try {
                    final Constructor<? extends Tiler> cons = tcl.getConstructor(Integer.TYPE, Resource.Tileset.class);
                    return (new Factory() {
                        public Tiler create(int id, Resource.Tileset set) {
                            return (Utils.construct(cons, id, set));
                        }
                    });
                } catch (NoSuchMethodException e) {
                }
                throw (new RuntimeException("Could not find dynamic tiler contructor for " + tcl));
            }
            return (null);
        }
    }

    @Resource.PublishedCode(name = "tile", instancer = FactMaker.class)
    public static interface Factory {
        public Tiler create(int id, Resource.Tileset set);
    }

    @dolda.jglob.Discoverable
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ResName {
        public String value();
    }

    private static final Map<String, Factory> rnames = new TreeMap<String, Factory>();

    static {
        java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<Object>() {
            public Object run() {
                for (Class<?> cl : dolda.jglob.Loader.get(ResName.class).classes()) {
                    String nm = cl.getAnnotation(ResName.class).value();
                    try {
                        rnames.put(nm, (Factory) cl.newInstance());
                    } catch (InstantiationException e) {
                        throw (new Error(e));
                    } catch (IllegalAccessException e) {
                        throw (new Error(e));
                    }
                }
                return (null);
            }
        });
    }

    public static Factory byname(String name) {
        return (rnames.get(name));
    }
}
