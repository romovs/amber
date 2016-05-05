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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.media.opengl.GL2;
import javax.media.opengl.GL3;

public class GPUProfile extends Profile {
    private Collection<Frame> waiting = new LinkedList<Frame>();

    public GPUProfile(int hl) {
        super(hl);
    }

    public class Frame extends Profile.Frame {
        private List<String> nw = new LinkedList<String>();
        private List<Integer> queries = new LinkedList<Integer>();
        private GL3 gl;

        public Frame(GL3 gl) {
            this.gl = gl;
            query();
        }

        private void query() {
            int[] buf = new int[1];
            gl.glGenQueries(1, buf, 0);
            gl.glQueryCounter(buf[0], GL3.GL_TIMESTAMP);
            GOut.checkerr(gl);
            queries.add(buf[0]);
        }

        public void tick(String nm) {
            query();
            nw.add(nm);
        }

        public void fin() {
            query();
            waiting.add(this);
            check();
        }

        public void fin2() {
            long[] tms = new long[queries.size()];
            {
                int i = 0;
                for (Iterator<Integer> qi = queries.iterator(); qi.hasNext(); i++) {
                    int[] qo = {qi.next()};
                    gl.glGetQueryObjecti64v(qo[0], GL2.GL_QUERY_RESULT, tms, i);
                    gl.glDeleteQueries(1, qo, 0);
                }
            }
            int np = tms.length - 2;
            double total = (tms[tms.length - 1] - tms[0]) / 1000000000.0;
            String[] nm = new String[np];
            double[] prt = new double[np];
            for (int i = 0; i < prt.length; i++) {
                nm[i] = nw.get(i);
                prt[i] = (tms[i + 1] - tms[i]) / 1000000000.0;
            }
            fin(total, nm, prt);
            nw = null;
            queries = null;
        }
    }

    public void check() {
        int[] rb = new int[1];
        for (Iterator<Frame> i = waiting.iterator(); i.hasNext(); ) {
            Frame f = i.next();
            for (int qo : f.queries) {
                f.gl.glGetQueryObjectiv(qo, GL2.GL_QUERY_RESULT_AVAILABLE, rb, 0);
                if (rb[0] == 0)
                    return;
            }
            f.fin2();
            i.remove();
        }
    }
}
