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
import java.util.LinkedList;
import java.util.List;

public class NpcChat extends Window {
    Textlog out;
    List<Button> btns = null;

    @RName("npc")
    public static class $_ implements Factory {
        public Widget create(Widget parent, Object[] args) {
            return (new NpcChat((Coord) args[0], (String) args[1]));
        }
    }

    public NpcChat(Coord sz, String title) {
        super(sz, title);
        out = add(new Textlog(new Coord(sz.x, sz.y)), Coord.z);
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "log") {
            Color col = null;
            if (args.length > 1)
                col = (Color) args[1];
            out.append((String) args[0], col);
        } else if (msg == "btns") {
            if (btns != null) {
                for (Button b : btns)
                    ui.destroy(b);
                btns = null;
            }
            if (args.length > 0) {
                int y = out.sz.y + 3;
                btns = new LinkedList<Button>();
                for (Object text : args) {
                    Button b = add(Button.wrapped(out.sz.x, (String) text), new Coord(0, y));
                    btns.add(b);
                    y += b.sz.y + 3;
                }
            }
            pack();
        } else {
            super.uimsg(msg, args);
        }
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((btns != null) && (btns.contains(sender))) {
            wdgmsg("btn", btns.indexOf(sender));
            return;
        }
        super.wdgmsg(sender, msg, args);
    }
}
