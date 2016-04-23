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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.annotation.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.*;
import java.net.*;
import java.io.*;
import javax.imageio.*;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

@SuppressWarnings("serial")
public class Resource implements Serializable {
    private static File rescustom = new File("res");
    private static ResCache prscache;
    public static ThreadGroup loadergroup = null;
    private static Map<String, LayerFactory<?>> ltypes = new TreeMap<String, LayerFactory<?>>();
    public static Class<Image> imgc = Image.class;
    public static Class<Tile> tile = Tile.class;
    public static Class<Neg> negc = Neg.class;
    public static Class<Anim> animc = Anim.class;
    public static Class<Tileset> tileset = Tileset.class;
    public static Class<Pagina> pagina = Pagina.class;
    public static Class<AButton> action = AButton.class;
    public static Class<Audio> audio = Audio.class;
    public static Class<Tooltip> tooltip = Tooltip.class;

    public static String language = Utils.getpref("language", "en");
    public static Map<String, String> l10nTooltip, l10nPagina, l10nWindow, l10nButton, l10nFlower, l10nMsg, l10nLabel, l10nAction;
    public static final String BUNDLE_TOOLTIP = "tooltip";
    public static final String BUNDLE_PAGINA = "pagina";
    public static final String BUNDLE_WINDOW = "window";
    public static final String BUNDLE_BUTTON = "button";
    public static final String BUNDLE_FLOWER = "flower";
    public static final String BUNDLE_MSG = "msg";
    public static final String BUNDLE_LABEL = "label";
    public static final String BUNDLE_ACTION = "action";
    public static final boolean L10N_DEBUG = false;

    private Collection<Layer> layers = new LinkedList<Layer>();
    public final String name;
    public int ver;
    public ResSource source;
    public final transient Pool pool;
    private boolean used = false;

    public abstract static class Named implements Indir<Resource> {
        public final String name;
        public final int ver;

        public Named(String name, int ver) {
            this.name = name;
            this.ver = ver;
        }

        public boolean equals(Object other) {
            if (!(other instanceof Named))
                return (false);
            Named o = (Named) other;
            return (o.name.equals(this.name) && (o.ver == this.ver));
        }

        public int hashCode() {
            int ret = name.hashCode();
            ret = (ret * 31) + ver;
            return (ret);
        }
    }

    public static class Spec extends Named implements Serializable {
        public final transient Pool pool;

        public Spec(Pool pool, String name, int ver) {
            super(name, ver);
            this.pool = pool;
        }

        public Spec(Pool pool, String name) {
            this(pool, name, -1);
        }

        public Resource get(int prio) {
            return (pool.load(name, ver, prio).get());
        }

        public Resource get() {
            return (get(0));
        }
    }

    private Resource(Pool pool, String name, int ver) {
        this.pool = pool;
        this.name = name;
        this.ver = ver;
    }

    public static void setcache(ResCache cache) {
        prscache = cache;
    }

    public String basename() {
        int p = name.lastIndexOf('/');
        if (p < 0)
            return (name);
        return (name.substring(p + 1));
    }

    public static interface ResSource {
        public InputStream get(String name) throws IOException;
    }

    public static abstract class TeeSource implements ResSource, Serializable {
        public ResSource back;

        public TeeSource(ResSource back) {
            this.back = back;
        }

        public InputStream get(String name) throws IOException {
            StreamTee tee = new StreamTee(back.get(name));
            tee.setncwe();
            tee.attach(fork(name));
            return (tee);
        }

        public abstract OutputStream fork(String name) throws IOException;

        public String toString() {
            return ("forking source backed by " + back);
        }
    }

    public static class CacheSource implements ResSource, Serializable {
        public transient ResCache cache;

        public CacheSource(ResCache cache) {
            this.cache = cache;
        }

        public InputStream get(String name) throws IOException {
            return (cache.fetch("res/" + name));
        }

        public String toString() {
            return ("cache source backed by " + cache);
        }
    }

    public static class FileSource implements ResSource, Serializable {
        File base;

        public FileSource(File base) {
            this.base = base;
        }

        public InputStream get(String name) throws FileNotFoundException {
            File cur = base;
            String[] parts = name.split("/");
            for (int i = 0; i < parts.length - 1; i++)
                cur = new File(cur, parts[i]);
            cur = new File(cur, parts[parts.length - 1] + ".res");
            return (new FileInputStream(cur));
        }

        public String toString() {
            return ("filesystem res source (" + base + ")");
        }
    }

    public static class JarSource implements ResSource, Serializable {
        public InputStream get(String name) throws FileNotFoundException {
            InputStream s = Resource.class.getResourceAsStream("/res/" + name + ".res");
            if (s == null)
                throw (new FileNotFoundException("Could not find resource locally: " + name));
            return (s);
        }

        public String toString() {
            return ("local res source");
        }
    }

    @SuppressWarnings("static-access")
	public static class HttpSource implements ResSource, Serializable {
        private final transient SslHelper ssl;
        public URL baseurl;

        {
            ssl = new SslHelper();
            try {
                ssl.trust(ssl.loadX509(Resource.class.getResourceAsStream("ressrv.crt")));
            } catch (java.security.cert.CertificateException e) {
                throw (new Error("Invalid built-in certificate", e));
            } catch (IOException e) {
                throw (new Error(e));
            }
            ssl.ignoreName();
        }

        public HttpSource(URL baseurl) {
            this.baseurl = baseurl;
        }

        private URL encodeurl(URL raw) throws IOException {
        /* This is "kinda" ugly. It is, actually, how the Java
         * documentation recommend that it be done, though... */
            try {
                return (new URL(new URI(raw.getProtocol(), raw.getHost(), raw.getPath(), raw.getRef()).toASCIIString()));
            } catch (URISyntaxException e) {
                throw (new IOException(e));
            }
        }

        public InputStream get(String name) throws IOException {
            URL resurl = encodeurl(new URL(baseurl, name + ".res"));
            URLConnection c;
            int tries = 0;
            while (true) {
                try {
                    if (resurl.getProtocol().equals("https"))
                        c = ssl.connect(resurl);
                    else
                        c = resurl.openConnection();
            /* Apparently, some versions of Java Web Start has
		     * a bug in its internal cache where it refuses to
		     * reload a URL even when it has changed. */
                    c.setUseCaches(false);
                    c.addRequestProperty("User-Agent", "Haven/1.0");
                    return (c.getInputStream());
                } catch (ConnectException e) {
                    if (++tries >= 5)
                        throw (new IOException("Connection failed five times", e));
                }
            }
        }

