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

package haven.glsl;

import static haven.glsl.Cons.add;
import static haven.glsl.Cons.clamp;
import static haven.glsl.Cons.l;
import static haven.glsl.Cons.mix;
import static haven.glsl.Cons.mul;
import static haven.glsl.Cons.neg;
import static haven.glsl.Cons.normalize;
import static haven.glsl.Cons.pick;
import static haven.glsl.Cons.sub;
import static haven.glsl.Cons.vec4;
import static haven.glsl.Function.PDir.IN;
import static haven.glsl.Type.FLOAT;
import static haven.glsl.Type.VEC2;
import static haven.glsl.Type.VEC3;
import static haven.glsl.Type.VEC4;

import haven.Coord;
import haven.Coord3f;
import haven.GLProgram.VarID;
import haven.GOut;
import haven.Glob;
import haven.PView;
import haven.glsl.ValBlock.Value;

public abstract class MiscLib {
    private static final AutoVarying frageyen = new AutoVarying(VEC3, "s_eyen") {
        protected Expression root(VertexContext vctx) {
            return (vctx.eyen.depref());
        }
    };

    public static Value frageyen(final FragmentContext fctx) {
        return (fctx.mainvals.ext(frageyen, new ValBlock.Factory() {
            public Value make(ValBlock vals) {
                Value ret = vals.new Value(VEC3, new Symbol.Gen("eyen")) {
                    public Expression root() {
                        return (frageyen.ref());
                    }
                };
                ret.mod(new Macro1<Expression>() {
                    public Expression expand(Expression in) {
                        return (normalize(in));
                    }
                }, 0);
                return (ret);
            }
        }));
    }

    public static final AutoVarying fragobjv = new AutoVarying(VEC3, "s_objv") {
        protected Expression root(VertexContext vctx) {
            return (pick(vctx.objv.depref(), "xyz"));
        }
    };
    public static final AutoVarying fragmapv = new AutoVarying(VEC3, "s_mapv") {
        protected Expression root(VertexContext vctx) {
            return (pick(vctx.mapv.depref(), "xyz"));
        }
    };
    public static final AutoVarying frageyev = new AutoVarying(VEC3, "s_eyev") {
        protected Expression root(VertexContext vctx) {
            return (pick(vctx.eyev.depref(), "xyz"));
        }
    };
    private static final Object vertedir_id = new Object();

    public static Value vertedir(final VertexContext vctx) {
        return (vctx.mainvals.ext(vertedir_id, new ValBlock.Factory() {
            public Value make(ValBlock vals) {
                return (vals.new Value(VEC3, new Symbol.Gen("edir")) {
                    public Expression root() {
                        return (neg(normalize(pick(vctx.eyev.depref(), "xyz"))));
                    }
                });
            }
        }));
    }

    private static final Object fragedir_id = new Object();

    public static Value fragedir(final FragmentContext fctx) {
        return (fctx.mainvals.ext(fragedir_id, new ValBlock.Factory() {
            public Value make(ValBlock vals) {
                return (vals.new Value(VEC3, new Symbol.Gen("edir")) {
                    public Expression root() {
                        return (neg(normalize(frageyev.ref())));
                    }
                });
            }
        }));
    }

    public static final Uniform maploc = new Uniform.AutoApply(VEC3, PView.loc) {
        public void apply(GOut g, VarID loc) {
            Coord3f orig = PView.locxf(g).mul4(Coord3f.o);
            orig.z = g.st.get(PView.ctx).glob().map.getcz(orig.x, -orig.y);
            g.gl.glUniform3f(loc, orig.x, orig.y, orig.z);
        }
    };

    public static final Uniform time = new Uniform.AutoApply(FLOAT, "time") {
        public void apply(GOut g, VarID loc) {
            g.gl.glUniform1f(loc, (System.currentTimeMillis() % 3000000L) / 1000f);
        }
    };
    public static final Uniform globtime = new Uniform.AutoApply(FLOAT, "globtime") {
        public void apply(GOut g, VarID loc) {
            Glob glob = g.st.cur(PView.ctx).glob();
            g.gl.glUniform1f(loc, (glob.globtime() % 10000000L) / 1000f);
        }
    };

    private static Coord ssz(GOut g) {
        PView.RenderState wnd = g.st.cur(PView.wnd);
        if (wnd == null)
            return (g.sz);
        else
            return (wnd.sz());
    }

    public static final Uniform pixelpitch = new Uniform.AutoApply(VEC2) {
        public void apply(GOut g, VarID loc) {
            Coord sz = ssz(g);
            g.gl.glUniform2f(loc, 1.0f / sz.x, 1.0f / sz.y);
        }
    };
    public static final Uniform screensize = new Uniform.AutoApply(VEC2) {
        public void apply(GOut g, VarID loc) {
            Coord sz = ssz(g);
            g.gl.glUniform2f(loc, sz.x, sz.y);
        }
    };

    public static final Function colblend = new Function.Def(VEC4) {{
        Expression base = param(IN, VEC4).ref();
        Expression blend = param(IN, VEC4).ref();
        code.add(new Return(vec4(add(mul(pick(base, "rgb"), sub(l(1.0), pick(blend, "a"))),
                        mul(pick(blend, "rgb"), pick(blend, "a"))),
                pick(base, "a"))));
    }};
    public static final Function olblend = new Function.Def(VEC4) {{
        Expression base = param(IN, VEC4).ref();
        Expression blend = param(IN, VEC4).ref();
        /* XXX: It seems weird indeed that Haven would be the only
         * program having trouble with this, but on Intel GPUs, the
         * sign() function very much appears to be buggy somehow
         * (quite unclear just how; no explanation I can think of
         * seems to make sense). Thus, this uses (x * 1000) instead of
         * sign(x). Assuming color values mapped from uint8_t's, 1000
         * is enough to saturate the smallest deviations from 0.5. */
        code.add(new Return(vec4(mix(mul(l(2.0), pick(base, "rgb"), pick(blend, "rgb")),
                        sub(l(1.0), mul(l(2.0), sub(l(1.0), pick(base, "rgb")), sub(l(1.0), pick(blend, "rgb")))),
                        clamp(mul(sub(pick(blend, "rgb"), l(0.5)), l(1000.0)), l(0.0), l(1.0))),
                pick(base, "a"))));
    }};
    public static final Function cpblend = new Function.Def(VEC4) {{
        Expression base = param(IN, VEC4).ref();
        Expression blend = param(IN, VEC4).ref();
	    /* XXX: See olblend comment */
        code.add(new Return(vec4(mix(mul(l(2.0), pick(base, "rgb"), pick(blend, "rgb")),
                        sub(l(1.0), mul(l(2.0), sub(l(1.0), pick(base, "rgb")), sub(l(1.0), pick(blend, "rgb")))),
                        clamp(mul(sub(pick(base, "rgb"), l(0.5)), l(1000.0)), l(0.0), l(1.0))),
                pick(base, "a"))));
    }};
}
