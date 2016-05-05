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
import java.util.LinkedList;
import java.util.List;



public class IMeter extends Widget {
    private static final Resource ponysfx = Resource.local().loadwait("sfx/alarmpony");
    private static final Resource stomachsfx = Resource.local().loadwait("sfx/alarmHungry");
    static Coord off = new Coord(22, 7);
    static Coord fsz = new Coord(101, 24);
    static Coord msz = new Coord(75, 10);
    Indir<Resource> bg;
    List<Meter> meters;
    private boolean ponyalarm = true;
    private boolean hungeralarm = true;

    @RName("im")
    public static class $_ implements Factory {
        public Widget create(Widget parent, Object[] args) {
            Indir<Resource> bg = parent.ui.sess.getres((Integer) args[0]);
            List<Meter> meters = new LinkedList<Meter>();
            for (int i = 1; i < args.length; i += 2)
                meters.add(new Meter((Color) args[i], (Integer) args[i + 1]));
            return (new IMeter(bg, meters));
        }
    }

    public IMeter(Indir<Resource> bg, List<Meter> meters) {
        super(fsz);
        this.bg = bg;
        this.meters = meters;
    }

    public static class Meter {
        Color c;
        public int a;

        public Meter(Color c, int a) {
            this.c = c;
            this.a = a;
        }
    }

    public void draw(GOut g) {
        try {
            Tex bg = this.bg.get().layer(Resource.imgc).tex();
            g.chcolor(0, 0, 0, 255);
            g.frect(off, msz);
            g.chcolor();
            for (Meter m : meters) {
                int w = msz.x;
                w = (w * m.a) / 100;
                g.chcolor(m.c);
                g.frect(off, new Coord(w, msz.y));
            }
            g.chcolor();
            g.image(bg, Coord.z);
        } catch (Loading l) {
        }
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "set") {
            List<Meter> meters = new LinkedList<Meter>();
            for (int i = 0; i < args.length; i += 2)
                meters.add(new Meter((Color) args[i], (Integer) args[i + 1]));
            this.meters = meters;

            // Hunger alarm
            try {
                Resource res = bg.get();
                if (res != null && res.name.equals("gfx/hud/meter/nrj")) {
                    if (meters.get(0).a <= 20) {
                    	if(hungeralarm) {
                        Audio.play(stomachsfx, 1.0);
                        Window updwnd = new HungerWnd();
                        HavenPanel.lui.root.add(updwnd);
                        updwnd.show();
                        updwnd.raise();
                        hungeralarm = false;
                    	}
                    } else
                    	hungeralarm = true;
                }
            } catch (Loading e) {
            }
            
            if (Config.ponyalarm && ponyalarm) {
                try {
                    Resource res = bg.get();
                    if (res != null && res.name.equals("gfx/hud/meter/mount")) {
                        if (meters.get(0).a <= 10) {
                            Audio.play(ponysfx, Config.ponyalarmvol);
                            ponyalarm = false;
                        }
                    }
                } catch (Loading e) {
                }
            }
        } else {
            super.uimsg(msg, args);
        }
    }
}