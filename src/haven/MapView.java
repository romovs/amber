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

import haven.GLProgram.VarID;
import haven.automation.*;
import haven.pathfinder.PFListener;
import haven.pathfinder.Pathfinder;
import haven.resutil.BPRadSprite;

import javax.media.opengl.GL;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static haven.MCache.tilesz;
import static haven.OCache.posres;

public class MapView extends PView implements DTarget, Console.Directory, PFListener {
    public static boolean clickdb = false;
    public static long plgob = -1;
    public static Coord2d pllastcc;
    public Coord2d cc;
    public final Glob glob;
    private static final int view = 2;
    private Collection<Delayed> delayed = new LinkedList<Delayed>();
    private Collection<Delayed> delayed2 = new LinkedList<Delayed>();
    private Collection<Rendered> extradraw = new LinkedList<Rendered>();
    public Camera camera = restorecam();
    private Plob placing = null;
    private int[] visol = new int[32];
    private Grabber grab;
    private Selector selection;
    private MCache.Overlay miningOverlay;
    private Coord3f camoff = new Coord3f(Coord3f.o);
    public double shake = 0.0;
    public static int plobgran = 8;
    private static final Map<String, Class<? extends Camera>> camtypes = new HashMap<String, Class<? extends Camera>>();
    private String tooltip;
    private boolean showgrid;
    private TileOutline gridol;
    private Coord lasttc = Coord.z;
    private static final Gob.Overlay rovlsupport = new Gob.Overlay(new BPRadSprite(100.0F, 0, BPRadSprite.smatDanger));
    private static final Gob.Overlay rovlcolumn = new Gob.Overlay(new BPRadSprite(125.0F, 0, BPRadSprite.smatDanger));
    private static final Gob.Overlay rovlbeam = new Gob.Overlay(new BPRadSprite(150.0F, 0, BPRadSprite.smatDanger));
    private static final Gob.Overlay rovltrough = new Gob.Overlay(new BPRadSprite(200.0F, -10.0F, BPRadSprite.smatTrough));
    private static final Gob.Overlay rovlbeehive = new Gob.Overlay(new BPRadSprite(151.0F, -10.0F, BPRadSprite.smatBeehive));
    private long lastmmhittest = System.currentTimeMillis();
    private Coord lasthittestc = Coord.z;
    private GobSelectCallback gobselcb;
    private AreaSelectCallback areaselcb;
    private Pathfinder pf;
    public Thread pfthread;
    public SteelRefueler steelrefueler;
    private Thread musselPicker;
    private final PartyHighlight partyHighlight;
    public static final Set<Long> markedGobs = new HashSet<>();
    public static final Material.Colors markedFx = new Material.Colors(new Color(21, 127, 208, 255));
    public Object[] lastItemactClickArgs;

    public interface Delayed {
        public void run(GOut g);
    }

    public interface Grabber {
        boolean mmousedown(Coord mc, int button);

        boolean mmouseup(Coord mc, int button);

        boolean mmousewheel(Coord mc, int amount);

        void mmousemove(Coord mc);
    }

    public abstract class Camera extends GLState.Abstract {
        protected haven.Camera view = new haven.Camera(Matrix4f.identity());
        protected Projection proj = new Projection(Matrix4f.identity());

        public Camera() {
            resized();
        }

        public boolean keydown(KeyEvent ev) {
            return (false);
        }

        public boolean click(Coord sc) {
            return (false);
        }

        public void drag(Coord sc) {
        }

        public void release() {
        }

        public boolean wheel(Coord sc, int amount) {
            return (false);
        }

        public void resized() {
            float field = 0.5f;
            float aspect = ((float) sz.y) / ((float) sz.x);
            proj.update(Projection.makefrustum(new Matrix4f(), -field, field, -aspect * field, aspect * field, 1, 5000));
        }

        public void prep(Buffer buf) {
            proj.prep(buf);
            view.prep(buf);
        }

        public abstract float angle();

        public abstract void tick(double dt);
    }

    public class FollowCam extends Camera {
        private final float fr = 0.0f, h = 10.0f;
        private float ca, cd;
        private Coord3f curc = null;
        private float elev, telev;
        private float angl, tangl;
        private Coord dragorig = null;
        private float anglorig;

        public FollowCam() {
            elev = telev = (float) Math.PI / 6.0f;
            angl = tangl = 0.0f;
        }

        public void resized() {
            ca = (float) sz.y / (float) sz.x;
            cd = 400.0f * ca;
        }

        public boolean click(Coord c) {
            anglorig = tangl;
            dragorig = c;
            return (true);
        }

        public void drag(Coord c) {
            tangl = anglorig + ((float) (c.x - dragorig.x) / 100.0f);
            tangl = tangl % ((float) Math.PI * 2.0f);
        }

        private double f0 = 0.2, f1 = 0.5, f2 = 0.9;
        private double fl = Math.sqrt(2);
        private double fa = ((fl * (f1 - f0)) - (f2 - f0)) / (fl - 2);
        private double fb = ((f2 - f0) - (2 * (f1 - f0))) / (fl - 2);

        private float field(float elev) {
            double a = elev / (Math.PI / 4);
            return ((float) (f0 + (fa * a) + (fb * Math.sqrt(a))));
        }

        private float dist(float elev) {
            float da = (float) Math.atan(ca * field(elev));
            return ((float) (((cd - (h / Math.tan(elev))) * Math.sin(elev - da) / Math.sin(da)) - (h / Math.sin(elev))));
        }

        public void tick(double dt) {
            elev += (telev - elev) * (float) (1.0 - Math.pow(500, -dt));
            if (Math.abs(telev - elev) < 0.0001)
                elev = telev;

            float dangl = tangl - angl;
            while (dangl > Math.PI) dangl -= (float) (2 * Math.PI);
            while (dangl < -Math.PI) dangl += (float) (2 * Math.PI);
            angl += dangl * (float) (1.0 - Math.pow(500, -dt));
            if (Math.abs(tangl - angl) < 0.0001)
                angl = tangl;

            Coord3f cc = getcc();
            cc.y = -cc.y;
            if (curc == null)
                curc = cc;
            float dx = cc.x - curc.x, dy = cc.y - curc.y;
            float dist = (float) Math.sqrt((dx * dx) + (dy * dy));
            if (dist > 250) {
                curc = cc;
            } else if (dist > fr) {
                Coord3f oc = curc;
                float pd = (float) Math.cos(elev) * dist(elev);
                Coord3f cambase = new Coord3f(curc.x + ((float) Math.cos(tangl) * pd), curc.y + ((float) Math.sin(tangl) * pd), 0.0f);
                float a = cc.xyangle(curc);
                float nx = cc.x + ((float) Math.cos(a) * fr), ny = cc.y + ((float) Math.sin(a) * fr);
                Coord3f tgtc = new Coord3f(nx, ny, cc.z);
                curc = curc.add(tgtc.sub(curc).mul((float) (1.0 - Math.pow(500, -dt))));
                if (curc.dist(tgtc) < 0.01)
                    curc = tgtc;
                tangl = curc.xyangle(cambase);
            }

            float field = field(elev);
            view.update(PointedCam.compute(curc.add(camoff).add(0.0f, 0.0f, h), dist(elev), elev, angl));
            proj.update(Projection.makefrustum(new Matrix4f(), -field, field, -ca * field, ca * field, 1, 5000));
        }

        public float angle() {
            return (angl);
        }

        private static final float maxang = (float) (Math.PI / 2 - 0.1);
        private static final float mindist = 50.0f;

        public boolean wheel(Coord c, int amount) {
            float fe = telev;
            telev += amount * telev * 0.02f;
            if (telev > maxang)
                telev = maxang;
            if (dist(telev) < mindist)
                telev = fe;
            return (true);
        }

        public String toString() {
            return (String.format("%f %f %f", elev, dist(elev), field(elev)));
        }
    }

    static {
        camtypes.put("follow", FollowCam.class);
    }

    public class FreeCam extends Camera {
        private float dist = 50.0f;
        private float elev = (float) Math.PI / 4.0f;
        private float angl = 0.0f;
        private Coord dragorig = null;
        private float elevorig, anglorig;

        public void tick(double dt) {
            Coord3f cc = getcc();
            cc.y = -cc.y;
            if (Config.disableelev)
                cc.z = 0;
            view.update(PointedCam.compute(cc.add(camoff).add(0.0f, 0.0f, 15f), dist, elev, angl));
        }

        public float angle() {
            return (angl);
        }

        public boolean click(Coord c) {
            elevorig = elev;
            anglorig = angl;
            dragorig = c;
            return (true);
        }

        public void drag(Coord c) {
            if (c == null || dragorig == null)
                return;
            if (Config.reversebadcamx)
                c = new Coord(c.x + (dragorig.x - c.x) * 2, c.y);
            if (Config.reversebadcamy)
                c = new Coord(c.x, c.y + (dragorig.y - c.y) * 2);
            elev = elevorig - ((float) (c.y - dragorig.y) / 100.0f);
            if (elev < 0.0f) elev = 0.0f;
            if (elev > (Math.PI / 2.0)) elev = (float) Math.PI / 2.0f;
            angl = anglorig + ((float) (c.x - dragorig.x) / 100.0f);
            angl = angl % ((float) Math.PI * 2.0f);
        }

        public boolean wheel(Coord c, int amount) {
            float d = dist + (amount * Config.badcamsensitivity);
            if (d < 5)
                d = 5;
            dist = d;
            return (true);
        }
    }

    static {
        camtypes.put("bad", FreeCam.class);
    }

