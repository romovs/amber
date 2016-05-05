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

import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

public class MorphedMesh extends FastMesh {
    private static Map<Morpher.Factory, Collection<MorphedBuf>> bufs = new CacheMap<Morpher.Factory, Collection<MorphedBuf>>(CacheMap.RefType.WEAK);

    private static MorphedBuf buf(VertexBuf buf, Morpher.Factory morph) {
        Collection<MorphedBuf> bl;
        synchronized (bufs) {
            bl = bufs.get(morph);
            if (bl == null)
                bufs.put(morph, bl = new LinkedList<MorphedBuf>());
        }
        synchronized (bl) {
            for (MorphedBuf b : bl) {
                if (b.from == buf)
                    return (b);
            }
            MorphedBuf b = new MorphedBuf(buf, morph);
            bl.add(b);
            return (b);
        }
    }

    public MorphedMesh(FastMesh mesh, Morpher.Factory pose) {
        super(mesh, buf(mesh.vert, pose));
    }

    public boolean setup(RenderList rl) {
        ((MorphedBuf) vert).update();
        return (super.setup(rl));
    }

    public void cdraw(GOut g) {
        ((MorphedBuf) vert).update2(g);
        super.cdraw(g);
    }

    protected boolean compile() {
        return (false);
    }

    public String toString() {
        return ("morphed(" + from + ")");
    }

    public static interface Morpher {
        public static interface Factory {
            public Morpher create(MorphedBuf buf);
        }

        public boolean update();

        public void morphp(FloatBuffer dst, FloatBuffer src);

        public void morphd(FloatBuffer dst, FloatBuffer src);
    }

    public static enum MorphType {
        NONE, DUP, POS, DIR
    }

    public static interface MorphArray {
        public MorphType morphtype();

        public VertexBuf.AttribArray dup();
    }

    public static class MorphedBuf extends VertexBuf {
        public final VertexBuf from;
        private final Morpher morph;
        private final Pair[] parrays, darrays;

        private static class Pair {
            final FloatArray o, n;
            private FloatBuffer upd;

            Pair(FloatArray o, FloatArray n) {
                this.o = o;
                this.n = n;
            }
        }

        private static AttribArray[] ohBitterSweetJavaDays(VertexBuf from, Collection<Pair> pos, Collection<Pair> dir) {
            AttribArray[] ret = new AttribArray[from.bufs.length];
            for (int i = 0; i < from.bufs.length; i++) {
                MorphType type = (from.bufs[i] instanceof MorphArray) ? ((MorphArray) from.bufs[i]).morphtype() : MorphType.NONE;
                if (type != MorphType.NONE) {
                    ret[i] = ((MorphArray) from.bufs[i]).dup();
                    if (type == MorphType.POS) {
                        pos.add(new Pair((FloatArray) from.bufs[i], (FloatArray) ret[i]));
                        ret[i].vbomode(javax.media.opengl.GL.GL_DYNAMIC_DRAW);
                    } else if (type == MorphType.DIR) {
                        dir.add(new Pair((FloatArray) from.bufs[i], (FloatArray) ret[i]));
                        ret[i].vbomode(javax.media.opengl.GL.GL_DYNAMIC_DRAW);
                    }
                } else {
                    ret[i] = from.bufs[i];
                }
            }
            return (ret);
        }

        private MorphedBuf(VertexBuf buf, Morpher.Factory morph, Collection<Pair> pos, Collection<Pair> dir) {
            super(ohBitterSweetJavaDays(buf, pos, dir));
            this.from = buf;
            this.morph = morph.create(this);
            this.parrays = pos.toArray(new Pair[0]);
            this.darrays = dir.toArray(new Pair[0]);
        }

        private MorphedBuf(VertexBuf buf, Morpher.Factory morph) {
            this(buf, morph, new LinkedList<Pair>(), new LinkedList<Pair>());
        }

        public void update() {
            if (!morph.update())
                return;
            for (Pair p : parrays)
                morph.morphp(p.upd = Utils.wfbuf(p.n.data.capacity()), p.o.data);
            for (Pair p : darrays)
                morph.morphd(p.upd = Utils.wfbuf(p.n.data.capacity()), p.o.data);
        }

        public void update2(GOut g) {
            for (Pair p : parrays) {
                if (p.upd != null) {
                    g.gl.bglCopyBufferf(p.n.data, 0, p.upd, 0, p.upd.capacity());
                    p.n.update();
                    p.upd = null;
                }
            }
            for (Pair p : darrays) {
                if (p.upd != null) {
                    g.gl.bglCopyBufferf(p.n.data, 0, p.upd, 0, p.upd.capacity());
                    p.n.update();
                    p.upd = null;
                }
            }
        }
    }

    public static Morpher.Factory combine(final Morpher.Factory... parts) {
        return (new Morpher.Factory() {
            public Morpher create(MorphedBuf vb) {
                final Morpher[] mparts = new Morpher[parts.length];
                for (int i = 0; i < parts.length; i++)
                    mparts[i] = parts[i].create(vb);
                return (new Morpher() {
                    public boolean update() {
                        boolean ret = false;
                        for (Morpher p : mparts) {
                            if (p.update())
                                ret = true;
                        }
                        return (ret);
                    }

                    public void morphp(FloatBuffer dst, FloatBuffer src) {
                        for (Morpher p : mparts) {
                            p.morphp(dst, src);
                            src = dst;
                        }
                    }

                    public void morphd(FloatBuffer dst, FloatBuffer src) {
                        for (Morpher p : mparts) {
                            p.morphd(dst, src);
                            src = dst;
                        }
                    }
                });
            }
        });
    }
}
