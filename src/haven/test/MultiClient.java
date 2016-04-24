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

import java.util.Collection;
import java.util.HashSet;

public class MultiClient extends BaseTest {
    public Collection<TestClient> clients = new HashSet<TestClient>();
    public int num, delay;
    public int started;

    public MultiClient(int num, int delay) {
        this.num = num;
        this.delay = delay;
        this.started = 0;
    }

    public void run() {
        long lastck = System.currentTimeMillis();
        long laststarted = 0;
        try {
            while (true) {
                long now = System.currentTimeMillis();
                long timeout = 1000;
                if ((started < num) && (now - laststarted >= delay)) {
                    TestClient c = new TestClient("test" + (started + 1));
                    new CharSelector(c, null, null) {
                        public void succeed() {
                            System.out.println("Selected character");
                        }
                    };
                    synchronized (clients) {
                        clients.add(c);
                    }
                    c.start();
                    started++;
                    laststarted = now;
                }
                if ((started < num) && ((delay - (now - laststarted)) < timeout))
                    timeout = delay - (now - laststarted);
                if (timeout < 0)
                    timeout = 0;
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    num = 0;
                    stopall();
                }
                if (now - lastck > 1000) {
                    int alive = 0;
                    for (TestClient c : clients) {
                        if (c.alive())
                            alive++;
                    }
                    if ((alive == 0) && (started >= num)) {
                        printf("All clients are dead, exiting");
                        break;
                    }
                    printf("Alive: %d/%d/%d", alive, started, num);
                    lastck = now;
                }
            }
        } finally {
            stopall();
        }
    }

    public void stopall() {
        synchronized (clients) {
            for (TestClient c : clients)
                c.stop();
        }
    }

    public static void usage() {
        System.err.println("usage: MultiClient NUM [DELAY]");
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            usage();
            System.exit(1);
        }
        int num = Integer.parseInt(args[0]);
        int delay = 0;
        if (args.length > 1)
            delay = Integer.parseInt(args[1]);
        new MultiClient(num, delay).start();
    }
}
