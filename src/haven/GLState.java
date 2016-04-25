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

import static haven.GOut.checkerr;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL3;

import haven.glsl.ShaderMacro;

public abstract class GLState {
    public abstract void apply(GOut g);
    public abstract void unapply(GOut g);
    public abstract void prep(Buffer buf);

    public void applyfrom(GOut g, GLState from) {
        throw(new RuntimeException("Called applyfrom on non-conformant GLState (" + from + " -> " + this + ")"));
    }
    public void applyto(GOut g, GLState to) {
    }
    public void reapply(GOut g) {
    }

    public ShaderMacro[] shaders() {
        return(null);
    }

    public int capply() {
        return(10);
    }
    public int cunapply() {
        return(1);
    }
    public int capplyfrom(GLState from) {
        return(-1);
    }
    public int capplyto(GLState to) {
        return(0);
    }

    public interface Instancer<T extends GLState> {
        public T inststate(T[] states);
        public static final ShaderMacro mkinstanced = new ShaderMacro() {
            public void modify(haven.glsl.ProgramContext ctx) {
                ctx.instanced = true;
            }
        };
    }

    private static int slotnum = 0;
    private static Slot<?>[] deplist = new Slot<?>[0];
    private static Slot<?>[] idlist = new Slot<?>[0];

    public static class Slot<T extends GLState> {
        private volatile static boolean dirty = false;
        private static Collection<Slot<?>> all = new LinkedList<Slot<?>>();
        public final Type type;
        public final int id;
        public final Class<T> scl;
        public Instancer<T> instanced;
        private int depid = -1;
        private final Slot<?>[] dep, rdep;
        @SuppressWarnings("rawtypes")
		private Slot[] grdep;

        public static enum Type {
            SYS, GEOM, DRAW
        }

        public Slot(Type type, Class<T> scl, Slot<?>[] dep, Slot<?>[] rdep) {
            this.type = type;
            this.scl = scl;
            synchronized(Slot.class) {
                this.id = slotnum++;
                dirty = true;
                Slot<?>[] nlist = new Slot<?>[slotnum];
                System.arraycopy(idlist, 0, nlist, 0, idlist.length);
                nlist[this.id] = this;
                idlist = nlist;
                all.add(this);
            }
            if(dep == null)
                this.dep = new Slot<?>[0];
            else
                this.dep = dep;
            if(rdep == null)
                this.rdep = new Slot<?>[0];
            else
                this.rdep = rdep;
            for(Slot<?> ds : this.dep) {
                if(ds == null)
                    throw(new NullPointerException());
            }
            for(Slot<?> ds : this.rdep) {
                if(ds == null)
                    throw(new NullPointerException());
            }
        }

        public Slot(Type type, Class<T> scl, @SuppressWarnings("rawtypes") Slot... dep) {
            this(type, scl, dep, null);
        }

        public Slot<T> instanced(Instancer<T> inst) {
            if(inst == null)
                throw(new NullPointerException());
            this.instanced = inst;
            return(this);
        }

