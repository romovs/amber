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

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class TextEntry extends SIWidget {
    public static final Color defcol = new Color(255, 205, 109), dirtycol = new Color(255, 232, 209);
    public static final Text.Foundry fnd = new Text.Foundry(Text.serif, Config.fontsizeglobal + 2).aa(true);
    public static final BufferedImage lcap = Resource.loadimg("gfx/hud/text/l");
    public static final BufferedImage rcap = Resource.loadimg("gfx/hud/text/r");
    public static final BufferedImage mext = Resource.loadimg("gfx/hud/text/m");
    public static final Tex caret = Resource.loadtex("gfx/hud/text/caret");
    public static final Coord toff = new Coord(lcap.getWidth() - 1, 3);
    public static final Coord coff = new Coord(-3, -1);
    public boolean dshow = false;
    public LineEdit buf;
    public int sx;
    public boolean pw = false;
    public String text;
    private boolean dirty = false;
    private long focusstart;
    private Text.Line tcache = null;

    @RName("text")
    public static class $_ implements Factory {
        public Widget create(Widget parent, Object[] args) {
            if (args[0] instanceof Coord)
                return (new TextEntry((Coord) args[0], (String) args[1]));
            else
                return (new TextEntry((Integer) args[0], (String) args[1]));
        }
    }

    public void settext(String text) {
        buf.setline(text);
        redraw();
    }

    public void rsettext(String text) {
        buf = new LineEdit(this.text = text) {
            protected void done(String line) {
                activate(line);
            }

            protected void changed() {
                redraw();
                TextEntry.this.text = line;
                TextEntry.this.changed();
            }
        };
        redraw();
    }

    public void commit() {
        dirty = false;
        redraw();
    }

    public void uimsg(String name, Object... args) {
        if (name == "settext") {
            settext((String) args[0]);
        } else if (name == "get") {
            wdgmsg("text", buf.line);
        } else if (name == "pw") {
            pw = ((Integer) args[0]) != 0;
        } else if (name == "dshow") {
            dshow = ((Integer) args[0]) != 0;
        } else if (name == "cmt") {
            commit();
        } else {
            super.uimsg(name, args);
        }
    }

    protected String dtext() {
        if (pw) {
            String ret = "";
            for (int i = 0; i < buf.line.length(); i++)
                ret += "\u2022";
            return (ret);
        } else {
            return (buf.line);
        }
    }

    public void draw(BufferedImage img) {
        Graphics g = img.getGraphics();
        String dtext = dtext();
        tcache = fnd.render(dtext, (dshow && dirty) ? dirtycol : defcol);
        g.drawImage(mext, 0, 0, sz.x, sz.y, null);

        int cx = tcache.advance(buf.point);
        if (cx < sx) sx = cx;
        if (cx > sx + (sz.x - 1)) sx = cx - (sz.x - 1);
        g.drawImage(tcache.img, toff.x - sx, toff.y, null);

        g.drawImage(lcap, 0, 0, null);
        g.drawImage(rcap, sz.x - rcap.getWidth(), 0, null);

        g.dispose();
    }

    public void draw(GOut g) {
        super.draw(g);
        if (hasfocus && (((System.currentTimeMillis() - focusstart) % 1000) < 500)) {
            int cx = tcache.advance(buf.point);
            int lx = cx - sx + 1;
            g.image(caret, toff.add(coff).add(lx, 0));
        }
    }

    public TextEntry(int w, String deftext) {
        super(new Coord(w, mext.getHeight()));
        rsettext(deftext);
        setcanfocus(true);
    }

    @Deprecated
    public TextEntry(Coord sz, String deftext) {
        this(sz.x, deftext);
    }

    protected void changed() {
        dirty = true;
    }

    public void activate(String text) {
        if (canactivate)
            wdgmsg("activate", text);
    }

    public boolean type(char c, KeyEvent ev) {
        return (buf.key(ev));
    }

    public boolean keydown(KeyEvent e) {
        buf.key(e);
        return (true);
    }

    public boolean mousedown(Coord c, int button) {
        parent.setfocus(this);
        if (tcache != null) {
            buf.point = tcache.charat(c.x + sx);
        }
        return (true);
    }

    public void gotfocus() {
        focusstart = System.currentTimeMillis();
    }

    public void resize(int w) {
        resize(w, sz.y);
    }
}
