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
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import haven.glsl.Attribute;

public class MeshBuf {
    public final Collection<Vertex> v = new ArrayList<Vertex>();
    public final Collection<Face> f = new ArrayList<Face>();
    private VertexBuf vbuf = null;
    private int nextid = 0;
    private Layer<?>[] layers = new Layer<?>[0];
    private LayerID<?>[] lids = new LayerID<?>[0];

    public abstract class Layer<T> {
        public final int idx;

        public Layer() {
            idx = nextid++;
            layers = Utils.extend(layers, nextid);
            lids = Utils.extend(lids, nextid);
            layers[idx] = this;
            for (Vertex o : v)
                o.attrs = Utils.extend(o.attrs, nextid);
        }

        public void set(Vertex v, T data) {
            v.attrs[idx] = data;
        }

        @SuppressWarnings("unchecked")
        public T get(Vertex v) {
            return ((T) v.attrs[idx]);
        }

        public abstract VertexBuf.AttribArray build(Collection<T> in);

        public void copy(VertexBuf src, Vertex[] vmap, int off) {
        }
    }

    public static abstract class LayerID<L> {
        public abstract L cons(MeshBuf buf);
    }

    public static class CLayerID<L> extends LayerID<L> {
        public final Class<L> cl;
        private final java.lang.reflect.Constructor<L> cons;

        public CLayerID(Class<L> cl) {
            this.cl = cl;
            try {
                this.cons = cl.getConstructor(MeshBuf.class);
            } catch (NoSuchMethodException e) {
                throw (new RuntimeException(e));
            }
        }

        public L cons(MeshBuf buf) {
            return (Utils.construct(cons, buf));
        }
    }

    public class Tex extends Layer<Coord3f> {
        public VertexBuf.TexelArray build(Collection<Coord3f> in) {
            FloatBuffer data = Utils.wfbuf(in.size() * 2);
            for (Coord3f c : in) {
                data.put(c.x);
                data.put(c.y);
            }
            return (new VertexBuf.TexelArray(data));
        }

        public void copy(VertexBuf buf, Vertex[] vmap, int off) {
            VertexBuf.TexelArray src = buf.buf(VertexBuf.TexelArray.class);
            if (src == null)
                return;
            for (int i = 0, o = off * 2; i < vmap.length; i++, o += 2) {
                if (vmap[i] != null)
                    set(vmap[i], new Coord3f(src.data.get(o), src.data.get(o + 1), 0));
            }
        }
    }

    public static final LayerID<Tex> tex = new CLayerID<Tex>(Tex.class);

    public class Col extends Layer<Color> {
        public VertexBuf.ColorArray build(Collection<Color> in) {
            FloatBuffer data = Utils.wfbuf(in.size() * 4);
            for (Color c : in) {
                data.put(c.getRed() / 255.0f);
                data.put(c.getGreen() / 255.0f);
                data.put(c.getBlue() / 255.0f);
                data.put(c.getAlpha() / 255.0f);
            }
            return (new VertexBuf.ColorArray(data));
        }
    }

    public static final LayerID<Col> col = new CLayerID<Col>(Col.class);

    public abstract class AttribLayer<T> extends Layer<T> {
        public final Attribute attrib;

        public AttribLayer(Attribute attrib) {
            this.attrib = attrib;
        }
    }

    public class Vec1Layer extends AttribLayer<Float> {
        public Vec1Layer(Attribute attrib) {
            super(attrib);
        }

        public VertexBuf.Vec1Array build(Collection<Float> in) {
            FloatBuffer data = Utils.wfbuf(in.size());
            for (Float d : in)
                data.put(d);
            return (new VertexBuf.Vec1Array(data, attrib));
        }
    }

    public class Vec2Layer extends AttribLayer<Coord3f> {
        public Vec2Layer(Attribute attrib) {
            super(attrib);
        }

        public VertexBuf.Vec2Array build(Collection<Coord3f> in) {
            FloatBuffer data = Utils.wfbuf(in.size() * 2);
            for (Coord3f d : in) {
                data.put(d.x);
                data.put(d.y);
            }
            return (new VertexBuf.Vec2Array(data, attrib));
        }
    }

