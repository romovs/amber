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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

@SuppressWarnings("serial")
public class PrioQueue<E extends Prioritized> extends LinkedList<E> {
    public E peek() {
        E rv = null;
        int mp = 0;
        for (E e : this) {
            int ep = e.priority();
            if ((rv == null) || (ep > mp)) {
                mp = ep;
                rv = e;
            }
        }
        return (rv);
    }

    public E element() {
        E rv;
        if ((rv = peek()) == null)
            throw (new NoSuchElementException());
        return (rv);
    }

    public E poll() {
        E rv = peek();
        removeid(rv);
        return (rv);
    }

    public E remove() {
        E rv;
        if ((rv = poll()) == null)
            throw (new NoSuchElementException());
        return (rv);
    }

    public boolean removeid(E el) {
        for (Iterator<E> i = iterator(); i.hasNext(); ) {
            if (i.next() == el) {
                i.remove();
                return (true);
            }
        }
        return (false);
    }
}
