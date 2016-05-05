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

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ResCache {
    public OutputStream store(String name) throws IOException;

    public InputStream fetch(String name) throws IOException;

    public static ResCache global = StupidJavaCodeContainer.makeglobal();

    public static class StupidJavaCodeContainer {
        private static ResCache makeglobal() {
            ResCache ret;
            if (Config.fscache) {
                if ((ret = FileCache.foruser()) != null)
                    return (ret);
            }
            return (null);
        }
    }

    public static class TestCache implements ResCache {
        public OutputStream store(final String name) {
            return (new ByteArrayOutputStream() {
                public void close() {
                    byte[] res = toByteArray();
                    System.out.println(name + ": " + res.length);
                }
            });
        }

        public InputStream fetch(String name) throws IOException {
            throw (new FileNotFoundException());
        }
    }
}
