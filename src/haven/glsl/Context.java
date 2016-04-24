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

package haven.glsl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Context {
    public final Map<Symbol, String> symtab = new HashMap<Symbol, String>();
    public final Map<String, Symbol> rsymtab = new HashMap<String, Symbol>();
    public int symgen = 1;
    public List<Toplevel> vardefs = new LinkedList<Toplevel>();
    public List<Toplevel> fundefs = new LinkedList<Toplevel>();
    public Set<String> exts = new HashSet<String>();

    public void walk(Walker w) {
        for (Toplevel el : vardefs)
            w.el(el);
        for (Toplevel el : fundefs)
            w.el(el);
    }

    public void output(Output out) {
        out.write("#version 120\n\n");
        for (String ext : exts)
            out.write("#extension " + ext + ": require\n");
        for (Toplevel tl : vardefs)
            tl.output(out);
        if (!vardefs.isEmpty())
            out.write("\n");
        for (Toplevel tl : fundefs)
            tl.output(out);
    }
}
