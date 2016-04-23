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

import haven.glsl.ShaderMacro.Program;
import java.util.*;
import java.nio.*;
import javax.media.opengl.*;

public class FastMesh implements FRendered, Rendered.Instanced, Disposable {
    public static final GLState.Slot<GLState> vstate = new GLState.Slot<GLState>(GLState.Slot.Type.SYS, GLState.class);
    public final VertexBuf vert;
    public final ShortBuffer indb;
    public final int num, lo, hi;
    public FastMesh from;
    private Compiler compiler;
    private Coord3f nb, pb;

    public FastMesh(VertexBuf vert, ShortBuffer ind) {
        this.vert = vert;
        num = ind.capacity() / 3;
        if(ind.capacity() != num * 3)
            throw(new RuntimeException("Invalid index array length"));
        this.indb = ind;
        int lo = 65536, hi = 0;
        for(int i = 0; i < ind.capacity(); i++) {
            int idx = ((int)ind.get(i)) & 0xffff;
            lo = Math.min(lo, idx);
            hi = Math.max(hi, idx);
        }
        this.lo = (lo == 65536)?0:lo; this.hi = hi;
    }

    public FastMesh(VertexBuf vert, short[] ind) {
        this(vert, Utils.bufcp(ind));
    }

    public FastMesh(FastMesh from, VertexBuf vert) {
        this.from = from;
        if(from.vert.num != vert.num)
            throw(new RuntimeException("V-buf sizes must match"));
        this.vert = vert;
        this.indb = from.indb;
        this.num = from.num;
        this.lo = from.lo;
        this.hi = from.hi;
    }

    public static abstract class Compiled {
        public abstract void draw(GOut g);
        public abstract void dispose();
        public void prepare(GOut g) {}
    }

    public abstract class Compiler {
        private GLProgram[] kcache = new GLProgram[0];
        private Compiled[] vcache = new Compiled[0];
        private Object[] ids = new Object[0];

        private Object[] getid(GOut g) {
            ArrayList<Object> id = new ArrayList<Object>();
            for(int i = 0; i < vert.bufs.length; i++) {
                if(vert.bufs[i] instanceof VertexBuf.GLArray)
                    id.add(((VertexBuf.GLArray)vert.bufs[i]).progid(g));
                else
                    id.add(null);
            }
	    /* XXX: Probably, each auto-inst should have to be ID'd in
	     * some meaningful way, but I'm not currently sure what
	     * would consitute a proper ID. */
            id.add(g.st.prog.autoinst.length > 0);
            return(ArrayIdentity.intern(id.toArray(new Object[0])));
        }

        private Compiled last = null;
        public Compiled get(GOut g) {
            if(last != null)
                last.prepare(g);
            g.apply();
            GLProgram prog = g.st.prog;
            int i;
            for(i = 0; i < kcache.length; i++) {
                if(kcache[i] == prog)
                    return(last = vcache[i]);
            }
            Object[] id = getid(g);
            Compiled ret;
            create: {
                int o;
                for(o = 0; o < kcache.length; o++) {
                    if(ids[o] == id) {
                        ret = vcache[o];
                        break create;
                    }
                }
                ret = create(g);
            }
            kcache = Utils.extend(kcache, i + 1);
            vcache = Utils.extend(vcache, i + 1);
            ids = Utils.extend(ids, i + 1);
            kcache[i] = prog;
            vcache[i] = ret;
            ids[i] = id;
            return(last = ret);
        }

        public abstract Compiled create(GOut g);

        public void dispose() {
            for(Compiled c : vcache)
                c.dispose();
            kcache = new GLProgram[0];
            vcache = new Compiled[0];
        }
    }

    public class DLCompiler extends Compiler {
        public class DLCompiled extends Compiled {
            private DisplayList list;

