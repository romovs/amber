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

public class Speaking extends GAttrib {
    float zo;
    Text text;
    static IBox sb = null;
    Tex svans;
    static final int sx = 3;

    public Speaking(Gob gob, float zo, String text) {
        super(gob);
        if (sb == null)
            sb = new IBox("gfx/hud/emote", "tl", "tr", "bl", "br", "el", "er", "et", "eb");
        svans = Resource.loadtex("gfx/hud/emote/svans");
        this.zo = zo;
        this.text = Text.render(text, Color.BLACK);
    }

    public void update(String text) {
        this.text = Text.render(text, Color.BLACK);
    }

    @SuppressWarnings("deprecation")
	public void draw(GOut g, Coord c) {
        Coord sz = text.sz();
        if (sz.x < 10)
            sz.x = 10;
		Coord tl = c.add(new Coord(sx, sb.bsz().y + sz.y + svans.sz().y - 1).inv());
		Coord ftl = tl.add(sb.tloff());
        g.chcolor(Color.WHITE);
        g.frect(ftl, sz);
        sb.draw(g, tl, sz.add(sb.bsz()));
        g.chcolor(Color.BLACK);
        g.image(text.tex(), ftl);
        g.chcolor(Color.WHITE);
        g.image(svans, c.add(0, -svans.sz().y));
    }

    final PView.Draw2D fx = new PView.Draw2D() {
        public void draw2d(GOut g) {
            if (gob.sc != null)
                Speaking.this.draw(g, gob.sc.add(new Coord(gob.sczu.mul(zo))).add(3, 0));
        }
    };
}
