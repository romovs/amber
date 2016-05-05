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

import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL2;
import javax.media.opengl.GL3;

public abstract class BGL {
    protected static abstract class Command {
        public abstract void run(GL2 gl);
    }

    private static class BufState {
        Buffer buf;
        int position, limit;

        BufState(Buffer buf, int position) {
            if ((this.buf = buf) != null) {
                this.position = position;
                limit = buf.limit();
            }
        }

        BufState(Buffer buf) {
            if ((this.buf = buf) != null) {
                position = buf.position();
                limit = buf.limit();
            }
        }

        void restore() {
            if (buf != null) {
                buf.position(position);
                buf.limit(limit);
            }
        }
    }

    public interface ID {
        public int glid();
    }

    public interface Request {
        public void run(GL2 gl);
    }

    protected abstract void add(Command cmd);

    protected abstract Iterable<Command> dump();
    @SuppressWarnings("serial")
    //throw (new BGLException(this, list[i], exc));
    public static class BGLException extends RuntimeException {
        public final Dump dump;

        public BGLException(BGL buf, Command mark, Throwable cause) {
            super(cause);
            dump = new Dump(buf, mark);
        }
    }

    public void bglCheckErr() {
        add(new Command() {
            public void run(GL2 gl) {
                GOut.checkerr(gl);
            }
        });
    }

    public void bglCreate(final GLObject ob) {
        add(new Command() {
            public void run(GL2 gl) {
                ob.create(gl);
            }
        });
    }

    public void bglSubmit(final Request req) {
        add(new Command() {
            public void run(GL2 gl) {
                req.run(gl);
            }
        });
    }

    public void bglCopyBufferf(final FloatBuffer dst, final int doff, final FloatBuffer src, final int soff, final int len) {
        add(new Command() {
            public void run(GL2 gl) {
                dst.position(doff);
                src.position(soff).limit(len);
                dst.put(src);
                dst.rewind();
                src.rewind().limit(src.capacity());
            }
        });
    }

    public void bglCopyBufferf(final FloatBuffer dst, final int doff, final float[] src, final int soff, final int len) {
        add(new Command() {
            public void run(GL2 gl) {
                dst.position(doff);
                dst.put(src, soff, len);
                dst.rewind();
            }
        });
    }

