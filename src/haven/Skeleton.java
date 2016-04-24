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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.media.opengl.GL2;

public class Skeleton {
    public final Map<String, Bone> bones = new HashMap<String, Bone>();
    public final Bone[] blist; /* Topologically sorted */
    public final Pose bindpose;

    public Skeleton(Collection<Bone> bones) {
        Set<Bone> bset = new HashSet<Bone>(bones);
        blist = new Bone[bones.size()];
        int idx = 0;
        for (Bone b : bones)
            this.bones.put(b.name, b);
        while (!bset.isEmpty()) {
            boolean f = false;
            for (Iterator<Bone> i = bset.iterator(); i.hasNext(); ) {
                Bone b = i.next();
                boolean has;
                if (b.parent == null) {
                    has = true;
                } else {
                    has = false;
                    for (Bone p : blist) {
                        if (p == b.parent) {
                            has = true;
                            break;
                        }
                    }
                }
                if (has) {
                    blist[b.idx = idx++] = b;
                    i.remove();
                    f = true;
                }
            }
            if (!f)
                throw (new RuntimeException("Cyclical bone hierarchy"));
        }
        bindpose = mkbindpose();
    }

    public static class Bone {
        public String name;
        public Coord3f ipos, irax;
        public float irang;
        public Bone parent;
        public int idx;

        public Bone(String name, Coord3f ipos, Coord3f irax, float irang) {
            this.name = name;
            this.ipos = ipos;
            this.irax = irax;
            this.irang = irang;
        }
    }

    private static float[] rotasq(float[] q, float[] axis, float angle) {
        float m = (float) Math.sin(angle / 2.0);
        q[0] = (float) Math.cos(angle / 2.0);
        q[1] = m * axis[0];
        q[2] = m * axis[1];
        q[3] = m * axis[2];
        return (q);
    }

    private static float[] qqmul(float[] d, float[] a, float[] b) {
        float aw = a[0], ax = a[1], ay = a[2], az = a[3];
        float bw = b[0], bx = b[1], by = b[2], bz = b[3];
        d[0] = (aw * bw) - (ax * bx) - (ay * by) - (az * bz);
        d[1] = (aw * bx) + (ax * bw) + (ay * bz) - (az * by);
        d[2] = (aw * by) - (ax * bz) + (ay * bw) + (az * bx);
        d[3] = (aw * bz) + (ax * by) - (ay * bx) + (az * bw);
        return (d);
    }

    private static float[] vqrot(float[] d, float[] v, float[] q) {
        float vx = v[0], vy = v[1], vz = v[2];
        float qw = q[0], qx = q[1], qy = q[2], qz = q[3];
    /* I dearly wonder how the JIT's common-subexpression
     * eliminator does on these. */
        d[0] = (qw * qw * vx) + (2 * qw * qy * vz) - (2 * qw * qz * vy) + (qx * qx * vx) +
                (2 * qx * qy * vy) + (2 * qx * qz * vz) - (qz * qz * vx) - (qy * qy * vx);
        d[1] = (2 * qx * qy * vx) + (qy * qy * vy) + (2 * qy * qz * vz) + (2 * qw * qz * vx) -
                (qz * qz * vy) + (qw * qw * vy) - (2 * qw * qx * vz) - (qx * qx * vy);
        d[2] = (2 * qx * qz * vx) + (2 * qy * qz * vy) + (qz * qz * vz) - (2 * qw * qy * vx) -
                (qy * qy * vz) + (2 * qw * qx * vy) - (qx * qx * vz) + (qw * qw * vz);
        return (d);
    }

    private static float[] vset(float[] d, float[] s) {
        d[0] = s[0];
        d[1] = s[1];
        d[2] = s[2];
        return (d);
    }

    private static float[] qset(float[] d, float[] s) {
        d[0] = s[0];
        d[1] = s[1];
        d[2] = s[2];
        d[3] = s[3];
        return (d);
    }

    private static float[] vinv(float[] d, float[] s) {
        d[0] = -s[0];
        d[1] = -s[1];
        d[2] = -s[2];
        return (d);
    }

    private static float[] qinv(float[] d, float[] s) {
    /* Assumes |s| = 1.0 */
        d[0] = s[0];
        d[1] = -s[1];
        d[2] = -s[2];
        d[3] = -s[3];
        return (d);
    }

