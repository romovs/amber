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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

public class UI {
    public RootWidget root;
    final private LinkedList<Grab> keygrab = new LinkedList<Grab>(), mousegrab = new LinkedList<Grab>();
    public Map<Integer, Widget> widgets = new TreeMap<Integer, Widget>();
    public Map<Widget, Integer> rwidgets = new HashMap<Widget, Integer>();
    Receiver rcvr;
    public Coord mc = Coord.z, lcc = Coord.z;
    public Session sess;
    public boolean modshift, modctrl, modmeta, modsuper;
    public int keycode;
    public Object lasttip;
    long lastevent, lasttick;
    public Widget mouseon;
    public Console cons = new WidgetConsole();
    private Collection<AfterDraw> afterdraws = new LinkedList<AfterDraw>();
    public final ActAudio audio = new ActAudio();
    public GameUI gui = null;

    public static String fmAutoSelName = "";
    public static long fmAutoTime;
    private static final int FM_AUTO_TIMEOUT = 2000;

    {
        lastevent = lasttick = System.currentTimeMillis();
    }

    public interface Receiver {
        public void rcvmsg(int widget, String msg, Object... args);
    }

    public interface Runner {
        public Session run(UI ui) throws InterruptedException;
    }

    public interface AfterDraw {
        public void draw(GOut g);
    }

    private class WidgetConsole extends Console {
        {
            setcmd("q", new Command() {
                public void run(Console cons, String[] args) {
                    HackThread.tg().interrupt();
                }
            });
            setcmd("lo", new Command() {
                public void run(Console cons, String[] args) {
                    sess.close();
                }
            });
        }

        private void findcmds(Map<String, Command> map, Widget wdg) {
            if (wdg instanceof Directory) {
                Map<String, Command> cmds = ((Directory) wdg).findcmds();
                synchronized (cmds) {
                    map.putAll(cmds);
                }
            }
            for (Widget ch = wdg.child; ch != null; ch = ch.next)
                findcmds(map, ch);
        }

        public Map<String, Command> findcmds() {
            Map<String, Command> ret = super.findcmds();
            findcmds(ret, root);
            return (ret);
        }
    }

    @SuppressWarnings("serial")
    public static class UIException extends RuntimeException {
        public String mname;
        public Object[] args;

        public UIException(String message, String mname, Object... args) {
            super(message);
            this.mname = mname;
            this.args = args;
        }
    }

    public UI(Coord sz, Session sess) {
        root = new RootWidget(this, sz);
        widgets.put(0, root);
        rwidgets.put(root, 0);
        this.sess = sess;
    }

    public void setreceiver(Receiver rcvr) {
        this.rcvr = rcvr;
    }

    public void bind(Widget w, int id) {
        widgets.put(id, w);
        rwidgets.put(w, id);
        w.bound();
    }

    public void drawafter(AfterDraw ad) {
        synchronized (afterdraws) {
            afterdraws.add(ad);
        }
    }

    public void tick() throws InterruptedException {
        long now = System.currentTimeMillis();
        root.tick((now - lasttick) / 1000.0);
        lasttick = now;
    }

    public void draw(GOut g) {
        root.draw(g);
        synchronized (afterdraws) {
            for (AfterDraw ad : afterdraws)
                ad.draw(g);
            afterdraws.clear();
        }
    }

    public void newwidget(int id, String type, int parent, Object[] pargs, Object... cargs) throws InterruptedException {
        Widget.Factory f = Widget.gettype2(type);
        synchronized (this) {
            Widget pwdg = widgets.get(parent);
            if (pwdg == null)
                throw (new UIException("Null parent widget " + parent + " for " + id, type, cargs));

            if (type.equals("sm")) {
                synchronized (fmAutoSelName) {
                    if (fmAutoSelName != null && System.currentTimeMillis() - fmAutoTime < FM_AUTO_TIMEOUT) {
                        Widget w = new WidgetDummy();
                        pwdg.addchild(w, pargs);
                        bind(w, id);
                        for (int i = 0; i < cargs.length; i++) {
                            if (cargs[i].equals(fmAutoSelName)) {
                                rcvr.rcvmsg(id, "cl", new Object[]{i, 0});
                                fmAutoSelName = "";
                                return;
                            }
                        }
                        fmAutoSelName = "";
                    }
                }
            }

            Widget wdg = pwdg.makechild(f, pargs, cargs);
            bind(wdg, id);

            // drop everything except water containers if in area mining mode
            GameUI gui = pwdg.gameui();
            if (Config.dropore && gui != null && gui.map != null && gui.map.areamine != null && wdg instanceof GItem) {
                if (gui.maininv == pwdg) {
                    final GItem itm = (GItem) wdg;
                    Defer.later(new Defer.Callable<Void>() {
                        public Void call() {
                            try {
                                String name = itm.resource().name;
                                if (!name.endsWith("waterflask") && !name.endsWith("waterskin") && !name.endsWith("pebble-gold"))
                                    itm.wdgmsg("drop", Coord.z);
                            } catch (Loading e) {
                                Defer.later(this);
                            }
                            return null;
                        }
                    });
                }
            }
        }
    }

    public abstract class Grab {
        public final Widget wdg;

        public Grab(Widget wdg) {
            this.wdg = wdg;
        }

        public abstract void remove();
    }

    public Grab grabmouse(Widget wdg) {
        if (wdg == null) throw (new NullPointerException());
        Grab g = new Grab(wdg) {
            public void remove() {
                mousegrab.remove(this);
            }
        };
        mousegrab.addFirst(g);
        return (g);
    }

