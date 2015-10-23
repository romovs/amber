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

public class OptWnd extends Window {
    public final Panel main, video, audio, display, map, general, combat;
    public Panel current;

    public void chpanel(Panel p) {
        if (current != null)
            current.hide();
        (current = p).show();
    }

    public class PButton extends Button {
        public final Panel tgt;
        public final int key;

        public PButton(int w, String title, int key, Panel tgt) {
            super(w, title);
            this.tgt = tgt;
            this.key = key;
        }

        public void click() {
            chpanel(tgt);
        }

        public boolean type(char key, java.awt.event.KeyEvent ev) {
            if ((this.key != -1) && (key == this.key)) {
                click();
                return (true);
            }
            return (false);
        }
    }

    public class Panel extends Widget {
        public Panel() {
            visible = false;
            c = Coord.z;
        }
    }

    public class VideoPanel extends Panel {
        public VideoPanel(Panel back) {
            super();
            add(new PButton(200, "Back", 27, back), new Coord(270, 360));
            pack();
        }

        public class CPanel extends Widget {
            public final GLSettings cf;

            public CPanel(GLSettings gcf) {
                this.cf = gcf;
                int y = 0;
                add(new CheckBox("Per-fragment lighting") {
                    {
                        a = cf.flight.val;
                    }

                    public void set(boolean val) {
                        if (val) {
                            try {
                                cf.flight.set(true);
                            } catch (GLSettings.SettingException e) {
                                getparent(GameUI.class).error(e.getMessage());
                                return;
                            }
                        } else {
                            cf.flight.set(false);
                        }
                        a = val;
                        cf.dirty = true;
                    }
                }, new Coord(0, y));
                y += 25;
                add(new CheckBox("Render shadows") {
                    {
                        a = cf.lshadow.val;
                    }

                    public void set(boolean val) {
                        if (val) {
                            try {
                                cf.lshadow.set(true);
                            } catch (GLSettings.SettingException e) {
                                getparent(GameUI.class).error(e.getMessage());
                                return;
                            }
                        } else {
                            cf.lshadow.set(false);
                        }
                        a = val;
                        cf.dirty = true;
                    }
                }, new Coord(0, y));
                y += 25;
                add(new CheckBox("Antialiasing") {
                    {
                        a = cf.fsaa.val;
                    }

                    public void set(boolean val) {
                        try {
                            cf.fsaa.set(val);
                        } catch (GLSettings.SettingException e) {
                            getparent(GameUI.class).error(e.getMessage());
                            return;
                        }
                        a = val;
                        cf.dirty = true;
                    }
                }, new Coord(0, y));
                y += 25;
                add(new Label("Anisotropic filtering"), new Coord(0, y));
                if (cf.anisotex.max() <= 1) {
                    add(new Label("(Not supported)"), new Coord(15, y + 15));
                } else {
                    final Label dpy = add(new Label(""), new Coord(165, y + 15));
                    add(new HSlider(160, (int) (cf.anisotex.min() * 2), (int) (cf.anisotex.max() * 2), (int) (cf.anisotex.val * 2)) {
                        protected void added() {
                            dpy();
                            this.c.y = dpy.c.y + ((dpy.sz.y - this.sz.y) / 2);
                        }

                        void dpy() {
                            if (val < 2)
                                dpy.settext("Off");
                            else
                                dpy.settext(String.format("%.1f\u00d7", (val / 2.0)));
                        }

                        public void changed() {
                            try {
                                cf.anisotex.set(val / 2.0f);
                            } catch (GLSettings.SettingException e) {
                                getparent(GameUI.class).error(e.getMessage());
                                return;
                            }
                            dpy();
                            cf.dirty = true;
                        }
                    }, new Coord(0, y + 15));
                }
                y += 35;
                add(new CheckBox("Disable biome tile transitions (requires logout)") {
                    {
                        a = Config.disabletiletrans;
                    }
                    public void set(boolean val) {
                        Config.disabletiletrans = val;
                        Utils.setprefb("disabletiletrans", val);
                        a = val;
                    }
                }, new Coord(0, y));
                y += 35;
                add(new CheckBox("Disable flavor objects including ambient sounds") {
                    {
                        a = Config.hideflocomplete;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("hideflocomplete", val);
                        Config.hideflocomplete = val;
                        a = val;
                    }
                }, new Coord(0, y));
                y += 35;
                add(new CheckBox("Hide flavor objects (requires logout)") {
                    {
                        a = Config.hideflovisual;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("hideflovisual", val);
                        Config.hideflovisual = val;
                        a = val;
                    }
                }, new Coord(0, y));
                y += 35;
                add(new CheckBox("Show weather") {
                    {
                        a = Config.showweather;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("showweather", val);
                        Config.showweather = val;
                        a = val;
                    }
                }, new Coord(0, y));
                y += 35;
                add(new CheckBox("Simple crops (req. logout)") {
                    {
                        a = Config.simplecrops;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("simplecrops", val);
                        Config.simplecrops = val;
                        a = val;
                    }
                }, new Coord(0, y));
                y += 35;
                add(new CheckBox("Simple foragables (req. logout)") {
                    {
                        a = Config.simpleforage;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("simpleforage", val);
                        Config.simpleforage = val;
                        a = val;
                    }
                }, new Coord(0, y));
                y += 35;
                add(new CheckBox("Hide crops") {
                    {
                        a = Config.hidecrops;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("hidecrops", val);
                        Config.hidecrops = val;
                        a = val;
                    }
                }, new Coord(0, y));
                y += 35;
                add(new CheckBox("Limit background FPS to 1") {
                	{
                		a = Config.limitbgfps;
                	}
                
                	public void set(boolean val) {
                		Utils.setprefb("limitbgfps", val);
                		Config.limitbgfps = val;
                		a = val;
                		HavenPanel.bgfd = val ? 1000 : 200;
                	}
                }, new Coord(0, y));
                y = 0; //2nd column
                add(new CheckBox("Show FPS") {
                    {
                        a = Config.showfps;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("showfps", val);
                        Config.showfps = val;
                        a = val;
                    }
                }, new Coord(250, y));
                pack();
            }
        }

