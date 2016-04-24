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

import java.io.IOException;
import java.io.InputStream;

public class RepeatStream extends InputStream {
    private final Repeater rep;
    private InputStream cur;

    public interface Repeater {
        public InputStream cons();
    }

    public RepeatStream(Repeater rep) {
        this.rep = rep;
        this.cur = rep.cons();
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (cur == null)
            return (-1);
        int ret;
        while ((ret = cur.read(b, off, len)) < 0) {
            cur.close();
            if ((cur = rep.cons()) == null)
                return (-1);
        }
        return (ret);
    }

    public int read() throws IOException {
        if (cur == null)
            return (-1);
        int ret;
        while ((ret = cur.read()) < 0) {
            cur.close();
            if ((cur = rep.cons()) == null)
                return (-1);
        }
        return (ret);
    }

    public void close() throws IOException {
        if (cur != null)
            cur.close();
        cur = null;
    }
}
