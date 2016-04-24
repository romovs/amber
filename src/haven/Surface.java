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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Surface {
    private List<Vertex> v = new ArrayList<Vertex>();
    private Collection<Face> f = new ArrayList<Face>();
    @SuppressWarnings("rawtypes")
	private Map<DataID, Object> data = new HashMap<DataID, Object>();
    public Vertex[] vl, fv, tv;

    /* XXX: I'm starting to lose track of how many times I've
     * implemented this structure, but it seems to be hard to
     * generalize in Java. */
    public interface DataID<T> {
        public T make(Surface s);
    }

    @SuppressWarnings("unchecked")
    public <T> T data(DataID<T> id) {
        T ret = (T) data.get(id);
        if (ret == null)
            data.put(id, ret = id.make(this));
        return (ret);
    }

    public class Vertex extends Coord3f {
        public final int vi;
        public int ei, ne;

        public Vertex(float x, float y, float z) {
            super(x, y, z);
            vi = v.size();
            v.add(this);
        }

        public Vertex(Coord3f c) {
            this(c.x, c.y, c.z);
        }

        public Surface s() {
            return (Surface.this);
        }

        public void modify(MeshBuf buf, MeshBuf.Vertex v) {
        }
    }

    public static class MeshVertex extends MeshBuf.Vertex {
        public final Vertex v;

        public MeshVertex(MeshBuf buf, Vertex v) {
            buf.super(v, v.s().data(Surface.nrm).get(v));
            this.v = v;
            v.modify(buf, this);
        }
    }

    public class Face {
        public final Vertex v1, v2, v3;

        public Face(Vertex v1, Vertex v2, Vertex v3) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            f.add(this);
        }
    }

    public void fin() {
        this.vl = new Vertex[this.v.size()];
        for (Vertex v : this.v)
            vl[v.vi] = v;
        int nc = this.f.size() * 3;
        class Corner {
            Vertex v, f, t;

            Corner(Vertex v, Vertex f, Vertex t) {
                this.v = v;
                this.f = f;
                this.t = t;
            }
        }
        Corner[] cl = new Corner[nc];
        int i = 0;
        for (Face f : this.f) {
            cl[i++] = new Corner(f.v1, f.v3, f.v2);
            cl[i++] = new Corner(f.v2, f.v1, f.v3);
            cl[i++] = new Corner(f.v3, f.v2, f.v1);
        }
        Arrays.sort(cl, new Comparator<Corner>() {
            public int compare(Corner a, Corner b) {
                return (a.v.vi - b.v.vi);
            }
        });
        this.fv = new Vertex[nc];
        this.tv = new Vertex[nc];
        Vertex pv = null;
        for (i = 0; i < nc; i++) {
            if (pv != cl[i].v) {
                pv = cl[i].v;
                pv.ei = i;
            }
            this.fv[i] = cl[i].f;
            this.tv[i] = cl[i].t;
            pv.ne++;
        }
        this.v = null;
        this.f = null;
    }

    public void clear() {
        data.clear();
    }

    public class Normals {
        public final Coord3f[] buf = new Coord3f[vl.length];

        private Normals() {
        }

        public Coord3f get(Vertex v) {
            Coord3f ret = buf[v.vi];
            if (ret == null) {
                Coord3f nn = Coord3f.o;
                for (int i = 0, o = v.ei; i < v.ne; i++, o++)
                    nn = nn.add(tv[o].sub(v).cmul(fv[o].sub(v)).norm());
                ret = buf[v.vi] = nn.div(v.ne);
            }
            return (ret);
        }

        public void set(Vertex v, Coord3f n) {
            buf[v.vi] = n;
        }
    }

    public static final DataID<Normals> nrm = new DataID<Normals>() {
        public Normals make(Surface s) {
            return (s.new Normals());
        }
    };
}
