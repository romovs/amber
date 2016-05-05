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
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

public class StreamTee extends InputStream {
    private InputStream in;
    private List<OutputStream> forked = new LinkedList<OutputStream>();
    private boolean readeof = false;
    private boolean ncwe = false; /* NCWE = No Close Without EOF */

    public StreamTee(InputStream in) {
        this.in = in;
    }

    public int available() throws IOException {
        return (in.available());
    }

    public void close() throws IOException {
        in.close();
        if (!ncwe || readeof) {
            synchronized (forked) {
                for (OutputStream s : forked)
                    s.close();
            }
        }
    }

    public void setncwe() {
        ncwe = true;
    }

    public void flush() throws IOException {
        synchronized (forked) {
            for (OutputStream s : forked)
                s.flush();
        }
    }

    public void mark(int limit) {
    }

    public boolean markSupported() {
        return (false);
    }

    public int read() throws IOException {
        int rv = in.read();
        if (rv >= 0) {
            synchronized (forked) {
                for (OutputStream s : forked)
                    s.write(rv);
            }
        } else {
            readeof = true;
        }
        return (rv);
    }

    public int read(byte[] buf, int off, int len) throws IOException {
        int rv = in.read(buf, off, len);
        if (rv > 0) {
            synchronized (forked) {
                for (OutputStream s : forked)
                    s.write(buf, off, rv);
            }
        } else {
            readeof = true;
        }
        return (rv);
    }

    public void reset() throws IOException {
        throw (new IOException("Mark not supported on StreamTee"));
    }

    public void attach(OutputStream s) {
        synchronized (forked) {
            forked.add(s);
        }
    }
}
