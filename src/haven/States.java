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

import haven.glsl.*;
import javax.media.opengl.*;
import java.awt.Color;

public abstract class States extends GLState {
    private States() {}

    public static final Slot<ColState> color = new Slot<ColState>(Slot.Type.DRAW, ColState.class, HavenPanel.global);
    public static class ColState extends GLState {
        public final Color c;
        public final float[] ca;

        public ColState(Color c) {
            this.c = c;
            this.ca = Utils.c2fa(c);
        }

        public ColState(int r, int g, int b, int a) {
            this(Utils.clipcol(r, g, b, a));
        }

        public void apply(GOut g) {
            BGL gl = g.gl;
            gl.glColor4fv(ca, 0);
        }

        public int capply() {
            return(1);
        }

        public void unapply(GOut g) {
            BGL gl = g.gl;
            gl.glColor3f(1, 1, 1);
        }

        public int capplyfrom(GLState o) {
            if(o instanceof ColState)
                return(1);
            return(-1);
        }

        public void applyfrom(GOut g, GLState o) {
            apply(g);
        }

        public ShaderMacro[] shaders() {return(shaders);}

        public void prep(Buffer buf) {
            buf.put(color, this);
        }

        public boolean equals(Object o) {
            return((o instanceof ColState) && c.equals(((ColState)o).c));
        }

        public int hashCode() {
            return(c.hashCode());
        }

        public String toString() {
            return("ColState(" + c + ")");
        }

        private static final ShaderMacro[] shaders = {new haven.glsl.BaseColor()};
    }
    public static final ColState vertexcolor = new ColState(0, 0, 0, 0) {
        private final ShaderMacro[] shaders = {new haven.glsl.GLColorVary()};
        public void apply(GOut g) {}

        public ShaderMacro[] shaders() {return(shaders);}

        public boolean equals(Object o) {
            return(o == this);
        }

        public String toString() {
            return("ColState(vertex)");
        }
    };
    private static class InstColor extends ColState {
        private final ColState[] parts;

        InstColor(ColState[] parts) {
            super(0, 0, 0, 0);
            this.parts = parts;
        }

        public String toString() {
            return("instanced color");
        }

        public boolean equals(Object o) {
            return(o == this);
        }

        static final ShaderMacro[] shaders = {GLState.Instancer.mkinstanced, new BaseColor()};
        public ShaderMacro[] shaders() {return(shaders);}
    }
    static {color.instanced(new Instancer<ColState>() {
        public ColState inststate(ColState[] in) {
            if(in[0] == vertexcolor) {
                for(int i = 1; i < in.length; i++) {
                    if(in[i] != vertexcolor)
                        throw(new RuntimeException("cannot mix uniform and per-vertex coloring in instanced rendering"));
                }
                return(vertexcolor);
            } else {
                for(int i = 1; i < in.length; i++) {
                    if(in[i] == vertexcolor)
                        throw(new RuntimeException("cannot mix uniform and per-vertex coloring in instanced rendering"));
                }
                return(new InstColor(in));
            }
        }
    });}
    @Material.ResName("vcol")
    public static class $vcol implements Material.ResCons {
        public GLState cons(Resource res, Object... args) {
            return(new States.ColState((Color)args[0]));
        }
    }

    public static final StandAlone ndepthtest = new StandAlone(Slot.Type.GEOM, PView.proj) {
        public void apply(GOut g) {
            g.gl.glDisable(GL.GL_DEPTH_TEST);
        }

        public void unapply(GOut g) {
            g.gl.glEnable(GL.GL_DEPTH_TEST);
        }
    };

    public static final GLState xray = compose(ndepthtest, Rendered.last);

    public static final StandAlone fsaa = new StandAlone(Slot.Type.SYS, PView.proj) {
        public void apply(GOut g) {
            g.gl.glEnable(GL.GL_MULTISAMPLE);
        }

        public void unapply(GOut g) {
            g.gl.glDisable(GL.GL_MULTISAMPLE);
        }
    };

