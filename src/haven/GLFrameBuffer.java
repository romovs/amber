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

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL3;

public class GLFrameBuffer extends GLState {
    public static final Slot<GLFrameBuffer> slot = new Slot<GLFrameBuffer>(Slot.Type.SYS, GLFrameBuffer.class, HavenPanel.global);
    private final Attachment[] color;
    private final Attachment depth;
    private final RenderBuffer altdepth;
    private FBO fbo;
    private final int[] bufmask;

    public static class FBO extends GLObject implements BGL.ID {
        private int id;

        public FBO(GOut g) {
            super(g);
            g.gl.bglCreate(this);
        }

        public void create(GL2 gl) {
            int[] buf = new int[1];
            gl.glGenFramebuffers(1, buf, 0);
            this.id = buf[0];
            GOut.checkerr(gl);
        }

        protected void delete(BGL gl) {
            BGL.ID[] buf = {this};
            gl.glDeleteFramebuffers(1, buf, 0);
        }

        public int glid() {
            return (id);
        }
    }

    public static class RenderBuffer {
        public final Coord sz;
        public final int samples;
        public final int fmt;
        private RBO rbo;

        public RenderBuffer(Coord sz, int fmt, int samples) {
            this.sz = sz;
            this.fmt = fmt;
            this.samples = samples;
        }

        public RenderBuffer(Coord sz, int fmt) {
            this(sz, fmt, 1);
        }