        private static void makedeps(Collection<Slot<?>> slots) {
            Map<Slot<?>, Set<Slot<?>>> lrdep = new HashMap<Slot<?>, Set<Slot<?>>>();
            for(Slot<?> s : slots)
                lrdep.put(s, new HashSet<Slot<?>>());
            for(Slot<?> s : slots) {
                lrdep.get(s).addAll(Arrays.asList(s.rdep));
                for(Slot<?> ds : s.dep)
                    lrdep.get(ds).add(s);
            }
            Set<Slot<?>> left = new HashSet<Slot<?>>(slots);
            final Map<Slot<?>, Integer> order = new HashMap<Slot<?>, Integer>();
            int id = left.size() - 1;
            Slot<?>[] cp = new Slot<?>[0];
            while(!left.isEmpty()) {
                boolean err = true;
                fin:
                for(Iterator<Slot<?>> i = left.iterator(); i.hasNext();) {
                    Slot<?> s = i.next();
                    for(Slot<?> ds : lrdep.get(s)) {
                        if(left.contains(ds))
                            continue fin;
                    }
                    err = false;
                    order.put(s, s.depid = id--);
                    Set<Slot<?>> grdep = new HashSet<Slot<?>>();
                    for(Slot<?> ds : lrdep.get(s)) {
                        grdep.add(ds);
                        for(Slot<?> ds2 : ds.grdep)
                            grdep.add(ds2);
                    }
                    s.grdep = grdep.toArray(cp);
                    i.remove();
                }
                if(err)
                    throw(new RuntimeException("Cycle encountered while compiling state slot dependencies"));
            }
            @SuppressWarnings("rawtypes")
			Comparator<Slot> cmp = new Comparator<Slot>() {
                public int compare(Slot a, Slot b) {
                    return(order.get(a) - order.get(b));
                }
            };
            for(Slot<?> s : slots)
                Arrays.sort(s.grdep, cmp);
        }

        public static void update() {
            if(dirty) {
                synchronized(Slot.class) {
                    if(dirty) {
                        makedeps(all);
                        deplist = new Slot<?>[all.size()];
                        for(Slot s : all)
                            deplist[s.depid] = s;
                        dirty = false;
                    }
                }
            }
        }

        public String toString() {
            return("Slot<" + scl.getName() + ">");
        }
    }

    public static class Buffer {
        private GLState[] states = new GLState[slotnum];
        public final GLConfig cfg;

        public Buffer(GLConfig cfg) {
            this.cfg = cfg;
        }

        public Buffer copy() {
            Buffer ret = new Buffer(cfg);
            System.arraycopy(states, 0, ret.states, 0, states.length);
            return(ret);
        }

        public void copy(Buffer dest) {
            dest.adjust();
            System.arraycopy(states, 0, dest.states, 0, states.length);
            for(int i = states.length; i < dest.states.length; i++)
                dest.states[i] = null;
        }

        public void copy(Buffer dest, Slot.Type type) {
            dest.adjust();
            for(int i = 0; i < states.length; i++) {
                if(idlist[i].type == type)
                    dest.states[i] = states[i];
            }
            for(int i = states.length; i < dest.states.length; i++) {
                if(idlist[i].type == type)
                    dest.states[i] = null;
            }
        }

        public void copye(Buffer dest, Slot.Type type) {
            dest.adjust();
            for(int i = 0; i < states.length; i++) {
                if(idlist[i].type != type)
                    dest.states[i] = states[i];
            }
            for(int i = states.length; i < dest.states.length; i++) {
                if(i < idlist.length && idlist[i].type != type) // NOTE: hotfix for IOOB exception
                    dest.states[i] = null;
            }
        }

        public int ihash() {
            int ret = 0;
            for(int i = 0; i < states.length; i++) {
                if((idlist[i].instanced == null) && (states[i] != null))
                    ret = (ret * 31) + System.identityHashCode(states[i]);
            }
            return(ret);
        }

        public boolean iequals(Buffer o) {
            GLState[] a, b;
            int i = 0;

            if(states.length > o.states.length) {
                a = states; b = o.states;
            } else {
                b = states; a = o.states;
            }
            for(; i < b.length; i++) {
                if((idlist[i].instanced == null) && (a[i] != b[i]))
                    return(false);
            }
            for(; i < a.length; i++) {
                if((idlist[i].instanced == null) && (a[i] != null))
                    return(false);
            }
            return(true);
        }

        private void adjust() {
            if(states.length < slotnum) {
                GLState[] n = new GLState[slotnum];
                System.arraycopy(states, 0, n, 0, states.length);
                this.states = n;
            }
        }

        public <T extends GLState> void put(Slot<? super T> slot, T state) {
            if(states.length <= slot.id)
                adjust();
            states[slot.id] = state;
        }

