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

import java.awt.Color;
import java.util.Map;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

public abstract class PView extends Widget {
    public RenderList rls;
    public static final GLState.Slot<RenderContext> ctx = new GLState.Slot<RenderContext>(GLState.Slot.Type.SYS, RenderContext.class);
    public static final GLState.Slot<RenderState> wnd = new GLState.Slot<RenderState>(GLState.Slot.Type.SYS, RenderState.class, HavenPanel.proj2d, GLFrameBuffer.slot);
    public static final GLState.Slot<Projection> proj = new GLState.Slot<Projection>(GLState.Slot.Type.SYS, Projection.class, wnd);
    public static final GLState.Slot<Camera> cam = new GLState.Slot<Camera>(GLState.Slot.Type.SYS, Camera.class, proj);
    public static final GLState.Slot<Location.Chain> loc = new GLState.Slot<Location.Chain>(GLState.Slot.Type.GEOM, Location.Chain.class, cam).instanced(Location.Chain.instancer);
    public CPUProfile prof = new CPUProfile(300);
    protected Light.Model lm;
    private final WidgetContext cstate = new WidgetContext();
    private final WidgetRenderState rstate = new WidgetRenderState();
    private GLState pstate;

    public static class RenderContext extends GLState.Abstract {
        @SuppressWarnings("rawtypes")
		private Map<DataID, Object> data = new CacheMap<DataID, Object>(CacheMap.RefType.WEAK);

        public interface DataID<T> {
            public T make(RenderContext c);
        }

        @SuppressWarnings("unchecked")
        public <T> T data(DataID<T> id) {
            T ret = (T) data.get(id);
            if (ret == null)
                data.put(id, ret = id.make(this));
            return (ret);
        }

        public void prep(Buffer b) {
            b.put(ctx, this);
        }

        public Glob glob() {
            return (null);
        }
    }

    public abstract static class ConfContext extends RenderContext implements GLState.GlobalState {
        public FBConfig cfg = new FBConfig(this, sz());
        public FBConfig cur = new FBConfig(this, sz());

        protected abstract Coord sz();

        public Global global(RenderList rl, Buffer ctx) {
            return (glob);
        }

        private final Global glob = new Global() {
            public void postsetup(RenderList rl) {
                cfg.fin(cur);
                cur = cfg;
                cfg = new FBConfig(ConfContext.this, sz());
                if (cur.fb != null) {
                    for (RenderList.Slot s : rl.slots()) {
                        if (s.os.get(ctx) == ConfContext.this)
                            cur.state.prep(s.os);
                    }
                }
            }

            public void prerender(RenderList rl, GOut g) {
            }

            public void postrender(RenderList rl, GOut g) {
            }
        };
    }

    public class WidgetContext extends ConfContext {
        protected Coord sz() {
            return (PView.this.sz);
        }

        public Glob glob() {
            return (ui.sess.glob);
        }

        public PView widget() {
            return (PView.this);
        }
    }

    public static abstract class RenderState extends GLState {
        public void apply(GOut g) {
            BGL gl = g.gl;
            gl.glScissor(g.ul.x, g.root().sz.y - g.ul.y - g.sz.y, g.sz.x, g.sz.y);
        /* For the viewport, use the renderstate's indicated size
         * and offset explicitly, so as to not fail on partially
	     * clipped GOuts. */
            Coord ul = ul();
            Coord sz = sz();
            gl.glViewport(ul.x, g.root().sz.y - ul.y - sz.y, sz.x, sz.y);

            gl.glAlphaFunc(GL.GL_GREATER, 0.5f);
            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glEnable(GL.GL_CULL_FACE);
            gl.glEnable(GL.GL_SCISSOR_TEST);
            gl.glDepthFunc(GL.GL_LEQUAL);
            gl.glClearDepth(1.0);
        }

        public void unapply(GOut g) {
            BGL gl = g.gl;

            gl.glDisable(GL.GL_DEPTH_TEST);
            gl.glDisable(GL.GL_CULL_FACE);
            gl.glDisable(GL.GL_SCISSOR_TEST);

            gl.glViewport(g.root().ul.x, g.root().ul.y, g.root().sz.x, g.root().sz.y);
            gl.glScissor(g.root().ul.x, g.root().ul.y, g.root().sz.x, g.root().sz.y);
        }

        public void prep(Buffer b) {
            b.put(wnd, this);
        }

        public abstract Coord ul();

        public abstract Coord sz();
    }

    private class WidgetRenderState extends RenderState {
        public Coord ul() {
            return (rootpos());
        }

        public Coord sz() {
            return (PView.this.sz);
        }
    }

    public PView(Coord sz) {
        super(sz);
        pstate = makeproj();
        lm = new Light.Model();
        lm.cc = GL2.GL_SEPARATE_SPECULAR_COLOR;
    }

    protected GLState.Buffer basic(GOut g) {
        GLState.Buffer buf = g.basicstate();
        cstate.prep(buf);
        rstate.prep(buf);
        if (pstate != null)
            pstate.prep(buf);
        camera().prep(buf);
        if (ui.audio != null)
            ui.audio.prep(buf);
        return (buf);
    }

