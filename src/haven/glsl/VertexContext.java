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
import haven.GLProgram.VarID;

public class VertexContext extends ShaderContext {
    public VertexContext(ProgramContext prog) {
        super(prog);
    }

    public final Function.Def main = new Function.Def(Type.VOID, new Symbol.Fix("main"));
    public final ValBlock mainvals = new ValBlock();
    private final OrderList<CodeMacro> code = new OrderList<CodeMacro>();
    {
        code.add(new CodeMacro() {
            public void expand(Block blk) {
                mainvals.cons(blk);
            }
        }, 0);
    }

    public static final Variable gl_Vertex = new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_Vertex"));
    public static final Variable gl_Normal = new Variable.Implicit(Type.VEC3, new Symbol.Fix("gl_Normal"));
    public static final Variable gl_Color = new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_Color"));
    public static final Variable gl_Position = new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_Position"));
    public static final Variable gl_PointSize = new Variable.Implicit(Type.FLOAT, new Symbol.Fix("gl_PointSize"));
    public static final Variable[] gl_MultiTexCoord = {
            new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_MultiTexCoord0")),
            new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_MultiTexCoord1")),
            new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_MultiTexCoord2")),
            new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_MultiTexCoord3")),
            new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_MultiTexCoord4")),
            new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_MultiTexCoord5")),
            new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_MultiTexCoord6")),
            new Variable.Implicit(Type.VEC4, new Symbol.Fix("gl_MultiTexCoord7")),
    };

    private static final Uniform u_proj = new Uniform.AutoApply(Type.MAT4, "proj", PView.proj) {
        public void apply(GOut g, VarID loc) {
            g.gl.glUniformMatrix4fv(loc, 1, false, g.st.proj.m, 0);
        }
    };
    private static final Uniform u_cam = new Uniform.AutoApply(Type.MAT4, "cam", PView.cam) {
        public void apply(GOut g, VarID loc) {
            g.gl.glUniformMatrix4fv(loc, 1, false, PView.camxf(g).m, 0);
        }
    };
    private static final InstancedUniform u_wxf = new InstancedUniform.Mat4("wxf", PView.loc) {
        public Matrix4f forstate(GOut g, GLState.Buffer buf) {
            return(PView.locxf(buf));
        }
    };
    private static final Uniform u_mv = new Uniform.AutoApply(Type.MAT4, "mv", PView.loc, PView.cam) {
        public void apply(GOut g, VarID loc) {
            g.gl.glUniformMatrix4fv(loc, 1, false, PView.mvxf(g).m, 0);
        }
    };
    private static final Uniform u_pmv = new Uniform.AutoApply(Type.MAT4, "pmv", PView.loc, PView.cam, PView.proj) {
        public void apply(GOut g, VarID loc) {
            g.gl.glUniformMatrix4fv(loc, 1, false, PView.pmvxf(g).m, 0);
        }
    };

    private static final PostProc.AutoID xfpp = new PostProc.AutoID(1000);
    private boolean xfpinited = false, h_wxf = false, h_mv = false;
    private static class WorldTransform extends PostProc.AutoMacro {
        final Expression v;
        WorldTransform(Expression v) {super(xfpp); this.v = v;}
        public Expression expand(Context ctx) {
            return(new Mul(u_wxf.ref(), v));
        }
    }
    private static class MVTransform extends PostProc.AutoMacro {
        final Expression v; final boolean norm;
        MVTransform(Expression v, boolean norm) {super(xfpp); this.v = v; this.norm = norm;}
        private Expression norm(Expression xf) {return(norm?new Mat3Cons(xf):xf);}
        public Expression expand(Context ctx) {
            VertexContext vctx = (VertexContext)ctx;
            vctx.xfpinit();
            if(vctx.h_wxf)
                return(new Mul(norm(u_cam.ref()), norm(u_wxf.ref()), v));
            else
                return(new Mul(norm(u_mv.ref()), v));
        }
    }
    private static class PMVTransform extends PostProc.AutoMacro {
        final Expression v;
        PMVTransform(Expression v) {super(xfpp); this.v = v;}
        public Expression expand(Context ctx) {
            VertexContext vctx = (VertexContext)ctx;
            vctx.xfpinit();
            if(vctx.h_wxf)
                return(new Mul(u_proj.ref(), u_cam.ref(), u_wxf.ref(), v));
            else if(vctx.h_mv)
                return(new Mul(u_proj.ref(), u_mv.ref(), v));
            else
                return(new Mul(u_pmv.ref(), v));
        }
    }
    private void xfpinit() {
        if(xfpinited)
            return;
        walk(new Walker() {
            public void el(Element e) {
                if(e instanceof WorldTransform) h_wxf = true;
                else if(e instanceof MVTransform) h_mv = true;
                e.walk(this);
            }
        });
        if(prog.instanced)
            h_wxf = true;
        xfpinited = true;
    }

