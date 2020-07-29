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
import java.awt.event.KeyEvent;
import java.util.*;
import java.text.Collator;

public class BuddyWnd extends Widget implements Iterable<BuddyWnd.Buddy> {
    private List<Buddy> buddies = new ArrayList<Buddy>();
    private Map<Integer, Buddy> idmap = new HashMap<Integer, Buddy>();
    private BuddyList bl;
    private Button sbalpha;
    private Button sbgroup;
    private Button sbstatus;
    private TextEntry pname, charpass, opass, search;
    private FlowerMenu menu;
    private BuddyInfo info = null;
    private Widget infof;
    public int serial = 0;
    public static final int width = 263;
    public static final Tex online = Resource.loadtex("gfx/hud/online");
    public static final Tex offline = Resource.loadtex("gfx/hud/offline");
    public static final Color[] gc = new Color[]{
            new Color(255, 255, 255),
            new Color(0, 255, 0),
            new Color(255, 0, 0),
            new Color(166, 206, 248),
            new Color(0, 255, 255),
            new Color(255, 255, 0),
            new Color(255, 0, 255),
            new Color(255, 0, 128),
    };
    private Comparator<Buddy> bcmp;
    private Comparator<Buddy> alphacmp = new Comparator<Buddy>() {
        private Collator c = Collator.getInstance();

        public int compare(Buddy a, Buddy b) {
            return (c.compare(a.name, b.name));
        }
    };
    private Comparator<Buddy> groupcmp = new Comparator<Buddy>() {
        public int compare(Buddy a, Buddy b) {
            if (a.group == b.group) return (alphacmp.compare(a, b));
            else return (a.group - b.group);
        }
    };
    private Comparator<Buddy> statuscmp = new Comparator<Buddy>() {
        public int compare(Buddy a, Buddy b) {
            if (a.online == b.online) return (alphacmp.compare(a, b));
            else return (b.online - a.online);
        }
    };

    @RName("buddy")
    public static class $_ implements Factory {
        public Widget create(UI ui, Object[] args) {
            return (new BuddyWnd());
        }
    }

    public class Buddy {
        public int id;
        public String name;
        Text rname = null;
        public int online;
        public int group;
        public boolean seen;

        public Buddy(int id, String name, int online, int group, boolean seen) {
            this.id = id;
            this.name = name;
            this.online = online;
            this.group = group;
            this.seen = seen;
        }

        public void forget() {
            wdgmsg("rm", id);
        }

        public void endkin() {
            wdgmsg("rm", id);
        }

        public void chat() {
            wdgmsg("chat", id);
        }

        public void invite() {
            wdgmsg("inv", id);
        }

        public void describe() {
            wdgmsg("desc", id);
        }

        public void chname(String name) {
            wdgmsg("nick", id, name);
        }

        public void chgrp(int grp) {
            wdgmsg("grp", id, grp);
        }

        public Text rname() {
            if ((rname == null) || !rname.text.equals(name))
                rname = Text.render(name);
            return (rname);
        }

        private void chstatus(int status) {
            online = status;
        }

        public Map<String, Runnable> opts() {
            Map<String, Runnable> opts = new LinkedHashMap<>();
            if(online >= 0) {
                opts.put("Chat", this::chat);
                if(online == 1)
                    opts.put("Invite", this::invite);
                opts.put("End kinship", this::endkin);
            } else {
                opts.put("Forget", this::forget);
            }
            if(seen)
                opts.put("Describe", this::describe);
            return(opts);
        }
    }

    public Iterator<Buddy> iterator() {
        synchronized (buddies) {
            return (new ArrayList<Buddy>(buddies).iterator());
        }
    }

    public Buddy find(int id) {
        synchronized (buddies) {
            return (idmap.get(id));
        }
    }

    public static class GroupSelector extends Widget {
        public int group;

        public GroupSelector(int group) {
            super(new Coord((gc.length * 20) + 20, 20));
            this.group = group;
        }

        public void draw(GOut g) {
            for (int i = 0; i < gc.length; i++) {
                if (i == group) {
                    g.chcolor();
                    g.frect(new Coord(i * 20, 0), new Coord(19, 19));
                }
                g.chcolor(gc[i]);
                g.frect(new Coord(2 + (i * 20), 2), new Coord(15, 15));
            }
            g.chcolor();
        }

        public boolean mousedown(Coord c, int button) {
            if ((c.y >= 2) && (c.y < 17)) {
                int g = (c.x - 2) / 20;
                if ((g >= 0) && (g < gc.length) && (c.x >= 2 + (g * 20)) && (c.x < 17 + (g * 20))) {
                    changed(g);
                    return (true);
                }
            }
            return (super.mousedown(c, button));
        }

