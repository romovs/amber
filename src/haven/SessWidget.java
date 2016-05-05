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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class SessWidget extends AWidget {
    private final Defer.Future<Connection> conn;
    private boolean rep = false;

    @RName("sess")
    public static class $_ implements Factory {
        public Widget create(Widget parent, Object[] args) {
            String host = (String) args[0];
            int port = (Integer) args[1];
            byte[] cookie = Utils.hex2byte((String) args[2]);
            Object[] sargs = Utils.splice(args, 3);
            return (new SessWidget(host, port, cookie, sargs));
        }
    }

    static class Connection {
        final Session sess;
        final int error;

        Connection(Session sess, int error) {
            this.sess = sess;
            this.error = error;
        }
    }

    public SessWidget(final String addr, final int port, final byte[] cookie, final Object... args) {
        conn = Defer.later(new Defer.Callable<Connection>() {
            public Connection call() throws InterruptedException {
                InetAddress host;
                try {
                    host = InetAddress.getByName(addr);
                } catch (UnknownHostException e) {
                    return (new Connection(null, Session.SESSERR_CONN));
                }
                Session sess = new Session(new InetSocketAddress(host, port), ui.sess.username, cookie, args);
                try {
                    synchronized (sess) {
                        while (true) {
                            if (sess.state == "") {
                                Connection ret = new Connection(sess, 0);
                                sess = null;
                                return (ret);
                            } else if (sess.connfailed != 0) {
                                return (new Connection(null, sess.connfailed));
                            }
                            sess.wait();
                        }
                    }
                } finally {
                    if (sess != null)
                        sess.close();
                }
            }
        });
    }

    public void tick(double dt) throws InterruptedException {
        super.tick(dt);
        if (!rep && conn.done()) {
            wdgmsg("res", conn.get().error);
            rep = true;
        }
    }

    public void uimsg(String name, Object... args) {
        if (name == "exec") {
            ((RemoteUI) ui.rcvr).ret(conn.get().sess);
        } else {
            super.uimsg(name, args);
        }
    }

    public void destroy() {
        super.destroy();
    /* XXX: There's a race condition here, but I admit I'm not
     * sure what can properly be done about it, and it ought at
	 * least be uncommon. */
        if (conn.done()) {
            Session sess = conn.get().sess;
            if (sess != null)
                sess.close();
        } else {
            conn.cancel();
        }
    }
}
