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

import static haven.glsl.Cons.pick;
import static haven.glsl.Cons.texture2D;
import static haven.glsl.Type.SAMPLER2D;

import java.util.Collection;

import haven.GLState;
import haven.GOut;
import haven.Indir;
import haven.Material;
import haven.Resource;
import haven.TexGL;
import haven.TexR;
import haven.glsl.Expression;
import haven.glsl.Macro1;
import haven.glsl.ProgramContext;
import haven.glsl.ShaderMacro;
import haven.glsl.Tex2D;
import haven.glsl.Uniform;

public class TexPal extends GLState {
    public static final Slot<TexPal> slot = new Slot<TexPal>(Slot.Type.DRAW, TexPal.class);
    public final TexGL tex;

    public TexPal(TexGL tex) {
        this.tex = tex;
    }

    private static final Uniform ctex = new Uniform(SAMPLER2D);
    private static final ShaderMacro[] shaders = {new ShaderMacro() {
        public void modify(ProgramContext prog) {
            Tex2D.tex2d(prog.fctx).mod(new Macro1<Expression>() {
                public Expression expand(Expression in) {
                    return (texture2D(ctex.ref(), pick(in, "rg")));
                }
            }, -100);
        }
    }};

    public ShaderMacro[] shaders() {
        return (shaders);
    }

    private TexUnit sampler;

    public void reapply(GOut g) {
        g.gl.glUniform1i(g.st.prog.uniform(ctex), sampler.id);
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

    @Material.ResName("pal")
    public static class $res implements Material.ResCons2 {
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
            return (new Material.Res.Resolver() {
                public void resolve(Collection<GLState> buf) {
                    TexR rt = tres.get().layer(TexR.class, tid);
                    if (rt == null)
                        throw (new RuntimeException(String.format("Specified texture %d for %s not found in %s", tid, res, tres)));
                    buf.add(new TexPal((TexGL) rt.tex()));
                }
            });
        }
    }
}
