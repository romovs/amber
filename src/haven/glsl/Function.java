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

public abstract class Function {
    public final Symbol name;
    public final List<Parameter> pars = new LinkedList<Parameter>();

    public Function(Symbol name) {
        this.name = name;
    }

    private class Call extends Expression {
        private final Expression[] params;

        private Call(Expression... params) {
            this.params = params;
        }

        public void walk(Walker w) {
            for (Expression param : params)
                w.el(param);
        }

        public void output(Output out) {
            out.write(name);
            out.write("(");
            if (params.length > 0) {
                params[0].output(out);
                for (int i = 1; i < params.length; i++) {
                    out.write(", ");
                    params[i].output(out);
                }
            }
            out.write(")");
        }

        public Function fun() {
            return (Function.this);
        }
    }

    public Expression call(Expression... params) {
        ckparams(params);
        return (new Call(params));
    }

    public static class Def extends Function {
        public final Type type;
        public final Block code;
        private boolean fin = false;

        public Def(Type type, Symbol name) {
            super(name);
            this.type = type;
            this.code = new Block();
        }

        public Def(Type type) {
            this(type, new Symbol.Gen());
        }

        private class Definition extends Toplevel {
            public void walk(Walker w) {
                w.el(code);
            }

            public void output(Output out) {
                prototype(out);
                out.write("\n");
                code.output(out);
            }

            private Def fun() {
                return (Def.this);
            }
        }

        protected void cons() {
        }

        public void define(final Context ctx) {
            if (!fin) {
                cons();
                fin = true;
            }
            for (Toplevel tl : ctx.fundefs) {
                if ((tl instanceof Definition) && (((Definition) tl).fun() == Def.this))
                    return;
            }
            new Walker() {
                public void el(Element el) {
                    if (el instanceof Call) {
                        Function fun = ((Call) el).fun();
                        if (fun instanceof Def)
                            ((Def) fun).define(ctx);
                    }
                    el.walk(this);
                }
            }.el(code);
            ctx.fundefs.add(new Definition());
        }

        public void prototype(Output out) {
            out.write(type.name(out.ctx));
            out.write(" ");
            out.write(name);
            out.write("(");
            boolean f = true;
            for (Parameter par : pars) {
                if (!f)
                    out.write(", ");
                f = false;
                switch (par.dir) {
                    case IN:
                        break;
                    case OUT:
                        out.write("out ");
                        break;
                    case INOUT:
                        out.write("inout ");
                        break;
                }
                out.write(par.type.name(out.ctx));
                out.write(" ");
                out.write(par.name);
            }
            out.write(")");
        }

        public Type type(Expression... params) {
            return (type);
        }

        public void code(Statement stmt) {
            code.add(stmt);
        }

        public void code(Expression expr) {
            code.add(expr);
        }
    }

    public static class Builtin extends Function {
        private final Type type;

        public Builtin(Type type, Symbol name, int nargs) {
            super(name);
            this.type = type;
            for (int i = 0; i < nargs; i++)
                param(PDir.IN, null);
        }

        public Type type(Expression... params) {
            if (type == null)
                throw (new NullPointerException("type"));
            return (type);
        }

        public static final Builtin sin = new Builtin(null, new Symbol.Fix("sin"), 1);
        public static final Builtin cos = new Builtin(null, new Symbol.Fix("cos"), 1);
        public static final Builtin tan = new Builtin(null, new Symbol.Fix("tan"), 1);
        public static final Builtin asin = new Builtin(null, new Symbol.Fix("asin"), 1);
        public static final Builtin acos = new Builtin(null, new Symbol.Fix("acos"), 1);
        public static final Builtin atan = new Builtin(null, new Symbol.Fix("atan"), 1);

