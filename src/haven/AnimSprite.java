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

import java.util.*;

public class AnimSprite extends Sprite {
    private Rendered[] parts;
    private MeshAnim.Anim[] anims;

    public static final Factory fact = new Factory() {
        public Sprite create(Owner owner, Resource res, Message sdt) {
            if (res.layer(MeshAnim.Res.class) == null || Config.disableanimSet.contains(res.name))
                return (null);
            return (new AnimSprite(owner, res, sdt));
        }
    };

    private AnimSprite(Owner owner, Resource res, Message sdt) {
        super(owner, res);
        int mask = sdt.eom() ? 0xffff0000 : decnum(sdt);
        Collection<MeshAnim.Anim> anims = new LinkedList<MeshAnim.Anim>();
        for (MeshAnim.Res ar : res.layers(MeshAnim.Res.class)) {
            if ((ar.id < 0) || (((1 << ar.id) & mask) != 0))
                anims.add(ar.make());
        }
        this.anims = anims.toArray(new MeshAnim.Anim[0]);
        MorphedMesh.Morpher.Factory morph = MorphedMesh.combine(this.anims);
        Collection<Rendered> rl = new LinkedList<Rendered>();
        for (FastMesh.MeshRes mr : res.layers(FastMesh.MeshRes.class)) {
            if ((mr.mat != null) && ((mr.id < 0) || (((1 << mr.id) & mask) != 0))) {
                boolean stat = true;
                for (MeshAnim.Anim anim : anims) {
                    if (anim.desc().animp(mr.m)) {
                        stat = false;
                        break;
                    }
                }
                if (stat)
                    rl.add(mr.mat.get().apply(mr.m));
                else
                    rl.add(mr.mat.get().apply(new MorphedMesh(mr.m, morph)));
            }
        }
        parts = rl.toArray(new Rendered[0]);
    }

    public boolean setup(RenderList rl) {
        for (Rendered p : parts)
            rl.add(p, null);
        return (false);
    }

    public boolean tick(int idt) {
        boolean ret = false;
        float dt = idt / 1000.0f;
        for (MeshAnim.Anim anim : anims)
            ret = ret | anim.tick(dt);
        return (ret);
    }
}
