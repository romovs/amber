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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class PosixArgs {
    private List<Arg> parsed;
    public String[] rest;
    public String arg = null;

    private static class Arg {
        private char ch;
        private String arg;

        private Arg(char ch, String arg) {
            this.ch = ch;
            this.arg = arg;
        }
    }

    private PosixArgs() {
        parsed = new ArrayList<Arg>();
    }

    public static PosixArgs getopt(String[] argv, int start, String desc) {
        PosixArgs ret = new PosixArgs();
        List<Character> fl = new ArrayList<Character>(), fla = new ArrayList<Character>();
        List<String> rest = new ArrayList<String>();
        for (int i = 0; i < desc.length(); ) {
            char ch = desc.charAt(i++);
            if ((i < desc.length()) && (desc.charAt(i) == ':')) {
                i++;
                fla.add(ch);
            } else {
                fl.add(ch);
            }
        }
        boolean acc = true;
        for (int i = start; i < argv.length; ) {
            String arg = argv[i++];
            if (acc && arg.equals("--")) {
                acc = false;
            }
            if (acc && (arg.charAt(0) == '-')) {
                for (int o = 1; o < arg.length(); ) {
                    char ch = arg.charAt(o++);
                    if (fl.contains(ch)) {
                        ret.parsed.add(new Arg(ch, null));
                    } else if (fla.contains(ch)) {
                        if (o < arg.length()) {
                            ret.parsed.add(new Arg(ch, arg.substring(o)));
                            break;
                        } else if (i < argv.length) {
                            ret.parsed.add(new Arg(ch, argv[i++]));
                            break;
                        } else {
                            System.err.println("option requires an argument -- '" + ch + "'");
                            return (null);
                        }
                    } else {
                        System.err.println("invalid option -- '" + ch + "'");
                        return (null);
                    }
                }
            } else {
                rest.add(arg);
            }
        }
        ret.rest = rest.toArray(new String[0]);
        return (ret);
    }

    public static PosixArgs getopt(String[] argv, String desc) {
        return (getopt(argv, 0, desc));
    }

    public Iterable<Character> parsed() {
        return (new Iterable<Character>() {
            public Iterator<Character> iterator() {
                return (new Iterator<Character>() {
                    private int i = 0;

                    public boolean hasNext() {
                        return (i < parsed.size());
                    }

                    public Character next() {
                        if (i >= parsed.size())
                            throw (new NoSuchElementException());
                        Arg a = parsed.get(i++);
                        arg = a.arg;
                        return (a.ch);
                    }

                    public void remove() {
                        throw (new UnsupportedOperationException());
                    }
                });
            }
        });
    }
}
