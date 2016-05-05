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

import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.AttributedString;
import java.util.Map;

public class AttributedStringBuffer {
    private AttributedString current = new AttributedString("");

    public static String gettext(AttributedCharacterIterator s) {
        StringBuilder tbuf = new StringBuilder();
        for (int i = s.getBeginIndex(); i < s.getEndIndex(); i++)
            tbuf.append(s.setIndex(i));
        return (tbuf.toString());
    }

    public static void dump(AttributedCharacterIterator s, java.io.PrintStream out) {
        int cl = 0;
        Map<? extends Attribute, ?> attrs = null;
        for (int i = s.getBeginIndex(); i < s.getEndIndex(); i++) {
            char c = s.setIndex(i);
            if (i >= cl) {
                attrs = s.getAttributes();
                out.println();
                out.println(attrs);
                cl = s.getRunLimit();
            }
            out.print(c);
        }
        out.println();
    }

    public static AttributedString concat(AttributedCharacterIterator... strings) {
        StringBuilder tbuf = new StringBuilder();
        for (int i = 0; i < strings.length; i++) {
            AttributedCharacterIterator s = strings[i];
            for (int o = s.getBeginIndex(); o < s.getEndIndex(); o++)
                tbuf.append(s.setIndex(o));
        }
        AttributedString res = new AttributedString(tbuf.toString());
        int ro = 0;
        for (int i = 0; i < strings.length; i++) {
            AttributedCharacterIterator s = strings[i];
            int o = s.getBeginIndex();
            while (o < s.getEndIndex()) {
                s.setIndex(o);
                int n = s.getRunLimit();
                int l = n - o;
                res.addAttributes(s.getAttributes(), ro, ro + l);
                o = n;
                ro += l;
            }
        }
        return (res);
    }

    public static AttributedString concat(AttributedString... strings) {
        AttributedCharacterIterator[] its = new AttributedCharacterIterator[strings.length];
        for (int i = 0; i < strings.length; i++)
            its[i] = strings[i].getIterator();
        return (concat(its));
    }

    public void append(AttributedString string) {
        this.current = concat(this.current, string);
    }

    public void append(String string, Map<? extends Attribute, ?> attrs) {
        append(new AttributedString(string, attrs));
    }

    public AttributedString result() {
        return (current);
    }
}