        public String toString() {
            return ("HTTP res source (" + baseurl + ")");
        }
    }

    public static class Loading extends haven.Loading {
        private final Pool.Queued res;

        private Loading(Pool.Queued res) {
            super("Waiting for resource " + res.name + "...");
            this.res = res;
        }

        public String toString() {
            return ("#<Resource " + res.name + ">");
        }

        public boolean canwait() {
            return (true);
        }

        public void waitfor() throws InterruptedException {
            synchronized (res) {
                while (!res.done) {
                    res.wait();
                }
            }
        }

        public void boostprio(int prio) {
            res.boostprio(prio);
        }
    }

    public static class Pool {
        public int nloaders = 2;
        private final Collection<Loader> loaders = new LinkedList<Loader>();
        private final List<ResSource> sources = new LinkedList<ResSource>();
        private final Map<String, Resource> cache = new CacheMap<String, Resource>();
        private final PrioQueue<Queued> queue = new PrioQueue<Queued>();
        private final Map<String, Queued> queued = new HashMap<String, Queued>();
        private final Pool parent;

        public Pool(Pool parent, ResSource... sources) {
            this.parent = parent;
            for (ResSource source : sources)
                this.sources.add(source);
        }

        public Pool(ResSource... sources) {
            this(null, sources);
        }

        public void add(ResSource src) {
            sources.add(src);
        }

        private class Queued extends Named implements Prioritized, Serializable {
            volatile int prio;
            transient final Collection<Queued> rdep = new LinkedList<Queued>();
            Queued awaiting;
            volatile boolean done = false;
            Resource res;
            LoadException error;

            Queued(String name, int ver, int prio) {
                super(name, ver);
                this.prio = prio;
            }

            public int priority() {
                return (prio);
            }

            public void boostprio(int prio) {
                if (this.prio < prio)
                    this.prio = prio;
                Queued p = awaiting;
                if (p != null)
                    p.boostprio(prio);
            }

            public Resource get() {
                if (!done) {
                    boostprio(1);
                    throw (new Loading(this));
                }
                if (error != null)
                    throw (new RuntimeException("Delayed error in resource " + name + " (v" + ver + "), from " + error.src, error));
                return (res);
            }

            private void done() {
                synchronized (this) {
                    done = true;
                    for (Iterator<Queued> i = rdep.iterator(); i.hasNext(); ) {
                        Queued dq = i.next();
                        i.remove();
                        dq.prior(this);
                    }
                    this.notifyAll();
                }
                if (res != null) {
                    synchronized (cache) {
                        cache.put(name, res);
                    }
                    synchronized (queue) {
                        queued.remove(name);
                    }
                }
            }

            private void prior(Queued prior) {
                if ((res = prior.res) == null) {
                    error = prior.error;
                    synchronized (queue) {
                        queue.add(this);
                        queue.notify();
                    }
                    ckld();
                } else {
                    done();
                }
            }
        }

        private void handle(Queued res) {
            for (ResSource src : sources) {
                try {
                    InputStream in = src.get(res.name);
                    try {
                        Resource ret = new Resource(this, res.name, res.ver);
                        ret.source = src;
                        ret.load(in);
                        res.res = ret;
                        res.error = null;
                        break;
                    } finally {
                        in.close();
                    }
                } catch (Throwable t) {
                    LoadException error;
                    if (t instanceof LoadException)
                        error = (LoadException) t;
                    else
                        error = new LoadException(String.format("Load error in resource %s(v%d), from %s", res.name, res.ver, src), t, null);
                    error.src = src;
                    if(res.error != null) {
                        error.prev = res.error;
                        error.addSuppressed(res.error);
                    }
                    res.error = error;
                }
            }
            res.done();
        }

        public Named load(String name, int ver, int prio) {
            Queued ret;
            synchronized (cache) {
                Resource cur = cache.get(name);
                if (cur != null) {
                    if ((ver == -1) || (cur.ver == ver)) {
                        return (cur.indir());
                    } else if (ver < cur.ver) {
			/* Throw LoadException rather than
			 * RuntimeException here, to make sure
			 * obsolete resources doing nested loading get
			 * properly handled. This could be the wrong
			 * way of going about it, however; I'm not
			 * sure. */
                        throw (new LoadException(String.format("Weird version number on %s (%d > %d), loaded from %s", cur.name, cur.ver, ver, cur.source), cur));
                    }
                }
                synchronized (queue) {
                    Queued cq = queued.get(name);
                    if (cq != null) {
                        if ((ver == -1) || (cq.ver == ver)) {
                            cq.boostprio(prio);
                            return (cq);
                        }
                        if (ver < cq.ver)
                            throw (new LoadException(String.format("Weird version number on %s (%d > %d)", cq.name, cq.ver, ver), null));
                        queued.remove(name);
                        queue.removeid(cq);
                    }
                    Queued nq = new Queued(name, ver, prio);
                    queued.put(name, nq);
                    if (parent == null) {
                        queue.add(nq);
                        queue.notify();
                    } else {
                        Indir<Resource> pr = parent.load(name, ver, prio);
                        if (pr instanceof Queued) {
                            Queued pq = (Queued) pr;
                            synchronized (pq) {
                                if (pq.done) {
                                    nq.prior(pq);
                                } else {
                                    nq.awaiting = pq;
                                    pq.rdep.add(nq);
                                }
                            }
                        } else {
                            queued.remove(name);
                            nq.res = pr.get();
                            nq.done = true;
                        }
                    }
                    ret = nq;
                }
            }
            ckld();
            return (ret);
        }

        public Named load(String name, int ver) {
            return (load(name, ver, -5));
        }

        public Named load(String name) {
            return (load(name, -1));
        }

        public Indir<Resource> dynres(long id) {
            return (load(String.format("dyn/%x", id), 1));
        }

