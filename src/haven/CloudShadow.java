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

import static haven.glsl.Cons.add;
import static haven.glsl.Cons.amul;
import static haven.glsl.Cons.ass;
import static haven.glsl.Cons.eq;
import static haven.glsl.Cons.mul;
import static haven.glsl.Cons.pick;
import static haven.glsl.Cons.smoothstep;
import static haven.glsl.Cons.stmt;
import static haven.glsl.Cons.texture2D;
import static haven.glsl.Type.FLOAT;
import static haven.glsl.Type.SAMPLER2D;
import static haven.glsl.Type.VEC2;
import static haven.glsl.Type.VEC4;

import javax.media.opengl.GL;

import haven.GLProgram.VarID;
import haven.glsl.Block;
import haven.glsl.Expression;
import haven.glsl.If;
import haven.glsl.MiscLib;
import haven.glsl.Phong;
import haven.glsl.ProgramContext;
import haven.glsl.ShaderMacro;
import haven.glsl.Uniform;
import haven.glsl.ValBlock;
import haven.glsl.Variable;

public class CloudShadow extends GLState {
    public static final Slot<CloudShadow> slot = new Slot<CloudShadow>(Slot.Type.DRAW, CloudShadow.class, Light.lighting);
    public final TexGL tex;
    public DirLight light;
    public Coord3f vel;
    public float scale;
    public float cmin = 0.5f, cmax = 1.0f, rmin = 0.4f, rmax = 1.0f;

    public CloudShadow(TexGL tex, DirLight light, Coord3f vel, float scale) {
        this.tex = tex;
        this.light = light;
        this.vel = vel;
        this.scale = scale;
    }

    public static final Uniform tsky = new Uniform(SAMPLER2D);
    public static final Uniform cdir = new Uniform(VEC2);
    public static final Uniform cvel = new Uniform(VEC2);
    public static final Uniform cscl = new Uniform(FLOAT);
    public static final Uniform cthr = new Uniform(VEC4);
    private static final ShaderMacro[] shaders = {
            new ShaderMacro() {
                public void modify(ProgramContext prog) {
                    final Phong ph = prog.getmod(Phong.class);
                    if ((ph == null) || !ph.pfrag)
                        return;
                    final ValBlock.Value shval = prog.fctx.uniform.new Value(FLOAT) {
                        public Expression root() {
                            Expression tc = add(mul(add(pick(MiscLib.fragmapv.ref(), "xy"),
                                            mul(pick(MiscLib.fragmapv.ref(), "z"), cdir.ref())),
                                    cscl.ref()), mul(cvel.ref(), MiscLib.globtime.ref()));
                            Expression cl = pick(texture2D(tsky.ref(), tc), "r");
                            Expression th = cthr.ref();
                            return (add(mul(smoothstep(pick(th, "x"), pick(th, "y"), cl), pick(th, "w")), pick(th, "z")));
                        }

                        protected void cons2(Block blk) {
                            tgt = new Variable.Global(FLOAT).ref();
                            blk.add(ass(tgt, init));
                        }
                    };
                    shval.force();
                    ph.dolight.mod(new Runnable() {
                        public void run() {
                            ph.dolight.dcalc.add(new If(eq(MapView.amblight.ref(), ph.dolight.i),
                                            stmt(amul(ph.dolight.dl.tgt, shval.ref()))),
                                    ph.dolight.dcurs);
                        }
                    }, 0);
                }
            }
    };

    public ShaderMacro[] shaders() {
        return (shaders);
    }

    private TexUnit sampler;

    public void reapply(GOut g) {
        VarID u = g.st.prog.cuniform(tsky);
        if (u != null) {
            g.gl.glUniform1i(u, sampler.id);
            float zf = 1.0f / (light.dir[2] + 1.1f);
            float xd = -light.dir[0] * zf, yd = -light.dir[1] * zf;
            g.gl.glUniform2f(g.st.prog.uniform(cdir), xd, yd);
            g.gl.glUniform2f(g.st.prog.uniform(cvel), vel.x, vel.y);
            g.gl.glUniform1f(g.st.prog.uniform(cscl), scale);
            g.gl.glUniform4f(g.st.prog.uniform(cthr), cmin, cmax, rmin, rmax - rmin);
        }
    }

    public void apply(GOut g) {
        sampler = TexGL.lbind(g, tex);
        reapply(g);
    }

    public void unapply(GOut g) {
        sampler.act(g);
        g.gl.glBindTexture(GL.GL_TEXTURE_2D, null);
        sampler.free();
        sampler = null;
    }

    public void prep(Buffer buf) {
        buf.put(slot, this);
    }
}