        protected void changed(int group) {
            this.group = group;
        }
    }

    private class BuddyInfo extends Widget {
        private final Buddy buddy;
        private final Avaview ava;
        private final TextEntry nick;
        private final GroupSelector grp;
        private long atime, utime;
        private Label atimel = null;
        private Button[] opts = {};

        private BuddyInfo(Coord sz, Buddy buddy) {
            super(sz);
            this.buddy = buddy;
            this.ava = adda(new Avaview(Avaview.dasz, -1, "avacam"), sz.x / 2, 10, 0.5, 0);
            this.nick = add(new TextEntry(sz.x - 20, buddy.name) {
                {dshow = true;}
                public void activate(String text) {
                    buddy.chname(text);
                    commit();
                }
            }, 10, ava.c.y + ava.sz.y + 10);
            this.grp = add(new GroupSelector(buddy.group) {
                public void changed(int group) {
                    this.group = group;
                    buddy.chgrp(group);
                }
            }, 15, nick.c.y + nick.sz.y + 10);
            setopts();
        }

        public void draw(GOut g) {
            g.chcolor(0, 0, 0, 128);
            g.frect(Coord.z, sz);
            g.chcolor();
            super.draw(g);
        }

        public void tick(double dt) {
            if((utime != 0) && (Utils.ntime() >= utime))
                setatime();
        }

        private void setatime() {
            String text;
            if(buddy.online == 1) {
                this.utime = 0;
                text = "Last seen: Now";
            } else {
                int au, atime = (int)((long)Utils.ntime() - this.atime);
                String unit;
                if(atime >= (604800 * 2)) {
                    au = 604800;
                    unit = "week";
                } else if(atime >= 86400) {
                    au = 86400;
                    unit = "day";
                } else if(atime >= 3600) {
                    au = 3600;
                    unit = "hour";
                } else if(atime >= 60) {
                    au = 60;
                    unit = "minute";
                } else {
                    au = 1;
                    unit = "second";
                }
                int am = atime / au;
                this.utime = this.atime + ((am + 1) * au);
                text = "Last seen: " + am + " " + unit + ((am > 1)?"s":"") + " ago";
            }
            if(atimel != null)
                ui.destroy(atimel);
            atimel = add(new Label(text), 10, grp.c.y + grp.sz.y + 10);
        }

        private void setopts() {
            for(Button opt : this.opts)
                ui.destroy(opt);
            Map<String, Runnable> bopts = buddy.opts();
            List<Button> opts = new ArrayList<>(bopts.size());
            int y = grp.c.y + grp.sz.y + 35;
            for(Map.Entry<String, Runnable> opt : bopts.entrySet()) {
                Button btn = add(new Button(sz.x - 20, opt.getKey(), false, opt.getValue()), 10, y);
                y = btn.c.y + btn.sz.y + 5;
                opts.add(btn);
            }
            this.opts = opts.toArray(new Button[0]);
        }

        public void uimsg(String msg, Object... args) {
            if(msg == "i-ava") {
                Composited.Desc desc = Composited.Desc.decode(ui.sess, (Object[])args[0]);
                Resource.Resolver map = new Resource.Resolver.ResourceMap(ui.sess, (Object[])args[1]);
                ava.pop(desc, map);
            } else if(msg == "i-atime") {
                atime = (long)Utils.ntime() - ((Number)args[0]).longValue();
                setatime();
            } else {
                super.uimsg(msg, args);
            }
        }

        public void update() {
            nick.settext(buddy.name);
            nick.commit();
            grp.group = buddy.group;
            setatime();
            setopts();
        }
    }

    private class BuddyList extends Searchbox<Buddy> {
        public String filter;

        public BuddyList(int w, int h) {
            super(w, h, 20);
        }

        public Buddy listitem(int idx) {
            if (filter == null || filter == "")
                return buddies.get(idx);

            int num = 0;
            synchronized (buddies) {
                for (int i = 0; i < buddies.size(); i++) {
                    if (buddies.get(i).name.toLowerCase().contains(filter) && num++ == idx)
                        return buddies.get(i);
                }
            }

            return buddies.get(idx);
        }

        public int listitems() {
            if (filter == null || filter == "")
                return buddies.size();

            int num = 0;
            synchronized (buddies) {
                for (int i = 0; i < buddies.size(); i++) {
                    if (buddies.get(i).name.toLowerCase().contains(filter))
                        num++;
                }
            }
            return num;
        }
        public boolean searchmatch(int idx, String txt) {return(buddies.get(idx).name.toLowerCase().indexOf(txt.toLowerCase()) >= 0);}