        private void ckld() {
            int qsz;
            synchronized (queue) {
                qsz = queue.size();
            }
            synchronized (loaders) {
                while (loaders.size() < Math.min(nloaders, qsz)) {
                    final Loader n = new Loader();
                    Thread th = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<Thread>() {
                        public Thread run() {
                            return (new HackThread(loadergroup, n, "Haven resource loader"));
                        }
                    });
                    th.setDaemon(true);
                    th.start();
                    while (!n.added) {
                        try {
                            loaders.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }
        }

        public class Loader implements Runnable {
            private boolean added = false;

            public void run() {
                synchronized (loaders) {
                    loaders.add(this);
                    added = true;
                    loaders.notifyAll();
                }
                boolean intd = false;
                try {
                    while (true) {
                        Queued cur;
                        synchronized (queue) {
                            long start = System.currentTimeMillis(), now = start;
                            while ((cur = queue.poll()) == null) {
                                queue.wait(10000 - (now - start));
                                now = System.currentTimeMillis();
                                if (now - start >= 10000)
                                    return;
                            }
                        }
                        handle(cur);
                        cur = null;
                    }
                } catch (InterruptedException e) {
                    intd = true;
                } finally {
                    synchronized (loaders) {
                        loaders.remove(this);
                    }
                    if (!intd)
                        ckld();
                }
            }
        }

        public int qdepth() {
            int ret = (parent == null) ? 0 : parent.qdepth();
            synchronized (queue) {
                ret += queue.size();
            }
            return (ret);
        }

        public int numloaded() {
            int ret = (parent == null) ? 0 : parent.numloaded();
            synchronized (cache) {
                ret += cache.size();
            }
            return (ret);
        }

        public Collection<Resource> cached() {
            Set<Resource> ret = new HashSet<Resource>();
            if (parent != null)
                ret.addAll(parent.cached());
            synchronized (cache) {
                ret.addAll(cache.values());
            }
            return (ret);
        }

        public Collection<Resource> used() {
            Collection<Resource> ret = cached();
            for (Iterator<Resource> i = ret.iterator(); i.hasNext(); ) {
                Resource r = i.next();
                if (!r.used)
                    i.remove();
            }
            return (ret);
        }

        private final Set<Resource> loadwaited = new HashSet<Resource>();

        public Collection<Resource> loadwaited() {
            Set<Resource> ret = new HashSet<Resource>();
            if (parent != null)
                ret.addAll(parent.loadwaited());
            synchronized (loadwaited) {
                ret.addAll(loadwaited);
            }
            return (ret);
        }

        private Resource loadwaited(Resource res) {
            synchronized (loadwaited) {
                loadwaited.add(res);
            }
            return (res);
        }

        public Resource loadwaitint(String name, int ver) throws InterruptedException {
            return (loadwaited(Loading.waitforint(load(name, ver, 10))));
        }

        public Resource loadwaitint(String name) throws InterruptedException {
            return (loadwaitint(name, -1));
        }

        public Resource loadwait(String name, int ver) {
            return (loadwaited(Loading.waitfor(load(name, ver, 10))));
        }

        public Resource loadwait(String name) {
            return (loadwait(name, -1));
        }
    }

    private static Pool _local = null;

    public static Pool local() {
        if (_local == null) {
            synchronized (Resource.class) {
                if (_local == null) {
                    Pool local = new Pool(new FileSource(rescustom));
                    local.add(new JarSource());
                    try {
                        String dir = Config.resdir;
                        if (dir == null)
                            dir = System.getenv("HAFEN_RESDIR");
                        if (dir != null)
                            local.add(new FileSource(new File(dir)));
                    } catch (Exception e) {
			/* Ignore these. We don't want to be crashing the client
			 * for users just because of errors in development
			 * aids. */
                    }
                    _local = local;
                }
            }
        }
        return (_local);
    }

    private static Pool _remote = null;

    public static Pool remote() {
        if (_remote == null) {
            synchronized (Resource.class) {
                if (_remote == null) {
                    Pool remote = new Pool(local());
                    if (prscache != null)
                        remote.add(new CacheSource(prscache));
                    _remote = remote;
                    ;
                }
            }
        }
        return (_remote);
    }

    public static void addurl(URL url) {
        ResSource src = new HttpSource(url);
        if (prscache != null) {
			class Caching extends TeeSource {
                private final transient ResCache cache;

                Caching(ResSource bk, ResCache cache) {
                    super(bk);
                    this.cache = cache;
                }

                public OutputStream fork(String name) throws IOException {
                    return (cache.store("res/" + name));
                }
            }
            src = new Caching(src, prscache);
        }
        remote().add(src);
    }

    @Deprecated
    public static Resource load(String name, int ver) {
        return (remote().loadwait(name, ver));
    }

    @Deprecated
    public Resource loadwait() {
        return (this);
    }

    public static class LoadException extends RuntimeException {
        public Resource res;
        public ResSource src;
        public LoadException prev;

        public LoadException(String msg, Resource res) {
            super(msg);
            this.res = res;
        }

        public LoadException(String msg, Throwable cause, Resource res) {
            super(msg, cause);
            this.res = res;
        }

        public LoadException(Throwable cause, Resource res) {
            super("Load error in resource " + res.toString() + ", from " + res.source, cause);
            this.res = res;
        }
    }

    public static Coord cdec(Message buf) {
        return (new Coord(buf.int16(), buf.int16()));
    }

    public abstract class Layer implements Serializable {
        public abstract void init();

        public Resource getres() {
            return (Resource.this);
        }
    }

    public interface LayerFactory<T extends Layer> {
        public T cons(Resource res, Message buf);
    }

    public static class LayerConstructor<T extends Layer> implements LayerFactory<T> {
        public final Class<T> cl;
        private final Constructor<T> cons;

        public LayerConstructor(Class<T> cl) {
            this.cl = cl;
            try {
                this.cons = cl.getConstructor(Resource.class, Message.class);
            } catch (NoSuchMethodException e) {
                throw (new RuntimeException("No proper constructor found for layer type " + cl.getName(), e));
            }
        }

        public T cons(Resource res, Message buf) {
            try {
                return (cons.newInstance(res, buf));
            } catch (InstantiationException e) {
                throw (new LoadException(e, res));
            } catch (IllegalAccessException e) {
                throw (new LoadException(e, res));
            } catch (InvocationTargetException e) {
                Throwable c = e.getCause();
                if (c instanceof RuntimeException)
                    throw ((RuntimeException) c);
                else
                    throw (new LoadException(e, res));
            }
        }
    }

    public static void addltype(String name, LayerFactory<?> cons) {
        ltypes.put(name, cons);
    }

    public static <T extends Layer> void addltype(String name, Class<T> cl) {
        addltype(name, new LayerConstructor<T>(cl));
    }

    @dolda.jglob.Discoverable
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface LayerName {
        public String value();
    }

    static {
        if (!language.equals("en") || Resource.L10N_DEBUG) {
            l10nTooltip = l10n(BUNDLE_TOOLTIP, language);
            l10nPagina = l10n(BUNDLE_PAGINA, language);
            l10nWindow = l10n(BUNDLE_WINDOW, language);
            l10nButton = l10n(BUNDLE_BUTTON, language);
            l10nFlower = l10n(BUNDLE_FLOWER, language);
            l10nMsg = l10n(BUNDLE_MSG, language);
            l10nLabel = l10n(BUNDLE_LABEL, language);
            l10nAction = l10n(BUNDLE_ACTION, language);
        }

        for (Class<?> cl : dolda.jglob.Loader.get(LayerName.class).classes()) {
            String nm = cl.getAnnotation(LayerName.class).value();
            if (LayerFactory.class.isAssignableFrom(cl)) {
                try {
                    addltype(nm, cl.asSubclass(LayerFactory.class).newInstance());
                } catch (InstantiationException e) {
                    throw (new Error(e));
                } catch (IllegalAccessException e) {
                    throw (new Error(e));
                }
            } else if (Layer.class.isAssignableFrom(cl)) {
                addltype(nm, cl.asSubclass(Layer.class));
            } else {
                throw (new Error("Illegal resource layer class: " + cl));
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, String> l10n(String bundle, String langcode) {
        Properties props = new Properties();

        InputStream is = Config.class.getClassLoader().getResourceAsStream("l10n/" + bundle + "_" + langcode + ".properties");
        if (is == null)
            return null;

        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(is, "UTF-8");
            props.load(isr);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e) { // ignored
                }
            }
        }

        return props.size() > 0 ? new HashMap<>((Map) props) : null;
    }

    public interface IDLayer<T> {
        public T layerid();
    }

    @LayerName("image")
    public class Image extends Layer implements Comparable<Image>, IDLayer<Integer> {
        public transient BufferedImage img;
        transient private Tex tex;
        public final int z, subz;
        public final boolean nooff;
        public final int id;
        private int gay = -1;
        public Coord sz;
        public Coord o;

        public Image(Message buf) {
            z = buf.int16();
            subz = buf.int16();
            int fl = buf.uint8();
	    /* Obsolete flag 1: Layered */
            nooff = (fl & 2) != 0;
            id = buf.int16();
            o = cdec(buf);
            try {
                img = ImageIO.read(new MessageInputStream(buf));
            } catch (IOException e) {
                throw (new LoadException(e, Resource.this));
            }
            if (img == null)
                throw (new LoadException("Invalid image data in " + name, Resource.this));
            sz = Utils.imgsz(img);
        }

        public synchronized Tex tex() {
            if (tex != null)
                return (tex);
            tex = new TexI(img) {
                public String toString() {
                    return ("TexI(" + Resource.this.name + ", " + id + ")");
                }
            };
            return (tex);
        }

        private boolean detectgay() {
            for (int y = 0; y < sz.y; y++) {
                for (int x = 0; x < sz.x; x++) {
                    if ((img.getRGB(x, y) & 0x00ffffff) == 0x00ff0080)
                        return (true);
                }
            }
            return (false);
        }

        public boolean gayp() {
            if (gay == -1)
                gay = detectgay() ? 1 : 0;
            return (gay == 1);
        }

        public int compareTo(Image other) {
            return (z - other.z);
        }

        public Integer layerid() {
            return (id);
        }

        public void init() {
        }
    }

    @LayerName("tooltip")
    public class Tooltip extends Layer {
        public final String t;

        public Tooltip(Message buf) {
            String text = new String(buf.bytes(), Utils.utf8);
            if (language.equals("en") && !L10N_DEBUG) {
                this.t = text;
                return;
            }

            if (L10N_DEBUG) {
                Resource res = super.getres();
                if (res.name.startsWith("paginae/act") || res.name.startsWith("paginae/bld")
                        || res.name.startsWith("paginae/craft") || res.name.startsWith("paginae/gov")
                        || res.name.startsWith("paginae/pose") || res.name.startsWith("paginae/amber")
                        || res.name.startsWith("paginae/atk/ashoot")) {
                    Resource.l10nAction = Resource.saveStrings(Resource.BUNDLE_ACTION, Resource.l10nAction, res.name, text);
                } else {
                    Resource.l10nTooltip = Resource.saveStrings(Resource.BUNDLE_TOOLTIP, Resource.l10nTooltip, res.name, text);
                }
            }

            Resource res = super.getres();
            if (res != null && l10nTooltip != null) {
                String locText = l10nTooltip.get(res.name);
                if (locText != null) {
                    if (locText.equals(text) || !res.name.startsWith("gfx/invobjs") ||
                            // exclude meat "conditions" since the tooltip is dynamically generated and it won't be in right order
                            text.contains("Raw ") || text.contains("Filet of ") || text.contains("Sizzling") ||
                            text.contains("Roast") || text.contains("Meat") || text.contains("Spitroast") ||
                            // exclude food conditions
                            res.name.startsWith("gfx/invobjs/food/")) {
                        this.t = locText;
                    } else {
                        this.t = locText + " (" + text + ")";
                    }
                    return;
                }
            }

            this.t = text;
        }

        public void init() {
        }
    }

    @LayerName("tile")
    public class Tile extends Layer {
        transient BufferedImage img;
        transient private Tex tex;
        public final int id;
        public final int w;
        public final char t;

        public Tile(Message buf) {
            t = (char) buf.uint8();
            id = buf.uint8();
            w = buf.uint16();
            try {
                img = ImageIO.read(new MessageInputStream(buf));
            } catch (IOException e) {
                throw (new LoadException(e, Resource.this));
            }
            if (img == null)
                throw (new LoadException("Invalid image data in " + name, Resource.this));
        }

        public synchronized Tex tex() {
            if (tex == null)
                tex = new TexI(img);
            return (tex);
        }

        public void init() {
        }
    }

	@LayerName("neg")
    public class Neg extends Layer {
        public Coord cc;
        public Coord ac, bc;
        public Coord[][] ep;

        public Neg(Message buf) {
            cc = cdec(buf);
            ac = cdec(buf);
            bc = cdec(buf);
            buf.skip(4);
            ep = new Coord[8][0];
            int en = buf.uint8();
            for (int i = 0; i < en; i++) {
                int epid = buf.uint8();
                int cn = buf.uint16();
                ep[epid] = new Coord[cn];
                for (int o = 0; o < cn; o++)
                    ep[epid][o] = cdec(buf);
            }
        }

        public void init() {
        }
    }

	@LayerName("anim")
    public class Anim extends Layer {
        private int[] ids;
        public int id, d;
        public Image[][] f;

        public Anim(Message buf) {
            id = buf.int16();
            d = buf.uint16();
            ids = new int[buf.uint16()];
            for (int i = 0; i < ids.length; i++)
                ids[i] = buf.int16();
        }

        public void init() {
            f = new Image[ids.length][];
            Image[] typeinfo = new Image[0];
            for (int i = 0; i < ids.length; i++) {
                LinkedList<Image> buf = new LinkedList<Image>();
                for (Image img : layers(Image.class)) {
                    if (img.id == ids[i])
                        buf.add(img);
                }
                f[i] = buf.toArray(typeinfo);
            }
        }
    }

    @LayerName("tileset2")
    public class Tileset extends Layer {
        private String tn = "gnd";
        public String[] tags = {};
        public Object[] ta = new Object[0];
        private transient Tiler.Factory tfac;
        public WeightList<Indir<Resource>> flavobjs = new WeightList<Indir<Resource>>();
        public WeightList<Tile> ground;
        public WeightList<Tile>[] ctrans, btrans;
        public int flavprob;

        private Tileset() {
        }

        public Tileset(Message buf) {
            while (!buf.eom()) {
                int p = buf.uint8();
                switch (p) {
                    case 0:
                        tn = buf.string();
                        ta = buf.list();
                        break;
                    case 1:
                        int flnum = buf.uint16();
                        flavprob = buf.uint16();
                        for (int i = 0; i < flnum; i++) {
                            String fln = buf.string();
                            int flv = buf.uint16();
                            int flw = buf.uint8();
                            try {
                                flavobjs.add(pool.load(fln, flv), flw);
                            } catch (RuntimeException e) {
                                throw (new LoadException("Illegal resource dependency", e, Resource.this));
                            }
                        }
                        break;
                    case 2:
                        tags = new String[buf.int8()];
                        for (int i = 0; i < tags.length; i++)
                            tags[i] = buf.string();
                        Arrays.sort(tags);
                        break;
                    default:
                        throw (new LoadException("Invalid tileset part " + p + "  in " + name, Resource.this));
                }
            }
        }

        public Tiler.Factory tfac() {
            synchronized (this) {
                if (tfac == null) {
                    CodeEntry ent = layer(CodeEntry.class);
                    if (ent != null) {
                        tfac = ent.get(Tiler.Factory.class);
                    } else {
                        if ((tfac = Tiler.byname(tn)) == null)
                            throw (new RuntimeException("Invalid tiler name in " + Resource.this.name + ": " + tn));
                    }
                }
                return (tfac);
            }
        }

        private void packtiles(Collection<Tile> tiles, Coord tsz) {
            if (tiles.size() < 1)
                return;
            int min = -1, minw = -1, minh = -1, mine = -1;
            final int nt = tiles.size();
            for (int i = 1; i <= nt; i++) {
                int w = Tex.nextp2(tsz.x * i);
                int h;
                if ((nt % i) == 0)
                    h = nt / i;
                else
                    h = (nt / i) + 1;
                h = Tex.nextp2(tsz.y * h);
                int a = w * h;
                int e = (w < h) ? h : w;
                if ((min == -1) || (a < min) || ((a == min) && (e < mine))) {
                    min = a;
                    minw = w;
                    minh = h;
                    mine = e;
                }
            }
            final Tile[] order = new Tile[nt];
            final Coord[] place = new Coord[nt];
            Tex packbuf = new TexL(new Coord(minw, minh)) {
                {
                    mipmap(Mipmapper.avg);
                    minfilter(javax.media.opengl.GL2.GL_NEAREST_MIPMAP_LINEAR);
                    centroid = true;
                }

                public BufferedImage fill() {
                    BufferedImage buf = TexI.mkbuf(dim);
                    Graphics g = buf.createGraphics();
                    for (int i = 0; i < nt; i++)
                        g.drawImage(order[i].img, place[i].x, place[i].y, null);
                    g.dispose();
                    return (buf);
                }

                public String toString() {
                    return ("TileTex(" + Resource.this.name + ")");
                }

                public String loadname() {
                    return ("tileset in " + Resource.this.name);
                }
            };
            int x = 0, y = 0, n = 0;
            for (Tile t : tiles) {
                if (y >= minh)
                    throw (new LoadException("Could not pack tiles into calculated minimum texture", Resource.this));
                order[n] = t;
                place[n] = new Coord(x, y);
                t.tex = new TexSI(packbuf, place[n], tsz);
                n++;
                if ((x += tsz.x) > (minw - tsz.x)) {
                    x = 0;
                    y += tsz.y;
                }
            }
        }

        @SuppressWarnings("unchecked")
        public void init() {
            WeightList<Tile> ground = new WeightList<Tile>();
            WeightList<Tile>[] ctrans = new WeightList[15];
            WeightList<Tile>[] btrans = new WeightList[15];
            for (int i = 0; i < 15; i++) {
                ctrans[i] = new WeightList<Tile>();
                btrans[i] = new WeightList<Tile>();
            }
            int cn = 0, bn = 0;
            Collection<Tile> tiles = new LinkedList<Tile>();
            Coord tsz = null;
            for (Tile t : layers(Tile.class)) {
                if (t.t == 'g') {
                    ground.add(t, t.w);
                } else if (t.t == 'b') {
                    btrans[t.id - 1].add(t, t.w);
                    bn++;
                } else if (t.t == 'c') {
                    ctrans[t.id - 1].add(t, t.w);
                    cn++;
                }
                tiles.add(t);
                if (tsz == null) {
                    tsz = Utils.imgsz(t.img);
                } else {
                    if (!Utils.imgsz(t.img).equals(tsz)) {
                        throw (new LoadException("Different tile sizes within set", Resource.this));
                    }
                }
            }
            if (ground.size() > 0)
                this.ground = ground;
            if (cn > 0)
                this.ctrans = ctrans;
            if (bn > 0)
                this.btrans = btrans;
            packtiles(tiles, tsz);
        }
    }

    /* Only for backwards compatibility */
    @LayerName("tileset")
    public static class OrigTileset implements LayerFactory<Tileset> {
        public Tileset cons(Resource res, Message buf) {
            Tileset ret = res.new Tileset();
            int flnum = buf.uint16();
            ret.flavprob = buf.uint16();
            for (int i = 0; i < flnum; i++) {
                String fln = buf.string();
                int flv = buf.uint16();
                int flw = buf.uint8();
                try {
                    ret.flavobjs.add(res.pool.load(fln, flv), flw);
                } catch (RuntimeException e) {
                    throw (new LoadException("Illegal resource dependency", e, res));
                }
            }
            return (ret);
        }
    }

	@LayerName("pagina")
    public class Pagina extends Layer {
        public final String text;

        public Pagina(Message buf) {
            String text = new String(buf.bytes(), Utils.utf8);
            if (language.equals("en") && !L10N_DEBUG) {
                this.text = text;
                return;
            }

            if (Resource.L10N_DEBUG) {
                Resource res = super.getres();
                Resource.l10nPagina = Resource.saveStrings(Resource.BUNDLE_PAGINA, Resource.l10nPagina, res.name, text);
            }

            String locText = null;
            Resource res = super.getres();
            if (res != null && l10nPagina != null)
                locText = l10nPagina.get(res.name);

            this.text = locText != null ? locText : text;
        }

        public void init() {
        }
    }

	@LayerName("action")
    public class AButton extends Layer {
        public final String name;
        public final Named parent;
        public final char hk;
        public final String[] ad;
        public final String origName;

        public AButton(Message buf) {
            String pr = buf.string();
            int pver = buf.uint16();
            if (pr.length() == 0) {
                parent = null;
            } else {
                try {
                    parent = pool.load(pr, pver);
                } catch (RuntimeException e) {
                    throw (new LoadException("Illegal resource dependency", e, Resource.this));
                }
            }

            origName = buf.string();
            if (language.equals("en")) {
                name = origName;
            } else {
                String locText = null;
                Resource res = super.getres();
                if (res != null && l10nAction != null)
                    locText = l10nAction.get(res.name);

                name = locText != null ? locText : origName;
            }

            buf.string(); /* Prerequisite skill */
            hk = (char) buf.uint16();
            ad = new String[buf.uint16()];
            for (int i = 0; i < ad.length; i++)
                ad[i] = buf.string();
        }

        public void init() {
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface PublishedCode {
        String name();

        Class<? extends Instancer> instancer() default Instancer.class;

        public interface Instancer {
            public Object make(Class<?> cl) throws InstantiationException, IllegalAccessException;
        }
    }

	@LayerName("code")
    public class Code extends Layer {
        public final String name;
        transient public final byte[] data;

        public Code(Message buf) {
            name = buf.string();
            data = buf.bytes();
        }

        public void init() {
        }
    }

    public class ResClassLoader extends ClassLoader {
        public ResClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Resource getres() {
            return (Resource.this);
        }

        public String toString() {
            return ("cl:" + Resource.this.toString());
        }
    }

    ;

    public static Resource classres(final Class<?> cl) {
        return (java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<Resource>() {
            public Resource run() {
                ClassLoader l = cl.getClassLoader();
                if (l instanceof ResClassLoader)
                    return (((ResClassLoader) l).getres());
                throw (new RuntimeException("Cannot fetch resource of non-resloaded class " + cl));
            }
        }));
    }

    public <T> T getcode(Class<T> cl, boolean fail) {
        CodeEntry e = layer(CodeEntry.class);
        if (e == null) {
            if (fail)
                throw (new RuntimeException("Tried to fetch non-present res-loaded class " + cl.getName() + " from " + Resource.this.name));
            return (null);
        }
        return (e.get(cl, fail));
    }

    public static class LibClassLoader extends ClassLoader {
        private final ClassLoader[] classpath;

        public LibClassLoader(ClassLoader parent, Collection<ClassLoader> classpath) {
            super(parent);
            this.classpath = classpath.toArray(new ClassLoader[0]);
        }

        public Class<?> findClass(String name) throws ClassNotFoundException {
            for (ClassLoader lib : classpath) {
                try {
                    return (lib.loadClass(name));
                } catch (ClassNotFoundException e) {
                }
            }
            throw (new ClassNotFoundException("Could not find " + name + " in any of " + Arrays.asList(classpath).toString()));
        }
    }

	@LayerName("codeentry")
    public class CodeEntry extends Layer {
        private Map<String, Code> clmap = new TreeMap<String, Code>();
        private Map<String, String> pe = new TreeMap<String, String>();
        private Collection<Indir<Resource>> classpath = new LinkedList<Indir<Resource>>();
        transient private ClassLoader loader;
        transient private Map<String, Class<?>> lpe = null;
        transient private Map<Class<?>, Object> ipe = new HashMap<Class<?>, Object>();

        public CodeEntry(Message buf) {
            while (!buf.eom()) {
                int t = buf.uint8();
                if (t == 1) {
                    while (true) {
                        String en = buf.string();
                        String cn = buf.string();
                        if (en.length() == 0)
                            break;
                        pe.put(en, cn);
                    }
                } else if (t == 2) {
                    while (true) {
                        String ln = buf.string();
                        if (ln.length() == 0)
                            break;
                        int ver = buf.uint16();
                        classpath.add(pool.load(ln, ver));
                    }
                } else {
                    throw (new LoadException("Unknown codeentry data type: " + t, Resource.this));
                }
            }
        }

        public void init() {
            for (Code c : layers(Code.class))
                clmap.put(c.name, c);
        }

        public ClassLoader loader(final boolean wait) {
            synchronized (CodeEntry.this) {
                if (this.loader == null) {
                    this.loader = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<ClassLoader>() {
                        public ClassLoader run() {
                            ClassLoader ret = Resource.class.getClassLoader();
                            if (classpath.size() > 0) {
                                Collection<ClassLoader> loaders = new LinkedList<ClassLoader>();
                                for (Indir<Resource> res : classpath) {
                                    loaders.add((wait ? Loading.waitfor(res) : res.get()).layer(CodeEntry.class).loader(wait));
                                }
                                ret = new LibClassLoader(ret, loaders);
                            }
                            if (clmap.size() > 0) {
                                ret = new ResClassLoader(ret) {
                                    public Class<?> findClass(String name) throws ClassNotFoundException {
                                        Code c = clmap.get(name);
                                        if (c == null)
                                            throw (new ClassNotFoundException("Could not find class " + name + " in resource (" + Resource.this + ")"));
                                        return (defineClass(name, c.data, 0, c.data.length));
                                    }
                                };
                            }
                            return (ret);
                        }
                    });
                }
            }
            return (this.loader);
        }

        private void load() {
            synchronized (CodeEntry.class) {
                if (lpe != null)
                    return;
                ClassLoader loader = loader(false);
                lpe = new TreeMap<String, Class<?>>();
                try {
                    for (Map.Entry<String, String> e : pe.entrySet()) {
                        String name = e.getKey();
                        String clnm = e.getValue();
                        Class<?> cl = loader.loadClass(clnm);
                        lpe.put(name, cl);
                    }
                } catch (ClassNotFoundException e) {
                    throw (new LoadException(e, Resource.this));
                }
            }
        }

        public <T> Class<? extends T> getcl(Class<T> cl, boolean fail) {
            load();
            PublishedCode entry = cl.getAnnotation(PublishedCode.class);
            if (entry == null)
                throw (new RuntimeException("Tried to fetch non-published res-loaded class " + cl.getName() + " from " + Resource.this.name));
            Class<?> acl;
            synchronized (lpe) {
                if ((acl = lpe.get(entry.name())) == null) {
                    if (fail)
                        throw (new RuntimeException("Tried to fetch non-present res-loaded class " + cl.getName() + " from " + Resource.this.name));
                    return (null);
                }
            }
            return (acl.asSubclass(cl));
        }

        public <T> Class<? extends T> getcl(Class<T> cl) {
            return (getcl(cl, true));
        }

        public <T> T get(Class<T> cl, boolean fail) {
            load();
            PublishedCode entry = cl.getAnnotation(PublishedCode.class);
            if (entry == null)
                throw (new RuntimeException("Tried to fetch non-published res-loaded class " + cl.getName() + " from " + Resource.this.name));
            Class<?> acl;
            synchronized (lpe) {
                if ((acl = lpe.get(entry.name())) == null) {
                    if (fail)
                        throw (new RuntimeException("Tried to fetch non-present res-loaded class " + cl.getName() + " from " + Resource.this.name));
                    return (null);
                }
            }
            try {
                synchronized (ipe) {
                    Object pinst;
                    if ((pinst = ipe.get(acl)) != null) {
                        return (cl.cast(pinst));
                    } else {
                        T inst;
                        Object rinst;
                        if (entry.instancer() != PublishedCode.Instancer.class)
                            rinst = entry.instancer().newInstance().make(acl);
                        else
                            rinst = acl.newInstance();
                        try {
                            inst = cl.cast(rinst);
                        } catch (ClassCastException e) {
                            throw (new ClassCastException("Published class in " + Resource.this.name + " is not of type " + cl));
                        }
                        ipe.put(acl, inst);
                        return (inst);
                    }
                }
            } catch (InstantiationException e) {
                throw (new RuntimeException(e));
            } catch (IllegalAccessException e) {
                throw (new RuntimeException(e));
            }
        }

        public <T> T get(Class<T> cl) {
            return (get(cl, true));
        }
    }

	@LayerName("audio")
    public class Audio extends Layer implements IDLayer<String> {
        transient public byte[] coded;
        public final String id;
        public double bvol = 1.0;

        public Audio(byte[] coded, String id) {
            this.coded = coded;
            this.id = id.intern();
        }

        public Audio(Message buf) {
            this(buf.bytes(), "cl");
        }

        public void init() {
        }

        public haven.Audio.CS stream() {
            try {
                return (new haven.Audio.VorbisClip(new dolda.xiphutil.VorbisStream(new ByteArrayInputStream(coded))));
            } catch (IOException e) {
                throw (new RuntimeException(e));
            }
        }

        public String layerid() {
            return (id);
        }
    }

    @LayerName("audio2")
    public static class Audio2 implements LayerFactory<Audio> {
        public Audio cons(Resource res, Message buf) {
            int ver = buf.uint8();
            if ((ver == 1) || (ver == 2)) {
                String id = buf.string();
                double bvol = 1.0;
                if (ver == 2)
                    bvol = buf.uint16() / 1000.0;
                Audio ret = res.new Audio(buf.bytes(), id);
                ret.bvol = bvol;
                return (ret);
            } else {
                throw (new LoadException("Unknown audio layer version: " + ver, res));
            }
        }
    }

    @LayerName("midi")
    public class Music extends Resource.Layer {
        transient javax.sound.midi.Sequence seq;

        public Music(Message buf) {
            try {
                seq = javax.sound.midi.MidiSystem.getSequence(new MessageInputStream(buf));
            } catch (javax.sound.midi.InvalidMidiDataException e) {
                throw (new LoadException("Invalid MIDI data", Resource.this));
            } catch (IOException e) {
                throw (new LoadException(e, Resource.this));
            }
        }

        public void init() {
        }
    }

    @LayerName("font")
    public class Font extends Layer {
        public transient final java.awt.Font font;

        public Font(Message buf) {
            int ver = buf.uint8();
            if (ver == 1) {
                int type = buf.uint8();
                if (type == 0) {
                    try {
                        this.font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, new MessageInputStream(buf));
                    } catch (Exception e) {
                        throw (new RuntimeException(e));
                    }
                } else {
                    throw (new LoadException("Unknown font type: " + type, Resource.this));
                }
            } else {
                throw (new LoadException("Unknown font layer version: " + ver, Resource.this));
            }
        }

        public void init() {
        }
    }

