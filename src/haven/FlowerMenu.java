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
import static java.lang.Math.PI;

public class FlowerMenu extends Widget {
    public static final Color pink = new Color(255, 0, 128);
    public static final Color ptc = Color.YELLOW;
    public static final Text.Foundry ptf = new Text.Foundry(Text.dfont, 12);
    public static final IBox pbox = Window.wbox;
    public static final Tex pbg = Window.bg;
    public static final int ph = 30, ppl = 8;
    public Petal[] opts;
    private UI.Grab mg, kg;
    
    @RName("sm")
    public static class $_ implements Factory {
        public Widget create(Widget parent, Object[] args) {
            String[] opts = new String[args.length];
            for (int i = 0; i < args.length; i++)
                opts[i] = (String) args[i];
            return (new FlowerMenu(opts));
        }
    }
 
    public class Petal extends Widget {
        public String name;
        public double ta, tr;
        public int num;
        private Text text;
        private double a = 1;

        public Petal(String name) {
            super(Coord.z);
            this.name = name;
            text = ptf.render(name, ptc);
            resize(text.sz().x + 25, ph);
        }

        public void move(Coord c) {
            this.c = c.sub(sz.div(2));
        }

        public void move(double a, double r) {
            move(Coord.sc(a, r));
        }

        public void draw(GOut g) {
            g.chcolor(new Color(255, 255, 255, (int) (255 * a)));
            g.image(pbg, new Coord(3, 3), new Coord(3, 3), sz.add(new Coord(-6, -6)));
            pbox.draw(g, Coord.z, sz);
            g.image(text.tex(), sz.div(2).sub(text.sz().div(2)));
        }

        public boolean mousedown(Coord c, int button) {
            choose(this);
            return (true);
        }

        public Area ta(Coord tc) {
            return (Area.sized(tc.sub(sz.div(2)), sz));
        }

        public Area ta(double a, double r) {
            return (ta(Coord.sc(a, r)));
        }
    }
    
    public class Opening extends NormAnim {
        Opening() {
            super(0.15);
        }

        public void ntick(double s) {
            Petal pick = null;
            Petal harvest = null;
            Petal eat = null;
            Petal split = null;
            Petal study = null;
            for (Petal p : opts) {
                p.move(p.ta + ((1 - s) * PI), p.tr * s);
                p.a = s;
                if (p.name.equals(Resource.getLocString(Resource.l10nFlower, "Pick")))
                    pick = p;
                else if (p.name.equals(Resource.getLocString(Resource.l10nFlower, "Harvest")))
                    harvest = p;
                else if (p.name.equals(Resource.getLocString(Resource.l10nFlower, "Eat")))
                    eat = p;
                else if (p.name.equals(Resource.getLocString(Resource.l10nFlower, "Split")))
                    split = p;
                else if (p.name.equals("Study"))
                	study = p;
            }
            if (Config.autopick && pick != null && s == 1.0)
                choose(pick);
            else if (Config.autoharvest && harvest != null && s == 1.0)
                choose(harvest);
            else if (Config.autoeat && eat != null && s == 1.0 && study == null)
                choose(eat);
            else if (Config.autosplit && split != null && s == 1.0)
                choose(split);
        }
    }
    
    public class OpeningFast extends NormAnim {
        OpeningFast() {
            super(0.0);
        }

        public void ntick(double s) {
            Petal pick = null;
            for (Petal p : opts) {
                p.move(p.ta + ((1 - s) * PI), p.tr * s);
                p.a = s;
                if (p.name.equals("Pick"))
                    pick = p;
            }
            if (Config.autopick && pick != null && s == 1.0)
                choose(pick);
        }
    }

    public class Chosen extends NormAnim {
        Petal chosen;

        Chosen(Petal c) {
            super(0.75);
            chosen = c;
        }

        public void ntick(double s) {
            for (Petal p : opts) {
                if (p == chosen) {
                    if (s > 0.6) {
                        p.a = 1 - ((s - 0.6) / 0.4);
                    } else if (s < 0.3) {
                        p.move(p.ta, p.tr * (1 - (s / 0.3)));
                    }
                } else {
                    if (s > 0.3)
                        p.a = 0;
                    else
                        p.a = 1 - (s / 0.3);
                }
            }
            if (s == 1.0)
                ui.destroy(FlowerMenu.this);
        }
    }

