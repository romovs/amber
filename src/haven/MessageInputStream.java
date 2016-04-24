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

import java.io.InputStream;

public class MessageInputStream extends InputStream {
    private final Message bk;

    public MessageInputStream(Message from) {
        this.bk = from;
    }

    public int read() {
        while (bk.rt - bk.rh < 1) {
            if (!bk.underflow(1))
                return (-1);
        }
        return (Utils.ub(bk.rbuf[bk.rh++]));
    }

    public int read(byte[] buf, int off, int len) {
        int read = 0;
        while (len > 0) {
            while (bk.rh >= bk.rt) {
                if (!bk.underflow(Math.min(len, 1024))) {
                    return ((read > 0) ? read : -1);
                }
            }
            int r = Math.min(len, bk.rt - bk.rh);
            System.arraycopy(bk.rbuf, bk.rh, buf, off, r);
            bk.rh += r;
            off += r;
            len -= r;
            read += r;
        }
        return (read);
    }
}
