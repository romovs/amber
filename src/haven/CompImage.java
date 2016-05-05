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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.LinkedList;

public class CompImage {
    public Coord sz;
    private final Collection<Placed> cont = new LinkedList<Placed>();

    public interface Image {
        public void draw(Graphics g, Coord c);

        public Coord sz();
    }

    private static class Placed {
        Image img;
        Coord c;

        Placed(Image img, Coord c) {
            this.img = img;
            this.c = c;
        }
    }

    public CompImage() {
        sz = Coord.z;
    }

    public CompImage add(Image img, Coord c) {
        cont.add(new Placed(img, c));
        Coord imsz = img.sz();
        sz = new Coord(Math.max(sz.x, c.x + imsz.x),
                Math.max(sz.y, c.y + imsz.y));
        return (this);
    }

    public static Image mk(final BufferedImage img) {
        return (new Image() {
            public void draw(Graphics g, Coord c) {
                g.drawImage(img, c.x, c.y, null);
            }

            public Coord sz() {
                return (PUtils.imgsz(img));
            }
        });
    }

    public CompImage add(final BufferedImage img, Coord c) {
        add(mk(img), c);
        return (this);
    }

    public static Image mk(final CompImage img) {
        return (new Image() {
            public void draw(Graphics g, Coord c) {
                img.compose(g, c);
            }

            public Coord sz() {
                return (img.sz);
            }
        });
    }

    public CompImage add(final CompImage img, Coord c) {
        add(mk(img), c);
        return (this);
    }

    private void compose(Graphics on, Coord off) {
        for (Placed pl : cont)
            pl.img.draw(on, pl.c.add(off));
    }

    public BufferedImage compose() {
        BufferedImage ret = TexI.mkbuf(sz);
        Graphics g = ret.getGraphics();
        compose(g, Coord.z);
        g.dispose();
        return (ret);
    }

    public CompImage table(Coord base, Image[][] cells, Object cs, int rs, int[] cj) {
        int[] _cs = new int[cells.length];
        if (cs instanceof int[]) {
            int[] $cs = (int[]) cs;
            for (int i = 0; i < $cs.length; i++)
                _cs[i] = $cs[i];
        } else {
            for (int i = 0; i < _cs.length; i++)
                _cs[i] = ((Number) cs).intValue();
        }
        int[] w = new int[cells.length];
        for (int c = 0; c < cells.length; c++) {
            for (int r = 0; r < cells[c].length; r++)
                w[c] = Math.max(w[c], cells[c][r].sz().x);
        }
        int r = 0;
        int y = 0;
        while (true) {
            boolean a = false;
            int mh = 0;
            for (int c = 0, x = 0; c < cells.length; c++) {
                if (r >= cells[c].length)
                    continue;
                int j = (cj.length > c) ? cj[c] : 0;
                a = true;
                int cx = 0;
                if (j == 1)
                    cx = w[c] - cells[c][r].sz().x;
                else if (j == 2)
                    cx = (w[c] - cells[c][r].sz().x) / 2;
                add(cells[c][r], base.add(x + cx, y));
                x += w[c] + _cs[c];
                mh = Math.max(mh, cells[c][r].sz().y);
            }
            if (!a)
                break;
            r++;
            y += mh + rs;
        }
        return (this);
    }

    public static Image[][] transpose(Image[][] cells) {
        int w = 0;
        for (int r = 0; r < cells.length; r++)
            w = Math.max(w, cells[r].length);
        Image[][] ret = new Image[w][];
        for (int c = 0; c < w; c++) {
            ret[c] = new Image[cells.length];
            for (int r = 0; r < cells.length; r++) {
                ret[c][r] = cells[r][c];
            }
        }
        return (ret);
    }

    public static Image[][] transpose(Collection<Image[]> rows) {
        return (transpose(rows.toArray(new Image[0][])));
    }
}
