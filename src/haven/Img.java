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

public class Img extends Widget {
    private Indir<Resource> res;
    private Tex img;
    public boolean hit = false;

    @RName("img")
    public static class $_ implements Factory {
        public Widget create(Widget parent, Object[] args) {
            Indir<Resource> res;
            int a = 0;
            if (args[a] instanceof String) {
                String nm = (String) args[a++];
                int ver = (args.length > a) ? ((Integer) args[a++]) : -1;
                res = new Resource.Spec(Resource.remote(), nm, ver);
            } else {
                res = parent.ui.sess.getres((Integer) args[a++]);
            }
            Img ret = new Img(res);
            if (args.length > a)
                ret.hit = (Integer) args[a++] != 0;
            return (ret);
        }
    }

    public void draw(GOut g) {
        if (res != null) {
            try {
                img = res.get().layer(Resource.imgc).tex();
                resize(img.sz());
                res = null;
            } catch (Loading e) {
            }
        }
        if (img != null)
            g.image(img, Coord.z);
    }

    public Img(Tex img) {
        super(img.sz());
        this.res = null;
        this.img = img;
    }

    public Img(Indir<Resource> res) {
        super(Coord.z);
        this.res = res;
        this.img = null;
    }

    public void uimsg(String name, Object... args) {
        if (name == "ch") {
            if (args[0] instanceof String) {
                String nm = (String) args[0];
                int ver = (args.length > 1) ? ((Integer) args[1]) : -1;
                this.res = new Resource.Spec(Resource.remote(), nm, ver);
            } else {
                this.res = ui.sess.getres((Integer) args[0]);
            }
        } else {
            super.uimsg(name, args);
        }
    }

    public boolean mousedown(Coord c, int button) {
        if (hit) {
            wdgmsg("click", c, button, ui.modflags());
            return (true);
        }
        return (false);
    }
}