    public <L extends Layer> Collection<L> layers(final Class<L> cl) {
        used = true;
        return (new AbstractCollection<L>() {
            public int size() {
                int s = 0;
                for (@SuppressWarnings("unused") L l : this)
                    s++;
                return (s);
            }

            public Iterator<L> iterator() {
                return (new Iterator<L>() {
                    Iterator<Layer> i = layers.iterator();
                    L c = n();

                    private L n() {
                        while (i.hasNext()) {
                            Layer l = i.next();
                            if (cl.isInstance(l))
                                return (cl.cast(l));
                        }
                        return (null);
                    }

                    public boolean hasNext() {
                        return (c != null);
                    }

                    public L next() {
                        L ret = c;
                        if (ret == null)
                            throw (new NoSuchElementException());
                        c = n();
                        return (ret);
                    }

                    public void remove() {
                        throw (new UnsupportedOperationException());
                    }
                });
            }
        });
    }

    public <L extends Layer> L layer(Class<L> cl) {
        used = true;
        for (Layer l : layers) {
            if (cl.isInstance(l))
                return (cl.cast(l));
        }
        return (null);
    }

    public <I, L extends IDLayer<I>> L layer(Class<L> cl, I id) {
        used = true;
        for (Layer l : layers) {
            if (cl.isInstance(l)) {
                L ll = cl.cast(l);
                if (ll.layerid().equals(id))
                    return (ll);
            }
        }
        return (null);
    }