    public class OrthoCam extends Camera {
        public boolean exact;
        protected float dist = 500.0f;
        protected float elev = (float) Math.PI / 6.0f;
        protected float angl = -(float) Math.PI / 4.0f;
        protected float field = (float) (100 * Math.sqrt(2));
        private Coord dragorig = null;
        private float anglorig;
        protected Coord3f cc, jc;

        public OrthoCam(boolean exact) {
            this.exact = exact;
        }

        public OrthoCam() {
            this(false);
        }

        public void tick2(double dt) {
            Coord3f cc = getcc();
            cc.y = -cc.y;
            if (Config.disableelev)
                cc.z = 0;
            this.cc = cc;
        }

        public void tick(double dt) {
            tick2(dt);
            float aspect = ((float) sz.y) / ((float) sz.x);
            Matrix4f vm = PointedCam.compute(cc.add(camoff).add(0.0f, 0.0f, 15f), dist, elev, angl);
            if (exact) {
                if (jc == null)
                    jc = cc;
                float pfac = sz.x / (field * 2);
                Coord3f vjc = vm.mul4(jc).mul(pfac);
                Coord3f corr = new Coord3f(Math.round(vjc.x) - vjc.x, Math.round(vjc.y) - vjc.y, 0).div(pfac);
                if ((Math.abs(vjc.x) > 500) || (Math.abs(vjc.y) > 500))
                    jc = null;
                vm = Location.makexlate(new Matrix4f(), corr).mul1(vm);
            }
            view.update(vm);
            proj.update(Projection.makeortho(new Matrix4f(), -field, field, -field * aspect, field * aspect, 1, 5000));
        }

        public float angle() {
            return (angl);
        }

        public boolean click(Coord c) {
            anglorig = angl;
            dragorig = c;
            return (true);
        }

        public void drag(Coord c) {
            angl = anglorig + ((float) (c.x - dragorig.x) / 100.0f);
            angl = angl % ((float) Math.PI * 2.0f);
        }

        public String toString() {
            return (String.format("%f %f %f %f", dist, elev / Math.PI, angl / Math.PI, field));
        }
    }

    public class SOrthoCam extends OrthoCam {
        private Coord dragorig = null;
        private float anglorig;
        private float tangl = angl;
        private float tfield = field;
        private final float pi2 = (float) (Math.PI * 2);

        public SOrthoCam(boolean exact) {
            super(exact);
        }

        public SOrthoCam(String... args) {
            PosixArgs opt = PosixArgs.getopt(args, "e");
            for (char c : opt.parsed()) {
                switch (c) {
                    case 'e':
                        exact = true;
                        break;
                }
            }
        }

        public void tick2(double dt) {
            Coord3f mc = getcc();
            mc.y = -mc.y;
            if (Config.disableelev)
                mc.z = 0;
            if ((cc == null) || (Math.hypot(mc.x - cc.x, mc.y - cc.y) > 250))
                cc = mc;
            else if (!exact || (mc.dist(cc) > 2))
                cc = cc.add(mc.sub(cc).mul(1f - (float) Math.pow(500, -dt)));

            angl = angl + ((tangl - angl) * (1f - (float) Math.pow(500, -dt)));
            while (angl > pi2) {
                angl -= pi2;
                tangl -= pi2;
                anglorig -= pi2;
            }
            while (angl < 0) {
                angl += pi2;
                tangl += pi2;
                anglorig += pi2;
            }
            if (Math.abs(tangl - angl) < 0.001)
                angl = tangl;
            else
                jc = cc;

            field = field + ((tfield - field) * (1f - (float) Math.pow(500, -dt)));
            if (Math.abs(tfield - field) < 0.1)
                field = tfield;
            else
                jc = cc;
        }

        public boolean click(Coord c) {
            anglorig = angl;
            dragorig = c;
            return (true);
        }

        public void drag(Coord c) {
            if (c == null || dragorig == null)
                return;
            tangl = anglorig + ((float) (c.x - dragorig.x) / 100.0f);
        }

        private void chfield(float nf) {
            tfield = nf;
            float zoutlimdiv = Config.enableorthofullzoom ? 4f : 8f;
            tfield = Math.max(Math.min(tfield, sz.x * (float) Math.sqrt(2) / zoutlimdiv), 50);
            if (tfield > 100)
                release();
        }

        public boolean wheel(Coord c, int amount) {
            chfield(tfield + amount * 10);
            return (true);
        }

        public boolean keydown(KeyEvent ev) {
            if (ui.modctrl) {
                if (ev.getKeyCode() == KeyEvent.VK_LEFT) {
                    tangl = (float) (2 * Math.PI);
                    return (true);
                } else if (ev.getKeyCode() == KeyEvent.VK_RIGHT) {
                    tangl = (float) Math.PI;
                    return (true);
                } else if (ev.getKeyCode() == KeyEvent.VK_UP) {
                    tangl = (float) (3 * Math.PI / 2);
                    return (true);
                } else if (ev.getKeyCode() == KeyEvent.VK_DOWN) {
                    tangl = (float) (Math.PI / 2);
                    return (true);
                }
            } else {
                if (ev.getKeyCode() == KeyEvent.VK_LEFT) {
                    tangl = (float) (Math.PI * 0.5 * (Math.floor((tangl / (Math.PI * 0.5)) - 0.51) + 0.5));
                    return (true);
                } else if (ev.getKeyCode() == KeyEvent.VK_RIGHT) {
                    tangl = (float) (Math.PI * 0.5 * (Math.floor((tangl / (Math.PI * 0.5)) + 0.51) + 0.5));
                    return (true);
                } else if (ev.getKeyCode() == KeyEvent.VK_UP) {
                    chfield(tfield - 50);
                    return (true);
                } else if (ev.getKeyCode() == KeyEvent.VK_DOWN) {
                    chfield(tfield + 50);
                    return (true);
                } else if (ev.getKeyCode() == KeyEvent.VK_HOME) {
                    tangl = angl + (float) Utils.cangle(-(float) Math.PI * 0.25f - angl);
                    chfield((float) (100 * Math.sqrt(2)));
                }
            }

            return (false);
        }
    }

    static {
        camtypes.put("ortho", SOrthoCam.class);
    }

    @RName("mapview")
    public static class $_ implements Factory {
        public Widget create(UI ui, Object[] args) {
            Coord sz = (Coord) args[0];
            Coord2d mc = ((Coord)args[1]).mul(posres);
            int pgob = -1;
            if (args.length > 2)
                pgob = (Integer) args[2];
            return (new MapView(sz, ui.sess.glob, mc, pgob));
        }
    }

    public MapView(Coord sz, Glob glob, Coord2d cc, long plgob) {
        super(sz);
        this.glob = glob;
        this.cc = cc;
        this.plgob = plgob;
        this.gobs = new Gobs();
        this.gridol = new TileOutline(glob.map);
        this.partyHighlight = new PartyHighlight(glob.party, plgob);
        setcanfocus(true);
        markedGobs.clear();
    }

    public boolean visol(int ol) {
        return (visol[ol] > 0);
    }

    public void enol(int... overlays) {
        for (int ol : overlays)
            visol[ol]++;
    }

    public void disol(int... overlays) {
        for (int ol : overlays)
            visol[ol]--;
    }

    private final Rendered flavobjs = new Rendered() {
        private Collection<Gob> fol;
        private Coord cc = null;
        private int mseq = 0;
        private boolean loading = false;

        public void draw(GOut g) {}

        public Object staticp() {
            Coord cc = MapView.this.cc.floor(tilesz).div(MCache.cutsz);
            int mseq = glob.map.olseq;
            if(loading || !Utils.eq(cc, this.cc) || (mseq != this.mseq)) {
                loading = false;
                Collection<Gob> fol = new ArrayList<Gob>();
                Coord o = new Coord();
                for(o.y = -view; o.y <= view; o.y++) {
                    for(o.x = -view; o.x <= view; o.x++) {
                        try {
                            fol.addAll(glob.map.getfo(cc.add(o)));
                        } catch(Loading e) {
                            loading = true;
                        }
                    }
                }
                this.cc = cc;
                this.mseq = mseq;
                this.fol = fol;
            }
            return(fol);
        }

        public boolean setup(RenderList rl) {
            for(Gob fo : fol)
                addgob(rl, fo);
            return(false);
        }
    };

    private final Rendered map = new Rendered() {
        public void draw(GOut g) {
        }

        public boolean setup(RenderList rl) {
            Coord cc = MapView.this.cc.floor(tilesz).div(MCache.cutsz);
            Coord o = new Coord();
            for (o.y = -view; o.y <= view; o.y++) {
                for (o.x = -view; o.x <= view; o.x++) {
                    Coord2d pc = cc.add(o).mul(MCache.cutsz).mul(tilesz);
                    try {
                        MapMesh cut = glob.map.getcut(cc.add(o));
                        rl.add(cut, Location.xlate(new Coord3f((float)pc.x, -(float)pc.y, 0)));
                    } catch (Defer.DeferredException e) {
                        // there seems to be a rare problem with fetching gridcuts when teleporting, not sure why...
                        // we ignore Defer.DeferredException to prevent the client for crashing
                    }
                }
            }
            if (!Config.hideflocomplete) {
                if(!(rl.state().get(PView.ctx) instanceof ClickContext))
                    rl.add(flavobjs, null);
            }
            return (false);
        }
    };

