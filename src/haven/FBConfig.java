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

import java.util.Collection;
import java.util.LinkedList;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import haven.GLFrameBuffer.Attachment;
import haven.GLProgram.VarID;
import haven.glsl.Cons;
import haven.glsl.Expression;
import haven.glsl.Macro1;
import haven.glsl.MiscLib;
import haven.glsl.ProgramContext;
import haven.glsl.ShaderMacro;
import haven.glsl.Tex2D;
import haven.glsl.Type;
import haven.glsl.Uniform;

public class FBConfig {
    public final PView.ConfContext ctx;
    public Coord sz;
    public boolean hdr, tdepth;
    public int ms = 1;
    public GLFrameBuffer fb;
    public PView.RenderState wnd;
    public Attachment color[], depth;
    public GLState state;
    private RenderTarget[] tgts = new RenderTarget[0];
    private ResolveFilter[] res = new ResolveFilter[0];
    private GLState resp;

    public FBConfig(PView.ConfContext ctx, Coord sz) {
        this.ctx = ctx;
        this.sz = sz;
    }

    public boolean cleanp() {
        if (hdr || tdepth || (ms > 1))
            return (false);
        for (int i = 0; i < tgts.length; i++) {
            if (tgts[i] != null)
                return (false);
        }
        for (ResolveFilter rf : res) {
            if (!rf.cleanp())
                return (false);
        }
        return (true);
    }

    private static final ShaderMacro[] nosh = new ShaderMacro[0];

    private void create() {
        Collection<Attachment> color = new LinkedList<Attachment>();
        Attachment depth;
        Collection<ShaderMacro> shb = new LinkedList<ShaderMacro>();
        Collection<GLState> stb = new LinkedList<GLState>();
        {
            int fmt = hdr ? GL.GL_RGBA16F : GL.GL_RGBA;
            if (ms <= 1)
                color.add(Attachment.mk(new TexE(sz, fmt, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE)));
            else
                color.add(Attachment.mk(new TexMSE(sz, ms, fmt, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE)));
        }
        if (tdepth) {
            if (ms <= 1)
                depth = Attachment.mk(new TexE(sz, GL2.GL_DEPTH_COMPONENT, GL2.GL_DEPTH_COMPONENT, GL.GL_UNSIGNED_INT));
            else
                depth = Attachment.mk(new TexMSE(sz, ms, GL2.GL_DEPTH_COMPONENT, GL2.GL_DEPTH_COMPONENT, GL.GL_UNSIGNED_INT));
        } else {
            depth = Attachment.mk(new GLFrameBuffer.RenderBuffer(sz, GL2.GL_DEPTH_COMPONENT, ms));
        }
        for (int i = 0; i < tgts.length; i++) {
            if (tgts[i] != null) {
                color.add(tgts[i].maketex(this));
                GLState st = tgts[i].state(this, i + 1);
                if (st != null)
                    stb.add(st);
                ShaderMacro code = tgts[i].code(this, i + 1);
                if (code != null)
                    shb.add(code);
            }
        }
        this.color = color.toArray(new Attachment[0]);
        this.depth = depth;
    /* XXX: Shaders should be canonized and cached to avoid
     * creation of unnecessary identical programs when
	 * configurations change. */
        final ShaderMacro[] shaders;
        if (shb.size() < 1)
            shaders = nosh;
        else
            shaders = shb.toArray(new ShaderMacro[0]);
        this.fb = new GLFrameBuffer(this.color, this.depth) {
            public ShaderMacro[] shaders() {
                return (shaders);
            }
        };
        this.wnd = new PView.RenderState() {
            public Coord ul() {
                return (Coord.z);
            }

            public Coord sz() {
                return (sz);
            }
        };
        stb.add(fb);
        stb.add(wnd);
        this.state = GLState.compose(stb.toArray(new GLState[0]));
        if (res.length > 0) {
            ShaderMacro[] resp = new ShaderMacro[res.length];
            for (int i = 0; i < res.length; i++)
                resp[i] = res[i].code(this);
            resp = ArrayIdentity.intern(resp);
            this.resp = new States.AdHoc(resp) {
                public void apply(GOut g) {
                    for (ResolveFilter f : res)
                        f.apply(FBConfig.this, g);
                }

                public void unapply(GOut g) {
                    for (ResolveFilter f : res)
                        f.unapply(FBConfig.this, g);
                }
            };
        }
    }

    private static <T> boolean hasuo(T[] a, T[] b) {
        outer:
        for (T ae : a) {
            for (T be : b) {
                if (Utils.eq(ae, be))
                    continue outer;
            }
            return (false);
        }
        return (true);
    }

    public static boolean equals(FBConfig a, FBConfig b) {
        if (!a.sz.equals(b.sz))
            return (false);
        if ((a.hdr != b.hdr) || (a.tdepth != b.tdepth))
            return (false);
        if (a.ms != b.ms)
            return (false);
        if (!hasuo(a.tgts, b.tgts) || !hasuo(b.tgts, a.tgts))
            return (false);
        if (!hasuo(a.res, b.res) || !hasuo(b.res, a.res))
            return (false);
        return (true);
    }

    private void subsume(FBConfig last) {
        fb = last.fb;
        wnd = last.wnd;
        color = last.color;
        depth = last.depth;
        tgts = last.tgts;
        res = last.res;
        resp = last.resp;
        state = last.state;
    }

