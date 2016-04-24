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

package haven.rs;

import java.awt.image.BufferedImage;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import haven.BGL;
import haven.BufferBGL;
import haven.Callback;
import haven.Coord;
import haven.CurrentGL;
import haven.Drawn;
import haven.GLConfig;
import haven.GLFrameBuffer;
import haven.GLObject;
import haven.GLSettings;
import haven.GLState;
import haven.GOut;
import haven.HavenPanel;
import haven.TexE;

public class GBuffer {
    public final Context ctx;
    public final Coord sz;
    private final GLFrameBuffer buf;
    private final GLState ostate;

    public static class Context {
        private final Object dmon = new Object();
        public final GLProfile prof;
        public final GLAutoDrawable buf;
        private final GLState gstate;
        private GBuffer curdraw;
        private CurrentGL curgl;
        private GLState.Applier state;

        @SuppressWarnings("deprecation")
		public Context() {
            this.prof = GLProfile.getDefault();
            GLDrawableFactory df = GLDrawableFactory.getFactory(prof);
            gstate = new GLState() {
                public void apply(GOut g) {
                    BGL gl = g.gl;
                    gl.glColor3f(1, 1, 1);
                    gl.glPointSize(4);
                    gl.joglSetSwapInterval(1);
                    gl.glEnable(GL.GL_BLEND);
                    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                    if (g.gc.havefsaa())
                        g.gl.glDisable(GL.GL_MULTISAMPLE);
                    GOut.checkerr(gl);
                }

                public void unapply(GOut g) {
                }

                public void prep(Buffer buf) {
                    buf.put(HavenPanel.global, this);
                }
            };
        /* XXX: This seems a bit unreliable. On Xorg with nvidia
         * drivers, an OffscreenAutoDrawable produces no results,
	     * while a Pbuffer works; while on Xvfb with mesa-swx, an
	     * OffscreenAutoDrawable works, while Pbuffer creation
	     * fails. :-/ */
            this.buf = df.createOffscreenAutoDrawable(null, caps(prof), null, 1, 1, null);
            buf.addGLEventListener(new GLEventListener() {
                public void display(GLAutoDrawable d) {
                    GL2 gl = d.getGL().getGL2();
            /* gl = new TraceGL2(gl, System.err) */
                    redraw(gl);
                }

                public void init(GLAutoDrawable d) {
                    GL2 gl = d.getGL().getGL2();
                    if ((curgl == null) || (curgl.gl != gl)) {
                        GLConfig glconf = GLConfig.fromgl(gl, d.getContext(), d.getChosenGLCapabilities());
                        glconf.pref = GLSettings.defconf(glconf);
                        glconf.pref.meshmode.val = GLSettings.MeshMode.MEM;
                        curgl = new CurrentGL(gl, glconf);
                    }
                }

                public void reshape(GLAutoDrawable d, final int x, final int y, final int w, final int h) {
                }

                public void dispose(GLAutoDrawable d) {
                    BufferBGL buf = new BufferBGL();
                    GLObject.disposeall(curgl, buf);
                    buf.run(d.getGL().getGL2());
                }
            });
        }

        protected GLCapabilities caps(GLProfile prof) {
            GLCapabilities ret = new GLCapabilities(prof);
            ret.setDoubleBuffered(true);
            ret.setAlphaBits(8);
            ret.setRedBits(8);
            ret.setGreenBits(8);
            ret.setBlueBits(8);
            ret.setSampleBuffers(true);
            ret.setNumSamples(4);
            return (ret);
        }

        private void redraw(GL2 gl) {
            curdraw.redraw(gl);
        }
    }

    public GBuffer(Context ctx, Coord sz) {
        this.ctx = ctx;
        this.sz = sz;
        buf = new GLFrameBuffer(new TexE(sz, GL.GL_RGBA, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE), null);
        ostate = HavenPanel.OrthoState.fixed(new Coord(sz));
    }

    private static Context defctx = null;

    private static Context defctx() {
        synchronized (GBuffer.class) {
            if (defctx == null)
                defctx = new Context();
        }
        return (defctx);
    }

    public GBuffer(Coord sz) {
        this(defctx(), sz);
    }

    private Drawn curdraw;

    protected void redraw(GL2 gl) {
        if ((ctx.state == null) || (ctx.state.cgl.gl != gl))
            ctx.state = new GLState.Applier(ctx.curgl);
        BufferBGL glbuf = new BufferBGL();

        GLState.Buffer ibuf = new GLState.Buffer(ctx.state.cfg);
        ctx.gstate.prep(ibuf);
        ostate.prep(ibuf);
        buf.prep(ibuf);
        GOut g = new GOut(glbuf, ctx.curgl, ctx.state.cfg, ctx.state, ibuf, sz);
        g.state2d();
        g.apply();
        glbuf.glClearColor(0, 0, 0, 0);
        glbuf.glClear(GL.GL_COLOR_BUFFER_BIT);
        curdraw.draw(g);
        ctx.state.clean();
        GLObject.disposeall(ctx.curgl, glbuf);

        glbuf.run(gl);
    }

    public void render(Drawn thing) {
        synchronized (ctx.dmon) {
            curdraw = thing;
            ctx.curdraw = this;
            try {
                ctx.buf.display();
            } finally {
                curdraw = null;
                ctx.curdraw = null;
            }
        }
    }

    public void dispose() {
        buf.dispose();
    }

    public static void main(String[] args) {
        GBuffer test = new GBuffer(new Coord(250, 250));
        test.render(new Drawn() {
            public void draw(GOut g) {
                g.chcolor(255, 0, 128, 255);
                g.frect(new Coord(50, 50), new Coord(100, 100));
                g.getimage(new Callback<BufferedImage>() {
                    public void done(BufferedImage img) {
                        try {
                            javax.imageio.ImageIO.write(img, "PNG", new java.io.File("/tmp/bard.png"));
                        } catch (java.io.IOException e) {
                            throw (new RuntimeException(e));
                        }
                    }
                });
                GOut.checkerr(g.gl);
            }
        });

    }
}
