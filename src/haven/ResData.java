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

public class ResData {
    public Indir<Resource> res;
    public MessageBuf sdt;

    public ResData(Indir<Resource> res, Message sdt) {
        this.res = res;
        this.sdt = new MessageBuf(sdt);
    }

    public ResData clone() {
        return (new ResData(res, sdt));
    }

    public boolean equals(Object other) {
        if (!(other instanceof ResData))
            return (false);
        ResData o = (ResData) other;
        return (res.equals(o.res) && sdt.equals(o.sdt));
    }

    public static List<ResData> wrap(List<? extends Indir<Resource>> in) {
        List<ResData> ret = new ArrayList<ResData>(in.size());
        for (Indir<Resource> res : in)
            ret.add(new ResData(res, Message.nil));
        return (ret);
    }

    public static ResData[] wrap(Indir<Resource>[] in) {
        ResData[] ret = new ResData[in.length];
        for (int i = 0; i < in.length; i++)
            ret[i] = new ResData(in[i], Message.nil);
        return (ret);
    }
}
