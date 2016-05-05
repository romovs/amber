package dolda.xiphutil;

import java.io.IOException;
import java.io.InputStream;

import com.jcraft.jogg.Page;
import com.jcraft.jogg.SyncState;

/**
 * The <code>PageStream</code> class decodes Ogg pages from a byte
 * stream.
 *
 * @author Fredrik Tolf <code>&lt;fredrik@dolda2000.com&gt;</code>
 */
public class PageStream {
    private SyncState sync = new SyncState();
    private final InputStream in;
    private boolean eos = false;

    /**
     * Constructs a new <code>PageStream</code> object.
     *
     * @param in the Java IO <code>InputStream</code> to fetch pages
     *           from.
     */
    public PageStream(InputStream in) {
        this.in = in;
        sync.init();
    }

    /**
     * Fetches one page from the byte stream.
     *
     * @return the page fetched, or <code>null</code> if at the end of
     * the stream.
     * @throws java.io.IOException if the <code>InputStream</code>
     *                             itself throws an <code>IOException</code>.
     * @throws FormatException     if a format error is found in the
     *                             stream.
     */
    public Page page() throws IOException {
        if (eos)
            return (null);
        Page page = new Page();
        while (true) {
            int ret = sync.pageout(page);
            if (ret < 0)
                throw (new OggException()); /* ? */
            if (ret == 1) {
                if (page.eos() != 0)
                    eos = true;
                return (page);
            }
            int off = sync.buffer(4096);
            int len = in.read(sync.data, off, 4096);
            if (len < 0)
                return (null);
            sync.wrote(len);
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
