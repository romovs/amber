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

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;

public class Music {
    private static Player player;
    public static boolean enabled = true;
    private static boolean debug = false;

    static {
        enabled = Utils.parsebool(Utils.getpref("bgmen", "true"), true);
    }

    private static void debug(String str) {
        if (debug)
            System.out.println(str);
    }

    private static class Player extends HackThread {
        private Indir<Resource> res;
        private Thread waitfor;
        private Sequencer seq;
        private Synthesizer synth;
        private boolean done;
        private boolean loop = false;

        private Player(Indir<Resource> res, Thread waitfor) {
            super("Music player");
            setDaemon(true);
            this.res = res;
            this.waitfor = waitfor;
        }

        public void run() {
            try {
                if (waitfor != null)
                    waitfor.join();
                Resource res = Loading.waitforint(this.res);
                try {
                    seq = MidiSystem.getSequencer(false);
                    synth = MidiSystem.getSynthesizer();
                    seq.open();
                    seq.setSequence(res.layer(Resource.Music.class).seq);
                    synth.open();
                    seq.getTransmitter().setReceiver(synth.getReceiver());
                } catch (MidiUnavailableException e) {
                    return;
                } catch (InvalidMidiDataException e) {
                    return;
                } catch (IllegalArgumentException e) {
            /* The soft synthesizer appears to be throwing
             * non-checked exceptions through from the sampled
		     * audio system. Ignore them and only them. */
                    if (e.getMessage().startsWith("No line matching"))
                        return;
                    throw (e);
                }
                seq.addMetaEventListener(new MetaEventListener() {
                    public void meta(MetaMessage msg) {
                        debug("Meta " + msg.getType());
                        if (msg.getType() == 47) {
                            synchronized (Player.this) {
                                done = true;
                                Player.this.notifyAll();
                            }
                        }
                    }
                });
                do {
                    debug("Start loop");
                    done = false;
                    seq.start();
                    synchronized (this) {
                        while (!done)
                            this.wait();
                    }
                    seq.setTickPosition(0);
                } while (loop);
            } catch (InterruptedException e) {
            } finally {
                try {
                    debug("Exit player");
                    if (seq != null)
                        seq.close();
                    try {
                        if (synth != null)
                            synth.close();
                    } catch (Throwable e2) {
                        if (e2 instanceof InterruptedException) {
			    /* XXX: There appears to be a bug in Sun's
			     * software MIDI implementation that throws back
			     * an unchecked InterruptedException here when two
			     * interrupts come close together (such as in the
			     * case when the current player is first stopped,
			     * and then another started immediately afterwards
			     * on a new song before the first one has had time
			     * to terminate entirely). */
                        } else {
                            throw (new RuntimeException(e2));
                        }
                    }
                } finally {
                    synchronized (Music.class) {
                        if (player == this)
                            player = null;
                    }
                }
            }
        }
    }

    public static void play(Indir<Resource> res, boolean loop) {
        synchronized (Music.class) {
            if (player != null)
                player.interrupt();
            if (res != null) {
                player = new Player(res, player);
                player.loop = loop;
                player.start();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        debug = true;
        play(Resource.remote().load(args[0]), (args.length > 1) ? args[1].equals("y") : false);
        player.join();
    }

    public static void enable(boolean enabled) {
        if (!enabled)
            play(null, false);
        Music.enabled = enabled;
        Utils.setpref("bgmen", Boolean.toString(enabled));
    }

    static {
        Console.setscmd("bgm", new Console.Command() {
            public void run(Console cons, String[] args) {
                int i = 1;
                String opt;
                boolean loop = false;
                if (i < args.length) {
                    while ((opt = args[i]).charAt(0) == '-') {
                        i++;
                        if (opt.equals("-l"))
                            loop = true;
                    }
                    String resnm = args[i++];
                    int ver = -1;
                    if (i < args.length)
                        ver = Integer.parseInt(args[i++]);
                    Music.play(Resource.remote().load(resnm, ver), loop);
                } else {
                    Music.play(null, false);
                }
            }
        });
        Console.setscmd("bgmsw", new Console.Command() {
            public void run(Console cons, String[] args) {
                if (args.length < 2)
                    enable(!enabled);
                else
                    enable(Utils.parsebool(args[1], true));
            }
        });
    }
}
