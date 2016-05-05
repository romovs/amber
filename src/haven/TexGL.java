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

import static haven.GOut.checkerr;

import java.util.Collection;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import haven.glsl.ProgramContext;
import haven.glsl.ShaderMacro;
import haven.glsl.Tex2D;
import haven.glsl.Varying;

public abstract class TexGL extends Tex {
    public static boolean disableall = false;
    private static final WeakList<TexGL> active = new WeakList<TexGL>();
    protected TexOb t = null;
    protected boolean mipmap = false, centroid = false;
    protected int magfilter = GL.GL_NEAREST, minfilter = GL.GL_NEAREST, wrapmode = GL.GL_REPEAT;
    protected Coord tdim;
    private final Object idmon = new Object();
    private WeakList.Entry<TexGL> actref;
    private boolean setparams = true;

    public static class TexOb extends GLObject implements BGL.ID {
        private int id;

        public TexOb(GOut g) {
            super(g);
	        g.gl.bglCreate(this);
        }

        public void create(GL2 gl) {
            int[] buf = new int[1];
            gl.glGenTextures(1, buf, 0);
            this.id = buf[0];
        }

        protected void delete(BGL gl) {
            BGL.ID[] buf = {this};
            gl.glDeleteTextures(1, buf, 0);
        }

        public int glid() {
            return (id);
        }
    }

    public static final ShaderMacro mkcentroid = new ShaderMacro() {
        public void modify(ProgramContext prog) {
            Tex2D.get(prog).ipol = Varying.Interpol.CENTROID;
        }
    };

    public static GLState.TexUnit lbind(GOut g, TexGL tex) {
        GLState.TexUnit sampler = g.st.texalloc();
        sampler.act(g);
        try {
            g.gl.glBindTexture(GL.GL_TEXTURE_2D, tex.glid(g));
            return (sampler);
        } catch (Loading l) {
            sampler.free();
            throw (l);
        }
    }

    public static class TexDraw extends GLState {
        public static final Slot<TexDraw> slot = new Slot<TexDraw>(Slot.Type.DRAW, TexDraw.class, HavenPanel.global);
        private static final ShaderMacro[] nshaders = {Tex2D.mod};
        private static final ShaderMacro[] cshaders = {Tex2D.mod, mkcentroid};
        public final TexGL tex;
        private TexUnit sampler;

        public TexDraw(TexGL tex) {
            this.tex = tex;
        }

        public void prep(Buffer buf) {
            buf.put(slot, this);
        }

        public void reapply(GOut g) {
            BGL gl = g.gl;
            gl.glUniform1i(g.st.prog.uniform(Tex2D.tex2d), sampler.id);
        }

        public void apply(GOut g) {
            sampler = lbind(g, tex);
            reapply(g);
        }

        public void unapply(GOut g) {
            sampler.ufree(g);
            sampler = null;
        }

        public ShaderMacro[] shaders() {
        /* XXX: This combinatorial stuff does not seem quite right. */
            if (tex.centroid)
                return (cshaders);
            else
                return (nshaders);
        }

        public int capply() {
            return (100);
        }

        public int capplyfrom(GLState from) {
            if (from instanceof TexDraw)
                return (99);
            return (-1);
        }

        public void applyfrom(GOut g, GLState sfrom) {
            BGL gl = g.gl;
            TexDraw from = (TexDraw) sfrom;
            from.sampler.act(g);
            TexOb glid = tex.glid(g);
            sampler = from.sampler;
            from.sampler = null;
            gl.glBindTexture(GL.GL_TEXTURE_2D, glid);
            if (g.st.pdirty)
                reapply(g);
        }

        public String toString() {
            return ("TexDraw(" + tex + ")");
        }
    }

    private final TexDraw draw = new TexDraw(this);

    public GLState draw() {
        return (draw);
    }

    public static class TexClip extends GLState {
        public static final Slot<TexClip> slot = new Slot<TexClip>(Slot.Type.GEOM, TexClip.class, HavenPanel.global, TexDraw.slot);
        private static final ShaderMacro[] shaders = {Tex2D.clip};
        public final TexGL tex;
        private TexUnit sampler;

        public TexClip(TexGL tex) {
            this.tex = tex;
        }

