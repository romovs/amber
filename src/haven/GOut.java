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

import java.awt.Color;
import java.awt.image.*;
import javax.media.opengl.*;
import java.nio.*;

public class GOut {
    public final BGL gl;
    public final GLConfig gc;
    public Coord ul, sz, tx;
    public final CurrentGL curgl;
    private final GOut root;
    public final GLState.Applier st;
    private final GLState.Buffer def2d, cur2d;

    protected GOut(GOut o) {
        this.gl = o.gl;
        this.gc = o.gc;
        this.ul = o.ul;
        this.sz = o.sz;
        this.tx = o.tx;
        this.curgl = o.curgl;
        this.root = o.root;
        this.st = o.st;
        this.def2d = o.def2d;
        this.cur2d = new GLState.Buffer(gc);
        defstate();
    }

    public GOut(BGL gl, CurrentGL curgl, GLConfig cfg, GLState.Applier st, GLState.Buffer def2d, Coord sz) {
        this.gl = gl;
        this.gc = cfg;
        this.ul = this.tx = Coord.z;
        this.sz = sz;
        this.curgl = curgl;
        this.st = st;
        this.root = this;
        this.def2d = def2d;
        this.cur2d = new GLState.Buffer(gc);
        defstate();
    }

    @SuppressWarnings("serial")
	public static class GLException extends RuntimeException {
        public int code;
        public String str;
        private static javax.media.opengl.glu.GLU glu = new javax.media.opengl.glu.GLU();

        public GLException(int code) {
            super("GL Error: " + code + " (" + glu.gluErrorString(code) + ")");
            this.code = code;
            this.str = glu.gluErrorString(code);
        }

        public static String constname(Class<?> cl, int val) {
            String ret = null;
            for (java.lang.reflect.Field f : cl.getFields()) {
                if (((f.getModifiers() & java.lang.reflect.Modifier.STATIC) != 0) &&
                        ((f.getModifiers() & java.lang.reflect.Modifier.PUBLIC) != 0) &&
                        (f.getType() == Integer.TYPE)) {
                    int v;
                    try {
                        v = f.getInt(null);
                    } catch (IllegalAccessException e) {
                        continue;
                    }
                    if (v == val) {
                        if (ret == null)
                            ret = f.getName();
                        else
                            ret = ret + " or " + f.getName();
                    }
                }
            }
            if (ret == null)
                return (Integer.toString(val));
            return (ret);
        }

        public static String constname(int val) {
            return (constname(GL2.class, val));
        }
    }

    @SuppressWarnings("serial")
	public static class GLInvalidEnumException extends GLException {
        public GLInvalidEnumException() {
            super(GL.GL_INVALID_ENUM);
        }
    }

    @SuppressWarnings("serial")
	public static class GLInvalidValueException extends GLException {
        public GLInvalidValueException() {
            super(GL.GL_INVALID_VALUE);
        }
    }

    @SuppressWarnings("serial")
	public static class GLInvalidOperationException extends GLException {
        public GLInvalidOperationException() {
            super(GL.GL_INVALID_OPERATION);
        }
    }

    @SuppressWarnings("serial")
	public static class GLOutOfMemoryException extends GLException {
        public GLOutOfMemoryException() {
            super(GL.GL_OUT_OF_MEMORY);
        }
    }

    public static GLException glexcfor(int code) {
        switch (code) {
            case GL.GL_INVALID_ENUM:
                return (new GLInvalidEnumException());
            case GL.GL_INVALID_VALUE:
                return (new GLInvalidValueException());
            case GL.GL_INVALID_OPERATION:
                return (new GLInvalidOperationException());
            case GL.GL_OUT_OF_MEMORY:
                return (new GLOutOfMemoryException());
            default:
                return (new GLException(code));
        }
    }

    public static void checkerr(GL gl) {
        int err = gl.glGetError();
        if (err != 0)
            throw (glexcfor(err));
    }

    public static void checkerr(BGL gl) {
        gl.bglCheckErr();
    }

    private void checkerr() {
        checkerr(gl);
    }

    public GOut root() {
        return (root);
    }

    public GLState.Buffer basicstate() {
        return (def2d.copy());
    }