        private CPanel curcf = null;

        public void draw(GOut g) {
            if ((curcf == null) || (g.gc.pref != curcf.cf)) {
                if (curcf != null)
                    curcf.destroy();
                curcf = add(new CPanel(g.gc.pref), Coord.z);
            }
            super.draw(g);
        }
    }

    public OptWnd(boolean gopts) {
        super(new Coord(740, 400), "Options", true);
        main = add(new Panel());
        video = add(new VideoPanel(main));
        audio = add(new Panel());
        display = add(new Panel());
        map = add(new Panel());
        general = add(new Panel());
        combat = add(new Panel());

        int y;

        main.add(new PButton(200, "Video settings", 'v', video), new Coord(0, 0));
        main.add(new PButton(200, "Audio settings", 'a', audio), new Coord(0, 30));
        main.add(new PButton(200, "Display settings", 'd', display), new Coord(0, 60));
        main.add(new PButton(200, "Map settings", 'm', map), new Coord(0, 90));
        main.add(new PButton(200, "General settings", 'g', general), new Coord(210, 0));
        main.add(new PButton(200, "Combat settings", 'c', combat), new Coord(210, 30));

        if (gopts) {
            main.add(new Button(200, "Switch character") {
                public void click() {
                    getparent(GameUI.class).act("lo", "cs");
                }
            }, new Coord(270, 300));
            main.add(new Button(200, "Log out") {
                public void click() {
                    getparent(GameUI.class).act("lo");
                }
            }, new Coord(270, 330));
        }
        main.add(new Button(200, "Close") {
            public void click() {
                OptWnd.this.hide();
            }
        }, new Coord(270, 360));
        main.pack();

        // -------------------------------------------- audio
        y = 0;
        audio.add(new Label("Master audio volume"), new Coord(0, y));
        y += 15;
        audio.add(new HSlider(200, 0, 1000, (int) (Audio.volume * 1000)) {
            public void changed() {
                Audio.setvolume(val / 1000.0);
            }
        }, new Coord(0, y));
        y += 30;
        audio.add(new Label("In-game event volume"), new Coord(0, y));
        y += 15;
        audio.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (ui.audio.pos.volume * 1000);
            }

