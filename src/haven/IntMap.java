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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class IntMap<V> extends AbstractMap<Integer, V> {
    private static final Object nil = new Object();
    private Object[] vals;
    private int sz;

    public IntMap(int capacity) {
        vals = new Object[capacity];
    }

    public IntMap() {
        this(0);
    }

    public IntMap(Map<Integer, V> m) {
        this();
        putAll(m);
    }

    private Object icast(V v) {
        return ((v == null) ? nil : v);
    }

    @SuppressWarnings("unchecked")
    private V ocast(Object v) {
        return ((v == nil) ? null : ((V) v));
    }

    public boolean containsKey(int k) {
        return ((vals.length > k) && (vals[k] != null));
    }

    public boolean containsKey(Integer k) {
        return (containsKey(k.intValue()));
    }

    private class IteredEntry implements Entry<Integer, V> {
        private final int k;

        private IteredEntry(int k) {
            this.k = k;
        }

        public Integer getKey() {
            return (k);
        }

        public V getValue() {
            return (get(k));
        }

        @SuppressWarnings("unchecked")
        public boolean equals(Object o) {
            return ((o instanceof IntMap.IteredEntry) && (((IteredEntry) o).k == k));
        }

        public int hashCode() {
            return (k);
        }

        public V setValue(V nv) {
            return (put(k, nv));
        }
    }

    private Set<Entry<Integer, V>> entries = null;

    public Set<Entry<Integer, V>> entrySet() {
        if (entries == null)
            entries = new AbstractSet<Entry<Integer, V>>() {
                public int size() {
                    return (sz);
                }

                public Iterator<Entry<Integer, V>> iterator() {
                    return (new Iterator<Entry<Integer, V>>() {
                        private int ni = -1;
                        private int li = -1;

                        public boolean hasNext() {
                            if (ni < 0) {
                                for (ni = li + 1; ni < vals.length; ni++) {
                                    if (vals[ni] != null)
                                        break;
                                }
                            }
                            return (ni < vals.length);
                        }

                        public Entry<Integer, V> next() {
                            if (!hasNext())
                                throw (new NoSuchElementException());
                            Entry<Integer, V> ret = new IteredEntry(ni);
                            li = ni;
                            ni = -1;
                            return (ret);
                        }

                        public void remove() {
                            vals[li] = null;
                        }
                    });
                }

                public void clear() {
                    vals = new Object[0];
                }
            };
        return (entries);
    }

    public V get(int k) {
        if ((k < 0) || (vals.length <= k))
            return (null);
        return (ocast(vals[k]));
    }

    public V get(Object k) {
        if (!(k instanceof Integer))
            return (null);
        return (get(((Integer) k).intValue()));
    }

    public V put(int k, V v) {
        if (vals.length <= k) {
            Object[] n = new Object[k + 1];
            System.arraycopy(vals, 0, n, 0, vals.length);
            vals = n;
        }
        V ret = ocast(vals[k]);
        vals[k] = icast(v);
        return (ret);
    }

    public V put(Integer k, V v) {
        return (put(k.intValue(), v));
    }

    public V remove(int k) {
        if (k >= vals.length)
            return (null);
        V ret = ocast(vals[k]);
        vals[k] = null;
        return (ret);
    }

    public V remove(Integer k) {
        return (remove(k.intValue()));
    }
}