    public void image(BufferedImage img, Coord c) {
        if (img == null)
            return;
        Tex tex = new TexI(img);
        image(tex, c);
        tex.dispose();
    }

    public void image(Resource.Image img, Coord c) {
        if (img == null)
            return;
        image(img.tex(), c.add(img.o));
    }

    /* Draw texture at c, quite simply. */
    public void image(Tex tex, Coord c) {
        if (tex == null)
            return;
        st.set(cur2d);
        tex.crender(this, c.add(tx), ul, sz);
        checkerr();
    }

    public void image(Indir<Tex> tex, Coord c) {
        image(tex.get(), c);
    }

    public void aimage(Tex tex, Coord c, double ax, double ay) {
        Coord sz = tex.sz();
        image(tex, c.add((int) ((double) sz.x * -ax), (int) ((double) sz.y * -ay)));
    }

    /* Draw texture at c, scaling it to sz. */
    public void image(Tex tex, Coord c, Coord sz) {
        if (tex == null)
            return;
        st.set(cur2d);
        tex.crender(this, c.add(tx), ul, this.sz, sz);
        checkerr();
    }

    /* Draw texture at c, clipping everything outside ul to ul + sz. */
    public void image(Tex tex, Coord c, Coord ul, Coord sz) {
        if (tex == null)
            return;
        st.set(cur2d);
        ul = ul.add(this.tx);
        Coord br = ul.add(sz);
        if (ul.x < this.ul.x)
            ul.x = this.ul.x;
        if (ul.y < this.ul.y)
            ul.y = this.ul.y;
        if (br.x > this.ul.x + this.sz.x)
            br.x = this.ul.x + this.sz.x;
        if (br.y > this.ul.y + this.sz.y)
            br.y = this.ul.y + this.sz.y;
        tex.crender(this, c.add(this.tx), ul, br.sub(ul));
        checkerr();
    }

    public void rimagev(Tex tex, Coord c, int h) {
        Coord cc = new Coord(c);
        Coord sz = new Coord(tex.sz().x, h);
        for (; cc.y < c.y + h; cc.y += tex.sz().y)
            image(tex, cc, c, sz);
    }

    public void rimageh(Tex tex, Coord c, int w) {
        Coord cc = new Coord(c);
        Coord sz = new Coord(w, tex.sz().y);
        for (; cc.x < c.x + w; cc.x += tex.sz().x)
            image(tex, cc, c, sz);
    }

    public void rimage(Tex tex, Coord c, Coord sz) {
        Coord cc = new Coord();
        for (cc.y = c.y; cc.y < c.y + sz.y; cc.y += tex.sz().y) {
            for (cc.x = c.x; cc.x < c.x + sz.x; cc.x += tex.sz().x)
                image(tex, cc, c, sz);
        }
    }

    /* Draw texture at c, with the extra state s applied. */
    public void image(Tex tex, Coord c, GLState s) {
        st.set(cur2d);
        if (s != null)
            state(s);
        tex.crender(this, c.add(tx), ul, sz);
        checkerr();
    }

    public void vertex(Coord c) {
        gl.glVertex2i(c.x + tx.x, c.y + tx.y);
    }

    public void vertex(float x, float y) {
        gl.glVertex2f(x + tx.x, y + tx.y);
    }

    public void apply() {
        st.apply(this);
    }

    public void state(GLState st) {
        this.st.prep(st);
    }

    public void state2d() {
        st.set(cur2d);
    }

    public void line(Coord c1, Coord c2, double w) {
        st.set(cur2d);
        apply();
        gl.glLineWidth((float) w);
        gl.glBegin(GL.GL_LINES);
        vertex(c1);
        vertex(c2);
        gl.glEnd();
        checkerr();
    }

    public void text(String text, Coord c) {
        atext(text, c, 0, 0);
    }

    public void atext(String text, Coord c, double ax, double ay) {
        Text t = Text.render(text);
        Tex T = t.tex();
        Coord sz = t.sz();
        image(T, c.add((int) ((double) sz.x * -ax), (int) ((double) sz.y * -ay)));
        T.dispose();
        checkerr();
    }

