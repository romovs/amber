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

import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.Random;

public class SNoise3 {
    private final byte[] ptab = new byte[256];
    private final double[][] gtab = {
            {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
            {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
            {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1},
    };

    public SNoise3(Random rnd) {
        for (int i = 0; i < 256; i++)
            ptab[i] = (byte) i;
        for (int i = 0; i < 256; i++) {
            int r = rnd.nextInt(256);
            byte t = ptab[i];
            ptab[i] = ptab[r];
            ptab[r] = t;
        }
    }

    public SNoise3(long seed) {
        this(new Random(seed));
    }

    public SNoise3() {
        this(new Random());
    }

    public double get(double r, double x, double y, double z) {
        x /= r;
        y /= r;
        z /= r;

        double i, j, k;
        {
            double s = (x + y + z) / 3;
            i = floor(x + s);
            j = floor(y + s);
            k = floor(z + s);
        }

        double dx, dy, dz;
        {
            double s = (i + j + k) / 6;
            dx = x - (i - s);
            dy = y - (j - s);
            dz = z - (k - s);
        }

        int i1, j1, k1, i2, j2, k2;
        if ((dx >= dy) && (dy >= dz)) {
            i1 = 1;
            j1 = 0;
            k1 = 0;
            i2 = 1;
            j2 = 1;
            k2 = 0;
        } else if ((dx >= dz) && (dz >= dy)) {
            i1 = 1;
            j1 = 0;
            k1 = 0;
            i2 = 1;
            j2 = 0;
            k2 = 1;
        } else if ((dz >= dx) && (dx >= dy)) {
            i1 = 0;
            j1 = 0;
            k1 = 1;
            i2 = 1;
            j2 = 0;
            k2 = 1;
        } else if ((dz >= dy) && (dy >= dx)) {
            i1 = 0;
            j1 = 0;
            k1 = 1;
            i2 = 0;
            j2 = 1;
            k2 = 1;
        } else if ((dy >= dz) && (dz >= dx)) {
            i1 = 0;
            j1 = 1;
            k1 = 0;
            i2 = 0;
            j2 = 1;
            k2 = 1;
        } else /* if((dy >= dx) && (dx >= dz)) */ {
            i1 = 0;
            j1 = 1;
            k1 = 0;
            i2 = 1;
            j2 = 1;
            k2 = 0;
        }

        double x1 = dx - i1 + (1.0 / 6.0), y1 = dy - j1 + (1.0 / 6.0), z1 = dz - k1 + (1.0 / 6.0);
        double x2 = dx - i2 + (1.0 / 3.0), y2 = dy - j2 + (1.0 / 3.0), z2 = dz - k2 + (1.0 / 3.0);
        double x3 = dx - 0.5, y3 = dy - 0.5, z3 = dz - 0.5;

        int ip = (int) i, jp = (int) j, kp = (int) k;
        double[] g0 = gtab[((int) ptab[(int) (ip + ptab[(int) (jp + ptab[(int) kp & 0xff]) & 0xff]) & 0xff] & 0xff) % 12];
        double[] g1 = gtab[((int) ptab[(int) (ip + i1 + ptab[(int) (jp + j1 + ptab[(int) (kp + k1) & 0xff]) & 0xff]) & 0xff] & 0xff) % 12];
        double[] g2 = gtab[((int) ptab[(int) (ip + i2 + ptab[(int) (jp + j2 + ptab[(int) (kp + k2) & 0xff]) & 0xff]) & 0xff] & 0xff) % 12];
        double[] g3 = gtab[((int) ptab[(int) (ip + 1 + ptab[(int) (jp + 1 + ptab[(int) (kp + 1) & 0xff]) & 0xff]) & 0xff] & 0xff) % 12];

        double n0 = 0.6 - (dx * dx) - (dy * dy) - (dz * dz);
        double n1 = 0.6 - (x1 * x1) - (y1 * y1) - (z1 * z1);
        double n2 = 0.6 - (x2 * x2) - (y2 * y2) - (z2 * z2);
        double n3 = 0.6 - (x3 * x3) - (y3 * y3) - (z3 * z3);

        double v = 0.0;
        if (n0 > 0) v += n0 * n0 * n0 * n0 * ((g0[0] * dx) + (g0[1] * dy) + (g0[2] * dz));
        if (n1 > 0) v += n1 * n1 * n1 * n1 * ((g1[0] * x1) + (g1[1] * y1) + (g1[2] * z1));
        if (n2 > 0) v += n2 * n2 * n2 * n2 * ((g2[0] * x2) + (g2[1] * y2) + (g2[2] * z2));
        if (n3 > 0) v += n3 * n3 * n3 * n3 * ((g3[0] * x3) + (g3[1] * y3) + (g3[2] * z3));

        return (min(max(v * 32, -1.0), 1.0));
    }

    public double getr(double lo, double hi, double r, double x, double y, double z) {
        return ((((get(r, x, y, z) * 0.5) + 0.5) * (hi - lo)) + lo);
    }

    public int geti(int lo, int hi, double r, double x, double y, double z) {
        return (min((int) (((get(r, x, y, z) * 0.5) + 0.5) * (hi - lo)), (int) ((hi - lo) - 1)) + lo);
    }

    public static void main(String[] args) throws Exception {
        Coord sz = new Coord(512, 512);
        java.awt.image.WritableRaster buf = PUtils.imgraster(sz);
        SNoise3 n = new SNoise3(Long.parseLong(args[0]));
        for (int y = 0; y < sz.y; y++) {
            for (int x = 0; x < sz.x; x++) {
                buf.setSample(x, y, 0, n.geti(0, 256, 128, x, y, 0));
                buf.setSample(x, y, 1, n.geti(0, 256, 128, x, y, 1428));
                buf.setSample(x, y, 2, n.geti(0, 256, 128, x, y, 5291));
                buf.setSample(x, y, 3, 255);
            }
        }
        javax.imageio.ImageIO.write(PUtils.rasterimg(buf), "PNG", new java.io.File(args[1]));
    }
}
