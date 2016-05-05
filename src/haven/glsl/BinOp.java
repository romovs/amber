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

public abstract class BinOp extends Expression {
    public final Expression lhs, rhs;

    public BinOp(Expression lhs, Expression rhs) {
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

    public static class Eq extends BinOp {
        public String form() {
            return ("==");
        }

        public Eq(Expression l, Expression r) {
            super(l, r);
        }
    }

    public static class Ne extends BinOp {
        public String form() {
            return ("!=");
        }

        public Ne(Expression l, Expression r) {
            super(l, r);
        }
    }

    public static class Lt extends BinOp {
        public String form() {
            return ("<");
        }

        public Lt(Expression l, Expression r) {
            super(l, r);
        }
    }

    public static class Gt extends BinOp {
        public String form() {
            return (">");
        }

        public Gt(Expression l, Expression r) {
            super(l, r);
        }
    }

    public static class Le extends BinOp {
        public String form() {
            return ("<=");
        }

        public Le(Expression l, Expression r) {
            super(l, r);
        }
    }

    public static class Ge extends BinOp {
        public String form() {
            return (">=");
        }

        public Ge(Expression l, Expression r) {
            super(l, r);
        }
    }

    public static class Or extends BinOp {
        public String form() {
            return ("||");
        }

        public Or(Expression l, Expression r) {
            super(l, r);
        }
    }

    public static class And extends BinOp {
        public String form() {
            return ("&&");
        }

        public And(Expression l, Expression r) {
            super(l, r);
        }
    }

    public static class Sub extends BinOp {
        public String form() {
            return ("-");
        }

        public Sub(Expression l, Expression r) {
            super(l, r);
        }
    }

    public static class Div extends BinOp {
        public String form() {
            return ("/");
        }

        public Div(Expression l, Expression r) {
            super(l, r);
        }
    }
}