    private static float[] vvadd(float[] d, float[] a, float[] b) {
        float ax = a[0], ay = a[1], az = a[2];
        float bx = b[0], by = b[1], bz = b[2];
        d[0] = ax + bx;
        d[1] = ay + by;
        d[2] = az + bz;
        return (d);
    }

    private static float[] qqslerp(float[] d, float[] a, float[] b, float t) {
        float aw = a[0], ax = a[1], ay = a[2], az = a[3];
        float bw = b[0], bx = b[1], by = b[2], bz = b[3];
        if ((aw == bw) && (ax == bx) && (ay == by) && (az == bz))
            return (qset(d, a));
        float cos = (aw * bw) + (ax * bx) + (ay * by) + (az * bz);
        if (cos < 0) {
            bw = -bw;
            bx = -bx;
            by = -by;
            bz = -bz;
            cos = -cos;
        }
        float d0, d1;
        if (cos > 0.9999f) {
        /* Reasonable threshold? Is this function even critical
	     * for performance? */
            d0 = 1.0f - t;
            d1 = t;
        } else {
            float da = (float) Math.acos(Utils.clip(cos, 0.0, 1.0));
            float nf = 1.0f / (float) Math.sin(da);
            d0 = (float) Math.sin((1.0f - t) * da) * nf;
            d1 = (float) Math.sin(t * da) * nf;
        }
        d[0] = (d0 * aw) + (d1 * bw);
        d[1] = (d0 * ax) + (d1 * bx);
        d[2] = (d0 * ay) + (d1 * by);
        d[3] = (d0 * az) + (d1 * bz);
        return (d);
    }

    public Pose mkbindpose() {
        Pose p = new Pose();
        for (int i = 0; i < blist.length; i++) {
            Bone b = blist[i];
            p.lpos[i][0] = b.ipos.x;
            p.lpos[i][1] = b.ipos.y;
            p.lpos[i][2] = b.ipos.z;
            rotasq(p.lrot[i], b.irax.to3a(), b.irang);
        }
        p.gbuild();
        return (p);
    }

    public class Pose {
        public float[][] lpos, gpos;
        public float[][] lrot, grot;
        private Pose from = null;
        public int seq = 0;

        private Pose() {
            int nb = blist.length;
            lpos = new float[nb][3];
            gpos = new float[nb][3];
            lrot = new float[nb][4];
            grot = new float[nb][4];
        }

        public Pose(Pose from) {
            this();
            this.from = from;
            reset();
            gbuild();
        }

        public Skeleton skel() {
            return (Skeleton.this);
        }

        public void reset() {
            for (int i = 0; i < blist.length; i++) {
                vset(lpos[i], from.lpos[i]);
                qset(lrot[i], from.lrot[i]);
            }
        }

        public void gbuild() {
            int nb = blist.length;
            for (int i = 0; i < nb; i++) {
                Bone b = blist[i];
                if (b.parent == null) {
                    gpos[i][0] = lpos[i][0];
                    gpos[i][1] = lpos[i][1];
                    gpos[i][2] = lpos[i][2];
                    grot[i][0] = lrot[i][0];
                    grot[i][1] = lrot[i][1];
                    grot[i][2] = lrot[i][2];
                    grot[i][3] = lrot[i][3];
                } else {
                    int pi = b.parent.idx;
                    qqmul(grot[i], grot[pi], lrot[i]);
                    vqrot(gpos[i], lpos[i], grot[pi]);
                    vvadd(gpos[i], gpos[i], gpos[pi]);
                }
            }
            seq++;
        }

        public void blend(Pose o, float d) {
            for (int i = 0; i < blist.length; i++) {
                qqslerp(lrot[i], lrot[i], o.lrot[i], d);
                lpos[i][0] = lpos[i][0] + ((o.lpos[i][0] - lpos[i][0]) * d);
                lpos[i][1] = lpos[i][1] + ((o.lpos[i][1] - lpos[i][1]) * d);
                lpos[i][2] = lpos[i][2] + ((o.lpos[i][2] - lpos[i][2]) * d);
            }
        }

