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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.media.opengl.GL2;

@SuppressWarnings("serial")
public abstract class GLShader implements java.io.Serializable {
    public final String source, header;
    private transient ShaderOb gls;

    public GLShader(String source, String header) {
        this.source = source;
        this.header = header;
    }

    public static class ShaderOb extends GLObject implements BGL.ID {
        public final int type;
        private int id;

        public ShaderOb(GOut g, int type) {
            super(g);
            this.type = type;
            g.gl.bglCreate(this);
        }

        public void create(GL2 gl) {
            id = gl.glCreateShaderObjectARB(type);
            GOut.checkerr(gl);
        }

        protected void delete(BGL gl) {
            gl.glDeleteObjectARB(this);
        }

        public int glid() {
            return (id);
        }

        public void compile(BGL gl, final GLShader sh) {
        /* Does JOGL use the byte or char length or the supplied
         * String, and in case of the former, how does one know
	     * the coding it encodes the String as so as to supply the
	     * corrent length? It won't matter since all reasonable
	     * programs will be ASCII, of course, but still... */
            gl.glShaderSourceARB(this, 1, new String[]{sh.source}, new int[]{sh.source.length()}, 0);
            gl.glCompileShaderARB(this);
            gl.bglSubmit(new BGL.Request() {
                public void run(GL2 rgl) {
                    int[] buf = {0};
                    rgl.glGetObjectParameterivARB(id, GL2.GL_OBJECT_COMPILE_STATUS_ARB, buf, 0);
                    if (buf[0] != 1) {
                        String info = null;
                        rgl.glGetObjectParameterivARB(id, GL2.GL_OBJECT_INFO_LOG_LENGTH_ARB, buf, 0);
                        if (buf[0] > 0) {
                            byte[] logbuf = new byte[buf[0]];
                            rgl.glGetInfoLogARB(id, logbuf.length, buf, 0, logbuf, 0);
                /* The "platform's default charset" is probably a reasonable choice. */
                            info = new String(logbuf, 0, buf[0]);
                        }
                        throw (new ShaderException("Failed to compile shader", sh, info));
                    }
                }
            });
        }
    }

	public static class ShaderException extends RuntimeException {
        public final GLShader shader;
        public final String info;

        public ShaderException(String msg, GLShader shader, String info) {
            super(msg);
            this.shader = shader;
            this.info = info;
        }

        public String toString() {
            if (info == null)
                return (super.toString());
            else
                return (super.toString() + "\nLog:\n" + info);
        }
    }

    public abstract static class Splitter {
        private final BufferedReader in;
        public final StringBuilder main = new StringBuilder();
        public StringBuilder buf = main;

        public Splitter(Reader r) {
            in = new BufferedReader(r);
        }

        public Splitter(InputStream i) {
            this(new InputStreamReader(i, Utils.ascii));
        }

        public void parse() throws IOException {
            String ln;
            while ((ln = in.readLine()) != null) {
                if (ln.startsWith("#pp ")) {
                    String d = ln.substring(4).trim();
                    String a = "";
                    int p = d.indexOf(' ');
                    if (p >= 0) {
                        a = d.substring(p + 1);
                        d = d.substring(0, p).trim();
                    }
                    d = d.intern();
                    directive(d, a);
                } else {
                    buf.append(ln + "\n");
                }
            }
        }

        public abstract void directive(String directive, String args);
    }

	public static class VertexShader extends GLShader {
        public VertexShader(String source, String header) {
            super(source, header);
        }

        public VertexShader(String source) {
            this(source, "");
        }

        protected ShaderOb create(GOut g) {
            ShaderOb r = new ShaderOb(g, GL2.GL_VERTEX_SHADER);
            r.compile(g.gl, this);
            return (r);
        }

        public static VertexShader parse(Reader in) throws IOException {
            class VSplitter extends Splitter {
                StringBuilder header = new StringBuilder();

                VSplitter(Reader in) {
                    super(in);
                }

                public void directive(String d, String a) {
                    if (d == "header") {
                        buf = header;
                    } else if (d == "main") {
                        buf = main;
                    }
                }
            }
            VSplitter p = new VSplitter(in);
            p.parse();
            return (new VertexShader(p.main.toString(), p.header.toString()));
        }

        public static VertexShader load(Class<?> base, String name) {
            InputStream in = base.getResourceAsStream(name);
            try {
                try {
                    return (parse(new InputStreamReader(in, Utils.ascii)));
                } finally {
                    in.close();
                }
            } catch (IOException e) {
                throw (new RuntimeException(e));
            }
        }
    }

	public static class FragmentShader extends GLShader {
        public FragmentShader(String source, String header) {
            super(source, header);
        }

        public FragmentShader(String source) {
            this(source, "");
        }

        protected ShaderOb create(GOut g) {
            ShaderOb r = new ShaderOb(g, GL2.GL_FRAGMENT_SHADER);
            r.compile(g.gl, this);
            return (r);
        }

        public static FragmentShader parse(Reader in) throws IOException {
            class FSplitter extends Splitter {
                StringBuilder header = new StringBuilder();

                FSplitter(Reader in) {
                    super(in);
                }

                public void directive(String d, String a) {
                    if (d == "header") {
                        buf = header;
                    } else if (d == "main") {
                        buf = main;
                    }
                }
            }
            FSplitter p = new FSplitter(in);
            p.parse();
            return (new FragmentShader(p.main.toString(), p.header.toString()));
        }

        public static FragmentShader load(Class<?> base, String name) {
            InputStream in = base.getResourceAsStream(name);
            try {
                try {
                    return (parse(new InputStreamReader(in, Utils.ascii)));
                } finally {
                    in.close();
                }
            } catch (IOException e) {
                throw (new RuntimeException(e));
            }
        }
    }

    public ShaderOb glid(GOut g) {
        synchronized (this) {
            if ((gls != null) && (gls.cur != g.curgl)) {
                gls.dispose();
                gls = null;
            }
            if (gls == null)
                gls = create(g);
            return (gls);
        }
    }

    protected abstract ShaderOb create(GOut g);
}