        public RBO glid(GOut g) {
            BGL gl = g.gl;
            if ((rbo != null) && (rbo.cur != g.curgl))
                dispose();
            if (rbo == null) {
                rbo = new RBO(g);
                gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, rbo);
                if (samples <= 1)
                    gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, fmt, sz.x, sz.y);
                else
                    gl.glRenderbufferStorageMultisample(GL.GL_RENDERBUFFER, samples, fmt, sz.x, sz.y);
            }
            return (rbo);
        }

        public void dispose() {
            synchronized (this) {
                if (rbo != null) {
                    rbo.dispose();
                    rbo = null;
                }
            }
        }

        public static class RBO extends GLObject implements BGL.ID {
            private int id;

            public RBO(GOut g) {
                super(g);
                g.gl.bglCreate(this);
            }

            public void create(GL2 gl) {
                int[] buf = new int[1];
                gl.glGenRenderbuffers(1, buf, 0);
                this.id = buf[0];
                GOut.checkerr(gl);
            }

            protected void delete(BGL gl) {
                BGL.ID[] buf = {this};
                gl.glDeleteRenderbuffers(1, buf, 0);
            }

            public int glid() {
                return (id);
            }
        }
    }

    public static abstract class Attachment {
        public abstract void attach(GOut g, GLFrameBuffer fbo, int point);

        public abstract Coord sz();

        public static Attach2D mk(TexGL tex) {
            return (new Attach2D(tex));
        }

        public static AttachMS mk(TexMS tex) {
            return (new AttachMS(tex));
        }

        public static AttachRBO mk(RenderBuffer rbo) {
            return (new AttachRBO(rbo));
        }
    }

    public static class Attach2D extends Attachment {
        public final TexGL tex;
        public final int level;

        public Attach2D(TexGL tex, int level) {
            this.tex = tex;
            this.level = level;
        }

        public Attach2D(TexGL tex) {
            this(tex, 0);
        }

        public void attach(GOut g, GLFrameBuffer fbo, int point) {
            g.gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, point, GL.GL_TEXTURE_2D, tex.glid(g), level);
        }

        public Coord sz() {
            return (tex.sz());
        }
    }

    public static class AttachMS extends Attachment {
        public final TexMS tex;

        public AttachMS(TexMS tex) {
            this.tex = tex;
        }

        public void attach(GOut g, GLFrameBuffer fbo, int point) {
            g.gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, point, GL3.GL_TEXTURE_2D_MULTISAMPLE, tex.glid(g), 0);
        }

        public Coord sz() {
            return (new Coord(tex.w, tex.h));
        }
    }

    public static class AttachRBO extends Attachment {
        public final RenderBuffer rbo;

        public AttachRBO(RenderBuffer rbo) {
            this.rbo = rbo;
        }

        public void attach(GOut g, GLFrameBuffer fbo, int point) {
            g.gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, point, GL.GL_RENDERBUFFER, rbo.glid(g));
        }

        public Coord sz() {
            return (rbo.sz);
        }
    }

    public GLFrameBuffer(Attachment[] color, Attachment depth) {
        this.color = color;
        this.bufmask = new int[this.color.length];
        if (depth == null) {
            if (this.color.length == 0)
                throw (new RuntimeException("Cannot create a framebuffer with neither color nor depth"));
            this.altdepth = new RenderBuffer(color[0].sz(), GL2.GL_DEPTH_COMPONENT);
            this.depth = new AttachRBO(this.altdepth);
        } else {
            this.altdepth = null;
            this.depth = depth;
        }
    }

    private static Attachment[] javaIsRetarded(TexGL[] color) {
        Attachment[] ret = new Attachment[color.length];
        for (int i = 0; i < color.length; i++)
            ret[i] = new Attach2D(color[i]);
        return (ret);
    }

    public GLFrameBuffer(TexGL[] color, TexGL depth) {
        this(javaIsRetarded(color), (depth == null) ? null : new Attach2D(depth));
    }

    public GLFrameBuffer(TexGL color, TexGL depth) {
        this((color == null) ? (new TexGL[0]) : (new TexGL[]{color}), depth);
    }

    public Coord sz() {
        return (depth.sz());
    }

    public void apply(GOut g) {
        BGL gl = g.gl;
        synchronized (this) {
            if ((fbo != null) && (fbo.cur != g.curgl))
                dispose();
            if (fbo == null) {
                fbo = new FBO(g);
                gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo);
                for (int i = 0; i < color.length; i++)
                    color[i].attach(g, this, GL.GL_COLOR_ATTACHMENT0 + i);
                depth.attach(g, this, GL.GL_DEPTH_ATTACHMENT);
                if (color.length == 0) {
                    gl.glDrawBuffer(GL.GL_NONE);
                    gl.glReadBuffer(GL.GL_NONE);
                } else if (color.length > 1) {
                    for (int i = 0; i < color.length; i++)
                        bufmask[i] = GL.GL_COLOR_ATTACHMENT0 + i;
                    gl.glDrawBuffers(color.length, Utils.splice(bufmask, 0), 0);
                }
                GOut.checkerr(gl);
                gl.bglSubmit(new BGL.Request() {
                    public void run(GL2 gl) {
                        int st = gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);
                        if (st != GL.GL_FRAMEBUFFER_COMPLETE)
                            throw (new RuntimeException("FBO failed completeness test: " + GOut.GLException.constname(st)));
                    }
                });
            } else {
                gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo);
            }
        }
        gl.glViewport(0, 0, sz().x, sz().y);
    }

    public void mask(GOut g, int id, boolean flag) {
        int nb = flag ? (GL.GL_COLOR_ATTACHMENT0 + id) : (GL.GL_NONE);
        if (bufmask[id] != nb) {
            bufmask[id] = nb;
            g.gl.glDrawBuffers(color.length, Utils.splice(bufmask, 0), 0);
        }
    }

    public void unapply(GOut g) {
        BGL gl = g.gl;
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, null);
        gl.glViewport(g.root().ul.x, g.root().ul.y, g.root().sz.x, g.root().sz.y);
    }

    public void prep(Buffer buf) {
        buf.put(slot, this);
    }

    public void dispose() {
        synchronized (this) {
            if (fbo != null) {
                fbo.dispose();
                fbo = null;
            }
        }
        if (altdepth != null)
            altdepth.dispose();
    }
}