        public Location bonetrans(final int bone) {
            return (new Location(Matrix4f.identity()) {
                private int cseq = -1;

                public Matrix4f fin(Matrix4f p) {
                    if (cseq != seq) {
                        Matrix4f xf = Transform.makexlate(new Matrix4f(), new Coord3f(gpos[bone][0], gpos[bone][1], gpos[bone][2]));
                        if (grot[bone][0] < 0.999999) {
                            float ang = (float) (Math.acos(grot[bone][0]) * 2.0);
                            xf = xf.mul1(Transform.makerot(new Matrix4f(), new Coord3f(grot[bone][1], grot[bone][2], grot[bone][3]).norm(), ang));
                        }
                        update(xf);
                        cseq = seq;
                    }
                    return (super.fin(p));
                }
            });
        }

        public Location bonetrans2(final int bone) {
            return (new Location(Matrix4f.identity()) {
                private int cseq = -1;
                private float[] pos = new float[3], rot = new float[4];

                public Matrix4f fin(Matrix4f p) {
                    if (cseq != seq) {
                        rot = qqmul(rot, grot[bone], qinv(rot, bindpose.grot[bone]));
                        pos = vvadd(pos, gpos[bone], vqrot(pos, vinv(pos, bindpose.gpos[bone]), rot));
                        Matrix4f xf = Transform.makexlate(new Matrix4f(), new Coord3f(pos[0], pos[1], pos[2]));
                        if (rot[0] < 0.999999) {
                            float ang = (float) (Math.acos(rot[0]) * 2.0);
                            xf = xf.mul1(Transform.makerot(new Matrix4f(), new Coord3f(rot[1], rot[2], rot[3]).norm(), ang));
                        }
                        update(xf);
                        cseq = seq;
                    }
                    return (super.fin(p));
                }
            });
        }

        public class BoneAlign extends Location {
            private final Coord3f ref;
            private final int orig, tgt;
            private int cseq = -1;

            public BoneAlign(Coord3f ref, Bone orig, Bone tgt) {
                super(Matrix4f.identity());
                this.ref = ref;
                this.orig = orig.idx;
                this.tgt = tgt.idx;
            }

            public Matrix4f fin(Matrix4f p) {
                if (cseq != seq) {
                    Coord3f cur = new Coord3f(gpos[tgt][0] - gpos[orig][0], gpos[tgt][1] - gpos[orig][1], gpos[tgt][2] - gpos[orig][2]).norm();
                    Coord3f axis = cur.cmul(ref).norm();
                    float ang = (float) Math.acos(cur.dmul(ref));
		    /*
		    System.err.println(cur + ", " + ref + ", " + axis + ", " + ang);
		    */
                    update(Transform.makexlate(new Matrix4f(), new Coord3f(gpos[orig][0], gpos[orig][1], gpos[orig][2]))
                            .mul1(Transform.makerot(new Matrix4f(), axis, -ang)));
                    cseq = seq;
                }
                return (super.fin(p));
            }
        }

        public void boneoff(int bone, float[] offtrans) {
	    /* It would be nice if these "new float"s get
	     * stack-allocated. */
            float[] rot = new float[4], xlate = new float[3];
            rot = qqmul(rot, grot[bone], qinv(rot, bindpose.grot[bone]));
            xlate = vvadd(xlate, gpos[bone], vqrot(xlate, vinv(xlate, bindpose.gpos[bone]), rot));
            offtrans[3] = 0;
            offtrans[7] = 0;
            offtrans[11] = 0;
            offtrans[15] = 1;
            offtrans[12] = xlate[0];
            offtrans[13] = xlate[1];
            offtrans[14] = xlate[2];
	    /* I must admit I don't /quite/ understand why the
	     * rotation needs to be inverted... */
            float w = -rot[0], x = rot[1], y = rot[2], z = rot[3];
            float xw = x * w * 2, xx = x * x * 2, xy = x * y * 2, xz = x * z * 2;
            float yw = y * w * 2, yy = y * y * 2, yz = y * z * 2;
            float zw = z * w * 2, zz = z * z * 2;
            offtrans[0] = 1 - (yy + zz);
            offtrans[5] = 1 - (xx + zz);
            offtrans[10] = 1 - (xx + yy);
            offtrans[1] = xy - zw;
            offtrans[2] = xz + yw;
            offtrans[4] = xy + zw;
            offtrans[6] = yz - xw;
            offtrans[8] = xz - yw;
            offtrans[9] = yz + xw;
        }