    public void atextstroked(String text, Coord c, Color color, Color stroke) {
        Text t = Text.renderstroked(text, color, stroke);
        Tex T = t.tex();
        image(T, c);
        T.dispose();
        checkerr();
    }

    public void atextstroked(String text, Coord c, double ax, double ay, Color color, Color stroke) {
        Text t = Text.renderstroked(text, color, stroke);
        Tex T = t.tex();
        Coord sz = t.sz();
        image(T, c.add((int) ((double) sz.x * -ax), (int) ((double) sz.y * -ay)));
        T.dispose();
        checkerr();
    }

    public void atextstroked(String text, Coord c, Color color, Color stroke, Text.Foundry foundry) {
        Text t = Text.renderstroked(text, color, stroke, foundry);
        Tex T = t.tex();
        image(T, c);
        T.dispose();
        checkerr();
    }

    public void atextstroked(String text, int y, Color color, Color stroke, Text.Foundry foundry) {
        Text t = Text.renderstroked(text, color, stroke, foundry);
        Tex T = t.tex();
        image(T, new Coord(sz.x / 2 - t.sz().x / 2, y));
        T.dispose();
        checkerr();
    }

    public void poly(Coord... c) {
        st.set(cur2d);
        apply();
        gl.glBegin(GL2.GL_POLYGON);
        for (Coord vc : c)
            vertex(vc);
        gl.glEnd();
        checkerr();
    }

    public void poly2(Object... c) {
        st.set(cur2d);
        st.put(States.color, States.vertexcolor);
        apply();
        gl.glBegin(GL2.GL_POLYGON);
        for (int i = 0; i < c.length; i += 2) {
            Coord vc = (Coord) c[i];
            Color col = (Color) c[i + 1];
            gl.glColor4f((col.getRed() / 255.0f), (col.getGreen() / 255.0f), (col.getBlue() / 255.0f), (col.getAlpha() / 255.0f));
            vertex(vc);
        }
        gl.glEnd();
        checkerr();
    }

    public void frect(Coord ul, Coord sz) {
        ul = tx.add(ul);
        Coord br = ul.add(sz);
        if (ul.x < this.ul.x) ul.x = this.ul.x;
        if (ul.y < this.ul.y) ul.y = this.ul.y;
        if (br.x > this.ul.x + this.sz.x) br.x = this.ul.x + this.sz.x;
        if (br.y > this.ul.y + this.sz.y) br.y = this.ul.y + this.sz.y;
        if ((ul.x >= br.x) || (ul.y >= br.y))
            return;
        st.set(cur2d);
        apply();
        gl.glBegin(GL2.GL_QUADS);
        gl.glVertex2i(ul.x, ul.y);
        gl.glVertex2i(br.x, ul.y);
        gl.glVertex2i(br.x, br.y);
        gl.glVertex2i(ul.x, br.y);
        gl.glEnd();
        checkerr();
    }

    public void frect(Coord c1, Coord c2, Coord c3, Coord c4) {
        st.set(cur2d);
        apply();
        gl.glBegin(GL2.GL_QUADS);
        vertex(c1);
        vertex(c2);
        vertex(c3);
        vertex(c4);
        gl.glEnd();
        checkerr();
    }

    public void ftexrect(Coord ul, Coord sz, GLState s, float tl, float tt, float tr, float tb) {
        ul = tx.add(ul);
        Coord br = ul.add(sz);
        Coord ult = new Coord(0, 0);
        Coord brt = new Coord(sz);
        if (ul.x < this.ul.x) {
            ult.x += this.ul.x - ul.x;
            ul.x = this.ul.x;
        }
        if (ul.y < this.ul.y) {
            ult.y += this.ul.y - ul.y;
            ul.y = this.ul.y;
        }
        if (br.x > this.ul.x + this.sz.x) {
            brt.x -= br.x - (this.ul.x + this.sz.x);
            br.x = this.ul.x + this.sz.x;
        }
        if (br.y > this.ul.y + this.sz.y) {
            brt.y -= br.y - (this.ul.y + this.sz.y);
            br.y = this.ul.y + this.sz.y;
        }
        if ((ul.x >= br.x) || (ul.y >= br.y))
            return;

        st.set(cur2d);
        state(s);
        apply();

        float l = tl + ((tr - tl) * ((float) ult.x) / ((float) sz.x));
        float t = tt + ((tb - tt) * ((float) ult.y) / ((float) sz.y));
        float r = tl + ((tr - tl) * ((float) brt.x) / ((float) sz.x));
        float b = tt + ((tb - tt) * ((float) brt.y) / ((float) sz.y));
        gl.glBegin(GL2.GL_QUADS);
        gl.glTexCoord2f(l, b);
        gl.glVertex2i(ul.x, ul.y);
        gl.glTexCoord2f(r, b);
        gl.glVertex2i(br.x, ul.y);
        gl.glTexCoord2f(r, t);
        gl.glVertex2i(br.x, br.y);
        gl.glTexCoord2f(l, t);
        gl.glVertex2i(ul.x, br.y);
        gl.glEnd();
        checkerr();
    }

