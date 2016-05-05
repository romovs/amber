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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;

public class ArrayIdentity {
    private static HashMap<Entry<?>, Entry<?>> set = new HashMap<Entry<?>, Entry<?>>();
    private static ReferenceQueue<Object> cleanq = new ReferenceQueue<Object>();

    private static class Entry<T> extends WeakReference<T[]> {
        private Entry(T[] arr) {
            super(arr, cleanq);
        }

        public boolean equals(Object x) {
            if (!(x instanceof Entry))
                return (false);
            T[] a = get();
            if (a == null)
                return (false);
            Entry<?> e = (Entry<?>) x;
            Object[] ea = e.get();
            if (ea == null)
                return (false);
            if (ea.length != a.length)
                return (false);
            for (int i = 0; i < a.length; i++) {
                if (a[i] != ea[i])
                    return (false);
            }
            return (true);
        }

        public int hashCode() {
            T[] a = get();
            if (a == null)
                return (0);
            int ret = 1;
            for (T o : a)
                ret = (ret * 31) + System.identityHashCode(o);
            return (ret);
        }
    }

    private static void clean() {
        Reference<?> ref;
        while ((ref = cleanq.poll()) != null)
            set.remove(ref);
    }

    @SuppressWarnings("unchecked")
    private static <T> Entry<T> getcanon(Entry<T> e) {
        return ((Entry<T>) set.get(e));
    }

    public static <T> T[] intern(T[] arr) {
        Entry<T> e = new Entry<T>(arr);
        synchronized (ArrayIdentity.class) {
            clean();
            Entry<T> e2 = getcanon(e);
            T[] ret;
            if (e2 == null) {
                set.put(e, e);
                ret = arr;
            } else {
                ret = e2.get();
                if (ret == null) {
                    set.remove(e2);
                    set.put(e, e);
                    ret = arr;
                }
            }
            return (ret);
        }
    }
}
