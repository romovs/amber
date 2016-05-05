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

import static haven.glsl.Type.VEC4;

import java.awt.Color;

import haven.glsl.Expression;
import haven.glsl.Macro1;
import haven.glsl.MiscLib;
import haven.glsl.ProgramContext;
import haven.glsl.ShaderMacro;
import haven.glsl.Uniform;

public class ColorMask extends GLState {
    public static final Slot<ColorMask> slot = new Slot<ColorMask>(Slot.Type.DRAW, ColorMask.class);
    public static final Uniform ccol = new Uniform(VEC4);
    private final float[] col;

    private static final ShaderMacro[] sh = {
            new ShaderMacro() {
                public void modify(ProgramContext prog) {
                    prog.fctx.fragcol.mod(new Macro1<Expression>() {
                        public Expression expand(Expression in) {
                            return (MiscLib.colblend.call(in, ccol.ref()));
                        }
                    }, 100);
                }
            }
    };

    public ColorMask(Color col) {
        this.col = Utils.c2fa(col);
    }

    public ShaderMacro[] shaders() {
        return (sh);
    }

    public void reapply(GOut g) {
        g.gl.glUniform4fv(g.st.prog.uniform(ccol), 1, col, 0);
    }

    public void apply(GOut g) {
        reapply(g);
    }

    public void unapply(GOut g) {
    }

    public void prep(Buffer buf) {
        buf.put(slot, this);
    }
}
