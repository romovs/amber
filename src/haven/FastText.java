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

public class FastText {
    public static final Text.Foundry fnd = new Text.Foundry(Text.sans, 10);
    private static final Tex[] ct = new Tex[225];

    private FastText() {
    }

    public static Tex ch(char c) {
        int i;
        if ((c < 32) || (c >= 256))
            i = 0;
        else
            i = c - 31;
        if (ct[i] == null)
            ct[i] = fnd.render(Character.toString(c)).tex();
        return (ct[i]);
    }

    public static int textw(String text) {
        int r = 0;
        for (int i = 0; i < text.length(); i++)
            r += ch(text.charAt(i)).sz().x;
        return (r);
    }

    public static void aprint(GOut g, Coord c, double ax, double ay, String text) {
        Coord lc = new Coord(c);
        if (ax > 0)
            lc.x -= textw(text) * ax;
        if (ay > 0)
            lc.y -= fnd.height() * ay;
        for (int i = 0; i < text.length(); i++) {
            Tex ch = ch(text.charAt(i));
            g.image(ch, lc);
            lc.x += ch.sz().x;
        }
    }

    public static void aprint(GOut g, Coord c, String text) {
        Coord lc = new Coord(c);
        lc.x -= textw(text) / 2;
        lc.y -= fnd.height();
        for (int i = 0; i < text.length(); i++) {
            Tex ch = ch(text.charAt(i));
            g.image(ch, lc);
            lc.x += ch.sz().x;
        }
    }

    public static void print(GOut g, Coord c, String text) {
        aprint(g, c, 0.0, 0.0, text);
    }

    public static void aprintf(GOut g, Coord c, double ax, double ay, String fmt, Object... args) {
        aprint(g, c, ax, ay, String.format(fmt, args));
    }

    public static void printf(GOut g, Coord c, String fmt, Object... args) {
        print(g, c, String.format(fmt, args));
    }
}
