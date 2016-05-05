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

public class Bufflist extends Widget {
    static final int margin = 2;
    static final int num = 5;
    public final static Resource buffswim = Resource.local().loadwait("gfx/hud/buffs/toggles/swim");
    public final static Resource bufftrack = Resource.local().loadwait("gfx/hud/buffs/toggles/tracking");
    public final static Resource buffcrime = Resource.local().loadwait("gfx/hud/buffs/toggles/crime");

    private void arrange(Widget imm) {
        int i = 0;
        Coord br = new Coord();
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (!(wdg instanceof Buff))
                continue;
            Buff ch = (Buff) wdg;
            Coord c = new Coord((Buff.cframe.sz().x + margin) * (i % num), (Buff.cframe.sz().y + margin) * (i / num));
            if (ch == imm)
                ch.c = c;
            else
                ch.move(c);
            i++;
            if (c.x > br.x) br.x = c.x;
            if (c.y > br.y) br.y = c.y;
        }
        resize(br.add(Buff.cframe.sz()));
    }

    public void addchild(Widget child, Object... args) {
        add(child);
        arrange(child);
    }

    public BuffToggle gettoggle(String name) {
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof BuffToggle) {
                BuffToggle tgl = (BuffToggle) wdg;
                if (tgl.name.equals(name))
                    return tgl;
            }
        }
        return null;
    }

    public void cdestroy(Widget ch) {
        arrange(null);
    }

    public void draw(GOut g) {
        draw(g, false);
    }
}
