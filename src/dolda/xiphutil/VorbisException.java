package dolda.xiphutil;

/**
 * Signals that a Vorbis stream was malformatted.
 *
 * @author Fredrik Tolf <code>&lt;fredrik@dolda2000.com&gt;</code>
 */
@SuppressWarnings("serial")
public class VorbisException extends FormatException {
    public VorbisException() {
        super("Invalid Vorbis data");
    }
}
