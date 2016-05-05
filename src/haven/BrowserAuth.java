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

import java.io.IOException;
import java.net.URL;

public abstract class BrowserAuth extends AuthClient.Credentials {
    public abstract String method();

    public String tryauth(AuthClient cl) throws IOException {
        if (WebBrowser.self == null)
            throw (new AuthException("Could not find any web browser to launch"));
        Message rpl = cl.cmd("web", method());
        String stat = rpl.string();
        URL url;
        if (stat.equals("ok")) {
            url = new URL(rpl.string());
        } else if (stat.equals("no")) {
            throw (new AuthException(rpl.string()));
        } else {
            throw (new RuntimeException("Unexpected reply `" + stat + "' from auth server"));
        }
        try {
            WebBrowser.self.show(url);
        } catch (WebBrowser.BrowserException e) {
            throw (new AuthException("Could not launch web browser"));
        }
        rpl = cl.cmd("wait");
        stat = rpl.string();
        if (stat.equals("ok")) {
            return (rpl.string());
        } else if (stat.equals("no")) {
            throw (new AuthException(rpl.string()));
        } else {
            throw (new RuntimeException("Unexpected reply `" + stat + "' from auth server"));
        }
    }
}