    public void ftexrect(Coord ul, Coord sz, GLState s) {
        ftexrect(ul, sz, s, 0, 0, 1, 1);
    }

    public void fellipse(Coord c, Coord r, int a1, int a2) {
        st.set(cur2d);
        apply();
        gl.glBegin(GL.GL_TRIANGLE_FAN);
        vertex(c);
        for (int i = a1; i <= a2; i += 5) {
            double a = (i * Math.PI * 2) / 360.0;
            vertex(c.add((int) (Math.cos(a) * r.x), -(int) (Math.sin(a) * r.y)));
        }
        gl.glEnd();
        checkerr();
    }

    public void fellipse(Coord c, Coord r) {
        fellipse(c, r, 0, 360);
    }

    public void rect(Coord ul, Coord sz) {
    	ul = tx.add(ul);
    	Coord br = ul.add(sz);
    	if(ul.x < this.ul.x) ul.x = this.ul.x;
    	if(ul.y < this.ul.y) ul.y = this.ul.y;
    	if(br.x > this.ul.x + this.sz.x) br.x = this.ul.x + this.sz.x;
    	if(br.y > this.ul.y + this.sz.y) br.y = this.ul.y + this.sz.y;
    	if((ul.x >= br.x) || (ul.y >= br.y))
    		return;
    	st.set(cur2d);
    	apply();
    	gl.glLineWidth(1);
    	gl.glBegin(GL.GL_LINE_LOOP);
    	gl.glVertex2i(ul.x, ul.y);
    	gl.glVertex2i(br.x, ul.y);
    	gl.glVertex2i(br.x, br.y);
    	gl.glVertex2i(ul.x, br.y);
    	gl.glEnd();
    	checkerr();
    }

    public void prect(Coord c, Coord ul, Coord br, double a) {
        st.set(cur2d);
        apply();
        gl.glEnable(GL2.GL_POLYGON_SMOOTH);
        gl.glBegin(GL.GL_TRIANGLE_FAN);
        vertex(c);
        vertex(c.add(0, ul.y));
        double p2 = Math.PI / 2;
        all:
        {
            float tc;

            tc = (float) (Math.tan(a) * -ul.y);
            if ((a > p2) || (tc > br.x)) {
                vertex(c.x + br.x, c.y + ul.y);
            } else {
                vertex(c.x + tc, c.y + ul.y);
                break all;
            }

            tc = (float) (Math.tan(a - (Math.PI / 2)) * br.x);
            if ((a > p2 * 2) || (tc > br.y)) {
                vertex(c.x + br.x, c.y + br.y);
            } else {
                vertex(c.x + br.x, c.y + tc);
                break all;
            }

            tc = (float) (-Math.tan(a - Math.PI) * br.y);
            if ((a > p2 * 3) || (tc < ul.x)) {
                vertex(c.x + ul.x, c.y + br.y);
            } else {
                vertex(c.x + tc, c.y + br.y);
                break all;
            }

            tc = (float) (-Math.tan(a - (3 * Math.PI / 2)) * -ul.x);
            if ((a > p2 * 4) || (tc < ul.y)) {
                vertex(c.x + ul.x, c.y + ul.y);
            } else {
                vertex(c.x + ul.x, c.y + tc);
                break all;
            }

            tc = (float) (Math.tan(a) * -ul.y);
            vertex(c.x + tc, c.y + ul.y);
        }
        gl.glEnd();
        gl.glDisable(GL2.GL_POLYGON_SMOOTH);
        checkerr();
    }

