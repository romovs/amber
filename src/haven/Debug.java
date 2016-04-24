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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

import javax.media.opengl.GL4bc;
import javax.media.opengl.TraceGL4bc;

public class Debug {
    public static boolean kf1, kf2, kf3, kf4;
    public static boolean pk1, pk2, pk3, pk4;
    public static PrintWriter log = new PrintWriter(System.err);

    public static void cycle() {
        pk1 = kf1;
        pk2 = kf2;
        pk3 = kf3;
        pk4 = kf4;
    }

    public static void dumpimage(BufferedImage img, String fn) {
        try {
            javax.imageio.ImageIO.write(img, "PNG", new File(fn));
        } catch (IOException e) {
            throw (new RuntimeException(e));
        }
    }

    public static class DumpGL extends TraceGL4bc {
        public final ByteArrayOutputStream buf;

        private DumpGL(GL4bc bk, ByteArrayOutputStream buf) {
            super(bk, new PrintStream(buf));
            this.buf = buf;
        }

        public DumpGL(GL4bc bk) {
            this(bk, new ByteArrayOutputStream());
        }

        public void reset() {
            buf.reset();
        }

        public void dump(String fn) {
            try {
                OutputStream out = new FileOutputStream(fn);
                try {
                    out.write(buf.toByteArray());
                } finally {
                    out.close();
                }
            } catch (IOException e) {
                throw (new RuntimeException(e));
            }
        }
    }

    static int dumpseq = 0;

    public static PrintWriter getdump() {
        try {
            return (new java.io.PrintWriter(new java.io.FileWriter("/tmp/dbdump-" + dumpseq++)));
        } catch (java.io.IOException e) {
            throw (new RuntimeException(e));
        }
    }
}
