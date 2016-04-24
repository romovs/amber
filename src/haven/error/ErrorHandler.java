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

package haven.error;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;

public class ErrorHandler extends ThreadGroup {
    private static final String[] sysprops = {
            "java.version",
            "java.vendor",
            "os.name",
            "os.arch",
            "os.version",
    };
    private final ThreadGroup initial;
    private Map<String, Object> props = new HashMap<String, Object>();
    private Reporter reporter;

    public static ErrorHandler find() {
        for (ThreadGroup tg = Thread.currentThread().getThreadGroup(); tg != null; tg = tg.getParent()) {
            if (tg instanceof ErrorHandler)
                return ((ErrorHandler) tg);
        }
        return (null);
    }

    public void lsetprop(String key, Object val) {
        props.put(key, val);
    }

    private class Reporter extends Thread {
        private Queue<Report> errors = new LinkedList<Report>();
        private ErrorStatus status;

        public Reporter(ErrorStatus status) {
            super(initial, "Error reporter");
            setDaemon(true);
            this.status = status;
        }

        public void run() {
            while (true) {
                synchronized (errors) {
                    try {
                        errors.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                    Report r;
                    while ((r = errors.poll()) != null) {
                        try {
                            doreport(r);
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }

        private void doreport(Report r) throws IOException {
            if (!status.goterror(r.t))
                return;
            status.done(null, null);
        }

        public void report(Thread th, Throwable t) {
            Report r = new Report(t);
            r.props.putAll(props);
            r.props.put("thnm", th.getName());
            r.props.put("thcl", th.getClass().getName());
            synchronized (errors) {
                errors.add(r);
                errors.notifyAll();
            }
            try {
                r.join();
            } catch (InterruptedException e) { /* XXX? */ }
        }
    }

    private void defprops() {
        for (String p : sysprops)
            props.put(p, System.getProperty(p));
        Runtime rt = Runtime.getRuntime();
        props.put("cpus", rt.availableProcessors());
        InputStream in = ErrorHandler.class.getResourceAsStream("/buildinfo");
        try {
            try {
                if (in != null) {
                    Properties info = new Properties();
                    info.load(in);
                    for (Map.Entry<Object, Object> e : info.entrySet())
                        props.put("jar." + (String) e.getKey(), e.getValue());
                }
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw (new Error(e));
        }
    }

    public ErrorHandler(ErrorStatus ui) {
        super("Haven client");
        initial = Thread.currentThread().getThreadGroup();
        reporter = new Reporter(ui);
        reporter.start();
        defprops();
    }

    public ErrorHandler() {
        this(new ErrorStatus.Simple());
    }

    public void sethandler(ErrorStatus handler) {
        reporter.status = handler;
    }

    public void uncaughtException(Thread t, Throwable e) {
        reporter.report(t, e);
    }
}
