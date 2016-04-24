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

import java.io.StringWriter;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import haven.GLBuffer;
import haven.GLProgram;
import haven.GLShader;
import haven.GLShader.FragmentShader;
import haven.GLShader.VertexShader;
import haven.GLState;
import haven.GOut;

public interface ShaderMacro {
    public void modify(ProgramContext prog);

    @SuppressWarnings("serial")
	public static class Program extends GLProgram {
        public static boolean dumpall = false;
        public transient final ProgramContext built;
        private final transient int[][] automask;
        private final transient Uniform.AutoApply[] auto;
        private final transient boolean[] adirty;
        private transient VarID[] autolocs;
        public final transient Attribute.AutoInstanced[] autoinst;
        public final transient GLBuffer[] curinst;

        private static Collection<GLShader> build(ProgramContext prog) {
            Collection<GLShader> ret = new LinkedList<GLShader>();
            StringWriter fbuf = new StringWriter();
            prog.fctx.construct(fbuf);
            ret.add(new FragmentShader(fbuf.toString()));
            StringWriter vbuf = new StringWriter();
            prog.vctx.construct(vbuf);
            ret.add(new VertexShader(vbuf.toString()));
            return (ret);
        }

        @SuppressWarnings("unchecked")
        public Program(ProgramContext ctx) {
            super(build(ctx));
            this.built = ctx;
            {
                List<Uniform.AutoApply> auto = new LinkedList<Uniform.AutoApply>();
                for (Uniform var : ctx.uniforms) {
                    if (var instanceof Uniform.AutoApply)
                        auto.add((Uniform.AutoApply) var);
                }
                this.auto = auto.toArray(new Uniform.AutoApply[0]);
            }
            this.adirty = new boolean[this.auto.length];
            {
                int max = -1;
                for (Uniform.AutoApply auto : this.auto) {
                    for (@SuppressWarnings("rawtypes") GLState.Slot slot : auto.deps)
                        max = Math.max(max, slot.id);
                }
                LinkedList<Integer>[] buf = (LinkedList<Integer>[]) new LinkedList[max + 1];
                for (int i = 0; i < auto.length; i++) {
                    for (@SuppressWarnings("rawtypes") GLState.Slot slot : auto[i].deps) {
                        if (buf[slot.id] == null)
                            buf[slot.id] = new LinkedList<Integer>();
                        buf[slot.id].add(i);
                    }
                }
                automask = new int[max + 1][];
                for (int i = 0; i <= max; i++) {
                    if (buf[i] == null) {
                        automask[i] = new int[0];
                    } else {
                        automask[i] = new int[buf[i].size()];
                        int o = 0;
                        for (int s : buf[i])
                            automask[i][o++] = s;
                    }
                }
            }
            {
                List<Attribute.AutoInstanced> autoinst = new LinkedList<Attribute.AutoInstanced>();
                for (Attribute var : ctx.attribs) {
                    if (var instanceof Attribute.AutoInstanced)
                        autoinst.add((Attribute.AutoInstanced) var);
                }
                this.autoinst = autoinst.toArray(new Attribute.AutoInstanced[0]);
            }
            this.curinst = new GLBuffer[this.autoinst.length];
        }

        public void adirty(@SuppressWarnings("rawtypes") GLState.Slot slot) {
            if (slot.id < automask.length) {
                for (int i : automask[slot.id])
                    adirty[i] = true;
            }
        }

        public void autoapply(GOut g, boolean all) {
            if (autolocs == null) {
                autolocs = new VarID[auto.length];
                for (int i = 0; i < auto.length; i++)
                    autolocs[i] = uniform(auto[i]);
            }
            for (int i = 0; i < auto.length; i++) {
                if (all || adirty[i])
                    auto[i].apply(g, autolocs[i]);
                adirty[i] = false;
            }
        }

        public static Program build(Collection<ShaderMacro> mods) {
            ProgramContext prog = new ProgramContext();
            for (ShaderMacro mod : mods)
                mod.modify(prog);
            Program ret = new Program(prog);
            if (dumpall || prog.dump) {
                System.err.println(mods + ": ");
                for (GLShader sh : ret.shaders) {
                    System.err.println("---> " + sh + ": ");
                    System.err.print(sh.source);
                }
                System.err.println();
                System.err.println("-------- " + ret);
                System.err.println();
            }
            return (ret);
        }

        public void dispose() {
            synchronized (this) {
                super.dispose();
                umap.clear();
                amap.clear();
                for (GLBuffer bo : curinst) {
                    if (bo != null)
                        bo.dispose();
                }
            }
        }

        protected void link(GOut g) {
            super.link(g);
            for (Uniform var : built.uniforms) {
                String nm = built.symtab.get(var.name);
                VarID loc = uniform(g, nm);
                umap.put(var, loc);
            }
            for (Attribute var : built.attribs) {
                String nm = built.symtab.get(var.name);
                VarID loc = attrib(g, nm);
                amap.put(var, loc);
            }
        }

        /* XXX: It would be terribly nice to replace these with some faster operation. */
        private final transient Map<Uniform, VarID> umap = new IdentityHashMap<Uniform, VarID>();

        public VarID cuniform(Uniform var) {
            return (umap.get(var));
        }

        public VarID uniform(Uniform var) {
            VarID r = cuniform(var);
            if (r == null)
                throw (new UnknownExternException("Uniform not found in symtab: " + var, this, "uniform", var.toString()));
            return (r);
        }

        private final transient Map<Attribute, VarID> amap = new IdentityHashMap<Attribute, VarID>();

        public VarID cattrib(Attribute var) {
            return (amap.get(var));
        }

        public VarID attrib(Attribute var) {
            VarID r = cattrib(var);
            if (r == null)
                throw (new UnknownExternException("Attribute not found in symtab: " + var, this, "attrib", var.toString()));
            return (r);
        }
    }

    public static final ShaderMacro dump = new ShaderMacro() {
	    public void modify(ProgramContext prog) {
		prog.dump = true;
	    }
	};
}
