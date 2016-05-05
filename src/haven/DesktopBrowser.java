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

import java.awt.Desktop;
import java.net.URL;

public class DesktopBrowser extends WebBrowser {
    private final Desktop desktop;

    private DesktopBrowser(Desktop desktop) {
        this.desktop = desktop;
    }

    public static DesktopBrowser create() {
        try {
            Class.forName("java.awt.Desktop");
            if (!Desktop.isDesktopSupported())
                return (null);
            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.BROWSE))
                return (null);
            return (new DesktopBrowser(desktop));
        } catch (Exception e) {
            return (null);
        }
    }

    public void show(URL url) {
        try {
            desktop.browse(url.toURI());
        } catch (Exception e) {
            throw (new BrowserException(e));
        }
    }
}