        public void apply(GOut g) {
            TexDraw draw = g.st.get(TexDraw.slot);
            if (draw == null) {
                sampler = lbind(g, tex);
            } else {
                if (draw.tex != this.tex)
                    throw (new RuntimeException("TexGL does not support different clip and draw textures."));
            }
            if (g.gc.pref.alphacov.val) {
                g.gl.glEnable(GL2.GL_SAMPLE_ALPHA_TO_COVERAGE);
                g.gl.glEnable(GL2.GL_SAMPLE_ALPHA_TO_ONE);
            }
        }

        public void unapply(GOut g) {
            if (g.st.old(TexDraw.slot) == null) {
                sampler.act(g);
                sampler.free();
                sampler = null;
            }
            if (g.gc.pref.alphacov.val) {
                g.gl.glDisable(GL2.GL_SAMPLE_ALPHA_TO_COVERAGE);
                g.gl.glDisable(GL2.GL_SAMPLE_ALPHA_TO_ONE);
            }
        }

        public ShaderMacro[] shaders() {
            return (shaders);
        }

        public int capply() {
            return (100);
        }

        public int capplyfrom(GLState from) {
            if (from instanceof TexClip)
                return (99);
            return (-1);
        }

        public void applyfrom(GOut g, GLState sfrom) {
            TexDraw draw = g.st.get(TexDraw.slot), old = g.st.old(TexDraw.slot);
            if ((old != null) || (draw != null))
                throw (new RuntimeException("TexClip is somehow being transition even though there is a TexDraw"));
            BGL gl = g.gl;
            TexClip from = (TexClip) sfrom;
            from.sampler.act(g);
            TexOb glid = tex.glid(g);
            sampler = from.sampler;
            from.sampler = null;
            gl.glBindTexture(GL2.GL_TEXTURE_2D, glid);
        }

        public void prep(Buffer buf) {
            buf.put(slot, this);
        }

        public String toString() {
            return ("TexClip(" + tex + ")");
        }
    }

    private final TexClip clip = new TexClip(this);

    public GLState clip() {
        return (clip);
    }

    public TexGL(Coord sz, Coord tdim) {
        super(sz);
        this.tdim = tdim;
    }

    public TexGL(Coord sz) {
        this(sz, new Coord(nextp2(sz.x), nextp2(sz.y)));
    }

    protected abstract void fill(GOut gl);

    public static int num() {
        synchronized (active) {
            return (active.size());
        }
    }

    public static void setallparams() {
        synchronized (active) {
            for (TexGL tex : active)
                tex.setparams = true;
        }
    }