        @SuppressWarnings("unchecked")
        public <T extends GLState> T get(Slot<T> slot) {
            if(states.length <= slot.id)
                return(null);
            return((T)states[slot.id]);
        }

        public int hashCode() {
            int h = 0;
            for(int i = 0; i < states.length; i++) {
                if(states[i] != null)
                    h = (h * 31) + states[i].hashCode();
            }
            return(h);
        }

        public boolean equals(Object oo) {
            if(!(oo instanceof Buffer))
                return(false);
            Buffer o = (Buffer)oo;
            GLState[] a, b;
            int i = 0;

            if(states.length > o.states.length) {
                a = states; b = o.states;
            } else {
                b = states; a = o.states;
            }
            for(; i < b.length; i++) {
                if(!Utils.eq(a[i], b[i]))
                    return(false);
            }
            for(; i < a.length; i++) {
                if(a[i] != null)
                    return(false);
            }
            return(true);
        }

        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append('[');
            for(int i = 0; i < states.length; i++) {
                if(i > 0)
                    buf.append(", ");
                if(states[i] == null)
                    buf.append("null");
                else
                    buf.append(states[i].toString());
            }
            buf.append(']');
            return(buf.toString());
        }

        /* Should be used very, very sparingly. */
        GLState[] states() {
            return(states);
        }
    }

    public static int bufdiff(Buffer f, Buffer t, boolean[] trans, boolean[] repl) {
        int cost = 0;
        f.adjust(); t.adjust();
        if(trans != null) {
            for(int i = 0; i < trans.length; i++) {
                trans[i] = false;
                repl[i] = false;
            }
        }
        for(int i = 0; i < f.states.length; i++) {
            if(((f.states[i] == null) != (t.states[i] == null)) ||
                    ((f.states[i] != null) && (t.states[i] != null) && !f.states[i].equals(t.states[i]))) {
                if(!repl[i]) {
                    int cat = -1, caf = -1;
                    if((t.states[i] != null) && (f.states[i] != null)) {
                        cat = f.states[i].capplyto(t.states[i]);
                        caf = t.states[i].capplyfrom(f.states[i]);
                    }
                    if((cat >= 0) && (caf >= 0)) {
                        cost += cat + caf;
                        if(trans != null)
                            trans[i] = true;
                    } else {
                        if(f.states[i] != null)
                            cost += f.states[i].cunapply();
                        if(t.states[i] != null)
                            cost += t.states[i].capply();
                        if(trans != null)
                            repl[i] = true;
                    }
                }
                for(Slot ds : idlist[i].grdep) {
                    int id = ds.id;
                    if(repl[id])
                        continue;
                    if(trans != null)
                        repl[id] = true;
                    if(t.states[id] != null)
                        cost += t.states[id].cunapply();
                    if(f.states[id] != null)
                        cost += f.states[id].capply();
                }
            }
        }
        return(cost);
    }

    public static class TexUnit {
        private final Applier st;
        public final int id;

        private TexUnit(Applier st, int id) {
            this.st = st;
            this.id = id;
        }

        public void act(GOut g) {
            st.texunit(g, id);
        }

        public void free() {
            if(st.textab[id] != null)
                throw(new RuntimeException("Texunit " + id + " freed twice"));
            st.textab[id] = this;
        }

        public void ufree(GOut g) {
            act(g);
            g.gl.glBindTexture(GL.GL_TEXTURE_2D, null);
            free();
        }
    }

    private static <S extends GLState> boolean inststate0(Buffer tgt, Slot<S> slot, List<Buffer> instances) {
        S[] buf = Utils.mkarray(slot.scl, instances.size());
        int n = 0;
        boolean hnn = false;
        for(Buffer st : instances) {
            if((buf[n++] = st.get(slot)) != null)
                hnn = true;
        }
        if(!hnn) {
            tgt.put(slot, null);
            return(true);
        }
        S st = slot.instanced.inststate(buf);
        if(st == null)
            return(false);
        tgt.put(slot, st);
        return(true);
    }

    public static Buffer inststate(GLConfig cfg, List<Buffer> instances) {
        Buffer ret = new Buffer(cfg);
        Buffer first = Utils.el(instances);
        first.copy(ret);
        for(int i = 0; i < ret.states.length; i++) {
            if(idlist[i].instanced != null) {
                if(!inststate0(ret, idlist[i], instances))
                    return(null);
            }
        }
        return(ret);
    }

    public static class Applier {
        public static boolean debug = false;
        private Buffer old, cur, next;
        public final CurrentGL cgl;
        public final GLConfig cfg;
        private boolean[] trans = new boolean[0], repl = new boolean[0];
        private ShaderMacro[][] shaders = new ShaderMacro[0][], nshaders = new ShaderMacro[0][];
        private int proghash = 0, nproghash = 0;
        public ShaderMacro.Program prog;
        public boolean pdirty = false, sdirty = false;
        public long time = 0;

        /* XXX: Arguably, there should be a way to more fundamentally
         * affect the gl_Position generation code instead of this. */
        public Matrix4f proj = Matrix4f.id;
        private Matrix4f cproj = null;

        public Applier(CurrentGL cgl) {
            this.cgl = cgl;
            this.cfg = cgl.cfg;
            this.old = new Buffer(cfg);
            this.cur = new Buffer(cfg);
            this.next = new Buffer(cfg);
        }

        public <T extends GLState> void put(Slot<? super T> slot, T state) {
            next.put(slot, state);
        }

        public <T extends GLState> T get(Slot<T> slot) {
            return(next.get(slot));
        }

        public <T extends GLState> T cur(Slot<T> slot) {
            return(cur.get(slot));
        }

        public <T extends GLState> T old(Slot<T> slot) {
            return(old.get(slot));
        }

        public void prep(GLState st) {
            st.prep(next);
        }

        public void set(Buffer to) {
            to.copy(next);
        }

        public void copy(Buffer dest) {
            next.copy(dest);
        }

        public Buffer cstate() {
            return(cur);
        }

        public Buffer state() {
            return(next);
        }

        public Buffer copy() {
            return(next.copy());
        }

        public <T extends GLState> void apply(GOut g, Slot<T> slot, T state) {
            int id = slot.id;
            next.states[id] = state;
            GLState old = cur.states[id];
            if((old == null) && (state == null)) {
            } else if((old != null) && (state == null)) {
                if(shaders[id] != null)
                    throw(new RuntimeException("Cannot quick-apply states with shaders"));
                old.unapply(g);
                cur.states[id] = null;
            } else if((old == null) && (state != null)) {
                if(state.shaders() != null)
                    throw(new RuntimeException("Cannot quick-apply states with shaders"));
                state.apply(g);
                cur.states[id] = state;
            } else if((old != null) && (state != null) && !old.equals(state)) {
                if(state.shaders() != shaders[id])
                    throw(new RuntimeException("Cannot quick-apply states with shader replacement"));
                if(state.capplyfrom(old) >= 0) {
                    state.applyfrom(g, old);
                    cur.states[id] = state;
                } else {
                    old.unapply(g);
                    cur.states[id] = null;
                    state.apply(g);
                    cur.states[id] = state;
                }
            }
        }

        public void apply(GOut g) {
            Slot.update();
            long st = 0;
            if(Config.profile) st = System.nanoTime();
            Slot<?>[] deplist = GLState.deplist;
            if(trans.length < deplist.length) {
                synchronized(Slot.class) {
                    trans = new boolean[deplist.length];
                    repl = new boolean[deplist.length];
                    shaders = Utils.extend(shaders, deplist.length);
                    nshaders = Utils.extend(shaders, deplist.length);
                }
            }
            bufdiff(cur, next, trans, repl);
            nproghash = proghash;
            for(int i = trans.length - 1; i >= 0; i--) {
                nshaders[i] = shaders[i];
                if(repl[i] || trans[i]) {
                    GLState nst = next.states[i];
                    ShaderMacro[] ns = (nst == null)?null:nst.shaders();
                    if(ns != nshaders[i]) {
                        nproghash ^= System.identityHashCode(nshaders[i]) ^ System.identityHashCode(ns);
                        nshaders[i] = ns;
                        sdirty = true;
                    }
                }
            }
            if(sdirty || (prog == null)) {
                ShaderMacro.Program np;
                np = findprog(nproghash, nshaders);
                if(np != prog) {
                    np.apply(g);
                    prog = np;
                    if(debug)
                        checkerr(g.gl);
                    pdirty = true;
                }
            }
            cur.copy(old);
            for(int i = deplist.length - 1; i >= 0; i--) {
                int id = deplist[i].id;
                if(repl[id]) {
                    if(cur.states[id] != null) {
                        cur.states[id].unapply(g);
                        if(debug)
                            stcheckerr(g, "unapply", cur.states[id]);
                    }
                    cur.states[id] = null;
                    proghash ^= System.identityHashCode(shaders[id]);
                    shaders[id] = null;
                }
            }
	    /* Note on invariants: States may exit non-locally
	     * from apply, applyto/applyfrom or reapply (e.g. by
	     * throwing Loading exceptions) in a defined way
	     * provided they do so before they have changed any GL
	     * state. If they exit non-locally after GL state has
	     * been altered, future results are undefined. */
            for(int i = 0; i < deplist.length; i++) {
                int id = deplist[i].id;
                if(repl[id]) {
                    if(next.states[id] != null) {
                        next.states[id].apply(g);
                        cur.states[id] = next.states[id];
                        proghash ^= System.identityHashCode(shaders[id]) ^ System.identityHashCode(nshaders[id]);
                        shaders[id] = nshaders[id];
                        if(debug)
                            stcheckerr(g, "apply", cur.states[id]);
                    }
                    if(!pdirty)
                        prog.adirty(deplist[i]);
                } else if(trans[id]) {
                    cur.states[id].applyto(g, next.states[id]);
                    if(debug)
                        stcheckerr(g, "applyto", cur.states[id]);
                    next.states[id].applyfrom(g, cur.states[id]);
                    cur.states[id] = next.states[id];
                    proghash ^= System.identityHashCode(shaders[id]) ^ System.identityHashCode(nshaders[id]);
                    shaders[id] = nshaders[id];
                    if(debug)
                        stcheckerr(g, "applyfrom", cur.states[id]);
                    if(!pdirty)
                        prog.adirty(deplist[i]);
                } else if(pdirty && (shaders[id] != null)) {
                    cur.states[id].reapply(g);
                    if(debug)
                        stcheckerr(g, "reapply", cur.states[id]);
                }
            }
            if(cproj != proj) {
                matmode(g, GL2.GL_PROJECTION);
                g.gl.glLoadMatrixf((cproj = proj).m, 0);
            }
            prog.autoapply(g, pdirty);
            pdirty = sdirty = false;
            checkerr(g.gl);
            if(Config.profile)
                time += System.nanoTime() - st;
        }

        public boolean inststate(List<Buffer> instances) {
            Buffer st = GLState.inststate(cfg, instances);
            if(st == null)
                return(false);
            set(st);
            return(true);
        }

        public void bindiarr(GOut g, List<Buffer> instances) {
            for(int i = 0; i < prog.autoinst.length; i++) {
                if(prog.curinst[i] == null)
                    prog.curinst[i] = new GLBuffer(g);
                prog.autoinst[i].filliarr(g, instances, prog.curinst[i]);
                prog.autoinst[i].bindiarr(g, prog.curinst[i]);
            }
        }

        public void unbindiarr(GOut g) {
            for(int i = 0; i < prog.autoinst.length; i++)
                prog.autoinst[i].unbindiarr(g, prog.curinst[i]);
        }

        @SuppressWarnings("serial")
		public static class ApplyException extends RuntimeException {
            public final transient GLState st;
            public final String func;

            public ApplyException(String func, GLState st, Throwable cause) {
                super("Error in " + func + " of " + st, cause);
                this.st = st;
                this.func = func;
            }
        }

        private void stcheckerr(GOut g, String func, GLState st) {
            try {
                checkerr(g.gl);
            } catch(RuntimeException e) {
                throw(new ApplyException(func, st, e));
            }
        }

        /* "Meta-states" */
        private int matmode = GL2.GL_MODELVIEW;
        private int texunit = 0;

        public void matmode(GOut g, int mode) {
            if(mode != matmode) {
                g.gl.glMatrixMode(mode);
                matmode = mode;
            }
        }

        public void texunit(GOut g, int unit) {
            if(unit != texunit) {
                g.gl.glActiveTexture(GL.GL_TEXTURE0 + unit);
                texunit = unit;
            }
        }

        private TexUnit[] textab = new TexUnit[0];

        public TexUnit texalloc() {
            int i;
            for(i = 0; i < textab.length; i++) {
                if(textab[i] != null) {
                    TexUnit ret = textab[i];
                    textab[i] = null;
                    return(ret);
                }
            }
            textab = new TexUnit[i + 1];
            return(new TexUnit(this, i));
        }

        public TexUnit texalloc(GOut g, TexGL tex) {
            TexUnit ret = texalloc();
            ret.act(g);
            g.gl.glBindTexture(GL.GL_TEXTURE_2D, tex.glid(g));
            return(ret);
        }

        public TexUnit texalloc(GOut g, TexMS tex) {
            TexUnit ret = texalloc();
            ret.act(g);
            g.gl.glBindTexture(GL3.GL_TEXTURE_2D_MULTISAMPLE, tex.glid(g));
            return(ret);
        }

        /* Program internation */
        public static class SavedProg {
            public final int hash;
            public final ShaderMacro.Program prog;
            public final ShaderMacro[][] shaders;
            public SavedProg next;
            boolean used = true;

            public SavedProg(int hash, ShaderMacro.Program prog, ShaderMacro[][] shaders) {
                this.hash = hash;
                this.prog = prog;
                this.shaders = Utils.splice(shaders, 0);
            }
        }

        private SavedProg[] ptab = new SavedProg[32];
        private int nprog = 0;
        private long lastclean = System.currentTimeMillis();

        private ShaderMacro.Program findprog(int hash, ShaderMacro[][] shaders) {
            int idx = hash & (ptab.length - 1);
            outer: for(SavedProg s = ptab[idx]; s != null; s = s.next) {
                if(s.hash != hash)
                    continue;
                int i;
                for(i = 0; i < s.shaders.length; i++) {
                    if(shaders[i] != s.shaders[i])
                        continue outer;
                }
                for(; i < shaders.length; i++) {
                    if(shaders[i] != null)
                        continue outer;
                }
                s.used = true;
                return(s.prog);
            }
            Collection<ShaderMacro> mods = new LinkedList<ShaderMacro>();
            for(int i = 0; i < shaders.length; i++) {
                if(shaders[i] == null)
                    continue;
                for(int o = 0; o < shaders[i].length; o++)
                    mods.add(shaders[i][o]);
            }
            ShaderMacro.Program prog = ShaderMacro.Program.build(mods);
            SavedProg s = new SavedProg(hash, prog, shaders);
            s.next = ptab[idx];
            ptab[idx] = s;
            nprog++;
            if(nprog > ptab.length)
                rehash(ptab.length * 2);
            return(prog);
        }

        private void rehash(int nlen) {
            SavedProg[] ntab = new SavedProg[nlen];
            for(int i = 0; i < ptab.length; i++) {
                while(ptab[i] != null) {
                    SavedProg s = ptab[i];
                    ptab[i] = s.next;
                    int ni = s.hash & (ntab.length - 1);
                    s.next = ntab[ni];
                    ntab[ni] = s;
                }
            }
            ptab = ntab;
        }

        public void clean() {
            long now = System.currentTimeMillis();
            if(now - lastclean > 60000) {
                for(int i = 0; i < ptab.length; i++) {
                    SavedProg c, p;
                    for(c = ptab[i], p = null; c != null; c = c.next) {
                        if(!c.used) {
                            if(p != null)
                                p.next = c.next;
                            else
                                ptab[i] = c.next;
                            c.prog.dispose();
                            nprog--;
                        } else {
                            c.used = false;
                            p = c;
                        }
                    }
                }
		/* XXX: Rehash into smaller table? It's probably not a
		 * problem, but it might be nice just for
		 * completeness. */
                lastclean = now;
            }
        }

        public int numprogs() {
            return(nprog);
        }
    }

    public class Wrapping implements Rendered {
        public final Rendered r;

        private Wrapping(Rendered r) {
            if(r == null)
                throw(new NullPointerException("Wrapping null in " + GLState.this));
            this.r = r;
        }

        public void draw(GOut g) {}

        public boolean setup(RenderList rl) {
            rl.add(r, GLState.this);
            return(false);
        }

        public GLState st() {return(GLState.this);}
    }

    public Wrapping apply(Rendered r) {
        return(new Wrapping(r));
    }

    public static abstract class Abstract extends GLState {
        public void apply(GOut g) {}
        public void unapply(GOut g) {}
    }

    public static class Composed extends Abstract {
        public final GLState[] states;

        private static GLState[] trim(GLState[] a) {
            int i, n;
            for(i = n = 0; i < a.length; i++) {
                if(a[i] != null)
                    n++;
            }
            GLState[] b = new GLState[n];
            for(i = n = 0; i < a.length; i++) {
                if(a[i] != null)
                    b[n++] = a[i];
            }
            return(b);
        }

        public Composed(GLState... states) {
            for(GLState st : states) {
                if(st == null) {
                    this.states = trim(states);
                    return;
                }
            }
            this.states = states;
        }

        public boolean equals(Object o) {
            if(!(o instanceof Composed))
                return(false);
            return(Arrays.equals(states, ((Composed)o).states));
        }

        public int hashCode() {
            return(Arrays.hashCode(states));
        }

        public void prep(Buffer buf) {
            for(GLState st : states)
                st.prep(buf);
        }

        public String toString() {
            return("composed(" + Arrays.asList(states) + ")");
        }
    }

    public static GLState compose(GLState... states) {
        return(new Composed(states));
    }

    public static class Delegate extends Abstract {
        public GLState del;

        public Delegate(GLState del) {
            this.del = del;
        }

        public void prep(Buffer buf) {
            del.prep(buf);
        }
    }

    public interface GlobalState {
        public Global global(RenderList r, Buffer ctx);
    }

    public interface Global {
        public void postsetup(RenderList rl);
        public void prerender(RenderList rl, GOut g);
        public void postrender(RenderList rl, GOut g);
    }

    public static abstract class StandAlone extends GLState {
        public final Slot<StandAlone> slot;

        public StandAlone(Slot.Type type, Slot<?>... dep) {
            slot = new Slot<StandAlone>(type, StandAlone.class, dep);
        }

        public void prep(Buffer buf) {
            buf.put(slot, this);
        }
    }

    public static final GLState nullstate = new GLState() {
        public void apply(GOut g) {}
        public void unapply(GOut g) {}
        public void prep(Buffer buf) {}
    };

    static {
        Console.setscmd("applydb", new Console.Command() {
            public void run(Console cons, String[] args) {
                Applier.debug = Utils.parsebool(args[1], false);
            }
        });
    }
}