    public Grab grabkeys(Widget wdg) {
        if (wdg == null) throw (new NullPointerException());
        Grab g = new Grab(wdg) {
            public void remove() {
                keygrab.remove(this);
            }
        };
        keygrab.addFirst(g);
        return (g);
    }

    private void removeid(Widget wdg) {
        if (rwidgets.containsKey(wdg)) {
            int id = rwidgets.get(wdg);
            widgets.remove(id);
            rwidgets.remove(wdg);
        }
        for (Widget child = wdg.child; child != null; child = child.next)
            removeid(child);
    }

    public void destroy(Widget wdg) {
        for (Iterator<Grab> i = mousegrab.iterator(); i.hasNext(); ) {
            Grab g = i.next();
            if (g.wdg.hasparent(wdg))
                i.remove();
        }
        for (Iterator<Grab> i = keygrab.iterator(); i.hasNext(); ) {
            Grab g = i.next();
            if (g.wdg.hasparent(wdg))
                i.remove();
        }
        removeid(wdg);
        wdg.reqdestroy();
    }

    public void destroy(int id) {
        synchronized (this) {
            if (widgets.containsKey(id)) {
                Widget wdg = widgets.get(id);
                destroy(wdg);
            }
        }
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
        int id;
        synchronized (this) {
            if (msg.endsWith("-identical"))
                return;
            if (!rwidgets.containsKey(sender))
                throw (new UIException("Wdgmsg sender (" + sender.getClass().getName() + ") is not in rwidgets", msg, args));
            id = rwidgets.get(sender);
        }
        if (rcvr != null)
            rcvr.rcvmsg(id, msg, args);
    }

    public void uimsg(int id, String msg, Object... args) {
        synchronized (this) {
            Widget wdg = widgets.get(id);
            if (wdg != null)
                wdg.uimsg(msg.intern(), args);
            else
                throw (new UIException("Uimsg to non-existent widget " + id, msg, args));
        }
    }

    private void setmods(InputEvent ev) {
        int mod = ev.getModifiersEx();
        Debug.kf1 = modshift = (mod & InputEvent.SHIFT_DOWN_MASK) != 0;
        Debug.kf2 = modctrl = (mod & InputEvent.CTRL_DOWN_MASK) != 0;
        Debug.kf3 = modmeta = (mod & (InputEvent.META_DOWN_MASK | InputEvent.ALT_DOWN_MASK)) != 0;
    /*
    Debug.kf4 = modsuper = (mod & InputEvent.SUPER_DOWN_MASK) != 0;
	*/
    }

    private Grab[] c(Collection<Grab> g) {
        return (g.toArray(new Grab[0]));
    }

    public void type(KeyEvent ev) {
        setmods(ev);
        for (Grab g : c(keygrab)) {
            if (g.wdg.type(ev.getKeyChar(), ev))
                return;
        }
        if (!root.type(ev.getKeyChar(), ev))
            root.globtype(ev.getKeyChar(), ev);
    }

    public void keydown(KeyEvent ev) {
        setmods(ev);
        keycode = ev.getKeyCode();
        for (Grab g : c(keygrab)) {
            if (g.wdg.keydown(ev))
                return;
        }
        if (!root.keydown(ev))
            root.globtype((char) 0, ev);
    }

    public void keyup(KeyEvent ev) {
        setmods(ev);
        keycode = -1;
        for (Grab g : c(keygrab)) {
            if (g.wdg.keyup(ev))
                return;
        }
        root.keyup(ev);
    }

    private Coord wdgxlate(Coord c, Widget wdg) {
        return (c.add(wdg.c.inv()).add(wdg.parent.rootpos().inv()));
    }

    public boolean dropthing(Widget w, Coord c, Object thing) {
        if (w instanceof DropTarget) {
            if (((DropTarget) w).dropthing(c, thing))
                return (true);
        }
        for (Widget wdg = w.lchild; wdg != null; wdg = wdg.prev) {
            Coord cc = w.xlate(wdg.c, true);
            if (c.isect(cc, wdg.sz)) {
                if (dropthing(wdg, c.add(cc.inv()), thing))
                    return (true);
            }
        }
        return (false);
    }

    public void mousedown(MouseEvent ev, Coord c, int button) {
        setmods(ev);
        lcc = mc = c;
        for (Grab g : c(mousegrab)) {
            if (g.wdg.mousedown(wdgxlate(c, g.wdg), button))
                return;
        }
        root.mousedown(c, button);
    }

    public void mouseup(MouseEvent ev, Coord c, int button) {
        setmods(ev);
        mc = c;
        for (Grab g : c(mousegrab)) {
            if (g.wdg.mouseup(wdgxlate(c, g.wdg), button))
                return;
        }
        root.mouseup(c, button);
    }

    public void mousemove(MouseEvent ev, Coord c) {
        setmods(ev);
        mc = c;
        root.mousemove(c);
    }

    public void mousewheel(MouseEvent ev, Coord c, int amount) {
        setmods(ev);
        lcc = mc = c;
        for (Grab g : c(mousegrab)) {
            if (g.wdg.mousewheel(wdgxlate(c, g.wdg), amount))
                return;
        }
        root.mousewheel(c, amount);
    }

    public int modflags() {
        return ((modshift ? 1 : 0) |
                (modctrl ? 2 : 0) |
                (modmeta ? 4 : 0) |
                (modsuper ? 8 : 0));
    }
    
    public void message(String str, Color msgColor) {
	if((cons!=null) && (gui!=null)){
	    gui.msg(str, msgColor);
	}
    }

    public void destroy() {
        audio.clear();
    }
}