    public <T extends GLState> T curstate(GLState.Slot<T> slot) {
        return (cur2d.get(slot));
    }

    public void usestate(GLState st) {
        st.prep(cur2d);
    }

    public <T extends GLState> void usestate(GLState.Slot<? super T> slot) {
        cur2d.put(slot, null);
    }

    public void defstate() {
        def2d.copy(cur2d);
    }

    public void chcolor(Color c) {
        usestate(new States.ColState(c));
    }

    public void chcolor(int r, int g, int b, int a) {
        chcolor(Utils.clipcol(r, g, b, a));
    }

    public void chcolor() {
        usestate(States.color);
    }
    public void chcolor(Color c, int a) {
        chcolor(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    Color getcolor() {
        States.ColState color = curstate(States.color);
        return ((color == null) ? Color.WHITE : color.c);
    }

    public GOut reclip(Coord ul, Coord sz) {
        GOut g = new GOut(this);
        g.tx = this.tx.add(ul);
        g.ul = new Coord(g.tx);
        Coord gbr = g.ul.add(sz), tbr = this.ul.add(this.sz);
        if (g.ul.x < this.ul.x)
            g.ul.x = this.ul.x;
        if (g.ul.y < this.ul.y)
            g.ul.y = this.ul.y;
        if (gbr.x > tbr.x)
            gbr.x = tbr.x;
        if (gbr.y > tbr.y)
            gbr.y = tbr.y;
        g.sz = gbr.sub(g.ul);
        return (g);
    }

    public GOut reclipl(Coord ul, Coord sz) {
        GOut g = new GOut(this);
        g.tx = this.tx.add(ul);
        g.ul = new Coord(g.tx);
        g.sz = sz;
        return (g);
    }

    public void getpixel(final Coord c, final Callback<Color> cb) {
        gl.bglSubmit(new BGL.Request() {
            public void run(GL2 gl) {
                byte[] buf = new byte[4];
                gl.glReadPixels(c.x + tx.x, root.sz.y - c.y - tx.y, 1, 1, GL.GL_RGBA, GL2.GL_UNSIGNED_BYTE, ByteBuffer.wrap(buf));
                checkerr(gl);
                cb.done(new Color(((int) buf[0]) & 0xff, ((int) buf[1]) & 0xff, ((int) buf[2]) & 0xff));
            }
        });
    }

    public void getimage(final Coord ul, final Coord sz, final Callback<BufferedImage> cb) {
        gl.bglSubmit(new BGL.Request() {
            public void run(GL2 gl) {
                byte[] buf = new byte[sz.x * sz.y * 4];
                gl.glReadPixels(ul.x + tx.x, root.sz.y - ul.y - sz.y - tx.y, sz.x, sz.y, GL.GL_RGBA, GL2.GL_UNSIGNED_BYTE, ByteBuffer.wrap(buf));
                checkerr(gl);
                for (int y = 0; y < sz.y / 2; y++) {
                    int to = y * sz.x * 4, bo = (sz.y - y - 1) * sz.x * 4;
                    for (int o = 0; o < sz.x * 4; o++, to++, bo++) {
                        byte t = buf[to];
                        buf[to] = buf[bo];
                        buf[bo] = t;
                    }
                }
                WritableRaster raster = Raster.createInterleavedRaster(new DataBufferByte(buf, buf.length), sz.x, sz.y, 4 * sz.x, 4, new int[]{0, 1, 2, 3}, null);
                cb.done(new BufferedImage(TexI.glcm, raster, false, null));
            }
        });
    }

    public void getimage(final Callback<BufferedImage> cb) {
        getimage(Coord.z, sz, cb);
    }
    public void astext(String text, Coord c, double ax, double ay, Color ca, Color cb) {
        Text t = Text.renderstroked(text, ca, cb);
        Tex T = t.tex();
        Coord sz = t.sz();
        image(T, c.add((int)((double)sz.x * -ax), (int)((double)sz.y * -ay)));
        T.dispose();
        checkerr();
    }
}
