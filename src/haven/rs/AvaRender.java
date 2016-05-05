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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import haven.Callback;
import haven.Camera;
import haven.Composited;
import haven.Composited.ED;
import haven.Composited.MD;
import haven.Coord;
import haven.Coord3f;
import haven.DirLight;
import haven.Drawn;
import haven.GLState;
import haven.GOut;
import haven.Indir;
import haven.Light;
import haven.Loading;
import haven.LocationCam;
import haven.Message;
import haven.MessageBuf;
import haven.PUtils;
import haven.PView;
import haven.Projection;
import haven.RenderList;
import haven.Rendered;
import haven.ResData;
import haven.Resource;
import haven.Skeleton;

public class AvaRender {
    public static Composited compose(Resource base, List<MD> mod, List<ED> equ) {
        Composited comp = new Composited(base.layer(Skeleton.Res.class).s);
        comp.chmod(mod);
        comp.chequ(equ);
        comp.changes(true);
        return (comp);
    }

    @SuppressWarnings("serial")
	private static class IntException extends RuntimeException {
        private IntException(InterruptedException cause) {
            super(cause);
        }
    }

    public static BufferedImage render(Coord sz, Indir<Resource> base, String camnm, List<MD> mod, List<ED> equ) throws InterruptedException {
        Composited tcomp;
        Camera tcam;
        while (true) {
            try {
                Skeleton.BoneOffset camoff = base.get().layer(Skeleton.BoneOffset.class, camnm);
                tcomp = compose(base.get(), mod, equ);
                GLState.Buffer buf = new GLState.Buffer(null);
                camoff.forpose(tcomp.pose).prep(buf);
                tcam = new LocationCam(buf.get(PView.loc));
                break;
            } catch (Loading ev) {
                ev.waitfor();
            }
        }
        final Composited comp = tcomp; /* ¦] */
        final Camera cam = tcam;
        final GBuffer buf = new GBuffer(sz);
        final BufferedImage[] ret = {null};
        try {
            buf.render(new Drawn() {
                public void draw(GOut g) {
                    float field = 0.5f;
                    float aspect = ((float) buf.sz.y) / ((float) buf.sz.x);
                    Projection proj = Projection.frustum(-field, field, -aspect * field, aspect * field, 1, 5000);

                    Light.Model lmod = new Light.Model();
                    lmod.cc = javax.media.opengl.GL2.GL_SEPARATE_SPECULAR_COLOR;

                    BufView view = new BufView(buf, GLState.compose(proj, cam, lmod, new Light.LightList()));
                    while (true) {
                        try {
                            view.render(new Rendered() {
                                public void draw(GOut g) {
                                }

                                public boolean setup(RenderList rl) {
                                    rl.add(comp, null);
                                    rl.add(new DirLight(Color.WHITE, Color.WHITE, Color.WHITE, new Coord3f(1, 1, 1).norm()), null);
                                    return (false);
                                }
                            }, g);
                            break;
                        } catch (Loading l) {
                            try {
                                l.waitfor();
                            } catch (InterruptedException e) {
                                throw (new IntException(e));
                            }
                        }
                    }
                    g.getimage(new Callback<BufferedImage>() {
                        public void done(BufferedImage res) {
                            ret[0] = res;
                        }
                    });
                }
            });
        } catch (IntException e) {
            throw ((InterruptedException) e.getCause());
        } finally {
            buf.dispose();
        }
        return (ret[0]);
    }

    public static final Server.Command call = new Server.Command() {
        public Object[] run(Server.Client cl, Object... args) throws InterruptedException {
            Coord sz = (Coord) args[0];
            Indir<Resource> base = Resource.local().load((String) args[1]);
            String camnm = (String) args[2];
            Object[] amod = (Object[]) args[3];
            Object[] aequ = (Object[]) args[4];
            List<MD> mod = new LinkedList<MD>();
            for (int i = 0; i < amod.length; i += 2) {
                Indir<Resource> mr = Resource.local().load((String) amod[i]);
                Object[] atex = (Object[]) amod[i + 1];
		    List<ResData> tex = new LinkedList<ResData>();
                for (int o = 0; o < atex.length; o++)
			tex.add(new ResData(Resource.local().load((String)atex[o]), Message.nil));
                mod.add(new MD(mr, tex));
            }
            List<ED> equ = new LinkedList<ED>();
            for (int i = 0; i < aequ.length; ) {
                if (aequ[i] instanceof Object[]) {
                    Object[] cequ = (Object[]) aequ[i];
                    int t = (Integer) cequ[0];
                    String at = (String) cequ[1];
                    Indir<Resource> er = Resource.local().load((String) cequ[2]);
                    byte[] sdt = (byte[]) cequ[3];
                    Coord3f off = new Coord3f((Float) cequ[4], (Float) cequ[5], (Float) cequ[6]);
                    equ.add(new ED(t, at, new ResData(er, new MessageBuf(sdt)), off));
                } else {
                    int t = (Integer) aequ[i++];
                    String at = (String) aequ[i++];
                    Indir<Resource> er = Resource.local().load((String) aequ[i++]);
                    Coord3f off = new Coord3f((Float) aequ[i++], (Float) aequ[i++], (Float) aequ[i++]);
                    equ.add(new ED(t, at, new ResData(er, Message.nil), off));
                }
            }
            BufferedImage ava = render(sz.mul(4), base, camnm, mod, equ);
            ava = PUtils.convolvedown(ava, sz, new PUtils.Lanczos(2));
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            try {
                javax.imageio.ImageIO.write(ava, "PNG", buf);
            } catch (IOException e) {
                throw (new Error(e));
            }
            return (new Object[]{"ok", buf.toByteArray()});
        }
    };

    public static void main(String[] args) throws Exception {
        Indir<Resource> base = Resource.local().load("gfx/borka/body");
	List<MD> mod = Arrays.asList(new MD(Resource.local().load("gfx/borka/male"), ResData.wrap(Arrays.asList(Resource.local().load("gfx/borka/male")))));
        List<ED> equ = new LinkedList<ED>();
        BufferedImage img = render(new Coord(512, 512), base, "avacam", mod, equ);
        img = PUtils.convolvedown(img, new Coord(128, 128), new PUtils.Lanczos(2));
        javax.imageio.ImageIO.write(img, "PNG", new java.io.File("/tmp/bard.png"));
    }
}