    public boolean equals(Object other) {
        if (!(other instanceof Resource))
            return (false);
        Resource o = (Resource) other;
        return (o.name.equals(this.name) && (o.ver == this.ver));
    }

    @SuppressWarnings("resource")
	private void load(InputStream st) throws IOException {
        Message in = new StreamMessage(st);
        byte[] sig = "Haven Resource 1".getBytes(Utils.ascii);
        if (!Arrays.equals(sig, in.bytes(sig.length)))
            throw (new LoadException("Invalid res signature", this));
        int ver = in.uint16();
        List<Layer> layers = new LinkedList<Layer>();
        if (this.ver == -1)
            this.ver = ver;
        else if (ver != this.ver)
            throw (new LoadException("Wrong res version (" + ver + " != " + this.ver + ")", this));
        while (!in.eom()) {
            LayerFactory<?> lc = ltypes.get(in.string());
            int len = in.int32();
            if (lc == null) {
                in.skip(len);
                continue;
            }
            Message buf = new LimitMessage(in, len);
            layers.add(lc.cons(this, buf));
            buf.skip();
        }
        this.layers = layers;
        for (Layer l : layers)
            l.init();
        used = false;
    }

    private transient Named indir = null;

    public Named indir() {
        if (indir != null)
            return (indir);
		class Ret extends Named implements Serializable {
            Ret(String name, int ver) {
                super(name, ver);
            }

            public Resource get() {
                return (Resource.this);
            }

            public String toString() {
                return (name);
            }
        }
        indir = new Ret(name, ver);
        return (indir);
    }

