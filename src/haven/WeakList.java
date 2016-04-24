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
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class WeakList<T> extends AbstractCollection<T> {
    private final ReferenceQueue<T> cleanq = new ReferenceQueue<T>();
    private Entry<T> head = null;

    private void clean() {
        Reference<? extends T> ref;
        while ((ref = cleanq.poll()) != null) {
            Entry<?> e = (Entry<?>) ref;
	    if (e.l != null)
	        e.unlink();
        }
    }

    public Iterator<T> iterator() {
        clean();
        return (new Iterator<T>() {
            Entry<T> c = head, l = null;
            T n = null;

            public boolean hasNext() {
                while (n == null) {
                    if (c == null)
                        return (false);
                    n = (l = c).get();
                    c = c.n;
                }
                return (true);
            }

            public T next() {
                if (!hasNext())
                    throw (new NoSuchElementException());
                T ret = n;
                n = null;
                return (ret);
            }

            public void remove() {
                if (l == null)
                    throw (new IllegalStateException());
                l.unlink();
                l = null;
            }
        });
    }

    public Entry<T> add2(T e) {
        clean();
        Entry<T> n = new Entry<T>(e, this);
        n.link();
        return (n);
    }

    public boolean add(T e) {
        add2(e);
        return (true);
    }

    public void clear() {
        head = null;
    }

    public int size() {
        int ret = 0;
        for (@SuppressWarnings("unused") T e : this)
            ret++;
        return (ret);
    }

    public static class Entry<E> extends WeakReference<E> {
        private Entry<E> n, p;
        private WeakList<E> l;

        private Entry(E e, WeakList<E> l) {
            super(e, l.cleanq);
            this.l = l;
        }

        private void link() {
            this.n = l.head;
            if (l.head != null)
                l.head.p = this;
            l.head = this;
        }

        private void unlink() {
            if (this.n != null)
                this.n.p = this.p;
            if (this.p != null)
                this.p.n = this.n;
            if (l.head == this)
                l.head = this.n;
            this.n = this.p = null;
        }

        public void remove() {
            if (l == null)
                throw (new IllegalStateException());
            unlink();
            l = null;
        }
    }
}
