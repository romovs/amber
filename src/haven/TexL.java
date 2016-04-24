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

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import haven.Defer.Future;

public abstract class TexL extends TexGL {
    protected Mipmapper mipmap = null;
    private Future<Prepared> decode = null;

    public abstract BufferedImage fill();

    public TexL(Coord sz) {
        super(sz);
        if ((sz.x != nextp2(sz.x)) || (sz.y != nextp2(sz.y)))
            throw (new RuntimeException("TexL does not support non-power-of-two textures"));
    }

    public void mipmap(Mipmapper mipmap) {
        this.mipmap = mipmap;
        dispose();
    }

    private class Prepared {
        BufferedImage img;
        byte[][] data;
        int ifmt;

        private Prepared() {
            img = fill();
            if (!Utils.imgsz(img).equals(dim))
                throw (new RuntimeException("Generated TexL image from " + TexL.this + " does not match declared size"));
            ifmt = TexI.detectfmt(img);
            LinkedList<byte[]> data = new LinkedList<byte[]>();
            if ((ifmt == GL.GL_RGB) || (ifmt == GL2.GL_BGR)) {
                if ((mipmap != null) && !(mipmap instanceof Mipmapper.Mipmapper3))
                    ifmt = -1;
            }
            if ((ifmt == GL.GL_RGB) || (ifmt == GL2.GL_BGR)) {
                byte[] pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
                data.add(pixels);
                if (mipmap != null) {
                    Coord msz = dim;
                    Mipmapper.Mipmapper3 alg = (Mipmapper.Mipmapper3) mipmap;
                    while ((msz.x > 1) || (msz.y > 1)) {
                        pixels = alg.gen3(msz, pixels, ifmt);
                        data.add(pixels);
                        msz = Mipmapper.nextsz(msz);
                    }
                }
            } else {
                byte[] pixels;
                if ((ifmt == GL.GL_RGBA) || (ifmt == GL.GL_BGRA)) {
                    pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
                } else {
                    pixels = TexI.convert(img, dim);
                    ifmt = GL.GL_RGBA;
                }
                data.add(pixels);
                if (mipmap != null) {
                    Coord msz = dim;
                    while ((msz.x > 1) || (msz.y > 1)) {
                        pixels = mipmap.gen4(msz, pixels, ifmt);
                        data.add(pixels);
                        msz = Mipmapper.nextsz(msz);
                    }
                }
            }
            this.data = data.toArray(new byte[0][]);
        }
    }

    private Future<Prepared> prepare() {
        return (Defer.later(new Defer.Callable<Prepared>() {
            public Prepared call() {
                Prepared ret = new Prepared();
                return (ret);
            }

            public String toString() {
                String nm = loadname();
                if (nm != null)
                    return ("Finalizing " + nm + "...");
                else
                    return ("Finalizing texture...");
            }
        }));
    }

    public String loadname() {
        return (null);
    }

    protected void fill(GOut g) {
        if (decode == null)
            decode = prepare();
        Prepared prep = decode.get();
        decode = null;
        BGL gl = g.gl;
        gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
        Coord cdim = tdim;
        for (int i = 0; i < prep.data.length; i++) {
            gl.glTexImage2D(GL.GL_TEXTURE_2D, i, GL.GL_RGBA, cdim.x, cdim.y, 0, prep.ifmt, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(prep.data[i]));
            cdim = Mipmapper.nextsz(cdim);
        }
    }
}
