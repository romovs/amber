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

package haven.resutil;

import java.awt.Color;
import java.util.Random;

import haven.Coord;
import haven.Coord3f;
import haven.GLState;
import haven.Light;
import haven.MapMesh;
import haven.Material;
import haven.MeshBuf;
import haven.Resource;
import haven.Resource.Tile;
import haven.Surface;
import haven.Surface.MeshVertex;
import haven.Tex;
import haven.TexGL;
import haven.TexSI;
import haven.Tiler;

public class GroundTile extends Tiler implements Tiler.MCons, Tiler.CTrans {
    private static final Material.Colors gcol = new Material.Colors(new Color(128, 128, 128), new Color(255, 255, 255), new Color(0, 0, 0), new Color(0, 0, 0));
    public final Resource.Tileset set;

    @ResName("gnd")
    public static class Fac implements Factory {
        public Tiler create(int id, Resource.Tileset set) {
            return (new GroundTile(id, set));
        }
    }

    public GroundTile(int id, Resource.Tileset set) {
        super(id);
        this.set = set;
    }

    private static GLState stfor(Tex tex, int z, boolean clip) {
        TexGL gt;
        if (tex instanceof TexGL)
            gt = (TexGL) tex;
        else if ((tex instanceof TexSI) && (((TexSI) tex).parent instanceof TexGL))
            gt = (TexGL) ((TexSI) tex).parent;
        else
            throw (new RuntimeException("Cannot use texture for ground-tile rendering: " + tex));
        GLState ret;
        if (clip)
            ret = GLState.compose(Light.deflight, gcol, gt.draw(), gt.clip(), new MapMesh.MLOrder(z));
        else
            ret = GLState.compose(Light.deflight, gcol, gt.draw(), new MapMesh.MLOrder(z));
        return (ret);
    }

    /* XXX: Some strange javac bug seems to make it resolve the
     * trans() references to the wrong signature, thus the name
     * distinction. */
    public void _faces(MapMesh m, Tile t, int z, Surface.Vertex[] v, float[] tcx, float[] tcy, int[] f) {
        Tex tex = t.tex();
        float tl = tex.tcx(0), tt = tex.tcy(0), tw = tex.tcx(tex.sz().x) - tl, th = tex.tcy(tex.sz().y) - tt;
        GLState st = stfor(tex, z, t.t != 'g');
        MeshBuf buf = MapMesh.Model.get(m, st);

        MeshBuf.Tex btex = buf.layer(MeshBuf.tex);
        MeshVertex[] mv = new MeshVertex[v.length];
        for (int i = 0; i < v.length; i++) {
            mv[i] = new MeshVertex(buf, v[i]);
            btex.set(mv[i], new Coord3f(tl + (tw * tcx[i]), tt + (th * tcy[i]), 0));
        }
        for (int i = 0; i < f.length; i += 3)
            buf.new Face(mv[f[i]], mv[f[i + 1]], mv[f[i + 2]]);
    }

    public void faces(MapMesh m, MPart d) {
        _faces(m, set.ground.pick(m.rnd(d.lc)), 0, d.v, d.tcx, d.tcy, d.f);
    }

    public void lay(MapMesh m, Random rnd, Coord lc, Coord gc) {
        lay(m, lc, gc, this, false);
    }

    private MCons tcons(final int z, final Tile t) {
        return (new MCons() {
            public void faces(MapMesh m, MPart d) {
                _faces(m, t, z, d.v, d.tcx, d.tcy, d.f);
            }
        });
    }

    public MCons tcons(final int z, final int bmask, final int cmask) {
        if ((bmask == 0) && (cmask == 0))
            return (MCons.nil);
        return (new MCons() {
            public void faces(MapMesh m, MPart d) {
                Random rnd = m.rnd(d.lc);
                if ((set.btrans != null) && (bmask != 0))
                    tcons(z, set.btrans[bmask - 1].pick(rnd)).faces(m, d);
                if ((set.ctrans != null) && (cmask != 0))
                    tcons(z, set.ctrans[cmask - 1].pick(rnd)).faces(m, d);
            }
        });
    }

    public void trans(MapMesh m, Random rnd, Tiler gt, Coord lc, Coord gc, int z, int bmask, int cmask) {
        if (m.map.gettile(gc) <= id)
            return;
        if ((set.btrans != null) && (bmask > 0))
            gt.lay(m, lc, gc, tcons(z, set.btrans[bmask - 1].pick(rnd)), false);
        if ((set.ctrans != null) && (cmask > 0))
            gt.lay(m, lc, gc, tcons(z, set.ctrans[cmask - 1].pick(rnd)), false);
    }
}
