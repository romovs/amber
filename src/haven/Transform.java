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

public abstract class Transform extends GLState {
    private Matrix4f xf;
    private Matrix4f lp = null, fin;

    public Transform(Matrix4f xf) {
        this.xf = xf;
    }

    public void update(Matrix4f xf) {
        this.xf = xf;
        this.lp = null;
    }

    public Matrix4f fin(Matrix4f p) {
        if (p != lp)
            fin = (lp = p).mul(xf);
        return (fin);
    }

    public static Matrix4f makexlate(Matrix4f d, Coord3f c) {
        d.m[0] = d.m[5] = d.m[10] = d.m[15] = 1.0f;
        d.m[1] = d.m[2] = d.m[3] =
                d.m[4] = d.m[6] = d.m[7] =
                        d.m[8] = d.m[9] = d.m[11] = 0.0f;
        d.m[12] = c.x;
        d.m[13] = c.y;
        d.m[14] = c.z;
        return (d);
    }

    public static Matrix4f makerot(Matrix4f d, Coord3f axis, float angle) {
        float c = (float) Math.cos(angle), s = (float) Math.sin(angle), C = 1.0f - c;
        float x = axis.x, y = axis.y, z = axis.z;
        d.m[3] = d.m[7] = d.m[11] = d.m[12] = d.m[13] = d.m[14] = 0.0f;
        d.m[15] = 1.0f;
        d.m[0] = (x * x * C) + c;
        d.m[4] = (y * x * C) - (z * s);
        d.m[8] = (z * x * C) + (y * s);
        d.m[1] = (x * y * C) + (z * s);
        d.m[5] = (y * y * C) + c;
        d.m[9] = (z * y * C) - (x * s);
        d.m[2] = (x * z * C) - (y * s);
        d.m[6] = (y * z * C) + (x * s);
        d.m[10] = (z * z * C) + c;
        return (d);
    }

    public static Matrix4f rxinvert(Matrix4f m) {
    /* This assumes that m is merely a composition of rotations
     * and translations. */
        return (m.trim3(1).transpose().mul1(makexlate(new Matrix4f(), new Coord3f(-m.m[12], -m.m[13], -m.m[14]))));
    }

    public String toString() {
        return (this.getClass().getName() + "(" + xf + ")");
    }
}
