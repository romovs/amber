package dolda.xiphutil;

/**
 * Signals that a format error was encountered in either an Ogg or a
 * Vorbis stream during decoding. This exception is a subclass of
 * <code>java.io.IOException</code> so that it fits snugly within
 * Java's normal IO pipelines.
 * <p/>
 * <p>Instances throws by the classes in this package will always be
 * constructed from the more specific classes {@link OggException} or
 * {@link VorbisException}.
 *
 * @author Fredrik Tolf <code>&lt;fredrik@dolda2000.com&gt;</code>
 */
@SuppressWarnings("serial")
public class FormatException extends java.io.IOException {
    public FormatException(String msg) {
        super(msg);
    }
}