    public class Cancel extends NormAnim {
        Cancel() {
            super(0.20);
        }

        public void ntick(double s) {
            for (Petal p : opts) {
                p.move(p.ta + ((s) * PI), p.tr * (1 - s));
                p.a = 1 - s;
            }
            if (s == 1.0)
                ui.destroy(FlowerMenu.this);
        }
    }
    
    public class CancelFast extends NormAnim {
        CancelFast() {
            super(0.0);
        }

        public void ntick(double s) {
            for (Petal p : opts) {
                p.move(p.ta + ((s) * PI), p.tr * (1 - s));
                p.a = 1 - s;
            }
            if (s == 1.0)
                ui.destroy(FlowerMenu.this);
        }
    }

    private void organize(Petal[] opts) {
        Area bounds = parent.area().xl(c.inv());
        int l = 1, p = 0, i = 0, mp = 0, ml = 1, t = 0, tt = -1;
        boolean muri = false;
        while (i < opts.length) {
            place:
            {
                double ta = (PI / 2) - (p * (2 * PI / (l * ppl)));
                double tr = 75 + (50 * (l - 1));
                if (!muri && !bounds.contains(opts[i].ta(ta, tr))) {
                    if (tt < 0) {
                        tt = ppl * l;
                        t = 1;
                        mp = p;
                        ml = l;
                    } else if (++t >= tt) {
                        muri = true;
                        p = mp;
                        l = ml;
                        continue;
                    }
                    break place;
                }
                tt = -1;
                opts[i].ta = ta;
                opts[i].tr = tr;
                i++;
            }
            if (++p >= (ppl * l)) {
                l++;
                p = 0;
            }
        }
    }

    public FlowerMenu(String... options) {
        super(Coord.z);
        opts = new Petal[options.length];
        for (int i = 0; i < options.length; i++) {
            String name = options[i];
            if (Resource.L10N_DEBUG &&
                    !name.startsWith("Follow ") && !name.startsWith("Travel along") &&
                    !name.startsWith("Extend ") && !name.startsWith("Connect "))
                Resource.l10nFlower = Resource.saveStrings(Resource.BUNDLE_FLOWER, Resource.l10nFlower, name, name);

            String locName = null;
            if (!Resource.language.equals("en") || Resource.L10N_DEBUG) {
                if (Resource.l10nFlower != null && Resource.l10nFlower.containsKey(name))
                    locName = Resource.l10nFlower.get(name);
            }
            add(opts[i] = new Petal(locName != null ? locName : name));
            opts[i].num = i;
        }
    }

    protected void added() {
        if (c.equals(-1, -1))
            c = parent.ui.lcc;
        mg = ui.grabmouse(this);
        kg = ui.grabkeys(this);
        organize(opts);
        if (Config.fastflower)
        new OpeningFast(); 
        else 
        new Opening();
    }

    public boolean mousedown(Coord c, int button) {
        if (!anims.isEmpty())
            return (true);
        if (!super.mousedown(c, button))
            choose(null);
        return (true);
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "cancel") {
        	if (Config.fastflower)
        		new CancelFast();
        	else
        		new Cancel();
            mg.remove();
            kg.remove();
        } else if (msg == "act") {
            new Chosen(opts[(Integer) args[0]]);
            mg.remove();
            kg.remove();
        }
    }

    public void draw(GOut g) {
        super.draw(g, false);
    }

    public boolean keydown(java.awt.event.KeyEvent ev) {
        return (true);
    }

    public boolean type(char key, java.awt.event.KeyEvent ev) {
        if (Config.userazerty)
            key = Utils.azerty2qwerty(key);

        if ((key >= '0') && (key <= '9')) {
            int opt = (key == '0') ? 10 : (key - '1');
            if (opt < opts.length) {
                choose(opts[opt]);
                kg.remove();
            }
            return (true);
        } else if (key == 27) {
            choose(null);
            kg.remove();
            return (true);
        }
        return (false);
    }

    public void choose(Petal option) {
        if (option == null) {
            wdgmsg("cl", -1);
        } else {
            wdgmsg("cl", option.num, ui.modflags());
        }
    }
}
