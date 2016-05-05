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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

public class PUtils {
    public static Coord imgsz(BufferedImage img) {
        return (new Coord(img.getWidth(), img.getHeight()));
    }

    public static Coord imgsz(Raster img) {
        return (new Coord(img.getWidth(), img.getHeight()));
    }

    public static WritableRaster byteraster(Coord sz, int bands) {
        return (Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, sz.x, sz.y, bands, null));
    }

    public static WritableRaster alpharaster(Coord sz) {
        return (byteraster(sz, 1));
    }

    public static WritableRaster imgraster(Coord sz) {
        return (byteraster(sz, 4));
    }

    public static WritableRaster copy(Raster src) {
        int w = src.getWidth(), h = src.getHeight(), b = src.getNumBands();
        WritableRaster ret = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, w, h, b, null);
        int[] buf = new int[w * h];
        for (int i = 0; i < b; i++)
            ret.setSamples(0, 0, w, h, i, src.getSamples(0, 0, w, h, i, buf));
        return (ret);
    }

    public static BufferedImage rasterimg(WritableRaster img) {
        return (new BufferedImage(TexI.glcm, img, false, null));
    }

    public static WritableRaster imggrow(WritableRaster img, int rad) {
        int h = img.getHeight(), w = img.getWidth();
        int[] buf = new int[w * h];
        int o = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int m = 0;
                int u = Math.max(0, y - rad), b = Math.min(h - 1, y + rad);
                int l = Math.max(0, x - rad), r = Math.min(w - 1, x + rad);
                for (int y2 = u; y2 <= b; y2++) {
                    for (int x2 = l; x2 <= r; x2++) {
                        m = Math.max(m, img.getSample(x2, y2, 0));
                    }
                }
                buf[o++] = m;
            }
        }
        img.setSamples(0, 0, w, h, 0, buf);
        return (img);
    }

    public static WritableRaster imgblur(WritableRaster img, int rad, double var) {
        int h = img.getHeight(), w = img.getWidth();
        double[] gk = new double[(rad * 2) + 1];
        for (int i = 0; i <= rad; i++)
            gk[rad + i] = gk[rad - i] = Math.exp(-0.5 * Math.pow(i / var, 2.0));
        double s = 0;
        for (double cw : gk) s += cw;
        s = 1.0 / s;
        for (int i = 0; i <= rad * 2; i++)
            gk[i] *= s;
        int[] buf = new int[w * h];
        for (int band = 0; band < img.getNumBands(); band++) {
            int o;
            o = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    double v = 0;
                    int l = Math.max(0, x - rad), r = Math.min(w - 1, x + rad);
                    for (int x2 = l, ks = l - (x - rad); x2 <= r; x2++, ks++)
                        v += img.getSample(x2, y, band) * gk[ks];
                    buf[o++] = (int) v;
                }
            }
            img.setSamples(0, 0, w, h, band, buf);
            o = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    double v = 0;
                    int u = Math.max(0, y - rad), b = Math.min(h - 1, y + rad);
                    for (int y2 = u, ks = u - (y - rad); y2 <= b; y2++, ks++)
                        v += img.getSample(x, y2, band) * gk[ks];
                    buf[o++] = (int) v;
                }
            }
            img.setSamples(0, 0, w, h, band, buf);
        }
        return (img);
    }

    public static WritableRaster alphadraw(WritableRaster dst, Raster alpha, Coord ul, Color col) {
        int r = col.getRed(), g = col.getGreen(), b = col.getBlue(), ba = col.getAlpha();
        int w = alpha.getWidth(), h = alpha.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = (alpha.getSample(x, y, 0) * ba) / 255;
                int dx = x + ul.x, dy = y + ul.y;
                dst.setSample(dx, dy, 0, ((r * a) + (dst.getSample(dx, dy, 0) * (255 - a))) / 255);
                dst.setSample(dx, dy, 1, ((g * a) + (dst.getSample(dx, dy, 1) * (255 - a))) / 255);
                dst.setSample(dx, dy, 2, ((b * a) + (dst.getSample(dx, dy, 2) * (255 - a))) / 255);
                dst.setSample(dx, dy, 3, Math.max((ba * a) / 255, dst.getSample(dx, dy, 3)));
            }
        }
        return (dst);
    }

    public static WritableRaster blit(WritableRaster dst, Raster src, Coord off) {
        int w = src.getWidth(), h = src.getHeight(), b = src.getNumBands();
        for (int y = 0; y < h; y++) {
            int dy = y + off.y;
            for (int x = 0; x < w; x++) {
                int dx = x + off.x;
                for (int i = 0; i < b; i++)
                    dst.setSample(dx, dy, i, src.getSample(x, y, i));
            }
        }
        return (dst);
    }

    public static WritableRaster gayblit(WritableRaster dst, int dband, Coord doff, Raster src, int sband, Coord soff) {
        if (doff.x < 0) {
            soff = soff.add(-doff.x, 0);
            doff = doff.add(-doff.x, 0);
        }
        if (doff.y < 0) {
            soff = soff.add(0, -doff.x);
            doff = doff.add(0, -doff.x);
        }
        int w = Math.min(src.getWidth() - soff.x, dst.getWidth() - doff.x), h = Math.min(src.getHeight() - soff.y, dst.getHeight() - doff.y);
        for (int y = 0; y < h; y++) {
            int sy = y + soff.y, dy = y + doff.y;
            for (int x = 0; x < w; x++) {
                int sx = x + soff.x, dx = x + doff.x;
                dst.setSample(dx, dy, dband, (dst.getSample(dx, dy, dband) * src.getSample(sx, sy, sband)) / 255);
            }
        }
        return (dst);
    }

    public static WritableRaster alphablit(WritableRaster dst, Raster src, Coord off) {
        int w = src.getWidth(), h = src.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = src.getSample(x, y, 3);
                int dx = x + off.x, dy = y + off.y;
                dst.setSample(dx, dy, 0, ((src.getSample(x, y, 0) * a) + (dst.getSample(dx, dy, 0) * (255 - a))) / 255);
                dst.setSample(dx, dy, 1, ((src.getSample(x, y, 1) * a) + (dst.getSample(dx, dy, 1) * (255 - a))) / 255);
                dst.setSample(dx, dy, 2, ((src.getSample(x, y, 2) * a) + (dst.getSample(dx, dy, 2) * (255 - a))) / 255);
                dst.setSample(dx, dy, 3, Math.max(src.getSample(x, y, 3), dst.getSample(dx, dy, 3)));
            }
        }
        return (dst);
    }

    public static WritableRaster blendblit(WritableRaster dst, Raster src, Coord off, int a) {
        int w = src.getWidth(), h = src.getHeight(), nb = dst.getNumBands();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                for (int b = 0; b < nb; b++) {
                    int dx = x + off.x, dy = y + off.y;
                    dst.setSample(dx, dy, b, ((src.getSample(x, y, b) * a) + (dst.getSample(dx, dy, b) * (255 - a))) / 255);
                }
            }
        }
        return (dst);
    }

    public static WritableRaster tilemod(WritableRaster dst, Raster tile, Coord off) {
        int w = dst.getWidth(), h = dst.getHeight(), b = dst.getNumBands();
        int tw = tile.getWidth(), th = tile.getHeight(), tb = tile.getNumBands();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int tx = Utils.floormod(x - off.x, tw), ty = Utils.floormod(y - off.y, th);
                for (int i = 0; i < b; i++)
                    dst.setSample(x, y, i, (dst.getSample(x, y, i) * ((i < tb) ? tile.getSample(tx, ty, i) : 255)) / 255);
            }
        }
        return (dst);
    }

    public static WritableRaster colmul(WritableRaster img, Color col) {
        int w = img.getWidth(), h = img.getHeight();
        int[] bm = {col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha()};
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                for (int b = 0; b < 4; b++)
                    img.setSample(x, y, b, (img.getSample(x, y, b) * bm[b]) / 255);
            }
        }
        return (img);
    }

    public static WritableRaster copyband(WritableRaster dst, int dband, Coord doff, Raster src, int sband, Coord soff, Coord sz) {
        dst.setSamples(doff.x, doff.y, sz.x, sz.y, dband, src.getSamples(soff.x, soff.y, sz.x, sz.y, sband, (int[]) null));
        return (dst);
    }

    public static WritableRaster copyband(WritableRaster dst, int dband, Coord doff, Raster src, int sband) {
        return (copyband(dst, dband, doff, src, sband, Coord.z, imgsz(src)));
    }

    public static WritableRaster copyband(WritableRaster dst, int dband, Raster src, int sband) {
        return (copyband(dst, dband, Coord.z, src, sband));
    }

    public static WritableRaster blurmask(Raster img, int grad, int brad, Color col) {
        Coord marg = new Coord(grad + brad, grad + brad), sz = imgsz(img).add(marg.mul(2));
        return (alphadraw(imgraster(sz), imgblur(imggrow(copyband(alpharaster(sz), 0, marg, img, 3), grad), brad, brad), Coord.z, col));
    }

    public static WritableRaster blurmask2(Raster img, int grad, int brad, Color col) {
        return (alphablit(blurmask(img, grad, brad, col), img, new Coord(grad + brad, grad + brad)));
    }

    public static WritableRaster glowmask(Raster img) {
        Coord sz = imgsz(img);
        int nb = img.getNumBands();
        WritableRaster ret = alpharaster(sz);
        float[] hsv = new float[3];
        float max = 0;
        for (int y = 0; y < sz.y; y++) {
            for (int x = 0; x < sz.x; x++) {
                Color.RGBtoHSB(img.getSample(x, y, 0), img.getSample(x, y, 1), img.getSample(x, y, 2), hsv);
                float a = (nb > 3) ? (img.getSample(x, y, 3) / 255f) : 1f;
                float val = ((1f - hsv[1]) * hsv[2]) * a;
                max = Math.max(max, val);
            }
        }
        float imax = 1f / max;
        for (int y = 0; y < sz.y; y++) {
            for (int x = 0; x < sz.x; x++) {
                Color.RGBtoHSB(img.getSample(x, y, 0), img.getSample(x, y, 1), img.getSample(x, y, 2), hsv);
                float a = (nb > 3) ? (img.getSample(x, y, 3) / 255f) : 1f;
                float val = ((1f - hsv[1]) * hsv[2]) * a;
                ret.setSample(x, y, 0, Math.min(Math.max((int) (Math.sqrt(val * imax) * 255), 0), 255));
            }
        }
        return (ret);
    }

    public static BufferedImage glowmask(Raster img, int grad, Color col) {
        Coord sz = imgsz(img), off = new Coord(grad, grad);
        WritableRaster buf = imgraster(sz.add(off.mul(2)));
        for (int i = 0; i < grad; i++) {
            alphadraw(buf, img, off, col);
            imgblur(buf, 2, 2);
        }
        return (rasterimg(buf));
    }

    public static class BlurFurn extends Text.Imager {
        public final int grad, brad;
        public final Color col;

        public BlurFurn(Text.Furnace bk, int grad, int brad, Color col) {
            super(bk);
            this.grad = grad;
            this.brad = brad;
            this.col = col;
        }

        public BufferedImage proc(Text text) {
            return (rasterimg(blurmask2(text.img.getRaster(), grad, brad, col)));
        }
    }

    public static class TexFurn extends Text.Imager {
        public final BufferedImage tex;

        public TexFurn(Text.Furnace bk, BufferedImage tex) {
            super(bk);
            this.tex = tex;
        }

        public BufferedImage proc(Text text) {
            tilemod(text.img.getRaster(), tex.getRaster(), Coord.z);
            return (text.img);
        }
    }

    public static void dumpband(Raster img, int band) {
        int w = img.getWidth(), h = img.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                System.err.print((char) ('a' + ((img.getSample(x, y, band) * ('z' - 'a')) / 255)));
            }
            System.err.println();
        }
    }

    public static BufferedImage monochromize(BufferedImage img, Color col) {
        Coord sz = Utils.imgsz(img);
        BufferedImage ret = TexI.mkbuf(sz);
        Raster src = img.getRaster();
        WritableRaster dst = ret.getRaster();
        boolean hasalpha = (src.getNumBands() == 4);
        for (int y = 0; y < sz.y; y++) {
            for (int x = 0; x < sz.x; x++) {
                int r = src.getSample(x, y, 0),
                        g = src.getSample(x, y, 1),
                        b = src.getSample(x, y, 2);
                int a = hasalpha ? src.getSample(x, y, 3) : 255;
                int max = Math.max(r, Math.max(g, b)),
                        min = Math.min(r, Math.min(g, b));
                int val = (max + min) / 2;
                dst.setSample(x, y, 0, (col.getRed() * val) / 255);
                dst.setSample(x, y, 1, (col.getGreen() * val) / 255);
                dst.setSample(x, y, 2, (col.getBlue() * val) / 255);
                dst.setSample(x, y, 3, (col.getAlpha() * a) / 255);
            }
        }
        return (ret);
    }

    public static interface Convolution {
        public double cval(double td);

        public double support();
    }

    public static final Convolution box = new Convolution() {
        public double cval(double td) {
            return (((td >= -0.5) && (td < 0.5)) ? 1.0 : 0.0);
        }

        public double support() {
            return (0.5);
        }
    };

    public static class Hanning implements Convolution {
        private final double sz;

        public Hanning(double sz) {
            this.sz = sz;
        }

        public double cval(double td) {
            if (td == 0)
                return (1.0);
            else if ((td < -sz) || (td > sz))
                return (0.0);
            double tdp = td * Math.PI;
            return ((Math.sin(tdp) / tdp) * (0.5 + (0.5 * Math.cos(tdp / sz))));
        }

        public double support() {
            return (sz);
        }
    }

    public static class Hamming implements Convolution {
        private final double sz;

        public Hamming(double sz) {
            this.sz = sz;
        }

        public double cval(double td) {
            if (td == 0)
                return (1.0);
            else if ((td < -sz) || (td > sz))
                return (0.0);
            double tdp = td * Math.PI;
            return ((Math.sin(tdp) / tdp) * (0.54 + (0.46 * Math.cos(tdp / sz))));
        }

        public double support() {
            return (sz);
        }
    }

    public static class Lanczos implements Convolution {
        private final double sz;

        public Lanczos(double sz) {
            this.sz = sz;
        }

        public double cval(double td) {
            if (td == 0)
                return (1.0);
            else if ((td < -sz) || (td > sz))
                return (0.0);
            double tdp = td * Math.PI;
            double wtdp = tdp / sz;
            return ((Math.sin(tdp) / tdp) * (Math.sin(wtdp) / wtdp));
        }

        public double support() {
            return (sz);
        }
    }

    public static BufferedImage convolvedown(BufferedImage img, Coord tsz, Convolution filter) {
        Raster in = img.getRaster();
        int w = in.getWidth(), h = in.getHeight(), nb = in.getNumBands();
        double xf = (double) w / (double) tsz.x, ixf = 1.0 / xf;
        double yf = (double) h / (double) tsz.y, iyf = 1.0 / yf;
        double[] ca = new double[nb];
        WritableRaster buf = byteraster(new Coord(tsz.x, h), nb);
        double support = filter.support();

        {
            double[] cf = new double[tsz.x * (int) Math.ceil(2 * support * xf + 2)];
            int[] cl = new int[tsz.x];
            int[] cr = new int[tsz.x];
            for (int x = 0, ci = 0; x < tsz.x; x++) {
                int si = ci;
                double wa = 0.0;
                cl[x] = Math.max((int) Math.floor((x + 0.5 - support) * xf), 0);
                cr[x] = Math.min((int) Math.ceil((x + 0.5 + support) * xf), w - 1);
                for (int sx = cl[x]; sx <= cr[x]; sx++) {
                    double tx = ((sx + 0.5) * ixf) - x - 0.5;
                    double fw = filter.cval(tx);
                    wa += fw;
                    cf[ci++] = fw;
                }
                wa = 1.0 / wa;
                for (; si < ci; si++)
                    cf[si] *= wa;
            }
            for (int y = 0; y < h; y++) {
                for (int x = 0, ci = 0; x < tsz.x; x++) {
                    for (int b = 0; b < nb; b++)
                        ca[b] = 0.0;
                    for (int sx = cl[x]; sx <= cr[x]; sx++) {
                        double fw = cf[ci++];
                        for (int b = 0; b < nb; b++)
                            ca[b] += in.getSample(sx, y, b) * fw;
                    }
                    for (int b = 0; b < nb; b++)
                        buf.setSample(x, y, b, Utils.clip((int) ca[b], 0, 255));
                }
            }
        }

        WritableRaster res = byteraster(tsz, nb);
        {
            double[] cf = new double[tsz.y * (int) Math.ceil(2 * support * yf + 2)];
            int[] cu = new int[tsz.y];
            int[] cd = new int[tsz.y];
            for (int y = 0, ci = 0; y < tsz.y; y++) {
                int si = ci;
                double wa = 0.0;
                cu[y] = Math.max((int) Math.floor((y + 0.5 - support) * yf), 0);
                cd[y] = Math.min((int) Math.ceil((y + 0.5 + support) * yf), h - 1);
                for (int sy = cu[y]; sy <= cd[y]; sy++) {
                    double ty = ((sy + 0.5) * iyf) - y - 0.5;
                    double fw = filter.cval(ty);
                    wa += fw;
                    cf[ci++] = fw;
                }
                wa = 1.0 / wa;
                for (; si < ci; si++)
                    cf[si] *= wa;
            }
            for (int x = 0; x < tsz.x; x++) {
                for (int y = 0, ci = 0; y < tsz.y; y++) {
                    for (int b = 0; b < nb; b++)
                        ca[b] = 0.0;
                    for (int sy = cu[y]; sy <= cd[y]; sy++) {
                        double fw = cf[ci++];
                        for (int b = 0; b < nb; b++)
                            ca[b] += buf.getSample(x, sy, b) * fw;
                    }
                    for (int b = 0; b < nb; b++)
                        res.setSample(x, y, b, Utils.clip((int) ca[b], 0, 255));
                }
            }
        }
        return (new BufferedImage(img.getColorModel(), res, false, null));
    }

    public static void main(String[] args) throws Exception {
        Convolution[] filters = {
                box,
                new Hanning(1),
                new Hanning(2),
                new Hamming(1),
                new Lanczos(2),
                new Lanczos(3),
        };
        //BufferedImage in = Resource.loadimg("gfx/invobjs/herbs/crowberry");
        BufferedImage in = javax.imageio.ImageIO.read(new java.io.File("/tmp/e.jpg"));
        Coord tsz = new Coord(300, 300);
        for (int i = 0; i < filters.length; i++) {
            long start = System.nanoTime();
            BufferedImage out = convolvedown(in, tsz, filters[i]);
            System.err.println(System.nanoTime() - start);
            javax.imageio.ImageIO.write(out, "PNG", new java.io.File("/tmp/barda" + i + ".png"));
        }
    }
}