    protected abstract GLState camera();

    protected abstract void setup(RenderList rls);

    protected Projection makeproj() {
        float field = 0.5f;
        float aspect = ((float) sz.y) / ((float) sz.x);
        return (Projection.frustum(-field, field, -aspect * field, aspect * field, 1, 5000));
    }

    public void resize(Coord sz) {
        super.resize(sz);
        pstate = makeproj();
    }

    private final Rendered scene = new Rendered() {
        public void draw(GOut g) {
        }

        public boolean setup(RenderList rl) {
            PView.this.setup(rl);
            return (false);
        }
    };

    protected Color clearcolor() {
        return (Color.BLACK);
    }

    public void draw(GOut g) {
        if ((g.sz.x < 1) || (g.sz.y < 1))
            return;
        if ((rls == null) || (rls.cfg != g.gc))
            rls = new RenderList(g.gc);
        CPUProfile.Frame curf = null;
        if (Config.profile)
            curf = prof.new Frame();
        GLState.Buffer bk = g.st.copy();
        GLState.Buffer def = basic(g);
        if (g.gc.pref.fsaa.val)
            States.fsaa.prep(def);
        try {
            lm.prep(def);
            new Light.LightList().prep(def);
            rls.setup(scene, def);
            if (curf != null)
                curf.tick("setup");
            rls.fin();
            if (curf != null)
                curf.tick("sort");
            GOut rg;
            if (cstate.cur.fb != null) {
                GLState.Buffer gb = g.basicstate();
                HavenPanel.OrthoState.fixed(cstate.cur.fb.sz()).prep(gb);
                cstate.cur.fb.prep(gb);
                cstate.cur.fb.prep(def);
                rg = new GOut(g.gl, g.curgl, g.gc, g.st, gb, cstate.cur.fb.sz());
            } else {
                rg = g;
            }
            rg.st.set(def);
            Color cc = clearcolor();
            if ((cc == null) && (cstate.cur.fb != null))
                cc = new Color(0, 0, 0, 0);
            rg.apply();
            BGL gl = rg.gl;
            if (cc == null) {
                gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
            } else {
                gl.glClearColor((float) cc.getRed() / 255f, (float) cc.getGreen() / 255f, (float) cc.getBlue() / 255f, (float) cc.getAlpha() / 255f);
                gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);
            }
            if (curf != null)
                curf.tick("cls");
            g.st.time = 0;
            rls.render(rg);
            if (cstate.cur.fb != null)
                cstate.cur.resolve(g);
            if (curf != null) {
                curf.add("apply", g.st.time);
                curf.tick("render", g.st.time);
            }
        } finally {
            g.st.set(bk);
        }
        for (RenderList.Slot s : rls.slots()) {
            if (!s.d)
                break;
            if (s.r instanceof Render2D)
                ((Render2D) s.r).draw2d(g);
        }
        if (curf != null)
            curf.tick("2d");
        if (curf != null)
            curf.fin();
    }

    public interface Render2D extends Rendered {
        public void draw2d(GOut g);
    }

    public static abstract class Draw2D implements Render2D {
        public void draw(GOut g) {
        }

        public boolean setup(RenderList r) {
            return (true);
        }
    }

    public static Matrix4f camxf(GLState.Buffer buf) {
        Camera cam_s = buf.get(cam);
        return ((cam_s == null) ? Matrix4f.id : cam_s.fin(Matrix4f.id));
    }

    public static Matrix4f camxf(GOut g) {
        return (camxf(g.st.cstate()));
    }

    public static Matrix4f locxf(GLState.Buffer buf) {
        Location.Chain loc_s = buf.get(loc);
        return ((loc_s == null) ? Matrix4f.id : loc_s.fin(Matrix4f.id));
    }

    public static Matrix4f locxf(GOut g) {
        return (locxf(g.st.cstate()));
    }

    public static Matrix4f mvxf(GOut g, GLState.Buffer buf) {
        Camera cam_s = buf.get(cam);
        Location.Chain loc_s = buf.get(loc);
        Matrix4f ret = Matrix4f.id;
        if (cam_s != null) ret = cam_s.fin(ret);
        if (loc_s != null) ret = loc_s.fin(ret);
        return (ret);
    }

    public static Matrix4f mvxf(GOut g) {
        return (mvxf(g, g.st.cstate()));
    }

    public static Matrix4f pmvxf(GOut g, GLState.Buffer buf) {
        Camera cam_s = buf.get(cam);
        Location.Chain loc_s = buf.get(loc);
        Matrix4f ret = g.st.proj;
        if (cam_s != null) ret = cam_s.fin(ret);
        if (loc_s != null) ret = loc_s.fin(ret);
        return (ret);
    }

    public static Matrix4f pmvxf(GOut g) {
        return (pmvxf(g, g.st.cstate()));
    }
}