    public static Expression camxf(Expression v) {
        return(new Mul(u_cam.ref(), v));
    }
    public static Expression projxf(Expression v) {
        return(new Mul(u_proj.ref(), v));
    }
    public static Expression wxf(Expression v) {
        return(new WorldTransform(v));
    }
    public static Expression mvxf(Expression v) {
        return(new MVTransform(v, false));
    }
    public static Expression pmvxf(Expression v) {
        return(new PMVTransform(v));
    }

    /* If, at some unexpected point in an unexpected future, I were
     * to use anisotropic transforms, this will have to get a matrix
     * inverter implemented for it. */
    public Expression nxf(Expression v) {
        return(new MVTransform(v, true));
    }

    public final ValBlock.Value objv = mainvals.new Value(Type.VEC4, new Symbol.Gen("objv")) {
        public Expression root() {
            return(gl_Vertex.ref());
        }
    };
    public final ValBlock.Value mapv = mainvals.new Value(Type.VEC4, new Symbol.Gen("mapv")) {
        {softdep(objv);}

        public Expression root() {
            return(new PostProc.AutoMacro(PostProc.misc) {
                public Expression expand(Context ctx) {
                    if(objv.used) {
                        return(wxf(objv.ref()));
                    } else {
                        return(wxf(gl_Vertex.ref()));
                    }
                }
            });
        }
    };
    public final ValBlock.Value eyev = mainvals.new Value(Type.VEC4, new Symbol.Gen("eyev")) {
        {softdep(objv); softdep(mapv);}

        public Expression root() {
            return(new PostProc.AutoMacro(PostProc.misc) {
                public Expression expand(Context ctx) {
                    if(mapv.used) {
                        return(camxf(mapv.ref()));
                    } else if(objv.used) {
                        return(mvxf(objv.ref()));
                    } else {
                        return(mvxf(gl_Vertex.ref()));
                    }
                }
            });
        }
    };
    public final ValBlock.Value eyen = mainvals.new Value(Type.VEC3, new Symbol.Gen("eyen")) {
        public Expression root() {
            return(nxf(gl_Normal.ref()));
        }
    };
    public final ValBlock.Value posv = mainvals.new Value(Type.VEC4, new Symbol.Gen("posv")) {
        {
            softdep(objv); softdep(mapv); softdep(eyev);
            force();
        }

        public Expression root() {
            return(new PostProc.AutoMacro(PostProc.misc) {
                public Expression expand(Context ctx) {
                    if(eyev.used) {
                        return(projxf(eyev.ref()));
                    } else if(mapv.used) {
                        return(projxf(camxf(mapv.ref())));
                    } else if(objv.used) {
                        return(pmvxf(objv.ref()));
                    } else {
                        return(pmvxf(gl_Vertex.ref()));
                    }
                }
            });
        }

        protected void cons2(Block blk) {
            tgt = gl_Position.ref();
            blk.add(new LBinOp.Assign(tgt, init));
        }
    };
    public final ValBlock.Value ptsz = mainvals.new Value(Type.FLOAT, new Symbol.Gen("ptsz")) {
        public Expression root() {
            return(new FloatLiteral(1.0));
        }

        protected void cons2(Block blk) {
            tgt = gl_PointSize.ref();
            blk.add(new LBinOp.Assign(tgt, init));
        }
    };

    public void mainmod(CodeMacro macro, int order) {
        code.add(macro, order);
    }

    public void construct(java.io.Writer out) {
        for(CodeMacro macro : code)
            macro.expand(main.code);
        main.define(this);
        PostProc.autoproc(this);
        output(new Output(out, this));
    }
}
