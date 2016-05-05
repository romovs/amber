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

import java.util.LinkedList;
import java.util.List;

public class CPUProfile extends Profile {
    public CPUProfile(int hl) {
        super(hl);
    }

    public class Frame extends Profile.Frame {
        private List<Long> pw = new LinkedList<Long>();
        private List<String> nw = new LinkedList<String>();
        private long then, last;

        public Frame() {
            last = then = System.nanoTime();
        }

        public void tick(String nm) {
            long now = System.nanoTime();
            pw.add(now - last);
            nw.add(nm);
            last = now;
        }

        public void add(String nm, long tm) {
            pw.add(tm);
            nw.add(nm);
        }

        public void tick(String nm, long subtm) {
            long now = System.nanoTime();
            pw.add(now - last - subtm);
            nw.add(nm);
            last = now;
        }

        public void fin() {
            double total = (System.nanoTime() - then) / 1000000000.0;
            String[] nm = new String[nw.size()];
            double[] prt = new double[pw.size()];
            for (int i = 0; i < pw.size(); i++) {
                nm[i] = nw.get(i);
                prt[i] = pw.get(i) / 1000000000.0;
            }
            fin(total, nm, prt);
            pw = null;
            nw = null;
        }
    }
}
