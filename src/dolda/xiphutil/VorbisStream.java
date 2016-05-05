package dolda.xiphutil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.jcraft.jogg.Packet;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;

/**
 * The <code>VorbisStream</code> class provides a convenient means of
 * decoding Vorbis data. It can be constructed either from a
 * <code>PacketStream</code> object which can come from any source, or
 * from a normal Java IO <code>InputStream</code> object. In the
 * latter case, it will treat the data as Ogg data and construct a
 * <code>PacketStream</code> accordingly.
 * <p/>
 * <p>Decoded data can be fetched either as arrays of floats,
 * representing individual samples, using the {@link #decode()}
 * function, or a Java IO <code>InputStream</code> can be fetched,
 * which will emit a 16-bit little endian PCM stream.
 * <p/>
 * <p>After a <code>VorbisStream</code> object has been constructed,
 * it will immediately provide the metadata from the Vorbis stream in
 * public fields. See {@link #uc}, {@link #vnd}, {@link #chn} and
 * {@link #rate} for details.
 *
 * @author Fredrik Tolf <code>&lt;fredrik@dolda2000.com&gt;</code>
 */
public class VorbisStream {
    private PacketStream in;
    private Info info = new Info();
    private Comment cmt = new Comment();
    private DspState dsp = new DspState();
    private Block blk = new Block(dsp);
    private float[][][] pcmp;
    private int[] idxp;
    /**
     * A <code>java.util.Map</code> instance, providing the Vorbis
     * comments as key-value pairs decoded as normal
     * <code>java.lang.String</code>s. The keys are guaranteed to be
     * lower case and interned, and the values are exactly as provided
     * from the Vorbis comment. Please see the
     * <a href="http://www.xiph.org/vorbis/doc/v-comment.html">Vorbis
     * documentation</a> for more information on standardized keys.
     */
    public final Map<String, String> uc;
    /**
     * A string identifying the program and/or library which encoded
     * the Vorbis data.
     */
    public final String vnd;
    /**
     * The number of channels in this Vorbis data. One channel means
     * monaural sound, and two channels means stereo sound, but do
     * note that other values may occur as well.
     */
    public final int chn;
    /**
     * The sampling frequency of the Vorbis data in Hertz.
     */
    public final int rate;

    /**
     * Constructs a <code>VorbisStream</code> from a
     * <code>PacketStream</code>. The constructor will have ensured
     * that the fields containing the Vorbis metadata have been
     * initialized when it returns.
     *
     * @throws java.io.IOException if the
     *                             <code>PacketStream</code> itself throws an
     *                             <code>IOException</code>.
     * @throws FormatException     if a format
     *                             error is found in the input.
     */
    public VorbisStream(PacketStream in) throws IOException {
        this.in = in;
        info.init();
        cmt.init();
        for (int i = 0; i < 3; i++) {
            Packet pkt = in.packet();
            if (pkt == null)
                throw (new VorbisException());
            if (info.synthesis_headerin(cmt, pkt) < 0)
                throw (new VorbisException());
        }
        vnd = new String(cmt.vendor, 0, cmt.vendor.length - 1, "UTF-8");
        HashMap<String, String> uc = new HashMap<String, String>();
        for (int i = 0; i < cmt.user_comments.length - 1; i++) {
            byte[] cb = cmt.user_comments[i];
            String cs = new String(cb, 0, cb.length - 1, "UTF-8");
            int ep;
            if ((ep = cs.indexOf('=')) < 1)
                throw (new VorbisException());
            uc.put(cs.substring(0, ep).toLowerCase().intern(), cs.substring(ep + 1));
        }
        this.uc = Collections.unmodifiableMap(uc);
        chn = info.channels;
        rate = info.rate;
        dsp.synthesis_init(info);
        blk.init(dsp);
        pcmp = new float[1][][];
        idxp = new int[chn];
    }

    /**
     * Constructs a <code>VorbisStream</code> from a Java IO
     * <code>InputStream</code>. The data from the stream will be
     * decoded as Ogg data containing Vorbis. The constructor will
     * have ensured that the fields containing the Vorbis metadata
     * have been initialized when it returns.
     *
     * @throws java.io.IOException if the <code>InputStream</code>
     *                             itself throws an <code>IOException</code>.
     * @throws FormatException     if a format error is found in
     *                             the input.
     */
    public VorbisStream(InputStream in) throws IOException {
        this(new PacketStream(new PageStream(in)));
    }

