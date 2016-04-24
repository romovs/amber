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

package haven.resutil;

import static haven.glsl.Cons.add;
import static haven.glsl.Cons.l;
import static haven.glsl.Cons.mul;
import static haven.glsl.Cons.pick;
import static haven.glsl.Cons.sub;
import static haven.glsl.Cons.texture2D;
import static haven.glsl.Type.SAMPLER2D;
import static haven.glsl.Type.VEC3;

import java.nio.FloatBuffer;
import java.util.Collection;

import javax.media.opengl.GL;

import haven.BGL;
import haven.GLState;
import haven.GOut;
import haven.Indir;
import haven.Material;
import haven.MeshBuf;
import haven.Message;
import haven.MorphedMesh;
import haven.Resource;
import haven.TexGL;
import haven.TexR;
import haven.Utils;
import haven.VertexBuf;
import haven.glsl.Attribute;
import haven.glsl.AutoVarying;
import haven.glsl.Expression;
import haven.glsl.Macro1;
import haven.glsl.MiscLib;
import haven.glsl.ProgramContext;
import haven.glsl.ShaderMacro;
import haven.glsl.Tex2D;
import haven.glsl.Uniform;
import haven.glsl.ValBlock;
import haven.glsl.VertexContext;

public class BumpMap extends GLState {
    public static final Slot<BumpMap> slot = new Slot<BumpMap>(Slot.Type.DRAW, BumpMap.class);
    public static final Attribute tan = new Attribute(VEC3);
    public static final Attribute bit = new Attribute(VEC3);
    private static final Uniform ctex = new Uniform(SAMPLER2D);
    public final TexGL tex;
    private TexUnit sampler;

    public BumpMap(TexGL tex) {
        this.tex = tex;
    }

    private static final ShaderMacro[] shaders = {
            new ShaderMacro() {
                final AutoVarying tanc = new AutoVarying(VEC3) {
                    protected Expression root(VertexContext vctx) {
                        return (vctx.nxf(tan.ref()));
                    }
                };
                final AutoVarying bitc = new AutoVarying(VEC3) {
                    protected Expression root(VertexContext vctx) {
                        return (vctx.nxf(bit.ref()));
                    }
                };

                public void modify(final ProgramContext prog) {
                    final ValBlock.Value nmod = prog.fctx.uniform.new Value(VEC3) {
                        public Expression root() {
                            return (mul(sub(pick(texture2D(ctex.ref(), Tex2D.texcoord(prog.fctx).ref()), "rgb"),
                                    l(0.5)), l(2.0)));
                        }
                    };
                    nmod.force();
                    MiscLib.frageyen(prog.fctx).mod(new Macro1<Expression>() {
                        public Expression expand(Expression in) {
                            Expression m = nmod.ref();
                            return (add(mul(pick(m, "s"), tanc.ref()),
                                    mul(pick(m, "t"), bitc.ref()),
                                    mul(pick(m, "p"), in)));
                        }
                    }, -100);
        /*
        prog.fctx.fragcol.mod(new Macro1<Expression>() {
			public Expression expand(Expression in) {
			    return(mix(in, vec4(nmod.ref(), l(1.0)), l(0.5)));
			}
		    }, 1000);
		*/
                }
            }
    };

    public ShaderMacro[] shaders() {
        return (shaders);
    }

    public void reapply(GOut g) {
        g.gl.glUniform1i(g.st.prog.uniform(ctex), sampler.id);
    }

    public void apply(GOut g) {
        sampler = TexGL.lbind(g, tex);
        reapply(g);
    }

    public void unapply(GOut g) {
        BGL gl = g.gl;
        sampler.act(g);
        gl.glBindTexture(GL.GL_TEXTURE_2D, null);
        sampler.free();
        sampler = null;
    }

    public void prep(Buffer buf) {
        if (buf.cfg.pref.flight.val)
            buf.put(slot, this);
    }

    public static final MeshBuf.LayerID<MeshBuf.Vec3Layer> ltan = new MeshBuf.V3LayerID(tan);
    public static final MeshBuf.LayerID<MeshBuf.Vec3Layer> lbit = new MeshBuf.V3LayerID(bit);

    @Material.ResName("bump")
    public static class $bump implements Material.ResCons2 {
        public Material.Res.Resolver cons(final Resource res, Object... args) {
            final Indir<Resource> tres;
            final int tid;
            int a = 0;
            if (args[a] instanceof String) {
                tres = res.pool.load((String) args[a], (Integer) args[a + 1]);
                tid = (Integer) args[a + 2];
                a += 3;
            } else {
                tres = res.indir();
                tid = (Integer) args[a];
                a += 1;
            }
            return (new Material.Res.Resolver() {
                public void resolve(Collection<GLState> buf) {
                    TexR rt = tres.get().layer(TexR.class, tid);
                    if (rt == null)
                        throw (new RuntimeException(String.format("Specified texture %d for %s not found in %s", tid, res, tres)));
			/* XXX: It is somewhat doubtful that this cast is really quite reasonable. */
                    buf.add(new BumpMap((TexGL) rt.tex()));
                }
            });
        }
    }

    @VertexBuf.ResName("tan")
    public static class Tangents extends VertexBuf.Vec3Array implements MorphedMesh.MorphArray {
        public Tangents(FloatBuffer data) {
            super(data, tan);
        }

        public Tangents(Resource res, Message buf, int nv) {
            this(VertexBuf.loadbuf(Utils.wfbuf(nv * 3), buf));
        }

        public MorphedMesh.MorphType morphtype() {
            return (MorphedMesh.MorphType.DIR);
        }

        public Tangents dup() {
            return (new Tangents(Utils.bufcp(data)));
        }
    }

    @VertexBuf.ResName("bit")
    public static class BiTangents extends VertexBuf.Vec3Array implements MorphedMesh.MorphArray {
        public BiTangents(FloatBuffer data) {
            super(data, bit);
        }

        public BiTangents(Resource res, Message buf, int nv) {
            this(VertexBuf.loadbuf(Utils.wfbuf(nv * 3), buf));
        }

        public MorphedMesh.MorphType morphtype() {
            return (MorphedMesh.MorphType.DIR);
        }

        public BiTangents dup() {
            return (new BiTangents(Utils.bufcp(data)));
        }
    }
}
