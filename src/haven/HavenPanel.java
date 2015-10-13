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

import java.awt.GraphicsConfiguration;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import java.util.*;
import javax.media.opengl.*;
import javax.media.opengl.awt.*;
import javax.media.opengl.glu.GLU;

public class HavenPanel extends GLCanvas implements Runnable, Console.Directory {
    UI ui;
    public static UI lui;
    boolean inited = false;
    public static int w, h;
    public boolean bgmode = false;
    public static long bgfd = Utils.getprefi("bghz", 200);
    long fd = 10, fps = 0;
    double uidle = 0.0, ridle = 0.0;
    Queue<InputEvent> events = new LinkedList<InputEvent>();
    private String cursmode = "tex";
    private Resource lastcursor = null;
    public Coord mousepos = new Coord(0, 0);
    private MouseEvent mousemv = null;
    public CPUProfile uprof = new CPUProfile(300), rprof = new CPUProfile(300);
    public GPUProfile gprof = new GPUProfile(300);
    public static final GLState.Slot<GLState> global = new GLState.Slot<GLState>(GLState.Slot.Type.SYS, GLState.class);
    public static final GLState.Slot<GLState> proj2d = new GLState.Slot<GLState>(GLState.Slot.Type.SYS, GLState.class, global);
    private GLState gstate, rtstate, ostate;
    private GLState.Applier state = null;
    private GLConfig glconf = null;

    private static GLCapabilities stdcaps() {
        GLProfile prof = GLProfile.getDefault();
        GLCapabilities cap = new GLCapabilities(prof);
        cap.setDoubleBuffered(true);
        cap.setAlphaBits(8);
        cap.setRedBits(8);
        cap.setGreenBits(8);
        cap.setBlueBits(8);
        cap.setSampleBuffers(true);
        cap.setNumSamples(4);
        return (cap);
    }

    public HavenPanel(int w, int h, GLCapabilitiesChooser cc) {
        super(stdcaps(), cc, null, null);
        setSize(this.w = w, this.h = h);
        newui(null);
        initgl();
        if (Toolkit.getDefaultToolkit().getMaximumCursorColors() >= 256 || Config.hwcursor)
            cursmode = "awt";
        setCursor(Toolkit.getDefaultToolkit().createCustomCursor(TexI.mkbuf(new Coord(1, 1)), new java.awt.Point(), ""));
    }

    public HavenPanel(int w, int h) {
        this(w, h, null);
    }

