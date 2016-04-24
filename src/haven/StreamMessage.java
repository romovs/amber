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

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamMessage extends Message implements Closeable, Flushable {
    private final InputStream bkin;
    private final OutputStream bkou;
    private int behind = 0;

    public StreamMessage(InputStream in, OutputStream out) {
        this.bkin = in;
        this.bkou = out;
    }

    public StreamMessage(InputStream in) {
        this(in, null);
    }

    public StreamMessage(OutputStream out) {
        this(null, out);
    }

    @SuppressWarnings("serial")
	public static class IOError extends BinError {
        public IOError(Throwable cause) {
            super(cause);
        }
    }

    public boolean underflow(int hint) {
        if (bkin == null)
            throw (new RuntimeException("No input stream"));
        if (hint + rt - rh <= rbuf.length) {
            System.arraycopy(rbuf, rh, rbuf, 0, rt - rh);
        } else {
            int l = (rbuf.length == 0) ? 1024 : rbuf.length;
            while (l < hint + rt - rh)
                l *= 2;
            byte[] n = new byte[l];
            System.arraycopy(rbuf, rh, n, 0, rt - rh);
            rbuf = n;
        }
        behind += rh;
        rt -= rh;
        rh = 0;
        int rv;
        try {
            rv = bkin.read(rbuf, rt, rbuf.length - rt);
        } catch (IOException e) {
            throw (new IOError(e));
        }
        if (rv < 0)
            return (false);
        rt += rv;
        return (true);
    }

    public int tell() {
        return (behind + rh);
    }

    public void flush() {
        try {
            bkou.write(wbuf, 0, wh);
        } catch (IOException e) {
            throw (new IOError(e));
        }
        wh = 0;
    }

    public void overflow(int min) {
        if (bkou == null)
            throw (new RuntimeException("No output stream"));
        if (wh > 1024)
            flush();
        if (wt - wh < min) {
            int l = (wbuf.length == 0) ? 1024 : wbuf.length;
            while (l < wh + min)
                l *= 2;
            byte[] n = new byte[l];
            System.arraycopy(wbuf, 0, n, 0, wh);
            wbuf = n;
            wt = wbuf.length;
        }
    }

    public void close() {
        try {
            try {
                if (bkin != null)
                    bkin.close();
            } finally {
                if (bkou != null) {
                    try {
                        flush();
                    } finally {
                        bkou.close();
                    }
                }
            }
        } catch (IOException e) {
            throw (new IOError(e));
        }
    }
}
