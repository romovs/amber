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
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import haven.GLProgram.VarID;

public class VertexBuf {
    public static final GLState.Slot<Binding> bound = new GLState.Slot<Binding>(GLState.Slot.Type.GEOM, Binding.class);
    public final AttribArray[] bufs;
    public final int num;

    public VertexBuf(AttribArray... bufs) {
        AttribArray[] na = new AttribArray[bufs.length];
        na[0] = bufs[0];
        int num = na[0].size();
        for (int i = 1; i < bufs.length; i++) {
            na[i] = bufs[i];
            if (na[i].size() != num)
                throw (new RuntimeException("Buffer sizes do not match"));
        }
        this.bufs = na;
        this.num = num;
    }

    public <T extends AttribArray> T buf(Class<T> type) {
        for (AttribArray a : bufs) {
            if (type.isInstance(a))
                return (type.cast(a));
        }
        return (null);
    }

    public abstract class Binding extends GLState {
        public void prep(GLState.Buffer buf) {
            buf.put(bound, this);
        }
    }

    public class MemBinding extends Binding {
        public void apply(GOut g) {
            for (int i = 0; i < bufs.length; i++) {
                if (bufs[i] instanceof GLArray)
                    ((GLArray) bufs[i]).bind(g, false);
            }
        }

        public void unapply(GOut g) {
            for (int i = 0; i < bufs.length; i++) {
                if (bufs[i] instanceof GLArray)
                    ((GLArray) bufs[i]).unbind(g);
            }
        }
    }

    public abstract static class AttribArray {
        public final int n;

        public AttribArray(int n) {
            this.n = n;
        }

        public abstract Buffer data();

        public abstract Buffer direct();

        public abstract int elsize();

        public int size() {
            Buffer b = data();
            b.rewind();
            return (b.capacity() / this.n);
        }

        /* XXX: It would be terribly nice if GLArray could be a
         * multiply inhereted class and these could be put in it
         * instead; but alas, this is Java. QQ */
        private GLBuffer bufobj;
        private int bufmode = GL.GL_STATIC_DRAW;
        private boolean update = false;

