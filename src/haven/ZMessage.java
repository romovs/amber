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

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class ZMessage extends Message {
    private Inflater z = new Inflater();
    private final Message bk;

    public ZMessage(Message from) {
        this.bk = from;
    }

    public boolean underflow(int hint) {
        if (z == null)
            return (false);
        boolean ret = false;
        if (rbuf.length - rt < 1) {
            byte[] n = new byte[Math.max(1024, rt - rh) + rt - rh];
            System.arraycopy(rbuf, rh, n, 0, rt - rh);
            rt -= rh;
            rh = 0;
            rbuf = n;
        }
        try {
            while (true) {
                int rv = z.inflate(rbuf, rt, rbuf.length - rt);
                if (rv == 0) {
                    if (z.finished()) {
                        z.end();
                        z = null;
                        return (ret);
                    }
                    if (z.needsInput()) {
                        if (bk.rt - bk.rh < 1) {
                            if (!bk.underflow(128))
                                throw (new EOF("Unterminated z-blob"));
                        }
                        z.setInput(bk.rbuf, bk.rh, bk.rt - bk.rh);
                    }
                } else {
                    rt += rv;
                    return (true);
                }
            }
        } catch (DataFormatException e) {
            throw (new RuntimeException("Malformed z-blob", e));
        }
    }

    public void overflow(int min) {
        throw (new RuntimeException("ZMessages are not writable yet"));
    }
}
