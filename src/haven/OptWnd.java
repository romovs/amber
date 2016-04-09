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


import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class OptWnd extends Window {
    public final Panel main, video, audio, display, map, general, combat, hide, control, uis, quality;
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
                add(new CheckBox("Hide flavor objects but keep sounds (requires logout)") {
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
        hide = add(new Panel());
        control = add(new Panel());
        uis = add(new Panel());
        quality = add(new Panel());
        
        int y;

        main.add(new PButton(200, "Video settings", 'v', video), new Coord(0, 0));
        main.add(new PButton(200, "Audio settings", 'a', audio), new Coord(0, 30));
        main.add(new PButton(200, "Display settings", 'd', display), new Coord(0, 60));
        main.add(new PButton(200, "Map settings", 'm', map), new Coord(0, 90));
        main.add(new PButton(200, "General settings", 'g', general), new Coord(210, 0));
        main.add(new PButton(200, "Combat settings", 'c', combat), new Coord(210, 30));
        main.add(new PButton(200, "Hide settings", 'h', hide), new Coord(0, 120));
        main.add(new PButton(200, "Control settings", 'k', control), new Coord(210, 60));
        main.add(new PButton(200, "UI settings", 'u', uis), new Coord(210, 90));
        main.add(new PButton(200, "Quality settings", 'q', quality), new Coord(420, 0));
        
        if (gopts) {
            main.add(new Button(200, "Switch character") {
                public void click() {
                    GameUI gui = gameui();
                    gui.act("lo", "cs");
                    if (gui != null & gui.map != null)
                        gui.map.canceltasks();
                }
            }, new Coord(270, 300));
            main.add(new Button(200, "Log out") {
                public void click() {
                    GameUI gui = gameui();
                    gui.act("lo");
                    if (gui != null & gui.map != null)
                        gui.map.canceltasks();
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
        audio.add(new CheckBox("Alarm on new party chat message") {
            {
                a = Config.partychatalarm;
            }

            public void set(boolean val) {
                Utils.setprefb("partychatalarm", val);
                Config.partychatalarm = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 15;
        audio.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.partychatalarmvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.partychatalarmvol = vol;
                Utils.setprefd("partychatalarmvol", vol);
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
        y += 20;
        audio.add(new CheckBox("Alarm when pony power < 10%") {
            {
                a = Config.ponyalarm;
            }

            public void set(boolean val) {
                Utils.setprefb("ponyalarm", val);
                Config.ponyalarm = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 15;
        audio.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.ponyalarmvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.ponyalarmvol = vol;
                Utils.setprefd("ponyalarmvol", vol);
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
        y += 20;
        audio.add(new CheckBox("Disable metallic mining sound") {
            {
                a = Config.nometallicsfx;
            }

            public void set(boolean val) {
                Utils.setprefb("nometallicsfx", val);
                Config.nometallicsfx = val;
                a = val;
            }
        }, new Coord(250, y));
        y += 20;
        audio.add(new CheckBox("Alarm on rare curios (bluebells, glimmers, ...)") {
            {
                a = Config.alarmonforagables;
            }

            public void set(boolean val) {
                Utils.setprefb("alarmonforagables", val);
                Config.alarmonforagables = val;
                a = val;
            }
        }, new Coord(250, y));
        y += 15;
        audio.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.alarmonforagablesvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alarmonforagablesvol = vol;
                Utils.setprefd("alarmonforagablesvol", vol);
            }
        }, new Coord(250, y));
        y += 20;
        audio.add(new CheckBox("Alarm on bears & lynx") {
            {
                a = Config.alarmbears;
            }

            public void set(boolean val) {
                Utils.setprefb("alarmbears", val);
                Config.alarmbears = val;
                a = val;
            }
        }, new Coord(250, y));
        y += 15;
        audio.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.alarmbearsvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alarmbearsvol = vol;
                Utils.setprefd("alarmbearsvol", vol);
            }
        }, new Coord(250, y));
        y += 20;
        audio.add(new Label("Fireplace sound volume (req. restart)"), new Coord(250, y));
        y += 15;
        audio.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.sfxfirevol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.sfxfirevol = vol;
                Utils.setprefd("sfxfirevol", vol);
            }
        }, new Coord(250, y));
        y += 20;
        audio.add(new CheckBox("Alarm on trolls") {
            {
                a = Config.alarmtroll;
            }

            public void set(boolean val) {
                Utils.setprefb("alarmtroll", val);
                Config.alarmtroll = val;
                a = val;
            }
        }, new Coord(250, y));
        y += 15;
        audio.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.alarmtrollvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alarmtrollvol = vol;
                Utils.setprefd("alarmtrollvol", vol);
            }
        }, new Coord(250, y));
        y += 20;
        audio.add(new CheckBox("Alarm on mammoths") {
            {
                a = Config.alarmmammoth;
            }

            public void set(boolean val) {
                Utils.setprefb("alarmmammoth", val);
                Config.alarmmammoth = val;
                a = val;
            }
        }, new Coord(250, y));
        y += 15;
        audio.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.alarmmammothvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alarmmammothvol = vol;
                Utils.setprefd("alarmmammothvol", vol);
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
        y += 35;
        display.add(new CheckBox("Show attributes & softcap values in craft window") {
            {
                a = Config.showcraftcap;
            }

            public void set(boolean val) {
                Utils.setprefb("showcraftcap", val);
                Config.showcraftcap = val;
                a = val;
            }
        }, new Coord(0, y));
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
        }, new Coord(0, y));
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
        }, new Coord(0, y));
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
        }, new Coord(0, y));
        y += 35;
        display.add(new CheckBox("Show study remaining time") {
            {
                a = Config.showstudylefttime;
            }

            public void set(boolean val) {
                Utils.setprefb("showstudylefttime", val);
                Config.showstudylefttime = val;
                a = val;
            }
        }, new Coord(0, y));
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
        }, new Coord(0, y));
        // -------------------------------------------- display 2nd column
        y = 0;
        display.add(new CheckBox("Remove toggle ui via space") {
            {
                a = Config.toggleuinot;
            }

            public void set(boolean val) {
                Utils.setprefb("toggleuinot", val);
                Config.toggleuinot = val;
                a = val;
            }
        }, new Coord(520, y));
        y += 35;
        display.add(new CheckBox("Display Fep Meter (Req. Restart)") {
            {
                a = Config.fepmeter;
            }

            public void set(boolean val) {
                Utils.setprefb("fepmeter", val);
                Config.fepmeter = val;
                a = val;
            }
        }, new Coord(520, y));
        y += 35;
        display.add(new CheckBox("Display Hunger Meter (Req. Restart)") {
            {
                a = Config.hungermeter;
            }

            public void set(boolean val) {
                Utils.setprefb("hungermeter", val);
                Config.hungermeter = val;
                a = val;
            }
        }, new Coord(520, y));
        y += 35;
        display.add(new CheckBox("Log satiation gain when eating") {
            {
                a = Config.logfoodchanges;
            }

            public void set(boolean val) {
                Utils.setprefb("logfoodchanges", val);
                Config.logfoodchanges = val;
                a = val;
            }
        }, new Coord(520, y));
        y += 35;
        display.add(new CheckBox("Show wear bars") {
            {
                a = Config.showwearbars;
            }

            public void set(boolean val) {
                Utils.setprefb("showwearbars", val);
                Config.showwearbars = val;
                a = val;
            }
        }, new Coord(520, y));
        y += 35;
        display.add(new CheckBox("Highlight party members") {
            {
                a = Config.highlightParty;
            }

            public void set(boolean val) {
                Utils.setprefb("highlightParty", val);
                Config.highlightParty = val;
                a = val;
            }
        }, new Coord(520, y));
        y += 35;
        display.add(new CheckBox("Show troughs/beehives radius") {
            {
                a = Config.showfarmrad;
            }

            public void set(boolean val) {
                Utils.setprefb("showfarmrad", val);
                Config.showfarmrad = val;
                a = val;
            }
        }, new Coord(520, y));
        y += 35;
        display.add(new CheckBox("Show animal radius") {
            {
                a = Config.showanimalrad;
            }

            public void set(boolean val) {
                Utils.setprefb("showanimalrad", val);
                Config.showanimalrad = val;
                a = val;
            }
        }, new Coord(520, y));
        y += 35;
        display.add(new CheckBox("Hide Horizon Meter  (Req. Restart)") {
            {
                a = Config.hideum;
            }

            public void set(boolean val) {
                Utils.setprefb("hideum", val);
                Config.hideum = val;
                a = val;
            }
        }, new Coord(520, y));
        y += 35;
        display.add(new CheckBox("Show F-key toolbar") {
            {
                a = Config.fbelt;
            }

            public void set(boolean val) {
                Utils.setprefb("fbelt", val);
                Config.fbelt = val;
                a = val;
                FBelt fbelt = gameui().fbelt;
                if (fbelt != null) {
                    if (val)
                        fbelt.show();
                    else
                        fbelt.hide();
                }
            }
        }, new Coord(520, y));
        y += 35;
        display.add(new CheckBox("Highlight empty/finished drying frames") {
            {
                a = Config.showdframestatus;
            }

            public void set(boolean val) {
                Utils.setprefb("showdframestatus", val);
                Config.showdframestatus = val;
                a = val;
            }
        }, new Coord(520, y));

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
                Utils.delpref("haven.study.position");
                Utils.delpref("fbelt_c");
                Utils.delpref("fbelt_vertical");
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

        map.add(new Label("Show boulders:"), new Coord(180, 0));
        map.add(new Label("Show bushes:"), new Coord(325, 0));
        map.add(new Label("Show trees:"), new Coord(470, 0));
        map.add(new Label("Hide icons:"), new Coord(615, 0));

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
                a = Config.servertimesyslog;
            }

            public void set(boolean val) {
                Utils.setprefb("servertimesyslog", val);
                Config.servertimesyslog = val;
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
                Utils.setprefb("fastflower", val);
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
        general.add(new CheckBox("Automatically select 'Eat' action") {
            {
                a = Config.autoeat;
            }

            public void set(boolean val) {
                Utils.setprefb("autoeat", val);
                Config.autoeat = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
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
        y += 35;
        general.add(new CheckBox("Show server time") {
            {
                a = Config.showservertime;
            }

            public void set(boolean val) {
                Utils.setprefb("showservertime", val);
                Config.showservertime = val;
                a = val;
            }
        }, new Coord(0, y));
        // -------------------------------------------- general 2nd column
        y = 0;
        general.add(new CheckBox("Show swimming/tracking/crime buffs (req. logout)") {
            {
                a = Config.showtoggles;
            }

            public void set(boolean val) {
                Utils.setprefb("showtoggles", val);
                Config.showtoggles = val;
                a = val;
            }
        }, new Coord(260, y));
        y += 35;
        general.add(new CheckBox("Select System log on login") {
            {
                a = Config.syslogonlogin;
            }

            public void set(boolean val) {
                Utils.setprefb("syslogonlogin", val);
                Config.syslogonlogin = val;
                a = val;
            }
        }, new Coord(260, y));
        y += 35;
        general.add(new CheckBox("Automatically select 'Split' action") {
            {
                a = Config.autosplit;
            }

            public void set(boolean val) {
                Utils.setprefb("autosplit", val);
                Config.autosplit = val;
                a = val;
            }
        }, new Coord(260, y));
        y += 35;
        general.add(new CheckBox("Drop Leeches Automatically") {
            {
                a = Config.dropleeches;
            }

            public void set(boolean val) {
                Utils.setprefb("dropleeches", val);
                Config.dropleeches = val;
                a = val;
            }
        }, new Coord(260, y));
        y += 35;
        general.add(new CheckBox("Auto-miner: drop mined ore") {
            {
                a = Config.dropore;
            }

            public void set(boolean val) {
                Utils.setprefb("dropore", val);
                Config.dropore = val;
                a = val;
            }
        }, new Coord(260, y));

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
        y += 35;
        combat.add(new CheckBox("Highlight current opponent") {
            {
                a = Config.hlightcuropp;
            }

            public void set(boolean val) {
                Utils.setprefb("hlightcuropp", val);
                Config.hlightcuropp = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        combat.add(new CheckBox("Aggro closest unknown/red player on Tab key") {
            {
                a = Config.agroclosest;
            }

            public void set(boolean val) {
                Utils.setprefb("agroclosest", val);
                Config.agroclosest = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        combat.add(new CheckBox("Smaller Combat Move Icons") {
            {
                a = Config.smallicon;
            }

            public void set(boolean val) {
                Utils.setprefb("smallicon", val);
                Config.smallicon = val;
                a = val;
            }
        }, new Coord(0, y));

        combat.add(new PButton(200, "Back", 27, main), new Coord(270, 360));
        combat.pack();
        // -------------------- HIDE SETTINGS
        y = 0;
        hide.add(new Label("Ctrl + h to toggle hide"), new Coord(0, y));
        y += 35;
        hide.add(new CheckBox("Disable highlight boxes") {
            {
                a = Config.nohidebox;
            }

            public void set(boolean val) {
                Utils.setprefb("nohidebox", val);
                Config.nohidebox = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        hide.add(new CheckBox("Hide Everything") {
            {
                a = Config.hideall;
            }

            public void set(boolean val) {
                Utils.setprefb("hideall", val);
                Config.hideall = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        hide.add(new CheckBox("Hide Trees") {
            {
                a = Config.hidetrees;
            }
            public void set(boolean val) {
                Utils.setprefb("hidetrees", val);
                Config.hidetrees = val;
                a = val;
            }
        }, new Coord(0,y));
        y += 35;
        hide.add(new CheckBox("Hide Bushes") {
            {
                a = Config.hidebushes;
            }
            public void set(boolean val) {
                Utils.setprefb("hidebushes", val);
                Config.hidebushes = val;
                a = val;
            }
        }, new Coord(0,y));
        y += 35;
                hide.add(new CheckBox("Hide Crops") {
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
                hide.add(new CheckBox("Hide Walls") {
                    {
                        a = Config.hidewalls;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("hidewalls", val);
                        Config.hidewalls = val;
                        a = val;
                    }
                }, new Coord(0, y));
                y += 35;
                hide.add(new CheckBox("Hide Wagons") {
                    {
                        a = Config.hidewagons;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("hidewagons", val);
                        Config.hidewagons = val;
                        a = val;
                    }
                }, new Coord(0, y));
                y += 35;
                hide.add(new CheckBox("Hide Drying Frames") {
                    {
                        a = Config.hidedframes;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("hidedframes", val);
                        Config.hidedframes = val;
                        a = val;
                    }
                }, new Coord(0, y));
                y += 35;
                hide.add(new CheckBox("Hide Houses (Hides also door)") {
                    {
                        a = Config.hidehouses;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("hidehouses", val);
                        Config.hidehouses = val;
                        a = val;
                    }
                }, new Coord(0, y));
                y = 0;
                hide.add(new CheckBox("Hide Hearth Fires") {
                    {
                        a = Config.hidehfs;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("hidehfs", val);
                        Config.hidehfs = val;
                        a = val;
                    }
                	}, new Coord(260, y));
                y += 35;
                hide.add(new CheckBox("Hide Dream Catchers") {
                    {
                        a = Config.hidedcatchers;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("hidedcatchers", val);
                        Config.hidedcatchers = val;
                        a = val;
                    }
                	}, new Coord(260, y));
                y = 0;
                hide.add(new Label("Red"), new Coord(550, y));
                y += 15;
                hide.add(new HSlider(150, 0, 255, 0) {
                    protected void attach(UI ui) {
                        super.attach(ui);
                        val = (int) (Config.hidered);
                    }

                    public void changed() {
                        double vol = val;
                        Config.hidered = vol;
                        Utils.setprefd("hidered", vol);
                    }
                }, new Coord(550, y));
                y += 20;
                hide.add(new Label("Green"), new Coord(550, y));
                y += 15;
                hide.add(new HSlider(150, 0, 255, 0) {
                    protected void attach(UI ui) {
                        super.attach(ui);
                        val = (int) (Config.hidegreen);
                    }

                    public void changed() {
                        double vol = val;
                        Config.hidegreen = vol;
                        Utils.setprefd("hidegreen", vol);
                    }
                }, new Coord(550, y));
                y += 20;
                hide.add(new Label("Blue"), new Coord(550, y));
                y += 15;
                hide.add(new HSlider(150, 0, 255, 0) {
                    protected void attach(UI ui) {
                        super.attach(ui);
                        val = (int) (Config.hideblue);
                    }

                    public void changed() {
                        double vol = val;
                        Config.hideblue = vol;
                        Utils.setprefd("hideblue", vol);
                    }
                }, new Coord(550, y));
                
                hide.add(new PButton(200, "Back", 27, main), new Coord(270, 360));
                hide.pack();
        // -------------------------------------------- control
        y = 0;
        control.add(new CheckBox("Free camera rotation") {
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
        control.add(new Label("Bad camera scrolling sensitivity"), new Coord(0, y));
        control.add(new HSlider(50, 0, 50, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = Config.badcamsensitivity;
            }
            public void changed() {
                Config.badcamsensitivity = val;
                Utils.setprefi("badcamsensitivity", val);
            }
        }, new Coord(180, y));
        y += 35;
        control.add(new CheckBox("Minimap: use MMB to drag & L/RMB to move") {
            {
                a = Config.alternmapctrls;
            }

            public void set(boolean val) {
                Utils.setprefb("alternmapctrls", val);
                Config.alternmapctrls = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        control.add(new CheckBox("Use French (AZERTY) keyboard layout") {
            {
                a = Config.userazerty;
            }

            public void set(boolean val) {
                Utils.setprefb("userazerty", val);
                Config.userazerty = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        control.add(new CheckBox("Reverse bad camera MMB x-axis") {
            {
                a = Config.reversebadcamx;
            }

            public void set(boolean val) {
                Utils.setprefb("reversebadcamx", val);
                Config.reversebadcamx = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        control.add(new CheckBox("Reverse bad camera MMB y-axis") {
            {
                a = Config.reversebadcamy;
            }

            public void set(boolean val) {
                Utils.setprefb("reversebadcamy", val);
                Config.reversebadcamy = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        control.add(new CheckBox("Force hardware cursor (req. restart)") {
            {
                a = Config.hwcursor;
            }

            public void set(boolean val) {
                Utils.setprefb("hwcursor", val);
                Config.hwcursor = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        control.add(new CheckBox("Disable dropping items over water (overridable with Ctrl)") {
            {
                a = Config.nodropping;
            }

            public void set(boolean val) {
                Utils.setprefb("nodropping", val);
                Config.nodropping = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        control.add(new CheckBox("Enable full zoom-out in Ortho cam") {
            {
                a = Config.enableorthofullzoom;
            }

            public void set(boolean val) {
                Utils.setprefb("enableorthofullzoom", val);
                Config.enableorthofullzoom = val;
                a = val;
            }
        }, new Coord(0, y));
        y = 0;
        control.add(new CheckBox("Transfer items in ascending order instead of descending") {
            {
                a = Config.sortascending;
            }

            public void set(boolean val) {
                Utils.setprefb("sortascending", val);
                Config.sortascending = val;
                a = val;
            }
        }, new Coord(320, y));

        control.add(new PButton(200, "Back", 27, main), new Coord(270, 360));
        control.pack();

        // -------------------------------------------- uis

        y = 0;
        uis.add(new Label("Language (req. restart):"), new Coord(0, y));
        uis.add(langDropdown(), new Coord(120, y));

        y += 35;
        uis.add(new CheckBox("Show quick hand slots") {
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
        }, new Coord(0, y));
        y += 35;
        uis.add(new CheckBox("Show F-key toolbar") {
            {
                a = Config.fbelt;
            }

            public void set(boolean val) {
                Utils.setprefb("fbelt", val);
                Config.fbelt = val;
                a = val;
                GameUI gui = gameui();
                if (gui != null) {
                    FBelt fbelt = gui.fbelt;
                    if (fbelt != null) {
                        if (val)
                            fbelt.show();
                        else
                            fbelt.hide();
                    }
                }
            }
        }, new Coord(0, y));
        y += 35;
        uis.add(new CheckBox("Hide extensions menu (req. restart)") {
            {
                a = Config.hidexmenu;
            }

            public void set(boolean val) {
                Utils.setprefb("hidexmenu", val);
                Config.hidexmenu = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        uis.add(new CheckBox("Show inventory on login") {
            {
                a = Config.showinvonlogin;
            }

            public void set(boolean val) {
                Utils.setprefb("showinvonlogin", val);
                Config.showinvonlogin = val;
                a = val;
            }
        }, new Coord(0, y));
        y += 35;
        uis.add(new Label("Chat font size (req. restart):"), new Coord(0, y + 1));
        uis.add(chatFntSzDropdown(), new Coord(180, y));
        y += 35;
        uis.add(new CheckBox("Hide quests panel") {
            {
                a = Config.noquests;
            }

            public void set(boolean val) {
                Utils.setprefb("noquests", val);
                Config.noquests = val;
                try {
                    if (val)
                        gameui().questpanel.hide();
                    else
                        gameui().questpanel.show();
                } catch (NullPointerException npe) { // ignored
                }
                a = val;
            }
        }, new Coord(0, y));

        uis.add(new Button(220, "Reset Windows (req. logout)") {
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
                Utils.delpref("fbelt_c");
                Utils.delpref("fbelt_vertical");
            }
        }, new Coord(260, 320));

        uis.add(new PButton(200, "Back", 27, main), new Coord(270, 360));
        uis.pack();

        // -------------------------------------------- quality

        y = 0;
        quality.add(new CheckBox("Show item quality") {
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
        quality.add(new Label("Highest"), new Coord(0, y));
        quality.add(new Label("Avg E/S/V"), new Coord(65, y));
        quality.add(new Label("All"), new Coord(150, y));
        quality.add(new Label("Avg S/V"), new Coord(205, y));
        quality.add(new Label("Lowest"), new Coord(275, y));
        y += 10;
        quality.add(new HSlider(310, 0, 4, 0) {
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
        quality.add(new CheckBox("Show LP gain multiplier for curios") {
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
        quality.add(new CheckBox("Use arithmetic average") {
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
        quality.add(new CheckBox("Round item quality to a whole number") {
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
        quality.add(new CheckBox("Draw background for quality values") {
            {
                a = Config.qualitybg;
            }

            public void set(boolean val) {
                Utils.setprefb("qualitybg", val);
                Config.qualitybg = val;
                a = val;
            }
        }, new Coord(0, y));

        quality.add(new PButton(200, "Back", 27, main), new Coord(270, 360));
        quality.pack();

        chpanel(main);
    }

    private Dropbox<Locale> langDropdown() {
        List<Locale> languages = enumerateLanguages();
        Dropbox<Locale> lang = new Dropbox<Locale>(120, 5, 16) {
            @Override
            protected Locale listitem(int i) {
                return languages.get(i);
            }

            @Override
            protected int listitems() {
                return languages.size();
            }

            @Override
            protected void drawitem(GOut g, Locale item, int i) {
                g.text(item.getDisplayName(), Coord.z);
            }

            @Override
            public void change(Locale item) {
                super.change(item);
                Resource.language = item.toString();
                Utils.setpref("language", item.toString());
            }
        };
        lang.change(new Locale(Resource.language));
        return lang;
    }

    private static final Pair[] chatFntSz = new Pair[]{
            new Pair<>("0", 0),
            new Pair<>("1", 1),
            new Pair<>("2", 2),
            new Pair<>("3", 3)
    };

    @SuppressWarnings("unchecked")
    private Dropbox<Pair<String, Integer>> chatFntSzDropdown() {
        Dropbox<Pair<String, Integer>> sizes = new Dropbox<Pair<String, Integer>>(55, 4, 16) {
            @Override
            protected Pair<String, Integer> listitem(int i) {
                return chatFntSz[i];
            }

            @Override
            protected int listitems() {
                return chatFntSz.length;
            }

            @Override
            protected void drawitem(GOut g, Pair<String, Integer> item, int i) {
                g.text(item.a, Coord.z);
            }

            @Override
            public void change(Pair<String, Integer> item) {
                super.change(item);
                Config.chatfontsize = item.b;
                Utils.setprefi("chatfontsize", item.b);
            }
        };
        sizes.change(new Pair<String, Integer>(Config.chatfontsize + "", Config.chatfontsize));
        return sizes;
    }

    private List<Locale> enumerateLanguages() {
        Set<Locale> languages = new HashSet<>();
        languages.add(new Locale("en"));

        Enumeration<URL> en;
        try {
            en = this.getClass().getClassLoader().getResources("l10n");
            if (en.hasMoreElements()) {
                URL url = en.nextElement();
                JarURLConnection urlcon = (JarURLConnection) (url.openConnection());
                try (JarFile jar = urlcon.getJarFile()) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        String name = entries.nextElement().getName();
                        // we assume that if tooltip localization exists then the rest exist as well
                        // up to dev to make sure that it's true
                        if (name.startsWith("l10n/" + Resource.BUNDLE_TOOLTIP))
                            languages.add(new Locale(name.substring(13, 15)));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ArrayList<Locale>(languages);
    }

    public OptWnd() {
        this(true);
    }

    public void setMapSettings() {
        final String charname = gameui().chrid;

         CheckListbox boulderlist = new CheckListbox(130, 18) {
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Config.boulderssel = getselected();
                Utils.setprefsa("boulderssel_" + charname, Config.boulderssel);
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


        CheckListbox bushlist = new CheckListbox(130, 18) {
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Config.bushessel = getselected();
                Utils.setprefsa("bushessel_" + charname, Config.bushessel);
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

        CheckListbox treelist = new CheckListbox(130, 18) {
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Config.treessel = getselected();
                Utils.setprefsa("treessel_" + charname, Config.treessel);
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

        CheckListbox iconslist = new CheckListbox(130, 18) {
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Config.iconssel = getselected();
                Utils.setprefsa("iconssel_" + charname, Config.iconssel);
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
        map.pack();
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