            public void changed() {
                ui.audio.pos.setvolume(val / 1000.0);
            }
        }, new Coord(0, y));
        y += 20;
        audio.add(new Label("Ambient volume"), new Coord(0, y));
        y += 15;
        audio.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (ui.audio.amb.volume * 1000);
            }

            public void changed() {
                ui.audio.amb.setvolume(val / 1000.0);
            }
        }, new Coord(0, y));
        y += 20;
        audio.add(new CheckBox("Alarm on unknown players") {
            {
                a = Config.alarmunknown;
            }

            public void set(boolean val) {
                Utils.setprefb("alarmunknown", val);
                Config.alarmunknown = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 15;
        audio.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int)(Config.alarmunknownvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alarmunknownvol = vol;
                Utils.setprefd("alarmunknownvol", vol);
            }
        }, new Coord(0, y));
        y += 20;
        audio.add(new CheckBox("Alarm on red players") {
            {
                a = Config.alarmred;
            }

            public void set(boolean val) {
                Utils.setprefb("alarmred", val);
                Config.alarmred = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 15;
        audio.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.alarmredvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alarmredvol = vol;
                Utils.setprefd("alarmredvol", vol);
            }
        }, new Coord(0, y));
        //
        y += 20;
        audio.add(new CheckBox("Alarm on Battering Rams") {
            {
                a = Config.alarmram;
            }

            public void set(boolean val) {
                Utils.setprefb("alarmram", val);
                Config.alarmram = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 15;
        audio.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.alarmramvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alarmramvol = vol;
                Utils.setprefd("alarmramvol", vol);
            }
        }, new Coord(0, y));
        //
        y += 20;
        audio.add(new Label("Timers alarm volume"), new Coord(0, y));
        y += 15;
        audio.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.timersalarmvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.timersalarmvol = vol;
                Utils.setprefd("timersalarmvol", vol);
            }
        }, new Coord(0, y));
        y += 20;
        audio.add(new CheckBox("Alarm on new private chat") {
            {
                a = Config.chatalarm;
            }

            public void set(boolean val) {
                Utils.setprefb("chatalarm", val);
                Config.chatalarm = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 15;
        audio.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.chatalarmvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.chatalarmvol = vol;
                Utils.setprefd("chatalarmvol", vol);
            }
        }, new Coord(0, y));
        y += 20;
        audio.add(new CheckBox("Alarm when curio finishes") {
            {
                a = Config.studyalarm;
            }

            public void set(boolean val) {
                Utils.setprefb("studyalarm", val);
                Config.studyalarm = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 15;
        audio.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.studyalarmvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.studyalarmvol = vol;
                Utils.setprefd("studyalarmvol", vol);
            }
        }, new Coord(0, y));

        // -------------------------------------------- audio 2nd column
        y = 0;
        audio.add(new Label("'Chip' sound volume"), new Coord(250, y));
        y += 15;
        audio.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.sfxchipvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.sfxchipvol = vol;
                Utils.setprefd("sfxchipvol", vol);
            }
        }, new Coord(250, y));
        y += 20;
        audio.add(new Label("'Squeak' sound volume"), new Coord(250, y));
        y += 15;
        audio.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.sfxsqueakvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.sfxsqueakvol = vol;
                Utils.setprefd("sfxsqueakvol", vol);
            }
        }, new Coord(250, y));
        y += 20;
        audio.add(new Label("Quern sound volume"), new Coord(250, y));
        y += 15;
        audio.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.sfxquernvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.sfxquernvol = vol;
                Utils.setprefd("sfxquernvol", vol);
            }
        }, new Coord(250, y));

        audio.add(new PButton(200, "Back", 27, main), new Coord(270, 360));
        audio.pack();

        // -------------------------------------------- display
        y = 0;
        display.add(new CheckBox("Display kin names") {
            {
                a = Config.showkinnames;
            }

            public void set(boolean val) {
                Utils.setprefb("showkinnames", val);
                Config.showkinnames = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        display.add(new CheckBox("Free camera rotation") {
            {
                a = Config.camfree;
            }

            public void set(boolean val) {
                Utils.setprefb("camfree", val);
                Config.camfree = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        display.add(new Label("Bad camera scrolling sensitivity"), new Coord(0, y));
        display.add(new HSlider(50, 0, 50, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = Config.badcamsensitivity;
            }
            public void changed() {
                Config.badcamsensitivity = val;
                Utils.setprefi("badcamsensitivity", val);
            }
        }, new Coord(160, y));
        y += 35;
        display.add(new CheckBox("Show item quality") {
            {
                a = Config.showquality;
            }

            public void set(boolean val) {
                Utils.setprefb("showquality", val);
                Config.showquality = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 20;
        display.add(new Label("High"), new Coord(0, y));
        display.add(new Label("Avg E/S/V"), new Coord(35, y));
        display.add(new Label("All"), new Coord(100, y));
        display.add(new Label("Avg S/V"), new Coord(135, y));
        display.add(new Label("Low"), new Coord(190, y));
        y += 10;
        display.add(new HSlider(210, 0, 4, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = Config.showqualitymode;
            }
            public void changed() {
                Config.showqualitymode = val;
                Utils.setprefi("showqualitymode", val);
            }
        }, new Coord(0, y));
        y += 25;
        display.add(new CheckBox("Show LP gain multiplier for curios") {
            {
                a = Config.showlpgainmult;
            }

            public void set(boolean val) {
                Utils.setprefb("showlpgainmult", val);
                Config.showlpgainmult = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        display.add(new CheckBox("Use arithmetic average") {
            {
                a = Config.arithavg;
            }

            public void set(boolean val) {
                Utils.setprefb("arithavg", val);
                Config.arithavg = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        display.add(new CheckBox("Round item quality to a whole number") {
            {
                a = Config.qualitywhole;
            }

            public void set(boolean val) {
                Utils.setprefb("qualitywhole", val);
                Config.qualitywhole = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        display.add(new CheckBox("Display item completion as progress bar") {
            {
                a = Config.itemmeterbar;
            }

            public void set(boolean val) {
                Utils.setprefb("itemmeterbar", val);
                Config.itemmeterbar = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        display.add(new CheckBox("Display item completion as percentage") {
            {
                a = Config.itempercentage;
            }

            public void set(boolean val) {
                Utils.setprefb("itempercentage", val);
                Config.itempercentage = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        display.add(new CheckBox("Show hourglass percentage") {
            {
                a = Config.showprogressperc;
            }

            public void set(boolean val) {
                Utils.setprefb("showprogressperc", val);
                Config.showprogressperc = val;
                a = val;
            }
        }, new Coord(0, y));

        // -------------------------------------------- display 2nd column
        y = 0;
        display.add(new Label("Chat font size (requires restart): Small"), new Coord(260, y + 1));
        display.add(new HSlider(40, 0, 3, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = Config.chatfontsize;
            }
            public void changed() {
                Config.chatfontsize = val;
                Utils.setprefi("chatfontsize", val);
            }
        }, new Coord(452, y));
        display.add(new Label("Large"), new Coord(495, y + 1));
        y += 35;
        display.add(new CheckBox("Show quick hand slots") {
            {
                a = Config.quickslots;
            }

            public void set(boolean val) {
                Utils.setprefb("quickslots", val);
                Config.quickslots = val;
                a = val;

                try {
                    Widget qs = ((GameUI) parent.parent.parent).quickslots;
                    if (qs != null) {
                        if (val)
                            qs.show();
                        else
                            qs.hide();
                    }
                } catch (ClassCastException e) { // in case we are at the login screen
                }
            }
        }, new Coord(260, y));
        y += 35;
        display.add(new CheckBox("Show Attribute/Ability values in craft window") {
            {
                a = Config.showcraftcap;
            }

            public void set(boolean val) {
                Utils.setprefb("showcraftcap", val);
                Config.showcraftcap = val;
                a = val;
            }
        }, new Coord(260, y));
        y += 35;
        display.add(new CheckBox("Show objects health") {
            {
                a = Config.showgobhp;
            }

            public void set(boolean val) {
                Utils.setprefb("showgobhp", val);
                Config.showgobhp = val;
                a = val;
            }
        }, new Coord(260, y));
        y += 35;
        display.add(new CheckBox("Show player paths") {
            {
                a = Config.showplayerpaths;
            }

            public void set(boolean val) {
                Utils.setprefb("showplayerpaths", val);
                Config.showplayerpaths = val;
                a = val;
            }
        }, new Coord(260, y));
        y += 35;
        display.add(new CheckBox("Show animal paths") {
            {
                a = Config.showanimalpaths;
            }

            public void set(boolean val) {
                Utils.setprefb("showanimalpaths", val);
                Config.showanimalpaths = val;
                a = val;
            }
        }, new Coord(260, y));
        y += 35;
        display.add(new CheckBox("Show study remaining time (req. restart)") {
            {
                a = Config.showstudylefttime;
            }

            public void set(boolean val) {
                Utils.setprefb("showstudylefttime", val);
                Config.showstudylefttime = val;
                a = val;
            }
        }, new Coord(260, y));
        y += 35;
        display.add(new CheckBox("Show inventory on login") {
            {
                a = Config.showinvonlogin;
            }

            public void set(boolean val) {
                Utils.setprefb("showinvonlogin", val);
                Config.showinvonlogin = val;
                a = val;
            }
        }, new Coord(260, y));
        y += 35;
        display.add(new CheckBox("Show contents bars for buckets/flasks") {
            {
                a = Config.showcontentsbars;
            }

            public void set(boolean val) {
                Utils.setprefb("showcontentsbars", val);
                Config.showcontentsbars = val;
                a = val;
            }
        }, new Coord(260, y));

        display.add(new Button(220, "Reset Windows (req. logout)") {
            @Override
            public void click() {
                for (String wndcap : Window.persistentwnds)
                    Utils.delpref(wndcap + "_c");
                Utils.delpref("mmapc");
                Utils.delpref("mmapwndsz");
                Utils.delpref("mmapsz");
                Utils.delpref("quickslotsc");
                Utils.delpref("chatsz");
                Utils.delpref("chatvis");
                Utils.delpref("gui-bl-visible");
                Utils.delpref("gui-br-visible");
                Utils.delpref("gui-ul-visible");
                Utils.delpref("gui-ur-visible");
                Utils.delpref("menu-visible");
            }
        }, new Coord(260, 320));
        display.add(new PButton(200, "Back", 27, main), new Coord(270, 360));
        display.pack();

        // -------------------------------------------- map
        y = 0;
        map.add(new CheckBox("Show players on minimap") {
            {
                a = Config.showplayersmmap;
            }

            public void set(boolean val) {
                Utils.setprefb("showplayersmmap", val);
                Config.showplayersmmap = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        map.add(new CheckBox("Save map tiles to disk") {
            {
                a = Config.savemmap;
            }

            public void set(boolean val) {
                Utils.setprefb("savemmap", val);
                Config.savemmap = val;
                MapGridSave.mgs = null;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        map.add(new CheckBox("MMB to drag & L/RMB to move") {
            {
                a = Config.alternmapctrls;
            }

            public void set(boolean val) {
                Utils.setprefb("alternmapctrls", val);
                Config.alternmapctrls = val;
                a = val;
            }
        }, new Coord(0, y));

        map.add(new Label("Show boulders:"), new Coord(180, 0));
        CheckListbox boulderlist = new CheckListbox(130, 18) {
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Config.boulderssel = getselected();
                Utils.setprefsa("boulderssel", Config.boulderssel);
            }
        };
        for (String boulder : Config.boulders) {
            boolean selected = false;
            if (Config.boulderssel != null) {
                for (String sboulder : Config.boulderssel) {
                    if (sboulder.equals(boulder)) {
                        selected = true;
                        break;
                    }
                }
            }
            boulderlist.items.add(new CheckListboxItem(boulder, selected));
        }
        map.add(boulderlist, new Coord(180, 15));

        map.add(new Label("Show bushes:"), new Coord(325, 0));
        CheckListbox bushlist = new CheckListbox(130, 18) {
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Config.bushessel = getselected();
                Utils.setprefsa("bushessel", Config.bushessel);
            }
        };
        for (String bush : Config.bushes) {
            boolean selected = false;
            if (Config.bushessel != null) {
                for (String sbush : Config.bushessel) {
                    if (sbush.equals(bush)) {
                        selected = true;
                        break;
                    }
                }
            }
            bushlist.items.add(new CheckListboxItem(bush, selected));
        }
        map.add(bushlist, new Coord(325, 15));

        map.add(new Label("Show trees:"), new Coord(470, 0));
        CheckListbox treelist = new CheckListbox(130, 18) {
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Config.treessel = getselected();
                Utils.setprefsa("treessel", Config.treessel);
            }
        };
        for (String tree : Config.trees) {
            boolean selected = false;
            if (Config.treessel != null) {
                for (String stree : Config.treessel) {
                    if (stree.equals(tree)) {
                        selected = true;
                        break;
                    }
                }
            }
            treelist.items.add(new CheckListboxItem(tree, selected));
        }
        map.add(treelist, new Coord(470, 15));

        map.add(new Label("Hide icons:"), new Coord(615, 0));
        CheckListbox iconslist = new CheckListbox(130, 18) {
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Config.iconssel = getselected();
                Utils.setprefsa("iconssel", Config.iconssel);
            }
        };
        for (String icon : Config.icons) {
            boolean selected = false;
            if (Config.iconssel != null) {
                for (String sicon : Config.iconssel) {
                    if (sicon.equals(icon)) {
                        selected = true;
                        break;
                    }
                }
            }
            iconslist.items.add(new CheckListboxItem(icon, selected));
        }
        map.add(iconslist, new Coord(615, 15));


        map.add(new PButton(200, "Back", 27, main), new Coord(270, 360));
        map.pack();

        // -------------------------------------------- general
        y = 0;
        general.add(new CheckBox("Save chat logs to disk") {
            {
                a = Config.chatsave;
            }

            public void set(boolean val) {
                Utils.setprefb("chatsave", val);
                Config.chatsave = val;
                a = val;
                if (!val && Config.chatlog != null) {
                    try {
                        Config.chatlog.close();
                        Config.chatlog = null;
                    } catch (Exception e) {
                    }
                }
            }
        }, new Coord(0, y));
        y += 35;
        general.add(new CheckBox("Show timestamps in chats") {
            {
                a = Config.chattimestamp;
            }

            public void set(boolean val) {
                Utils.setprefb("chattimestamp", val);
                Config.chattimestamp = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        general.add(new CheckBox("Notify when kin comes online") {
            {
                a = Config.notifykinonline;
            }

            public void set(boolean val) {
                Utils.setprefb("notifykinonline", val);
                Config.notifykinonline = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        general.add(new CheckBox("Auto hearth") {
            {
                a = Config.autohearth;
            }

            public void set(boolean val) {
                Utils.setprefb("autohearth", val);
                Config.autohearth = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        general.add(new CheckBox("Print server time to System log") {
            {
                a = Config.servertime;
            }

            public void set(boolean val) {
                Utils.setprefb("servertime", val);
                Config.servertime = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        general.add(new CheckBox("Automatically select 'Pick' action") {
            {
                a = Config.autopick;
            }

            public void set(boolean val) {
                Utils.setprefb("autopick", val);
                Config.autopick = val;
                a = val;
            }
        }, new Coord(0, y));
        //
        y += 35;
        general.add(new CheckBox("Fast Flower Menu") {
            {
                a = Config.fastflower;
            } 

            public void set(boolean val) {
                Utils.setprefb("autopick", val);
                Config.fastflower = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        general.add(new CheckBox("Auto Logout after 5 minutes afking") {
            {
                a = Config.afklogout;
            } 

            public void set(boolean val) {
                Utils.setprefb("afklogout", val);
                Config.afklogout = val;
                a = val;
            }
        }, new Coord(0, y));
        //
        y += 35;
        general.add(new CheckBox("Automatically select 'Harvest' action") {
            {
                a = Config.autoharvest;
            }

            public void set(boolean val) {
                Utils.setprefb("autoharvest", val);
                Config.autoharvest = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        general.add(new CheckBox("Toggle Tracking on logon") {
            {
                a = Config.toggletracking;
            }

            public void set(boolean val) {
                Utils.setprefb("toggletracking", val);
                Config.toggletracking = val;
                a = val;
            }
        }, new Coord(500, y));
        y += 25;
        general.add(new CheckBox("Toggle Criminal Acts on logon") {
            {
                a = Config.togglecriminalacts;
            }

            public void set(boolean val) {
                Utils.setprefb("togglecriminalacts", val);
                Config.togglecriminalacts = val;
                a = val;
            }
        }, new Coord(500,y));
        y -= 25;
        general.add(new CheckBox("Run on login") {
            {
                a = Config.runonlogin;
            }

            public void set(boolean val) {
                Utils.setprefb("runonlogin", val);
                Config.runonlogin = val;
                a = val;
            }
        }, new Coord(0, y));

        general.add(new PButton(200, "Back", 27, main), new Coord(270, 360));
        general.pack();

        // -------------------------------------------- combat
        y = 0;
        combat.add(new CheckBox("Display damage received by opponents") {
            {
                a = Config.showdmgop;
            }

            public void set(boolean val) {
                Utils.setprefb("showdmgop", val);
                Config.showdmgop = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        combat.add(new CheckBox("Display damage received by me") {
            {
                a = Config.showdmgmy;
            }

            public void set(boolean val) {
                Utils.setprefb("showdmgmy", val);
                Config.showdmgmy = val;
                a = val;
            }
        }, new Coord(0, y));

        combat.add(new PButton(200, "Back", 27, main), new Coord(270, 360));
        combat.pack();

        chpanel(main);
    }

    public OptWnd() {
        this(true);
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (msg == "close")) {
            hide();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public void show() {
        chpanel(main);
        super.show();
    }
}