    public class Vec3Layer extends AttribLayer<Coord3f> {
        public Vec3Layer(Attribute attrib) {
            super(attrib);
        }

        public VertexBuf.Vec3Array build(Collection<Coord3f> in) {
            FloatBuffer data = Utils.wfbuf(in.size() * 3);
            for (Coord3f d : in) {
                data.put(d.x);
                data.put(d.y);
                data.put(d.z);
            }
            return (new VertexBuf.Vec3Array(data, attrib));
        }
    }

    public class Vec4Layer extends AttribLayer<float[]> {
        public Vec4Layer(Attribute attrib) {
            super(attrib);
        }

        public VertexBuf.Vec4Array build(Collection<float[]> in) {
            FloatBuffer data = Utils.wfbuf(in.size() * 4);
            for (float[] d : in) {
                data.put(d[0]);
                data.put(d[1]);
                data.put(d[2]);
                data.put(d[3]);
            }
            return (new VertexBuf.Vec4Array(data, attrib));
        }
    }

    public static abstract class ALayerID<L> extends LayerID<L> {
        public final Attribute attrib;

        public ALayerID(Attribute attrib) {
            this.attrib = attrib;
        }
    }

    public static class V1LayerID extends ALayerID<Vec1Layer> {
        public V1LayerID(Attribute attrib) {
            super(attrib);
        }

        public Vec1Layer cons(MeshBuf buf) {
            return (buf.new Vec1Layer(attrib));
        }
    }

    public static class V2LayerID extends ALayerID<Vec2Layer> {
        public V2LayerID(Attribute attrib) {
            super(attrib);
        }

        public Vec2Layer cons(MeshBuf buf) {
            return (buf.new Vec2Layer(attrib));
        }
    }

    public static class V3LayerID extends ALayerID<Vec3Layer> {
        public V3LayerID(Attribute attrib) {
            super(attrib);
        }

        public Vec3Layer cons(MeshBuf buf) {
            return (buf.new Vec3Layer(attrib));
        }
    }

    public static class V4LayerID extends ALayerID<Vec4Layer> {
        public V4LayerID(Attribute attrib) {
            super(attrib);
        }

        public Vec4Layer cons(MeshBuf buf) {
            return (buf.new Vec4Layer(attrib));
        }
    }

    @SuppressWarnings("unchecked")
    public <L extends Layer<?>> L layer(LayerID<L> id) {
        if (id == null)
            throw (new NullPointerException());
        for (int i = 0; i < lids.length; i++) {
            if (lids[i] == id)
                return ((L) layers[i]);
        }
        L ret = id.cons(this);
        lids[ret.idx] = id;
        return (ret);
    }

    public class Vertex {
        public Coord3f pos, nrm;
        private Object[] attrs = new Object[layers.length];
        private short idx;

        public Vertex(Coord3f pos, Coord3f nrm) {
            this.pos = pos;
            this.nrm = nrm;
            v.add(this);
        }

