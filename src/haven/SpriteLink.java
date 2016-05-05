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

import java.util.Arrays;
import java.util.Map;

import haven.Resource.Tileset;
import haven.Sprite.Factory;
import haven.Sprite.Owner;

@SuppressWarnings("serial")
@Resource.LayerName("slink")
public class SpriteLink extends Resource.Layer {
    public final Factory f;

    public static final Factory sfact = new Factory() {
        public Sprite create(Owner owner, Resource res, Message sdt) {
            SpriteLink link = res.layer(SpriteLink.class);
            if (link != null)
                return (link.f.create(owner, res, sdt));
            return (null);
        }
    };

    public static class ByTile implements Factory {
        private final String[] tag;
        private final Factory[] sub;
        private final Factory def;

        private ByTile(Resource res, Message buf, Map<Integer, Factory> refs) {
            tag = new String[buf.uint8()];
            sub = new Factory[tag.length];
            for (int i = 0; i < tag.length; i++) {
                tag[i] = buf.string();
                sub[i] = refs.get(buf.int16());
            }
            def = refs.get(buf.int16());
        }

        public Sprite create(Owner owner, Resource res, Message sdt) {
            Gob gob = (Gob) owner;
            Glob glob = gob.glob;
            Tileset t = glob.map.tileset(glob.map.gettile(new Coord(gob.getc()).div(MCache.tilesz)));
            for (int i = 0; i < tag.length; i++) {
                if (Arrays.binarySearch(t.tags, tag[i]) >= 0)
                    return (sub[i].create(owner, res, sdt));
            }
            return ((def == null) ? null : def.create(owner, res, sdt));
        }
    }

    public static class ByRes implements Factory {
        private final Indir<Resource> res;

        private ByRes(Resource res, Message buf, Map<Integer, Factory> refs) {
            String resnm = buf.string();
            int resver = buf.uint16();
            this.res = res.pool.load(resnm, resver);
        }

        public Sprite create(Owner owner, Resource res, Message sdt) {
            return (Sprite.create(owner, this.res.get(), sdt));
        }

        public String toString() {
            return (res.toString());
        }
    }

    public SpriteLink(Resource res, Message buf) {
        res.super();
        int ver = buf.uint8();
        if (ver != 1)
            throw (new Resource.LoadException("Unknown spritelink version: " + ver, getres()));
        Map<Integer, Factory> refs = new IntMap<Factory>(16);
        while (true) {
            int id = buf.int16();
            if (id < 0)
                break;
            char t = (char) buf.uint8();
            Factory f;
            switch (t) {
                case 't':
                    f = new ByTile(res, buf, refs);
                    break;
                case 'r':
                    f = new ByRes(res, buf, refs);
                    break;
                default:
                    throw (new Resource.LoadException("Unknown spritelink type: `" + t + "'", getres()));
            }
            refs.put(id, f);
        }
        this.f = refs.get(buf.int16());
    }

    public void init() {
    }
}
