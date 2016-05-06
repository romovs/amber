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

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Charlist extends Widget {
    public static final Tex bg = Resource.loadtex("gfx/hud/avakort");
    public static final int margin = 6;
    public int height, y, sel = 0;
    public List<Char> chars = new ArrayList<Char>();
    public static Charlist instance;

    public static class Char {
        static Text.Foundry tf = new Text.Foundry(Text.serif, 20);
        public String name;
        Text nt;
        // Avaview ava;
        Button plb;

        public Char(String name) {
            this.name = name;
            nt = tf.render(name);
        }
    }

    @RName("charlist")
    public static class $_ implements Factory {
        public Widget create(Widget parent, Object[] args) {
            return (new Charlist((Integer) args[0]));
        }
    }

    public Charlist(int height) {
        super(new Coord(bg.sz().x, 40 + (bg.sz().y * height) + (margin * (height - 1))));
        this.height = height;
        y = 0;
        setcanfocus(true);
        instance = this;
    }

    protected void added() {
        parent.setfocus(this);
    }

    public void scroll(int amount) {
        y += amount;
        synchronized (chars) {
            if (y > chars.size() - height)
                y = chars.size() - height;
        }
        if (y < 0)
            y = 0;
    }

    public void draw(GOut g) {
        int y = 20;
        synchronized (chars) {
            for (Char c : chars) {
                // c.ava.hide();
                c.plb.hide();
            }
            for (int i = 0; (i < height) && (i + this.y < chars.size()); i++) {
                boolean sel = (i + this.y) == this.sel;
                Char c = chars.get(i + this.y);
                if (hasfocus && sel) {
                    g.chcolor(255, 255, 128, 255);
                    g.image(bg, new Coord(0, y));
                    g.chcolor();
                } else {
                    g.image(bg, new Coord(0, y));
                }
                // c.ava.show();
                c.plb.show();
                // int off = (bg.sz().y - c.ava.sz.y) / 2;
                // c.ava.c = new Coord(off, off + y);
                c.plb.c = bg.sz().add(-10, y - 2).sub(c.plb.sz);
                // g.image(c.nt.tex(), new Coord(off + c.ava.sz.x + 5, off + y));
                g.image(c.nt.tex(), new Coord(5, 5 + y));
                y += bg.sz().y + margin;
            }
        }
        super.draw(g);
    }

    public boolean mousewheel(Coord c, int amount) {
        scroll(amount);
        return (true);
    }
    
    public static void choose_player(String name) {
		instance.wdgmsg("play", name);
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender instanceof Button) {
            synchronized (chars) {
                for (Char c : chars) {
                    if (sender == c.plb)
                        wdgmsg("play", c.name);
                }
            }
        } else if (sender instanceof Avaview) {
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "add") {
            Char c = new Char((String) args[0]);
            List<Indir<Resource>> resl = new LinkedList<Indir<Resource>>();
            for (int i = 1; i < args.length; i++)
                resl.add(ui.sess.getres((Integer) args[i]));
            // c.ava = new Avaview(new Coord(0, 0), this, resl);
            // c.ava.hide();
            c.plb = add(new Button(100, "Play"));
            c.plb.hide();
            synchronized (chars) {
                chars.add(c);
            }
        }
    }

    public boolean keydown(java.awt.event.KeyEvent ev) {
        if (ev.getKeyCode() == KeyEvent.VK_UP) {
            sel = Math.max(sel - 1, 0);
            return (true);
        } else if (ev.getKeyCode() == KeyEvent.VK_DOWN) {
            sel = Math.min(sel + 1, chars.size() - 1);
            return (true);
        } else if (ev.getKeyCode() == KeyEvent.VK_ENTER) {
            if ((sel >= 0) && (sel < chars.size())) {
                chars.get(sel).plb.click();
            }
            return (true);
        }
        return (false);
    }
}