    private void initgl() {
        final Thread caller = Thread.currentThread();
        final haven.error.ErrorHandler h = haven.error.ErrorHandler.find();
        addGLEventListener(new GLEventListener() {
            Debug.DumpGL dump = null;

            public void display(GLAutoDrawable d) {
                GL2 gl = d.getGL().getGL2();
            /*
            if((dump == null) || (dump.getDownstreamGL() != gl))
			dump = new Debug.DumpGL((GL4bc)gl);
		    if(Debug.kf2 && !Debug.pk2)
			dump.dump("/tmp/gldump");
		    dump.reset();
		    gl = dump;
		    */
                if (inited)
                    redraw(gl);
            }

            public void init(GLAutoDrawable d) {
                GL gl = d.getGL();
                glconf = GLConfig.fromgl(gl, d.getContext(), getChosenGLCapabilities());
                glconf.pref = GLSettings.load(glconf, true);
                ui.cons.add(glconf);
                if (h != null) {
                    h.lsetprop("gl.vendor", gl.glGetString(gl.GL_VENDOR));
                    h.lsetprop("gl.version", gl.glGetString(gl.GL_VERSION));
                    h.lsetprop("gl.renderer", gl.glGetString(gl.GL_RENDERER));
                    h.lsetprop("gl.exts", Arrays.asList(gl.glGetString(gl.GL_EXTENSIONS).split(" ")));
                    h.lsetprop("gl.caps", d.getChosenGLCapabilities().toString());
                    h.lsetprop("gl.conf", glconf);
                }
                gstate = new GLState() {
                    public void apply(GOut g) {
                        BGL gl = g.gl;
                        gl.glColor3f(1, 1, 1);
                        gl.glPointSize(4);
                        gl.joglSetSwapInterval(1);
                        gl.glEnable(GL.GL_BLEND);
                        //gl.glEnable(GL.GL_LINE_SMOOTH);
                        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                        if (g.gc.glmajver >= 2)
                            gl.glBlendEquationSeparate(GL.GL_FUNC_ADD, GL2.GL_MAX);
                        if (g.gc.havefsaa()) {
                    /* Apparently, having sample
				     * buffers in the config enables
				     * multisampling by default on
				     * some systems. */
                            g.gl.glDisable(GL.GL_MULTISAMPLE);
                        }
                        GOut.checkerr(gl);
                    }

                    public void unapply(GOut g) {
                    }

                    public void prep(Buffer buf) {
                        buf.put(global, this);
                    }
                };
            }

            public void reshape(GLAutoDrawable d, final int x, final int y, final int w, final int h) {
                ostate = OrthoState.fixed(new Coord(w, h));
                rtstate = new GLState() {
                    public void apply(GOut g) {
                        g.st.proj = Projection.makeortho(new Matrix4f(), 0, w, 0, h, -1, 1);
                    }

                    public void unapply(GOut g) {
                    }

                    public void prep(Buffer buf) {
                        buf.put(proj2d, this);
                    }
                };
                HavenPanel.this.w = w;
                HavenPanel.this.h = h;
            }

            public void displayChanged(GLAutoDrawable d, boolean cp1, boolean cp2) {
            }

            public void dispose(GLAutoDrawable d) {
            }
        });
    }

    public static abstract class OrthoState extends GLState {
        protected abstract Coord sz();

        public void apply(GOut g) {
            Coord sz = sz();
            g.st.proj = Projection.makeortho(new Matrix4f(), 0, sz.x, sz.y, 0, -1, 1);
        }

        public void unapply(GOut g) {
        }

        public void prep(Buffer buf) {
            buf.put(proj2d, this);
        }

        public static OrthoState fixed(final Coord sz) {
            return (new OrthoState() {
                protected Coord sz() {
                    return (sz);
                }
            });
        }
    }

    public void init() {
        setFocusTraversalKeysEnabled(false);
        newui(null);
        addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                synchronized (events) {
                    events.add(e);
                    events.notifyAll();
                }
            }

            public void keyPressed(KeyEvent e) {
                synchronized (events) {
                    events.add(e);
                    events.notifyAll();
                }
            }

