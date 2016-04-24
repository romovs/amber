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
import java.util.Map;

public class IDRef {
    private static Map<Object, WRef> map = new HashMap<Object, WRef>();
    private static ReferenceQueue<IDRef> queue = new ReferenceQueue<IDRef>();
    private static int nextseq = 0;
    /* Just for debugging */
    private final Object val;
    private final int seq;

    private IDRef(Object val) {
        synchronized (IDRef.class) {
            this.seq = nextseq++;
        }
        this.val = val;
    }

    private static class WRef extends WeakReference<IDRef> {
        private final Object val;

        private WRef(IDRef ref, Object val) {
            super(ref, queue);
            this.val = val;
        }
    }

    public static IDRef intern(Object x) {
        if (x == null)
            return (null);
        synchronized (map) {
            WRef old;
            while ((old = (WRef) queue.poll()) != null) {
                if (map.get(old.val) == old)
                    map.remove(old.val);
            }
            WRef ref = map.get(x);
            IDRef id = (ref == null) ? null : (ref.get());
            if (id == null) {
                id = new IDRef(x);
                ref = new WRef(id, x);
                map.put(x, ref);
            }
            return (id);
        }
    }

    public String toString() {
        return ("<ID: " + val + " (" + seq + ")>");
    }
}
