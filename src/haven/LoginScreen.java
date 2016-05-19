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

public class LoginScreen extends Widget {
    Login cur;
    Text error;
    IButton btn;
    Button optbtn;
    OptWnd opts;
    static Text.Foundry textf, textfs;
    static Tex bg = Resource.loadtex("gfx/loginscr");
    Text progress = null;

    static {
        textf = new Text.Foundry(Text.sans, 16).aa(true);
        textfs = new Text.Foundry(Text.sans, 14).aa(true);
    }

    public LoginScreen() {
        super(bg.sz());
        setfocustab(true);
        add(new Img(bg), Coord.z);
        optbtn = adda(new Button(100, "Options"), sz.x-110, 40, 0, 1);
        new UpdateChecker().start();
        LoginList ll = new LoginList(new Coord(10, 10), new Coord(200, this.sz.y-20), this);
        this.add(ll);
        ll.show();
        ll.raise();
        GameUI.swimon = false;
        GameUI.trackon = false;
        GameUI.crimeon = false;
    }

    private static abstract class Login extends Widget {
        abstract Object[] data();

        abstract boolean enter();
    }

    private class Pwbox extends Login {
        TextEntry user, pass;

        private Pwbox(String username, boolean save) {
            setfocustab(true);
            add(new Label("User name", textf), Coord.z);
            add(user = new TextEntry(150, username), new Coord(0, 20));
            add(new Label("Password", textf), new Coord(0, 50));
            add(pass = new TextEntry(150, ""), new Coord(0, 70));
            pass.pw = true;
            if (user.text.equals(""))
                setfocus(user);
            else
                setfocus(pass);
            resize(new Coord(150, 150));
            LoginScreen.this.add(this, new Coord(345, 310));
        }

        public void wdgmsg(Widget sender, String name, Object... args) {
        }

        Object[] data() {
            return (new Object[]{new AuthClient.NativeCred(user.text, pass.text), false});
        }

        boolean enter() {
            if (user.text.equals("")) {
                setfocus(user);
                return (false);
            } else if (pass.text.equals("")) {
                setfocus(pass);
                return (false);
            } else {
                return (true);
            }
        }

        public boolean globtype(char k, KeyEvent ev) {
            if ((k == 'r') && ((ev.getModifiersEx() & (KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) != 0)) {
                return (true);
            }
            return (false);
        }
    }

    private class Tokenbox extends Login {
        Text label;
        Button btn;

        private Tokenbox(String username) {
            label = textfs.render("Identity is saved for " + username, java.awt.Color.WHITE);
            add(btn = new Button(100, "Forget me"), new Coord(75, 30));
            resize(new Coord(250, 100));
            LoginScreen.this.add(this, new Coord(295, 330));
        }

        Object[] data() {
            return (new Object[0]);
        }

        boolean enter() {
            return (true);
        }

        public void wdgmsg(Widget sender, String name, Object... args) {
            if (sender == btn) {
                LoginScreen.this.wdgmsg("forget");
                return;
            }
            super.wdgmsg(sender, name, args);
        }

        public void draw(GOut g) {
            g.image(label.tex(), new Coord((sz.x / 2) - (label.sz().x / 2), 0));
            super.draw(g);
        }

        public boolean globtype(char k, KeyEvent ev) {
            if ((k == 'f') && ((ev.getModifiersEx() & (KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) != 0)) {
                LoginScreen.this.wdgmsg("forget");
                return (true);
            }
            return (false);
        }
    }

    private static class LoginList extends Widget {
        private static final int ITEM_HEIGHT = 20;
        private static final Text.Foundry specialFnd = new Text.Foundry(Text.special, 14);
        private static final Text.Foundry fnd = new Text.Foundry(Text.sans, 14);
        private Tex xicon = Text.render("\u2716", Color.RED, specialFnd).tex();
        private LoginData curLD;

        public LoginList(Coord c, Coord sz, Widget parent) {
            super(parent.ui, c, sz);
            curLD = null;
        }

