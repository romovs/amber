/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Bjorn Johannessen <johannessen.bjorn@gmail.com>
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

public abstract class Listbox<T> extends ListWidget<T> {
    public final int h;
    public final Scrollbar sb;

    public Listbox(int w, int h, int itemh) {
	super(new Coord(w, h * itemh), itemh);
	this.h = h;
	this.sb = adda(new Scrollbar(sz.y, 0, 0), sz.x, 0, 1, 0);
    }

    protected void drawsel(GOut g) {
	g.chcolor(255, 255, 0, 128);
	g.frect(Coord.z, g.sz);
	g.chcolor();
    }

    protected void drawbg(GOut g) {
	g.chcolor(Color.BLACK);
	g.frect(Coord.z, sz);
	g.chcolor();
    }

    public void draw(GOut g) {
	sb.max = listitems() - h;
	drawbg(g);
	int n = listitems();
	for(int i = 0; i < h; i++) {
	    int idx = i + sb.val;
	    if(idx >= n)
		break;
	    T item = listitem(idx);
	    int w = sz.x - (sb.vis()?sb.sz.x:0);
	    GOut ig = g.reclip(new Coord(0, i * itemh), new Coord(w, itemh));
	    if(item == sel)
		drawsel(ig);
	    drawitem(ig, item, idx);
	}
	super.draw(g);
    }

    public boolean mousewheel(Coord c, int amount) {
	sb.ch(amount);
	return(true);
    }

    protected void itemclick(T item, int button) {
	if(button == 1)
	    change(item);
    }

    protected void itemactivate(T item) {}

    public T itemat(Coord c) {
	int idx = (c.y / itemh) + sb.val;
	if(idx >= listitems())
	    return(null);
	return(listitem(idx));
    }

    public boolean mousedown(Coord c, int button) {
	if(super.mousedown(c, button))
	    return(true);
	T item = itemat(c);
	if((item == null) && (button == 1))
	    change(null);
	else if(item != null)
	    itemclick(item, button);
	return(true);
    }

    public boolean mouseclick(Coord c, int button, int count) {
        if(super.mouseclick(c, button, count))
            return(true);
        T item = itemat(c);
        if(item != null && button == 1 && count >= 2)
            itemactivate(item);
        return(true);
    }

    // ensures that selected element is visible
    public void showsel() {
        if (sb.val + h - 1 < selindex)
            sb.val = Math.max(0, selindex - h + 1);
        if (sb.val > selindex)
            sb.val = Math.max(0, selindex);
    }
}