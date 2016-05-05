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

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

public class Matrix4f {
    public final float[] m;
    public static final Matrix4f id = identity();

    public Matrix4f() {
        m = new float[16];
    }

    public Matrix4f(Matrix4f b) {
        this();
        System.arraycopy(b.m, 0, m, 0, 16);
    }

    public Matrix4f(float e00, float e01, float e02, float e03, float e10, float e11, float e12, float e13, float e20, float e21, float e22, float e23, float e30, float e31, float e32, float e33) {
        this();
        m[0] = e00;
        m[4] = e01;
        m[8] = e02;
        m[12] = e03;
        m[1] = e10;
        m[5] = e11;
        m[9] = e12;
        m[13] = e13;
        m[2] = e20;
        m[6] = e21;
        m[10] = e22;
        m[14] = e23;
        m[3] = e30;
        m[7] = e31;
        m[11] = e32;
        m[15] = e33;
    }

    public Matrix4f(float[] m) {
        this.m = m;
    }

    public static Matrix4f identity() {
        return (new Matrix4f(1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1));
    }

    public Matrix4f load(Matrix4f o) {
        for (int i = 0; i < 16; i++)
            m[i] = o.m[i];
        return (this);
    }

    public float get(int x, int y) {
        return (m[y + (x * 4)]);
    }

    public void set(int x, int y, float v) {
        m[y + (x * 4)] = v;
    }

    public Matrix4f add(Matrix4f b) {
        Matrix4f n = new Matrix4f();
        for (int i = 0; i < 16; i++)
            n.m[i] = m[i] + b.m[i];
        return (n);
    }

    public Coord3f mul4(Coord3f b) {
        float x = (m[0] * b.x) + (m[4] * b.y) + (m[8] * b.z) + m[12];
        float y = (m[1] * b.x) + (m[5] * b.y) + (m[9] * b.z) + m[13];
        float z = (m[2] * b.x) + (m[6] * b.y) + (m[10] * b.z) + m[14];
        return (new Coord3f(x, y, z));
    }

    public float[] mul4(float[] b) {
        float x = (m[0] * b[0]) + (m[4] * b[1]) + (m[8] * b[2]) + (m[12] * b[3]);
        float y = (m[1] * b[0]) + (m[5] * b[1]) + (m[9] * b[2]) + (m[13] * b[3]);
        float z = (m[2] * b[0]) + (m[6] * b[1]) + (m[10] * b[2]) + (m[14] * b[3]);
        float w = (m[3] * b[0]) + (m[7] * b[1]) + (m[11] * b[2]) + (m[15] * b[3]);
        return (new float[]{x, y, z, w});
    }

    public Matrix4f mul(Matrix4f o) {
        Matrix4f n = new Matrix4f();
        int i = 0;
        for (int x = 0; x < 16; x += 4) {
            for (int y = 0; y < 4; y++) {
                n.m[i++] = (m[y] * o.m[x]) + (m[y + 4] * o.m[x + 1]) + (m[y + 8] * o.m[x + 2]) + (m[y + 12] * o.m[x + 3]);
            }
        }
        return (n);
    }

    public Matrix4f mul1(Matrix4f o) {
        int i = 0;
    /* This should get allocated on the stack unless the JVM sucks. */
        float[] n = new float[16];
        for (int x = 0; x < 16; x += 4) {
            for (int y = 0; y < 4; y++) {
                n[i++] = (m[y] * o.m[x]) + (m[y + 4] * o.m[x + 1]) + (m[y + 8] * o.m[x + 2]) + (m[y + 12] * o.m[x + 3]);
            }
        }
        for (i = 0; i < 16; i++)
            m[i] = n[i];
        return (this);
    }

