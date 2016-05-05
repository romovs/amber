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

import static haven.GOut.checkerr;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import javax.media.opengl.GL;

import haven.TexGL.TexOb;

public class TexCube {
    protected TexOb t = null;
    private Object idmon = new Object();
    protected int tdim;
    protected final BufferedImage back;

    public TexCube(BufferedImage img) {
        Coord sz = Utils.imgsz(img);
        tdim = sz.x / 4;
        if ((tdim * 4) != sz.x)
            throw (new RuntimeException("Cube-mapped texture has width undivisible by 4"));
        if ((tdim * 3) != sz.y)
            throw (new RuntimeException("Cube-mapped texture is not 4:3"));
        this.back = img;
    }

    private static final int[][] order = {
            {3, 1},            // +X
            {1, 1},            // -X
            {2, 0},            // +Y
            {2, 2},            // -Y
            {2, 1},            // +Z
            {0, 1},            // -Z
    };

    protected void fill(GOut g) {
        BGL gl = g.gl;
        Coord dim = new Coord(tdim, tdim);
        for (int i = 0; i < order.length; i++) {
            ByteBuffer data = ByteBuffer.wrap(TexI.convert(back, dim, new Coord(order[i][0] * tdim, order[i][1] * tdim), dim));
            gl.glTexImage2D(GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL.GL_RGBA, tdim, tdim, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, data);
        }
    }

    private void create(GOut g) {
        BGL gl = g.gl;
        t = new TexOb(g);
        gl.glBindTexture(GL.GL_TEXTURE_CUBE_MAP, t);
        gl.glTexParameteri(GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        fill(g);
        checkerr(gl);
    }

    public TexOb glid(GOut g) {
        synchronized (idmon) {
            if ((t != null) && (t.cur != g.curgl))
                dispose();
            if (t == null)
                create(g);
            return (t);
        }
    }

    public void dispose() {
        synchronized (idmon) {
            if (t != null) {
                t.dispose();
                t = null;
            }
        }
    }
}
