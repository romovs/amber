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

import javax.media.opengl.GL2;

public class DirLight extends Light {
    public float[] dir;

    public DirLight(FColor col, Coord3f dir) {
        super(col);
        this.dir = dir.norm().to4a(0.0f);
    }

    public DirLight(Color col, Coord3f dir) {
        super(col);
        this.dir = dir.norm().to4a(0.0f);
    }

    public DirLight(FColor amb, FColor dif, FColor spc, Coord3f dir) {
        super(amb, dif, spc);
        this.dir = dir.norm().to4a(0.0f);
    }

    public DirLight(Color amb, Color dif, Color spc, Coord3f dir) {
        super(amb, dif, spc);
        this.dir = dir.norm().to4a(0.0f);
    }

    public void enable(GOut g, int idx) {
        super.enable(g, idx);
        BGL gl = g.gl;
        gl.glLightfv(GL2.GL_LIGHT0 + idx, GL2.GL_POSITION, dir, 0);
    }
}