    public static final Slot<Coverage> coverage = new Slot<Coverage>(Slot.Type.DRAW, Coverage.class, PView.proj);
    public static class Coverage extends GLState {
        public final float cov;
        public final boolean inv;

        public Coverage(float cov, boolean inv) {
            this.cov = cov;
            this.inv = inv;
        }

        public void apply(GOut g) {
            BGL gl = g.gl;
            gl.glEnable(GL.GL_SAMPLE_COVERAGE);
            gl.glSampleCoverage(cov, inv);
        }

        public void unapply(GOut g) {
            BGL gl = g.gl;
            gl.glSampleCoverage(1.0f, false);
            gl.glDisable(GL.GL_SAMPLE_COVERAGE);
        }

        public void prep(Buffer buf) {
            buf.put(coverage, this);
        }
    };

    public static final StandAlone presdepth = new StandAlone(Slot.Type.GEOM, PView.proj) {
        public void apply(GOut g) {
            g.gl.glDepthMask(false);
        }

        public void unapply(GOut g) {
            g.gl.glDepthMask(true);
        }
    };
    @Material.ResName("maskdepth")
    public static class $maskdepth implements Material.ResCons {
        public GLState cons(Resource res, Object... args) {return(presdepth);}
    }

    public static final StandAlone prescolor = new StandAlone(Slot.Type.DRAW, PView.proj) {
        public void apply(GOut g) {
            g.gl.glColorMask(false, false, false, false);
        }

        public void unapply(GOut g) {
            g.gl.glColorMask(true, true, true, true);
        }
    };
    @Material.ResName("maskcol")
    public static class $colmask implements Material.ResCons {
        public GLState cons(Resource res, Object... args) {
            return(prescolor);
        }
    }

    public static final Slot<Fog> fog = new Slot<Fog>(Slot.Type.DRAW, Fog.class, PView.proj);
    public static class Fog extends GLState {
        public final Color c;
        public final float[] ca;
        public final float s, e;

        public Fog(Color c, float s, float e) {
            this.c = c;
            this.ca = Utils.c2fa(c);
            this.s = s;
            this.e = e;
        }

        public void apply(GOut g) {
            BGL gl = g.gl;
            gl.glFogi(GL2.GL_FOG_MODE, GL2.GL_LINEAR);
            gl.glFogf(GL2.GL_FOG_START, s);
            gl.glFogf(GL2.GL_FOG_END, e);
            gl.glFogfv(GL2.GL_FOG_COLOR, ca, 0);
            gl.glEnable(GL2.GL_FOG);
        }

        public void unapply(GOut g) {
            BGL gl = g.gl;
            gl.glDisable(GL2.GL_FOG);
        }

        public void prep(Buffer buf) {
            buf.put(fog, this);
        }
    }

    public static final Slot<DepthOffset> depthoffset = new Slot<DepthOffset>(Slot.Type.GEOM, DepthOffset.class, PView.proj);
    public static class DepthOffset extends GLState {
        public final int mode;
        public final float factor, units;

        public DepthOffset(int mode, float factor, float units) {
            this.mode = mode;
            this.factor = factor;
            this.units = units;
        }

        public DepthOffset(float factor, float units) {
            this(GL.GL_POLYGON_OFFSET_FILL, factor, units);
        }

        public void apply(GOut g) {
            BGL gl = g.gl;
            gl.glPolygonOffset(factor, units);
            gl.glEnable(mode);
        }

        public void unapply(GOut g) {
            BGL gl = g.gl;
            gl.glDisable(mode);
        }

        public void prep(Buffer buf) {
            buf.put(depthoffset, this);
        }
    }

    public static class PolygonMode extends GLState {
        public static final Slot<PolygonMode> slot = new Slot<PolygonMode>(Slot.Type.GEOM, PolygonMode.class, PView.proj);
        public final int mode;

