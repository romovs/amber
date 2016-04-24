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

import java.awt.Graphics;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

public class TexI extends TexGL {
    public static ComponentColorModel glcm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{8, 8, 8, 8}, true, false, ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);
    public BufferedImage back;
    public boolean mutable;
    private int fmt = GL.GL_RGBA;
    public Mipmapper mmalg = Mipmapper.avg;

    public TexI(BufferedImage img) {
        super(Utils.imgsz(img));
        back = img;
        mutable = false;
    }

    public TexI(Coord sz) {
        super(sz);
        mutable = true;
    }

    /* Java's image model is a little bit complex, so these may not be
     * entirely correct. They should be corrected if oddities are
     * detected. */
    public static int detectfmt(BufferedImage img) {
        ColorModel cm = img.getColorModel();
        if (!(img.getSampleModel() instanceof PixelInterleavedSampleModel))
            return (-1);
        PixelInterleavedSampleModel sm = (PixelInterleavedSampleModel) img.getSampleModel();
        int[] cs = cm.getComponentSize();
        int[] off = sm.getBandOffsets();
    /*
    System.err.print(this + ": " + cm.getNumComponents() + ", (");
	for(int i = 0; i < off.length; i++)
	    System.err.print(((i > 0)?" ":"") + off[i]);
	System.err.print("), (");
	for(int i = 0; i < off.length; i++)
	    System.err.print(((i > 0)?" ":"") + cs[i]);
	System.err.print(")");
	System.err.println();
	*/
        if ((cm.getNumComponents() == 4) && (off.length == 4)) {
            if (((cs[0] == 8) && (cs[1] == 8) && (cs[2] == 8) && (cs[3] == 8)) &&
                    (cm.getTransferType() == DataBuffer.TYPE_BYTE) &&
                    (cm.getTransparency() == java.awt.Transparency.TRANSLUCENT)) {
                if ((off[0] == 0) && (off[1] == 1) && (off[2] == 2) && (off[3] == 3))
                    return (GL.GL_RGBA);
                if ((off[0] == 2) && (off[1] == 1) && (off[2] == 0) && (off[3] == 3))
                    return (GL.GL_BGRA);
            }
        } else if ((cm.getNumComponents() == 3) && (off.length == 3)) {
            if (((cs[0] == 8) && (cs[1] == 8) && (cs[2] == 8)) &&
                    (cm.getTransferType() == DataBuffer.TYPE_BYTE) &&
                    (cm.getTransparency() == java.awt.Transparency.OPAQUE)) {
                if ((off[0] == 0) && (off[1] == 1) && (off[2] == 2))
                    return (GL.GL_RGB);
                if ((off[0] == 2) && (off[1] == 1) && (off[2] == 0))
                    return (GL2.GL_BGR);
            }
        }
        return (-1);
    }

    protected void fill(GOut g) {
        BGL gl = g.gl;
        Coord sz = Utils.imgsz(back);
        int ifmt = detectfmt(back);
        if ((ifmt == GL.GL_RGBA) || (ifmt == GL.GL_BGRA)) {
            byte[] pixels = ((DataBufferByte) back.getRaster().getDataBuffer()).getData();
            if (mutable)
                pixels = Utils.splice(pixels, 0);
            if (sz.equals(tdim)) {
                gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, fmt, tdim.x, tdim.y, 0, ifmt, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(pixels));
                if (mipmap)
                    genmipmap(gl, 1, tdim, pixels, ifmt);
            } else {
                gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, fmt, tdim.x, tdim.y, 0, ifmt, GL.GL_UNSIGNED_BYTE, null);
                gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, sz.x, sz.y, ifmt, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(pixels));
            }
        } else if ((ifmt == GL.GL_RGB) || (ifmt == GL2.GL_BGR)) {
            gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
            byte[] pixels = ((DataBufferByte) back.getRaster().getDataBuffer()).getData();
            if (mutable)
                pixels = Utils.splice(pixels, 0);
            if (sz.equals(tdim)) {
                gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, fmt, tdim.x, tdim.y, 0, ifmt, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(pixels));
                if (mipmap)
                    genmipmap3(gl, 1, tdim, pixels, ifmt);
            } else {
                gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, fmt, tdim.x, tdim.y, 0, ifmt, GL.GL_UNSIGNED_BYTE, null);
                gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, sz.x, sz.y, ifmt, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(pixels));
            }
        } else {
	    /* System.err.println("Weird: " + this); */
            byte[] pixels = convert(back, tdim);
            gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, fmt, tdim.x, tdim.y, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(pixels));
            if (mipmap)
                genmipmap(gl, 1, tdim, pixels, GL.GL_RGBA);
        }
    }

    private void genmipmap(BGL gl, int lev, Coord dim, byte[] data, int ifmt) {
        Coord ndim = Mipmapper.nextsz(dim);
        byte[] ndata = mmalg.gen4(dim, data, ifmt);
        gl.glTexImage2D(GL.GL_TEXTURE_2D, lev, fmt, ndim.x, ndim.y, 0, ifmt, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(ndata));
        if ((ndim.x > 1) || (ndim.y > 1))
            genmipmap(gl, lev + 1, ndim, ndata, ifmt);
    }

    private void genmipmap3(BGL gl, int lev, Coord dim, byte[] data, int ifmt) {
        if (mmalg instanceof Mipmapper.Mipmapper3) {
            Coord ndim = Mipmapper.nextsz(dim);
            byte[] ndata = ((Mipmapper.Mipmapper3) mmalg).gen3(dim, data, ifmt);
            gl.glTexImage2D(GL.GL_TEXTURE_2D, lev, fmt, ndim.x, ndim.y, 0, ifmt, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(ndata));
            if ((ndim.x > 1) || (ndim.y > 1))
                genmipmap3(gl, lev + 1, ndim, ndata, ifmt);
        } else {
            genmipmap(gl, lev, dim, convert(back, dim), GL.GL_RGBA);
        }
    }

    public int getRGB(Coord c) {
        return (back.getRGB(c.x, c.y));
    }

    public TexI mkmask() {
        TexI n = new TexI(back);
        n.fmt = GL.GL_ALPHA;
        return (n);
    }

    public static BufferedImage mkbuf(Coord sz) {
        WritableRaster buf = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, sz.x, sz.y, 4, null);
        BufferedImage tgt = new BufferedImage(glcm, buf, false, null);
        return (tgt);
    }

    public static byte[] convert(BufferedImage img, Coord tsz, Coord ul, Coord sz) {
        WritableRaster buf = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, tsz.x, tsz.y, 4, null);
        BufferedImage tgt = new BufferedImage(glcm, buf, false, null);
        Graphics g = tgt.createGraphics();
        g.drawImage(img, 0, 0, sz.x, sz.y, ul.x, ul.y, ul.x + sz.x, ul.y + sz.y, null);
        g.dispose();
        return (((DataBufferByte) buf.getDataBuffer()).getData());
    }

    public static byte[] convert(BufferedImage img, Coord tsz) {
        return (convert(img, tsz, Coord.z, Utils.imgsz(img)));
    }
}
