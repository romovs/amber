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

public class ISBox extends Widget implements DTarget {
    static Tex bg = Resource.loadtex("gfx/hud/bosq");
    static Text.Foundry lf;
    private Indir<Resource> res;
    private Text label;

    static {
        lf = new Text.Foundry(Text.sans, 22, java.awt.Color.WHITE);
        lf.aa = true;
    }

    @RName("isbox")
    public static class $_ implements Factory {
        public Widget create(Widget parent, Object[] args) {
            Indir<Resource> res;
            if (args[0] instanceof String)
                res = Resource.remote().load((String) args[0]);
            else
                res = parent.ui.sess.getres((Integer) args[0]);
            return (new ISBox(res, (Integer) args[1], (Integer) args[2], (Integer) args[3]));
        }
    }

    private void setlabel(int rem, int av, int bi) {
        if (bi < 0)
            label = lf.renderf("%d/%d", rem, av);
        else
            label = lf.renderf("%d/%d/%d", rem, av, bi);
    }

    public ISBox(Indir<Resource> res, int rem, int av, int bi) {
        super(bg.sz());
        this.res = res;
        setlabel(rem, av, bi);
    }

    public void draw(GOut g) {
        g.image(bg, Coord.z);
        try {
            Tex t = res.get().layer(Resource.imgc).tex();
            Coord dc = new Coord(6, (bg.sz().y / 2) - (t.sz().y / 2));
            g.image(t, dc);
        } catch (Loading e) {
        }
        g.image(label.tex(), new Coord(40, (bg.sz().y / 2) - (label.tex().sz().y / 2)));
    }

    public Object tooltip(Coord c, Widget prev) {
        try {
            if (res.get().layer(Resource.tooltip) != null)
                return (res.get().layer(Resource.tooltip).t);
        } catch (Loading e) {
        }
        return (null);
    }

    public boolean mousedown(Coord c, int button) {
        if (button == 1) {
            if (ui.modshift)
                wdgmsg("xfer");
            else
                wdgmsg("click");
            return (true);
        } else if (button == 3) {
            if (ui.modshift) {
                // we assume inv max size is 7*8.
                for (int i = 0; i < 7*8; i++)
                    wdgmsg("xfer");
                return true;
            }
        }
        return (false);
    }

    public boolean mousewheel(Coord c, int amount) {
        if (amount < 0)
            wdgmsg("xfer2", -1, ui.modflags());
        if (amount > 0)
            wdgmsg("xfer2", 1, ui.modflags());
        return (true);
    }

    public boolean drop(Coord cc, Coord ul) {
        wdgmsg("drop");
        return (true);
    }

    public boolean iteminteract(Coord cc, Coord ul) {
        wdgmsg("iact");
        return (true);
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "chnum") {
            setlabel((Integer) args[0], (Integer) args[1], (Integer) args[2]);
        } else {
            super.uimsg(msg, args);
        }
    }

    public int getfreespace() {
        if (label == null || label.text == null)
            return Integer.MAX_VALUE;
        int sep = label.text.indexOf('/');
        if (sep > 0) {
            String count = label.text.substring(0, sep);
            String capacity = label.text.substring(sep + 1);
            try {
                return Integer.parseInt(capacity) - Integer.parseInt(count);
            } catch (NumberFormatException nfe) {
            }
        }
        return Integer.MAX_VALUE;
    }
}
