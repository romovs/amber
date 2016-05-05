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

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class Block extends Statement {
    public final List<Statement> stmts = new LinkedList<Statement>();

    public Block(Statement... stmts) {
        for (Statement s : stmts)
            this.stmts.add(s);
    }

    public final static class Local extends Variable {
        public Local(Type type, Symbol name) {
            super(type, name);
        }

        public Local(Type type) {
            this(type, new Symbol.Gen());
        }

        public class Def extends Statement {
            private final Expression init;

            public Def(Expression init) {
                this.init = init;
            }

            public void walk(Walker w) {
                if (init != null)
                    w.el(init);
            }

            public void output(Output out) {
                out.write(type.name(out.ctx));
                out.write(" ");
                out.write(name);
                if (init != null) {
                    out.write(" = ");
                    init.output(out);
                }
                out.write(";");
            }
        }
    }

    public void add(Statement stmt, Statement before) {
        if (stmt == null)
            throw (new NullPointerException());
        if (before == null) {
            stmts.add(stmt);
        } else {
            for (ListIterator<Statement> i = stmts.listIterator(); i.hasNext(); ) {
                Statement cur = i.next();
                if (cur == before) {
                    i.previous();
                    i.add(stmt);
                    return;
                }
            }
            throw (new RuntimeException(before + " is not already in block"));
        }
    }

    public void add(Statement stmt) {
        add(stmt, null);
    }

    public void add(Expression expr, Statement before) {
        add(Statement.expr(expr), before);
    }

    public void add(Expression expr) {
        add(Statement.expr(expr), null);
    }

    public Local local(Type type, Symbol name, Expression init, Statement before) {
        Local ret = new Local(type, name);
        add(ret.new Def(init), before);
        return (ret);
    }

    public Local local(Type type, Symbol name, Expression init) {
        return (local(type, name, init, null));
    }

    public Local local(Type type, String prefix, Expression init) {
        return (local(type, new Symbol.Gen(prefix), init));
    }

    public Local local(Type type, Expression init) {
        return (local(type, new Symbol.Gen(), init));
    }

    public void walk(Walker w) {
        for (Statement s : stmts)
            w.el(s);
    }

    public void trail(Output out, boolean nl) {
        if (stmts.isEmpty())
            return;
        out.write("{\n");
        out.indent++;
        for (Statement s : stmts) {
            out.indent();
            s.output(out);
            out.write("\n");
        }
        out.indent--;
        out.indent();
        out.write("}");
        if (nl)
            out.write("\n");
    }

    public void output(Output out) {
        out.indent();
        trail(out, true);
    }
}
