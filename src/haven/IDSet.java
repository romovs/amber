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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;

public class IDSet<T> {
    private final HashMap<WRef<T>, WRef<T>> bk = new HashMap<WRef<T>, WRef<T>>();
    private final ReferenceQueue<T> queue = new ReferenceQueue<T>();

    private static class WRef<T> extends WeakReference<T> {
        private final int hash;

        private WRef(T ob, ReferenceQueue<T> queue) {
            super(ob, queue);
            hash = ob.hashCode();
        }

        public boolean equals(Object o) {
            if (!(o instanceof WRef))
                return (false);
            WRef<?> r = (WRef<?>) o;
            return (Utils.eq(get(), r.get()));
        }

        public int hashCode() {
            return (hash);
        }
    }

    private void clean() {
        WRef<?> old;
        while ((old = (WRef<?>) queue.poll()) != null)
            bk.remove(old);
    }

    public T intern(T ob) {
        synchronized (bk) {
            clean();
            WRef<T> ref = new WRef<T>(ob, queue);
            WRef<T> old = bk.get(ref);
            if (old == null) {
                bk.put(ref, ref);
                return (ob);
            } else {
        /* Should never return null, since ob is referenced in
         * this frame during the entirety of the lookup. */
                return (old.get());
            }
        }
    }

    public int size() {
        synchronized (bk) {
            return (bk.size());
        }
    }
}