    protected void setparams(GOut g) {
        BGL gl = g.gl;
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, minfilter);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, magfilter);
        if ((minfilter == GL.GL_LINEAR_MIPMAP_LINEAR) && (g.gc.pref.anisotex.val >= 1))
            gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAX_ANISOTROPY_EXT, Math.max(g.gc.pref.anisotex.val, 1.0f));
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, wrapmode);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, wrapmode);
    }

    private void create(GOut g) {
        BGL gl = g.gl;
        t = new TexOb(g);
        gl.glBindTexture(GL.GL_TEXTURE_2D, t);
        setparams(g);
        try {
            fill(g);
        } catch (Loading l) {
            t.dispose();
            t = null;
            throw (l);
        }
        try {
            checkerr(gl);
        } catch (GOut.GLOutOfMemoryException e) {
            throw (new RuntimeException("Out of memory when creating texture " + this, e));
        }
    }

    public float tcx(int x) {
        return (((float) x) / ((float) tdim.x));
    }

    public float tcy(int y) {
        return (((float) y) / ((float) tdim.y));
    }

    @Deprecated
    public void mipmap() {
        mipmap = true;
        minfilter = GL.GL_LINEAR_MIPMAP_LINEAR;
        dispose();
    }

    public void magfilter(int filter) {
        magfilter = filter;
        setparams = true;
    }

    public void minfilter(int filter) {
        minfilter = filter;
        setparams = true;
    }

    public void wrapmode(int mode) {
        wrapmode = mode;
        setparams = true;
    }

    public TexOb glid(GOut g) {
        BGL gl = g.gl;
        synchronized (idmon) {
            if ((t != null) && (t.cur != g.curgl))
                dispose();
            if (t == null) {
                create(g);
                synchronized (active) {
                    actref = active.add2(this);
                }
            } else if (setparams) {
                gl.glBindTexture(GL.GL_TEXTURE_2D, t);
                setparams(g);
                setparams = false;
            }
            return (t);
        }
    }

    public void render(GOut g, Coord c, Coord ul, Coord br, Coord sz) {
        BGL gl = g.gl;
        g.st.prep(draw);
        g.apply();
        checkerr(gl);
        if (!disableall) {
            gl.glBegin(GL2.GL_QUADS);
            float l = ((float) ul.x) / ((float) tdim.x);
            float t = ((float) ul.y) / ((float) tdim.y);
            float r = ((float) br.x) / ((float) tdim.x);
            float b = ((float) br.y) / ((float) tdim.y);
            gl.glTexCoord2f(l, t);
            gl.glVertex3i(c.x, c.y, 0);
            gl.glTexCoord2f(r, t);
            gl.glVertex3i(c.x + sz.x, c.y, 0);
            gl.glTexCoord2f(r, b);
            gl.glVertex3i(c.x + sz.x, c.y + sz.y, 0);
            gl.glTexCoord2f(l, b);
            gl.glVertex3i(c.x, c.y + sz.y, 0);
            gl.glEnd();
            checkerr(gl);
        }
    }

    public void dispose() {
        synchronized (idmon) {
            if (t != null) {
                t.dispose();
                t = null;
                synchronized (active) {
                    actref.remove();
                    actref = null;
                }
            }
        }
    }

    /*
    public BufferedImage get(GOut g, boolean invert) {
	BGL gl = g.gl;
	g.state2d();
	g.apply();
	GLState.TexUnit s = g.st.texalloc();
	s.act(g);
	gl.glBindTexture(GL.GL_TEXTURE_2D, glid(g));
	byte[] buf = new byte[tdim.x * tdim.y * 4];
	gl.glGetTexImage(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(buf));
	s.free();
	GOut.checkerr(gl);
	if(invert) {
	    for(int y = 0; y < tdim.y / 2; y++) {
		int to = y * tdim.x * 4, bo = (tdim.y - y - 1) * tdim.x * 4;
		for(int o = 0; o < tdim.x * 4; o++, to++, bo++) {
		    byte t = buf[to];
		    buf[to] = buf[bo];
		    buf[bo] = t;
		}
	    }
	}
	WritableRaster raster = Raster.createInterleavedRaster(new DataBufferByte(buf, buf.length), tdim.x, tdim.y, 4 * tdim.x, 4, new int[] {0, 1, 2, 3}, null);
	return(new BufferedImage(TexI.glcm, raster, false, null));
    }

    public BufferedImage get(GOut g) {
	return(get(g, true));
    }
    */

    @Material.ResName("tex")
    public static class $tex implements Material.ResCons2 {
        public Material.Res.Resolver cons(final Resource res, Object... args) {
            final Indir<Resource> tres;
            final int tid;
            int a = 0;
            if (args[a] instanceof String) {
                tres = res.pool.load((String) args[a], (Integer) args[a + 1]);
                tid = (Integer) args[a + 2];
                a += 3;
            } else {
                tres = res.indir();
                tid = (Integer) args[a];
                a += 1;
            }
            boolean tclip = true;
            while (a < args.length) {
                String f = (String) args[a++];
                if (f.equals("a"))
                    tclip = false;
            }
            final boolean clip = tclip; /* ¦] */
            return (new Material.Res.Resolver() {
                public void resolve(Collection<GLState> buf) {
                    Tex tex;
                    TexR rt = tres.get().layer(TexR.class, tid);
                    if (rt != null) {
                        tex = rt.tex();
                    } else {
                        Resource.Image img = tres.get().layer(Resource.imgc, tid);
                        if (img != null) {
                            tex = img.tex();
                        } else {
                            throw (new RuntimeException(String.format("Specified texture %d for %s not found in %s", tid, res, tres)));
                        }
                    }
                    buf.add(tex.draw());
                    if (clip)
                        buf.add(tex.clip());
                }
            });
        }
    }

    static {
        Console.setscmd("texdis", new Console.Command() {
            public void run(Console cons, String[] args) {
                disableall = (Integer.parseInt(args[1]) != 0);
            }
        });
    }
}
