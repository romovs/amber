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

package haven.resutil;

import haven.*;

import java.util.*;

public class CSprite extends Sprite {
    private final Coord3f cc;
    private final List<Rendered> parts = new ArrayList<Rendered>();
    private final Random rnd;

    public CSprite(Owner owner, Resource res) {
        super(owner, res);
        rnd = owner.mkrandoom();
        Gob gob = (Gob) owner;
        cc = gob.getrc();
    }

    public void addpart(Location loc, GLState mat, Rendered part) {
        parts.add(GLState.compose(loc, mat).apply(part));
    }

    public void addpart(float xo, float yo, GLState mat, Rendered part) {
        Coord3f pc = new Coord3f(xo, -yo, owner.glob().map.getcz(cc.x + xo, cc.y + yo) - cc.z);
        Location loc = new Location(Transform.makexlate(new Matrix4f(), pc)
                .mul1(Transform.makerot(new Matrix4f(), Coord3f.zu, (float) (rnd.nextFloat() * Math.PI * 2))));
        addpart(loc, mat, part);
    }

    public boolean setup(RenderList rl) {
        rl.prepc(Location.goback("gobx"));
        for (Rendered p : parts)
            rl.add(p, null);
        return (false);
    }

    public Object staticp() {
	return(CONSTANS);
    }
}
