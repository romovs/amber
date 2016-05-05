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

package haven.rs;

import javax.media.opengl.GL;

import haven.Coord;
import haven.GLState;
import haven.GOut;
import haven.PView;
import haven.RenderList;
import haven.Rendered;

public class BufView {
    public final GBuffer buf;
    public RenderList rls;
    public GLState basicstate;
    private final RenderState rstate = new RenderState();

    public BufView(GBuffer buf, GLState basic) {
        this.buf = buf;
        this.basicstate = basic;
    }

    private class RenderState extends PView.RenderState {
        public Coord ul() {
            return (Coord.z);
        }

        public Coord sz() {
            return (buf.sz);
        }
    }

    protected GLState.Buffer basic(GOut g) {
        GLState.Buffer buf = g.basicstate();
        rstate.prep(buf);
        if (basicstate != null)
            basicstate.prep(buf);
        return (buf);
    }

    public void clear2d(GOut g, java.awt.Color cc) {
        g.state2d();
        g.apply();
        g.gl.glClearColor((float) cc.getRed() / 255f, (float) cc.getGreen() / 255f, (float) cc.getBlue() / 255f, (float) cc.getAlpha() / 255f);
        g.gl.glClear(GL.GL_COLOR_BUFFER_BIT);
    }

    protected void clear(GOut g) {
        g.gl.glClearColor(0, 0, 0, 0);
        g.gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);
    }

    public void render(Rendered root, GOut g) {
        if ((rls == null) || (rls.cfg != g.gc)) {
            rls = new RenderList(g.gc);
            rls.ignload = false;
        }
        GLState.Buffer bk = g.st.copy();
        try {
            GLState.Buffer def = basic(g);
            rls.setup(root, def);
            rls.fin();
            g.st.set(def);
            g.apply();
            clear(g);
            rls.render(g);
        } finally {
            g.st.set(bk);
        }
    }
}
