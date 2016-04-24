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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import dolda.xiphutil.VorbisStream;

public class Audio {
    public static boolean enabled = true;
    private static Player player;
    public static final AudioFormat fmt = new AudioFormat(44100, 16, 2, true, false);
    private static int bufsize = 4096;
    public static double volume = 1.0;

    static {
        volume = Double.parseDouble(Utils.getpref("sfxvol", "1.0"));
    }

    public static void setvolume(double volume) {
        Audio.volume = volume;
        Utils.setpref("sfxvol", Double.toString(volume));
    }

    public interface CS {
        public int get(double[][] buf, int len);
    }

    public static class Mixer implements CS {
        public final boolean cont;
        private final Collection<CS> clips = new LinkedList<CS>();

        public Mixer(boolean continuous) {
            this.cont = continuous;
        }

        public Mixer() {
            this(false);
        }

        public int get(double[][] dst, int ns) {
            int nch = dst.length;
            double[][] buf = new double[nch][ns];
            int max = 0;
            synchronized (clips) {
                clip:
                for (Iterator<CS> i = clips.iterator(); i.hasNext(); ) {
                    CS cs = i.next();
                    int left = ns;
                    int boff = 0;
                    while (left > 0) {
                        int ret = cs.get(buf, left);
                        if (ret < 0) {
                            i.remove();
                            continue clip;
                        }
                        if (boff + ret > max) {
                            for (int ch = 0; ch < nch; ch++)
                                Arrays.fill(dst[ch], max, boff + ret, 0.0);
                            max = boff + ret;
                        }
                        for (int ch = 0; ch < nch; ch++) {
                            for (int sm = 0; sm < ret; sm++)
                                dst[ch][boff + sm] += buf[ch][sm];
                        }
                        left -= ret;
                        boff += ret;
                    }
                }
            }
            if (cont) {
                for (int ch = 0; ch < nch; ch++) {
                    for (int sm = max; sm < ns; sm++)
                        dst[ch][sm] = 0;
                }
                return (ns);
            } else {
                return ((max > 0) ? max : -1);
            }
        }

        public void add(CS clip) {
            synchronized (clips) {
                clips.add(clip);
            }
        }

        public void stop(CS clip) {
            synchronized (clips) {
                for (Iterator<CS> i = clips.iterator(); i.hasNext(); ) {
                    if (i.next() == clip) {
                        i.remove();
                        break;
                    }
                }
            }
        }

        public boolean playing(CS clip) {
            synchronized (clips) {
                for (CS cs : clips) {
                    if (cs == clip)
                        return (true);
                }
            }
            return (false);
        }

        public boolean empty() {
            synchronized (clips) {
                return (clips.isEmpty());
            }
        }

        public Collection<CS> current() {
            synchronized (clips) {
                return (new ArrayList<CS>(clips));
            }
        }

        public void clear() {
            synchronized (clips) {
                clips.clear();
            }
        }
    }

    public static class PCMClip implements CS {
        public final InputStream clip;
        public final int sch;
        private final byte[] dbuf = new byte[256];
        private int head = 0, tail = 0;

        public PCMClip(InputStream clip, int nch) {
            this.clip = clip;
            this.sch = nch;
        }

        public int get(double[][] dst, int ns) {
            int nch = dst.length;
            double[] dec = new double[sch];
            for (int sm = 0; sm < ns; sm++) {
                while (tail - head < 2 * sch) {
                    if (head > 0) {
                        for (int i = 0; i < tail - head; i++)
                            dbuf[i] = dbuf[head + i];
                        tail -= head;
                        head = 0;
                    }
                    try {
                        int ret = clip.read(dbuf, tail, dbuf.length - tail);
                        if (ret < 0)
                            return ((sm > 0) ? sm : -1);
                        tail += ret;
                    } catch (IOException e) {
                        return (-1);
                    }
                }
                for (int ch = 0; ch < sch; ch++) {
                    int b1 = dbuf[head++] & 0xff;
                    int b2 = dbuf[head++] & 0xff;
                    int v = b1 + (b2 << 8);
                    if (v >= 32768)
                        v -= 65536;
                    dec[ch] = v * 0x1.0p-15;
                }
                for (int ch = 0; ch < nch; ch++)
                    dst[ch][sm] = dec[ch % sch];
            }
            return (ns);
        }
    }

    public static class VorbisClip implements CS {
        public final VorbisStream clip;
        private float[][] data = new float[1][0];
        private int dp = 0;

        public VorbisClip(VorbisStream clip) {
            this.clip = clip;
        }

        public int get(double[][] dst, int ns) {
            int nch = dst.length;
            if (data == null)
                return (-1);
            for (int sm = 0; sm < ns; sm++) {
                while (dp >= data[0].length) {
                    try {
                        if ((data = clip.decode()) == null)
                            return ((sm > 0) ? sm : -1);
                    } catch (IOException e) {
                        return (-1);
                    }
                    dp = 0;
                }
                for (int ch = 0; ch < nch; ch++)
                    dst[ch][sm] = data[ch % clip.chn][dp];
                dp++;
            }
            return (ns);
        }
    }