        public final Rendered debug = new Rendered() {
            public void draw(GOut g) {
                BGL gl = g.gl;
                g.st.put(Light.lighting, null);
                g.state(States.xray);
                g.apply();
                gl.glBegin(GL2.GL_LINES);
                for (int i = 0; i < blist.length; i++) {
                    if (blist[i].parent != null) {
                        int pi = blist[i].parent.idx;
                        gl.glColor3f(1.0f, 0.0f, 0.0f);
                        gl.glVertex3f(gpos[pi][0], gpos[pi][1], gpos[pi][2]);
                        gl.glColor3f(0.0f, 1.0f, 0.0f);
                        gl.glVertex3f(gpos[i][0], gpos[i][1], gpos[i][2]);
                    }
                }
                gl.glEnd();
            }

            public boolean setup(RenderList rl) {
                rl.prepo(States.vertexcolor);
                rl.prepo(States.xray);
                return (true);
            }
        };
    }

    public interface HasPose {
        public Pose getpose();
    }

    public static Pose getpose(Object owner) {
        if (owner instanceof HasPose)
            return (((HasPose) owner).getpose());
        return (null);
    }

    public interface ModOwner {
        public double getv();

        public Coord3f getc();

        public Glob glob();

        public static final ModOwner nil = new ModOwner() {
            public double getv() {
                return (0);
            }

            public Coord3f getc() {
                return (Coord3f.o);
            }

            public Glob glob() {
                throw (new NullPointerException());
            }
        };
    }

    public abstract class PoseMod {
        public final ModOwner owner;
        public float[][] lpos, lrot;

        public PoseMod(ModOwner owner) {
            this.owner = owner;
            int nb = blist.length;
            lpos = new float[nb][3];
            lrot = new float[nb][4];
            for (int i = 0; i < nb; i++)
                lrot[i][0] = 1;
        }

        @Deprecated
        public PoseMod() {
            this(ModOwner.nil);
        }

        public Skeleton skel() {
            return (Skeleton.this);
        }

        public void reset() {
            for (int i = 0; i < blist.length; i++) {
                lpos[i][0] = 0;
                lpos[i][1] = 0;
                lpos[i][2] = 0;
                lrot[i][0] = 1;
                lrot[i][1] = 0;
                lrot[i][2] = 0;
                lrot[i][3] = 0;
            }
        }

        public void rot(int bone, float ang, float ax, float ay, float az) {
            float[] x = {ax, ay, az};
            qqmul(lrot[bone], lrot[bone], rotasq(new float[4], x, ang));
        }

        public void apply(Pose p) {
            for (int i = 0; i < blist.length; i++) {
                vvadd(p.lpos[i], p.lpos[i], lpos[i]);
                qqmul(p.lrot[i], p.lrot[i], lrot[i]);
            }
        }

        public boolean tick(float dt) {
            return (false);
        }

        public void age() {
        }

        public abstract boolean stat();

        public abstract boolean done();
    }

    public PoseMod nilmod() {
        return (new PoseMod(ModOwner.nil) {
            public boolean stat() {
                return (true);
            }

            public boolean done() {
                return (false);
            }
        });
    }

    public static PoseMod combine(final PoseMod... mods) {
        PoseMod first = mods[0];
        return (first.skel().new PoseMod(first.owner) {
            final boolean stat;

            {
                boolean s = true;
                for (PoseMod m : mods)
                    s = s && m.stat();
                stat = s;
            }

            public void apply(Pose p) {
                for (PoseMod m : mods)
                    m.apply(p);
            }

            public boolean tick(float dt) {
                boolean ret = false;
                for (PoseMod m : mods) {
                    if (m.tick(dt))
                        ret = true;
                }
                return (ret);
            }

            public void age() {
                for (PoseMod m : mods)
                    m.age();
            }

            public boolean stat() {
                return (stat);
            }

            public boolean done() {
                for (PoseMod m : mods) {
                    if (m.done())
                        return (true);
                }
                return (false);
            }
        });
    }

    @Resource.PublishedCode(name = "pose")
    public interface ModFactory {
        public PoseMod create(Skeleton skel, ModOwner owner, Resource res, Message sdt);

