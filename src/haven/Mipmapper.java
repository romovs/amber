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
import java.awt.image.Raster;
import java.util.Random;

public abstract class Mipmapper {
    public abstract byte[] gen4(Coord dim, byte[] data, int fmt);

    public static abstract class Mipmapper3 extends Mipmapper {
        public abstract byte[] gen3(Coord dim, byte[] data, int fmt);
    }

    public static Coord nextsz(Coord dim) {
        Coord ndim = dim.div(2);
        ndim.x = Math.max(ndim.x, 1);
        ndim.y = Math.max(ndim.y, 1);
        return (ndim);
    }

    public static final Mipmapper3 avg = new Mipmapper3() {
        public byte[] gen4(Coord dim, byte[] data, int fmt) {
            int dst = dim.x * 4;
            dim = dim.div(2);
            boolean lx = false, ly = false;
            if (dim.x < 1) {
                dim.x = 1;
                lx = true;
            }
            if (dim.y < 1) {
                dim.y = 1;
                ly = true;
            }
            byte[] ndata = new byte[dim.x * dim.y * 4];
            int[] r = new int[4], g = new int[4], b = new int[4], a = new int[4];
            int na = 0, da = 0;
            for (int y = 0; y < dim.y; y++) {
                for (int x = 0; x < dim.x; x++) {
                    r[0] = ((int) data[da + 0]) & 0xff;
                    g[0] = ((int) data[da + 1]) & 0xff;
                    b[0] = ((int) data[da + 2]) & 0xff;
                    a[0] = ((int) data[da + 3]) & 0xff;
                    if (!lx) {
                        r[1] = ((int) data[da + 0 + 4]) & 0xff;
                        g[1] = ((int) data[da + 1 + 4]) & 0xff;
                        b[1] = ((int) data[da + 2 + 4]) & 0xff;
                        a[1] = ((int) data[da + 3 + 4]) & 0xff;
                    } else {
                        r[1] = r[0];
                        g[1] = g[0];
                        b[1] = b[0];
                        a[1] = a[0];
                    }
                    if (!ly) {
                        r[2] = ((int) data[da + 0 + dst]) & 0xff;
                        g[2] = ((int) data[da + 1 + dst]) & 0xff;
                        b[2] = ((int) data[da + 2 + dst]) & 0xff;
                        a[2] = ((int) data[da + 3 + dst]) & 0xff;
                    } else {
                        r[2] = r[0];
                        g[2] = g[0];
                        b[2] = b[0];
                        a[2] = a[0];
                    }
                    if (!lx && !ly) {
                        r[3] = ((int) data[da + 0 + dst + 4]) & 0xff;
                        g[3] = ((int) data[da + 1 + dst + 4]) & 0xff;
                        b[3] = ((int) data[da + 2 + dst + 4]) & 0xff;
                        a[3] = ((int) data[da + 3 + dst + 4]) & 0xff;
                    } else if (!ly) {
                        r[3] = r[2];
                        g[3] = g[2];
                        b[3] = b[2];
                        a[3] = a[2];
                    } else {
                        r[3] = r[1];
                        g[3] = g[1];
                        b[3] = b[1];
                        a[3] = a[1];
                    }
                    int n = 0, cr = 0, cg = 0, cb = 0;
                    for (int i = 0; i < 4; i++) {
                        if (a[i] < 128)
                            continue;
                        cr += r[i];
                        cg += g[i];
                        cb += b[i];
                        n++;
                    }
                    if (n <= 1) {
                        ndata[na + 3] = 0;
                    } else {
                        ndata[na + 0] = (byte) (cr / n);
                        ndata[na + 1] = (byte) (cg / n);
                        ndata[na + 2] = (byte) (cb / n);
                        ndata[na + 3] = (byte) 255;
                    }
                    na += 4;
                    da += lx ? 4 : 8;
                }
                da += ly ? 0 : dst;
            }
            return (ndata);
        }

        public byte[] gen3(Coord dim, byte[] data, int fmt) {
            int dst = dim.x * 3;
            dim = dim.div(2);
            boolean lx = false, ly = false;
            if (dim.x < 1) {
                dim.x = 1;
                lx = true;
            }
            if (dim.y < 1) {
                dim.y = 1;
                ly = true;
            }
            byte[] ndata = new byte[dim.x * dim.y * 3];
            int[] r = new int[4], g = new int[4], b = new int[4];
            int na = 0, da = 0;
            for (int y = 0; y < dim.y; y++) {
                for (int x = 0; x < dim.x; x++) {
                    r[0] = ((int) data[da + 0]) & 0xff;
                    g[0] = ((int) data[da + 1]) & 0xff;
                    b[0] = ((int) data[da + 2]) & 0xff;
                    if (!lx) {
                        r[1] = ((int) data[da + 0 + 3]) & 0xff;
                        g[1] = ((int) data[da + 1 + 3]) & 0xff;
                        b[1] = ((int) data[da + 2 + 3]) & 0xff;
                    } else {
                        r[1] = r[0];
                        g[1] = g[0];
                        b[1] = b[0];
                    }
                    if (!ly) {
                        r[2] = ((int) data[da + 0 + dst]) & 0xff;
                        g[2] = ((int) data[da + 1 + dst]) & 0xff;
                        b[2] = ((int) data[da + 2 + dst]) & 0xff;
                    } else {
                        r[2] = r[0];
                        g[2] = g[0];
                        b[2] = b[0];
                    }
                    if (!lx && !ly) {
                        r[3] = ((int) data[da + 0 + dst + 3]) & 0xff;
                        g[3] = ((int) data[da + 1 + dst + 3]) & 0xff;
                        b[3] = ((int) data[da + 2 + dst + 3]) & 0xff;
                    } else if (!ly) {
                        r[3] = r[2];
                        g[3] = g[2];
                        b[3] = b[2];
                    } else {
                        r[3] = r[1];
                        g[3] = g[1];
                        b[3] = b[1];
                    }
                    int cr = 0, cg = 0, cb = 0;
                    for (int i = 0; i < 4; i++) {
                        cr += r[i];
                        cg += g[i];
                        cb += b[i];
                    }
                    ndata[na + 0] = (byte) (cr / 4);
                    ndata[na + 1] = (byte) (cg / 4);
                    ndata[na + 2] = (byte) (cb / 4);
                    na += 3;
                    da += lx ? 3 : 6;
                }
                da += ly ? 0 : dst;
            }
            return (ndata);
        }
    };

