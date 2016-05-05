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

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;

public class Defer extends ThreadGroup {
    private static final Map<ThreadGroup, Defer> groups = new WeakHashMap<ThreadGroup, Defer>();
    private final Queue<Future<?>> queue = new PrioQueue<Future<?>>();
    private final Collection<Thread> pool = new LinkedList<Thread>();
    private static final int maxthreads = 2;

    public interface Callable<T> {
        public T call() throws InterruptedException;
    }

    @SuppressWarnings("serial")
	public static class CancelledException extends RuntimeException {

		public CancelledException() {
            super("Execution cancelled");
        }

        public CancelledException(Throwable cause) {
            super(cause);
        }
    }

    @SuppressWarnings("serial")
	public static class DeferredException extends RuntimeException {
        public DeferredException(Throwable cause) {
            super(cause);
        }
    }

    @SuppressWarnings("serial")
	public static class NotDoneException extends Loading {
        public final transient Future<?> future;

        public NotDoneException(Future<?> future) {
            this.future = future;
        }

        public NotDoneException(Future<?> future, Loading cause) {
            super(cause);
            this.future = future;
        }

        public String getMessage() {
            if (rec != null) {
                String msg = rec.getMessage();
                if (msg != null)
                    return (msg);
            }
            if (future == null)
                return (null);
            String msg = future.task.toString();
            if ((msg == null) && (future.running == null))
                return ("Waiting on job queue...");
            if (msg == null)
                return (null);
            if (future.running == null)
                msg = msg + " (queued)";
            return (msg);
        }

        public boolean canwait() {
            return (true);
        }

        public void waitfor() throws InterruptedException {
            synchronized (future) {
                while (!future.done())
                    future.wait();
            }
        }
    }

    public class Future<T> implements Runnable, Prioritized {
        public final Callable<T> task;
        private final AccessControlContext secctx;
        private int prio = 0;
        private T val;
        private volatile String state = "";
        private Throwable exc = null;
        private Loading lastload = null;
        private Thread running = null;

        private Future(Callable<T> task) {
            this.task = task;
            this.secctx = AccessController.getContext();
        }

        public void cancel() {
            synchronized (this) {
                if (running != null) {
                    running.interrupt();
                } else if (state != "done") {
                    exc = new CancelledException();
                    chstate("done");
                }
            }
        }

        private void chstate(String nst) {
            synchronized (this) {
                this.state = nst;
                notifyAll();
            }
        }

        public void run() {
            synchronized (this) {
                if (state == "done")
                    return;
                running = Thread.currentThread();
            }
            try {
                try {
                    val = AccessController.doPrivileged(new PrivilegedExceptionAction<T>() {
                        public T run() throws InterruptedException {
                            return (task.call());
                        }
                    }, secctx);
                } catch (PrivilegedActionException we) {
                    if (we.getException() instanceof InterruptedException)
                        throw ((InterruptedException) we.getException());
                    throw (new RuntimeException(we.getException()));
                }
                lastload = null;
                chstate("done");
            } catch (InterruptedException exc) {
                this.exc = new CancelledException(exc);
                chstate("done");
            } catch (Loading exc) {
                lastload = exc;
            } catch (Throwable exc) {
                this.exc = exc;
                chstate("done");
            } finally {
                if (state != "done")
                    chstate("resched");
                running = null;
            }
        }

        public T get() {
            synchronized (this) {
                boostprio(5);
                if (state == "done") {
                    if (exc != null)
                        throw (new DeferredException(exc));
                    return (val);
                }
                if (state == "resched") {
                    defer(this);
                    state = "";
                }
                throw (new NotDoneException(this, lastload));
            }
        }

        public boolean done() {
            synchronized (this) {
                boostprio(5);
                if (state == "resched") {
                    defer(this);
                    state = "";
                }
                return (state == "done");
            }
        }

        public int priority() {
            return (prio);
        }

        public void boostprio(int prio) {
            synchronized (this) {
                if (this.prio < prio)
                    this.prio = prio;
            }
        }
    }

    private class Worker extends HackThread {
        private Worker() {
            super(Defer.this, null, "Worker thread");
            setDaemon(true);
        }

        public void run() {
            try {
                while (true) {
                    Future<?> f;
                    try {
                        long start = System.currentTimeMillis();
                        synchronized (queue) {
                            while ((f = queue.poll()) == null) {
                                if (System.currentTimeMillis() - start > 5000)
                                    return;
                                queue.wait(1000);
                            }
                        }
                    } catch (InterruptedException e) {
                        return;
                    }
                    f.run();
                    f = null;
                }
            } finally {
                synchronized (queue) {
                    pool.remove(this);
                    if ((pool.size() < 1) && !queue.isEmpty()) {
                        Thread n = new Worker();
                        n.start();
                        pool.add(n);
                    }
                }
            }
        }
    }

    public Defer(ThreadGroup parent) {
        super(parent, "DPC threads");
    }

    private void defer(final Future<?> f) {
        synchronized (queue) {
            boolean e = queue.isEmpty();
            queue.add(f);
            queue.notify();
            if ((pool.isEmpty() || !e) && (pool.size() < maxthreads)) {
                Thread n = AccessController.doPrivileged(new PrivilegedAction<Thread>() {
                    public Thread run() {
                        Thread ret = new Worker();
                        ret.start();
                        return (ret);
                    }
                });
                pool.add(n);
            }
        }
    }

    public <T> Future<T> defer(Callable<T> task) {
        Future<T> f = new Future<T>(task);
        defer(f);
        return (f);
    }

    private static Defer getgroup() {
        return (AccessController.doPrivileged(new PrivilegedAction<Defer>() {
            public Defer run() {
                ThreadGroup tg = Thread.currentThread().getThreadGroup();
                if (tg instanceof Defer)
                    return ((Defer) tg);
                Defer d;
                synchronized (groups) {
                    if ((d = groups.get(tg)) == null)
                        groups.put(tg, d = new Defer(tg));
                }
                return (d);
            }
        }));
    }

    public static <T> Future<T> later(Callable<T> task) {
        Defer d = getgroup();
        return (d.defer(task));
    }
}
