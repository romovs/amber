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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class I2<T> implements Iterator<T> {
    private Iterator<Iterator<T>> is;
    private Iterator<T> cur;
    private T co;
    private boolean hco;

    @SafeVarargs
    public I2(Iterator<T>... is) {
        this.is = Arrays.asList(is).iterator();
        f();
    }

    public I2(Collection<Iterator<T>> is) {
        this.is = is.iterator();
        f();
    }

    private void f() {
        while (true) {
            if ((cur != null) && cur.hasNext()) {
                co = cur.next();
                hco = true;
                return;
            }
            if (is.hasNext()) {
                cur = is.next();
                continue;
            }
            hco = false;
            return;
        }
    }

    public boolean hasNext() {
        return (hco);
    }

    public T next() {
        if (!hco)
            throw (new NoSuchElementException());
        T ret = co;
        f();
        return (ret);
    }

    public void remove() {
        throw (new UnsupportedOperationException());
    }
}
