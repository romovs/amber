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

import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

public interface Rendered extends Drawn {
    public boolean setup(RenderList r);
    public static final Object CONSTANS = new Object();

    public default Object staticp() {return(null);}

    public static interface Instanced extends Rendered {
        public Rendered instanced(GLConfig gc, List<GLState.Buffer> instances);
    }

    public static interface RComparator<T extends Rendered> {
        public int compare(T a, T b, GLState.Buffer sa, GLState.Buffer sb);
    }

    public static final GLState.Slot<Order> order = new GLState.Slot<Order>(GLState.Slot.Type.GEOM, Order.class, HavenPanel.global);
    public static abstract class Order<T extends Rendered> extends GLState {
        public abstract int mainz();
        public abstract RComparator<? super T> cmp();

        public void apply(GOut g) {}
        public void unapply(GOut g) {}
        public void prep(GLState.Buffer buf) {
            buf.put(order, this);
        }

        public static class Default extends Order<Rendered> {
            private final int z;

            public Default(int z) {
                this.z = z;
            }

            public int mainz() {
                return(z);
            }

            private final static RComparator<Rendered> cmp = new RComparator<Rendered>() {
                public int compare(Rendered a, Rendered b, GLState.Buffer sa, GLState.Buffer sb) {
                    return(0);
                }
            };

            public RComparator<Rendered> cmp() {
                return(cmp);
            }
        }
    }

    public final static Order<?> deflt = new Order.Default(0);
    public final static Order<?> first = new Order.Default(Integer.MIN_VALUE);
    public final static Order<?> last = new Order.Default(Integer.MAX_VALUE);
    public final static Order<?> postfx = new Order.Default(5000);
    public final static Order<?> postpfx = new Order.Default(5500);

    public static class EyeOrder extends Order.Default {
        public EyeOrder(int z) {super(z);}

        private static final RComparator<Rendered> cmp = new RComparator<Rendered>() {
            public int compare(Rendered a, Rendered b, GLState.Buffer sa, GLState.Buffer sb) {
		/* It would be nice to be able to cache these
		 * results somewhere. */
                Camera ca = sa.get(PView.cam);
                Location.Chain la = sa.get(PView.loc);
                Matrix4f mva = ca.fin(Matrix4f.id).mul(la.fin(Matrix4f.id));
                float da = (float)Math.sqrt((mva.m[12] * mva.m[12]) + (mva.m[13] * mva.m[13]) + (mva.m[14] * mva.m[14]));
                Camera cb = sb.get(PView.cam);
                Location.Chain lb = sb.get(PView.loc);
                Matrix4f mvb = cb.fin(Matrix4f.id).mul(lb.fin(Matrix4f.id));
                float db = (float)Math.sqrt((mvb.m[12] * mvb.m[12]) + (mvb.m[13] * mvb.m[13]) + (mvb.m[14] * mvb.m[14]));
                if(da < db)
                    return(1);
                else if(da > db)
                    return(-1);
                else
                    return(0);
            }
        };

        public RComparator<Rendered> cmp() {
            return(cmp);
        }
    }

    public final static Order<?> eyesort = new EyeOrder(10000);
    public final static Order<?> eeyesort = new EyeOrder(4500);

    public final static GLState.StandAlone skip = new GLState.StandAlone(GLState.Slot.Type.GEOM, HavenPanel.global) {
        public void apply(GOut g) {}
        public void unapply(GOut g) {}
    };

    public static class Dot implements Rendered {
        public void draw(GOut g) {
            BGL gl = g.gl;
            g.st.put(Light.lighting, null);
            g.state(States.xray);
            g.apply();
            gl.glBegin(GL2.GL_POINTS);
            gl.glColor3f(1.0f, 0.0f, 0.0f);
            gl.glVertex3f(0.0f, 0.0f, 0.0f);
            gl.glEnd();
        }

        public boolean setup(RenderList r) {
            return(true);
        }
    }

    public static class Axes implements Rendered {
        public final float[] mid;

        public Axes(java.awt.Color mid) {
            this.mid = Utils.c2fa(mid);
        }

        public Axes() {
            this(java.awt.Color.BLACK);
        }

        public void draw(GOut g) {
            BGL gl = g.gl;
            g.st.put(Light.lighting, null);
            g.state(States.xray);
            g.apply();
            gl.glBegin(GL.GL_LINES);
            gl.glColor4fv(mid, 0);
            gl.glVertex3f(0, 0, 0);
            gl.glColor3f(1, 0, 0);
            gl.glVertex3f(1, 0, 0);
            gl.glColor4fv(mid, 0);
            gl.glVertex3f(0, 0, 0);
            gl.glColor3f(0, 1, 0);
            gl.glVertex3f(0, 1, 0);
            gl.glColor4fv(mid, 0);
            gl.glVertex3f(0, 0, 0);
            gl.glColor3f(0, 0, 1);
            gl.glVertex3f(0, 0, 1);
            gl.glEnd();
        }

        public boolean setup(RenderList r) {
            r.state().put(States.color, null);
            return(true);
        }
    }

