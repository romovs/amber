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

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.media.opengl.GL2;

public class BufferBGL extends BGL {
    private Command[] list;
    private int n = 0;

    public BufferBGL(int c) {
        list = new Command[c];
    }

    public BufferBGL() {
        this(128);
    }

    public void run(GL2 gl) {
        for (int i = 0; i < n; i++) {
            try {
                list[i].run(gl);
            } catch (Exception exc) {
                // FIXME
                // haven't investigated yet why this fails with assortment of exceptions
                // so for now at least don't crash the clinet
                // throw (new BGLException(this, list[i], exc));
            }
        }
    }

    protected void add(Command cmd) {
        if (n >= list.length)
            list = Utils.extend(list, list.length * 2);
        list[n++] = cmd;
    }

    protected Iterable<Command> dump() {
        return (new Iterable<Command>() {
            public Iterator<Command> iterator() {
                return (new Iterator<Command>() {
                    int i = 0;

                    public boolean hasNext() {
                        return (i < n);
                    }

                    public Command next() {
                        if (i < n)
                            return (list[i++]);
                        throw (new NoSuchElementException());
                    }

                    public void remove() {
                        throw (new UnsupportedOperationException());
                    }
                });
            }
        });
    }
}
