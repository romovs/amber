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
import java.awt.Graphics2D;

public abstract class Profile {
    public static final Color[] cols;
    public final Frame[] hist;
    protected int i = 0;

    static {
        cols = new Color[16];
        for (int i = 0; i < 16; i++) {
            int lo = ((i & 8) == 0) ? 0x00 : 0x55;
            int hi = ((i & 8) == 0) ? 0xaa : 0xff;
            int r = ((i & 4) != 0) ? hi : lo;
            int g = ((i & 2) != 0) ? hi : lo;
            int b = ((i & 1) != 0) ? hi : lo;
            cols[i] = new Color(r, g, b);
        }
    }

    public abstract class Frame {
        public String nm[];
        public double total, prt[];

        protected void fin(double total, String[] nm, double[] prt) {
            this.nm = nm;
            this.total = total;
            this.prt = prt;
            hist[i] = this;
            if (++i >= hist.length)
                i = 0;
        }

        public String toString() {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < prt.length; i++) {
                if (i > 0)
                    buf.append(", ");
                buf.append(nm[i] + ": " + prt[i]);
            }
            buf.append(", total: " + total);
            return (buf.toString());
        }
    }

    public Profile(int hl) {
        hist = new Frame[hl];
    }

    public Frame last() {
        if (i == 0)
            return (hist[hist.length - 1]);
        return (hist[i - 1]);
    }

    public void draw(TexIM tex, double scale) {
        int h = tex.sz().y;
        Graphics2D g = tex.graphics();
        g.setBackground(new Color(0, 0, 0, 0));
        g.clearRect(0, 0, tex.sz().x, h);
        for (int i = 0; i < hist.length; i++) {
            Frame f = hist[i];
            if (f == null)
                continue;
            double a = 0;
            for (int o = 0; o < f.prt.length; o++) {
                double c = a + f.prt[o];
                g.setColor(cols[o + 1]);
                g.drawLine(i, (int) (h - (a / scale)), i, (int) (h - (c / scale)));
                a = c;
            }
        }
        tex.update();
    }

    public void dump(java.io.PrintStream out) {
        String[] parts = new String[0];
        double[] avg = new double[0];
        double[] min = new double[0];
        double[] max = new double[0];
        int n = 0;
        for (Frame f : hist) {
            if (f == null)
                continue;
            for (int i = 0; i <= f.prt.length; i++) {
                String nm;
                double tm;
                if (i < f.prt.length) {
                    nm = f.nm[i];
                    tm = f.prt[i];
                } else {
                    nm = "total";
                    tm = f.total;
                }
                int o;
                for (o = 0; o < parts.length; o++) {
                    if (parts[o].equals(nm))
                        break;
                }
                if (o < parts.length) {
                    avg[o] += tm;
                    min[o] = Math.min(min[o], tm);
                    max[o] = Math.max(max[o], tm);
                } else {
                    parts = Utils.extend(parts, parts.length + 1);
                    avg = Utils.extend(avg, avg.length + 1);
                    min = Utils.extend(min, min.length + 1);
                    max = Utils.extend(max, max.length + 1);
                    parts[o] = nm;
                    avg[o] = min[o] = max[o] = tm;
                }
            }
            n++;
        }
        for (int i = 0; i < avg.length; i++)
            avg[i] /= n;
        double[] vsum = new double[avg.length];
        for (Frame f : hist) {
            if (f == null)
                continue;
            for (int i = 0; i <= f.prt.length; i++) {
                String nm;
                double tm;
                if (i < f.prt.length) {
                    nm = f.nm[i];
                    tm = f.prt[i];
                } else {
                    nm = "total";
                    tm = f.total;
                }
                int o;
                for (o = 0; o < parts.length; o++) {
                    if (parts[o].equals(nm))
                        break;
                }
                vsum[o] += Math.pow(avg[o] - tm, 2);
            }
        }
        for (int i = 0; i < vsum.length; i++)
            vsum[i] = Math.sqrt(vsum[i] / n);
        for (int i = 0; i < parts.length; i++)
            out.println(String.format("%s: %.2f\u00b1%.2f (%.2f-%.2f)", parts[i], avg[i] * 1000, vsum[i] * 1000, min[i] * 1000, max[i] * 1000));
        out.println();
    }
}