    public static class Line implements Rendered {
        public final Coord3f end;

        public Line(Coord3f end) {
            this.end = end;
        }

        public void draw(GOut g) {
            BGL gl = g.gl;
            g.apply();
            gl.glBegin(GL.GL_LINES);
            gl.glColor3f(1, 0, 0);
            gl.glVertex3f(0, 0, 0);
            gl.glColor3f(0, 1, 0);
            gl.glVertex3f(end.x, end.y, end.z);
            gl.glEnd();
        }

        public boolean setup(RenderList r) {
            r.state().put(States.color, null);
            r.state().put(Light.lighting, null);
            return(true);
        }
    }

    public static class Cube implements Rendered {
        public void draw(GOut g) {
            BGL gl = g.gl;
            g.apply();

            gl.glEnable(GL2.GL_COLOR_MATERIAL);
            gl.glBegin(GL2.GL_QUADS);
            gl.glNormal3f(0.0f, 0.0f, 1.0f);
            gl.glColor3f(0.0f, 0.0f, 1.0f);
            gl.glVertex3f(-1.0f, 1.0f, 1.0f);
            gl.glVertex3f(-1.0f, -1.0f, 1.0f);
            gl.glVertex3f(1.0f, -1.0f, 1.0f);
            gl.glVertex3f(1.0f, 1.0f, 1.0f);

            gl.glNormal3f(1.0f, 0.0f, 0.0f);
            gl.glColor3f(1.0f, 0.0f, 0.0f);
            gl.glVertex3f(1.0f, 1.0f, 1.0f);
            gl.glVertex3f(1.0f, -1.0f, 1.0f);
            gl.glVertex3f(1.0f, -1.0f, -1.0f);
            gl.glVertex3f(1.0f, 1.0f, -1.0f);

            gl.glNormal3f(-1.0f, 0.0f, 0.0f);
            gl.glColor3f(0.0f, 1.0f, 1.0f);
            gl.glVertex3f(-1.0f, 1.0f, 1.0f);
            gl.glVertex3f(-1.0f, 1.0f, -1.0f);
            gl.glVertex3f(-1.0f, -1.0f, -1.0f);
            gl.glVertex3f(-1.0f, -1.0f, 1.0f);

            gl.glNormal3f(0.0f, 1.0f, 0.0f);
            gl.glColor3f(0.0f, 1.0f, 0.0f);
            gl.glVertex3f(-1.0f, 1.0f, 1.0f);
            gl.glVertex3f(1.0f, 1.0f, 1.0f);
            gl.glVertex3f(1.0f, 1.0f, -1.0f);
            gl.glVertex3f(-1.0f, 1.0f, -1.0f);

            gl.glNormal3f(0.0f, -1.0f, 0.0f);
            gl.glColor3f(1.0f, 0.0f, 1.0f);
            gl.glVertex3f(-1.0f, -1.0f, 1.0f);
            gl.glVertex3f(-1.0f, -1.0f, -1.0f);
            gl.glVertex3f(1.0f, -1.0f, -1.0f);
            gl.glVertex3f(1.0f, -1.0f, 1.0f);

            gl.glNormal3f(0.0f, 0.0f, -1.0f);
            gl.glColor3f(1.0f, 1.0f, 0.0f);
            gl.glVertex3f(-1.0f, 1.0f, -1.0f);
            gl.glVertex3f(1.0f, 1.0f, -1.0f);
            gl.glVertex3f(1.0f, -1.0f, -1.0f);
            gl.glVertex3f(-1.0f, -1.0f, -1.0f);
            gl.glEnd();
            gl.glColor3f(1.0f, 1.0f, 1.0f);
            gl.glDisable(GL2.GL_COLOR_MATERIAL);
        }

        public boolean setup(RenderList rls) {
            rls.state().put(States.color, null);
            return(true);
        }
    }

    public static class TCube implements Rendered {
        public final Coord3f bn, bp;
        public States.ColState sc = new States.ColState(new java.awt.Color(255, 64, 64, 128)), ec = new States.ColState(new java.awt.Color(255, 255, 255, 255));

        public TCube(Coord3f bn, Coord3f bp) {
            this.bn = bn;
            this.bp = bp;
        }