    private final Rendered mapol = new Rendered() {
        private final GLState[] mats;

        {
            mats = new GLState[32];
            mats[0] = olmat(255, 0, 128, 32);
            mats[1] = olmat(0, 0, 255, 32);
            mats[2] = olmat(255, 0, 0, 32);
            mats[3] = olmat(128, 0, 255, 32);
            mats[4] = olmat(255, 255, 255, 32);
            mats[5] = olmat(0, 255, 128, 32);
            mats[16] = olmat(0, 255, 0, 32);
            mats[17] = olmat(255, 255, 0, 32);
            mats[18] = olmat(29, 196, 51, 60);
        }

        private GLState olmat(int r, int g, int b, int a) {
            return (new Material(Light.deflight,
                    new Material.Colors(Color.BLACK, new Color(0, 0, 0, a), Color.BLACK, new Color(r, g, b, 255), 0),
                    States.presdepth));
        }

        public void draw(GOut g) {
        }

        public boolean setup(RenderList rl) {
            Coord cc = MapView.this.cc.floor(tilesz).div(MCache.cutsz);
            Coord o = new Coord();
            for (o.y = -view; o.y <= view; o.y++) {
                for (o.x = -view; o.x <= view; o.x++) {
                    Coord2d pc = cc.add(o).mul(MCache.cutsz).mul(tilesz);
                    for (int i = 0; i < visol.length; i++) {
                        if (mats[i] == null)
                            continue;
                        if (visol[i] > 0) {
                            Rendered olcut;
                            olcut = glob.map.getolcut(i, cc.add(o));
                            if (olcut != null)
                                rl.add(olcut, GLState.compose(Location.xlate(new Coord3f((float)pc.x, -(float)pc.y, 0)), mats[i]));
                        }
                    }
                }
            }
            return (false);
        }
    };

    void addgob(RenderList rl, final Gob gob) {
        if (Config.hidecrops && gob.type != null && gob.type.has(Gob.Type.PLANT))
            return;

        GLState xf;
        try {
            xf = Following.xf(gob);
        } catch (Loading e) {
            xf = null;
        }
        GLState extra = null;
        if (xf == null) {
            xf = gob.loc;
            try {
                Coord3f c = gob.getc();
                Tiler tile = glob.map.tiler(glob.map.gettile(new Coord2d(c).floor(tilesz)));
                extra = tile.drawstate(glob, rl.cfg, c);
            } catch (Loading e) {
                extra = null;
            }
        }
        rl.add(gob, GLState.compose(extra, xf, gob.olmod, gob.save));

        Gob.Overlay rovl = null;
        boolean show = false;

        if (gob.type == Gob.Type.WOODEN_SUPPORT) {
            rovl = rovlsupport;
            show = Config.showminerad;
        } else if (gob.type == Gob.Type.STONE_SUPPORT) {
            rovl = rovlcolumn;
            show = Config.showminerad;
        } else if (gob.type == Gob.Type.METAL_SUPPORT) {
            rovl = rovlbeam;
            show = Config.showminerad;
        }

        if (gob.type == Gob.Type.TROUGH) {
            rovl = rovltrough;
            show = Config.showfarmrad;
        } else if (gob.type == Gob.Type.BEEHIVE) {
            rovl = rovlbeehive;
            show = Config.showfarmrad;
        }

        if (show && !gob.ols.contains(rovl))
            gob.ols.add(rovl);
        else if (!show && rovl != null)
            gob.ols.remove(rovl);
    }

    public static class ChangeSet implements OCache.ChangeCallback {
        public final Set<Gob> changed = new HashSet<Gob>();
        public final Set<Gob> removed = new HashSet<Gob>();

        public void changed(Gob ob) {
            changed.add(ob);
        }

        public void removed(Gob ob) {
            changed.remove(ob);
            removed.add(ob);
        }
    }

    private class Gobs implements Rendered {
        final OCache oc = glob.oc;
        final ChangeSet changed = new ChangeSet();
        final Map<Gob, GobSet> parts = new HashMap<Gob, GobSet>();
        Integer ticks = 0;
        {oc.callback(changed);}

        class GobSet implements Rendered {
            private final String nm;
            final Collection<Gob> obs = new HashSet<Gob>();
            Object seq = this;

            GobSet(String nm) {
                this.nm = nm;
            }

            void take(Gob ob) {
                obs.add(ob);
                seq = ticks;
            }

            void remove(Gob ob) {
                if(obs.remove(ob))
                    seq = ticks;
            }

            void update() {
            }

            public void draw(GOut g) {}

            public boolean setup(RenderList rl) {
                for(Gob gob : obs)
                    addgob(rl, gob);
                return(false);
            }

            public Object staticp() {
                return(seq);
            }

            public int size() {
                return(obs.size());
            }

            public String toString() {
                return("GobSet(" + nm + ")");
            }
        }

        class Transitory extends GobSet {
            final Map<Gob, Integer> age = new HashMap<Gob, Integer>();

            Transitory(String nm) {super(nm);}

            void take(Gob ob) {
                super.take(ob);
                age.put(ob, ticks);
            }

            void remove(Gob ob) {
                super.remove(ob);
                age.remove(ob);
            }
        }

        final GobSet oldfags = new GobSet("old");
        final GobSet semistat = new GobSet("semistat");
        final GobSet semifags = new Transitory("semi") {
            int cycle = 0;

            void update() {
                if (++cycle >= 300) {
                    Collection<Gob> cache = new ArrayList<Gob>();
                    for (Map.Entry<Gob, Integer> ob : age.entrySet()) {
                        if (ticks - ob.getValue() > 450)
                            cache.add(ob.getKey());
                    }
                    for (Gob ob : cache)
                        put(oldfags, ob);
                    cycle = 0;
                }
            }
        };
        final GobSet newfags = new Transitory("new") {
            int cycle = 0;

            void update() {
                if(++cycle >= 20) {
                    Collection<Gob> cache = new ArrayList<Gob>();
                    Collection<Gob> scache = new ArrayList<Gob>();
                    for(Map.Entry<Gob, Integer> ob : age.entrySet()) {
                        if(ticks - ob.getValue() > 30) {
                            Gob gob = ob.getKey();
                            if(gob.staticp() instanceof Gob.SemiStatic)
                                scache.add(gob);
                            else
                                cache.add(gob);
                        }
                    }
                    for(Gob ob : cache)
                        put(semifags, ob);
                    for(Gob ob : scache)
                        put(semistat, ob);
                    cycle = 0;
                }
            }
        };
        final GobSet dynamic = new GobSet("dyn") {
            int cycle = 0;

            void update() {
                if(++cycle >= 5) {
                    Collection<Gob> cache = new ArrayList<Gob>();
                    for(Gob ob : obs) {
                        Object seq = ob.staticp();
                        if((seq instanceof Gob.Static) || (seq instanceof Gob.SemiStatic))
                            cache.add(ob);
                    }
                    for(Gob ob : cache)
                        put(newfags, ob);
                    cycle = 0;
                }
            }

            public Object staticp() {return(null);}
        };
        final GobSet[] all = {oldfags, semifags, semistat, newfags, dynamic};

        void put(GobSet set, Gob ob) {
            GobSet p = parts.get(ob);
            if(p != set) {
                if(p != null)
                    p.remove(ob);
                parts.put(ob, set);
                set.take(ob);
            }
        }

        void remove(Gob ob) {
            GobSet p = parts.get(ob);
            if(p != null) {
                parts.remove(ob);
                p.remove(ob);
            }
        }

        Gobs() {
            synchronized(oc) {
                for(Gob ob : oc)
                    changed.changed(ob);
            }
        }

        void update() {
            for(Gob ob : changed.removed)
                remove(ob);
            changed.removed.clear();

            for(Gob ob : changed.changed) {
                if(ob.staticp() instanceof Gob.Static)
                    put(newfags, ob);
                else
                    put(dynamic, ob);
            }
            changed.changed.clear();

            for(GobSet set : all)
                set.update();
        }

        public void draw(GOut g) {}

        public boolean setup(RenderList rl) {
            synchronized(oc) {
                update();
                for(GobSet set : all)
                    rl.add(set, null);
                ticks++;
            }
            return(false);
        }

        public String toString() {
            return(String.format("%,dd %,dn %,dS %,ds %,do", dynamic.size(), newfags.size(), semistat.size(), semifags.size(), oldfags.size()));
        }
    }
    private final Rendered gobs;

    public String toString() {
        String cc;
        try {
            cc = getcc().toString();
        } catch(Loading l) {
            cc = "<nil>";
        }
        return(String.format("Camera[%s (%s)], Caches[%s]", cc, camera, gobs));
    }

    public GLState camera() {
        return (camera);
    }

    protected Projection makeproj() {
        return (null);
    }

    private Coord3f smapcc = null;
    private ShadowMap smap = null;
    private double lsmch = 0;

    private void updsmap(RenderList rl, DirLight light) {
        if (rl.cfg.pref.lshadow.val) {
            if (smap == null)
                smap = new ShadowMap(new Coord(2048, 2048), 750, 5000, 1);
            smap.light = light;
            Coord3f dir = new Coord3f(-light.dir[0], -light.dir[1], -light.dir[2]);
            Coord3f cc = getcc();
            cc.y = -cc.y;
            boolean ch = false;
            double now = Utils.rtime();
            if ((smapcc == null) || (smapcc.dist(cc) > 50)) {
                smapcc = cc;
                ch = true;
            } else {
                if (now - lsmch > 0.1)
                    ch = true;
            }
            if (ch) {
                smap.setpos(smapcc.add(dir.neg().mul(1000f)), dir);
                lsmch = now;
            }
            rl.prepc(smap);
        } else {
            if (smap != null)
                smap.dispose();
            smap = null;
            smapcc = null;
        }
    }

    public DirLight amb = null;
    private Outlines outlines = new Outlines(false);

