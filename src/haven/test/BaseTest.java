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

import haven.Audio;
import haven.Resource;

public abstract class BaseTest implements Runnable {
    public ThreadGroup tg;
    public Thread me;

    public BaseTest() {
        tg = new ThreadGroup("Test process");
        Resource.loadergroup = tg;
        Audio.enabled = false;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                printf("Terminating test upon JVM shutdown...");
                BaseTest.this.stop();
                try {
                    me.join();
                    printf("Shut down cleanly");
                } catch (InterruptedException e) {
                    printf("Termination handler interrupted");
                }
            }
        });
    }

    public static void printf(String fmt, Object... args) {
        System.out.println(String.format(fmt, args));
    }

    public void start() {
        me = new Thread(tg, this, "Test controller");
        me.start();
    }

    public void stop() {
        me.interrupt();
    }
}