            public void draw(GOut g) {
                BGL gl = g.gl;
                if((list != null) && (list.cur != g.curgl)) {
                    list.dispose();
                    list = null;
                }
                if(list == null) {
                    list = new DisplayList(g);
                    gl.glNewList(list, GL2.GL_COMPILE);
                    cdraw(g);
                    gl.glEndList();
                }
                gl.glCallList(list);
            }

            public void dispose() {
                if(list != null) {
                    list.dispose();
                    list = null;
                }
            }
        }

        public DLCompiled create(GOut g) {return(new DLCompiled());}
    }

    public class VAOState extends GLState {
        private GLBuffer ind;
        private GLVertexArray vao;

        private void bindindbo(GOut g) {
            BGL gl = g.gl;
            if((ind != null) && (ind.cur != g.curgl)) {
                ind.dispose();
                ind = null;
            }
            if(ind == null) {
                ind = new GLBuffer(g);
                gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ind);
                indb.rewind();
                gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, indb.remaining() * 2, indb, GL.GL_STATIC_DRAW);
                GOut.checkerr(gl);
            } else {
                gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ind);
            }
        }

        public void apply(GOut g) {
            BGL gl = g.gl;
            if((vao != null) && (vao.cur != g.curgl)) {
                vao.dispose();
                vao = null;
            }
            if(vao == null) {
                vao = new GLVertexArray(g);
                gl.glBindVertexArray(vao);
                for(VertexBuf.AttribArray buf : vert.bufs) {
                    if(buf instanceof VertexBuf.GLArray)
                        ((VertexBuf.GLArray)buf).bind(g, true);
                }
                bindindbo(g);
            } else {
                gl.glBindVertexArray(vao);
            }
        }

        public void unapply(GOut g) {
            BGL gl = g.gl;
            gl.glBindVertexArray(null);
            gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, null);
        }

        public int capplyfrom(GLState o) {
            if(o instanceof VAOState)
                return(1);
            return(-1);
        }

        public void applyfrom(GOut g, GLState from) {
            apply(g);
        }

        public void dispose() {
            if(vao != null) {
                vao.dispose();
                vao = null;
            }
            if(ind != null) {
                ind.dispose();
                ind = null;
            }
        }

        public void prep(Buffer buf) {
            buf.put(vstate, this);
        }
    }

    public class VAOCompiler extends Compiler {
        public class VAOCompiled extends Compiled {
            private VAOState st = new VAOState();

            public void prepare(GOut g) {
                GLState cur = g.st.cur(vstate);
                if(cur != null)
                    g.state(cur);
            }

            public void draw(GOut g) {
                BGL gl = g.gl;
                g.st.apply(g, vstate, st);
                gl.glDrawRangeElements(GL.GL_TRIANGLES, lo, hi, num * 3, GL.GL_UNSIGNED_SHORT, 0);
            }

            public boolean drawinst(GOut g, List<GLState.Buffer> inst) {
                BGL gl = g.gl;
                g.st.apply(g, vstate, st);
                g.st.bindiarr(g, inst);
                gl.glDrawElementsInstanced(GL.GL_TRIANGLES, num * 3, GL.GL_UNSIGNED_SHORT, 0, inst.size());
                g.st.unbindiarr(g);
                return(true);
            }

            public void dispose() {
                st.dispose();
            }
        }

        public VAOCompiled create(GOut g) {return(new VAOCompiled());}
    }

    private void cbounds() {
        Coord3f nb = null, pb = null;
        VertexBuf.VertexArray vbuf = null;
        for(VertexBuf.AttribArray buf : vert.bufs) {
            if(buf instanceof VertexBuf.VertexArray) {
                vbuf = (VertexBuf.VertexArray)buf;
                break;
            }
        }
        for(int i = 0; i < indb.capacity(); i++) {
            int vi = indb.get(i) * 3;
            float x = vbuf.data.get(vi), y = vbuf.data.get(vi + 1), z = vbuf.data.get(vi + 2);
            if(nb == null) {
                nb = new Coord3f(x, y, z);
                pb = new Coord3f(x, y, z);
            } else {
                nb.x = Math.min(nb.x, x); pb.x = Math.max(pb.x, x);
                nb.y = Math.min(nb.y, y); pb.y = Math.max(pb.y, y);
                nb.z = Math.min(nb.z, z); pb.z = Math.max(pb.z, z);
            }
        }
        this.nb = nb;
        this.pb = pb;
    }

    public Coord3f nbounds() {
        if(nb == null) cbounds();
        return(nb);
    }
    public Coord3f pbounds() {
        if(pb == null) cbounds();
        return(pb);
    }

    public void cdraw(GOut g) {
        g.apply();
        indb.rewind();
        for(int i = 0; i < vert.bufs.length; i++) {
            if(vert.bufs[i] instanceof VertexBuf.GLArray)
                ((VertexBuf.GLArray)vert.bufs[i]).bind(g, false);
        }
        g.gl.glDrawRangeElements(GL.GL_TRIANGLES, lo, hi, num * 3, GL.GL_UNSIGNED_SHORT, indb);
        for(int i = 0; i < vert.bufs.length; i++) {
            if(vert.bufs[i] instanceof VertexBuf.GLArray)
                ((VertexBuf.GLArray)vert.bufs[i]).unbind(g);
        }
    }

    private GLSettings.MeshMode curmode = null;
    private Compiler compiler(GLConfig gc) {
        if(compile()) {
            if(curmode != gc.pref.meshmode.val) {
                if(compiler != null) {
                    compiler.dispose();
                    compiler = null;
                }
                switch(gc.pref.meshmode.val) {
                    case VAO:
                        compiler = new VAOCompiler();
                        break;
                    case DLIST:
                        compiler = new DLCompiler();
                        break;
				default:
					break;
                }
                curmode = gc.pref.meshmode.val;
            }
        } else if(compiler != null) {
            compiler.dispose();
            compiler = null;
            curmode = null;
        }
        return(compiler);
    }

    public void draw(GOut g) {
        BGL gl = g.gl;
        Compiler compiler = compiler(g.gc);
        if(compiler != null) {
            compiler.get(g).draw(g);
        } else {
            cdraw(g);
        }
        GOut.checkerr(gl);
    }

    protected boolean compile() {
        return(true);
    }

    public boolean drawinst(GOut g, List<GLState.Buffer> st) {
        Compiler compiler = compiler(g.gc);
        if(!(compiler instanceof VAOCompiler))
            return(false);
        if(!g.st.inststate(st))
            return(false);
        return(((VAOCompiler.VAOCompiled)compiler.get(g)).drawinst(g, st));
    }

    /* XXX: One might start to question if it isn't about time to
     * dispose of display-list drawing. */
    private class Instanced implements Rendered, FRendered, Disposable {
        final List<GLState.Buffer> instances;
        final VAOCompiler compiler;
        final Map<Program, Arrays> arrays = new HashMap<Program, Arrays>();

        class Arrays {
            final Program prog;
            final GLBuffer[] data;

            Arrays(GOut g, Program prog) {
                this.prog = prog;
                this.data = new GLBuffer[prog.autoinst.length];
                for(int i = 0; i < data.length; i++) {
                    data[i] = new GLBuffer(g);
                    prog.autoinst[i].filliarr(g, instances, data[i]);
                }
            }

            void bind(GOut g) {
                for(int i = 0; i < data.length; i++)
                    prog.autoinst[i].bindiarr(g, data[i]);
            }

            void unbind(GOut g) {
                for(int i = 0; i < data.length; i++)
                    prog.autoinst[i].unbindiarr(g, data[i]);
            }

            void dispose() {
                for(GLBuffer buf : data)
                    buf.dispose();
            }
        }

        Instanced(VAOCompiler compiler, List<GLState.Buffer> instances) {
            this.compiler = compiler;
            this.instances = instances;
        }

        public void draw(GOut g) {
            BGL gl = g.gl;
            g.st.apply(g, vstate, ((VAOCompiler.VAOCompiled)compiler.get(g)).st);
            Arrays ar = arrays.get(g.st.prog);
            if(ar == null) {
                arrays.put(g.st.prog, ar = new Arrays(g, g.st.prog));
                if(arrays.size() > 10)
                    System.err.println("warning: creating very many instance arrays for " + FastMesh.this);
            }
            ar.bind(g);
            gl.glDrawElementsInstanced(GL.GL_TRIANGLES, num * 3, GL.GL_UNSIGNED_SHORT, 0, instances.size());
            ar.unbind(g);
        }

        public void drawflat(GOut g) {
            draw(g);
        }

        public boolean setup(RenderList r) {
            throw(new RuntimeException("Instanced meshes are transformed into, not set up"));
        }

        public void dispose() {
            for(Arrays ar : arrays.values())
                ar.dispose();
        }
    }

    public Rendered instanced(GLConfig gc, List<GLState.Buffer> st) {
        Compiler compiler = compiler(gc);
        if(!(compiler instanceof VAOCompiler))
            return(null);
        VAOCompiler vc = (VAOCompiler)compiler;
        return(new Instanced((VAOCompiler)compiler, new ArrayList<GLState.Buffer>(st)));
    }

    public void dispose() {
        if(compiler != null) {
            compiler.dispose();
            compiler = null;
        }
        vert.dispose();
    }

    public void drawflat(GOut g) {
        draw(g);
        GOut.checkerr(g.gl);
    }

    public boolean setup(RenderList r) {
        return(true);
    }

    public static class ResourceMesh extends FastMesh {
        public final int id;
        public final Resource res;

        public ResourceMesh(VertexBuf vert, short[] ind, MeshRes info) {
            super(vert, ind);
            this.id = info.id;
            this.res = info.getres();
        }

        public String toString() {
            return("FastMesh(" + res.name + ", " + id + ")");
        }
    }

    @SuppressWarnings("serial")
	@Resource.LayerName("mesh")
    public static class MeshRes extends Resource.Layer implements Resource.IDLayer<Integer> {
        public transient FastMesh m;
        public transient Material.Res mat;
        public final Map<String, String> rdat;
        private transient short[] tmp;
        public final int id, ref;
        private int matid;

        public MeshRes(Resource res, Message buf) {
            res.super();
            int fl = buf.uint8();
            int num = buf.uint16();
            matid = buf.int16();
            if((fl & 2) != 0) {
                id = buf.int16();
            } else {
                id = -1;
            }
            if((fl & 4) != 0) {
                ref = buf.int16();
            } else {
                ref = -1;
            }
            Map<String, String> rdat = new HashMap<String, String>();
            if((fl & 8) != 0) {
                while(true) {
                    String k = buf.string();
                    if(k.equals(""))
                        break;
                    rdat.put(k, buf.string());
                }
            }
            this.rdat = Collections.unmodifiableMap(rdat);
            if((fl & ~15) != 0)
                throw(new Resource.LoadException("Unsupported flags in fastmesh: " + fl, getres()));
            short[] ind = new short[num * 3];
            for(int i = 0; i < num * 3; i++)
                ind[i] = (short)buf.uint16();
            this.tmp = ind;
        }

        public void init() {
            VertexBuf v = getres().layer(VertexBuf.VertexRes.class).b;
            this.m = new ResourceMesh(v, this.tmp, this);
            this.tmp = null;
            if(matid >= 0) {
                for(Material.Res mr : getres().layers(Material.Res.class)) {
                    if(mr.id == matid)
                        this.mat = mr;
                }
                if(this.mat == null)
                    throw(new Resource.LoadException("Could not find specified material: " + matid, getres()));
            }
        }

        public Integer layerid() {
            return(id);
        }
    }
}
