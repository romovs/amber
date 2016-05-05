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
import java.io.Reader;

public class PeekReader extends Reader {
    private final Reader back;
    private boolean p = false;
    private int la;

    public PeekReader(Reader back) {
        this.back = back;
    }

    public void close() throws IOException {
        back.close();
    }

    public int read() throws IOException {
        if (p) {
            p = false;
            return (la);
        } else {
            return (back.read());
        }
    }

    public int read(char[] b, int off, int len) throws IOException {
        int r = 0;
        while (r < len) {
            int c = read();
            if (c < 0)
                return (r);
            b[off + r++] = (char) c;
        }
        return (r);
    }

    public boolean ready() throws IOException {
        if (p)
            return (true);
        return (back.ready());
    }

    protected boolean whitespace(char c) {
        return (Character.isWhitespace(c));
    }

    public int peek(boolean skipws) throws IOException {
        while (!p || (skipws && (la >= 0) && whitespace((char) la))) {
            la = back.read();
            p = true;
        }
        return (la);
    }

    public int peek() throws IOException {
        return (peek(false));
    }
}
