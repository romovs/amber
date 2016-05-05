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

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;

@SuppressWarnings("serial")
public class GLConfig implements java.io.Serializable, Console.Directory {
    private static final Pattern slvp = Pattern.compile("^(\\d+)\\.(\\d+)");
    public int glslver, glmajver, glminver;
    public int maxlights;
    public float anisotropy;
    public Collection<String> exts;
    public transient GLCapabilitiesImmutable caps;
    public GLSettings pref;

    private GLConfig() {
    }

    private static int glgeti(GL gl, int param) {
        int[] buf = {0};
        gl.glGetIntegerv(param, buf, 0);
        GOut.checkerr(gl);
        return (buf[0]);
    }

    private static float glgetf(GL gl, int param) {
        float[] buf = {0};
        gl.glGetFloatv(param, buf, 0);
        GOut.checkerr(gl);
        return (buf[0]);
    }

    public static String glconds(GL gl, int param) {
        GOut.checkerr(gl);
        String ret = gl.glGetString(param);
        if (gl.glGetError() != 0)
            return (null);
        return (ret);
    }

    public static class HardwareException extends RuntimeException {
        public HardwareException(String msg) {
            super(msg);
        }
    }

    private void assertcaps() {
        if (!haveglsl())
            throw (new HardwareException("Graphics context does not support programmable shading."));
    }

    public static GLConfig fromgl(GL gl, GLContext ctx, GLCapabilitiesImmutable caps) {
        GLConfig c = new GLConfig();
        try {
            c.glmajver = glgeti(gl, GL2.GL_MAJOR_VERSION);
            c.glminver = glgeti(gl, GL2.GL_MINOR_VERSION);
        } catch (GOut.GLException e) {
            c.glmajver = 1;
            c.glminver = 0;
        }
        c.maxlights = glgeti(gl, GL2.GL_MAX_LIGHTS);
        c.exts = Arrays.asList(gl.glGetString(GL.GL_EXTENSIONS).split(" "));
        c.caps = caps;
        c.pref = GLSettings.defconf(c);
        String slv = glconds(gl, GL2.GL_SHADING_LANGUAGE_VERSION);
        if (slv != null) {
            Matcher m = slvp.matcher(slv);
            if (m.find()) {
                try {
                    int major = Integer.parseInt(m.group(1));
                    int minor = Integer.parseInt(m.group(2));
                    if ((major > 0) && (major < 256) && (minor >= 0) && (minor < 256))
                        c.glslver = (major << 8) | minor;
                } catch (NumberFormatException e) {
                }
            }
        }
        if (c.exts.contains("GL_EXT_texture_filter_anisotropic"))
            c.anisotropy = glgetf(gl, GL.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
        else
            c.anisotropy = 0;
        c.assertcaps();
        return (c);
    }

    public boolean havefsaa() {
        return (exts.contains("GL_ARB_multisample") && caps.getSampleBuffers());
    }

    public boolean haveglsl() {
        return (exts.contains("GL_ARB_fragment_shader") && exts.contains("GL_ARB_vertex_shader") && (glslver >= 0x0114));
    }

    public boolean havefbo() {
        return (exts.contains("GL_EXT_framebuffer_object"));
    }

    public void resetprefs() {
        pref = GLSettings.defconf(this);
        pref.dirty = true;
    }

    private transient Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();

    {
        cmdmap.put("gl", new Console.Command() {
            public void run(Console cons, String[] args) throws Exception {
                if (args.length >= 3) {
                    String var = args[1].intern();
                    for (GLSettings.Setting<?> s : pref.settings()) {
                        if (s.nm == var) {
                            s.set(args[2]);
                            pref.dirty = true;
                            return;
                        }
                    }
                    throw (new Exception("No such setting: " + var));
                }
            }
        });
        cmdmap.put("glreset", new Console.Command() {
            public void run(Console cons, String[] args) {
                resetprefs();
            }
        });
    }

    public Map<String, Console.Command> findcmds() {
        return (cmdmap);
    }
}