    public static BufferedImage loadimg(String name) {
        return (local().loadwait(name).layer(imgc).img);
    }

    public static Tex loadtex(String name) {
        return (local().loadwait(name).layer(imgc).tex());
    }

    public String toString() {
        return (name + "(v" + ver + ")");
    }

    public static void loadlist(Pool pool, InputStream list, int prio) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(list, "us-ascii"));
        String ln;
        while ((ln = in.readLine()) != null) {
            int pos = ln.indexOf(':');
            if (pos < 0)
                continue;
            String nm = ln.substring(0, pos);
            int ver;
            try {
                ver = Integer.parseInt(ln.substring(pos + 1));
            } catch (NumberFormatException e) {
                continue;
            }
            try {
                pool.load(nm, ver, prio);
            } catch (RuntimeException e) {
            }
        }
        in.close();
    }

    public static void dumplist(Collection<Resource> list, Writer dest) {
        PrintWriter out = new PrintWriter(dest);
        List<Resource> sorted = new ArrayList<Resource>(list);
        Collections.sort(sorted, new Comparator<Resource>() {
            public int compare(Resource a, Resource b) {
                return (a.name.compareTo(b.name));
            }
        });
        for (Resource res : sorted)
            out.println(res.name + ":" + res.ver);
    }

    public static void updateloadlist(File file, File resdir) throws Exception {
        BufferedReader r = new BufferedReader(new FileReader(file));
        Map<String, Integer> orig = new HashMap<String, Integer>();
        String ln;
        while ((ln = r.readLine()) != null) {
            int pos = ln.indexOf(':');
            if (pos < 0) {
                System.err.println("Weird line: " + ln);
                continue;
            }
            String nm = ln.substring(0, pos);
            int ver = Integer.parseInt(ln.substring(pos + 1));
            orig.put(nm, ver);
        }
        r.close();
        Pool pool = new Pool(new FileSource(resdir));
        for (String nm : orig.keySet())
            pool.load(nm);
        while (true) {
            int d = pool.qdepth();
            if (d == 0)
                break;
            System.out.print("\033[1GLoading... " + d + "\033[K");
            Thread.sleep(500);
        }
        System.out.println();
        Collection<Resource> cur = new LinkedList<Resource>();
        for (Map.Entry<String, Integer> e : orig.entrySet()) {
            String nm = e.getKey();
            int ver = e.getValue();
            Resource res = Loading.waitfor(pool.load(nm));
            if (res.ver != ver)
                System.out.println(nm + ": " + ver + " -> " + res.ver);
            cur.add(res);
        }
        Writer w = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
        try {
            dumplist(cur, w);
        } finally {
            w.close();
        }
    }

    public static String getLocString(Map<String, String> l10nMap, String label) {
        if (!language.equals("en") && l10nMap != null) {
            String ll = l10nMap.get(label);
            if (ll != null)
                label = ll;
        }
        return label;
    }

    public static String getLocStringOrNull(Map<String, String> l10nMap, String label) {
        if (!language.equals("en") && l10nMap != null) {
            String ll = l10nMap.get(label);
            if (ll != null)
                return ll;
        }
        return null;
    }

    public static String getLocString(Map<String, String> l10nMap, String key, String def) {
        if (!language.equals("en") && l10nMap != null) {
            String ll = l10nMap.get(key);
            if (ll != null)
                return ll;
        }
        return def;
    }

    public static Map<String, String> saveStrings(String bundle, Map<String, String> map, String key, String val) {
        synchronized (Resource.class) {
            if (key == null || key.equals(""))
                return map;

            if (map != null && map.containsKey(key))
                return map;

            if (val == null)
                val = key;

            try {
                Integer.parseInt(key);
                return map;
            } catch (NumberFormatException nfe) {
            }
            try {
                Integer.parseInt(val);
                return map;
            } catch (NumberFormatException nfe) {
            }

            if (key.startsWith("Village shield:") || key.startsWith("Essence:") ||  // inspect tool
                    key.endsWith("is ONLINE") || key.endsWith("is offline") ||      // kin online/offline
                    key.startsWith("Experience points gained:"))
                return map;

            new File("l10n").mkdirs();

            CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
            encoder.onMalformedInput(CodingErrorAction.REPORT);
            encoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            BufferedWriter out = null;
            try {
                key = key.replace(" ", "\\ ").replace(":", "\\:").replace("=", "\\=");
                val = val.replace("\\", "\\\\").replace("\n", "\\n").replace("\u0000", "");
                out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("l10n/" + bundle + "_new.properties", true), encoder));
                out.write(key + " = " + val);
                out.newLine();
                if (map == null)
                    map = new HashMap<>();
                map.put(key, val);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return map;
        }
    }

    public static void main(String[] args) throws Exception {
        String cmd = args[0].intern();
        if (cmd == "update") {
            updateloadlist(new File(args[1]), new File(args[2]));
        }
    }
}
