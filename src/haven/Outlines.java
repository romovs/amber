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

import static haven.glsl.Cons.aadd;
import static haven.glsl.Cons.abs;
import static haven.glsl.Cons.add;
import static haven.glsl.Cons.ass;
import static haven.glsl.Cons.col3;
import static haven.glsl.Cons.cross;
import static haven.glsl.Cons.div;
import static haven.glsl.Cons.dot;
import static haven.glsl.Cons.floor;
import static haven.glsl.Cons.gt;
import static haven.glsl.Cons.ivec2;
import static haven.glsl.Cons.l;
import static haven.glsl.Cons.linc;
import static haven.glsl.Cons.lt;
import static haven.glsl.Cons.max;
import static haven.glsl.Cons.min;
import static haven.glsl.Cons.mix;
import static haven.glsl.Cons.mul;
import static haven.glsl.Cons.neg;
import static haven.glsl.Cons.pick;
import static haven.glsl.Cons.smoothstep;
import static haven.glsl.Cons.stmt;
import static haven.glsl.Cons.sub;
import static haven.glsl.Cons.texelFetch;
import static haven.glsl.Cons.texture2D;
import static haven.glsl.Cons.vec2;
import static haven.glsl.Cons.vec4;
import static haven.glsl.Type.FLOAT;
import static haven.glsl.Type.INT;
import static haven.glsl.Type.SAMPLER2D;
import static haven.glsl.Type.SAMPLER2DMS;
import static haven.glsl.Type.VEC3;

import java.awt.Color;

import haven.glsl.Expression;
import haven.glsl.For;
import haven.glsl.Function;
import haven.glsl.Function.PDir;
import haven.glsl.If;
import haven.glsl.LValue;
import haven.glsl.Macro1;
import haven.glsl.MiscLib;
import haven.glsl.ProgramContext;
import haven.glsl.Return;
import haven.glsl.ShaderMacro;
import haven.glsl.Tex2D;
import haven.glsl.Uniform;

public class Outlines implements Rendered {
    private boolean symmetric;

    public void draw(GOut g) {
    }

    private final static Uniform snrm = new Uniform(SAMPLER2D);
    private final static Uniform sdep = new Uniform(SAMPLER2D);
    private final static Uniform msnrm = new Uniform(SAMPLER2DMS);
    private final static Uniform msdep = new Uniform(SAMPLER2DMS);
    private final static ShaderMacro[][] shaders = new ShaderMacro[4][];