        public static final ModFactory def = new ModFactory() {
            public PoseMod create(Skeleton skel, ModOwner owner, Resource res, Message sdt) {
                int mask = Sprite.decnum(sdt);
                Collection<PoseMod> poses = new ArrayList<PoseMod>(16);
                for (ResPose p : res.layers(ResPose.class)) {
                    if ((p.id < 0) || ((mask & (1 << p.id)) != 0)) {
                        if (Config.disableanimSet.contains("/idle") && res.name.endsWith("/idle") &&
                                !res.name.startsWith("gfx/borka"))
                            continue;
                        poses.add(p.forskel(owner, skel, p.defmode));
                    }
                }
                if (poses.size() == 0)
                    return (skel.nilmod());
                else if (poses.size() == 1)
                    return (Utils.el(poses));
                else
                    return (combine(poses.toArray(new PoseMod[0])));
            }
        };
    }

    public PoseMod mkposemod(ModOwner owner, Resource res, Message sdt) {
        ModFactory f = res.getcode(ModFactory.class, false);
        if (f == null)
            f = ModFactory.def;
        return (f.create(this, owner, res, sdt));
    }

    public static class ResourceSkeleton extends Skeleton {
        public final Resource res;

        public ResourceSkeleton(Collection<Bone> bones, Res info) {
            super(bones);
            this.res = info.getres();
        }

        public String toString() {
            return ("Skeleton(" + res.name + ")");
        }
    }

    @Resource.LayerName("skel")
    public static class Res extends Resource.Layer {
        public final transient Skeleton s;

        public Res(Resource res, Message buf) {
            res.super();
            Map<String, Bone> bones = new HashMap<String, Bone>();
            Map<Bone, String> pm = new HashMap<Bone, String>();
            while (!buf.eom()) {
                String bnm = buf.string();
                Coord3f pos = new Coord3f((float) buf.cpfloat(), (float) buf.cpfloat(), (float) buf.cpfloat());
                Coord3f rax = new Coord3f((float) buf.cpfloat(), (float) buf.cpfloat(), (float) buf.cpfloat()).norm();
                float rang = (float) buf.cpfloat();
                String bp = buf.string();
                Bone b = new Bone(bnm, pos, rax, rang);
                if (bones.put(bnm, b) != null)
                    throw (new RuntimeException("Duplicate bone name: " + b.name));
                pm.put(b, bp);
            }
            for (Bone b : bones.values()) {
                String bp = pm.get(b);
                if (bp.length() == 0) {
                    b.parent = null;
                } else {
                    if ((b.parent = bones.get(bp)) == null)
                        throw (new Resource.LoadException("Parent bone " + bp + " not found for " + b.name, getres()));
                }
            }
            s = new ResourceSkeleton(bones.values(), this);
        }

        public void init() {
        }
    }

    public class TrackMod extends PoseMod {
        public final Track[] tracks;
        public final FxTrack[] effects;
        private final Collection<FxTrack.EventListener> cbl = new ArrayList<FxTrack.EventListener>(0);
        public final float len;
        public final WrapMode mode;
        private final boolean stat;
        private boolean done;
        public float time = 0.0f;
        protected boolean speedmod = false;
        protected double nspeed = 0.0;
        private boolean back = false;

        public TrackMod(ModOwner owner, Track[] tracks, FxTrack[] effects, float len, WrapMode mode) {
            super(owner);
            this.tracks = tracks;
            this.effects = effects;
            this.len = len;
            this.mode = mode;
            for (Track t : tracks) {
                if ((t != null) && (t.frames.length > 1)) {
                    stat = false;
                    aupdate(0.0f);
                    return;
                }
            }
            stat = done = true;
            aupdate(0.0f);
        }

        @Deprecated
        public TrackMod(Track[] tracks, float len, WrapMode mode) {
            this(ModOwner.nil, tracks, new FxTrack[0], len, mode);
        }