        public static final Builtin pow = new Builtin(null, new Symbol.Fix("pow"), 2);
        public static final Builtin exp = new Builtin(null, new Symbol.Fix("exp"), 1);
        public static final Builtin log = new Builtin(null, new Symbol.Fix("log"), 1);
        public static final Builtin exp2 = new Builtin(null, new Symbol.Fix("exp2"), 1);
        public static final Builtin log2 = new Builtin(null, new Symbol.Fix("log2"), 1);
        public static final Builtin sqrt = new Builtin(null, new Symbol.Fix("sqrt"), 1);
        public static final Builtin inversesqrt = new Builtin(null, new Symbol.Fix("inversesqrt"), 1);

        public static final Builtin abs = new Builtin(null, new Symbol.Fix("abs"), 1);
        public static final Builtin sign = new Builtin(null, new Symbol.Fix("sign"), 1);
        public static final Builtin floor = new Builtin(null, new Symbol.Fix("floor"), 1);
        public static final Builtin ceil = new Builtin(null, new Symbol.Fix("ceil"), 1);
        public static final Builtin fract = new Builtin(null, new Symbol.Fix("fract"), 1);
        public static final Builtin mod = new Builtin(null, new Symbol.Fix("mod"), 2);
        public static final Builtin min = new Builtin(null, new Symbol.Fix("min"), 2);
        public static final Builtin max = new Builtin(null, new Symbol.Fix("max"), 2);
        public static final Builtin clamp = new Builtin(null, new Symbol.Fix("clamp"), 3);
        public static final Builtin mix = new Builtin(null, new Symbol.Fix("mix"), 3);
        public static final Builtin step = new Builtin(null, new Symbol.Fix("step"), 2);
        public static final Builtin smoothstep = new Builtin(null, new Symbol.Fix("smoothstep"), 3);

        public static final Builtin length = new Builtin(Type.FLOAT, new Symbol.Fix("length"), 1);
        public static final Builtin distance = new Builtin(Type.FLOAT, new Symbol.Fix("distance"), 2);
        public static final Builtin dot = new Builtin(Type.FLOAT, new Symbol.Fix("dot"), 2);
        public static final Builtin cross = new Builtin(Type.VEC3, new Symbol.Fix("cross"), 2);
        public static final Builtin normalize = new Builtin(null, new Symbol.Fix("normalize"), 1);
        public static final Builtin reflect = new Builtin(null, new Symbol.Fix("reflect"), 2);

        public static final Builtin transpose = new Builtin(null, new Symbol.Fix("transpose"), 1);

        public static final Builtin texture2D = new Builtin(Type.VEC4, new Symbol.Fix("texture2D"), 2);
        public static final Builtin texture3D = new Builtin(Type.VEC4, new Symbol.Fix("texture3D"), 2);
        public static final Builtin textureCube = new Builtin(Type.VEC4, new Symbol.Fix("textureCube"), 2);
        public static final Builtin texelFetch = new Builtin(Type.VEC4, new Symbol.Fix("texelFetch"), 3);
    }

    public enum PDir {IN, OUT, INOUT;}

    public static class Parameter extends Variable {
        public final PDir dir;

        private Parameter(PDir dir, Type type, Symbol name) {
            super(type, name);
            this.dir = dir;
        }
    }

    public Parameter param(PDir dir, Type type, Symbol name) {
        Parameter ret = new Parameter(dir, type, name);
        pars.add(ret);
        return (ret);
    }

    public Parameter param(PDir dir, Type type, String prefix) {
        return (param(dir, type, new Symbol.Gen(prefix)));
    }

    public Parameter param(PDir dir, Type type) {
        return (param(dir, type, new Symbol.Gen()));
    }

    public Function param1(PDir dir, Type type) {
        param(dir, type);
        return (this);
    }

    void ckparams(Expression... params) {
        if (params.length != pars.size())
            throw (new RuntimeException(String.format("Wrong number of arguments to %s; expected %d, got %d", name, pars.size(), params.length)));
        int i = 0;
        for (Parameter par : pars) {
            if (((par.dir == PDir.OUT) || (par.dir == PDir.INOUT)) && !(params[i] instanceof LValue))
                throw (new RuntimeException(String.format("Must have l-value for %s parameter %d to %s", par.dir, i, name)));
            i++;
        }
    }

    public abstract Type type(Expression... params);
}