        public void bindvbo(GOut g) {
            BGL gl = g.gl;
            synchronized (this) {
                if ((bufobj != null) && (bufobj.cur != g.curgl))
                    dispose();
                if (bufobj == null) {
                    bufobj = new GLBuffer(g);
                    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufobj);
                    Buffer data = data();
                    data.rewind();
                    gl.glBufferData(GL.GL_ARRAY_BUFFER, data.remaining() * elsize(), data, bufmode);
                    GOut.checkerr(gl);
                    update = false;
                } else if (update) {
                    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufobj);
                    Buffer data = data();
                    data.rewind();
                    gl.glBufferData(GL.GL_ARRAY_BUFFER, data.remaining() * elsize(), data, bufmode);
                    update = false;
                } else {
                    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufobj);
                }
            }
        }

        public void vbomode(int mode) {
            bufmode = mode;
            dispose();
        }

        public void dispose() {
            synchronized (this) {
                if (bufobj != null) {
                    bufobj.dispose();
                    bufobj = null;
                }
            }
        }

        public void update() {
            update = true;
        }
    }

    public static interface GLArray {
        public void bind(GOut g, boolean asvbo);

        public void unbind(GOut g);

        public Object progid(GOut g);
    }

    public abstract static class FloatArray extends AttribArray {
        public FloatBuffer data;

        public FloatArray(int n, FloatBuffer data) {
            super(n);
            data.rewind();
            if (data.capacity() % n != 0)
                throw (new RuntimeException(String.format("float-array length %d does not match element count %d", data.capacity(), n)));
            this.data = data;
        }

        public FloatBuffer data() {
            return (data);
        }

        public FloatBuffer direct() {
            if (!data.isDirect())
                data = Utils.bufcp(data);
            return (data);
        }

        public int elsize() {
            return (4);
        }
    }

    public abstract static class IntArray extends AttribArray {
        public IntBuffer data;

        public IntArray(int n, IntBuffer data) {
            super(n);
            data.rewind();
            if (data.capacity() % n != 0)
                throw (new RuntimeException(String.format("int-array length %d does not match element count %d", data.capacity(), n)));
            this.data = data;
        }

        public IntBuffer data() {
            return (data);
        }

        public IntBuffer direct() {
            if (!data.isDirect())
                data = Utils.bufcp(data);
            return (data);
        }

        public int elsize() {
            return (4);
        }
    }

    @ResName("pos")
    public static class VertexArray extends FloatArray implements GLArray, MorphedMesh.MorphArray {
        public VertexArray(FloatBuffer data) {
            super(3, data);
        }

        public VertexArray(Resource res, Message buf, int nv) {
            this(loadbuf(Utils.wfbuf(nv * 3), buf));
        }

        public VertexArray dup() {
            return (new VertexArray(Utils.bufcp(data)));
        }

        public void bind(GOut g, boolean asvbo) {
            BGL gl = g.gl;
            if (asvbo) {
                bindvbo(g);
                gl.glVertexPointer(3, GL.GL_FLOAT, 0, 0);
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, null);
            } else {
                gl.glVertexPointer(3, GL.GL_FLOAT, 0, direct(), 0);
            }
            gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        }

        public void unbind(GOut g) {
            g.gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        }

        public Object progid(GOut g) {
            return (null);
        }

        /* XXX: It feels very much like morphing should be layered
         * strictly above VertexBuf, but I can't quite see an
         * alternative to this at this point. */
        public MorphedMesh.MorphType morphtype() {
            return (MorphedMesh.MorphType.POS);
        }
    }

    @ResName("nrm")
    public static class NormalArray extends FloatArray implements GLArray, MorphedMesh.MorphArray {
        public NormalArray(FloatBuffer data) {
            super(3, data);
        }

        public NormalArray(Resource res, Message buf, int nv) {
            this(loadbuf(Utils.wfbuf(nv * 3), buf));
        }

        public NormalArray dup() {
            return (new NormalArray(Utils.bufcp(data)));
        }

        public void bind(GOut g, boolean asvbo) {
            BGL gl = g.gl;
            if (asvbo) {
                bindvbo(g);
                gl.glNormalPointer(GL.GL_FLOAT, 0, 0);
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, null);
            } else {
                gl.glNormalPointer(GL.GL_FLOAT, 0, direct(), 0);
            }
            gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
        }

        public void unbind(GOut g) {
            g.gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
        }

        public Object progid(GOut g) {
            return (null);
        }

        public MorphedMesh.MorphType morphtype() {
            return (MorphedMesh.MorphType.DIR);
        }
    }

    @ResName("col")
    public static class ColorArray extends FloatArray implements GLArray {
        public ColorArray(FloatBuffer data) {
            super(4, data);
        }

        public ColorArray(Resource res, Message buf, int nv) {
            this(loadbuf(Utils.wfbuf(nv * 4), buf));
        }

        public ColorArray dup() {
            return (new ColorArray(Utils.bufcp(data)));
        }

        public void bind(GOut g, boolean asvbo) {
            BGL gl = g.gl;
            if (asvbo) {
                bindvbo(g);
                gl.glColorPointer(4, GL.GL_FLOAT, 0, 0);
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, null);
            } else {
                gl.glColorPointer(4, GL.GL_FLOAT, 0, direct(), 0);
            }
            gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
        }

        public void unbind(GOut g) {
            g.gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
        }

        public Object progid(GOut g) {
            return (null);
        }
    }

    @ResName("tex")
    public static class TexelArray extends FloatArray implements GLArray {
        public TexelArray(FloatBuffer data) {
            super(2, data);
        }

        public TexelArray(Resource res, Message buf, int nv) {
            this(loadbuf(Utils.wfbuf(nv * 2), buf));
        }

        public TexelArray dup() {
            return (new TexelArray(Utils.bufcp(data)));
        }

        public void bind(GOut g, boolean asvbo) {
            BGL gl = g.gl;
            if (asvbo) {
                bindvbo(g);
                gl.glTexCoordPointer(2, GL.GL_FLOAT, 0, 0);
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, null);
            } else {
                gl.glTexCoordPointer(2, GL.GL_FLOAT, 0, direct(), 0);
            }
            gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
        }

        public void unbind(GOut g) {
            g.gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
        }

        public Object progid(GOut g) {
            return (null);
        }
    }

    public static class NamedFloatArray extends FloatArray implements GLArray {
        public final haven.glsl.Attribute attr;
        private VarID bound = null;

        public NamedFloatArray(int n, FloatBuffer data, haven.glsl.Attribute attr) {
            super(n, data);
            this.attr = attr;
        }

        public void bind(GOut g, boolean asvbo) {
            if ((bound = g.st.prog.cattrib(attr)) != null) {
                BGL gl = g.gl;
                if (asvbo) {
                    bindvbo(g);
                    gl.glVertexAttribPointer(bound, n, GL2.GL_FLOAT, false, 0, 0);
                    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, null);
                } else {
                    gl.glVertexAttribPointer(bound, n, GL2.GL_FLOAT, false, 0, direct(), 0);
                }
                gl.glEnableVertexAttribArray(bound);
            }
        }

        public void unbind(GOut g) {
            if (bound != null) {
                g.gl.glDisableVertexAttribArray(bound);
                bound = null;
            }
        }

        public Object progid(GOut g) {
        /* XXX: This is not a good ID, as it doesn't intern
         * locations in various programs. */
            return (g.st.prog.cattrib(attr));
        }
    }

    public static class Vec1Array extends NamedFloatArray implements GLArray {
        public Vec1Array(FloatBuffer data, haven.glsl.Attribute attr) {
            super(1, data, attr);
        }
    }

    public static class Vec2Array extends NamedFloatArray implements GLArray {
        public Vec2Array(FloatBuffer data, haven.glsl.Attribute attr) {
            super(2, data, attr);
        }
    }

    public static class Vec3Array extends NamedFloatArray implements GLArray {
        public Vec3Array(FloatBuffer data, haven.glsl.Attribute attr) {
            super(3, data, attr);
        }
    }

    public static class Vec4Array extends NamedFloatArray implements GLArray {
        public Vec4Array(FloatBuffer data, haven.glsl.Attribute attr) {
            super(4, data, attr);
        }
    }

    public void dispose() {
        for (AttribArray buf : bufs)
            buf.dispose();
    }

    @dolda.jglob.Discoverable
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ResName {
        public String value();
    }

    public interface ArrayCons {
        public void cons(Collection<AttribArray> dst, Resource res, Message buf, int nvert);
    }

    public static FloatBuffer loadbuf(FloatBuffer dst, Message buf) {
        for (int i = 0; i < dst.capacity(); i++)
            dst.put(i, buf.float32());
        return (dst);
    }

    private static final Map<String, ArrayCons> rnames = new TreeMap<String, ArrayCons>();

    static {
        for (Class<?> cl : dolda.jglob.Loader.get(ResName.class).classes()) {
            String nm = cl.getAnnotation(ResName.class).value();
            if (ArrayCons.class.isAssignableFrom(cl)) {
                try {
                    rnames.put(nm, cl.asSubclass(ArrayCons.class).newInstance());
                } catch (InstantiationException e) {
                    throw (new Error(e));
                } catch (IllegalAccessException e) {
                    throw (new Error(e));
                }
            } else if (AttribArray.class.isAssignableFrom(cl)) {
                final java.lang.reflect.Constructor<? extends AttribArray> cons;
                try {
                    cons = cl.asSubclass(AttribArray.class).getConstructor(Resource.class, Message.class, Integer.TYPE);
                } catch (NoSuchMethodException e) {
                    throw (new Error("No proper constructor for res-consable vertex-array class " + cl, e));
                }
                rnames.put(nm, new ArrayCons() {
                    public void cons(Collection<AttribArray> dst, Resource res, Message buf, int num) {
                        dst.add(Utils.construct(cons, res, buf, num));
                    }
                });
            } else {
                throw (new Error("Illegal vertex-array constructor class: " + cl));
            }
        }
    }

    @SuppressWarnings("serial")
	@Resource.LayerName("vbuf2")
    public static class VertexRes extends Resource.Layer {
        public transient final VertexBuf b;

        private VertexRes(Resource res, VertexBuf b) {
            res.super();
            this.b = b;
        }

        public VertexRes(Resource res, Message buf) {
            res.super();
            List<AttribArray> bufs = new LinkedList<AttribArray>();
            @SuppressWarnings("unused")
			int fl = buf.uint8();
            int num = buf.uint16();
            while (!buf.eom()) {
                String nm = buf.string();
                ArrayCons cons = rnames.get(nm);
                if (cons == null)
                    throw (new Resource.LoadException("Unknown vertex-array name: " + nm, res));
                cons.cons(bufs, res, buf, num);
            }
            this.b = new VertexBuf(bufs.toArray(new AttribArray[0]));
        }

        public void init() {
        }
    }

    @Resource.LayerName("vbuf")
    public static class Legacy implements Resource.LayerFactory<VertexRes> {
        public VertexRes cons(Resource res, Message buf) {
            ArrayList<AttribArray> bufs = new ArrayList<AttribArray>();
            @SuppressWarnings("unused")
			int fl = buf.uint8();
            int num = buf.uint16();
            while (!buf.eom()) {
                int id = buf.uint8();
                if (id == 0) {
                    FloatBuffer data = Utils.wfbuf(num * 3);
                    for (int i = 0; i < num * 3; i++)
                        data.put((float) buf.cpfloat());
                    bufs.add(new VertexArray(data));
                } else if (id == 1) {
                    FloatBuffer data = Utils.wfbuf(num * 3);
                    for (int i = 0; i < num * 3; i++)
                        data.put((float) buf.cpfloat());
                    bufs.add(new NormalArray(data));
                } else if (id == 2) {
                    FloatBuffer data = Utils.wfbuf(num * 2);
                    for (int i = 0; i < num * 2; i++)
                        data.put((float) buf.cpfloat());
                    bufs.add(new TexelArray(data));
                } else if (id == 3) {
                    int mba = buf.uint8();
                    IntBuffer ba = Utils.wibuf(num * mba);
                    for (int i = 0; i < num * mba; i++)
                        ba.put(-1);
                    ba.rewind();
                    FloatBuffer bw = Utils.wfbuf(num * mba);
                    int[] na = new int[num];
                    List<String> bones = new ArrayList<String>();
                    while (true) {
                        String bone = buf.string();
                        if (bone.length() == 0)
                            break;
                        int bidx = bones.size();
                        bones.add(bone);
                        while (true) {
                            int run = buf.uint16();
                            int st = buf.uint16();
                            if (run == 0)
                                break;
                            for (int i = 0; i < run; i++) {
                                float w = (float) buf.cpfloat();
                                int v = i + st;
                                int cna = na[v]++;
                                if (cna >= mba)
                                    continue;
                                bw.put(v * mba + cna, w);
                                ba.put(v * mba + cna, bidx);
                            }
                        }
                    }
                    PoseMorph.normweights(bw, ba, mba);
                    bufs.add(new PoseMorph.BoneArray(mba, ba, bones.toArray(new String[0])));
                    bufs.add(new PoseMorph.WeightArray(mba, bw));
                }
            }
            return (new VertexRes(res, new VertexBuf(bufs.toArray(new AttribArray[0]))));
        }
    }
}
