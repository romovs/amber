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

package haven.test;

import haven.Widget;

public class DumpBot extends Robot {
    public DumpBot(TestClient c) {
        super(c);
    }

    public void newwdg(int id, Widget w, Object... args) {
        System.out.println(c + ": new widget: " + w + " (" + id + ")");
    }

    public void dstwdg(int id, Widget w) {
        System.out.println(c + ": destroyed: " + w + " (" + id + ")");
    }

    public void uimsg(int id, Widget w, String msg, Object... args) {
        System.out.println(c + ": uimsg: " + w + " (" + id + "): " + msg);
    }
}
