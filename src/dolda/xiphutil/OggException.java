package dolda.xiphutil;

/**
 * Signals that an Ogg stream was malformatted.
 *
 * @author Fredrik Tolf <code>&lt;fredrik@dolda2000.com&gt;</code>
 */
@SuppressWarnings("serial")
public class OggException extends FormatException {
    public OggException() {
        super("Invalid Ogg data");
    }
}
