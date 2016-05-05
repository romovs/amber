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

public class Varying extends Variable.Global {
    public static enum Interpol {
        NORMAL, FLAT, NOPERSPECTIVE, CENTROID
    }

    ;
    public Interpol ipol = Interpol.NORMAL;

    protected Interpol ipol(Context ctx) {
        return (ipol);
    }

    public Varying(Type type, Symbol name) {
        super(type, name);
    }

    private class Def extends Definition {
        public void output(Output out) {
            switch (ipol(out.ctx)) {
                case FLAT: {
                    out.write("flat ");
                    break;
                }
                case NOPERSPECTIVE: {
                    out.write("noperspective ");
                    break;
                }
                case CENTROID: {
                    out.write("centroid ");
                    break;
                }
			default:
				break;
            }
            out.write("varying ");
            super.output(out);
        }
    }

    public void use(Context ctx) {
        if (!defined(ctx))
            ctx.vardefs.add(new Def());
    }
}
