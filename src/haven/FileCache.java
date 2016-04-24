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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileCache implements ResCache {
    private final File base;

    public FileCache(File base) {
        this.base = base;
    }

    public static FileCache foruser() {
        try {
            String path = System.getProperty("user.home", null);
            if (path == null)
                return (null);
            File home = new File(path);
            if (!home.exists() || !home.isDirectory() || !home.canRead() || !home.canWrite())
                return (null);
            File base = new File(new File(new File(home, ".haven"), "hafen"), "cache");
            if (!base.exists() && !base.mkdirs())
                return (null);
            return (new FileCache(base));
        } catch (SecurityException e) {
            return (null);
        }
    }

    private File forres(String nm) {
        File res = base;
        String[] comp = nm.split("/");
        for (int i = 0; i < comp.length - 1; i++) {
            res = new File(res, comp[i]);
        }
        return (new File(res, comp[comp.length - 1] + ".cached"));
    }

    public OutputStream store(String name) throws IOException {
        final File nm = forres(name);
        File dir = nm.getParentFile();
        final File tmp = new File(dir, nm.getName() + ".new");
        dir.mkdirs();
        tmp.delete();
        OutputStream ret = new FilterOutputStream(new FileOutputStream(tmp)) {
            public void close() throws IOException {
                super.close();
                if (!tmp.renameTo(nm)) {
            /* Apparently Java doesn't support atomic
             * renames on Windows... :-/ */
                    nm.delete();
                    tmp.renameTo(nm);
                }
            }
        };
        return (ret);
    }

    public InputStream fetch(String name) throws IOException {
        return (new FileInputStream(forres(name)));
    }

    public String toString() {
        return ("FileCache(" + base + ")");
    }
}