        public String toString() {
            return (String.format("MeshBuf.Vertex(%s, %s)", pos, nrm));
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

    public interface LayerMapper {
        public Layer<?> mapbuf(MeshBuf buf, VertexBuf.AttribArray src);
    }

    public Vertex[] copy(FastMesh src, LayerMapper mapper) {
        int min = -1, max = -1;
        for (int i = 0; i < src.num * 3; i++) {
            int idx = src.indb.get(i);
            if ((min < 0) || (idx < min))
                min = idx;
            if (idx > max)
                max = idx;
        }
        int nv = 0;
        VertexBuf.VertexArray posb = src.vert.buf(VertexBuf.VertexArray.class);
        VertexBuf.NormalArray nrmb = src.vert.buf(VertexBuf.NormalArray.class);
        Vertex[] vmap = new Vertex[max + 1 - min];
        for (int i = 0; i < src.num * 3; i++) {
            int idx = src.indb.get(i);
            if (vmap[idx - min] == null) {
                int o = idx * posb.n;
                Coord3f pos = new Coord3f(posb.data.get(o), posb.data.get(o + 1), posb.data.get(o + 2));
                o = idx * nrmb.n;
                Coord3f nrm = new Coord3f(nrmb.data.get(o), nrmb.data.get(o + 1), nrmb.data.get(o + 2));
                vmap[idx - min] = new Vertex(pos, nrm);
                nv++;
            }
        }
        for (VertexBuf.AttribArray data : src.vert.bufs) {
            Layer<?> l = mapper.mapbuf(this, data);
            if (l != null)
                l.copy(src.vert, vmap, min);
        }
        for (int i = 0; i < src.num; i++) {
            int o = i * 3;
            new Face(vmap[src.indb.get(o) - min],
                    vmap[src.indb.get(o + 1) - min],
                    vmap[src.indb.get(o + 2) - min]);
        }
        Vertex[] vl = new Vertex[nv];
        int n = 0;
        for (int i = 0; i < vmap.length; i++) {
            if (vmap[i] != null)
                vl[n++] = vmap[i];
        }
        return (vl);
    }

    private static final LayerMapper defmapper = new LayerMapper() {
        public Layer<?> mapbuf(MeshBuf buf, VertexBuf.AttribArray src) {
            if (src instanceof VertexBuf.TexelArray)
                return (buf.layer(tex));
            return (null);
        }
    };

    public Vertex[] copy(FastMesh src) {
        return (copy(src, defmapper));
    }

    @SuppressWarnings("unchecked")
    private <T> VertexBuf.AttribArray mklayer(Layer<T> l, Object[] abuf) {
        int i = 0;
        boolean f = false;
        for (Vertex v : this.v) {
            if ((abuf[i++] = v.attrs[l.idx]) != null)
                f = true;
        }
        if (!f)
            return (null);
        return (l.build(Arrays.asList((T[]) abuf)));
    }

    private void mkvbuf() {
        if (v.isEmpty())
            throw (new RuntimeException("Tried to build empty vertex buffer"));

        FloatBuffer pos, nrm;
        {
            pos = Utils.wfbuf(v.size() * 3);
            nrm = Utils.wfbuf(v.size() * 3);
            int pi = 0;
            short i = 0;
            for (Vertex v : this.v) {
                pos.put(pi + 0, v.pos.x);
                pos.put(pi + 1, v.pos.y);
                pos.put(pi + 2, v.pos.z);
                nrm.put(pi + 0, v.nrm.x);
                nrm.put(pi + 1, v.nrm.y);
                nrm.put(pi + 2, v.nrm.z);
                pi += 3;
                v.idx = i++;
                if (i == 0)
                    throw (new RuntimeException("Too many vertices in meshbuf"));
            }
        }

        VertexBuf.AttribArray[] arrays = new VertexBuf.AttribArray[layers.length + 2];
        int li = 0;
        arrays[li++] = new VertexBuf.VertexArray(pos);
        arrays[li++] = new VertexBuf.NormalArray(nrm);

        Object[] abuf = new Object[v.size()];
        for (int i = 0; i < layers.length; i++) {
            VertexBuf.AttribArray l = mklayer(layers[i], abuf);
            if (l != null)
                arrays[li++] = l;
        }

        this.vbuf = new VertexBuf(Utils.splice(arrays, 0, li));
    }

    public void clearfaces() {
        this.f.clear();
    }

    public FastMesh mkmesh() {
        if (f.isEmpty())
            throw (new RuntimeException("Tried to build empty mesh"));
        if (this.vbuf == null)
            mkvbuf();
        short[] idx = new short[f.size() * 3];
        int ii = 0;
        for (Face f : this.f) {
            idx[ii + 0] = f.v1.idx;
            idx[ii + 1] = f.v2.idx;
            idx[ii + 2] = f.v3.idx;
            ii += 3;
        }
        return (new FastMesh(this.vbuf, idx));
    }

    public boolean emptyp() {
        return (f.isEmpty());
    }
}
