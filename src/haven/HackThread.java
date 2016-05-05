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

import java.util.HashSet;
import java.util.Set;

public class HackThread extends Thread {
    public HackThread(ThreadGroup tg, Runnable target, String name) {
    /* Hack #1: Override stupid security-managers' whims to move
     * threads into whimsical thread-groups. */
        super((tg == null) ? tg() : tg, target, name);
    }

    public HackThread(Runnable target, String name) {
        this(null, target, name);
    }

    public HackThread(String name) {
        this(null, name);
    }

    public static ThreadGroup tg() {
        return (Thread.currentThread().getThreadGroup());
    }

    /* Hack #2: Allow hooking into thread interruptions to as to
     * interrupt normally uninterruptible stuff like Sockets. For a
     * more thorough explanation why this is necessary, see
     * HackSocket. */
    private Set<Runnable> ils = new HashSet<Runnable>();

    public void addil(Runnable r) {
        synchronized (ils) {
            ils.add(r);
        }
    }

    public void remil(Runnable r) {
        synchronized (ils) {
            ils.remove(r);
        }
    }

    public void interrupt() {
        super.interrupt();
        synchronized (ils) {
            for (Runnable r : ils)
                r.run();
        }
    }
}
