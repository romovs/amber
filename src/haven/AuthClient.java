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
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.MessageDigest;

public class AuthClient {
    private static final SslHelper ssl;
    private Socket sk;
    private InputStream skin;
    private OutputStream skout;

    static {
        ssl = new SslHelper();
        try {
            ssl.trust(SslHelper.loadX509(Resource.class.getResourceAsStream("authsrv.crt")));
        } catch (Exception e) {
            throw (new RuntimeException(e));
        }
    }

    public AuthClient(String host, int port) throws IOException {
        sk = ssl.connect(host, port);
        skin = sk.getInputStream();
        skout = sk.getOutputStream();
    }

    private static byte[] digest(byte[] pw) {
        MessageDigest dig;
        try {
            dig = MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw (new RuntimeException(e));
        }
        dig.update(pw);
        return (dig.digest());
    }

    public String trypasswd(String user, byte[] phash) throws IOException {
        Message rpl = cmd("pw", user, phash);
        String stat = rpl.string();
        if (stat.equals("ok")) {
            String acct = rpl.string();
            return (acct);
        } else if (stat.equals("no")) {
            return (null);
        } else {
            throw (new RuntimeException("Unexpected reply `" + stat + "' from auth server"));
        }
    }

    public String trytoken(String user, byte[] token) throws IOException {
        Message rpl = cmd("token", user, token);
        String stat = rpl.string();
        if (stat.equals("ok")) {
            String acct = rpl.string();
            return (acct);
        } else if (stat.equals("no")) {
            return (null);
        } else {
            throw (new RuntimeException("Unexpected reply `" + stat + "' from auth server"));
        }
    }

    public byte[] getcookie() throws IOException {
        Message rpl = cmd("cookie");
        String stat = rpl.string();
        if (stat.equals("ok")) {
            return (rpl.bytes(32));
        } else {
            throw (new RuntimeException("Unexpected reply `" + stat + "' from auth server"));
        }
    }

    public byte[] gettoken() throws IOException {
        Message rpl = cmd("mktoken");
        String stat = rpl.string();
        if (stat.equals("ok")) {
            return (rpl.bytes(32));
        } else {
            throw (new RuntimeException("Unexpected reply `" + stat + "' from auth server"));
        }
    }

    public void close() throws IOException {
        sk.close();
    }

    private void sendmsg(MessageBuf msg) throws IOException {
        if (msg.size() > 65535)
            throw (new RuntimeException("Too long message in AuthClient (" + msg.size() + " bytes)"));
        byte[] buf = new byte[msg.size() + 2];
        buf[0] = (byte) ((msg.size() & 0xff00) >> 8);
        buf[1] = (byte) (msg.size() & 0x00ff);
        msg.fin(buf, 2);
        skout.write(buf);
    }

    private void esendmsg(Object... args) throws IOException {
        MessageBuf buf = new MessageBuf();
        for (Object arg : args) {
            if (arg instanceof String) {
                buf.addstring((String) arg);
            } else if (arg instanceof byte[]) {
                buf.addbytes((byte[]) arg);
            } else {
                throw (new RuntimeException("Illegal argument to esendmsg: " + arg.getClass()));
            }
        }
        sendmsg(buf);
    }

    private static void readall(InputStream in, byte[] buf) throws IOException {
        int rv;
        for (int i = 0; i < buf.length; i += rv) {
            rv = in.read(buf, i, buf.length - i);
            if (rv < 0)
                throw (new IOException("Premature end of input"));
        }
    }

    private Message recvmsg() throws IOException {
        byte[] header = new byte[2];
        readall(skin, header);
        int len = (Utils.ub(header[0]) << 8) | Utils.ub(header[1]);
        byte[] buf = new byte[len];
        readall(skin, buf);
        return (new MessageBuf(buf));
    }

    public Message cmd(Object... args) throws IOException {
        esendmsg(args);
        return (recvmsg());
    }

    public static abstract class Credentials {
        public abstract String tryauth(AuthClient cl) throws IOException;

        public abstract String name();

        public void discard() {
        }

        protected void finalize() {
            discard();
        }
        
        @SuppressWarnings("serial")
        public static class AuthException extends RuntimeException {
            public AuthException(String msg) {
                super(msg);
            }
        }
    }

    public static class NativeCred extends Credentials {
        public final String username;
        public String pass;
        private byte[] phash;

        public NativeCred(String username, byte[] phash) {
            this.username = username;
            if ((this.phash = phash).length != 32)
                throw (new IllegalArgumentException("Password hash must be 32 bytes"));
        }

        private static byte[] ohdearjava(String a) {
            try {
                return (digest(a.getBytes("utf-8")));
            } catch (UnsupportedEncodingException e) {
                throw (new RuntimeException(e));
            }
        }

        public NativeCred(String username, String pw) {
            this(username, ohdearjava(pw));
            this.pass = pw;
        }

        public String name() {
            return (username);
        }

        public String tryauth(AuthClient cl) throws IOException {
            Message rpl = cl.cmd("pw", username, phash);
            String stat = rpl.string();
            if (stat.equals("ok")) {
                String acct = rpl.string();
                return (acct);
            } else if (stat.equals("no")) {
                String err = rpl.string();
                throw (new AuthException(err));
            } else {
                throw (new RuntimeException("Unexpected reply `" + stat + "' from auth server"));
            }
        }

        public void discard() {
            if (phash != null) {
                for (int i = 0; i < phash.length; i++)
                    phash[i] = 0;
                phash = null;
            }
        }
    }

    @SuppressWarnings("serial")
	public static class TokenCred extends Credentials implements Serializable {
        public final String acctname;
        public final byte[] token;

        public TokenCred(String acctname, byte[] token) {
            this.acctname = acctname;
            if ((this.token = token).length != 32)
                throw (new IllegalArgumentException("Token must be 32 bytes"));
        }

        public String name() {
            throw (new UnsupportedOperationException());
        }

        public String tryauth(AuthClient cl) throws IOException {
            Message rpl = cl.cmd("token", acctname, token);
            String stat = rpl.string();
            if (stat.equals("ok")) {
                String acct = rpl.string();
                return (acct);
            } else if (stat.equals("no")) {
                String err = rpl.string();
                throw (new AuthException(err));
            } else {
                throw (new RuntimeException("Unexpected reply `" + stat + "' from auth server"));
            }
        }
    }

    public static void main(final String[] args) throws Exception {
        Thread t = new HackThread(new Runnable() {
            public void run() {
                try {
                    AuthClient test = new AuthClient("127.0.0.1", 1871);
                    try {
                        String acct = new NativeCred(args[0], args[1]).tryauth(test);
                        if (acct == null) {
                            System.err.println("failed");
                            return;
                        }
                        System.out.println(acct);
                        System.out.println(Utils.byte2hex(test.getcookie()));
                    } finally {
                        test.close();
                    }
                } catch (Exception e) {
                    throw (new RuntimeException(e));
                }
            }
        }, "Test");
        t.start();
        t.join();
    }
}
