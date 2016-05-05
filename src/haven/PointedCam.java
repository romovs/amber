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

public class PointedCam extends Camera {
    Coord3f base = Coord3f.o;
    float dist = 5.0f, e, a;

    public PointedCam() {
        super(Matrix4f.identity());
    }

    public Matrix4f fin(Matrix4f p) {
        update(compute(base, dist, e, a));
        return (super.fin(p));
    }

    public static Matrix4f compute(Coord3f base, float dist, float e, float a) {
        return (makexlate(new Matrix4f(), new Coord3f(0.0f, 0.0f, -dist))
                .mul1(makerot(new Matrix4f(), new Coord3f(-1.0f, 0.0f, 0.0f), ((float) Math.PI / 2.0f) - e))
                .mul1(makerot(new Matrix4f(), new Coord3f(0.0f, 0.0f, -1.0f), ((float) Math.PI / 2.0f) + a))
                .mul1(makexlate(new Matrix4f(), base.inv())));
    }
}
