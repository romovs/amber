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

public class Projection extends Transform {
    private Matrix4f bk;

    public Projection(Matrix4f xf) {
        super(xf);
    }

    public void apply(GOut g) {
        bk = g.st.proj;
        g.st.proj = fin(Matrix4f.id);
    }

    public void unapply(GOut g) {
        g.st.proj = bk;
    }

    public void prep(Buffer b) {
        b.put(PView.proj, this);
    }

    public float[] toclip(Coord3f ec) {
        return (fin(Matrix4f.id).mul4(ec.to4a(1)));
    }

    public Coord3f tonorm(Coord3f ec) {
        float[] o = toclip(ec);
        float d = 1 / o[3];
        return (new Coord3f(o[0] * d, o[1] * d, o[2] * d));
    }

    public Coord3f toscreen(Coord3f ec, Coord sz) {
        Coord3f n = tonorm(ec);
        return (new Coord3f(((n.x + 1) / 2) * sz.x,
                ((-n.y + 1) / 2) * sz.y,
                n.z));
    }

    public static Matrix4f makefrustum(Matrix4f d, float left, float right, float bottom, float top, float near, float far) {
        d.m[0] = (2 * near) / (right - left);
        d.m[5] = (2 * near) / (top - bottom);
        d.m[8] = (right + left) / (right - left);
        d.m[9] = (top + bottom) / (top - bottom);
        d.m[10] = -(far + near) / (far - near);
        d.m[11] = -1.0f;
        d.m[14] = -(2 * far * near) / (far - near);
        d.m[1] = d.m[2] = d.m[3] =
                d.m[4] = d.m[6] = d.m[7] =
                        d.m[12] = d.m[13] = d.m[15] = 0.0f;
        return (d);
    }

    public static Projection frustum(float left, float right, float bottom, float top, float near, float far) {
        return (new Projection(makefrustum(new Matrix4f(), left, right, bottom, top, near, far)));
    }

    public static Matrix4f makeortho(Matrix4f d, float left, float right, float bottom, float top, float near, float far) {
        d.m[0] = 2 / (right - left);
        d.m[5] = 2 / (top - bottom);
        d.m[10] = -2 / (far - near);
        d.m[12] = -(right + left) / (right - left);
        d.m[13] = -(top + bottom) / (top - bottom);
        d.m[14] = -(far + near) / (far - near);
        d.m[15] = 1.0f;
        d.m[1] = d.m[2] = d.m[3] =
                d.m[4] = d.m[6] = d.m[7] =
                        d.m[8] = d.m[9] = d.m[11] = 0.0f;
        return (d);
    }

    public static Projection ortho(float left, float right, float bottom, float top, float near, float far) {
        return (new Projection(makeortho(new Matrix4f(), left, right, bottom, top, near, far)));
    }

    public static class Modification extends Projection {
        public Projection bk;

        public Modification(Projection bk, Matrix4f mod) {
            super(mod);
            this.bk = bk;
        }

        public Matrix4f fin(Matrix4f p) {
            return (bk.fin(super.fin(p)));
        }
    }
}
