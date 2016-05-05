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

public abstract class ListWidget<T> extends Widget {
    public final int itemh;
    public T sel;
    public int selindex;

    public ListWidget(Coord sz, int itemh) {
	super(sz);
	this.itemh = itemh;
    }

    protected abstract T listitem(int i);
    protected abstract int listitems();
    protected abstract void drawitem(GOut g, T item, int i);

    public void change(T item) {
        change(indexof(item));
    }

    public void change(int index) {
        int count = listitems();
        if (index >= 0 && index < count) {
            selindex = index;
            sel = listitem(index);
        } else {
            selindex = -1;
            sel = null;
        }
    }

    public int indexof(T item) {
        for (int i = 0; i < listitems(); i++)
            if (listitem(i) == item)
                return i;
        return -1;
    }

	public boolean mouseclick(Coord c, int button, int count) {
		return false;
	}
}