        public void aupdate(float time) {
            if (time > len)
                time = len;
            reset();
            for (int i = 0; i < tracks.length; i++) {
                Track t = tracks[i];
                if ((t == null) || (t.frames.length == 0))
                    continue;
                if (t.frames.length == 1) {
                    qset(lrot[i], t.frames[0].rot);
                    vset(lpos[i], t.frames[0].trans);
                } else {
                    Track.Frame cf, nf;
                    float ct, nt;
                    int l = 0, r = t.frames.length;
                    int n = 0;
                    while (true) {
                        if (++n > 100)
                            throw (new RuntimeException("Cannot find track frame in " + this + " for time " + time));
			/* c should never be able to be >= frames.length */
                        int c = l + ((r - l) >> 1);
                        ct = t.frames[c].time;
                        nt = (c < t.frames.length - 1) ? (t.frames[c + 1].time) : len;
                        if (ct > time) {
                            r = c;
                        } else if (nt < time) {
                            l = c + 1;
                        } else {
                            cf = t.frames[c];
                            nf = t.frames[(c + 1) % t.frames.length];
                            break;
                        }
                    }
                    float d;
                    if (nt == ct)
                        d = 0;
                    else
                        d = (time - ct) / (nt - ct);
                    qqslerp(lrot[i], cf.rot, nf.rot, d);
                    lpos[i][0] = cf.trans[0] + ((nf.trans[0] - cf.trans[0]) * d);
                    lpos[i][1] = cf.trans[1] + ((nf.trans[1] - cf.trans[1]) * d);
                    lpos[i][2] = cf.trans[2] + ((nf.trans[2] - cf.trans[2]) * d);
                }
            }
        }

        public void listen(FxTrack.EventListener l) {
            cbl.add(l);
        }

        private void playfx(float ot, float nt) {
            if (!(owner instanceof Gob))
                return;
            Gob gob = (Gob) owner;
            if (ot > nt) {
                playfx(Math.min(ot, len), len);
                playfx(0, Math.max(0, nt));
            } else {
                for (FxTrack t : effects) {
                    for (FxTrack.Event ev : t.events) {
                        if ((ev.time >= ot) && (ev.time < nt)) {
                            for (FxTrack.EventListener l : cbl)
                                l.event(ev);
                            ev.trigger(gob);
                        }
                    }
                }
            }
        }

        public boolean tick(float dt) {
            if (speedmod)
                dt *= owner.getv() / nspeed;
            float nt = time + (back ? -dt : dt);
            switch (mode) {
                case LOOP:
                    nt %= len;
                    break;
                case ONCE:
                    if (nt > len) {
                        nt = len;
                        done = true;
                    }
                    break;
                case PONG:
                    if (!back && (nt > len)) {
                        nt = len;
                        back = true;
                    } else if (back && (nt < 0)) {
                        nt = 0;
                        done = true;
                    }
                    break;
                case PONGLOOP:
                    if (!back && (nt > len)) {
                        nt = len;
                        back = true;
                    } else if (back && (nt < 0)) {
                        nt = 0;
                        back = false;
                    }
                    break;
            }
            float ot = this.time;
            this.time = nt;
            if (!stat) {
                aupdate(this.time);
                if (!back)
                    playfx(ot, nt);
                else
                    playfx(nt, ot);
                return (true);
            } else {
                return (false);
            }
        }

        public void age() {
            switch (mode) {
                case PONGLOOP:
                    back = Math.random() >= 0.5;
                case LOOP:
                    time = (float) Math.random() * len;
                    break;
                case PONG:
                    back = true;
                    time = 0;
                    break;
                case ONCE:
                    time = len;
                    break;
            }
            aupdate(time);
        }

        public boolean stat() {
            return (stat);
        }

        public boolean done() {
            return (done);
        }
    }

    public static class Track {
        public final String bone;
        public final Frame[] frames;

        public static class Frame {
            public final float time;
            public final float[] trans, rot;

            public Frame(float time, float[] trans, float[] rot) {
                this.time = time;
                this.trans = trans;
                this.rot = rot;
            }
        }

        public Track(String bone, Frame[] frames) {
            this.bone = bone;
            this.frames = frames;
        }
    }

    public static class FxTrack {
        public final Event[] events;

        public static interface EventListener {
            public void event(Event ev);
        }

        public static abstract class Event {
            public final float time;

            public Event(float time) {
                this.time = time;
            }

            public abstract void trigger(Gob gob);
        }

        public FxTrack(Event[] events) {
            this.events = events;
        }

        public static class SpawnSprite extends Event {
            public final Indir<Resource> res;
            public final byte[] sdt;
            public final Location loc;

            public SpawnSprite(float time, Indir<Resource> res, byte[] sdt, Location loc) {
                super(time);
                this.res = res;
                this.sdt = (sdt == null) ? new byte[0] : sdt;
                this.loc = loc;
            }

