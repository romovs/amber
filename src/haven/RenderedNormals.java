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

import static haven.glsl.Cons.add;
import static haven.glsl.Cons.l;
import static haven.glsl.Cons.mul;
import static haven.glsl.Cons.vec4;

import haven.glsl.Expression;
import haven.glsl.MiscLib;
import haven.glsl.ProgramContext;
import haven.glsl.ShaderMacro;

public class RenderedNormals extends FBConfig.RenderTarget {
    private static final IntMap<ShaderMacro[]> shcache = new IntMap<ShaderMacro[]>();

    private static ShaderMacro[] code(final int id) {
        ShaderMacro[] ret = shcache.get(id);
        if (ret == null) {
            ret = new ShaderMacro[]{
                    new ShaderMacro() {
                        public void modify(final ProgramContext prog) {
                            MiscLib.frageyen(prog.fctx);
                            prog.fctx.new FragData(id) {
                                public Expression root() {
                                    return (vec4(mul(add(MiscLib.frageyen(prog.fctx).depref(), l(1.0)), l(0.5)), l(1.0)));
                                }
                            };
                        }
                    }
            };
            shcache.put(id, ret);
        }
        return (ret);
    }

    public static final GLState.Slot<GLState> slot = new GLState.Slot<GLState>(GLState.Slot.Type.SYS, GLState.class, GLFrameBuffer.slot, States.presdepth.slot);

    public GLState state(final FBConfig cfg, final int id) {
        return (new GLState() {
            private final ShaderMacro[] shaders = code(id);

            public ShaderMacro[] shaders() {
                return (shaders);
            }

            public void apply(GOut g) {
                GLFrameBuffer fb = g.st.get(GLFrameBuffer.slot);
                if (fb != cfg.fb)
                    throw (new RuntimeException("Applying normal rendering in illegal framebuffer context"));
                if (g.st.get(States.presdepth.slot) != null)
                    fb.mask(g, id, false);
            }

            public void unapply(GOut g) {
                g.st.cur(GLFrameBuffer.slot).mask(g, id, true);
            }

            public void prep(Buffer buf) {
                buf.put(slot, this);
            }
        });
    }

    public static final PView.RenderContext.DataID<RenderedNormals> id = new PView.RenderContext.DataID<RenderedNormals>() {
        public RenderedNormals make(PView.RenderContext ctx) {
            return (new RenderedNormals());
        }
    };
}