    public static final Mipmapper rnd = new Mipmapper() {
        public byte[] gen4(Coord dim, byte[] data, int fmt) {
            Random rnd = new Random();
            int dst = dim.x * 4;
            dim = dim.div(2);
            boolean lx = false, ly = false;
            if (dim.x < 1) {
                dim.x = 1;
                lx = true;
            }
            if (dim.y < 1) {
                dim.y = 1;
                ly = true;
            }
            byte[] ndata = new byte[dim.x * dim.y * 4];
            int[] o = new int[4];
            o[0] = 0;
            o[1] = lx ? 0 : 4;
            o[2] = ly ? 0 : dst;
            o[3] = lx ? dst : (ly ? 4 : (dst + 4));
            int na = 0, da = 0;
            for (int y = 0; y < dim.y; y++) {
                for (int x = 0; x < dim.x; x++) {
                    int so = da + o[rnd.nextInt(4)];
                    ndata[na + 0] = data[so + 0];
                    ndata[na + 1] = data[so + 1];
                    ndata[na + 2] = data[so + 2];
                    ndata[na + 3] = data[so + 3];
                    na += 4;
                    da += lx ? 4 : 8;
                }
                da += ly ? 0 : dst;
            }
            return (ndata);
        }
    };

    public static final Mipmapper cnt = new Mipmapper() {
        public byte[] gen4(Coord dim, byte[] data, int fmt) {
            int dst = dim.x * 4;
            dim = dim.div(2);
            boolean lx = false, ly = false;
            if (dim.x < 1) {
                dim.x = 1;
                lx = true;
            }
            if (dim.y < 1) {
                dim.y = 1;
                ly = true;
            }
            byte[] ndata = new byte[dim.x * dim.y * 4];
            int[] r = new int[4], g = new int[4], b = new int[4], a = new int[4];
            int na = 0, da = 0;
            for (int y = 0; y < dim.y; y++) {
                for (int x = 0; x < dim.x; x++) {
                    r[0] = ((int) data[da + 0]) & 0xff;
                    g[0] = ((int) data[da + 1]) & 0xff;
                    b[0] = ((int) data[da + 2]) & 0xff;
                    a[0] = ((int) data[da + 3]) & 0xff;
                    if (!lx) {
                        r[1] = ((int) data[da + 0 + 4]) & 0xff;
                        g[1] = ((int) data[da + 1 + 4]) & 0xff;
                        b[1] = ((int) data[da + 2 + 4]) & 0xff;
                        a[1] = ((int) data[da + 3 + 4]) & 0xff;
                    } else {
                        r[1] = r[0];
                        g[1] = g[0];
                        b[1] = b[0];
                        a[1] = a[0];
                    }
                    if (!ly) {
                        r[2] = ((int) data[da + 0 + dst]) & 0xff;
                        g[2] = ((int) data[da + 1 + dst]) & 0xff;
                        b[2] = ((int) data[da + 2 + dst]) & 0xff;
                        a[2] = ((int) data[da + 3 + dst]) & 0xff;
                    } else {
                        r[2] = r[0];
                        g[2] = g[0];
                        b[2] = b[0];
                        a[2] = a[0];
                    }
                    if (!lx && !ly) {
                        r[3] = ((int) data[da + 0 + dst + 4]) & 0xff;
                        g[3] = ((int) data[da + 1 + dst + 4]) & 0xff;
                        b[3] = ((int) data[da + 2 + dst + 4]) & 0xff;
                        a[3] = ((int) data[da + 3 + dst + 4]) & 0xff;
                    } else if (!ly) {
                        r[3] = r[2];
                        g[3] = g[2];
                        b[3] = b[2];
                        a[3] = a[2];
                    } else {
                        r[3] = r[1];
                        g[3] = g[1];
                        b[3] = b[1];
                        a[3] = a[1];
                    }
                    int n = 0, cr = 0, cg = 0, cb = 0;
                    for (int i = 0; i < 4; i++) {
                        if (a[i] < 128)
                            continue;
                        cr += r[i];
                        cg += g[i];
                        cb += b[i];
                        n++;
                    }
                    if (n <= 1) {
                        ndata[na + 3] = 0;
                    } else {
                        cr /= n;
                        cg /= n;
                        cb /= n;
                        int md = -1, mi = -1;
                        for (int i = 0; i < 4; i++) {
                            if (a[i] < 128)
                                continue;
                            int d = Math.abs(r[i] - cr) + Math.abs(g[i] - cg) + Math.abs(b[i] - cb);
                            if ((md == -1) || (d > md)) {
                                md = d;
                                mi = i;
                            }
                        }
                        ndata[na + 0] = (byte) r[mi];
                        ndata[na + 1] = (byte) g[mi];
                        ndata[na + 2] = (byte) b[mi];
                        ndata[na + 3] = (byte) 255;
                    }
                    na += 4;
                    da += lx ? 4 : 8;
                }
                da += ly ? 0 : dst;
            }
            return (ndata);
        }
    };

