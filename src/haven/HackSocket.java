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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.ClosedByInterruptException;

/*
 * This is horrible. This code exists to mitigate two bugs in Sun's
 * J2SE.
 *
 * First of all, the standard java.net.Socket implementation is not
 * interruptible per the standard Thread.interrupt mechanism. It is
 * very hard to see that as anything but a bug.
 *
 * Second of all, the java.nio.channels.SocketChannel has a bug on
 * Windows that prevents it from connecting to IPv6 addresses. (The
 * reason for that bug is most likely that Sun has two completely
 * different implementations for the java.net sockets and the NIO
 * sockets, that don't seem to share a single line of code, and they
 * probably missed one of the four functions for creating a socket
 * when making the code IPv6-capable, but that's an aside.)
 *
 * So where does this leave Haven, which uses thread interruption
 * heavily, if I want to be able to connect to IPv6 addresses? Well,
 * in hack-land, of course; where else?
 */
public class HackSocket extends Socket {
    private InputStream in = null;
    private OutputStream out = null;
    private ThreadLocal<InterruptAction> ia = new ThreadLocal<InterruptAction>();

    private class InterruptAction implements Runnable {
        private boolean interrupted;

        public void run() {
            interrupted = true;
            try {
                HackSocket.this.close();
            } catch (IOException e) {
        /*
         * Emm, well... Yeah.
		 *
		 * If the close fails, there isn't really a
		 * lot to do about it, I guess. It's probably
		 * unreasonable to throw exceptions around on
		 * the thread calling interrupt(), though, so
		 * the best action is probably to discard this
		 * exception.
		 */
            }
        }
    }

    private void hook() {
        Thread ct = Thread.currentThread();
        if (!(ct instanceof HackThread))
            throw (new RuntimeException("Tried to use an HackSocket on a non-hacked thread."));
        final HackThread ut = (HackThread) ct;
        InterruptAction ia = new InterruptAction();
        ut.addil(ia);
        this.ia.set(ia);
    }

    private void release() throws ClosedByInterruptException {
        HackThread ut = (HackThread) Thread.currentThread();
        InterruptAction ia = this.ia.get();
        if (ia == null)
            throw (new Error("Tried to release a hacked thread without an interrupt handler."));
        ut.remil(ia);
        if (ia.interrupted) {
            ut.interrupt();
            throw (new ClosedByInterruptException());
        }
    }

    public void connect(SocketAddress address, int timeout) throws IOException {
        hook();
        try {
            super.connect(address, timeout);
        } finally {
            release();
        }
    }

    public void connect(SocketAddress address) throws IOException {
        connect(address, 0);
    }

    private class HackInputStream extends InputStream {
        private InputStream bk;

        private HackInputStream(InputStream bk) {
            this.bk = bk;
        }

        public void close() throws IOException {
            bk.close();
        }

        public int read() throws IOException {
            hook();
            try {
                return (bk.read());
            } finally {
                release();
            }
        }

        public int read(byte[] buf) throws IOException {
            hook();
            try {
                return (bk.read(buf));
            } finally {
                release();
            }
        }

        public int read(byte[] buf, int off, int len) throws IOException {
            hook();
            try {
                return (bk.read(buf, off, len));
            } finally {
                release();
            }
        }
    }

    private class HackOutputStream extends OutputStream {
        private OutputStream bk;

        private HackOutputStream(OutputStream bk) {
            this.bk = bk;
        }

        public void close() throws IOException {
            bk.close();
        }

        public void flush() throws IOException {
            hook();
            try {
                bk.flush();
            } finally {
                release();
            }
        }

        public void write(int b) throws IOException {
            hook();
            try {
                bk.write(b);
            } finally {
                release();
            }
        }

        public void write(byte[] buf) throws IOException {
            hook();
            try {
                bk.write(buf);
            } finally {
                release();
            }
        }

        public void write(byte[] buf, int off, int len) throws IOException {
            hook();
            try {
                bk.write(buf, off, len);
            } finally {
                release();
            }
        }
    }

    public InputStream getInputStream() throws IOException {
        synchronized (this) {
            if (in == null)
                in = new HackInputStream(super.getInputStream());
            return (in);
        }
    }

    public OutputStream getOutputStream() throws IOException {
        synchronized (this) {
            if (out == null)
                out = new HackOutputStream(super.getOutputStream());
            return (out);
        }
    }
}
