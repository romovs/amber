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

package haven.resutil;

import static haven.glsl.Cons.lt;
import static haven.glsl.Cons.mul;
import static haven.glsl.Cons.pick;
import static haven.glsl.Cons.texture2D;
import static haven.glsl.Type.FLOAT;
import static haven.glsl.Type.SAMPLER2D;
import static haven.glsl.Type.VEC2;
import static haven.glsl.Type.VEC4;

import haven.GLState;
import haven.GOut;
import haven.MeshBuf;
import haven.TexGL;
import haven.glsl.Attribute;
import haven.glsl.AutoVarying;
import haven.glsl.Block;
import haven.glsl.CodeMacro;
import haven.glsl.Discard;
import haven.glsl.Expression;
import haven.glsl.FragmentContext;
import haven.glsl.If;
import haven.glsl.Macro1;
import haven.glsl.ProgramContext;
import haven.glsl.ShaderMacro;
import haven.glsl.Uniform;
import haven.glsl.ValBlock;
import haven.glsl.ValBlock.Value;
import haven.glsl.Varying.Interpol;
import haven.glsl.VertexContext;

public class AlphaTex extends GLState {
    public static final Slot<AlphaTex> slot = new Slot<AlphaTex>(Slot.Type.DRAW, AlphaTex.class);
    public static final Attribute clipc = new Attribute(VEC2);
    public static final MeshBuf.LayerID<MeshBuf.Vec2Layer> lclip = new MeshBuf.V2LayerID(clipc);
    private static final Uniform ctex = new Uniform(SAMPLER2D);
    private static final Uniform cclip = new Uniform(FLOAT);
    public final TexGL tex;
    public final float cthr;
    private TexUnit sampler;

    public AlphaTex(TexGL tex, float clip) {
        this.tex = tex;
        this.cthr = clip;
    }

    public AlphaTex(TexGL tex) {
        this(tex, 0);
    }

    private static final AutoVarying fc = new AutoVarying(VEC2) {
        {
            ipol = Interpol.CENTROID;
        }

        protected Expression root(VertexContext vctx) {
            return (clipc.ref());
        }
    };

    private static Value value(FragmentContext fctx) {
        return (fctx.uniform.ext(ctex, new ValBlock.Factory() {
            public Value make(ValBlock vals) {
                return (vals.new Value(VEC4) {
                    public Expression root() {
                        return (texture2D(ctex.ref(), fc.ref()));
                    }
                });
            }
        }));
    }

    private static final ShaderMacro main = new ShaderMacro() {
        public void modify(ProgramContext prog) {
            final Value val = value(prog.fctx);
            val.force();
            prog.fctx.fragcol.mod(new Macro1<Expression>() {
                public Expression expand(Expression in) {
                    return (mul(in, val.ref()));
                }
            }, 100);
        }
    };
    private static final ShaderMacro clip = new ShaderMacro() {
        public void modify(ProgramContext prog) {
            final Value val = value(prog.fctx);
            val.force();
            prog.fctx.mainmod(new CodeMacro() {
                public void expand(Block blk) {
                    blk.add(new If(lt(pick(val.ref(), "a"), cclip.ref()),
                            new Discard()));
                }
            }, -100);
        }
    };

    private static final ShaderMacro[] shnc = {main};
    private static final ShaderMacro[] shwc = {main, clip};

    public ShaderMacro[] shaders() {
        return ((cthr > 0) ? shwc : shnc);
    }

    public boolean reqshader() {
        return (true);
    }

    public void reapply(GOut g) {
        g.gl.glUniform1i(g.st.prog.uniform(ctex), sampler.id);
        if (cthr > 0)
            g.gl.glUniform1f(g.st.prog.uniform(cclip), cthr);
    }

    public void apply(GOut g) {
        sampler = TexGL.lbind(g, tex);
        reapply(g);
    }

    public void unapply(GOut g) {
        sampler.ufree(g);
        sampler = null;
    }

    public void prep(Buffer buf) {
        buf.put(slot, this);
    }
}
