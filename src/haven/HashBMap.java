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
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class HashBMap<K, V> extends AbstractMap<K, V> implements BMap<K, V> {
    private final Map<K, V> fmap;
    private final Map<V, K> rmap;
    private final BMap<V, K> rev;

    private HashBMap(Map<K, V> f, Map<V, K> r, BMap<V, K> rev) {
        fmap = f;
        rmap = r;
        this.rev = rev;
    }

    public HashBMap() {
        fmap = new HashMap<K, V>();
        rmap = new HashMap<V, K>();
        rev = new HashBMap<V, K>(rmap, fmap, this);
    }

    public boolean containsKey(Object k) {
        return (fmap.containsKey(k));
    }

    private Set<Entry<K, V>> entries = null;

    public Set<Entry<K, V>> entrySet() {
        if (entries == null) {
            entries = new AbstractSet<Entry<K, V>>() {
                public int size() {
                    return (fmap.size());
                }

                public Iterator<Entry<K, V>> iterator() {
                    return (new Iterator<Entry<K, V>>() {
                        private final Iterator<Entry<K, V>> iter = fmap.entrySet().iterator();
                        private Entry<K, V> next, last;

                        @SuppressWarnings("hiding")
						class IteredEntry<K, V> implements Entry<K, V> {
                            private final K k;
                            private final V v;

                            private IteredEntry(K k, V v) {
                                this.k = k;
                                this.v = v;
                            }

                            public K getKey() {
                                return (k);
                            }

                            public V getValue() {
                                return (v);
                            }

                            @SuppressWarnings("unchecked")
							public boolean equals(Object o) {
                                return ((o instanceof IteredEntry) && (((IteredEntry<?, ?>) o).k == k) && (((IteredEntry<?, ?>) o).v == v));
                            }

                            public int hashCode() {
                                return (k.hashCode() ^ v.hashCode());
                            }

                            public V setValue(V nv) {
                                throw (new UnsupportedOperationException());
                            }
                        }

                        public boolean hasNext() {
                            if (next != null)
                                return (true);
                            if (!iter.hasNext())
                                return (false);
                            Entry<K, V> e = iter.next();
                            next = new IteredEntry<K, V>(e.getKey(), e.getValue());
                            return (true);
                        }

                        public Entry<K, V> next() {
                            if (!hasNext())
                                throw (new NoSuchElementException());
                            Entry<K, V> ret = last = next;
                            next = null;
                            return (ret);
                        }

                        public void remove() {
                            iter.remove();
                            if (rmap.remove(last.getValue()) != last.getKey())
                                throw (new ConcurrentModificationException("reverse-map invariant broken"));
                        }
                    });
                }

                public void clear() {
                    fmap.clear();
                    rmap.clear();
                }
            };
        }
        return (entries);
    }

    public V get(Object k) {
        return (fmap.get(k));
    }

    public V put(K k, V v) {
        if ((k == null) || (v == null))
            throw (new NullPointerException());
        V old = fmap.put(k, v);
        rmap.put(v, k);
        return (old);
    }

    public V remove(Object k) {
        V old = fmap.remove(k);
        rmap.remove(old);
        return (old);
    }

    public BMap<V, K> reverse() {
        return (rev);
    }
}
