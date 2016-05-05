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

public abstract class LBinOp extends Expression {
    public final LValue lhs;
    public final Expression rhs;

    public LBinOp(LValue lhs, Expression rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public void walk(Walker w) {
        w.el(lhs);
        w.el(rhs);
    }

    public abstract String form();

    public void output(Output out) {
        out.write("(");
        lhs.output(out);
        out.write(" " + form() + " ");
        rhs.output(out);
        out.write(")");
    }

    public static class Assign extends LBinOp {
        public String form() {
            return ("=");
        }

        public Assign(LValue l, Expression r) {
            super(l, r);
        }
    }

    public static class AAdd extends LBinOp {
        public String form() {
            return ("+=");
        }

        public AAdd(LValue l, Expression r) {
            super(l, r);
        }
    }

    public static class ASub extends LBinOp {
        public String form() {
            return ("-=");
        }

        public ASub(LValue l, Expression r) {
            super(l, r);
        }
    }

    public static class AMul extends LBinOp {
        public String form() {
            return ("*=");
        }

        public AMul(LValue l, Expression r) {
            super(l, r);
        }
    }

    public static class ADiv extends LBinOp {
        public String form() {
            return ("/=");
        }

        public ADiv(LValue l, Expression r) {
            super(l, r);
        }
    }
}