            public void trigger(Gob gob) {
                final Coord3f fc;
                try {
                    fc = gob.getc();
                } catch (Loading e) {
                    return;
                }
                Gob n = gob.glob.oc.new Virtual(gob.rc, gob.a) {
                    public Coord3f getc() {
                        return (new Coord3f(fc));
                    }

                    public boolean setup(RenderList rl) {
                        if (SpawnSprite.this.loc != null)
                            rl.prepc(SpawnSprite.this.loc);
                        return (super.setup(rl));
                    }
                };
                n.ols.add(new Gob.Overlay(-1, res, new MessageBuf(sdt)));
            }
        }

        public static class Trigger extends Event {
            public final String id;

            public Trigger(float time, String id) {
                super(time);
                this.id = id.intern();
            }

            public void trigger(Gob gob) {
            }
        }
    }

    @SuppressWarnings("serial")
	@Resource.LayerName("skan")
    public static class ResPose extends Resource.Layer implements Resource.IDLayer<Integer> {
        public final int id;
        public final float len;
        public final transient Track[] tracks;
        public final transient FxTrack[] effects;
        public final double nspeed;
        public final WrapMode defmode;

        private static Track.Frame[] parseframes(Message buf) {
            Track.Frame[] frames = new Track.Frame[buf.uint16()];
            for (int i = 0; i < frames.length; i++) {
                float tm = (float) buf.cpfloat();
                float[] trans = new float[3];
                for (int o = 0; o < 3; o++)
                    trans[o] = (float) buf.cpfloat();
                float rang = (float) buf.cpfloat();
                float[] rax = new float[3];
                for (int o = 0; o < 3; o++)
                    rax[o] = (float) buf.cpfloat();
                frames[i] = new Track.Frame(tm, trans, rotasq(new float[4], rax, rang));
            }
            return (frames);
        }

        private FxTrack parsefx(Message buf) {
            FxTrack.Event[] events = new FxTrack.Event[buf.uint16()];
            for (int i = 0; i < events.length; i++) {
                float tm = (float) buf.cpfloat();
                int t = buf.uint8();
                switch (t) {
                    case 0:
                        String resnm = buf.string();
                        int resver = buf.uint16();
                        byte[] sdt = buf.bytes(buf.uint8());
                        Indir<Resource> res = getres().pool.load(resnm, resver);
                        events[i] = new FxTrack.SpawnSprite(tm, res, sdt, null);
                        break;
                    case 1:
                        String id = buf.string();
                        events[i] = new FxTrack.Trigger(tm, id);
                        break;
                    default:
                        throw (new Resource.LoadException("Illegal control event: " + t, getres()));
                }
            }
            return (new FxTrack(events));
        }

        public ResPose(Resource res, Message buf) {
            res.super();
            this.id = buf.int16();
            int fl = buf.uint8();
            int mode = buf.uint8();
            if (mode == 0)
                defmode = WrapMode.ONCE;
            else if (mode == 1)
                defmode = WrapMode.LOOP;
            else if (mode == 2)
                defmode = WrapMode.PONG;
            else if (mode == 3)
                defmode = WrapMode.PONGLOOP;
            else
                throw (new Resource.LoadException("Illegal animation mode: " + mode, getres()));
            this.len = (float) buf.cpfloat();
            if ((fl & 1) != 0)
                nspeed = buf.cpfloat();
            else
                nspeed = -1;
            Collection<Track> tracks = new LinkedList<Track>();
            Collection<FxTrack> fx = new LinkedList<FxTrack>();
            while (!buf.eom()) {
                String bnm = buf.string();
                if (bnm.equals("{ctl}")) {
                    fx.add(parsefx(buf));
                } else {
                    tracks.add(new Track(bnm, parseframes(buf)));
                }
            }
            this.tracks = tracks.toArray(new Track[0]);
            this.effects = fx.toArray(new FxTrack[0]);
        }

        private Track[] iaIaCthulhuFhtagn(Skeleton skel) {
            Track[] remap = new Track[skel.blist.length];
            for (Track t : tracks) {
                Skeleton.Bone b = skel.bones.get(t.bone);
                if (b == null)
                    throw (new RuntimeException("Bone \"" + t.bone + "\" in animation reference does not exist in skeleton " + skel));
                remap[b.idx] = t;
            }
            return (remap);
        }

