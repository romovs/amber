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

public class PosLight extends Light {
    public float[] pos;
    public float ac = 1.0f, al = 0.0f, aq = 0.0f;

    public PosLight(FColor col, Coord3f pos) {
        super(col);
        this.pos = pos.to4a(1);
    }

    public PosLight(Color col, Coord3f pos) {
        super(col);
        this.pos = pos.to4a(1);
    }

    public PosLight(FColor amb, FColor dif, FColor spc, Coord3f pos) {
        super(amb, dif, spc);
        this.pos = pos.to4a(1);
    }

    public PosLight(Color amb, Color dif, Color spc, Coord3f pos) {
        super(amb, dif, spc);
        this.pos = pos.to4a(1);
    }

    public void move(Coord3f pos) {
        this.pos = pos.to4a(1);
    }

    public void att(float c, float l, float q) {
        ac = c;
        al = l;
        aq = q;
    }

    public void enable(GOut g, int idx) {
        super.enable(g, idx);
        BGL gl = g.gl;
        gl.glLightfv(GL2.GL_LIGHT0 + idx, GL2.GL_POSITION, pos, 0);
        gl.glLightf(GL2.GL_LIGHT0 + idx, GL2.GL_CONSTANT_ATTENUATION, ac);
        gl.glLightf(GL2.GL_LIGHT0 + idx, GL2.GL_LINEAR_ATTENUATION, al);
        gl.glLightf(GL2.GL_LIGHT0 + idx, GL2.GL_QUADRATIC_ATTENUATION, aq);
    }

    public void disable(GOut g, int idx) {
        BGL gl = g.gl;
        gl.glLightf(GL2.GL_LIGHT0 + idx, GL2.GL_CONSTANT_ATTENUATION, 1.0f);
        gl.glLightf(GL2.GL_LIGHT0 + idx, GL2.GL_LINEAR_ATTENUATION, 0.0f);
        gl.glLightf(GL2.GL_LIGHT0 + idx, GL2.GL_QUADRATIC_ATTENUATION, 0.0f);
        super.disable(g, idx);
    }
}