    public static final Mipmapper dav = new Mipmapper() {
        public byte[] gen4(Coord dim, byte[] data, int fmt) {
            int dst = dim.x * 4;
            dim = dim.div(2);
            boolean lx = false, ly = false;
            if (dim.x < 1) {
                dim.x = 1;
                lx = true;
            }
            if (dim.y < 1) {
                dim.y = 1;
                ly = true;
            }
            byte[] ndata = new byte[dim.x * dim.y * 4];
            int[] r = new int[4], g = new int[4], b = new int[4], a = new int[4];
            int na = 0, da = 0;
            for (int y = 0; y < dim.y; y++) {
                for (int x = 0; x < dim.x; x++) {
                    r[0] = ((int) data[da + 0]) & 0xff;
                    g[0] = ((int) data[da + 1]) & 0xff;
                    b[0] = ((int) data[da + 2]) & 0xff;
                    a[0] = ((int) data[da + 3]) & 0xff;
                    if (!lx) {
                        r[1] = ((int) data[da + 0 + 4]) & 0xff;
                        g[1] = ((int) data[da + 1 + 4]) & 0xff;
                        b[1] = ((int) data[da + 2 + 4]) & 0xff;
                        a[1] = ((int) data[da + 3 + 4]) & 0xff;
                    } else {
                        r[1] = r[0];
                        g[1] = g[0];
                        b[1] = b[0];
                        a[1] = a[0];
                    }
                    if (!ly) {
                        r[2] = ((int) data[da + 0 + dst]) & 0xff;
                        g[2] = ((int) data[da + 1 + dst]) & 0xff;
                        b[2] = ((int) data[da + 2 + dst]) & 0xff;
                        a[2] = ((int) data[da + 3 + dst]) & 0xff;
                    } else {
                        r[2] = r[0];
                        g[2] = g[0];
                        b[2] = b[0];
                        a[2] = a[0];
                    }
                    if (!lx && !ly) {
                        r[3] = ((int) data[da + 0 + dst + 4]) & 0xff;
                        g[3] = ((int) data[da + 1 + dst + 4]) & 0xff;
                        b[3] = ((int) data[da + 2 + dst + 4]) & 0xff;
                        a[3] = ((int) data[da + 3 + dst + 4]) & 0xff;
                    } else if (!ly) {
                        r[3] = r[2];
                        g[3] = g[2];
                        b[3] = b[2];
                        a[3] = a[2];
                    } else {
                        r[3] = r[1];
                        g[3] = g[1];
                        b[3] = b[1];
                        a[3] = a[1];
                    }
                    int n = 0, cr = 0, cg = 0, cb = 0;
                    for (int i = 0; i < 4; i++) {
                        if (a[i] < 128)
                            continue;
                        cr += r[i];
                        cg += g[i];
                        cb += b[i];
                        n++;
                    }
                    if (n <= 1) {
                        ndata[na + 3] = 0;
                    } else {
                        cr /= n;
                        cg /= n;
                        cb /= n;
                        int md = -1, mi = -1;
                        for (int i = 0; i < 4; i++) {
                            if (a[i] < 128)
                                continue;
                            int d = Math.abs(r[i] - cr) + Math.abs(g[i] - cg) + Math.abs(b[i] - cb);
                            if ((md == -1) || (d < md)) {
                                md = d;
                                mi = i;
                            }
                        }
                        ndata[na + 0] = (byte) r[mi];
                        ndata[na + 1] = (byte) g[mi];
                        ndata[na + 2] = (byte) b[mi];
                        ndata[na + 3] = (byte) 255;
                    }
                    na += 4;
                    da += lx ? 4 : 8;
                }
                da += ly ? 0 : dst;
            }
            return (ndata);
        }
    };

    public static final Mipmapper lanczos = new Mipmapper() {
        final PUtils.Convolution filter = new PUtils.Lanczos(2);

        public byte[] gen4(Coord dim, byte[] data, int fmt) {
            BufferedImage img = PUtils.rasterimg(Raster.createInterleavedRaster(new DataBufferByte(data, data.length),
                    dim.x, dim.y, dim.x * 4, 4, new int[]{0, 1, 2, 3}, null));
            dim = nextsz(dim);
            BufferedImage sm = PUtils.convolvedown(img, dim, filter);
            return (((DataBufferByte) sm.getRaster().getDataBuffer()).getData());
        }
    };
}