            public void keyReleased(KeyEvent e) {
                synchronized (events) {
                    events.add(e);
                    events.notifyAll();
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                synchronized (events) {
                    events.add(e);
                    events.notifyAll();
                }
            }

            public void mouseReleased(MouseEvent e) {
                synchronized (events) {
                    events.add(e);
                    events.notifyAll();
                }
            }
        });
        addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) {
                synchronized (events) {
                    mousemv = e;
                }
            }

            public void mouseMoved(MouseEvent e) {
                synchronized (events) {
                    mousemv = e;
                }
            }
        });
        addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                synchronized (events) {
                    events.add(e);
                    events.notifyAll();
                }
            }
        });
        inited = true;
    }

    UI newui(Session sess) {
        if (ui != null)
            ui.destroy();
        ui = new UI(new Coord(w, h), sess);
        ui.root.guprof = uprof;
        ui.root.grprof = rprof;
        ui.root.ggprof = gprof;
        if (getParent() instanceof Console.Directory)
            ui.cons.add((Console.Directory) getParent());
        ui.cons.add(this);
        if (glconf != null)
            ui.cons.add(glconf);
        lui = ui;
        return (ui);
    }

    private static Cursor makeawtcurs(BufferedImage img, Coord hs) {
        java.awt.Dimension cd = Toolkit.getDefaultToolkit().getBestCursorSize(img.getWidth(), img.getHeight());
        BufferedImage buf = TexI.mkbuf(new Coord((int) cd.getWidth(), (int) cd.getHeight()));
        java.awt.Graphics g = buf.getGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return (Toolkit.getDefaultToolkit().createCustomCursor(buf, new java.awt.Point(hs.x, hs.y), ""));
    }

    void rootdraw(GLState.Applier state, UI ui, BGL gl) {
        GLState.Buffer ibuf = new GLState.Buffer(state.cfg);
        gstate.prep(ibuf);
        ostate.prep(ibuf);
        GOut g = new GOut(gl, state.cgl, state.cfg, state, ibuf, new Coord(w, h));
        state.set(ibuf);

        g.state(ostate);
        g.apply();
        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        synchronized (ui) {
            ui.draw(g);
        }

        if (Config.showfps) {
            FastText.aprintf(g, new Coord(HavenPanel.w - 50, 15), 0, 1, "FPS: %d", fps);
        }

        if (Config.dbtext) {
            int y = h - 150;
            FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "FPS: %d (%d%%, %d%% idle)", fps, (int) (uidle * 100.0), (int) (ridle * 100.0));
            Runtime rt = Runtime.getRuntime();
            long free = rt.freeMemory(), total = rt.totalMemory();
            FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "Mem: %,011d/%,011d/%,011d/%,011d", free, total - free, total, rt.maxMemory());
            FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "Tex-current: %d", TexGL.num());
            FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "GL progs: %d", g.st.numprogs());
            GameUI gi = ui.root.findchild(GameUI.class);
            if ((gi != null) && (gi.map != null)) {
                try {
                    FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "MV pos: %s (%s)", gi.map.getcc(), gi.map.camera);
                } catch (Loading e) {
                }
                if (gi.map.rls != null)
                    FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "Rendered: %,d+%,d(%,d)", gi.map.rls.drawn, gi.map.rls.instanced, gi.map.rls.instancified);
            }
            if (Resource.remote().qdepth() > 0)
                FastText.aprintf(g, new Coord(10, y -= 15), 0, 1, "RQ depth: %d (%d)", Resource.remote().qdepth(), Resource.remote().numloaded());
        }
        Object tooltip;
        try {
            synchronized (ui) {
                tooltip = ui.root.tooltip(mousepos, ui.root);
            }
        } catch (Loading e) {
            tooltip = "...";
        }
        Tex tt = null;
        if (tooltip != null) {
            if (tooltip instanceof Text) {
                tt = ((Text) tooltip).tex();
            } else if (tooltip instanceof Tex) {
                tt = (Tex) tooltip;
            } else if (tooltip instanceof Indir<?>) {
                Indir<?> t = (Indir<?>) tooltip;
                Object o = t.get();
                if (o instanceof Tex)
                    tt = (Tex) o;
            } else if (tooltip instanceof String) {
                if (((String) tooltip).length() > 0)
                    tt = (Text.render((String) tooltip)).tex();
            }
        }
        if (tt != null) {
            Coord sz = tt.sz();
            Coord pos = mousepos.add(sz.inv());
            if (pos.x < 0)
                pos.x = 0;
            if (pos.y < 0)
                pos.y = 0;
            g.chcolor(244, 247, 21, 192);
            g.rect(pos.add(-3, -3), sz.add(6, 6));
            g.chcolor(35, 35, 35, 192);
            g.frect(pos.add(-2, -2), sz.add(4, 4));
            g.chcolor();
            g.image(tt, pos);
        }
        ui.lasttip = tooltip;
        Resource curs = ui.root.getcurs(mousepos);
        if (curs != null) {
            if (cursmode == "awt") {
                if (curs != lastcursor) {
                    try {
                        setCursor(makeawtcurs(curs.layer(Resource.imgc).img, curs.layer(Resource.negc).cc));
                        lastcursor = curs;
                    } catch (Exception e) {
                        cursmode = "tex";
                    }
                }
            } else if (cursmode == "tex") {
                Coord dc = mousepos.add(curs.layer(Resource.negc).cc.inv());
                g.image(curs.layer(Resource.imgc), dc);
            }
        }
        state.clean();
        GLObject.disposeall(state.cgl, gl);
    }

    private static class Frame {
        BGL buf;
        CurrentGL on;
        CPUProfile.Frame pf;
        long doneat;

        Frame(BGL buf, CurrentGL on) {
            this.buf = buf;
            this.on = on;
        }
    }

    private Frame[] curdraw = {null};

    void redraw(GL2 gl) {
        if ((state == null) || (state.cgl.gl != gl))
            state = new GLState.Applier(new CurrentGL(gl, glconf));

        Frame f;
        synchronized (curdraw) {
            f = curdraw[0];
            curdraw[0] = null;
        }
        if ((f != null) && (f.on.gl == gl)) {
            GPUProfile.Frame curgf = null;
            if (Config.profilegpu)
                curgf = gprof.new Frame((GL3) gl);
            if (f.pf != null)
                f.pf.tick("awt");
            f.buf.run(gl);
            GOut.checkerr(gl);
            if (f.pf != null)
                f.pf.tick("gl");
            if (curgf != null) {
                curgf.tick("draw");
                curgf.fin();
            }

            if (glconf.pref.dirty) {
                glconf.pref.save();
                glconf.pref.dirty = false;
            }
            f.doneat = System.currentTimeMillis();
        }
    }

    void dispatch() {
        synchronized (events) {
            if (mousemv != null) {
                mousepos = new Coord(mousemv.getX(), mousemv.getY());
                ui.mousemove(mousemv, mousepos);
                mousemv = null;
            }
            InputEvent e = null;
            while ((e = events.poll()) != null) {
                if (e instanceof MouseEvent) {
                    MouseEvent me = (MouseEvent) e;
                    if (me.getID() == MouseEvent.MOUSE_PRESSED) {
                        ui.mousedown(me, new Coord(me.getX(), me.getY()), me.getButton());
                    } else if (me.getID() == MouseEvent.MOUSE_RELEASED) {
                        ui.mouseup(me, new Coord(me.getX(), me.getY()), me.getButton());
                    } else if (me instanceof MouseWheelEvent) {
                        ui.mousewheel(me, new Coord(me.getX(), me.getY()), ((MouseWheelEvent) me).getWheelRotation());
                    }
                } else if (e instanceof KeyEvent) {
                    KeyEvent ke = (KeyEvent) e;
                    if (ke.getID() == KeyEvent.KEY_PRESSED) {
                        ui.keydown(ke);
                    } else if (ke.getID() == KeyEvent.KEY_RELEASED) {
                        ui.keyup(ke);
                    } else if (ke.getID() == KeyEvent.KEY_TYPED) {
                        ui.type(ke);
                    }
                }
                ui.lastevent = System.currentTimeMillis();
            }
        }
    }

    private Frame bufdraw = null;
    private final Runnable drawfun = new Runnable() {
        private void uglyjoglhack() throws InterruptedException {
            try {
                display();
            } catch (RuntimeException e) {
                if (e.getCause() instanceof InterruptedException) {
                    throw ((InterruptedException) e.getCause());
                } else {
                    throw (e);
                }
            }
        }

        public void run() {
            try {
                uglyjoglhack();
                if (state == null)
                    throw (new RuntimeException("State applier is still null after redraw"));
                synchronized (drawfun) {
                    drawfun.notifyAll();
                }
                while (true) {
                    long then = System.currentTimeMillis();
                    int waited = 0;
                    Frame current;
                    synchronized (drawfun) {
                        while ((current = bufdraw) == null)
                            drawfun.wait();
                        bufdraw = null;
                        drawfun.notifyAll();
                        waited += System.currentTimeMillis() - then;
                    }
                    CPUProfile.Frame curf = null;
                    if (Config.profile)
                        current.pf = curf = rprof.new Frame();
                    synchronized (curdraw) {
                        curdraw[0] = current;
                    }
                    uglyjoglhack();
                    if (curf != null) {
                        curf.tick("aux");
                        curf.fin();
                    }
                    long now = System.currentTimeMillis();
                    waited += now - current.doneat;
                    ridle = (ridle * 0.95) + (((double) waited / ((double) (now - then))) * 0.05);
                    current = null; /* Just for the GC. */
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    };

    public void run() {
        try {
            Thread drawthread = new HackThread(drawfun, "Render thread");
            drawthread.start();
            synchronized (drawfun) {
                while (state == null)
                    drawfun.wait();
            }
            try {
                long now, then;
                long frames[] = new long[128];
                int framep = 0, waited[] = new int[128];
                while (true) {
                    int fwaited = 0;
                    Debug.cycle();
                    UI ui = this.ui;
                    then = System.currentTimeMillis();
                    CPUProfile.Frame curf = null;
                    if (Config.profile)
                        curf = uprof.new Frame();
                    synchronized (ui) {
                        if (ui.sess != null)
                            ui.sess.glob.ctick();
                        dispatch();
                        ui.tick();
                        if ((ui.root.sz.x != w) || (ui.root.sz.y != h))
                            ui.root.resize(new Coord(w, h));
                    }
                    if (curf != null)
                        curf.tick("dsp");

                    BGL buf = new BGL();
                    GLState.Applier state = this.state;
                    rootdraw(state, ui, buf);
                    if (curf != null)
                        curf.tick("draw");
                    synchronized (drawfun) {
                        now = System.currentTimeMillis();
                        while (bufdraw != null)
                            drawfun.wait();
                        bufdraw = new Frame(buf, state.cgl);
                        drawfun.notifyAll();
                        fwaited += System.currentTimeMillis() - now;
                    }

                    ui.audio.cycle();
                    if (curf != null)
                        curf.tick("aux");

                    now = System.currentTimeMillis();
                    long fd = bgmode ? this.bgfd : this.fd;
                    if (now - then < fd) {
                        synchronized (events) {
                            events.wait(fd - (now - then));
                        }
                        fwaited += System.currentTimeMillis() - now;
                    }

                    frames[framep] = now;
                    waited[framep] = fwaited;
                    for (int i = 0, ckf = framep, twait = 0; i < frames.length; i++) {
                        ckf = (ckf - 1 + frames.length) % frames.length;
                        twait += waited[ckf];
                        if (now - frames[ckf] > 1000) {
                            fps = i;
                            uidle = ((double) twait) / ((double) (now - frames[ckf]));
                            break;
                        }
                    }
                    framep = (framep + 1) % frames.length;

                    if (curf != null)
                        curf.tick("wait");
                    if (curf != null)
                        curf.fin();
                    if (Thread.interrupted())
                        throw (new InterruptedException());
                }
            } finally {
                drawthread.interrupt();
                drawthread.join();
            }
        } catch (InterruptedException e) {
        } finally {
            ui.destroy();
        }
    }

    public GraphicsConfiguration getconf() {
        return (getGraphicsConfiguration());
    }

    private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();

    {
        cmdmap.put("hz", new Console.Command() {
            public void run(Console cons, String[] args) {
                fd = 1000 / Integer.parseInt(args[1]);
            }
        });
        cmdmap.put("bghz", new Console.Command() {
            public void run(Console cons, String[] args) {
                bgfd = 1000 / Integer.parseInt(args[1]);
                Utils.setprefi("bghz", (int) bgfd);
            }
        });
    }

    public Map<String, Console.Command> findcmds() {
        return (cmdmap);
    }
}
