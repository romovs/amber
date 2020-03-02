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


import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.prefs.BackingStoreException;
import java.util.stream.Collectors;

public class OptWnd extends Window {
    public static final int VERTICAL_MARGIN = 10;
    public static final int HORIZONTAL_MARGIN = 5;
    public static final int VERTICAL_AUDIO_MARGIN = 5;
    public final Panel main, video, audio, display, map, general, combat, control, uis, quality, flowermenus, soundalarms, keybind;
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

        public boolean keydown(java.awt.event.KeyEvent ev) {
            if((this.key != -1) && (ev.getKeyChar() == this.key)) {
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
            add(new PButton(200, "Back", 27, back), new Coord(210, 360));
            resize(new Coord(620, 400));
        }

        public class CPanel extends Widget {
            public final GLSettings cf;

            public CPanel(GLSettings gcf) {
                this.cf = gcf;
                final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(this, new Coord(620, 350)));
                appender.setVerticalMargin(VERTICAL_MARGIN);
                appender.setHorizontalMargin(HORIZONTAL_MARGIN);
                appender.add(new CheckBox("Per-fragment lighting") {
                    {
                        a = cf.flight.val;
                    }

                    public void set(boolean val) {
                        if (val) {
                            try {
                                cf.flight.set(true);
                            } catch (GLSettings.SettingException e) {
                                GameUI gui = getparent(GameUI.class);
                                if (gui != null)
                                    gui.error(e.getMessage());
                                return;
                            }
                        } else {
                            cf.flight.set(false);
                        }
                        a = val;
                        cf.dirty = true;
                    }
                });
                appender.add(new CheckBox("Render shadows") {
                    {
                        a = cf.lshadow.val;
                    }

                    public void set(boolean val) {
                        if (val) {
                            try {
                                cf.lshadow.set(true);
                            } catch (GLSettings.SettingException e) {
                                GameUI gui = getparent(GameUI.class);
                                if (gui != null)
                                    gui.error(e.getMessage());
                                return;
                            }
                        } else {
                            cf.lshadow.set(false);
                        }
                        a = val;
                        cf.dirty = true;
                    }
                });
                appender.add(new CheckBox("Antialiasing") {
                    {
                        a = cf.fsaa.val;
                    }

                    public void set(boolean val) {
                        try {
                            cf.fsaa.set(val);
                        } catch (GLSettings.SettingException e) {
                            GameUI gui = getparent(GameUI.class);
                            if (gui != null)
                                gui.error(e.getMessage());
                            return;
                        }
                        a = val;
                        cf.dirty = true;
                    }
                });
                appender.add(new Label("Anisotropic filtering"));
                if (cf.anisotex.max() <= 1) {
                    appender.add(new Label("(Not supported)"));
                } else {
                    final Label dpy = new Label("");
                    appender.addRow(
                            new HSlider(160, (int) (cf.anisotex.min() * 2), (int) (cf.anisotex.max() * 2), (int) (cf.anisotex.val * 2)) {
                                protected void added() {
                                    dpy();
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
                            },
                            dpy);
                }
                appender.add(new CheckBox("Disable biome tile transitions (requires logout)") {
                    {
                        a = Config.disabletiletrans;
                    }
                    public void set(boolean val) {
                        Config.disabletiletrans = val;
                        Utils.setprefb("disabletiletrans", val);
                        a = val;
                    }
                });
                appender.add(new CheckBox("Disable terrain smoothing (requires logout)") {
                    {
                        a = Config.disableterrainsmooth;
                    }
                    public void set(boolean val) {
                        Config.disableterrainsmooth = val;
                        Utils.setprefb("disableterrainsmooth", val);
                        a = val;
                    }
                });
                appender.add(new CheckBox("Disable terrain elevation (requires logout)") {
                    {
                        a = Config.disableelev;
                    }
                    public void set(boolean val) {
                        Config.disableelev = val;
                        Utils.setprefb("disableelev", val);
                        a = val;
                    }
                });
                appender.add(new CheckBox("Disable flavor objects including ambient sounds") {
                    {
                        a = Config.hideflocomplete;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("hideflocomplete", val);
                        Config.hideflocomplete = val;
                        a = val;
                    }
                });
                appender.add(new CheckBox("Hide flavor objects but keep sounds (requires logout)") {
                    {
                        a = Config.hideflovisual;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("hideflovisual", val);
                        Config.hideflovisual = val;
                        a = val;
                    }
                });
                appender.add(new CheckBox("Show weather") {
                    {
                        a = Config.showweather;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("showweather", val);
                        Config.showweather = val;
                        a = val;
                    }
                });
                appender.add(new CheckBox("Simple crops (req. logout)") {
                    {
                        a = Config.simplecrops;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("simplecrops", val);
                        Config.simplecrops = val;
                        a = val;
                    }
                });
                appender.add(new CheckBox("Hide crops") {
                    {
                        a = Config.hidecrops;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("hidecrops", val);
                        Config.hidecrops = val;
                        a = val;
                    }
                });
                appender.add(new CheckBox("Show FPS") {
                    {
                        a = Config.showfps;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("showfps", val);
                        Config.showfps = val;
                        a = val;
                    }
                });

                appender.add(new Label("Disable animations (req. restart):"));
                CheckListbox disanimlist = new CheckListbox(320, Math.min(8, Config.disableanim.values().size()), 18 + Config.fontadd) {
                    @Override
                    protected void itemclick(CheckListboxItem itm, int button) {
                        super.itemclick(itm, button);
                        Utils.setprefchklst("disableanim", Config.disableanim);
                    }
                };
                for (CheckListboxItem itm : Config.disableanim.values())
                    disanimlist.items.add(itm);
                appender.add(disanimlist);

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
        super(new Coord(620, 400), "Options", true);

        main = add(new Panel());
        video = add(new VideoPanel(main));
        audio = add(new Panel());
        display = add(new Panel());
        map = add(new Panel());
        general = add(new Panel());
        combat = add(new Panel());
        control = add(new Panel());
        uis = add(new Panel());
        quality = add(new Panel());
        flowermenus = add(new Panel());
        soundalarms = add(new Panel());
        keybind = add(new Panel());

        initMain(gopts);
        initAudio();
        initDisplay();
        initMap();
        initGeneral();
        initCombat();
        initControl();
        initUis();
        initQuality();
        initFlowermenus();
        initSoundAlarms();
        initKeyBind();

        chpanel(main);
    }

    private void initMain(boolean gopts) {
        main.add(new PButton(200, "Video settings", 'v', video), new Coord(0, 0));
        main.add(new PButton(200, "Audio settings", 'a', audio), new Coord(0, 30));
        main.add(new PButton(200, "Display settings", 'd', display), new Coord(0, 60));
        main.add(new PButton(200, "Map settings", 'm', map), new Coord(0, 90));
        main.add(new PButton(200, "General settings", 'g', general), new Coord(210, 0));
        main.add(new PButton(200, "Combat settings", 'c', combat), new Coord(210, 30));
        main.add(new PButton(200, "Control settings", 'k', control), new Coord(210, 60));
        main.add(new PButton(200, "UI settings", 'u', uis), new Coord(210, 90));
        main.add(new PButton(200, "Quality settings", 'q', quality), new Coord(420, 0));
        main.add(new PButton(200, "Menu settings", 'f', flowermenus), new Coord(420, 30));
        main.add(new PButton(200, "Sound alarms", 's', soundalarms), new Coord(420, 60));
        main.add(new PButton(200, "Key Bindings", 'b', keybind), new Coord(420, 90));
        if (gopts) {
            main.add(new Button(200, "Switch character") {
                public void click() {
                    GameUI gui = gameui();
                    gui.act("lo", "cs");
                    if (gui != null & gui.map != null)
                        gui.map.canceltasks();
                }
            }, new Coord(210, 300));
            main.add(new Button(200, "Log out") {
                public void click() {
                    GameUI gui = gameui();
                    gui.act("lo");
                    if (gui != null & gui.map != null)
                        gui.map.canceltasks();
                }
            }, new Coord(210, 330));
        }
        main.add(new Button(200, "Close") {
            public void click() {
                OptWnd.this.hide();
            }
        }, new Coord(210, 360));
        main.pack();
    }

    private void initAudio() {
        initAudioFirstColumn();
        audio.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        audio.pack();
    }

    private void initAudioFirstColumn() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(audio, new Coord(620, 350)));
        appender.setVerticalMargin(0);
        appender.add(new Label("Master audio volume"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, (int) (Audio.volume * 1000)) {
            public void changed() {
                Audio.setvolume(val / 1000.0);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new Label("In-game event volume"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (ui.audio.pos.volume * 1000);
            }

            public void changed() {
                ui.audio.pos.setvolume(val / 1000.0);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new Label("Ambient volume"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (ui.audio.amb.volume * 1000);
            }

            public void changed() {
                ui.audio.amb.setvolume(val / 1000.0);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new Label("Timers alarm volume"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.timersalarmvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.timersalarmvol = vol;
                Utils.setprefd("timersalarmvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new Label("'Chip' sound volume"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.sfxchipvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.sfxchipvol = vol;
                Utils.setprefd("sfxchipvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new Label("Quern sound volume"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.sfxquernvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.sfxquernvol = vol;
                Utils.setprefd("sfxquernvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new Label("'Whip' sound volume"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.sfxwhipvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.sfxwhipvol = vol;
                Utils.setprefd("sfxwhipvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new Label("Fireplace sound volume (req. restart)"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.sfxfirevol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.sfxfirevol = vol;
                Utils.setprefd("sfxfirevol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new Label("Bees sound volume (req. logout)"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.sfxbeevol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.sfxbeevol = vol;
                Utils.setprefd("sfxbeevol", vol);
            }
        });
    }

    private void initDisplay() {
        initDisplayFirstColumn();
        display.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        display.pack();
    }

    private void initDisplayFirstColumn() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(display, new Coord(620, 350)));
        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.add(new CheckBox("Display kin names") {
            {
                a = Config.showkinnames;
            }

            public void set(boolean val) {
                Utils.setprefb("showkinnames", val);
                Config.showkinnames = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Display item completion progress bar") {
            {
                a = Config.itemmeterbar;
            }

            public void set(boolean val) {
                Utils.setprefb("itemmeterbar", val);
                Config.itemmeterbar = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show hourglass percentage") {
            {
                a = Config.showprogressperc;
            }

            public void set(boolean val) {
                Utils.setprefb("showprogressperc", val);
                Config.showprogressperc = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show attributes & softcap values in craft window") {
            {
                a = Config.showcraftcap;
            }

            public void set(boolean val) {
                Utils.setprefb("showcraftcap", val);
                Config.showcraftcap = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show objects health") {
            {
                a = Config.showgobhp;
            }

            public void set(boolean val) {
                Utils.setprefb("showgobhp", val);
                Config.showgobhp = val;
                a = val;

                GameUI gui = gameui();
                if (gui != null && gui.map != null) {
                    if (val)
                        gui.map.addHealthSprites();
                    else
                        gui.map.removeCustomSprites(Sprite.GOB_HEALTH_ID);
                }
            }
        });
        appender.add(new CheckBox("Show player's path") {
            {
                a = Config.showplayerpaths;
            }

            public void set(boolean val) {
                Utils.setprefb("showplayerpaths", val);
                Config.showplayerpaths = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show animal radius") {
            {
                a = Config.showanimalrad;
            }

            public void set(boolean val) {
                Utils.setprefb("showanimalrad", val);
                Config.showanimalrad = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Highlight empty/finished drying frames") {
            {
                a = Config.showdframestatus;
            }

            public void set(boolean val) {
                Utils.setprefb("showdframestatus", val);
                Config.showdframestatus = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Highlight finished garden pots") {
            {
                a = Config.highlightpots;
            }

            public void set(boolean val) {
                Utils.setprefb("highlightpots", val);
                Config.highlightpots = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Draw circles around party members") {
            {
                a = Config.partycircles;
            }

            public void set(boolean val) {
                Utils.setprefb("partycircles", val);
                Config.partycircles = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show last used curios in study window") {
            {
                a = Config.studyhist;
            }

            public void set(boolean val) {
                Utils.setprefb("studyhist", val);
                Config.studyhist = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Display buff icon when study has free slots") {
            {
                a = Config.studybuff;
            }

            public void set(boolean val) {
                Utils.setprefb("studybuff", val);
                Config.studybuff = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Miniature trees (req. logout)") {
            {
                a = Config.bonsai;
            }

            public void set(boolean val) {
                Utils.setprefb("bonsai", val);
                Config.bonsai = val;
                a = val;
            }
        });
    }

    private void initMap() {
        map.add(new Label("Show boulders:"), new Coord(10, 0));
        map.add(new Label("Show bushes:"), new Coord(165, 0));
        map.add(new Label("Show trees:"), new Coord(320, 0));
        map.add(new Label("Hide icons:"), new Coord(475, 0));

        map.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        map.pack();
    }

    private void initGeneral() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(general, new Coord(620, 350)));

        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);

        appender.add(new CheckBox("Save chat logs to disk") {
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
        });
        appender.add(new CheckBox("Save map tiles to disk") {
            {
                a = Config.savemmap;
            }

            public void set(boolean val) {
                Utils.setprefb("savemmap", val);
                Config.savemmap = val;
                MapGridSave.mgs = null;
                a = val;
            }
        });
        appender.add(new CheckBox("Show timestamps in chats") {
            {
                a = Config.chattimestamp;
            }

            public void set(boolean val) {
                Utils.setprefb("chattimestamp", val);
                Config.chattimestamp = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Notify when kin comes online") {
            {
                a = Config.notifykinonline;
            }

            public void set(boolean val) {
                Utils.setprefb("notifykinonline", val);
                Config.notifykinonline = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Auto hearth") {
            {
                a = Config.autohearth;
            }

            public void set(boolean val) {
                Utils.setprefb("autohearth", val);
                Config.autohearth = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Auto logout on unknown/red players") {
            {
                a = Config.autologout;
            }

            public void set(boolean val) {
                Utils.setprefb("autologout", val);
                Config.autologout = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Run on login") {
            {
                a = Config.runonlogin;
            }

            public void set(boolean val) {
                Utils.setprefb("runonlogin", val);
                Config.runonlogin = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show server time") {
            {
                a = Config.showservertime;
            }

            public void set(boolean val) {
                Utils.setprefb("showservertime", val);
                Config.showservertime = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Enable tracking on login") {
            {
                a = Config.enabletracking;
            }

            public void set(boolean val) {
                Utils.setprefb("enabletracking", val);
                Config.enabletracking = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Enable criminal acts on login") {
            {
                a = Config.enablecrime;
            }

            public void set(boolean val) {
                Utils.setprefb("enablecrime", val);
                Config.enablecrime = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Drop mined stones") {
            {
                a = Config.dropMinedStones;
            }

            public void set(boolean val) {
                Utils.setprefb("dropMinedStones", val);
                Config.dropMinedStones = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Drop mined ore") {
            {
                a = Config.dropMinedOre;
            }

            public void set(boolean val) {
                Utils.setprefb("dropMinedOre", val);
                Config.dropMinedOre = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Drop mined silver/gold ore") {
            {
                a = Config.dropMinedOrePrecious;
            }

            public void set(boolean val) {
                Utils.setprefb("dropMinedOrePrecious", val);
                Config.dropMinedOrePrecious = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Drop mined cat gold, petrified seashells, strange crystals") {
            {
                a = Config.dropMinedCurios;
            }

            public void set(boolean val) {
                Utils.setprefb("dropMinedCurios", val);
                Config.dropMinedCurios = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Drop everything!!!") {
            {
                a = Config.dropEverything;
            }

            public void set(boolean val) {
                //Utils.setprefb("dropEverything", val);
                Config.dropEverything = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Drop soil") {
            {
                a = Config.dropSoil;
            }

            public void set(boolean val) {
                Utils.setprefb("dropSoil", val);
                Config.dropSoil = val;
                a = val;
            }
        });
        general.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        general.pack();
    }

    private void initCombat() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(combat, new Coord(620, 350)));

        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);

        appender.add(new CheckBox("Display damage") {
            {
                a = Config.showdmgop;
            }

            public void set(boolean val) {
                Utils.setprefb("showdmgop", val);
                Config.showdmgop = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Highlight current opponent") {
            {
                a = Config.hlightcuropp;
            }

            public void set(boolean val) {
                Utils.setprefb("hlightcuropp", val);
                Config.hlightcuropp = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Display cooldown time") {
            {
                a = Config.showcooldown;
            }

            public void set(boolean val) {
                Utils.setprefb("showcooldown", val);
                Config.showcooldown = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show arrow vectors") {
            {
                a = Config.showarchvector;
            }

            public void set(boolean val) {
                Utils.setprefb("showarchvector", val);
                Config.showarchvector = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Log combat actions to system log") {
            {
                a = Config.logcombatactions;
            }

            public void set(boolean val) {
                Utils.setprefb("logcombatactions", val);
                Config.logcombatactions = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Alternative combat UI") {
            {
                a = Config.altfightui;
            }

            public void set(boolean val) {
                Utils.setprefb("altfightui", val);
                Config.altfightui = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Simplified opening indicators") {
            {
                a = Config.combaltopenings;
            }

            public void set(boolean val) {
                Utils.setprefb("combaltopenings", val);
                Config.combaltopenings = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show key bindings in combat UI") {
            {
                a = Config.combshowkeys;
            }

            public void set(boolean val) {
                Utils.setprefb("combshowkeys", val);
                Config.combshowkeys = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Aggro players in proximity to the mouse cursor") {
            {
                a = Config.proximityaggro;
            }

            public void set(boolean val) {
                Utils.setprefb("proximityaggro", val);
                Config.proximityaggro = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Aggro animals in proximity to the mouse cursor") {
            {
                a = Config.proximityaggroanimal;
            }

            public void set(boolean val) {
                Utils.setprefb("proximityaggroanimal", val);
                Config.proximityaggroanimal = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Chase small games in proximity to the mouse cursor (Alt+RMB)") {
            {
                a = Config.proximitychase;
            }

            public void set(boolean val) {
                Utils.setprefb("proximitychase", val);
                Config.proximitychase = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Lift corpses etc in proximity to the mouse cursor") {
            {
                a = Config.proximitylift;
            }

            public void set(boolean val) {
                Utils.setprefb("proximitylift", val);
                Config.proximitylift = val;
                a = val;
            }
        });

        combat.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        combat.pack();
    }

    private void initControl() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(control, new Coord(620, 350)));

        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);

        appender.addRow(new Label("Bad camera scrolling sensitivity"),
                new HSlider(50, 0, 50, 0) {
                    protected void attach(UI ui) {
                        super.attach(ui);
                        val = Config.badcamsensitivity;
                    }

                    public void changed() {
                        Config.badcamsensitivity = val;
                        Utils.setprefi("badcamsensitivity", val);
                    }
                });
        appender.add(new CheckBox("Use French (AZERTY) keyboard layout") {
            {
                a = Config.userazerty;
            }

            public void set(boolean val) {
                Utils.setprefb("userazerty", val);
                Config.userazerty = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Reverse bad camera MMB x-axis") {
            {
                a = Config.reversebadcamx;
            }

            public void set(boolean val) {
                Utils.setprefb("reversebadcamx", val);
                Config.reversebadcamx = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Reverse bad camera MMB y-axis") {
            {
                a = Config.reversebadcamy;
            }

            public void set(boolean val) {
                Utils.setprefb("reversebadcamy", val);
                Config.reversebadcamy = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Force hardware cursor (req. restart)") {
            {
                a = Config.hwcursor;
            }

            public void set(boolean val) {
                Utils.setprefb("hwcursor", val);
                Config.hwcursor = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Disable dropping items over water (overridable with Ctrl)") {
            {
                a = Config.nodropping;
            }

            public void set(boolean val) {
                Utils.setprefb("nodropping", val);
                Config.nodropping = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Disable dropping items over anywhere (overridable with Ctrl)") {
            {
                a = Config.nodropping_all;
            }

            public void set(boolean val) {
                Utils.setprefb("nodropping_all", val);
                Config.nodropping_all = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Enable full zoom-out in Ortho cam") {
            {
                a = Config.enableorthofullzoom;
            }

            public void set(boolean val) {
                Utils.setprefb("enableorthofullzoom", val);
                Config.enableorthofullzoom = val;
                a = val;
            }
        });

        control.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        control.pack();
    }

    private void initUis() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(uis, new Coord(620, 310)));

        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);

        appender.addRow(new Label("Language (req. restart):"), langDropdown());
        appender.add(new CheckBox("Show quick hand slots") {
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
        });
        appender.add(new CheckBox("Alternative equipment belt window") {
            {
                a = Config.quickbelt;
            }

            public void set(boolean val) {
                Utils.setprefb("quickbelt", val);
                Config.quickbelt = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show F-key toolbar") {
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
        });
        appender.add(new CheckBox("Show inventory on login") {
            {
                a = Config.showinvonlogin;
            }

            public void set(boolean val) {
                Utils.setprefb("showinvonlogin", val);
                Config.showinvonlogin = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show Craft/Build history toolbar") {
            {
                a = Config.histbelt;
            }

            public void set(boolean val) {
                Utils.setprefb("histbelt", val);
                Config.histbelt = val;
                a = val;
                GameUI gui = gameui();
                if (gui != null) {
                    CraftHistoryBelt histbelt = gui.histbelt;
                    if (histbelt != null) {
                        if (val)
                            histbelt.show();
                        else
                            histbelt.hide();
                    }
                }
            }
        });
        appender.add(new CheckBox("Display confirmation dialog when using magic") {
            {
                a = Config.confirmmagic;
            }

            public void set(boolean val) {
                Utils.setprefb("confirmmagic", val);
                Config.confirmmagic = val;
                a = val;
            }
        });
        appender.addRow(new Label("Tree bounding box color (6-digit HEX):"),
                new TextEntry(85, Config.treeboxclr) {
                    @Override
                    public boolean keydown(KeyEvent ev) {
                        if (!parent.visible)
                            return false;

                        boolean ret = buf.key(ev);
                        if (text.length() == 6) {
                            Color clr = Utils.hex2rgb(text);
                            if (clr != null) {
                                GobHitbox.fillclrstate = new States.ColState(clr);
                                Utils.setpref("treeboxclr", text);
                            }
                        }
                        return ret;
                    }
                }
        );
        appender.addRow(new Label("Chat font size (req. restart):"), makeFontSizeChatDropdown());
        appender.add(new CheckBox("Font antialiasing") {
            {
                a = Config.fontaa;
            }

            public void set(boolean val) {
                Utils.setprefb("fontaa", val);
                Config.fontaa = val;
                a = val;
            }
        });
        appender.addRow(new CheckBox("Custom interface font (req. restart):") {
            {
                a = Config.usefont;
            }

            public void set(boolean val) {
                Utils.setprefb("usefont", val);
                Config.usefont = val;
                a = val;
            }
        }, makeFontsDropdown());

        final Label fontAdd = new Label("");
        appender.addRow(
                new Label("Increase font size by (req. restart):"),
                new HSlider(160, 0, 3, Config.fontadd) {
                    public void added() {
                        updateLabel();
                    }
                    public void changed() {
                        Utils.setprefi("fontadd", val);
                        Config.fontadd = val;
                        updateLabel();
                    }
                    private void updateLabel() {
                        fontAdd.settext(String.format("%d", val));
                    }
                },
                fontAdd
        );

        Button resetWndBtn = new Button(220, "Reset Windows (req. logout)") {
            @Override
            public void click() {
                try {
                    for (String key : Utils.prefs().keys()) {
                        if (key.endsWith("_c")) {
                            Utils.delpref(key);
                        }
                    }
                } catch (BackingStoreException e) {
                }
                Utils.delpref("mmapc");
                Utils.delpref("mmapwndsz");
                Utils.delpref("mmapsz");
                Utils.delpref("quickslotsc");
                Utils.delpref("chatsz");
                Utils.delpref("chatvis");
                Utils.delpref("menu-visible");
                Utils.delpref("fbelt_vertical");
            }
        };
        uis.add(resetWndBtn, new Coord(620 / 2 - resetWndBtn.sz.x / 2 , 320));
        uis.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        uis.pack();
    }

    private void initQuality() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(quality, new Coord(620, 350)));
        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);
        appender.add(new CheckBox("Show item quality") {
            {
                a = Config.showquality;
            }

            public void set(boolean val) {
                Utils.setprefb("showquality", val);
                Config.showquality = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Round item quality to a whole number") {
            {
                a = Config.qualitywhole;
            }

            public void set(boolean val) {
                Utils.setprefb("qualitywhole", val);
                Config.qualitywhole = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Draw background for quality values") {
            {
                a = Config.qualitybg;
            }

            public void set(boolean val) {
                Utils.setprefb("qualitybg", val);
                Config.qualitybg = val;
                a = val;
            }
        });
        appender.addRow(
            new Label("Background transparency (req. restart):"),
            new HSlider(200, 0, 255, Config.qualitybgtransparency) {
                public void changed() {
                    Utils.setprefi("qualitybgtransparency", val);
                    Config.qualitybgtransparency = val;
                }
            });

        quality.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        quality.pack();
    }

    private void initFlowermenus() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(flowermenus, new Coord(620, 350)));

        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);

        appender.add(new CheckBox("Automatically pick all clustered mussels (auto 'Pick' needs to be enabled)") {
            {
                a = Config.autopickmussels;
            }

            public void set(boolean val) {
                Utils.setprefb("autopickmussels", val);
                Config.autopickmussels = val;
                a = val;
            }
        });
        appender.add(new Label("Automatic selecton:"));

        CheckListbox flowerlist = new CheckListbox(140, 17) {
            @Override
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Utils.setprefchklst("flowersel", Config.flowermenus);
            }
        };

        Utils.loadprefchklist("flowersel", Config.flowermenus);
        for (CheckListboxItem itm : Config.flowermenus.values())
            flowerlist.items.add(itm);
        flowermenus.add(flowerlist, new Coord(0, 50));

        flowermenus.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        flowermenus.pack();
    }

    private void initSoundAlarms() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(soundalarms, new Coord(620, 350)));

        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);

        appender.setVerticalMargin(0);
        appender.add(new CheckBox("Alarm on unknown players") {
            {
                a = Config.alarmunknown;
            }

            public void set(boolean val) {
                Utils.setprefb("alarmunknown", val);
                Config.alarmunknown = val;
                a = val;
            }
        });
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int)(Config.alarmunknownvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alarmunknownvol = vol;
                Utils.setprefd("alarmunknownvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new CheckBox("Alarm on red players") {
            {
                a = Config.alarmred;
            }

            public void set(boolean val) {
                Utils.setprefb("alarmred", val);
                Config.alarmred = val;
                a = val;
            }
        });
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.alarmredvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alarmredvol = vol;
                Utils.setprefd("alarmredvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new CheckBox("Alarm on new private/party chat") {
            {
                a = Config.chatalarm;
            }

            public void set(boolean val) {
                Utils.setprefb("chatalarm", val);
                Config.chatalarm = val;
                a = val;
            }
        });
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.chatalarmvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.chatalarmvol = vol;
                Utils.setprefd("chatalarmvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new CheckBox("Alarm when curio finishes") {
            {
                a = Config.studyalarm;
            }

            public void set(boolean val) {
                Utils.setprefb("studyalarm", val);
                Config.studyalarm = val;
                a = val;
            }
        });
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.studyalarmvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.studyalarmvol = vol;
                Utils.setprefd("studyalarmvol", vol);
            }
        });
        appender.add(new CheckBox("Alarm on trolls") {
            {
                a = Config.alarmtroll;
            }

            public void set(boolean val) {
                Utils.setprefb("alarmtroll", val);
                Config.alarmtroll = val;
                a = val;
            }
        });
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.alarmtrollvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alarmtrollvol = vol;
                Utils.setprefd("alarmtrollvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new CheckBox("Alarm on battering rams and catapults") {
            {
                a = Config.alarmbram;
            }

            public void set(boolean val) {
                Utils.setprefb("alarmbram", val);
                Config.alarmbram = val;
                a = val;
            }
        });
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.alarmbramvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alarmbramvol = vol;
                Utils.setprefd("alarmbramvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new CheckBox("Alarm on localized resources") {
            {
                a = Config.alarmlocres;
            }

            public void set(boolean val) {
                Utils.setprefb("alarmlocres", val);
                Config.alarmlocres = val;
                a = val;
            }
        });
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.alarmlocresvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alarmlocresvol = vol;
                Utils.setprefd("alarmlocresvol", vol);
            }
        });

        soundalarms.add(new Label("Alarm on"), new Coord(470, 0));
        CheckListbox itemslist = new CheckListbox(145, 18) {
            @Override
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Utils.setprefchklst("alarmitems", Config.alarmitems);
            }
        };
        for (CheckListboxItem itm : Config.alarmitems.values())
            itemslist.items.add(itm);
        soundalarms.add(itemslist, new Coord(470, 15));
        soundalarms.add(new HSlider(145, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.alarmonforagablesvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alarmonforagablesvol = vol;
                Utils.setprefd("alarmonforagablesvol", vol);
            }
        }, new Coord(470, 340));

        soundalarms.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        soundalarms.pack();
    }

    private static final Text kbtt = RichText.render("$col[255,255,0]{Escape}: Cancel input\n" +
            "$col[255,255,0]{Backspace}: Revert to default\n" +
            "$col[255,255,0]{Delete}: Disable keybinding", 0);

    private final static int KB_NAME_W = 170;

    private void initKeyBind() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(withScrollport(keybind, new Coord(620, 350)));

        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);

        appender.addRow(KB_NAME_W, new Label("Inventory"), new SetButton(175, GameUI.kb_inv));
        appender.addRow(KB_NAME_W, new Label("Equipment"), new SetButton(175, GameUI.kb_equ));
        appender.addRow(KB_NAME_W, new Label("Character sheet"), new SetButton(175, GameUI.kb_chr));
        appender.addRow(KB_NAME_W, new Label("Kith & Kin"), new SetButton(175, GameUI.kb_bud));
        appender.addRow(KB_NAME_W, new Label("Options"), new SetButton(175, GameUI.kb_opt));
        appender.addRow(KB_NAME_W, new Label("Toggle chat"), new SetButton(175, GameUI.kb_chat));
        appender.addRow(KB_NAME_W, new Label("Quick chat"), new SetButton(175, ChatUI.kb_quick));
        appender.addRow(KB_NAME_W, new Label("Take screenshot & Upload "), new SetButton(175, GameUI.kb_shoot));
        appender.addRow(KB_NAME_W, new Label("Take screenshot & Save"), new SetButton(175, GameUI.kb_shoot_save));
        appender.addRow(KB_NAME_W, new Label("Combat action 1"), new SetButton(175, Fightsess.kb_acts[0]));
        for(int i = 1; i < Fightsess.kb_acts.length; i++) {
            appender.addRow(KB_NAME_W, new Label("Combat action " + (i + 1)), new SetButton(175, Fightsess.kb_acts[i]));
        }
        appender.addRow(KB_NAME_W, new Label("Switch combat opponent"), new SetButton(175, Fightsess.kb_switch));
        appender.addRow(KB_NAME_W, new Label("Drink hotkey"), new SetButton(175, GameUI.kb_drink));

        appender.addRow(KB_NAME_W, new Label("Bind other elements..."), new PointBind(200));

        keybind.add(new PButton(200, "Back", 27, main), new Coord(210, 360));
        keybind.pack();
    }

    public static class PointBind extends Button {
        public static final String msg = "Bind other elements...";
        public static final Resource curs = Resource.local().loadwait("gfx/hud/curs/wrench");
        private UI.Grab mg, kg;
        private KeyBinding cmd;

        public PointBind(int w) {
            super(w, msg, false);
            tooltip = RichText.render("Bind a key to an element not listed above, such as an action-menu " +
                            "button. Click the element to bind, and then press the key to bind to it. " +
                            "Right-click to stop rebinding.",
                    300);
        }

        public void click() {
            if(mg == null) {
                change("Click element...");
                mg = ui.grabmouse(this);
            } else if(kg != null) {
                kg.remove();
                kg = null;
                change(msg);
            }
        }

        private boolean handle(KeyEvent ev) {
            switch(ev.getKeyCode()) {
                case KeyEvent.VK_SHIFT: case KeyEvent.VK_CONTROL: case KeyEvent.VK_ALT:
                case KeyEvent.VK_META: case KeyEvent.VK_WINDOWS:
                    return(false);
            }
            int code = ev.getKeyCode();
            if(code == KeyEvent.VK_ESCAPE) {
                return(true);
            }
            if(code == KeyEvent.VK_BACK_SPACE) {
                cmd.set(null);
                return(true);
            }
            if(code == KeyEvent.VK_DELETE) {
                cmd.set(KeyMatch.nil);
                return(true);
            }
            KeyMatch key = KeyMatch.forevent(ev, ~cmd.modign);
            if(key != null)
                cmd.set(key);
            return(true);
        }

        public boolean mousedown(Coord c, int btn) {
            if(mg == null)
                return(super.mousedown(c, btn));
            Coord gc = ui.mc;
            if(btn == 1) {
                this.cmd = KeyBinding.Bindable.getbinding(ui.root, gc);
                return(true);
            }
            if(btn == 3) {
                mg.remove();
                mg = null;
                change(msg);
                return(true);
            }
            return(false);
        }

        public boolean mouseup(Coord c, int btn) {
            if(mg == null)
                return(super.mouseup(c, btn));
            Coord gc = ui.mc;
            if(btn == 1) {
                if((this.cmd != null) && (KeyBinding.Bindable.getbinding(ui.root, gc) == this.cmd)) {
                    mg.remove();
                    mg = null;
                    kg = ui.grabkeys(this);
                    change("Press key...");
                } else {
                    this.cmd = null;
                }
                return(true);
            }
            if(btn == 3)
                return(true);
            return(false);
        }

        public Resource getcurs(Coord c) {
            if(mg == null)
                return(null);
            return(curs);
        }

        public boolean keydown(KeyEvent ev) {
            if(kg == null)
                return(super.keydown(ev));
            if(handle(ev)) {
                kg.remove();
                kg = null;
                cmd = null;
                change("Click another element...");
                mg = ui.grabmouse(this);
            }
            return(true);
        }
    }

    public class SetButton extends KeyMatch.Capture {
        public final KeyBinding cmd;

        public SetButton(int w, KeyBinding cmd) {
            super(w, cmd.key());
            this.cmd = cmd;
        }

        public void set(KeyMatch key) {
            super.set(key);
            cmd.set(key);
        }

        protected KeyMatch mkmatch(KeyEvent ev) {
            return(KeyMatch.forevent(ev, ~cmd.modign));
        }

        protected boolean handle(KeyEvent ev) {
            if (ev.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                cmd.set(null);
                super.set(cmd.key());
                return (true);
            }
            return (super.handle(ev));
        }

        public Object tooltip(Coord c, Widget prev) {
            return (kbtt.tex());
        }
    }

    private Dropbox<Locale> langDropdown() {
        List<Locale> languages = enumerateLanguages();
        List<String> values = languages.stream().map(x -> x.getDisplayName()).collect(Collectors.toList());
        return new Dropbox<Locale>(10, values) {
            {
                super.change(new Locale(Resource.language));
            }

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
                Utils.setpref("language", item.toString());
            }
        };
    }

    @SuppressWarnings("unchecked")
    private Dropbox<String> makeFontsDropdown() {
        final List<String> fonts = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        return new Dropbox<String>(8, fonts) {
            {
                super.change(Config.font);
            }

            @Override
            protected String listitem(int i) {
                return fonts.get(i);
            }

            @Override
            protected int listitems() {
                return fonts.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                Config.font = item;
                Utils.setpref("font", item);
            }
        };
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

    private static final List<Integer> fontSize = Arrays.asList(10, 11, 12, 13, 14, 15, 16);

    private Dropbox<Integer> makeFontSizeChatDropdown() {
        List<String> values = fontSize.stream().map(x -> x.toString()).collect(Collectors.toList());
        return new Dropbox<Integer>(fontSize.size(), values) {
            {
                super.change(Config.fontsizechat);
            }

            @Override
            protected Integer listitem(int i) {
                return fontSize.get(i);
            }

            @Override
            protected int listitems() {
                return fontSize.size();
            }

            @Override
            protected void drawitem(GOut g, Integer item, int i) {
                g.text(item.toString(), Coord.z);
            }

            @Override
            public void change(Integer item) {
                super.change(item);
                Config.fontsizechat = item;
                Utils.setprefi("fontsizechat", item);
            }
        };
    }

    static private Scrollport.Scrollcont withScrollport(Widget widget, Coord sz) {
        final Scrollport scroll = new Scrollport(sz);
        widget.add(scroll, new Coord(0, 0));
        return scroll.cont;
    }

    public OptWnd() {
        this(true);
    }

    public void setMapSettings() {
        final String charname = gameui().chrid;

        CheckListbox boulderlist = new CheckListbox(140, 18) {
            @Override
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Utils.setprefchklst("boulderssel_" + charname, Config.boulders);
            }
        };
        for (CheckListboxItem itm : Config.boulders.values())
            boulderlist.items.add(itm);
        map.add(boulderlist, new Coord(10, 15));

        CheckListbox bushlist = new CheckListbox(140, 18) {
            @Override
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Utils.setprefchklst("bushessel_" + charname, Config.bushes);
            }
        };
        for (CheckListboxItem itm : Config.bushes.values())
            bushlist.items.add(itm);
        map.add(bushlist, new Coord(165, 15));

        CheckListbox treelist = new CheckListbox(140, 18) {
            @Override
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Utils.setprefchklst("treessel_" + charname, Config.trees);
            }
        };
        for (CheckListboxItem itm : Config.trees.values())
            treelist.items.add(itm);
        map.add(treelist, new Coord(320, 15));

        CheckListbox iconslist = new CheckListbox(140, 18) {
            @Override
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Utils.setprefchklst("iconssel_" + charname, Config.icons);
            }
        };
        for (CheckListboxItem itm : Config.icons.values())
            iconslist.items.add(itm);
        map.add(iconslist, new Coord(475, 15));


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