        protected void drawbg(GOut g) {
            g.chcolor(0, 0, 0, 128);
            g.frect(Coord.z, sz);
            g.chcolor();
        }

        public void drawitem(GOut g, Buddy b, int idx) {
            if(soughtitem(idx)) {
                g.chcolor(255, 255, 0, 32);
                g.frect(Coord.z, g.sz);
                g.chcolor();
            }
            if (b.online == 1)
                g.image(online, Coord.z);
            else if (b.online == 0)
                g.image(offline, Coord.z);
            g.chcolor(gc[b.group]);
            g.aimage(b.rname().tex(), new Coord(25, 10), 0, 0.5);
            g.chcolor();
        }

        public void draw(GOut g) {
            super.draw(g);
            if (buddies.size() == 0)
                g.atext("You are alone in the world", sz.div(2), 0.5, 0.5);
        }

        public void change(Buddy b) {
            if(b == null) {
                BuddyWnd.this.wdgmsg("ch", (Object)null);
            } else {
                BuddyWnd.this.wdgmsg("ch", b.id);
            }
        }

        public void opts(final Buddy b, Coord c) {
            if(menu == null) {
                Map<String, Runnable> bopts = b.opts();
                menu = new FlowerMenu(bopts.keySet().toArray(new String[0])) {
                    public void destroy() {
                        menu = null;
                        super.destroy();
                    }

                    public void choose(Petal opt) {
                        if(opt != null) {
                            Runnable act = bopts.get(opt.name);
                            if(act != null)
                                act.run();
                            uimsg("act", opt.num);
                        } else {
                            uimsg("cancel");
                        }
                    }
                };
                ui.root.add(menu, c);
            }
        }

        public void itemclick(Buddy b, int button) {
            if (button == 1) {
                change(b);
            } else if (button == 3) {
                opts(b, ui.mc);
            }
        }
    }

    public BuddyWnd() {
        super(new Coord(width, 380));
        setfocustab(true);
        int y = 0;
        add(new Img(CharWnd.catf.render(Resource.getLocString(Resource.BUNDLE_LABEL, "Kin")).tex()), new Coord(0, 0));
        search = add(new TextEntry(110, "") {
            @Override
            public boolean keydown(KeyEvent ev) {
                if (!parent.visible)
                    return false;

                boolean ret = buf.key(ev);
                bl.filter = text.toLowerCase();
                return ret;
            }
        }, new Coord(width - 110, y + 5));
        y += 35;

        bl = add(new BuddyList(width - Window.wbox.bisz().x, 7), new Coord(Window.wbox.btloff().x, y));
        Frame.around(this, Collections.singletonList(bl));
        y += 150;

        add(new Label("Sort by:"), new Coord(0, y));
        y += 15;

        int sbw = (width - 20) / 3;

        sbstatus = add(new Button(sbw, "Status") {
            public void click() {
                setcmp(statuscmp);
            }
        }, new Coord(0, y));
        sbgroup = add(new Button(sbw, "Group") {
            public void click() {
                setcmp(groupcmp);
            }
        }, new Coord(sbw + 10, y));
        sbalpha = add(new Button(sbw, "Name") {
            public void click() {
                setcmp(alphacmp);
            }
        }, new Coord(width - sbw, y));
        String sort = Utils.getpref("buddysort", "");
        if (sort.equals("")) {
            bcmp = statuscmp;
        } else {
            if (sort.equals("alpha")) bcmp = alphacmp;
            if (sort.equals("group")) bcmp = groupcmp;
            if (sort.equals("status")) bcmp = statuscmp;
        }
        y += 35;

        add(new Label("Presentation name:"), new Coord(0, y));
        y += 15;
        pname = add(new TextEntry(width, "") {
            {
                dshow = true;
            }

            public void activate(String text) {
                setpname(text);
            }
        }, new Coord(0, y));
        y += 25;
        add(new Button(75, "Set") {
            public void click() {
                setpname(pname.text);
            }
        }, new Coord(0, y));
        y += 35;

        add(new Label("My hearth secret:"), new Coord(0, y));
        y += 15;
        charpass = add(new TextEntry(width, "") {
            {
                dshow = true;
            }

            public void activate(String text) {
                setpwd(text);
            }
        }, new Coord(0, y));
        y += 25;
        add(new Button(sbw, "Set") {
            public void click() {
                setpwd(charpass.text);
            }
        }, new Coord(0, y));
        add(new Button(sbw, "Clear") {
            public void click() {
                setpwd("");
            }
        }, new Coord(sbw + 10, y));
        add(new Button(sbw, "Random") {
            public void click() {
                setpwd(randpwd());
            }
        }, new Coord(width - sbw, y));
        y += 35;

        add(new Label("Make kin by hearth secret:"), new Coord(0, y));
        y += 15;
        opass = add(new TextEntry(width, "") {
            public void activate(String text) {
                BuddyWnd.this.wdgmsg("bypwd", text);
                settext("");
            }
        }, new Coord(0, y));
        y += 25;
        add(new Button(75, "Add kin") {
            public void click() {
                BuddyWnd.this.wdgmsg("bypwd", opass.text);
                opass.settext("");
            }
        }, new Coord(0, y));
        pack();
    }

