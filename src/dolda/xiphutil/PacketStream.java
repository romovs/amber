package dolda.xiphutil;

import java.io.IOException;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;

/**
 * The <code>PacketStream</code> class decodes Ogg packets from a page
 * stream.
 *
 * @author Fredrik Tolf <code>&lt;fredrik@dolda2000.com&gt;</code>
 */
public class PacketStream {
    private StreamState strm = null;
    private Page page = null;
    private final PageStream in;
    private boolean eos = false;

    /**
     * Constructs a new <code>PacketStream</code> object.
     *
     * @param in the {@link PageStream} object to decode packets from.
     */
    public PacketStream(PageStream in) {
        this.in = in;
    }

    /**
     * Fetches one packet from the stream.
     *
     * @return the packet fetched, or <code>null</code> if at the end
     * of the stream.
     * @throws java.io.IOException if the <code>PageStream</code>
     *                             itself throws an <code>IOException</code>.
     * @throws FormatException     if a format error is found in the
     *                             stream.
     */
    public Packet packet() throws IOException {
        if (eos)
            return (null);
        if (strm == null) {
            strm = new StreamState();
            page = in.page();
            strm.init(page.serialno());
        }
        Packet pkt = new Packet();
        while (true) {
            int ret = strm.packetout(pkt);
            if (ret < 0)
                throw (new OggException()); /* ? */
            if (ret == 1)
                return (pkt);
            if (page == null) {
                if ((page = in.page()) == null) {
                    eos = true;
                    return (null);
                }
            }
            if (strm.pagein(page) != 0)
                throw (new OggException());
            page = null;
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