    public void setup(RenderList rl) {
        synchronized (glob) {
            if (glob.lightamb != null) {
                Color lightamb, lightdif, lightspc;
                if (Config.daylight) {
                    lightamb = glob.dlightamb;
                    lightdif = glob.dlightamb;
                    lightspc = glob.dlightspc;
                } else {
                    lightamb = glob.lightamb;
                    lightdif = glob.lightdif;
                    lightspc = glob.lightspc;
                }
                DirLight light = new DirLight(lightamb, lightdif, lightspc, Coord3f.o.sadd((float) glob.lightelev, (float) glob.lightang, 1f));
                rl.add(light, null);
                updsmap(rl, light);
                amb = light;
            } else {
                amb = null;
            }
            if (Config.showweather) {
                for (Glob.Weather w : glob.weather)
                    w.gsetup(rl);
                for (Glob.Weather w : glob.weather) {
                    if (w instanceof Rendered)
                        rl.add((Rendered) w, null);
                }
            }
        }
    /* XXX: MSAA level should be configurable. */
        if (rl.cfg.pref.fsaa.val) {
            FBConfig cfg = ((PView.ConfContext) rl.state().get(PView.ctx)).cfg;
            cfg.ms = 4;
        }
        if (rl.cfg.pref.outline.val)
            rl.add(outlines, null);
        rl.add(map, null);
        if (showgrid)
            rl.add(gridol, null);
        rl.add(mapol, null);
        rl.add(gobs, null);
        if (placing != null)
            addgob(rl, placing);
        synchronized (extradraw) {
            for (Rendered extra : extradraw)
                rl.add(extra, null);
            extradraw.clear();
        }
    }

    public static final haven.glsl.Uniform amblight = new haven.glsl.Uniform.AutoApply(haven.glsl.Type.INT) {
        public void apply(GOut g, VarID loc) {
            int idx = -1;
            RenderContext ctx = g.st.get(PView.ctx);
            if (ctx instanceof WidgetContext) {
                Widget wdg = ((WidgetContext) ctx).widget();
                if (wdg instanceof MapView)
                    idx = g.st.get(Light.lights).index(((MapView) wdg).amb);
            }
            g.gl.glUniform1i(loc, idx);
        }
    };

    public void drawadd(Rendered extra) {
        synchronized (extradraw) {
            extradraw.add(extra);
        }
    }

    public Gob player() {
        return ((plgob < 0) ? null : glob.oc.getgob(plgob));
    }

    public Coord3f getcc() {
        Gob pl = player();
        if (pl != null)
            return (pl.getc());
        else
            return(glob.map.getzp(cc));
    }

    public static class ClickContext extends RenderContext {
    }

    private TexGL clickbuf = null;
    private GLFrameBuffer clickfb = null;
    private final RenderContext clickctx = new ClickContext();

    private GLState.Buffer clickbasic(GOut g) {
        GLState.Buffer ret = basic(g);
        clickctx.prep(ret);
        if ((clickbuf == null) || !clickbuf.sz().equals(sz)) {
            if (clickbuf != null) {
                clickfb.dispose();
                clickfb = null;
                clickbuf.dispose();
                clickbuf = null;
            }
            clickbuf = new TexE(sz, GL.GL_RGBA, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE);
            clickfb = new GLFrameBuffer(clickbuf, null);
        }
        clickfb.prep(ret);
        new States.Blending(GL.GL_ONE, GL.GL_ZERO).prep(ret);
        return (ret);
    }

    private abstract static class Clicklist<T> extends RenderList {
        private Map<States.ColState, T> rmap = new WeakHashMap<States.ColState, T>();
        private Map<T, Reference<States.ColState>> idmap = new WeakHashMap<T, Reference<States.ColState>>();
        private int i = 1;
        private GLState.Buffer plain, bk;

        abstract protected T map(Rendered r);

        private Clicklist(GLConfig cfg) {
            super(cfg);
            this.bk = new GLState.Buffer(cfg);
        }

        protected States.ColState getcol(T t) {
            Reference<States.ColState> prevr = idmap.get(t);
            States.ColState prev = (prevr == null) ? null : prevr.get();
            if (prev != null)
                return (prev);
            int cr = ((i & 0x00000f) << 4) | ((i & 0x00f000) >> 12),
                    cg = ((i & 0x0000f0) << 0) | ((i & 0x0f0000) >> 16),
                    cb = ((i & 0x000f00) >> 4) | ((i & 0xf00000) >> 20);
            Color col = new Color(cr, cg, cb);
            States.ColState cst = new States.ColState(col);
            i++;
            rmap.put(cst, t);
            idmap.put(t, new WeakReference<States.ColState>(cst));
            return (cst);
        }

        protected void render(GOut g, Rendered r) {
            try {
                if (r instanceof FRendered)
                    ((FRendered) r).drawflat(g);
            } catch (RenderList.RLoad l) {
                if (ignload) return;
                else throw (l);
            }
        }

        public void get(GOut g, Coord c, final Callback<T> cb) {
            g.getpixel(c, col -> cb.done(rmap.get(new States.ColState(col))));
        }

        public void setup(Rendered r, GLState.Buffer t) {
            this.plain = t;
            super.setup(r, t);
        }

        protected void setup(Slot s, Rendered r) {
            T t = map(r);
            super.setup(s, r);
            s.os.copy(bk);
            plain.copy(s.os);
            bk.copy(s.os, GLState.Slot.Type.GEOM);
            if (t != null)
                getcol(t).prep(s.os);
        }

        public boolean aging() {
            return (i > (1 << 20));
        }
    }

    private static class Maplist extends Clicklist<MapMesh> {
        private int mode = 0;
        private MapMesh limit = null;

        private Maplist(GLConfig cfg) {
            super(cfg);
        }

        protected MapMesh map(Rendered r) {
            if (r instanceof MapMesh)
                return ((MapMesh) r);
            return (null);
        }

        protected void render(GOut g, Rendered r) {
            if (r instanceof MapMesh) {
                MapMesh m = (MapMesh) r;
                if (mode != 0)
                    g.state(States.vertexcolor);
                if ((limit == null) || (limit == m))
                    m.drawflat(g, mode);
            }
        }
    }

    private void checkmapclick(final GOut g, final Coord c, final Callback<Coord2d> cb) {
        new Object() {
            MapMesh cut;
            Coord tile;
            Coord2d pixel;
            int dfl = 0;

            {
                Maplist rl = new Maplist(g.gc);
                rl.setup(map, clickbasic(g));
                rl.fin();

                rl.render(g);
                if(clickdb)
                    g.getimage(img -> Debug.dumpimage(img, Debug.somedir("click1.png")));
                rl.get(g, c, hit -> {cut = hit; ckdone(1);});
                // rl.limit = hit;

                rl.mode = 1;
                rl.render(g);
                if(clickdb)
                    g.getimage(img -> Debug.dumpimage(img, Debug.somedir("click2.png")));
                g.getpixel(c, col -> {
                    tile = new Coord(col.getRed() - 1, col.getGreen() - 1);
                    pixel = new Coord2d((col.getBlue() * tilesz.x) / 255, (col.getAlpha() * tilesz.y) / 255);
                    ckdone(2);
                });
            }

            void ckdone(int fl) {
                synchronized (this) {
                    if ((dfl |= fl) == 3) {
                        if ((cut == null) || !tile.isect(Coord.z, cut.sz))
                            cb.done(null);
                        else
                            cb.done(cut.ul.add(tile).mul(tilesz).add(pixel));
                    }
                }
            }
        };
    }

    public static class ClickInfo {
        public final ClickInfo from;
        public final Rendered r;
        public Gob gob;

        public ClickInfo(ClickInfo from, Rendered r) {
            this.from = from;
            this.r = r;
        }

