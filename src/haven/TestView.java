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

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL2;

public class TestView extends PView {
    static final FastMesh[] tmesh;

    static {
        Resource res = Resource.local().loadwait("gfx/borka/male");
        List<FastMesh> l = new ArrayList<FastMesh>();
        for (FastMesh.MeshRes m : res.layers(FastMesh.MeshRes.class))
            l.add(m.m);
        tmesh = l.toArray(new FastMesh[0]);
    }

    final PointedCam camera;
    int sel = -1;

    public TestView(Coord sz) {
        super(sz);
        camera = new PointedCam();
        camera.a = (float) Math.PI * 3 / 2;
        camera.e = (float) Math.PI / 2;
        setcanfocus(true);
    }

    protected Camera camera() {
        return (camera);
    }

    public static class Cube implements Rendered {
        public void draw(GOut g) {
            g.apply();
            BGL gl = g.gl;

            gl.glBegin(GL2.GL_QUADS);
            gl.glNormal3f(0.0f, 0.0f, 1.0f);
            gl.glColor3f(1.0f, 0.0f, 0.0f);
            gl.glVertex3f(-1.0f, 1.0f, 1.0f);
            gl.glVertex3f(-1.0f, -1.0f, 1.0f);
            gl.glVertex3f(1.0f, -1.0f, 1.0f);
            gl.glVertex3f(1.0f, 1.0f, 1.0f);

            gl.glNormal3f(1.0f, 0.0f, 0.0f);
            gl.glColor3f(0.0f, 1.0f, 0.0f);
            gl.glVertex3f(1.0f, 1.0f, 1.0f);
            gl.glVertex3f(1.0f, -1.0f, 1.0f);
            gl.glVertex3f(1.0f, -1.0f, -1.0f);
            gl.glVertex3f(1.0f, 1.0f, -1.0f);

            gl.glNormal3f(-1.0f, 0.0f, 0.0f);
            gl.glColor3f(0.0f, 0.0f, 1.0f);
            gl.glVertex3f(-1.0f, 1.0f, 1.0f);
            gl.glVertex3f(-1.0f, 1.0f, -1.0f);
            gl.glVertex3f(-1.0f, -1.0f, -1.0f);
            gl.glVertex3f(-1.0f, -1.0f, 1.0f);

            gl.glNormal3f(0.0f, 1.0f, 0.0f);
            gl.glColor3f(0.0f, 1.0f, 1.0f);
            gl.glVertex3f(-1.0f, 1.0f, 1.0f);
            gl.glVertex3f(1.0f, 1.0f, 1.0f);
            gl.glVertex3f(1.0f, 1.0f, -1.0f);
            gl.glVertex3f(-1.0f, 1.0f, -1.0f);

            gl.glNormal3f(0.0f, -1.0f, 0.0f);
            gl.glColor3f(1.0f, 0.0f, 1.0f);
            gl.glVertex3f(-1.0f, -1.0f, 1.0f);
            gl.glVertex3f(-1.0f, -1.0f, -1.0f);
            gl.glVertex3f(1.0f, -1.0f, -1.0f);
            gl.glVertex3f(1.0f, -1.0f, 1.0f);

            gl.glNormal3f(0.0f, 0.0f, -1.0f);
            gl.glColor3f(1.0f, 1.0f, 0.0f);
            gl.glVertex3f(-1.0f, 1.0f, -1.0f);
            gl.glVertex3f(1.0f, 1.0f, -1.0f);
            gl.glVertex3f(1.0f, -1.0f, -1.0f);
            gl.glVertex3f(-1.0f, -1.0f, -1.0f);
            gl.glEnd();
        }

        public boolean setup(RenderList rls) {
            rls.state().put(States.color, States.vertexcolor);
            return (true);
        }
    }

    protected void setup(RenderList rls) {
        int i = 0;
        for (FastMesh m : tmesh) {
            if ((sel == -1) || (i == sel))
                rls.add(m, null);
            i++;
        }
        rls.add(new Cube(), Location.xlate(new Coord3f(-1.5f, 0, 0)));
        rls.add(new Cube(), Location.xlate(new Coord3f(1.5f, 0, 0)));
    }

    public void mousemove(Coord c) {
        if (c.x < 0 || c.x >= sz.x || c.y < 0 || c.y >= sz.y)
            return;
        camera.e = (float) Math.PI / 2 * ((float) c.y / (float) sz.y);
        camera.a = (float) Math.PI * 2 * ((float) c.x / (float) sz.x);
    }

    public boolean mousewheel(Coord c, int amount) {
        float d = camera.dist + (amount * 5);
        if (d < 5)
            d = 5;
        camera.dist = d;
        return (true);
    }

    public boolean type(char key, java.awt.event.KeyEvent ev) {
        if (key == ' ') {
            sel = -1;
            return (true);
        } else if ((key >= '0') && (key < '0' + tmesh.length)) {
            sel = key - '0';
            return (true);
        }
        return (false);
    }
}