        public void draw(GOut g) {
            g.chcolor(0, 0, 0, 128);
            g.frect(Coord.z, sz);
            g.chcolor();

            synchronized(Config.logins) {
                if(Config.logins.size() > 0) {
                    for(int i = 0; i < Config.logins.size(); i++) {
                        LoginData ld = Config.logins.get(i);
                        if(ld == curLD) {
                            g.chcolor(96, 96, 96, 255);
                            g.frect(new Coord(0, i * ITEM_HEIGHT), new Coord(sz.x-30, ITEM_HEIGHT));
                            g.chcolor();
                        }
                        g.aimage(Text.render(ld.name, Color.WHITE, fnd).tex(), new Coord(10, i * ITEM_HEIGHT + 10), 0, 0.5);
                        g.aimage(xicon, new Coord(sz.x - 20, i * ITEM_HEIGHT + 10), 0, 0.5);
                    }
                }
            }
            super.draw(g);
        }

        public boolean mousedown(Coord c, int button) {
            if(super.mousedown(c, button))
                return(true);
            if(button == 1) {
                int sel = (c.y / ITEM_HEIGHT);
                synchronized(Config.logins) {
                    if(sel < Config.logins.size() && sel >= 0) {
                        curLD = Config.logins.get(sel);
                        if (c.x >= sz.x - 25 && c.x <= sz.x - 25 + 20) {
                            synchronized(Config.logins) {
                                Config.logins.remove(curLD);
                                Config.saveLogins();
                                curLD = null;
                            }
                        } else if (c.x < sz.x - 35) {
                            parent.wdgmsg("forget");
                            parent.wdgmsg("login", new Object[]{new AuthClient.NativeCred(curLD.name, curLD.pass), false});
                        }
                    }
                }
                return(true);
            }
            return(false);
        }
    }

    private void mklogin() {
        synchronized (ui) {
            adda(btn = new IButton("gfx/hud/buttons/login", "u", "d", "o") {
                protected void depress() {
                    Audio.play(Button.lbtdown.stream());
                }

                protected void unpress() {
                    Audio.play(Button.lbtup.stream());
                }
            }, 419, 510, 0.5, 0.5);
            progress(null);
        }
    }

    private void error(String error) {
        synchronized (ui) {
            if (this.error != null)
                this.error = null;
            if (error != null)
                this.error = textf.render(error, java.awt.Color.RED);
        }
    }

    private void progress(String p) {
        synchronized (ui) {
            if (progress != null)
                progress = null;
            if (p != null)
                progress = textf.render(p, java.awt.Color.WHITE);
        }
    }

    private void clear() {
        if (cur != null) {
            ui.destroy(cur);
            cur = null;
            ui.destroy(btn);
            btn = null;
        }
        progress(null);
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == btn) {
            if (cur.enter())
                super.wdgmsg("login", cur.data());
            return;
        } else if (sender == optbtn) {
            if (opts == null) {
                opts = adda(new OptWnd(false) {
                    public void hide() {
                /* XXX */
                        reqdestroy();
                    }
                }, sz.div(2), 0.5, 0.5);
            } else {
                opts.reqdestroy();
                opts = null;
            }
            return;
        } else if (sender == opts) {
            opts.reqdestroy();
            opts = null;
        }
        super.wdgmsg(sender, msg, args);
    }

    public void cdestroy(Widget ch) {
        if (ch == opts) {
            opts = null;
        }
    }

    public void uimsg(String msg, Object... args) {
        synchronized (ui) {
            if (msg == "passwd") {
                clear();
                cur = new Pwbox((String) args[0], (Boolean) args[1]);
                mklogin();
            } else if (msg == "token") {
                clear();
                cur = new Tokenbox((String) args[0]);
                mklogin();
            } else if (msg == "error") {
                error((String) args[0]);
            } else if (msg == "prg") {
                error(null);
                clear();
                progress((String) args[0]);
            }
        }
    }

    public void presize() {
        c = parent.sz.div(2).sub(sz.div(2));
    }

    protected void added() {
        presize();
        parent.setfocus(this);
    }

    public void draw(GOut g) {
        super.draw(g);
        if (error != null)
            g.image(error.tex(), new Coord(420 - (error.sz().x / 2), 450));
        if (progress != null)
            g.image(progress.tex(), new Coord(420 - (progress.sz().x / 2), 350));
    }

    public boolean type(char k, KeyEvent ev) {
        if (k == 10) {
            if ((cur != null) && cur.enter())
                wdgmsg("login", cur.data());
            return (true);
        }
        return (super.type(k, ev));
    }
}
