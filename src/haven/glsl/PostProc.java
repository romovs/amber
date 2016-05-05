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

import java.util.ArrayList;
import java.util.Collection;

public class PostProc implements Walker {
    public final Object id;
    public final Context ctx;

    public interface Processed {
        public void process(PostProc proc);

        public Object ppid();
    }

    public static class AutoID {
        public final String name;
        public final int order;

        public AutoID(String name, int order) {
            this.name = name;
            this.order = order;
        }

        public AutoID(int order) {
            this("<nil>", order);
        }

        public PostProc proc(Context ctx) {
            return (new PostProc(this, ctx));
        }

        public String toString() {
            return ("AutoID(\"" + name + "\", " + order + ")");
        }
    }

    public static final AutoID misc = new AutoID("misc", 0);

    public PostProc(Object id, Context ctx) {
        this.id = id;
        this.ctx = ctx;
    }

    public PostProc(Object id) {
        this(id, null);
    }

    public PostProc() {
        this.id = this;
        this.ctx = null;
    }

    public void el(Element el) {
        if ((el instanceof Processed) && (((Processed) el).ppid() == id))
            ((Processed) el).process(this);
        el.walk(this);
    }

    public static void autoproc(Context ctx) {
        final Collection<AutoID> closed = new ArrayList<AutoID>();
        final int[] curo = {Integer.MIN_VALUE};
        while (true) {
            final int[] mino = {0};
            final AutoID[] min = {null};
            ctx.walk(new Walker() {
                public void el(Element el) {
                    if (el instanceof Processed) {
                        Object key = ((Processed) el).ppid();
                        if (key instanceof AutoID) {
                            AutoID id = (AutoID) key;
                            if (!closed.contains(id) && ((min[0] == null) || (id.order < mino[0]))) {
                                if (id.order < curo[0])
                                    throw (new RuntimeException("New postprocessor " + id.name + " with order " + id.order + " added when at order " + curo[0]));
                                min[0] = id;
                                mino[0] = id.order;
                            }
                        }
                    }
                    el.walk(this);
                }
            });
            AutoID id = min[0];
            if (id == null)
                return;
            ctx.walk(id.proc(ctx));
            curo[0] = id.order;
            closed.add(id);
        }
    }

    public static abstract class ProcExpression extends Expression implements Processed {
        public final Object id;

        public ProcExpression(Object id) {
            this.id = id;
        }

        public Object ppid() {
            return (id);
        }
    }

    public static abstract class AutoMacro extends ProcExpression {
        protected Expression exp = null;

        public AutoMacro(Object id) {
            super(id);
        }

        protected abstract Expression expand(Context ctx);

        protected Expression expand0(PostProc proc) {
            return (expand(proc.ctx));
        }

        public void process(PostProc proc) {
            exp = expand0(proc);
        }

        public void walk(Walker w) {
            if (exp != null)
                w.el(exp);
        }

        public void output(Output out) {
            exp.output(out);
        }
    }
}