    private static ShaderMacro shader(final boolean symmetric, final boolean ms) {
        return (new ShaderMacro() {
            Color color = Color.BLACK;
            Coord[] points = {
                    new Coord(-1, 0),
                    new Coord(1, 0),
                    new Coord(0, -1),
                    new Coord(0, 1),
            };

            Expression sample(boolean nrm, Expression c, Expression s, Coord o) {
                if (ms) {
                    Expression ctc = ivec2(floor(mul(c, MiscLib.screensize.ref())));
                    if (!o.equals(Coord.z))
                        ctc = add(ctc, ivec2(o));
                    return (texelFetch((nrm ? msnrm : msdep).ref(), ctc, s));
                } else {
                    Expression ctc = c;
                    if (!o.equals(Coord.z))
                        ctc = add(c, mul(vec2(o), MiscLib.pixelpitch.ref()));
                    return (texture2D((nrm ? snrm : sdep).ref(), ctc));
                }
            }

            Function ofac = new Function.Def(FLOAT) {{
                Expression sample = param(PDir.IN, INT).ref();
                Expression tc = Tex2D.rtexcoord.ref();
                LValue ret = code.local(FLOAT, l(0.0)).ref();
                Expression lnrm = code.local(VEC3, mul(sub(pick(sample(true, tc, sample, Coord.z), "rgb"), l(0.5)), l(2.0))).ref();
                Expression ldep = code.local(FLOAT, pick(sample(false, tc, sample, Coord.z), "z")).ref();
            /* XXX: Current depth detection doesn't work well
             * with frustum projections, perhaps because of
		     * the lack of precision in the depth buffer
		     * (though I'm not sure I buy that explanation
		     * yet). */
                LValue dh = code.local(FLOAT, l(0.0002)).ref(), dl = code.local(FLOAT, l(-0.0002)).ref();
                for (int i = 0; i < points.length; i++) {
                    Expression cdep = pick(sample(false, tc, sample, points[i]), "z");
                    cdep = sub(ldep, cdep);
                    cdep = code.local(FLOAT, cdep).ref();
                    code.add(stmt(ass(dh, max(dh, cdep))));
                    code.add(stmt(ass(dl, min(dl, cdep))));
                }
                if (symmetric)
                    code.add(aadd(ret, smoothstep(l(5.0), l(6.0), max(div(dh, neg(dl)), div(dl, neg(dh))))));
                else
                    code.add(aadd(ret, smoothstep(l(5.0), l(6.0), div(dh, neg(dl)))));
                for (int i = 0; i < points.length; i++) {
                    Expression cnrm = mul(sub(pick(sample(true, tc, sample, points[i]), "rgb"), l(0.5)), l(2.0));
                    if (symmetric) {
                        code.add(aadd(ret, sub(l(1.0), abs(dot(lnrm, cnrm)))));
                    } else {
                        cnrm = code.local(VEC3, cnrm).ref();
                        code.add(new If(gt(pick(cross(lnrm, cnrm), "z"), l(0.0)),
                                stmt(aadd(ret, sub(l(1.0), abs(dot(lnrm, cnrm)))))));
                    }
                }
                code.add(new Return(smoothstep(l(0.4), l(0.6), min(ret, l(1.0)))));
            }};

            Function msfac = new Function.Def(FLOAT) {{
                LValue ret = code.local(FLOAT, l(0.0)).ref();
                LValue i = code.local(INT, null).ref();
                code.add(new For(ass(i, l(0)), lt(i, FBConfig.numsamples.ref()), linc(i),
                        stmt(aadd(ret, ofac.call(i)))));
                code.add(new Return(div(ret, FBConfig.numsamples.ref())));
            }};

            public void modify(ProgramContext prog) {
                prog.fctx.fragcol.mod(new Macro1<Expression>() {
                    public Expression expand(Expression in) {
                        Expression of = (!ms) ? ofac.call(l(-1)) : msfac.call();
                        return (vec4(col3(color), mix(l(0.0), l(1.0), of)));
                    }
                }, 0);
            }
        });
    }

    static {
	/* XXX: It would be good to have some kind of more convenient
	 * shader internation. */
        shaders[0] = new ShaderMacro[]{shader(false, false)};
        shaders[1] = new ShaderMacro[]{shader(false, true)};
        shaders[2] = new ShaderMacro[]{shader(true, false)};
        shaders[3] = new ShaderMacro[]{shader(true, true)};
    }

    public Outlines(final boolean symmetric) {
        this.symmetric = symmetric;
    }

    public boolean setup(RenderList rl) {
        final PView.ConfContext ctx = (PView.ConfContext) rl.state().get(PView.ctx);
        final RenderedNormals nrm = ctx.data(RenderedNormals.id);
        final boolean ms = ctx.cfg.ms > 1;
        ctx.cfg.tdepth = true;
        ctx.cfg.add(nrm);
        rl.prepc(Rendered.postfx);
        rl.add(new Rendered.ScreenQuad(), new States.AdHoc(shaders[(symmetric ? 2 : 0) | (ms ? 1 : 0)]) {
            private TexUnit tnrm;
            private TexUnit tdep;

            public void reapply(GOut g) {
                BGL gl = g.gl;
                gl.glUniform1i(g.st.prog.uniform(!ms ? snrm : msnrm), tnrm.id);
                gl.glUniform1i(g.st.prog.uniform(!ms ? sdep : msdep), tdep.id);
            }

            public void apply(GOut g) {
                if (!ms) {
                    tnrm = g.st.texalloc(g, ((GLFrameBuffer.Attach2D) nrm.tex).tex);
                    tdep = g.st.texalloc(g, ((GLFrameBuffer.Attach2D) ctx.cur.depth).tex);
                } else {
                    tnrm = g.st.texalloc(g, ((GLFrameBuffer.AttachMS) nrm.tex).tex);
                    tdep = g.st.texalloc(g, ((GLFrameBuffer.AttachMS) ctx.cur.depth).tex);
                }
                reapply(g);
            }

            public void unapply(GOut g) {
                tnrm.ufree(g);
                tnrm = null;
                tdep.ufree(g);
                tdep = null;
            }
        });
        return (false);
    }
}
