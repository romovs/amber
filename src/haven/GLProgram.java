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

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.media.opengl.GL2;

@SuppressWarnings("serial")
public class GLProgram implements java.io.Serializable {
    public final Collection<GLShader> shaders;
    private transient ProgOb glp;

    public GLProgram(Collection<GLShader> shaders) {
        this.shaders = new ArrayList<GLShader>(shaders);
    }

    public static class ProgramException extends RuntimeException {
        public final GLProgram program;

        public ProgramException(String msg, GLProgram program) {
            super(msg);
            this.program = program;
        }
    }

    public static class UnknownExternException extends ProgramException {
        public final String type, symbol;

        public UnknownExternException(String msg, GLProgram program, String type, String symbol) {
            super(msg, program);
            this.type = type;
            this.symbol = symbol;
        }
    }

    public static class LinkException extends ProgramException {
        public final String info;

        public LinkException(String msg, GLProgram program, String info) {
            super(msg, program);
            this.info = info;
        }

        public String toString() {
            if (info == null)
                return (super.toString());
            else
                return (super.toString() + "\nLog:\n" + info);
        }
    }

    public static class ProgOb extends GLObject implements BGL.ID {
        private int id;

        public ProgOb(GOut g) {
            super(g);
            g.gl.bglCreate(this);
        }

        public void create(GL2 gl) {
            id = gl.glCreateProgramObjectARB();
        }

        public void delete(BGL gl) {
            gl.glDeleteObjectARB(this);
        }

        public int glid() {
            return (id);
        }

        public void link(GOut g, final GLProgram prog) {
            BGL gl = g.gl;
            for (GLShader sh : prog.shaders)
                gl.glAttachShader(this, sh.glid(g));
            gl.glLinkProgram(this);
            gl.bglSubmit(new BGL.Request() {
                public void run(GL2 rgl) {
                    int[] buf = {0};
                    rgl.glGetObjectParameterivARB(id, GL2.GL_OBJECT_LINK_STATUS_ARB, buf, 0);
                    if (buf[0] != 1) {
                        String info = null;
                        rgl.glGetObjectParameterivARB(id, GL2.GL_OBJECT_INFO_LOG_LENGTH_ARB, buf, 0);
                        if (buf[0] > 0) {
                            byte[] logbuf = new byte[buf[0]];
                            rgl.glGetInfoLogARB(id, logbuf.length, buf, 0, logbuf, 0);
                /* The "platform's default charset" is probably a reasonable choice. */
                            info = new String(logbuf, 0, buf[0]);
                        }
                        throw (new LinkException("Failed to link GL program", prog, info));
                    }
                }
            });
        }

        public int uniform(GL2 gl, String name) {
            int r = gl.glGetUniformLocationARB(id, name);
            if (r < 0)
                throw (new NoSuchElementException(name));
            return (r);
        }

        public int attrib(GL2 gl, String name) {
            int r = gl.glGetAttribLocation(id, name);
            if (r < 0)
                throw (new NoSuchElementException(name));
            return (r);
        }
    }

    protected void link(GOut g) {
        glp = new ProgOb(g);
        glp.link(g, this);
    }

    public ProgOb glob(GOut g) {
        synchronized (this) {
            if ((glp != null) && (glp.cur != g.curgl))
                dispose();
            if (glp == null)
                link(g);
            return (glp);
        }
    }

    public void apply(GOut g) {
        g.gl.glUseProgramObjectARB(glob(g));
    }

    public abstract static class VarID implements BGL.ID, BGL.Request {
        public final String name;
        protected int id;

        private VarID(String name) {
            this.name = name;
        }

        public int glid() {
            return (id);
        }
    }

    public void dispose() {
        synchronized (this) {
            if (glp != null) {
                ProgOb cur = glp;
                glp = null;
                cur.dispose();
            }
            umap.clear();
            amap.clear();
        }
    }

    private final Map<String, VarID> umap = new IdentityHashMap<String, VarID>();

    public VarID uniform(GOut g, String name) {
        VarID r = umap.get(name);
        if (r == null) {
            final ProgOb glob = glob(g);
            r = new VarID(name) {
                public void run(GL2 gl) {
                    if ((this.id = glob.uniform(gl, name)) < 0)
                        throw (new UnknownExternException("Unknown uniform name: " + name, GLProgram.this, "uniform", name));
                }
            };
            g.gl.bglSubmit(r);
            umap.put(name, r);
        }
        return (r);
    }

    private final Map<String, VarID> amap = new IdentityHashMap<String, VarID>();

    public VarID attrib(GOut g, String name) {
        VarID r = amap.get(name);
        if (r == null) {
            final ProgOb glob = glob(g);
            r = new VarID(name) {
                public void run(GL2 gl) {
                    if ((this.id = glob.attrib(gl, name)) < 0)
                        throw (new UnknownExternException("Unknown attribute name: " + name, GLProgram.this, "attrib", name));
                }
            };
            g.gl.bglSubmit(r);
            amap.put(name, r);
        }
        return (r);
    }
}