        public ClickInfo() {
            this(null, null);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof ClickInfo))
                return (false);
            ClickInfo o = (ClickInfo) obj;
            return (Utils.eq(from, o.from) && (r == o.r));
        }

        public int hashCode() {
            return (((from != null) ? (from.hashCode() * 31) : 0) + System.identityHashCode(r));
        }

        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append("#<clickinfo");
            for (ClickInfo c = this; c != null; c = c.from) {
                buf.append(' ');
                buf.append(c.r);
            }
            buf.append(">");
            return (buf.toString());
        }

        public Rendered[] array() {
            int n = 0;
            for (ClickInfo c = this; c != null; c = c.from)
                n++;
            Rendered[] buf = new Rendered[n];
            int i = 0;
            for (ClickInfo c = this; c != null; c = c.from)
                buf[i++] = c.r;
            return (buf);
        }
    }

    private static class Goblist extends Clicklist<ClickInfo> {
        private ClickInfo curinfo;

        public Goblist(GLConfig cfg) {
            super(cfg);
            curinfo = null;
        }

        public ClickInfo map(Rendered r) {
            if (r instanceof FRendered)
                return (curinfo);
            else
                return (null);
        }

        public void add(Rendered r, GLState t) {
            ClickInfo previnfo = curinfo;
            curinfo = new ClickInfo(previnfo, r);
            super.add(r, t);
            curinfo = previnfo;
        }
    }

    private Clicklist<ClickInfo> curgoblist = null;

    private void checkgobclick(GOut g, Coord c, Callback<ClickInfo> cb) {
        if ((curgoblist == null) || (curgoblist.cfg != g.gc) || curgoblist.aging())
            curgoblist = new Goblist(g.gc);
        Clicklist<ClickInfo> rl = curgoblist;
        rl.setup(gobs, clickbasic(g));
        rl.fin();
        rl.render(g);
        rl.get(g, c, cb);
    }

    public void delay(Delayed d) {
        synchronized (delayed) {
            delayed.add(d);
        }
    }

    public void delay2(Delayed d) {
        synchronized (delayed2) {
            delayed2.add(d);
        }
    }

    protected void undelay(Collection<Delayed> list, GOut g) {
        synchronized (list) {
            for (Delayed d : list)
                d.run(g);
            list.clear();
        }
    }

    static class PolText {
        Text text;
        double tm;
        PolText(Text text, double tm) {this.text = text; this.tm = tm;}
    }

    private static final Text.Furnace polownertf = new PUtils.BlurFurn(new Text.Foundry(Text.serif, 30).aa(true), 3, 1, Color.BLACK);
    private final Map<Integer, PolText> polowners = new HashMap<Integer, PolText>();


    public void setpoltext(int id, String text) {
        synchronized(polowners) {
            polowners.put(id, new PolText(polownertf.render(text), Utils.rtime()));
        }
    }

    private void poldraw(GOut g) {
        if (polowners.isEmpty())
            return;
        double now = Utils.rtime();
        synchronized (polowners) {
            int y = (sz.y - polowners.values().stream().map(t -> t.text.sz().y).reduce(0, (a, b) -> a + b + 10)) / 2;
            for (Iterator<PolText> i = polowners.values().iterator(); i.hasNext(); ) {
                PolText t = i.next();
                double poldt = now - t.tm;
                if (poldt < 6.0) {
                    int a;
                    if (poldt < 1.0)
                        a = (int) (255 * poldt);
                    else if (poldt < 4.0)
                        a = 255;
                    else
                        a = (int) ((255 * (2.0 - (poldt - 4.0))) / 2.0);
                    g.chcolor(255, 255, 255, a);
                    g.aimage(t.text.tex(), new Coord((sz.x - t.text.sz().x) / 2, y), 0.0, 0.0);
                    y += t.text.sz().y + 10;
                    g.chcolor();
                } else {
                    i.remove();
                }
            }
        }
    }

    private void drawarrow(GOut g, double a) {
        Coord hsz = sz.div(2);
        double ca = -Coord.z.angle(hsz);
        Coord ac;
        if ((a > ca) && (a < -ca)) {
            ac = new Coord(sz.x, hsz.y - (int) (Math.tan(a) * hsz.x));
        } else if ((a > -ca) && (a < Math.PI + ca)) {
            ac = new Coord(hsz.x - (int) (Math.tan(a - Math.PI / 2) * hsz.y), 0);
        } else if ((a > -Math.PI - ca) && (a < ca)) {
            ac = new Coord(hsz.x + (int) (Math.tan(a + Math.PI / 2) * hsz.y), sz.y);
        } else {
            ac = new Coord(0, hsz.y + (int) (Math.tan(a) * hsz.x));
        }
        Coord bc = ac.add(Coord.sc(a, -10));
        g.line(bc, bc.add(Coord.sc(a, -40)), 2);
        g.line(bc, bc.add(Coord.sc(a + Math.PI / 4, -10)), 2);
        g.line(bc, bc.add(Coord.sc(a - Math.PI / 4, -10)), 2);
    }

    public Coord3f screenxf(Coord3f mc) {
        if (Config.disableelev)
            mc.z = 0;
        Coord3f mloc = new Coord3f(mc.x, -mc.y, mc.z);
	    /* XXX: Peeking into the camera really is doubtfully nice. */
        return(camera.proj.toscreen(camera.view.fin(Matrix4f.id).mul4(mloc), sz));
    }

    public Coord3f screenxf(Coord2d mc) {
        Coord3f cc;
        try {
            cc = getcc();
        } catch(Loading e) {
            return(null);
        }
        return(screenxf(new Coord3f((float)mc.x, (float)mc.y, cc.z)));
    }

    public double screenangle(Coord2d mc, boolean clip) {
        Coord3f cc;
        try {
            cc = getcc();
        } catch(Loading e) {
            return(Double.NaN);
        }
        Coord3f mloc = new Coord3f((float)mc.x, -(float)mc.y, cc.z);
        float[] sloc = camera.proj.toclip(camera.view.fin(Matrix4f.id).mul4(mloc));
        if(clip) {
            float w = sloc[3];
            if((sloc[0] > -w) && (sloc[0] < w) && (sloc[1] > -w) && (sloc[1] < w))
                return(Double.NaN);
        }
        float a = ((float)sz.y) / ((float)sz.x);
        return(Math.atan2(sloc[1] * a, sloc[0]));
    }

    private void partydraw(GOut g) {
        for (Party.Member m : ui.sess.glob.party.memb.values()) {
            if (m.gobid == this.plgob)
                continue;
            Coord2d mc = m.getc();
            if (mc == null)
                continue;
            double a = screenangle(mc, true);
            if (Double.isNaN(a))
                continue;
            g.chcolor(m.col);
            drawarrow(g, a);
        }
        g.chcolor();
    }

    private Loading camload = null, lastload = null;

    public void draw(GOut g) {
        glob.map.sendreqs();
        if ((olftimer != 0) && (olftimer < Utils.rtime()))
            unflashol();
        try {
            if (camload != null)
                throw (new Loading(camload));
            Gob pl = player();
            if (pl != null)
                this.cc = new Coord2d(pl.getc());
            undelay(delayed, g);
            super.draw(g);
            undelay(delayed2, g);
            poldraw(g);
            partydraw(g);
            try {
                glob.map.reqarea(cc.floor(tilesz).sub(MCache.cutsz.mul(view + 1)),
                        cc.floor(tilesz).add(MCache.cutsz.mul(view + 1)));
            } catch (Defer.DeferredException e) {
                // there seems to be a rare problem with fetching gridcuts when teleporting, not sure why...
                // we ignore Defer.DeferredException to prevent the client for crashing
            }

            if (showgrid) {
                double tx = Math.ceil(cc.x / tilesz.x / MCache.cutsz.x);
                double ty = Math.ceil(cc.y / tilesz.y / MCache.cutsz.y);
                Coord tc = new Coord((int)(tx - view - 1) * MCache.cutsz.x, (int)(ty - view - 1) * MCache.cutsz.y);
                if (!tc.equals(lasttc)) {
                    lasttc = tc;
                    gridol.update(tc);
                }
            }
        } catch (Loading e) {
            lastload = e;
            String text = e.getMessage();
            if (text == null)
                text = "Loading...";
            g.chcolor(Color.BLACK);
            g.frect(Coord.z, sz);
            g.chcolor(Color.WHITE);
            g.atext(text, sz.div(2), 0.5, 0.5);
            if (e instanceof Resource.Loading) {
                ((Resource.Loading) e).boostprio(5);
            }
        }
    }

    public void tick(double dt) {
        camload = null;
        try {
            if ((shake = shake * Math.pow(100, -dt)) < 0.01)
                shake = 0;
            camoff.x = (float) ((Math.random() - 0.5) * shake);
            camoff.y = (float) ((Math.random() - 0.5) * shake);
            camoff.z = (float) ((Math.random() - 0.5) * shake);
            camera.tick(dt);
        } catch (Loading e) {
            camload = e;
        }
        if (placing != null)
            placing.ctick((int) (dt * 1000));
        partyHighlight.update();
    }

    public void resize(Coord sz) {
        super.resize(sz);
        camera.resized();
    }

    public static interface PlobAdjust {
        public void adjust(Plob plob, Coord pc, Coord2d mc, int modflags);

        public boolean rotate(Plob plob, int amount, int modflags);
    }

    public static class StdPlace implements PlobAdjust {
        boolean freerot = false;
        Coord2d gran = (plobgran == 0)?null:new Coord2d(1.0 / plobgran, 1.0 / plobgran).mul(tilesz);

        public void adjust(Plob plob, Coord pc, Coord2d mc, int modflags) {
            if ((modflags & 2) == 0)
                plob.rc = mc.floor(tilesz).mul(tilesz).add(tilesz.div(2));
            else if(gran != null)
                plob.rc = mc.add(gran.div(2)).floor(gran).mul(gran);
            else
                plob.rc = mc;
            Gob pl = plob.mv().player();
            if ((pl != null) && !freerot)
                plob.a = Math.round(plob.rc.angle(pl.rc) / (Math.PI / 2)) * (Math.PI / 2);
        }

        public boolean rotate(Plob plob, int amount, int modflags) {
            if ((modflags & 1) == 0)
                return (false);
            freerot = true;
            if ((modflags & 2) == 0)
                plob.a = (Math.PI / 4) * Math.round((plob.a + (amount * Math.PI / 4)) / (Math.PI / 4));
            else
                plob.a += amount * Math.PI / 16;
            plob.a = Utils.cangle(plob.a);
            return (true);
        }
    }

    public class Plob extends Gob {
        public PlobAdjust adjust = new StdPlace();
        Coord lastmc = null;

        private Plob(Indir<Resource> res, Message sdt) {
            super(MapView.this.glob, Coord2d.z);
            setattr(new ResDrawable(this, res, sdt));
            if (ui.mc.isect(rootpos(), sz)) {
                delay(new Adjust(ui.mc.sub(rootpos()), 0));
            }
        }

        public MapView mv() {
            return (MapView.this);
        }

        private class Adjust extends Maptest {
            int modflags;

            Adjust(Coord c, int modflags) {
                super(c);
                this.modflags = modflags;
            }

            public void hit(Coord pc, Coord2d mc) {
                adjust.adjust(Plob.this, pc, mc, modflags);
                lastmc = pc;
            }
        }
    }

    private int olflash;
    private double olftimer;

    private void unflashol() {
        for (int i = 0; i < visol.length; i++) {
            if ((olflash & (1 << i)) != 0)
                visol[i]--;
        }
        olflash = 0;
        olftimer = 0;
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "place") {
            int a = 0;
            Indir<Resource> res = ui.sess.getres((Integer) args[a++]);
            Message sdt;
            if ((args.length > a) && (args[a] instanceof byte[]))
                sdt = new MessageBuf((byte[]) args[a++]);
            else
                sdt = Message.nil;
            placing = new Plob(res, sdt);
            while (a < args.length) {
                Indir<Resource> ores = ui.sess.getres((Integer) args[a++]);
                Message odt;
                if ((args.length > a) && (args[a] instanceof byte[]))
                    odt = new MessageBuf((byte[]) args[a++]);
                else
                    odt = Message.nil;
                placing.ols.add(new Gob.Overlay(-1, ores, odt));
            }
        } else if (msg == "unplace") {
            placing = null;
        } else if (msg == "move") {
            cc = ((Coord)args[0]).mul(posres);
        } else if (msg == "plob") {
            if (args[0] == null)
                plgob = -1;
            else
                plgob = (Integer) args[0];
        } else if (msg == "flashol") {
            unflashol();
            olflash = (Integer) args[0];
            for (int i = 0; i < visol.length; i++) {
                if ((olflash & (1 << i)) != 0)
                    visol[i]++;
            }
            olftimer = Utils.rtime() + (((Number)args[1]).doubleValue() / 1000.0);
        } else if (msg == "sel") {
            boolean sel = ((Integer) args[0]) != 0;
            synchronized (this) {
                if (sel && (selection == null)) {
                    selection = new Selector();
                } else if (!sel && (selection != null)) {
                    selection.destroy();
                    selection = null;
                }
            }
        } else if (msg == "shake") {
            shake += ((Number) args[0]).doubleValue();
        } else {
            super.uimsg(msg, args);
        }
    }

    private UI.Grab camdrag = null;

    public abstract class Maptest implements Delayed {
        private final Coord pc;

        public Maptest(Coord c) {
            this.pc = c;
        }

        public void run(GOut g) {
            GLState.Buffer bk = g.st.copy();
            try {
                BGL gl = g.gl;
                g.st.set(clickbasic(g));
                g.apply();
                gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);
                checkmapclick(g, pc, mc -> {
                    synchronized(ui) {
                        if(mc != null)
                            hit(pc, mc);
                        else
                            nohit(pc);
                    }
                });
            } finally {
                g.st.set(bk);
            }
        }

        protected abstract void hit(Coord pc, Coord2d mc);

        protected void nohit(Coord pc) {
        }
    }

    public abstract class Hittest implements Delayed {
        private final Coord clickc;
        private Coord2d mapcl;
        private ClickInfo gobcl;
        private int dfl = 0;

        public Hittest(Coord c) {
            clickc = c;
        }

        public void run(GOut g) {
            GLState.Buffer bk = g.st.copy();
            try {
                BGL gl = g.gl;
                g.st.set(clickbasic(g));
                g.apply();
                gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);
                checkmapclick(g, clickc, mc -> {mapcl = mc; ckdone(1);});
                g.st.set(bk);
                g.st.set(clickbasic(g));
                g.apply();
                gl.glClear(GL.GL_COLOR_BUFFER_BIT);
                checkgobclick(g, clickc, cl -> {gobcl = cl; ckdone(2);});
            } finally {
                g.st.set(bk);
            }
        }

        private void ckdone(int fl) {
            synchronized(this) {
                synchronized(ui) {
                    if((dfl |= fl) == 3) {
                        if(mapcl != null) {
                            if(gobcl == null)
                                hit(clickc, mapcl, null);
                            else
                                hit(clickc, mapcl, gobcl);
                        } else {
                            nohit(clickc);
                        }
                    }
                }
            }
        }

        protected abstract void hit(Coord pc, Coord2d mc, ClickInfo inf);

        protected void nohit(Coord pc) {
        }
    }

    public static interface Clickable {
        public Object[] clickargs(ClickInfo inf);
    }

    public static Object[] gobclickargs(ClickInfo inf) {
        if (inf == null)
            return (new Object[0]);
        for (ClickInfo c = inf; c != null; c = c.from) {
            if (c.r instanceof Clickable)
                return (((Clickable) c.r).clickargs(inf));
        }
        Rendered[] st = inf.array();
        for (int g = 0; g < st.length; g++) {
            if (st[g] instanceof Gob) {
                Gob gob = (Gob) st[g];
                Object[] ret = {0, (int) gob.id, gob.rc.floor(posres), 0, -1};
                for (int i = g - 1; i >= 0; i--) {
                    if (st[i] instanceof Gob.Overlay) {
                        ret[0] = 1;
                        ret[3] = ((Gob.Overlay) st[i]).id;
                    }
                    if (st[i] instanceof FastMesh.ResourceMesh)
                        ret[4] = ((FastMesh.ResourceMesh) st[i]).id;
                }
                inf.gob = gob;
                return (ret);
            }
        }
        return (new Object[0]);
    }

    private interface GobCheck {
        boolean check(Gob gob);
    }
    
    private class GobCheckAtk implements GobCheck {
        public boolean check(Gob gob) {
            if (gob == null || gob.type == null) {
                return false;
            }
            Gob.Type type = gob.type;
            switch (type) {
            case PLAYER:
                return Config.proximityaggro && !gob.isFriend();
            case MAMMOTH:
            case BAT:
            case EAGLE:
            case MOB:
            case BEAR:
            case LYNX:
            case WILDGOAT:
            case TROLL:
            case WALRUS:
            case TROUGH:
                return Config.proximityaggroanimal;
            default:
                return false;
            }
        }
    }

    private class GobCheckLift implements GobCheck {
        public boolean check(Gob gob) {
            //TODO
            return true;
        }
    }

    private class GobCheckChase implements GobCheck {
        public boolean check(Gob gob) {
            //TODO
            return true;
        }
    }

    private class Click extends Hittest {
        int clickb;

        private Click(Coord c, int b) {
            super(c);
            clickb = b;

            synchronized (Pathfinder.class) {
                if (pf != null) {
                    pf.terminate = true;
                    pfthread.interrupt();
                }
            }
        }
        
        
        protected Gob proximity(GobCheck check, Coord2d mc) {
            Gob target = null;
            synchronized (glob.oc) {
                for (Gob gob : glob.oc) {
                    if (!gob.isplayer() && check.check(gob)) {
                        double dist = gob.rc.dist(mc);
                        if ((target == null || dist < target.rc.dist(mc)) && dist <= 5 * tilesz.x)
                            target = gob;
                    }
                }
            }
            return target;
        }

        protected void hit(Coord pc, Coord2d mc, ClickInfo inf) {
            lastItemactClickArgs = null;
            if (clickb == 1)
                pllastcc = mc;
            // reset alt so we could walk with alt+lmb while having item on the cursor
            int modflags = ui.modflags();
            if (gameui().vhand != null && clickb == 1)
                modflags = modflags & ~4;

            Resource curs = ui.root.getcurs(c);

            if (musselPicker != null) {
                try {
                    musselPicker.interrupt();
                } catch (NullPointerException e) {
                }
            }

            Gob target = null;
            if (clickb == 1 && curs != null && curs.name.equals("gfx/hud/curs/atk")
                    && (Config.proximityaggro || Config.proximityaggroanimal)) {
                target = proximity(new GobCheckAtk(), mc);
            }
            if (clickb == 1 && curs != null && curs.name.equals("gfx/hud/curs/hand")
                    && (Config.proximitylift)) {
                target = proximity(new GobCheckLift(), mc);
            }
            if (clickb == 3 && (modflags & 4) != 0 && curs != null && curs.name.equals("gfx/hud/curs/arw")
                    && (Config.proximitylift)) {
                target = proximity(new GobCheckChase(), mc);
                modflags = modflags & ~4;
            }
            if (target != null) {
                wdgmsg("click", target.sc, target.rc.floor(posres), clickb, modflags, 0, (int) target.id, target.rc.floor(posres), 0, -1);
                return;
            }

            Object[] args = {pc, mc.floor(posres), clickb, modflags};
            args = Utils.extend(args, gobclickargs(inf));

            if (inf == null) {
                if (Config.pf && clickb == 1 && curs != null && !curs.name.equals("gfx/hud/curs/study")) {
                    pfLeftClick(mc.floor(), null);
                } else {
                    wdgmsg("click", args);
                }
            } else {
                Gob gob = inf.gob;
                if (ui.modmeta && ui.modctrl && clickb == 1 && gob != null) {
                    if (markedGobs.contains(gob.id))
                        markedGobs.remove(gob.id);
                    else
                        markedGobs.add(gob.id);

                    glob.oc.changed(gob);
                } else if (ui.modmeta && clickb == 1) {
                    if (gobselcb != null)
                        gobselcb.gobselect(gob);

                    if (gameui().vhand == null) {   // do not highlight when walking with an item
                        for (Widget w = gameui().chat.lchild; w != null; w = w.prev) {
                            if (w instanceof ChatUI.MultiChat) {
                                ChatUI.MultiChat chat = (ChatUI.MultiChat) w;
                                if (chat.name().equals(Resource.getLocString(Resource.BUNDLE_LABEL, "Area Chat"))) {
                                    chat.send(ChatUI.CMD_PREFIX_HLIGHT + gob.id);
                                    break;
                                }
                            }
                        }
                    }
                }

                if (Config.pf && curs != null && !curs.name.equals("gfx/hud/curs/study") && gob != null) {
                    pfRightClick(gob, (int)args[8], clickb, 0, null);
                } else {
                    wdgmsg("click", args);
                    if (Config.autopickmussels && gob.type == Gob.Type.MUSSEL)
                        startMusselsPicker(gob);
                }
            }
        }
    }

    public void registerGobSelect(GobSelectCallback callback) {
        this.gobselcb = callback;
    }

    public void unregisterGobSelect() {
        this.gobselcb = null;
    }

    public void registerAreaSelect(AreaSelectCallback callback) {
        this.areaselcb = callback;
    }

    public void unregisterAreaSelect() {
        this.areaselcb = null;
    }

    public void pfLeftClick(Coord mc, String action) {
        Gob player = player();
        if (player == null)
            return;
        synchronized (Pathfinder.class) {
            if (pf != null) {
                pf.terminate = true;
                pfthread.interrupt();
                // cancel movement
                if (player.getattr(Moving.class) != null)
                    wdgmsg("gk", 27);
            }

            Coord src = player.rc.floor();
            int gcx = haven.pathfinder.Map.origin - (src.x - mc.x);
            int gcy = haven.pathfinder.Map.origin - (src.y - mc.y);
            if (gcx < 0 || gcx >= haven.pathfinder.Map.sz || gcy < 0 || gcy >= haven.pathfinder.Map.sz)
                return;

            pf = new Pathfinder(this, new Coord(gcx, gcy), action);
            pf.addListener(this);
            pfthread = new Thread(pf, "Pathfinder");
            pfthread.start();
        }
    }

    public void pfRightClick(Gob gob, int meshid, int clickb, int modflags, String action) {
        Gob player = player();
        if (player == null)
            return;
        synchronized (Pathfinder.class) {
            if (pf != null) {
                pf.terminate = true;
                pfthread.interrupt();
                // cancel movement
                if (player.getattr(Moving.class) != null)
                    wdgmsg("gk", 27);
            }

            Coord src = player.rc.floor();
            int gcx = haven.pathfinder.Map.origin - (src.x - gob.rc.floor().x);
            int gcy = haven.pathfinder.Map.origin - (src.y - gob.rc.floor().y);
            if (gcx < 0 || gcx >= haven.pathfinder.Map.sz || gcy < 0 || gcy >= haven.pathfinder.Map.sz)
                return;

            pf = new Pathfinder(this, new Coord(gcx, gcy), gob, meshid, clickb, modflags, action);
            pf.addListener(this);
            pfthread = new Thread(pf, "Pathfinder");
            pfthread.start();
        }
    }

    public void pfDone(final Pathfinder thread) {
        if (haven.pathfinder.Map.DEBUG_TIMINGS)
            System.out.println("-= PF DONE =-");
    }

    public void grab(Grabber grab) {
        this.grab = grab;
    }

    public void release(Grabber grab) {
        if (this.grab == grab)
            this.grab = null;
    }

    public boolean mousedown(Coord c, int button) {
        if (miningOverlay != null && button == 1) {
            miningOverlay.destroy();
            disol(18);
            miningOverlay = null;
        }
        parent.setfocus(this);
        if (button == 2) {
            if (camera.click(c))
                camdrag = ui.grabmouse(this);
        } else if (placing != null) {
            if (placing.lastmc != null)
                wdgmsg("place", placing.rc.floor(posres), (int)Math.round(placing.a * 32768 / Math.PI), button, ui.modflags());
        } else if ((grab != null) && grab.mmousedown(c, button)) {
        } else {
            delay(new Click(c, button));
        }
        return (true);
    }

    public void mousemove(Coord c) {
        if (grab != null)
            grab.mmousemove(c);
        if (camdrag != null) {
            ((Camera) camera).drag(c);
        } else if (placing != null) {
            if ((placing.lastmc == null) || !placing.lastmc.equals(c)) {
                delay(placing.new Adjust(c, ui.modflags()));
            }
        } else if (ui.modshift && !ui.modctrl && Config.resinfo) {
            long now = System.currentTimeMillis();
            if ((now - lastmmhittest > 500 || lasthittestc.dist(c) > tilesz.x) && gameui().hand.isEmpty()) {
                lastmmhittest = now;
                lasthittestc = c;

                delay(new Hittest(c) {
                    protected void hit(Coord pc, Coord2d mc, ClickInfo inf) {
                        if (inf != null) {
                            MapView.gobclickargs(inf);
                            if (inf.gob != null) {
                                Resource res = inf.gob.getres();
                                if (res != null) {
                                    tooltip = res.name;
                                    return;
                                }
                            }
                        }
                        tooltip = null;
                    }

                    protected void nohit(Coord pc) {
                        tooltip = null;
                    }
                });
            }
        } else if (ui.modshift && ui.modctrl && Config.resinfo) {
            long now = System.currentTimeMillis();
            if (now - lastmmhittest > 500 || lasthittestc.dist(c) > tilesz.x) {
                lastmmhittest = now;
                lasthittestc = c;
                delay(new Hittest(c) {
                    public void hit(Coord pc, Coord2d mc, ClickInfo inf) {
                        if (inf == null) {
                            MCache map = ui.sess.glob.map;
                            int t = map.gettile(mc.floor(tilesz));
                            Resource res = map.tilesetr(t);
                            if (res != null) {
                                tooltip = res.name;
                                return;
                            }
                        }
                        tooltip = null;
                    }

                    public void nohit(Coord pc) {
                        tooltip = null;
                    }
                });
            }
        }
    }

    public boolean mouseup(Coord c, int button) {
        if (button == 2) {
            if (camdrag != null) {
                camera.release();
                camdrag.remove();
                camdrag = null;
            }
        } else if (grab != null) {
            grab.mmouseup(c, button);
        }
        return (true);
    }

    public boolean mousewheel(Coord c, int amount) {
        if ((grab != null) && grab.mmousewheel(c, amount))
            return (true);
        if ((placing != null) && placing.adjust.rotate(placing, amount, ui.modflags()))
            return (true);
        return (((Camera) camera).wheel(c, amount));
    }

    public boolean drop(final Coord cc, final Coord ul) {
        delay(new Hittest(cc) {
            public void hit(Coord pc, Coord2d mc, ClickInfo inf) {
                if ((Config.nodropping || Config.nodropping_all) && !ui.modctrl) {
                    // no dropping at all or when we are on water
                    boolean nodropping = false;
                    if (Config.nodropping_all) {
                        // no dropping over anywhere
                        nodropping = true;
                    } else {
                        // we came here because Config.nodropping is set, check water tiles
                        int t = glob.map.gettile(player().rc.floor(tilesz));
                        Resource res = glob.map.tilesetr(t);
                        if (res != null &&
                                (res.name.equals("gfx/tiles/water") ||
                                    res.name.equals("gfx/tiles/deep") ||
                                    res.name.equals("gfx/tiles/odeeper") ||
                                    res.name.equals("gfx/tiles/odeep") ||
                                    res.name.equals("gfx/tiles/owater"))) {
                            nodropping = true;
                        }
                    }
                    if (nodropping) {
                        // we really don't want dropping, so click is moving
                        if (Config.pf) {
                            pfLeftClick(mc.floor(), null);
                        } else {
                            wdgmsg("click", pc, mc.floor(posres), 1, 0);
                        }
                        return;
                	}
                }
                wdgmsg("drop", pc, mc.floor(posres), ui.modflags());
            }
        });
        return (true);
    }

    public boolean iteminteract(Coord cc, Coord ul) {
        delay(new Hittest(cc) {
            public void hit(Coord pc, Coord2d mc, ClickInfo inf) {
                lastItemactClickArgs = null;
                if (inf == null && Config.tilecenter) {
                    mc.x = ((int)mc.x / 11) * 11 + Integer.signum((int)mc.x) * 5;
                    mc.y = ((int)mc.y / 11) * 11 + Integer.signum((int)mc.y) * 5;
                }

                Object[] args = {pc, mc.floor(posres), ui.modflags()};
                args = Utils.extend(args, gobclickargs(inf));

                if (ui.modshift && ui.modmeta)
                    lastItemactClickArgs = args;
                wdgmsg("itemact", args);
            }
        });
        return (true);
    }

    public void iteminteractreplay() {
        wdgmsg("itemact", lastItemactClickArgs);
    }

    public boolean keydown(KeyEvent ev) {
        if (placing != null) {
            if ((ev.getKeyCode() == KeyEvent.VK_LEFT) && placing.adjust.rotate(placing, -1, ui.modflags()))
                return (true);
            if ((ev.getKeyCode() == KeyEvent.VK_RIGHT) && placing.adjust.rotate(placing, 1, ui.modflags()))
                return (true);
        }
        if (camera.keydown(ev))
            return (true);
        return (super.keydown(ev));
    }

    public boolean globtype(char c, KeyEvent ev) {
        int code = ev.getKeyCode();
        if (code == KeyEvent.VK_ADD)
            camera.wheel(Coord.z, -1);
        else if (code == KeyEvent.VK_SUBTRACT)
            camera.wheel(Coord.z, 1);
        else if (ev.isShiftDown() && code == KeyEvent.VK_C) {
            if (camera != null) {
                String cam = camera instanceof OrthoCam ? "bad" : "ortho";
                String[] args = new String[0];
                camera = makecam(camtypes.get(cam), args);
                Utils.setpref("defcam", cam);
                Utils.setprefb("camargs", Utils.serialize(args));
                refreshGobsAll();
            }
            return true;
        }
        return (false);
    }

    public Object tooltip(Coord c, Widget prev) {
        if (selection != null) {
            if (selection.tt != null)
                return (selection.tt);
        } else if (tooltip != null && ui.modshift) {
            return Text.render(tooltip);
        }
        return (super.tooltip(c, prev));
    }

    public class GrabXL implements Grabber {
        private final Grabber bk;
        public boolean mv = false;

        public GrabXL(Grabber bk) {
            this.bk = bk;
        }

        public boolean mmousedown(Coord cc, final int button) {
            delay(new Maptest(cc) {
                public void hit(Coord pc, Coord2d mc) {
                    bk.mmousedown(mc.round(), button);
                }
            });
            return (true);
        }

        public boolean mmouseup(Coord cc, final int button) {
            delay(new Maptest(cc) {
                public void hit(Coord pc, Coord2d mc) {
                    bk.mmouseup(mc.round(), button);
                }
            });
            return (true);
        }

        public boolean mmousewheel(Coord cc, final int amount) {
            delay(new Maptest(cc) {
                public void hit(Coord pc, Coord2d mc) {
                    bk.mmousewheel(mc.round(), amount);
                }
            });
            return (true);
        }

        public void mmousemove(Coord cc) {
            if (mv) {
                delay(new Maptest(cc) {
                    public void hit(Coord pc, Coord2d mc) {
                        bk.mmousemove(mc.round());
                    }
                });
            }
        }
    }

    private class Selector implements Grabber {
        Coord sc;
        MCache.Overlay ol;
        UI.Grab mgrab;
        int modflags;
        Text tt;
        MapView mv;
        final GrabXL xl = new GrabXL(this) {
            public boolean mmousedown(Coord cc, int button) {
                if (button != 1)
                    return (false);
                return (super.mmousedown(cc, button));
            }

            public boolean mmousewheel(Coord cc, int amount) {
                return (false);
            }
        };

        public Selector() {
        }

        public Selector(MapView mv) {
            this.mv = mv;
        }

        {
            grab(xl);
            enol(17);
        }

        public boolean mmousedown(Coord mc, int button) {
            synchronized (MapView.this) {
                if (selection != this)
                    return (false);
                if (sc != null) {
                    ol.destroy();
                    mgrab.remove();
                }
                sc = mc.div(MCache.tilesz2);
                modflags = ui.modflags();
                xl.mv = true;
                mgrab = ui.grabmouse(MapView.this);
                synchronized (glob.map.grids) {
                    ol = glob.map.new Overlay(sc, sc, 1 << 17);
                }
                return (true);
            }
        }

        public boolean mmouseup(Coord mc, int button) {
            synchronized (MapView.this) {
                if (sc != null) {
                    Coord ec = mc.div(MCache.tilesz2);
                    xl.mv = false;
                    tt = null;
                    Resource curs = ui.root.getcurs(c);
                    if (button == 1 && curs != null && curs.name.equals("gfx/hud/curs/mine")) {
                        synchronized (glob.map.grids) {
                            miningOverlay = glob.map.new Overlay(ol.getc1(), ol.getc2(), 1 << 18);
                        }
                        enol(18);
                    }
                    ol.destroy();
                    mgrab.remove();
                    if (areaselcb != null)
                        areaselcb.areaselect(ol.getc1(), ol.getc2());
                    wdgmsg("sel", sc, ec, modflags);
                    sc = null;
                }
                return (true);
            }
        }

        public boolean mmousewheel(Coord mc, int amount) {
            return (false);
        }

        public void mmousemove(Coord mc) {
            synchronized (MapView.this) {
                if (sc != null) {
                    Coord tc = mc.div(MCache.tilesz2);
                    Coord c1 = new Coord(Math.min(tc.x, sc.x), Math.min(tc.y, sc.y));
                    Coord c2 = new Coord(Math.max(tc.x, sc.x), Math.max(tc.y, sc.y));
                    ol.update(c1, c2);
                    tt = Text.render(String.format("%d\u00d7%d", c2.x - c1.x + 1, c2.y - c1.y + 1));
                }
            }
        }

        public void destroy() {
            synchronized (MapView.this) {
                if (sc != null) {
                    ol.destroy();
                    mgrab.remove();
                }
                release(xl);
                disol(17);
            }
        }
    }

    private Camera makecam(Class<? extends Camera> ct, String... args) {
        try {
            try {
                Constructor<? extends Camera> cons = ct.getConstructor(MapView.class, String[].class);
                return (cons.newInstance(new Object[]{this, args}));
            } catch (IllegalAccessException e) {
            } catch (NoSuchMethodException e) {
            }
            try {
                Constructor<? extends Camera> cons = ct.getConstructor(MapView.class);
                return (cons.newInstance(new Object[]{this}));
            } catch (IllegalAccessException e) {
            } catch (NoSuchMethodException e) {
            }
        } catch (InstantiationException e) {
            throw (new Error(e));
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException)
                throw ((RuntimeException) e.getCause());
            throw (new RuntimeException(e));
        }
        throw (new RuntimeException("No valid constructor found for camera " + ct.getName()));
    }

    private Camera restorecam() {
        Class<? extends Camera> ct = camtypes.get(Utils.getpref("defcam", null));
        if (ct == null)
            return (new SOrthoCam(true));
        String[] args = (String[]) Utils.deserialize(Utils.getprefb("camargs", null));
        if (args == null) args = new String[0];
        try {
            return (makecam(ct, args));
        } catch (Exception e) {
            return (new SOrthoCam(true));
        }
    }

    private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();

    {
        cmdmap.put("cam", (cons, args) -> {
            if (args.length >= 2) {
                Class<? extends Camera> ct = camtypes.get(args[1]);
                String[] cargs = Utils.splice(args, 2);
                if (ct != null) {
                    camera = makecam(ct, cargs);
                    Utils.setpref("defcam", args[1]);
                    Utils.setprefb("camargs", Utils.serialize(cargs));
                } else {
                    throw (new Exception("no such camera: " + args[1]));
                }
            }
        });
        Console.setscmd("placegrid", new Console.Command() {
            public void run(Console cons, String[] args) {
                if ((plobgran = Integer.parseInt(args[1])) < 0)
                    plobgran = 0;
            }
        });
        cmdmap.put("whyload", (cons, args) -> {
            Loading l = lastload;
            if (l == null)
                throw (new Exception("Not loading"));
            l.printStackTrace(cons.out);
        });
        Console.setscmd("clickdb", (cons, args) -> clickdb = Utils.parsebool(args[1], false));
    }

    public Map<String, Console.Command> findcmds() {
        return (cmdmap);
    }

    public void togglegrid() {
        showgrid = !showgrid;
        if (showgrid) {
            Coord tc = new Coord((int) (cc.x / tilesz.x / MCache.cutsz.x - view - 1) * MCache.cutsz.x,
                    (int) (cc.y / tilesz.y / MCache.cutsz.y - view - 1) * MCache.cutsz.y);
            lasttc = tc;
            gridol.update(tc);
        }
    }

    public void aggroclosest() {
        OCache oc = ui.sess.glob.oc;
        synchronized (oc) {
            Gob gobcls = null;
            double gobclsdist = Double.MAX_VALUE;

            for (Gob gob : oc) {
                try {
                    Resource res = gob.getres();
                    if (res != null && "body".equals(res.basename()) && gob.id != player().id) {
                        if (!gob.isFriend()) {
                            double dist = player().rc.dist(gob.rc);
                            if (dist < gobclsdist) {
                                gobcls = gob;
                                gobclsdist = dist;
                            }
                        }
                    }
                } catch (Loading l) {
                }
            }

            if (gobcls != null) {
                gameui().act("aggro");
                wdgmsg("click", gobcls.sc, Coord.z, 1, ui.modflags(), 0, (int) gobcls.id, gobcls.rc.floor(posres), 0, 0);
                Gob pl = player();
                wdgmsg("click", pl.sc, pl.rc.floor(posres), 3, 0);
            }
        }
    }

    public void canceltasks() {
        if (pf != null)
            pf.terminate = true;
        if (steelrefueler != null)
            steelrefueler.terminate();
        if (musselPicker != null)
            musselPicker.interrupt();
    }

    public void removeCustomSprites(int id) {
        OCache oc = ui.sess.glob.oc;
        synchronized (oc) {
            for (Gob gob : oc) {
                Gob.Overlay ol = gob.findol(id);
                if (ol != null)
                    gob.ols.remove(ol);
            }
        }
    }

    public void addHealthSprites() {
        OCache oc = glob.oc;
        synchronized (oc) {
            for (Gob gob : oc) {
                final GobHealth hlt = gob.getattr(GobHealth.class);
                if (hlt != null && hlt.hp < 4) {
                    Gob.Overlay ol = gob.findol(Sprite.GOB_HEALTH_ID);
                    if (ol == null)
                        gob.addol(new Gob.Overlay(Sprite.GOB_HEALTH_ID, new GobHealthSprite(hlt.hp)));
                    else if (((GobHealthSprite)ol.spr).val != hlt.hp)
                        ((GobHealthSprite)ol.spr).update(hlt.hp);
                    oc.changed(gob);
                }
            }
        }
    }

    public void refreshGobsAll() {
        OCache oc = glob.oc;
        synchronized (oc) {
            for (Gob gob : oc)
                oc.changed(gob);
        }
    }

    public void refreshGobsHidable() {
        OCache oc = glob.oc;
        synchronized (oc) {
            for (Gob gob : oc) {
                if (gob.type == Gob.Type.TREE)
                    oc.changed(gob);
            }
        }
    }

    public void refreshGobsGrowthStages() {
        OCache oc = glob.oc;
        synchronized (oc) {
            for (Gob gob : oc) {
                if (Gob.Type.PLANT.has(gob.type) || gob.type == Gob.Type.TREE || gob.type == Gob.Type.BUSH)
                    oc.changed(gob);
            }
        }
    }

    public void startMusselsPicker(Gob gob) {
        musselPicker = new Thread(new MusselPicker(gameui(), gob), "MusselPicker");
        musselPicker.start();
    }
}