    public static class VolAdjust implements CS {
        public final CS bk;
        public double vol = 1.0, bal = 0.0;
        private double[] cvol = {};

        public VolAdjust(CS bk, double vol) {
            this.bk = bk;
            this.vol = vol;
        }

        public VolAdjust(CS bk) {
            this(bk, 1.0);
        }

        public int get(double[][] dst, int ns) {
            int nch = dst.length;
            int ret = bk.get(dst, ns);
            if (ret < 0)
                return (ret);
            if (cvol.length != nch)
                cvol = new double[nch];
            for (int i = 0; i < cvol.length; i++)
                cvol[i] = vol;
            if (bal < 0)
                cvol[1] *= 1.0 + bal;
            if (bal > 0)
                cvol[0] *= 1.0 - bal;
            for (int ch = 0; ch < nch; ch++) {
                for (int sm = 0; sm < ret; sm++)
                    dst[ch][sm] *= cvol[ch % cvol.length];
            }
            return (ret);
        }
    }

    public static class Resampler implements CS {
        public final CS bk;
        public double irate, orate;
        public double sp;
        private double ack;
        private double[] lval = {0}, nval = {0};
        private double[][] data = {};
        private int dp = 0, dl = 0;

        public Resampler(CS bk, double irate, double orate) {
            this.bk = bk;
            this.irate = irate;
            this.orate = orate;
        }

        public Resampler(CS bk, double irate) {
            this(bk, irate, fmt.getSampleRate());
        }

        public Resampler(CS bk) {
            this(bk, fmt.getSampleRate());
        }

        public int get(double[][] dst, int ns) {
            int nch = dst.length;
            if (nval.length != nch) {
                nval = new double[nch];
                lval = new double[nch];
            }
            if (data.length != nch)
                data = new double[nch][512];
            double esp = sp * irate / orate;
            for (int sm = 0; sm < ns; sm++) {
                ack += esp;
                while (ack >= 1.0) {
                    while (dp >= dl) {
                        if ((dl = bk.get(data, 512)) < 0)
                            return ((sm > 0) ? sm : -1);
                        dp = 0;
                    }
                    for (int ch = 0; ch < nch; ch++) {
                        lval[ch] = nval[ch];
                        nval[ch] = data[ch][dp];
                    }
                    dp++;
                    ack -= 1.0;
                }
                for (int ch = 0; ch < nch; ch++)
                    dst[ch][sm] = (lval[ch] * (1.0 - ack)) + (nval[ch] * ack);
            }
            return (ns);
        }
    }

    public static class Monitor implements CS {
        public final CS bk;
        public boolean eof = false;

        public Monitor(CS bk) {
            this.bk = bk;
        }

        public int get(double[][] dst, int ns) {
            int ret = bk.get(dst, ns);
            if ((ret < 0) && !eof) {
                eof = true;
                eof();
            }
            return (ret);
        }

        protected void eof() {
            synchronized (this) {
                notifyAll();
            }
        }

        public void finwait() throws InterruptedException {
            synchronized (this) {
                while (!eof) {
                    wait();
                }
            }
        }
    }

    public static abstract class Repeater implements CS {
        private CS cur = null;

        public int get(double[][] buf, int ns) {
            while (true) {
                if (cur == null) {
                    if ((cur = cons()) == null)
                        return (-1);
                }
                int ret = cur.get(buf, ns);
                if (ret >= 0)
                    return (ret);
                cur = null;
            }
        }

        protected abstract CS cons();
    }

    public static class LDump implements CS {
        public final CS bk;
        private double val = 0.0;
        private int n = 0;

        public LDump(CS bk, int iv) {
            this.bk = bk;
        }

        public LDump(CS bk) {
            this(bk, 44100);
        }

        public int get(double[][] buf, int ns) {
            int nch = buf.length;
            int ret = bk.get(buf, ns);
            if (ret < 0) {
                if (n > 0)
                    System.err.println(val / n);
            } else {
                for (int ch = 0; ch < nch; ch++) {
                    for (int sm = 0; sm < ret; sm++) {
                        val += Math.abs(buf[ch][sm]);
                        if (++n >= 44100) {
                            System.err.print((val / n) + " ");
                            val = 0;
                            n = 0;
                        }
                    }
                }
            }
            return (ret);
        }
    }

    private static class Player extends HackThread {
        private final CS stream;
        private final int nch;
        private final Object queuemon = new Object();
        private Collection<Runnable> queue = new LinkedList<Runnable>();
        private volatile boolean reopen = false;

        Player(CS stream) {
            super("Haven audio player");
            this.stream = stream;
            nch = fmt.getChannels();
            setDaemon(true);
        }