    /**
     * Perform a decode cycle. The return value is an array of float
     * arrays. It contains one float array for each channel in the
     * Vorbis stream, each of which contains one float for each
     * sample. Each sample ranges from -1.0 to 1.0.
     * <p/>
     * <p>The array is guaranteed to contain as many sample arrays as
     * the {@link #chn} field indicates. The sample arrays are also
     * guaranteed to be of pairwise equal length, and to contain at
     * least one sample. Therefore, if <code>buf</code> is a returned
     * value from this function, the expression
     * <code>buf[0].length</code> is guaranteed to work for fetching
     * the number of samples from this cycle.
     *
     * @return The decoded data, or <code>null</code> when the stream
     * ends.
     * @throws java.io.IOException if the backing input stream
     *                             itself throws an <code>IOException</code>.
     * @throws FormatException     if a format error is found in
     *                             the input.
     */
    public float[][] decode() throws IOException {
        while (true) {
            int len = dsp.synthesis_pcmout(pcmp, idxp);
            if (len > 0) {
                float[][] ret = new float[chn][];
                for (int i = 0; i < chn; i++) {
                    ret[i] = new float[len];
                    System.arraycopy(pcmp[0][i], idxp[i], ret[i], 0, len);
                }
                dsp.synthesis_read(len);
                return (ret);
            }
            Packet pkt = in.packet();
            if (pkt == null)
                return (null);
            if ((blk.synthesis(pkt) != 0) || (dsp.synthesis_blockin(blk) != 0))
                throw (new VorbisException());
        }
    }

    /**
     * Constructs and returns a <code>java.io.InputStream</code> which
     * uses the {@link #decode()} function to decode data, and encodes
     * it as a 16-bit little endian byte stream of PCM data. The
     * <code>close</code> function of the <code>InputStream</code>
     * object will chain to the {@link #close()} function of this
     * <code>VorbisStream</code> object.
     *
     * @return A <code>java.io.InputStream</code> object from which
     * the PCM stream can be read.
     */
    public InputStream pcmstream() {
        return (new InputStream() {
            private byte[] buf;
            private int bufp;

            private boolean convert() throws IOException {
                float[][] inb = decode();
                if (inb == null) {
                    buf = null;
                    return (false);
                }
                buf = new byte[2 * chn * inb[0].length];
                int p = 0;
                for (int i = 0; i < inb[0].length; i++) {
                    for (int c = 0; c < chn; c++) {
                        int s = (int) (inb[c][i] * 32767);
                        s = Math.min(Math.max(s, -32767), 32767);
                        buf[p++] = (byte) s;
                        buf[p++] = (byte) (s >> 8);
                    }
                }
                bufp = 0;
                return (true);
            }

            public int read() throws IOException {
                byte[] rb = new byte[1];
                int ret;
                do {
                    ret = read(rb);
                    if (ret < 0)
                        return (-1);
                } while (ret == 0);
                return (rb[0]);
            }

            public int read(byte[] dst, int off, int len) throws IOException {
                if ((buf == null) && !convert())
                    return (-1);
                if (buf.length - bufp < len)
                    len = buf.length - bufp;
                System.arraycopy(buf, bufp, dst, off, len);
                if ((bufp += len) == buf.length)
                    buf = null;
                return (len);
            }

            public void close() throws IOException {
                VorbisStream.this.close();
            }
        });
    }

    /**
     * Returns a string description of this object.
     */
    public String toString() {
        return (String.format("Vorbis Stream (encoded by `%s', %d comments, %d channels, sampled at %d Hz)", vnd, uc.size(), chn, rate));
    }

    /**
     * This function implements a main function that can be used for
     * either testing the functionality of this
     * <code>VorbisStream</code> implementation or individual
     * Ogg/Vorbis files. It takes one command-line argument to be
     * interpreted as the path to an Ogg/Vorbis file to be decoded
     * into PCM data. The PCM data is output to standard output.
     * <p/>
     * You can use the <code>sox</code> program to play Ogg/Vorbis
     * files with this class as well:
     * <pre>
     * java dolda.xiphutil.VorbisStream test.ogg | sox -t .raw -r 44100 -sw -c 2 - -t ossdsp /dev/dsp
     * </pre>
     */
    public static void main(String[] args) throws Exception {
        VorbisStream vs = new VorbisStream(new FileInputStream(args[0]));
        InputStream pcm = vs.pcmstream();
        byte[] buf = new byte[4096];
        int ret;
        while ((ret = pcm.read(buf)) >= 0) {
            System.out.write(buf, 0, ret);
        }
    }

    /**
     * Closes the stream backing this object.
     *
     * @throws java.io.IOException if the backing input stream
     *                             itself throws an <code>IOException</code>.
     */
    public void close() throws IOException {
        in.close();
    }
}
