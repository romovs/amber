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

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public abstract class ConsoleHost extends Widget {
    public static Text.Foundry cmdfoundry = new Text.Foundry(Text.mono, 12, new java.awt.Color(245, 222, 179));
    LineEdit cmdline = null;
    private Text.Line cmdtext = null;
    private String cmdtextf = null;
    private List<String> history = new ArrayList<String>();
    private int hpos = history.size();
    private String hcurrent;
    private UI.Grab kg;

    private class CommandLine extends LineEdit {
        private CommandLine() {
            super();
        }

        private CommandLine(String line) {
            super(line);
        }

        private void cancel() {
            cmdline = null;
            kg.remove();
        }

        protected void done(String line) {
            history.add(line);
            try {
                ui.cons.run(line);
            } catch (Exception e) {
                String msg = e.getMessage();
                if (msg == null)
                    msg = e.toString();
                ui.cons.out.println(msg);
                error(msg);
            }
            cancel();
        }

        public boolean key(char c, int code, int mod) {
            if (c == 27) {
                cancel();
            } else if ((c == 8) && (mod == 0) && (line.length() == 0) && (point == 0)) {
                cancel();
            } else if (code == KeyEvent.VK_UP) {
                if (hpos > 0) {
                    if (hpos == history.size())
                        hcurrent = line;
                    cmdline = new CommandLine(history.get(--hpos));
                }
            } else if (code == KeyEvent.VK_DOWN) {
                if (hpos < history.size()) {
                    if (++hpos == history.size())
                        cmdline = new CommandLine(hcurrent);
                    else
                        cmdline = new CommandLine(history.get(hpos));
                }
            } else {
                return (super.key(c, code, mod));
            }
            return (true);
        }
    }

    public ConsoleHost(Coord sz) {
        super(sz);
    }

    public ConsoleHost() {
    }

    public ConsoleHost(UI ui, Coord c, Coord sz) {
        super(ui, c, sz);
    }

    public void drawcmd(GOut g, Coord c) {
        if (cmdline != null) {
            if ((cmdtext == null) || (cmdtextf != cmdline.line))
                cmdtext = cmdfoundry.render(":" + (cmdtextf = cmdline.line));
            g.image(cmdtext.tex(), c);
            int lx = cmdtext.advance(cmdline.point + 1);
            g.line(c.add(lx + 1, 2), c.add(lx + 1, 14), 1);
        }
    }

    public void entercmd() {
        kg = ui.grabkeys(this);
        hpos = history.size();
        cmdline = new CommandLine();
    }

    public boolean type(char ch, KeyEvent ev) {
        if (cmdline == null) {
            return (super.type(ch, ev));
        } else {
            cmdline.key(ev);
            return (true);
        }
    }

    public boolean keydown(KeyEvent ev) {
        if (cmdline != null) {
            cmdline.key(ev);
            return (true);
        }
        return (super.keydown(ev));
    }

    public abstract void error(String msg);
}