    public void glActiveTexture(final int texture) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glActiveTexture(texture);
            }
        });
    }

    public void glAlphaFunc(final int func, final float val) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glAlphaFunc(func, val);
            }
        });
    }

    public void glAttachShader(final ID program, final ID shader) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glAttachShader(program.glid(), shader.glid());
            }
        });
    }

    public void glBegin(final int mode) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glBegin(mode);
            }
        });
    }

    public void glBindBuffer(final int target, final ID buffer) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glBindBuffer(target, (buffer == null) ? 0 : buffer.glid());
            }
        });
    }

    public void glBindFramebuffer(final int target, final ID buffer) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glBindFramebuffer(target, (buffer == null) ? 0 : buffer.glid());
            }
        });
    }

    public void glBindRenderbuffer(final int target, final ID buffer) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glBindRenderbuffer(target, (buffer == null) ? 0 : buffer.glid());
            }
        });
    }

    public void glBindTexture(final int target, final ID texture) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glBindTexture(target, (texture == null) ? 0 : texture.glid());
            }
        });
    }

    public void glBindVertexArray(final ID array) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glBindVertexArray((array == null) ? 0 : array.glid());
            }
        });
    }

    public void glBlendEquationSeparate(final int cmode, final int amode) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glBlendEquationSeparate(cmode, amode);
            }
        });
    }

    public void glBlendFunc(final int sfac, final int dfac) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glBlendFunc(sfac, dfac);
            }
        });
    }

    public void glBufferData(final int target, final long size, Buffer data, final int usage) {
        final BufState ds = new BufState(data);
        add(new Command() {
            public void run(GL2 gl) {
                ds.restore();
                gl.glBufferData(target, size, ds.buf, usage);
            }
        });
    }

    public void glCallList(final ID list) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glCallList(list.glid());
            }
        });
    }

    public void glClear(final int mask) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glClear(mask);
            }
        });
    }

    public void glClearColor(final float r, final float g, final float b, final float a) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glClearColor(r, g, b, a);
            }
        });
    }

    public void glClearDepth(final double d) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glClearDepth(d);
            }
        });
    }

    public void glColor3f(final float r, final float g, final float b) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glColor3f(r, g, b);
            }
        });
    }

    public void glColor4f(final float r, final float g, final float b, final float a) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glColor4f(r, g, b, a);
            }
        });
    }

    public void glColor4fv(final float[] v, final int n) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glColor4fv(v, n);
            }
        });
    }

    public void glColorMask(final boolean r, final boolean g, final boolean b, final boolean a) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glColorMask(r, g, b, a);
            }
        });
    }

    public void glColorMaterial(final int face, final int mode) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glColorMaterial(face, mode);
            }
        });
    }

    public void glColorPointer(final int size, final int type, final int stride, final long pointer) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glColorPointer(size, type, stride, pointer);
            }
        });
    }

    public void glColorPointer(final int size, final int type, final int stride, Buffer data) {
        final BufState ds = new BufState(data);
        add(new Command() {
            public void run(GL2 gl) {
                ds.restore();
                gl.glColorPointer(size, type, stride, ds.buf);
            }
        });
    }

    public void glColorPointer(final int size, final int type, final int stride, Buffer data, int position) {
        final BufState ds = new BufState(data, position);
        add(new Command() {
            public void run(GL2 gl) {
                ds.restore();
                gl.glColorPointer(size, type, stride, ds.buf);
            }
        });
    }

    public void glCompileShaderARB(final ID shader) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glCompileShaderARB(shader.glid());
            }
        });
    }

    public void glDeleteBuffers(final int count, final ID[] buffers, final int n) {
        add(new Command() {
            public void run(GL2 gl) {
                int[] buf = new int[buffers.length];
                for (int i = 0; i < buf.length; i++)
                    buf[i] = buffers[i].glid();
                gl.glDeleteBuffers(count, buf, n);
            }
        });
    }

    public void glDeleteFramebuffers(final int count, final ID[] buffers, final int n) {
        add(new Command() {
            public void run(GL2 gl) {
                int[] buf = new int[buffers.length];
                for (int i = 0; i < buf.length; i++)
                    buf[i] = buffers[i].glid();
                gl.glDeleteFramebuffers(count, buf, n);
            }
        });
    }

    public void glDeleteLists(final ID list, final int range) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glDeleteLists(list.glid(), range);
            }
        });
    }

    public void glDeleteObjectARB(final ID id) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glDeleteObjectARB(id.glid());
            }
        });
    }

    public void glDeleteRenderbuffers(final int count, final ID[] buffers, final int n) {
        add(new Command() {
            public void run(GL2 gl) {
                int[] buf = new int[buffers.length];
                for (int i = 0; i < buf.length; i++)
                    buf[i] = buffers[i].glid();
                gl.glDeleteRenderbuffers(count, buf, n);
            }
        });
    }

    public void glDeleteTextures(final int count, final ID[] buffers, final int n) {
        add(new Command() {
            public void run(GL2 gl) {
                int[] buf = new int[buffers.length];
                for (int i = 0; i < buf.length; i++)
                    buf[i] = buffers[i].glid();
                gl.glDeleteTextures(count, buf, n);
            }
        });
    }

    public void glDeleteVertexArrays(final int count, final ID[] buffers, final int n) {
        add(new Command() {
            public void run(GL2 gl) {
                int[] buf = new int[buffers.length];
                for (int i = 0; i < buf.length; i++)
                    buf[i] = buffers[i].glid();
                gl.glDeleteVertexArrays(count, buf, n);
            }
        });
    }

    public void glDepthFunc(final int func) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glDepthFunc(func);
            }
        });
    }

    public void glDepthMask(final boolean mask) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glDepthMask(mask);
            }
        });
    }

    public void glDisable(final int cap) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glDisable(cap);
            }
        });
    }

    public void glDisableClientState(final int cap) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glDisableClientState(cap);
            }
        });
    }

    public void glDisableVertexAttribArray(final ID location) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glDisableVertexAttribArray(location.glid());
            }
        });
    }

    public void glDisableVertexAttribArray(final ID location, final int offset) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glDisableVertexAttribArray(location.glid() + offset);
            }
        });
    }

    public void glDrawBuffer(final int buf) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glDrawBuffer(buf);
            }
        });
    }

    public void glDrawBuffers(final int n, final int[] bufs, final int i) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glDrawBuffers(n, bufs, i);
            }
        });
    }

    public void glDrawArrays(final int mode, final int first, final int count) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glDrawArrays(mode, first, count);
            }
        });
    }

    public void glDrawElementsInstanced(final int mode, final int count, final int type, final long indices, final int primcount) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glDrawElementsInstanced(mode, count, type, indices, primcount);
            }
        });
    }

    public void glDrawElements(final int mode, final int count, final int type, Buffer indices) {
        final BufState is = new BufState(indices);
        add(new Command() {
            public void run(GL2 gl) {
                is.restore();
                gl.glDrawElements(mode, count, type, is.buf);
            }
        });
    }

    public void glDrawRangeElements(final int mode, final int start, final int end, final int count, final int type, Buffer indices) {
        final BufState is = new BufState(indices);
        add(new Command() {
            public void run(GL2 gl) {
                is.restore();
                gl.glDrawRangeElements(mode, start, end, count, type, is.buf);
            }
        });
    }

    public void glDrawRangeElements(final int mode, final int start, final int end, final int count, final int type, final long indices) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glDrawRangeElements(mode, start, end, count, type, indices);
            }
        });
    }

    public void glEnable(final int cap) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glEnable(cap);
            }
        });
    }

    public void glEnableClientState(final int cap) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glEnableClientState(cap);
            }
        });
    }

    public void glEnableVertexAttribArray(final ID location) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glEnableVertexAttribArray(location.glid());
            }
        });
    }

    public void glEnableVertexAttribArray(final ID location, final int offset) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glEnableVertexAttribArray(location.glid() + offset);
            }
        });
    }

    public void glEnd() {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glEnd();
            }
        });
    }

    public void glEndList() {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glEndList();
            }
        });
    }

    public void glHint(final int target, final int mode) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glHint(target, mode);
            }
        });
    }

    public void glFogi(final int pname, final int param) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glFogi(pname, param);
            }
        });
    }

    public void glFogf(final int pname, final float param) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glFogf(pname, param);
            }
        });
    }

    public void glFogfv(final int pname, final float[] param, final int n) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glFogfv(pname, param, n);
            }
        });
    }

    public void glFramebufferTexture2D(final int target, final int attachment, final int textarget, final ID texture, final int level) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glFramebufferTexture2D(target, attachment, textarget, texture.glid(), level);
            }
        });
    }

    public void glFramebufferRenderbuffer(final int target, final int attachment, final int rbtarget, final ID renderbuffer) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glFramebufferRenderbuffer(target, attachment, rbtarget, renderbuffer.glid());
            }
        });
    }

    public void glLightf(final int light, final int pname, final float param) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glLightf(light, pname, param);
            }
        });
    }

    public void glLightfv(final int light, final int pname, final float[] param, final int n) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glLightfv(light, pname, param, n);
            }
        });
    }

    public void glLightModelfv(final int pname, final float[] param, final int n) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glLightModelfv(pname, param, n);
            }
        });
    }

    public void glLightModeli(final int pname, final int param) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glLightModeli(pname, param);
            }
        });
    }

    public void glLineWidth(final float w) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glLineWidth(w);
            }
        });
    }

    public void glLineStipple(final int factor, final short pattern) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glLineStipple(factor, pattern);
            }
        });
    }

    public void glLinkProgram(final ID program) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glLinkProgram(program.glid());
            }
        });
    }

    public void glLoadMatrixf(final float[] m, final int i) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glLoadMatrixf(m, i);
            }
        });
    }

    public void glMaterialf(final int face, final int pname, final float param) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glMaterialf(face, pname, param);
            }
        });
    }

    public void glMaterialfv(final int face, final int pname, final float[] param, final int n) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glMaterialfv(face, pname, param, n);
            }
        });
    }

    public void glMatrixMode(final int mode) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glMatrixMode(mode);
            }
        });
    }

    public void glNewList(final ID list, final int mode) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glNewList(list.glid(), mode);
            }
        });
    }

    public void glNormal3f(final float x, final float y, final float z) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glNormal3f(x, y, z);
            }
        });
    }

    public void glNormalPointer(final int type, final int stride, final long pointer) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glNormalPointer(type, stride, pointer);
            }
        });
    }

    public void glNormalPointer(final int type, final int stride, Buffer data) {
        final BufState ds = new BufState(data);
        add(new Command() {
            public void run(GL2 gl) {
                ds.restore();
                gl.glNormalPointer(type, stride, ds.buf);
            }
        });
    }

    public void glNormalPointer(final int type, final int stride, Buffer data, int position) {
        final BufState ds = new BufState(data, position);
        add(new Command() {
            public void run(GL2 gl) {
                ds.restore();
                gl.glNormalPointer(type, stride, ds.buf);
            }
        });
    }

    public void glPixelStorei(final int pname, final int param) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glPixelStorei(pname, param);
            }
        });
    }

    public void glPointSize(final float size) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glPointSize(size);
            }
        });
    }

    public void glPolygonMode(final int face, final int mode) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glPolygonMode(face, mode);
            }
        });
    }

    public void glPolygonOffset(final float factor, final float units) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glPolygonOffset(factor, units);
            }
        });
    }

    public void glPopAttrib() {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glPopAttrib();
            }
        });
    }

    public void glPushAttrib(final int mask) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glPushAttrib(mask);
            }
        });
    }

    public void glReadBuffer(final int buf) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glReadBuffer(buf);
            }
        });
    }

    public void glRenderbufferStorage(final int target, final int format, final int width, final int height) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glRenderbufferStorage(target, format, width, height);
            }
        });
    }

    public void glRenderbufferStorageMultisample(final int target, final int samples, final int format, final int width, final int height) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glRenderbufferStorageMultisample(target, samples, format, width, height);
            }
        });
    }

    public void glSampleCoverage(final float value, final boolean invert) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glSampleCoverage(value, invert);
            }
        });
    }

    public void glScissor(final int x, final int y, final int w, final int h) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glScissor(x, y, w, h);
            }
        });
    }

    public void glShaderSourceARB(final ID shader, final int count, final String[] string, final int[] length, final int n) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glShaderSourceARB(shader.glid(), count, string, length, n);
            }
        });
    }

    public void glTexCoord2f(final float s, final float t) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glTexCoord2f(s, t);
            }
        });
    }

    public void glTexCoordPointer(final int size, final int type, final int stride, final long pointer) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glTexCoordPointer(size, type, stride, pointer);
            }
        });
    }

    public void glTexCoordPointer(final int size, final int type, final int stride, Buffer data) {
        final BufState ds = new BufState(data);
        add(new Command() {
            public void run(GL2 gl) {
                ds.restore();
                gl.glTexCoordPointer(size, type, stride, ds.buf);
            }
        });
    }

    public void glTexCoordPointer(final int size, final int type, final int stride, Buffer data, int position) {
        final BufState ds = new BufState(data, position);
        add(new Command() {
            public void run(GL2 gl) {
                ds.restore();
                gl.glTexCoordPointer(size, type, stride, ds.buf);
            }
        });
    }

    public void glTexImage2D(final int target, final int level, final int internalformat, final int width, final int height, final int border, final int format, final int type, Buffer data) {
        final BufState ds = new BufState(data);
        add(new Command() {
            public void run(GL2 gl) {
                ds.restore();
                gl.glTexImage2D(target, level, internalformat, width, height, border, format, type, ds.buf);
            }
        });
    }

    public void glTexSubImage2D(final int target, final int level, final int xoff, final int yoff, final int width, final int height, final int format, final int type, Buffer data) {
        final BufState ds = new BufState(data);
        add(new Command() {
            public void run(GL2 gl) {
                ds.restore();
                gl.glTexSubImage2D(target, level, xoff, yoff, width, height, format, type, ds.buf);
            }
        });
    }

    public void glTexImage2DMultisample(final int target, final int samples, final int internalformat, final int width, final int height, final boolean fixedsamplelocations) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.getGL3bc().glTexImage2DMultisample(target, samples, internalformat, width, height, fixedsamplelocations);
            }
        });
    }

    public void glTexParameterf(final int target, final int pname, final float param) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glTexParameterf(target, pname, param);
            }
        });
    }

    public void glTexParameteri(final int target, final int pname, final int param) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glTexParameteri(target, pname, param);
            }
        });
    }

    public void glUniform1f(final ID location, final float v0) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glUniform1f(location.glid(), v0);
            }
        });
    }

    public void glUniform2f(final ID location, final float v0, final float v1) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glUniform2f(location.glid(), v0, v1);
            }
        });
    }

    public void glUniform3f(final ID location, final float v0, final float v1, final float v2) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glUniform3f(location.glid(), v0, v1, v2);
            }
        });
    }

    public void glUniform4f(final ID location, final float v0, final float v1, final float v2, final float v3) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glUniform4f(location.glid(), v0, v1, v2, v3);
            }
        });
    }

    public void glUniform4fv(final ID location, final int count, final float[] val, final int n) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glUniform4fv(location.glid(), count, val, n);
            }
        });
    }

    public void glUniform1i(final ID location, final int v0) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glUniform1i(location.glid(), v0);
            }
        });
    }

    public void glUniformMatrix3fv(final ID location, final int count, final boolean transpose, final float[] value, final int n) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glUniformMatrix3fv(location.glid(), count, transpose, value, n);
            }
        });
    }

    public void glUniformMatrix4fv(final ID location, final int count, final boolean transpose, final float[] value, final int n) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glUniformMatrix4fv(location.glid(), count, transpose, value, n);
            }
        });
    }

    public void glUseProgramObjectARB(final ID program) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glUseProgramObjectARB(program.glid());
            }
        });
    }

    public void glVertex2f(final float x, final float y) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glVertex2f(x, y);
            }
        });
    }

    public void glVertex2i(final int x, final int y) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glVertex2i(x, y);
            }
        });
    }

    public void glVertex3f(final float x, final float y, final float z) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glVertex3f(x, y, z);
            }
        });
    }

    public void glVertex3i(final int x, final int y, final int z) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glVertex3i(x, y, z);
            }
        });
    }

    public void glVertexAttribDivisor(final ID location, final int divisor) {
        add(new Command() {
            public void run(GL2 gl) {
                ((GL3) gl).glVertexAttribDivisor(location.glid(), divisor);
            }
        });
    }

    public void glVertexAttribDivisor(final ID location, final int offset, final int divisor) {
        add(new Command() {
            public void run(GL2 gl) {
                ((GL3) gl).glVertexAttribDivisor(location.glid() + offset, divisor);
            }
        });
    }

    public void glVertexAttribPointer(final ID location, final int size, final int type, final boolean normalized, final int stride, Buffer pointer) {
        final BufState ps = new BufState(pointer);
        add(new Command() {
            public void run(GL2 gl) {
                ps.restore();
                gl.glVertexAttribPointer(location.glid(), size, type, normalized, stride, ps.buf);
            }
        });
    }

    public void glVertexAttribPointer(final ID location, final int size, final int type, final boolean normalized, final int stride, Buffer pointer, int position) {
        final BufState ps = new BufState(pointer, position);
        add(new Command() {
            public void run(GL2 gl) {
                ps.restore();
                gl.glVertexAttribPointer(location.glid(), size, type, normalized, stride, ps.buf);
            }
        });
    }

    public void glVertexAttribPointer(final ID location, final int size, final int type, final boolean normalized, final int stride, final long pointer) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glVertexAttribPointer(location.glid(), size, type, normalized, stride, pointer);
            }
        });
    }

    public void glVertexAttribPointer(final ID location, final int offset, final int size, final int type, final boolean normalized, final int stride, final long pointer) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glVertexAttribPointer(location.glid() + offset, size, type, normalized, stride, pointer);
            }
        });
    }

    public void glVertexPointer(final int size, final int type, final int stride, final long pointer) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glVertexPointer(size, type, stride, pointer);
            }
        });
    }

    public void glVertexPointer(final int size, final int type, final int stride, Buffer data) {
        final BufState ds = new BufState(data);
        add(new Command() {
            public void run(GL2 gl) {
                ds.restore();
                gl.glVertexPointer(size, type, stride, ds.buf);
            }
        });
    }

    public void glVertexPointer(final int size, final int type, final int stride, Buffer data, int position) {
        final BufState ds = new BufState(data, position);
        add(new Command() {
            public void run(GL2 gl) {
                ds.restore();
                gl.glVertexPointer(size, type, stride, ds.buf);
            }
        });
    }

    public void glViewport(final int x, final int y, final int w, final int h) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.glViewport(x, y, w, h);
            }
        });
    }

    public void joglSetSwapInterval(final int swap) {
        add(new Command() {
            public void run(GL2 gl) {
                gl.setSwapInterval(swap);
            }
        });
    }

    @SuppressWarnings("serial")
	public static class Dump implements Serializable {
        public final List<DCmd> list;
        public final DCmd mark;
        private final transient Map<Object, Dummy> dummies = new IdentityHashMap<Object, Dummy>();

        public static class Dummy implements Serializable {
            public final int id;
            public final String clnm;

            public Dummy(int id, Object o) {
                this.id = id;
                this.clnm = o.getClass().getName();
            }

            public String toString() {
                return (String.format("#<dummy %s #%d>", clnm, id));
            }
        }

        public static class InfoDummy extends Dummy {
            public final String info;

            public InfoDummy(int id, Object o, String info) {
                super(id, o);
                this.info = info;
            }

            public String toString() {
                return (String.format("#<dummy %s(%s) #%d>", clnm, info, id));
            }
        }

        public static class PopDummy extends Dummy {
            public final String[] varn;
            public final Object[] vars;

            public PopDummy(int id, Object o, Dump d) {
                super(id, o);
                int n = 0;
                for (Class<?> cl = o.getClass(); cl != null; cl = cl.getSuperclass())
                    n += cl.getDeclaredFields().length;
                String[] varn = new String[n];
                Object[] vars = new Object[n];
                int i = 0;
                for (Class<?> cl = o.getClass(); cl != null; cl = cl.getSuperclass()) {
                    for (Field f : cl.getDeclaredFields()) {
                        try {
                            f.setAccessible(true);
                        } catch (SecurityException e) {
                        }
                        varn[i] = f.getName();
                        try {
                            vars[i] = d.mapval(f.get(o));
                        } catch (IllegalAccessException e) {
                        }
                        i++;
                    }
                }
                this.varn = varn;
                this.vars = vars;
            }
        }

        private Dummy intern(Object o) {
            if (o == null)
                return (null);
            Dummy ret = dummies.get(o);
            if (ret == null) {
                int id = dummies.size();
                if (o instanceof BufState) {
                    ret = new PopDummy(id, o, this);
                } else if (o instanceof Buffer) {
                    Buffer bo = (Buffer) o;
                    ret = new InfoDummy(id, bo, String.format("p=%d, l=%d, c=%d", bo.position(), bo.limit(), bo.capacity()));
                } else {
                    ret = new Dummy(id, o);
                }
                dummies.put(o, ret);
            }
            return (ret);
        }

        private Object mapval(Object o) {
            if (o == null)
                return (null);
            Class<?> ft = o.getClass();
            if ((o instanceof Number) || (o instanceof Boolean) || (o instanceof String)) {
                return (o);
            } else if (ft.isArray()) {
                int len = Array.getLength(o);
                Object[] na = new Object[len];
                for (int i = 0; i < len; i++)
                    na[i] = mapval(Array.get(o, i));
                return (na);
            } else {
                return (intern(o));
            }
        }

        public static class DCmd implements Serializable {
            public final String clnm, mnm;
            public final String[] argn;
            public final Object[] args;

            public DCmd(Dump d, Object o) {
                this.clnm = o.getClass().getName();
                if (o.getClass().getEnclosingMethod() != null) {
                    mnm = o.getClass().getEnclosingMethod().getName();
                } else {
                    mnm = null;
                }
                Field[] fl = o.getClass().getDeclaredFields();
                argn = new String[fl.length];
                args = new Object[fl.length];
                for (int i = 0; i < fl.length; i++) {
                    Field f = fl[i];
                    try {
                        f.setAccessible(true);
                    } catch (SecurityException e) {
                    }
                    argn[i] = f.getName();
                    try {
                        args[i] = d.mapval(f.get(o));
                    } catch (IllegalAccessException e) {
                    }
                }
            }

            public String toString() {
                StringBuilder buf = new StringBuilder();
                buf.append("#<cmd ");
                buf.append((mnm != null) ? mnm : clnm);
                buf.append("(");
                for (int i = 0; i < argn.length; i++) {
                    if (i > 0)
                        buf.append(", ");
                    buf.append(argn[i]);
                    buf.append("=");
                    if (args[i] instanceof Object[])
                        buf.append(Arrays.asList((Object[]) args[i]).toString());
                    else
                        buf.append(args[i]);
                }
                buf.append(")>");
                return (buf.toString());
            }
        }

        public Dump(BGL buf, Command mark) {
            this.list = new ArrayList<DCmd>();
            DCmd marked = null;
            for (Command ocmd : buf.dump()) {
                DCmd cmd = new DCmd(this, ocmd);
                list.add(cmd);
                if (ocmd == mark)
                    marked = cmd;
            }
            this.mark = marked;
        }

        public void dump(PrintStream out) {
            for (DCmd cmd : list) {
                if (cmd == mark)
                    out.print("==> ");
                out.println(cmd);
            }
        }

        public void dump() {
            dump(System.out);
        }
    }
}
