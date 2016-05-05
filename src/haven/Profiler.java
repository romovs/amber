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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Profiler {
    private static Loop loop;
    public final Thread th;
    private boolean enabled;

    public Profiler(Thread th) {
        this.th = th;
    }

    public Profiler() {
        this(Thread.currentThread());
    }

    public void enable() {
        if (Thread.currentThread() != th)
            throw (new RuntimeException("Enabled from non-owning thread"));
        if (enabled)
            throw (new RuntimeException("Enabled when already enabled"));
        if (loop == null) {
            synchronized (Loop.class) {
                if (loop == null) {
                    loop = new Loop();
                    loop.start();
                }
            }
        }
        synchronized (loop.current) {
            loop.current.add(this);
        }
        enabled = true;
    }

    public void disable() {
        if (Thread.currentThread() != th)
            throw (new RuntimeException("Disabled from non-owning thread"));
        if (!enabled)
            throw (new RuntimeException("Disabled when already disabled"));
        synchronized (loop.current) {
            loop.current.remove(this);
        }
        enabled = false;
    }

    public static class Function {
        public final String cl, nm;
        public int dticks, iticks;
        public Map<Function, Integer> tticks = new HashMap<Function, Integer>();
        public Map<Function, Integer> fticks = new HashMap<Function, Integer>();
        public Map<Integer, Integer> lticks = new HashMap<Integer, Integer>();

        public Function(String cl, String nm) {
            this.cl = cl;
            this.nm = nm;
        }

        public Function(StackTraceElement f) {
            this(f.getClassName(), f.getMethodName());
        }

        public boolean equals(Object bp) {
            if (!(bp instanceof Function))
                return (false);
            Function b = (Function) bp;
            return (b.cl.equals(cl) && b.nm.equals(nm));
        }

        private int hc = 0;

        public int hashCode() {
            if (hc == 0)
                hc = cl.hashCode() * 31 + nm.hashCode();
            return (hc);
        }
    }

    private Map<Function, Function> funs = new HashMap<Function, Function>();
    private int nticks = 0;

    private Function getfun(StackTraceElement f) {
        Function key = new Function(f);
        Function ret = funs.get(key);
        if (ret == null) {
            ret = key;
            funs.put(ret, ret);
        }
        return (ret);
    }

    protected void tick(StackTraceElement[] bt) {
        nticks++;
        Function pf = getfun(bt[0]);
        pf.dticks++;
        if (pf.lticks.containsKey(bt[0].getLineNumber()))
            pf.lticks.put(bt[0].getLineNumber(), pf.lticks.get(bt[0].getLineNumber()) + 1);
        else
            pf.lticks.put(bt[0].getLineNumber(), 1);
        for (int i = 1; i < bt.length; i++) {
            StackTraceElement f = bt[i];
            Function fn = getfun(f);
            fn.iticks++;
            if (fn.tticks.containsKey(pf))
                fn.tticks.put(pf, fn.tticks.get(pf) + 1);
            else
                fn.tticks.put(pf, 1);
            if (pf.fticks.containsKey(fn))
                pf.fticks.put(fn, pf.fticks.get(fn) + 1);
            else
                pf.fticks.put(fn, 1);
            pf = fn;
        }
        System.err.print(".");
    }

    public void outputlp(OutputStream out, String cl, String fnm) {
        Function fn = funs.get(new Function(cl, fnm));
        if (fn == null)
            return;
        Map<Integer, Integer> lt = fn.lticks;
        PrintStream p = new PrintStream(out);
        List<Integer> lines = new ArrayList<Integer>(lt.keySet());
        Collections.sort(lines);
        for (int ln : lines) {
            p.printf("%d: %d\n", ln, lt.get(ln));
        }
        p.println();
    }

    public void output(OutputStream out) {
        PrintStream p = new PrintStream(out);
        List<Function> funs = new ArrayList<Function>(this.funs.keySet());
        Collections.sort(funs, new Comparator<Function>() {
            public int compare(Function a, Function b) {
                return (b.dticks - a.dticks);
            }
        });
        p.println("Functions sorted by direct ticks:");
        for (Function fn : funs) {
            if (fn.dticks < 1)
                continue;
            p.print("    ");
            String nm = fn.cl + "." + fn.nm;
            p.print(nm);
            for (int i = nm.length(); i < 60; i++)
                p.print(" ");
            p.printf("%6d (%5.2f%%)", fn.dticks, 100.0 * (double) fn.dticks / (double) nticks);
            p.println();
        }
        p.println();
        Collections.sort(funs, new Comparator<Function>() {
            public int compare(Function a, Function b) {
                return ((b.iticks + b.dticks) - (a.iticks + a.dticks));
            }
        });
        p.println("Functions sorted by direct and indirect ticks:");
        for (Function fn : funs) {
            p.print("    ");
            String nm = fn.cl + "." + fn.nm;
            p.print(nm);
            for (int i = nm.length(); i < 60; i++)
                p.print(" ");
            p.printf("%6d (%5.2f%%)", fn.iticks + fn.dticks, 100.0 * (double) (fn.iticks + fn.dticks) / (double) nticks);
            p.println();
        }
        p.println();
        p.println("Per-function time spent in callees:");
        for (Function fn : funs) {
            p.printf("  %s.%s\n", fn.cl, fn.nm);
            List<Map.Entry<Function, Integer>> cfs = new ArrayList<Map.Entry<Function, Integer>>(fn.tticks.entrySet());
            if (fn.dticks > 0)
                cfs.add(new AbstractMap.SimpleEntry<Function, Integer>(null, fn.dticks));
            Collections.sort(cfs, new Comparator<Map.Entry<Function, Integer>>() {
                public int compare(Map.Entry<Function, Integer> a, Map.Entry<Function, Integer> b) {
                    return (b.getValue() - a.getValue());
                }
            });
            for (Map.Entry<Function, Integer> cf : cfs) {
                p.print("    ");
                String nm;
                if (cf.getKey() == null)
                    nm = "<direct ticks>";
                else
                    nm = cf.getKey().cl + "." + cf.getKey().nm;
                p.print(nm);
                for (int i = nm.length(); i < 60; i++)
                    p.print(" ");
                p.printf("%6d (%5.2f%%)", cf.getValue(), 100.0 * (double) cf.getValue() / (double) (fn.dticks + fn.iticks));
                p.println();
            }
            p.println();
        }
        p.println();
        p.println("Per-function time spent by caller:");
        for (Function fn : funs) {
            p.printf("  %s.%s\n", fn.cl, fn.nm);
            List<Map.Entry<Function, Integer>> cfs = new ArrayList<Map.Entry<Function, Integer>>(fn.fticks.entrySet());
            Collections.sort(cfs, new Comparator<Map.Entry<Function, Integer>>() {
                public int compare(Map.Entry<Function, Integer> a, Map.Entry<Function, Integer> b) {
                    return (b.getValue() - a.getValue());
                }
            });
            for (Map.Entry<Function, Integer> cf : cfs) {
                p.print("    ");
                String nm = cf.getKey().cl + "." + cf.getKey().nm;
                p.print(nm);
                for (int i = nm.length(); i < 60; i++)
                    p.print(" ");
                p.printf("%6d (%5.2f%%)", cf.getValue(), 100.0 * (double) cf.getValue() / (double) (fn.dticks + fn.iticks));
                p.println();
            }
            p.println();
        }
    }

    public void output(String path) {
        try {
            OutputStream out = new FileOutputStream(path);
            try {
                output(out);
            } finally {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class Loop extends HackThread {
        private Collection<Profiler> current = new LinkedList<Profiler>();

        Loop() {
            super("Profiling thread");
            setDaemon(true);
        }

        public void run() {
            try {
                while (true) {
                    Thread.sleep(100);
                    Collection<Profiler> copy;
                    synchronized (current) {
                        copy = new ArrayList<Profiler>(current);
                    }
                    for (Profiler p : copy) {
                        StackTraceElement[] bt = p.th.getStackTrace();
                        if (!p.enabled)
                            continue;
                        p.tick(bt);
                    }
                }
            } catch (InterruptedException e) {
            }
        }
    }
}