        public class ResMod extends TrackMod {
            public ResMod(ModOwner owner, Skeleton skel, WrapMode mode) {
                skel.super(owner, iaIaCthulhuFhtagn(skel), ResPose.this.effects, ResPose.this.len, mode);
                if (ResPose.this.nspeed > 0) {
                    this.speedmod = true;
                    this.nspeed = ResPose.this.nspeed;
                }
            }

            public ResMod(ModOwner owner, Skeleton skel) {
                this(owner, skel, defmode);
            }

            public String toString() {
                return (String.format("#<pose %d in %s>", id, getres().name));
            }
        }

        public TrackMod forskel(ModOwner owner, Skeleton skel, WrapMode mode) {
            return (new ResMod(owner, skel, mode));
        }

        @Deprecated
        public TrackMod forskel(Skeleton skel, WrapMode mode) {
            return (forskel(ModOwner.nil, skel, mode));
        }

        @Deprecated
        public TrackMod forgob(Skeleton skel, WrapMode mode, Gob gob) {
            return (forskel(gob, skel, mode));
        }

        public Integer layerid() {
            return (id);
        }

        public void init() {
        }
    }

    @SuppressWarnings("serial")
	@Resource.LayerName("boneoff")
    public static class BoneOffset extends Resource.Layer implements Resource.IDLayer<String> {
        public final String nm;
        public final transient Command[] prog;
        private static final HatingJava[] opcodes = new HatingJava[256];

        static {
            opcodes[0] = new HatingJava() {
                public Command make(Message buf) {
                    final float x = (float) buf.cpfloat();
                    final float y = (float) buf.cpfloat();
                    final float z = (float) buf.cpfloat();
                    return (new Command() {
                        public GLState make(Pose pose) {
                            return (Location.xlate(new Coord3f(x, y, z)));
                        }
                    });
                }
            };
            opcodes[1] = new HatingJava() {
                public Command make(Message buf) {
                    final float ang = (float) buf.cpfloat();
                    final float ax = (float) buf.cpfloat();
                    final float ay = (float) buf.cpfloat();
                    final float az = (float) buf.cpfloat();
                    return (new Command() {
                        public GLState make(Pose pose) {
                            return (Location.rot(new Coord3f(ax, ay, az), ang));
                        }
                    });
                }
            };
            opcodes[2] = new HatingJava() {
                public Command make(Message buf) {
                    final String bonenm = buf.string();
                    return (new Command() {
                        public GLState make(Pose pose) {
                            Bone bone = pose.skel().bones.get(bonenm);
                            return (pose.bonetrans(bone.idx));
                        }
                    });
                }
            };
            opcodes[3] = new HatingJava() {
                public Command make(Message buf) {
                    float rx1 = (float) buf.cpfloat();
                    float ry1 = (float) buf.cpfloat();
                    float rz1 = (float) buf.cpfloat();
                    float l = (float) Math.sqrt((rx1 * rx1) + (ry1 * ry1) + (rz1 * rz1));
                    final Coord3f ref = new Coord3f(rx1 / l, ry1 / l, rz1 / l);
                    final String orignm = buf.string();
                    final String tgtnm = buf.string();
                    return (new Command() {
                        public GLState make(Pose pose) {
                            Bone orig = pose.skel().bones.get(orignm);
                            Bone tgt = pose.skel().bones.get(tgtnm);
                            return (pose.new BoneAlign(ref, orig, tgt));
                        }
                    });
                }
            };
        }

        public interface Command {
            public GLState make(Pose pose);
        }

        public interface HatingJava {
            public Command make(Message buf);
        }

        public BoneOffset(Resource res, Message buf) {
            res.super();
            this.nm = buf.string();
            List<Command> cbuf = new LinkedList<Command>();
            while (!buf.eom())
                cbuf.add(opcodes[buf.uint8()].make(buf));
            this.prog = cbuf.toArray(new Command[0]);
        }

        public String layerid() {
            return (nm);
        }

        public void init() {
        }

        public GLState forpose(Pose pose) {
            GLState[] ls = new GLState[prog.length];
            for (int i = 0; i < prog.length; i++)
                ls[i] = prog[i].make(pose);
            return (GLState.compose(ls));
        }
    }
}
