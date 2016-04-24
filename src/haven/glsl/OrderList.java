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

package haven.glsl;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class OrderList<E> extends AbstractCollection<E> {
    private final List<Element> bk = new ArrayList<Element>();
    private boolean sorted;

    class Element implements Comparable<Element> {
        final E e;
        final int o;

        Element(E e, int o) {
            this.e = e;
            this.o = o;
        }

        public int compareTo(Element b) {
            return (this.o - b.o);
        }
    }

    public boolean add(E e, int o) {
        bk.add(new Element(e, o));
        sorted = false;
        return (true);
    }

    public int size() {
        return (bk.size());
    }

    public Iterator<E> iterator() {
        if (!sorted) {
            Collections.sort(bk);
            sorted = true;
        }
        return (new Iterator<E>() {
            private final Iterator<Element> bi = bk.iterator();

            public boolean hasNext() {
                return (bi.hasNext());
            }

            public E next() {
                return (bi.next().e);
            }

            public void remove() {
                bi.remove();
            }
        });
    }
}
