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

import haven.*;
import static haven.glsl.Cons.*;
import static haven.glsl.Function.PDir.*;
import static haven.glsl.Type.*;
import haven.glsl.ValBlock.Value;

public class BaseColor implements ShaderMacro {
    public static final InstancedUniform u_color = new InstancedUniform.Vec4("color", States.color) {
	    public float[] forstate(GOut g, GLState.Buffer buf) {
		return(buf.get(States.color).ca);
	    }
	};

    public static final AutoVarying transfer = new AutoVarying(VEC4) {
	    protected Expression root(VertexContext vctx) {
		return(u_color.ref());
	    }
	};

    public void modify(ProgramContext prog) {
	prog.fctx.fragcol.mod(new Macro1<Expression>() {
		public Expression expand(Expression in) {
		    return(mul(in, transfer.ref()));
		}
	    }, 0);
    }
}
