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

import static haven.glsl.Cons.l;
import static haven.glsl.Cons.lt;
import static haven.glsl.Cons.mul;
import static haven.glsl.Cons.pick;
import static haven.glsl.Cons.texture2D;
import static haven.glsl.Type.VEC2;
import static haven.glsl.Type.VEC4;

import haven.glsl.ValBlock.Value;

public class Tex2D {
    public static final Uniform tex2d = new Uniform(Type.SAMPLER2D);
    public Varying.Interpol ipol = Varying.Interpol.NORMAL;

    public static final AutoVarying rtexcoord = new AutoVarying(VEC2, "s_tex2d") {
        protected Expression root(VertexContext vctx) {
            return (pick(VertexContext.gl_MultiTexCoord[0].ref(), "st"));
        }

        protected Interpol ipol(Context ctx) {
            Tex2D mod;
            if ((ctx instanceof ShaderContext) && ((mod = ((ShaderContext) ctx).prog.getmod(Tex2D.class)) != null))
                return (mod.ipol);
            return (super.ipol(ctx));
        }
    };

    public static Value texcoord(FragmentContext fctx) {
        return (fctx.uniform.ext(rtexcoord, new ValBlock.Factory() {
            public Value make(ValBlock vals) {
                return (vals.new Value(VEC2) {
                    public Expression root() {
                        return (rtexcoord.ref());
                    }
                });
            }
        }));
    }

    public static Value tex2d(final FragmentContext fctx) {
        return (fctx.uniform.ext(tex2d, new ValBlock.Factory() {
            public Value make(ValBlock vals) {
                texcoord(fctx);
                return (vals.new Value(VEC4) {
                    public Expression root() {
                        return (texture2D(tex2d.ref(), texcoord(fctx).depref()));
                    }
                });
            }
        }));
    }

    public Tex2D(ProgramContext prog) {
        prog.module(this);
    }

    public static Tex2D get(ProgramContext prog) {
        Tex2D t = prog.getmod(Tex2D.class);
        if (t == null)
            t = new Tex2D(prog);
        return (t);
    }

    public static final ShaderMacro mod = new ShaderMacro() {
        public void modify(ProgramContext prog) {
            final Value tex2d = tex2d(prog.fctx);
            tex2d.force();
            prog.fctx.fragcol.mod(new Macro1<Expression>() {
                public Expression expand(Expression in) {
                    return (mul(in, tex2d.ref()));
                }
            }, 0);
        }
    };

    public static final ShaderMacro clip = new ShaderMacro() {
        public void modify(ProgramContext prog) {
            final Value tex2d = tex2d(prog.fctx);
            tex2d.force();
            prog.fctx.mainmod(new CodeMacro() {
                public void expand(Block blk) {
                    blk.add(new If(lt(pick(tex2d.ref(), "a"), l(0.5)),
                            new Discard()));
                }
            }, -100);
        }
    };
}