    public Matrix4f transpose() {
        Matrix4f n = new Matrix4f();
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                n.set(x, y, get(y, x));
            }
        }
        return (n);
    }

    public float[] trim3() {
        return (new float[]{
                m[0], m[1], m[2],
                m[4], m[5], m[6],
                m[8], m[9], m[10],
        });
    }

    public Matrix4f trim3(float e33) {
        Matrix4f n = new Matrix4f(this);
        n.m[3] = n.m[7] = n.m[11] = n.m[12] = n.m[13] = n.m[14] = 0;
        n.m[15] = e33;
        return (n);
    }

    /* Shamelessly stolen from Mesa. */
    public Matrix4f invert() {
        float[] r = new float[16];

        r[0] = m[5] * m[10] * m[15] -
                m[5] * m[11] * m[14] -
                m[9] * m[6] * m[15] +
                m[9] * m[7] * m[14] +
                m[13] * m[6] * m[11] -
                m[13] * m[7] * m[10];

        r[4] = -m[4] * m[10] * m[15] +
                m[4] * m[11] * m[14] +
                m[8] * m[6] * m[15] -
                m[8] * m[7] * m[14] -
                m[12] * m[6] * m[11] +
                m[12] * m[7] * m[10];

        r[8] = m[4] * m[9] * m[15] -
                m[4] * m[11] * m[13] -
                m[8] * m[5] * m[15] +
                m[8] * m[7] * m[13] +
                m[12] * m[5] * m[11] -
                m[12] * m[7] * m[9];

        r[12] = -m[4] * m[9] * m[14] +
                m[4] * m[10] * m[13] +
                m[8] * m[5] * m[14] -
                m[8] * m[6] * m[13] -
                m[12] * m[5] * m[10] +
                m[12] * m[6] * m[9];

        r[1] = -m[1] * m[10] * m[15] +
                m[1] * m[11] * m[14] +
                m[9] * m[2] * m[15] -
                m[9] * m[3] * m[14] -
                m[13] * m[2] * m[11] +
                m[13] * m[3] * m[10];

        r[5] = m[0] * m[10] * m[15] -
                m[0] * m[11] * m[14] -
                m[8] * m[2] * m[15] +
                m[8] * m[3] * m[14] +
                m[12] * m[2] * m[11] -
                m[12] * m[3] * m[10];

        r[9] = -m[0] * m[9] * m[15] +
                m[0] * m[11] * m[13] +
                m[8] * m[1] * m[15] -
                m[8] * m[3] * m[13] -
                m[12] * m[1] * m[11] +
                m[12] * m[3] * m[9];

        r[13] = m[0] * m[9] * m[14] -
                m[0] * m[10] * m[13] -
                m[8] * m[1] * m[14] +
                m[8] * m[2] * m[13] +
                m[12] * m[1] * m[10] -
                m[12] * m[2] * m[9];

        r[2] = m[1] * m[6] * m[15] -
                m[1] * m[7] * m[14] -
                m[5] * m[2] * m[15] +
                m[5] * m[3] * m[14] +
                m[13] * m[2] * m[7] -
                m[13] * m[3] * m[6];

        r[6] = -m[0] * m[6] * m[15] +
                m[0] * m[7] * m[14] +
                m[4] * m[2] * m[15] -
                m[4] * m[3] * m[14] -
                m[12] * m[2] * m[7] +
                m[12] * m[3] * m[6];

        r[10] = m[0] * m[5] * m[15] -
                m[0] * m[7] * m[13] -
                m[4] * m[1] * m[15] +
                m[4] * m[3] * m[13] +
                m[12] * m[1] * m[7] -
                m[12] * m[3] * m[5];

        r[14] = -m[0] * m[5] * m[14] +
                m[0] * m[6] * m[13] +
                m[4] * m[1] * m[14] -
                m[4] * m[2] * m[13] -
                m[12] * m[1] * m[6] +
                m[12] * m[2] * m[5];

        r[3] = -m[1] * m[6] * m[11] +
                m[1] * m[7] * m[10] +
                m[5] * m[2] * m[11] -
                m[5] * m[3] * m[10] -
                m[9] * m[2] * m[7] +
                m[9] * m[3] * m[6];

        r[7] = m[0] * m[6] * m[11] -
                m[0] * m[7] * m[10] -
                m[4] * m[2] * m[11] +
                m[4] * m[3] * m[10] +
                m[8] * m[2] * m[7] -
                m[8] * m[3] * m[6];

        r[11] = -m[0] * m[5] * m[11] +
                m[0] * m[7] * m[9] +
                m[4] * m[1] * m[11] -
                m[4] * m[3] * m[9] -
                m[8] * m[1] * m[7] +
                m[8] * m[3] * m[5];

        r[15] = m[0] * m[5] * m[10] -
                m[0] * m[6] * m[9] -
                m[4] * m[1] * m[10] +
                m[4] * m[2] * m[9] +
                m[8] * m[1] * m[6] -
                m[8] * m[2] * m[5];

        float det = m[0] * r[0] + m[1] * r[4] + m[2] * r[8] + m[3] * r[12];
        if (det == 0)
            return (null);
        det = 1.0f / det;
        for (int i = 0; i < 16; i++)
            r[i] *= det;
        return (new Matrix4f(r));
    }

    public void getgl(GL gl, int matrix) {
        gl.glGetFloatv(matrix, m, 0);
    }

    public void loadgl(GL2 gl) {
        gl.glLoadMatrixf(m, 0);
    }

    public static Matrix4f fromgl(GL gl, int matrix) {
        Matrix4f m = new Matrix4f();
        m.getgl(gl, matrix);
        return (m);
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append('[');
        for (int y = 0; y < 4; y++) {
            if (y > 0)
                buf.append(", ");
            buf.append('[');
            for (int x = 0; x < 4; x++) {
                if (x > 0)
                    buf.append(", ");
                buf.append(Float.toString(get(x, y)));
            }
            buf.append(']');
        }
        buf.append(']');
        return (buf.toString());
    }

    public String toString2() {
        StringBuilder buf = new StringBuilder();
        for (int y = 0; y < 4; y++) {
            buf.append('[');
            for (int x = 0; x < 4; x++) {
                if (x > 0)
                    buf.append(", ");
                buf.append(Float.toString(get(x, y)));
            }
            buf.append("]\n");
        }
        return (buf.toString());
    }
}