        private int fillbuf(byte[] dst, int off, int len) {
            int ns = len / (2 * nch);
            double[][] val = new double[nch][ns];
            int left = ns, wr = 0;
            while (left > 0) {
                int ret = stream.get(val, left);
                if (ret <= 0)
                    return ((wr > 0) ? wr : -1);
                for (int i = 0; i < ret; i++) {
                    for (int o = 0; o < nch; o++) {
                        int iv = (int) (val[o][i] * volume * 32767.0);
                        if (iv < 0) {
                            if (iv < -32768)
                                iv = -32768;
                            iv += 65536;
                        } else {
                            if (iv > 32767)
                                iv = 32767;
                        }
                        dst[off++] = (byte) (iv & 0xff);
                        dst[off++] = (byte) ((iv & 0xff00) >> 8);
                        wr += 2;
                    }
                }
                left -= ret;
            }
            return (wr);
        }

        public void run() {
            SourceDataLine line = null;
            try {
                while (true) {
                    try {
                        line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, fmt));
                        line.open(fmt, bufsize);
                        line.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                    synchronized (this) {
                        reopen = false;
                        this.notifyAll();
                    }
                    byte[] buf = new byte[bufsize / 2];
                    while (true) {
                        if (Thread.interrupted())
                            throw (new InterruptedException());
                        synchronized (queuemon) {
                            Collection<Runnable> queue = this.queue;
                            if (queue.size() > 0) {
                                this.queue = new LinkedList<Runnable>();
                                for (Runnable r : queue)
                                    r.run();
                            }
                        }
                        int ret = fillbuf(buf, 0, buf.length);
                        if (ret < 0)
                            return;
                        for (int off = 0; off < ret; off += line.write(buf, off, ret - off)) ;
                        if (reopen)
                            break;
                    }
                    line.close();
                    line = null;
                }
            } catch (InterruptedException e) {
            } finally {
                synchronized (Audio.class) {
                    player = null;
                }
                if (line != null)
                    line.close();
            }
        }

        void reopen() {
            try {
                synchronized (this) {
                    reopen = true;
                    while (reopen && isAlive())
                        this.wait();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static Player ckpl(boolean creat) {
        synchronized (Audio.class) {
            if (enabled) {
                if (player == null) {
                    if (creat) {
                        player = new Player(new Mixer(true));
                        player.start();
                    } else {
                        return (null);
                    }
                }
                return (player);
            } else {
                return (null);
            }
        }
    }

    public static void play(CS clip) {
        if (clip == null)
            throw (new NullPointerException());
        Player pl = ckpl(true);
        if (pl != null)
            ((Mixer) pl.stream).add(clip);
    }

    public static void stop(CS clip) {
        Player pl = ckpl(false);
        ;
        if (pl != null)
            ((Mixer) pl.stream).stop(clip);
    }

    public static void queue(Runnable d) {
        Player pl = ckpl(true);
        synchronized (pl.queuemon) {
            pl.queue.add(d);
        }
    }

    private static Map<Resource, Resource.Audio> reslastc = new HashMap<Resource, Resource.Audio>();

    public static CS fromres(Resource res) {
        Collection<Resource.Audio> clips = res.layers(Resource.audio);
        synchronized (reslastc) {
            Resource.Audio last = reslastc.get(res);
            int sz = clips.size();
            int s = (int) (Math.random() * (((sz > 2) && (last != null)) ? (sz - 1) : sz));
            Resource.Audio clip = null;
            for (Resource.Audio cp : clips) {
                if (cp == last)
                    continue;
                clip = cp;
                if (--s < 0)
                    break;
            }
            if (sz > 2)
                reslastc.put(res, clip);
            return (clip.stream());
        }
    }

    public static void play(Resource res) {
        play(fromres(res));
    }

    public static void play(Resource res, double vol) {
        play(new Audio.VolAdjust(fromres(res), vol));
    }

    public static void play(final Indir<Resource> clip) {
        queue(new Runnable() {
            public void run() {
                try {
                    play(clip.get());
                } catch (Loading e) {
                    queue(this);
                }
            }
        });
    }

    public static void main(String[] args) throws Exception {
        Collection<Monitor> clips = new LinkedList<Monitor>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-b")) {
                bufsize = Integer.parseInt(args[++i]);
            } else {
                Monitor c = new Monitor(new PCMClip(new FileInputStream(args[i]), 2));
                clips.add(c);
            }
        }
        for (Monitor c : clips)
            play(c);
        for (Monitor c : clips)
            c.finwait();
    }

    static {
        Console.setscmd("sfx", new Console.Command() {
            public void run(Console cons, String[] args) {
                play(Resource.remote().load(args[1]));
            }
        });
        Console.setscmd("sfxvol", new Console.Command() {
            public void run(Console cons, String[] args) {
                setvolume(Double.parseDouble(args[1]));
            }
        });
        Console.setscmd("audiobuf", new Console.Command() {
            public void run(Console cons, String[] args) throws Exception {
                int nsz = Integer.parseInt(args[1]);
                if (nsz > 44100)
                    throw (new Exception("Rejecting buffer longer than 1 second"));
                bufsize = nsz * 4;
                Player pl = ckpl(false);
                if (pl != null)
                    pl.reopen();
            }
        });
    }
}