    private String randpwd() {
        String charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < 8; i++)
            buf.append(charset.charAt((int) (Math.random() * charset.length())));
        return (buf.toString());
    }

    public void setpwd(String pass) {
        wdgmsg("pwd", pass);
        charpass.settext(pass);
        charpass.commit();
    }

    public void setpname(String name) {
        wdgmsg("pname", name);
        pname.settext(name);
        pname.commit();
    }

    private void setcmp(Comparator<Buddy> cmp) {
        bcmp = cmp;
        String val = "";
        if (cmp == alphacmp) val = "alpha";
        if (cmp == groupcmp) val = "group";
        if (cmp == statuscmp) val = "status";
        Utils.setpref("buddysort", val);
        synchronized (buddies) {
            Collections.sort(buddies, bcmp);
        }
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "add") {
            int id = (Integer) args[0];
            String name = ((String) args[1]).intern();
            int online = (Integer) args[2];
            int group = (Integer) args[3];
            boolean seen = ((Integer) args[4]) != 0;
            Buddy b = new Buddy(id, name, online, group, seen);
            synchronized (buddies) {
                buddies.add(b);
                idmap.put(b.id, b);
                Collections.sort(buddies, bcmp);
            }
            serial++;
        } else if (msg == "rm") {
            int id = (Integer) args[0];
            Buddy b;
            synchronized (buddies) {
                b = idmap.get(id);
                if (b != null) {
                    buddies.remove(b);
                    idmap.remove(id);
                }
            }
            serial++;
        } else if (msg == "chst") {
            int id = (Integer) args[0];
            int online = (Integer) args[1];
            Buddy b = find(id);
            b.chstatus(online);
            if ((info != null) && (info.buddy == b))
                info.update();
            if (Config.notifykinonline)
                gameui().msg(b.name + " is " + (online > 0 ? "ONLINE" : "offline"), new Color(54, 105, 205));
        } else if (msg == "upd") {
            int id = (Integer) args[0];
            String name = (String) args[1];
            int online = (Integer) args[2];
            int grp = (Integer) args[3];
            boolean seen = ((Integer) args[4]) != 0;
            Buddy b = find(id);
            synchronized (b) {
                b.name = name;
                b.online = online;
                b.group = grp;
                b.seen = seen;
            }
            serial++;
        } else if (msg == "sel") {
            int id = (Integer) args[0];
            Window p = getparent(Window.class);
            if (p != null) {
                p.show();
                p.raise();
            }
            bl.change(find(id));
        } else if (msg == "pwd") {
            charpass.settext((String) args[0]);
            charpass.buf.point = charpass.buf.line.length();
            charpass.commit();
        } else if (msg == "pname") {
            pname.settext((String) args[0]);
            pname.buf.point = pname.buf.line.length();
            pname.commit();
        } else if(msg == "i-set") {
            Buddy b = (args[0] == null) ? null : find((Integer)args[0]);
            bl.sel = b;
            if((info == null) || (info.buddy != b)) {
                if(info != null) {
                    ui.destroy(info);
                    ui.destroy(infof);
                    info = null;
                    pack();
                }
                if(b != null) {
                    info = add(new BuddyInfo(new Coord(225, sz.y - 35 - Window.wbox.bisz().y), b), width + 20, 35);
                    infof = Frame.around(this, Collections.singletonList(info));
                }
                pack();
            }
        } else if(msg.substring(0, 2).equals("i-")) {
            if(info != null)
                info.uimsg(msg, args);
        } else {
            super.uimsg(msg, args);
        }
    }

    public void hide() {
        if (menu != null) {
            ui.destroy(menu);
            menu = null;
        }
        super.hide();
    }

    public void destroy() {
        if (menu != null) {
            ui.destroy(menu);
            menu = null;
        }
        super.destroy();
    }

    public String getCharName() {
        return pname != null ? pname.text : null;
    }

    public void clearSearch() {
        search.settext("");
        bl.filter = null;
    }
}