    public void fin(FBConfig last) {
        if (ms <= 1)
            add(new Resolve1());
        else
            add(new ResolveMS(ms));
        if (equals(this, last)) {
            subsume(last);
            return;
        }
        if (last.fb != null)
            last.fb.dispose();
        if (cleanp())
            return;
        create();
    }

    public void resolve(GOut g) {
        if (fb != null) {
            for (ResolveFilter rf : res)
                rf.prepare(this, g);
            g.ftexrect(Coord.z, sz, resp);
        }
    }

    public RenderTarget add(RenderTarget tgt) {
        if (tgt == null)
            throw (new NullPointerException());
        for (RenderTarget p : tgts) {
            if (Utils.eq(tgt, p))
                return (p);
        }
        int i;
        for (i = 0; i < tgts.length;) {
            if (tgts[i] == null)
                tgts[i] = tgt;
            return (tgt);
        }
        tgts = Utils.extend(tgts, i + 1);
        tgts[i] = tgt;
        return (tgt);
    }

    public ResolveFilter add(ResolveFilter rf) {
        if (rf == null)
            throw (new NullPointerException());
        for (ResolveFilter p : res) {
            if (Utils.eq(rf, p))
                return (p);
        }
        int l = res.length;
        res = Utils.extend(res, l + 1);
        res[l] = rf;
        return (rf);
    }

    public static abstract class RenderTarget {
        public Attachment tex;

        public Attachment maketex(FBConfig cfg) {
            if (cfg.ms <= 1)
                return (tex = Attachment.mk(new TexE(cfg.sz, GL.GL_RGBA, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE)));
            else
                return (tex = Attachment.mk(new TexMSE(cfg.sz, cfg.ms, GL.GL_RGBA, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE)));
        }

        public GLState state(FBConfig cfg, int id) {
            return (null);
        }

        public ShaderMacro code(FBConfig cfg, int id) {
            return (null);
        }
    }

    public static final Uniform numsamples = new Uniform.AutoApply(Type.INT) {
        public void apply(GOut g, VarID loc) {
            g.gl.glUniform1i(loc, ((PView.ConfContext) g.st.get(PView.ctx)).cur.ms);
        }
    };

    public interface ResolveFilter {
        public boolean cleanp();

        public void prepare(FBConfig cfg, GOut g);

        public ShaderMacro code(FBConfig cfg);

        public void apply(FBConfig cfg, GOut g);

        public void unapply(FBConfig cfg, GOut g);
    }

    private static class Resolve1 implements ResolveFilter {
        public void prepare(FBConfig cfg, GOut g) {
        }

        public boolean cleanp() {
            return (true);
        }

        private static final Uniform ctex = new Uniform(Type.SAMPLER2D);
        private static final ShaderMacro code = new ShaderMacro() {
            public void modify(ProgramContext prog) {
                prog.fctx.fragcol.mod(new Macro1<Expression>() {
                    public Expression expand(Expression in) {
                        return (Cons.texture2D(ctex.ref(), Tex2D.rtexcoord.ref()));
                    }
                }, 0);
            }
        };

        public ShaderMacro code(FBConfig cfg) {
            return (code);
        }

        private GLState.TexUnit csmp;

        public void apply(FBConfig cfg, GOut g) {
            csmp = g.st.texalloc(g, ((GLFrameBuffer.Attach2D) cfg.color[0]).tex);
            g.gl.glUniform1i(g.st.prog.uniform(ctex), csmp.id);
        }

        public void unapply(FBConfig cfg, GOut g) {
            csmp.ufree(g);
            csmp = null;
        }

        public boolean equals(Object o) {
            return (o instanceof Resolve1);
        }
    }

    private static class ResolveMS implements ResolveFilter {
        private final int samples;

        private ResolveMS(int samples) {
            this.samples = samples;
        }

        public void prepare(FBConfig cfg, GOut g) {
        }

        public boolean cleanp() {
            return (true);
        }

        private static final Uniform ctex = new Uniform(Type.SAMPLER2DMS);
        private final ShaderMacro code = new ShaderMacro() {
            public void modify(ProgramContext prog) {
                prog.fctx.fragcol.mod(new Macro1<Expression>() {
                    public Expression expand(Expression in) {
                        Expression[] texels = new Expression[samples];
                        for (int i = 0; i < samples; i++)
                            texels[i] = Cons.texelFetch(ctex.ref(), Cons.ivec2(Cons.floor(Cons.mul(Tex2D.rtexcoord.ref(), MiscLib.screensize.ref()))), Cons.l(i));
                        return (Cons.mul(Cons.add(texels), Cons.l(1.0 / samples)));
                    }
                }, 0);
            }
        };

        public ShaderMacro code(FBConfig cfg) {
            return (code);
        }

        private GLState.TexUnit csmp;

        public void apply(FBConfig cfg, GOut g) {
            csmp = g.st.texalloc(g, ((GLFrameBuffer.AttachMS) cfg.color[0]).tex);
            g.gl.glUniform1i(g.st.prog.uniform(ctex), csmp.id);
        }

        public void unapply(FBConfig cfg, GOut g) {
            csmp.ufree(g);
            csmp = null;
        }

        public boolean equals(Object o) {
            return ((o instanceof ResolveMS) && (((ResolveMS) o).samples == samples));
        }
    }
}
