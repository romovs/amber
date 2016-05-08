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

import java.util.*;
import java.awt.Color;

public class Party {
    Map<Long, Member> memb = new TreeMap<Long, Member>();
    Member leader = null;
    public static final int PD_LIST = 0;
    public static final int PD_LEADER = 1;
    public static final int PD_MEMBER = 2;
    private Glob glob;

    public Party(Glob glob) {
        this.glob = glob;
    }

    public class Member {
        long gobid;
        private Coord c = null;
        Color col = Color.BLACK;

        public Gob getgob() {
            return (glob.oc.getgob(gobid));
        }

        public Coord getc() {
            Gob gob;
            try {
                if ((gob = getgob()) != null)
                    return (new Coord(gob.getc()));
            } catch (Loading e) {
            }
            return (c);
        }

        public double geta() {
            try {
                Gob gob = getgob();
                if (gob != null)
                    return gob.geta();
            } catch (Loading e) {
            }
            return 0.0;
        }
    }

    public void msg(Message msg) {
        while (!msg.eom()) {
            int type = msg.uint8();
            if (type == PD_LIST) {
                ArrayList<Long> ids = new ArrayList<Long>();
                while (true) {
                    long id = msg.int32();
                    if (id < 0)
                        break;
                    ids.add(id);
                }
                Map<Long, Member> nmemb = new TreeMap<Long, Member>();
                for (long id : ids) {
                    Member m = memb.get(id);
                    if (m == null) {
                        m = new Member();
                        m.gobid = id;
                    }
                    nmemb.put(id, m);
                }
                long lid = (leader == null) ? -1 : leader.gobid;
                memb = nmemb;
                leader = memb.get(lid);
            } else if (type == PD_LEADER) {
                Member m = memb.get((long) msg.int32());
                if (m != null)
                    leader = m;
            } else if (type == PD_MEMBER) {
                Member m = memb.get((long) msg.int32());
                Coord c = null;
                boolean vis = msg.uint8() == 1;
                if (vis)
                    c = msg.coord();
                Color col = msg.color();
                if (m != null) {
                    m.c = c;
                    m.col = col;
                }
            }
        }
    }
}