        public void draw(GOut g) {
            BGL gl = g.gl;

            g.state(Light.deflight);
            g.state(sc);
            g.apply();
            gl.glEnable(GL2.GL_COLOR_MATERIAL);
            gl.glBegin(GL2.GL_QUADS);
            gl.glNormal3f(0.0f, 0.0f, 1.0f);
            gl.glVertex3f(bn.x, bp.y, bp.z);
            gl.glVertex3f(bn.x, bn.y, bp.z);
            gl.glVertex3f(bp.x, bn.y, bp.z);
            gl.glVertex3f(bp.x, bp.y, bp.z);

            gl.glNormal3f(1.0f, 0.0f, 0.0f);
            gl.glVertex3f(bp.x, bp.y, bp.z);
            gl.glVertex3f(bp.x, bn.y, bp.z);
            gl.glVertex3f(bp.x, bn.y, bn.z);
            gl.glVertex3f(bp.x, bp.y, bn.z);

            gl.glNormal3f(-1.0f, 0.0f, 0.0f);
            gl.glVertex3f(bn.x, bp.y, bp.z);
            gl.glVertex3f(bn.x, bp.y, bn.z);
            gl.glVertex3f(bn.x, bn.y, bn.z);
            gl.glVertex3f(bn.x, bn.y, bp.z);

            gl.glNormal3f(0.0f, 1.0f, 0.0f);
            gl.glVertex3f(bn.x, bp.y, bp.z);
            gl.glVertex3f(bp.x, bp.y, bp.z);
            gl.glVertex3f(bp.x, bp.y, bn.z);
            gl.glVertex3f(bn.x, bp.y, bn.z);

            gl.glNormal3f(0.0f, -1.0f, 0.0f);
            gl.glVertex3f(bn.x, bn.y, bp.z);
            gl.glVertex3f(bn.x, bn.y, bn.z);
            gl.glVertex3f(bp.x, bn.y, bn.z);
            gl.glVertex3f(bp.x, bn.y, bp.z);

            gl.glNormal3f(0.0f, 0.0f, -1.0f);
            gl.glVertex3f(bn.x, bp.y, bn.z);
            gl.glVertex3f(bp.x, bp.y, bn.z);
            gl.glVertex3f(bp.x, bn.y, bn.z);
            gl.glVertex3f(bn.x, bn.y, bn.z);
            gl.glEnd();
            gl.glDisable(GL2.GL_COLOR_MATERIAL);

            g.st.put(Light.lighting, null);
            g.state(ec);
            g.apply();
            gl.glLineWidth(1.2f);
            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3f(bn.x, bn.y, bp.z);
            gl.glVertex3f(bn.x, bp.y, bp.z);
            gl.glVertex3f(bp.x, bp.y, bp.z);
            gl.glVertex3f(bp.x, bn.y, bp.z);
            gl.glVertex3f(bn.x, bn.y, bp.z);
            gl.glEnd();
            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3f(bn.x, bn.y, bn.z);
            gl.glVertex3f(bn.x, bp.y, bn.z);
            gl.glVertex3f(bp.x, bp.y, bn.z);
            gl.glVertex3f(bp.x, bn.y, bn.z);
            gl.glVertex3f(bn.x, bn.y, bn.z);
            gl.glEnd();
            gl.glBegin(GL2.GL_LINES);
            gl.glVertex3f(bn.x, bn.y, bn.z); gl.glVertex3f(bn.x, bn.y, bp.z);
            gl.glVertex3f(bp.x, bn.y, bn.z); gl.glVertex3f(bp.x, bn.y, bp.z);
            gl.glVertex3f(bp.x, bp.y, bn.z); gl.glVertex3f(bp.x, bp.y, bp.z);
            gl.glVertex3f(bn.x, bp.y, bn.z); gl.glVertex3f(bn.x, bp.y, bp.z);
            gl.glEnd();
            gl.glPointSize(5);
            gl.glBegin(GL2.GL_POINTS);
            gl.glVertex3f(bn.x, bn.y, bn.z); gl.glVertex3f(bn.x, bn.y, bp.z);
            gl.glVertex3f(bp.x, bn.y, bn.z); gl.glVertex3f(bp.x, bn.y, bp.z);
            gl.glVertex3f(bp.x, bp.y, bn.z); gl.glVertex3f(bp.x, bp.y, bp.z);
            gl.glVertex3f(bn.x, bp.y, bn.z); gl.glVertex3f(bn.x, bp.y, bp.z);
            gl.glEnd();
        }

        public boolean setup(RenderList rls) {
            rls.state().put(States.color, null);
            rls.prepo(eyesort);
            rls.prepo(States.presdepth);
            return(true);
        }
    }

    public static class ScreenQuad implements Rendered {
        private static final Projection proj = new Projection(Matrix4f.id);
        private static final VertexBuf.VertexArray pos = new VertexBuf.VertexArray(Utils.bufcp(new float[] {
                -1, -1, 0,
                1, -1, 0,
                1,  1, 0,
                -1,  1, 0,
        }));
        private static final VertexBuf.TexelArray tex = new VertexBuf.TexelArray(Utils.bufcp(new float[] {
                0, 0,
                1, 0,
                1, 1,
                0, 1,
        }));
        public static final GLState state = new GLState.Abstract() {
            public void prep(Buffer buf) {
                proj.prep(buf);
                States.ndepthtest.prep(buf);
                States.presdepth.prep(buf);
                buf.put(PView.cam, null);
                buf.put(PView.loc, null);
            }
        };

        public void draw(GOut g) {
            BGL gl = g.gl;
            g.apply();
            pos.bind(g, false);
            tex.bind(g, false);
            gl.glDrawArrays(GL2.GL_QUADS, 0, 4);
            pos.unbind(g);
            tex.unbind(g);
        }

        public boolean setup(RenderList rls) {
            rls.prepo(state);
            return(true);
        }
    }
}
 