        public PolygonMode(int mode) {
            this.mode = mode;
        }

        public void apply(GOut g) {
            g.gl.glPolygonMode(GL.GL_FRONT_AND_BACK, mode);
        }

        public void unapply(GOut g) {
            g.gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_FILL);
        }

        public void prep(Buffer buf) {
            buf.put(slot, this);
        }
    }

    public static final StandAlone nullprog = new StandAlone(Slot.Type.DRAW, PView.proj) {
        private final ShaderMacro[] sh = {};

        public void apply(GOut g) {}
        public void unapply(GOut g) {}

        public ShaderMacro[] shaders() {
            return(sh);
        }
    };

    public static final Slot<GLState> adhoc = new Slot<GLState>(Slot.Type.DRAW, GLState.class, PView.wnd);
    public static class AdHoc extends GLState {
        private final ShaderMacro[] sh;

        public AdHoc(ShaderMacro[] sh) {
            this.sh = sh;
        }

        public AdHoc(ShaderMacro sh) {
            this(new ShaderMacro[] {sh});
        }

        public void apply(GOut g) {}
        public void unapply(GOut g) {}

        public ShaderMacro[] shaders() {return(sh);}

        public void prep(Buffer buf) {
            buf.put(adhoc, this);
        }
    }

    public static final Slot<GLState> adhocg = new Slot<GLState>(Slot.Type.GEOM, GLState.class, PView.wnd);
    public static class GeomAdHoc extends GLState {
        private final ShaderMacro[] sh;

        public GeomAdHoc(ShaderMacro[] sh) {
            this.sh = sh;
        }

        public GeomAdHoc(ShaderMacro sh) {
            this(new ShaderMacro[] {sh});
        }

        public void apply(GOut g) {}
        public void unapply(GOut g) {}

        public ShaderMacro[] shaders() {return(sh);}

        public void prep(Buffer buf) {
            buf.put(adhocg, this);
        }
    }

    public static final StandAlone normalize = new StandAlone(Slot.Type.GEOM, PView.proj) {
        public void apply(GOut g) {
            g.gl.glEnable(GL2.GL_NORMALIZE);
        }

        public void unapply(GOut g) {
            g.gl.glDisable(GL2.GL_NORMALIZE);
        }
    };

    public static final Slot<GLState> pointsize = new Slot<GLState>(Slot.Type.GEOM, GLState.class, HavenPanel.global);
    public static class PointSize extends GLState {
        private final float sz;

        public PointSize(float sz) {
            this.sz = sz;
        }

        public void apply(GOut g) {
            g.gl.glPointSize(sz);
        }

        public void unapply(GOut g) {}

        public void prep(Buffer buf) {
            buf.put(pointsize, this);
        }
    }
    public static class ProgPointSize extends GLState {
        public final ShaderMacro[] sh;

        public ProgPointSize(final ShaderMacro sh) {
            this.sh = new ShaderMacro[] {new ShaderMacro() {
                public void modify(ProgramContext prog) {
                    prog.vctx.ptsz.force();
                    sh.modify(prog);
                }
            }};
        }

        public ProgPointSize(final Expression ptsz) {
            this(new ShaderMacro() {
                public void modify(ProgramContext prog) {
                    prog.vctx.ptsz.mod(new Macro1<Expression>() {
                        public Expression expand(Expression in) {
                            return(ptsz);
                        }
                    }, 0);
                }
            });
        }

        public void apply(GOut g) {
            g.gl.glEnable(GL3.GL_PROGRAM_POINT_SIZE);
        }

        public void unapply(GOut g) {
            g.gl.glDisable(GL3.GL_PROGRAM_POINT_SIZE);
        }

        public ShaderMacro[] shaders() {return(sh);}

        public void prep(Buffer buf) {
            buf.put(pointsize, this);
        }
    }
}
