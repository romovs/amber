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

import haven.*;

import java.util.*;
import java.awt.Color;
import java.awt.image.*;

public class FoodInfo extends ItemInfo.Tip {
    public final double end, glut;
    public final Event[] evs;
    public final Effect[] efs;
    public final int[] types;

    public FoodInfo(Owner owner, double end, double glut, Event[] evs, Effect[] efs, int[] types) {
        super(owner);
        this.end = end;
        this.glut = glut;
        this.evs = evs;
        this.efs = efs;
        this.types = types;
    }

    public static class Event {
        public final CharWnd.FoodMeter.Event ev;
        public final BufferedImage img;
        public final double a;

        public Event(Resource res, double a) {
            this.ev = res.layer(CharWnd.FoodMeter.Event.class);
            this.img = res.layer(Resource.imgc).img;
            this.a = a;
        }
    }

    public static class Effect {
        public final List<ItemInfo> info;
        public final double p;

        public Effect(List<ItemInfo> info, double p) {
            this.info = info;
            this.p = p;
        }
    }

    public BufferedImage longtip() {
        String infoStr = Resource.getLocString(Resource.l10nLabel, "Energy: $col[128,128,255]{%s%%}, Hunger: $col[255,192,128]{%s%%}");
        String chanceStr = Resource.getLocString(Resource.l10nLabel, "$i{($col[192,192,255]{%d%%} chance)}");

        BufferedImage base = RichText.render(String.format(infoStr, Utils.odformat2(end * 100, 2), Utils.odformat2(glut * 100, 2)), 0).img;
        Collection<BufferedImage> imgs = new LinkedList<BufferedImage>();
        imgs.add(base);
        for (int i = 0; i < evs.length; i++) {
            Color col = Utils.blendcol(evs[i].ev.col, Color.WHITE, 0.5);
            imgs.add(catimgsh(5, evs[i].img, RichText.render(String.format("%s: $col[%d,%d,%d]{%s}", evs[i].ev.nm, col.getRed(), col.getGreen(), col.getBlue(), Utils.odformat2(evs[i].a, 2)), 0).img));
        }
        for (int i = 0; i < efs.length; i++) {
            BufferedImage efi = ItemInfo.longtip(efs[i].info);
            if (efs[i].p != 1)
                efi = catimgsh(5, efi, RichText.render(String.format(chanceStr, (int) Math.round(efs[i].p * 100)), 0).img);
            imgs.add(efi);
        }
        return (catimgs(0, imgs.toArray(new BufferedImage[0])));
    }
}
