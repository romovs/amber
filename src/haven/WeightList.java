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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("serial")
public class WeightList<T> implements java.io.Serializable {
    List<T> c;
    List<Integer> w;
    int tw = 0;

    public WeightList() {
        c = new ArrayList<T>();
        w = new ArrayList<Integer>();
    }

    public void add(T c, int w) {
        this.c.add(c);
        this.w.add(w);
        tw += w;
    }

    public T pick(int p) {
        p %= tw;
        int i = 0;
        while (true) {
            if ((p -= w.get(i)) < 0)
                break;
            i++;
        }
        return (c.get(i));
    }

    public T pick(Random gen) {
        return (pick(gen.nextInt(tw)));
    }

    public int size() {
        return (c.size());
    }
}
