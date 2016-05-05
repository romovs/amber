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

public class Location extends Transform {
    public final String id;

    public Location(Matrix4f xf, String id) {
        super(xf);
        this.id = id;
    }

    public Location(Matrix4f xf) {
        this(xf, null);
    }

    public static class Chain extends GLState {
        public final Location loc;
        public final Chain p;
        private Chain(Location loc, Chain p) {
            this.loc = loc;
            this.p = p;
        }

        public Matrix4f fin(Matrix4f o) {
            if (p == null)
                return (loc.fin(o));
            return (loc.fin(p.fin(o)));
        }

        public void apply(GOut g) {
        }

        public void unapply(GOut g) {
        }

        public void prep(Buffer b) {
            throw (new RuntimeException("Location chains should not be applied directly."));
        }

        public Chain back(String id) {
            for (Chain cur = this; cur != null; cur = cur.p) {
                if (cur.loc.id == id)
                    return (cur);
            }
            return (null);
        }

        public int hashCode() {
            int h = System.identityHashCode(loc);
            if(p != null)
                h = (h * 31) + p.hashCode();
            return(h);
        }

        public boolean equals(Object o) {
            if(!(o instanceof Chain))
                return(false);
            Chain c = (Chain)o;
            return((c.loc == loc) && Utils.eq(c.p, p));
        }


        public String toString() {
            String ret = loc.toString();
            if (p != null)
                ret += " -> " + p;
            return (ret);
        }

        public static final Instancer<Chain> instancer = new Instancer<Chain>() {
            final Chain instanced = new Chain(null, null) {
                public Matrix4f fin(Matrix4f o) {
                    throw (new RuntimeException("Current in instanced drawing; cannot finalize a single location"));
                }

                public String toString() {
                    return ("instanced location");
                }

                final haven.glsl.ShaderMacro[] shaders = {mkinstanced};

                public haven.glsl.ShaderMacro[] shaders() {
                    return (shaders);
                }
            };

            public Chain inststate(Chain[] in) {
                return (instanced);
            }
        };
    }

    public void apply(GOut g) {
        throw (new RuntimeException("Locations should not be applied directly."));
    }

    public void unapply(GOut g) {
        throw (new RuntimeException("Locations should not be applied directly."));
    }

    public void prep(Buffer b) {
        Chain p = b.get(PView.loc);
        b.put(PView.loc, new Chain(this, p));
    }

    public static Chain back(Buffer b, String id) {
        Chain s = b.get(PView.loc);
        return (s == null ? s : s.back(id));
    }

    public static Chain goback(Buffer b, String id) {
        Chain s = back(b, id);
        if (s == null)
            throw (new IllegalStateException("No such back-link: " + id));
        b.put(PView.loc, s);
        return (s);
    }

    public static GLState goback(final String id) {
        return (new GLState.Abstract() {
            public void prep(Buffer buf) {
                goback(buf, id);
            }
        });
    }

    public static Location xlate(Coord3f c) {
        return (new Location(makexlate(new Matrix4f(), c)));
    }

    public static Location rot(Coord3f axis, float angle) {
        return (new Location(makerot(new Matrix4f(), axis.norm(), angle)));
    }

    public static final Location onlyxl = new Location(Matrix4f.id) {
        private Matrix4f lp = null, fin;

        public Matrix4f fin(Matrix4f p) {
            if (p != lp) {
                fin = Matrix4f.identity();
                fin.m[12] = p.m[12];
                fin.m[13] = p.m[13];
                fin.m[14] = p.m[14];
            }
            return (fin);
        }
    };
}
