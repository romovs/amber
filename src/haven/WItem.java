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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.*;

import static haven.Inventory.sqsz;

public class WItem extends Widget implements DTarget {
    public static final Resource missing = Resource.local().loadwait("gfx/invobjs/missing");
    private static final Resource studyalarmsfx = Resource.local().loadwait("sfx/study");
    public final GItem item;
    private Resource cspr = null;
    private Message csdt = Message.nil;

    public WItem(GItem item) {
        super(sqsz);
        this.item = item;
    }

    public void drawmain(GOut g, GSprite spr) {
        spr.draw(g);
    }

    public static BufferedImage shorttip(List<ItemInfo> info) {
        return (ItemInfo.shorttip(info));
    }

    public static BufferedImage longtip(GItem item, List<ItemInfo> info) {
        BufferedImage img = ItemInfo.longtip(info);
        Resource.Pagina pg = item.res.get().layer(Resource.pagina);
        if (pg != null)
            img = ItemInfo.catimgs(0, img, RichText.render("\n" + pg.text, 200).img);
        return (img);
    }

    public BufferedImage longtip(List<ItemInfo> info) {
        return (longtip(item, info));
    }

    public class ItemTip implements Indir<Tex> {
        private final TexI tex;

        public ItemTip(BufferedImage img) {
            if (img == null)
                throw (new Loading());
            tex = new TexI(img);
        }

        public GItem item() {
            return (item);
        }

        public Tex get() {
            return (tex);
        }
    }

    public class ShortTip extends ItemTip {
        public ShortTip(List<ItemInfo> info) {
            super(shorttip(info));
        }
    }

    public class LongTip extends ItemTip {
        public LongTip(List<ItemInfo> info) {
            super(longtip(info));
        }
    }

    private long hoverstart;
    private ItemTip shorttip = null, longtip = null;
    private List<ItemInfo> ttinfo = null;

    public Object tooltip(Coord c, Widget prev) {
        long now = System.currentTimeMillis();
        if (prev == this) {
        } else if (prev instanceof WItem) {
            long ps = ((WItem) prev).hoverstart;
            if (now - ps < 1000)
                hoverstart = now;
            else
                hoverstart = ps;
        } else {
            hoverstart = now;
        }
        try {
            List<ItemInfo> info = item.info();
            if (info.size() < 1)
                return (null);
            if (info != ttinfo) {
                shorttip = longtip = null;
                ttinfo = info;
            }
            if (now - hoverstart < 1000) {
                if (shorttip == null)
                    shorttip = new ShortTip(info);
                return (shorttip);
            } else {
                if (longtip == null)
                    longtip = new LongTip(info);
                return (longtip);
            }
        } catch (Loading e) {
            return ("...");
        }
    }

    public abstract class AttrCache<T> {
        private List<ItemInfo> forinfo = null;
        private T save = null;

        public T get() {
            try {
                List<ItemInfo> info = item.info();
                if (info != forinfo) {
                    save = find(info);
                    forinfo = info;
                }
            } catch (Loading e) {
                return (null);
            }
            return (save);
        }

        protected abstract T find(List<ItemInfo> info);
    }

    public final AttrCache<Color> olcol = new AttrCache<Color>() {
        protected Color find(List<ItemInfo> info) {
            GItem.ColorInfo cinf = ItemInfo.find(GItem.ColorInfo.class, info);
            return ((cinf == null) ? null : cinf.olcol());
        }
    };

    public final AttrCache<Tex> itemnum = new AttrCache<Tex>() {
        protected Tex find(List<ItemInfo> info) {
            GItem.NumberInfo ninf = ItemInfo.find(GItem.NumberInfo.class, info);
            if (ninf == null) return (null);
            return (new TexI(Utils.outline2(Text.render(Integer.toString(ninf.itemnum()), Color.WHITE).img, Utils.contrast(Color.WHITE))));
        }
    };

    private GSprite lspr = null;

    public void tick(double dt) {
    /* XXX: This is ugly and there should be a better way to
     * ensure the resizing happens as it should, but I can't think
	 * of one yet. */
        GSprite spr = item.spr();
        if ((spr != null) && (spr != lspr)) {
            Coord sz = new Coord(spr.sz());
            if ((sz.x % sqsz.x) != 0)
                sz.x = sqsz.x * ((sz.x / sqsz.x) + 1);
            if ((sz.y % sqsz.y) != 0)
                sz.y = sqsz.y * ((sz.y / sqsz.y) + 1);
            resize(sz);
            lspr = spr;
        }
    }

    public void draw(GOut g) {
        GSprite spr = item.spr();
        if (spr != null) {
            Coord sz = spr.sz();
            g.defstate();
            if (olcol.get() != null)
                g.usestate(new ColorMask(olcol.get()));
            drawmain(g, spr);
            g.defstate();
            if (item.num >= 0) {
                g.atext(Integer.toString(item.num), sz, 1, 1);
            } else if (itemnum.get() != null) {
                g.aimage(itemnum.get(), sz, 1, 1);
            }
            if (item.meter > 0) {
                if (Config.itemmeterbar) {
                    g.chcolor(220, 60, 60, 255);
                    g.frect(Coord.z, new Coord((int) (sz.x / (100 / (double) item.meter)), 4));
                    g.chcolor();
                } else if (!Config.itempercentage){
                    double a = ((double) item.meter) / 100.0;
                    g.chcolor(255, 255, 255, 64);
                    Coord half = sz.div(2);
                    g.prect(half, half.inv(), half, a * Math.PI * 2);
                    g.chcolor();
                }
            }

            GItem.Quality quality = item.quality();
            if (Config.showquality) {
                if (quality != null && quality.max != 0) {
                    if (Config.showqualitymode == 2) {
                        g.image(quality.etex, new Coord(0, sz.y - 32));
                        g.image(quality.stex, new Coord(0, sz.y - 22));
                        g.image(quality.vtex, new Coord(0, sz.y - 12));
                    } else if (quality.curio && Config.showlpgainmult) {
                        g.image(quality.lpgaintex, new Coord(0, sz.y - 12));
                    } else if (Config.showqualitymode == 0) {
                        g.image(quality.maxtex, new Coord(0, sz.y - 12));
                    } else if (Config.showqualitymode == 1) {
                        g.image(Config.qualitywhole ? quality.avgwholetex : quality.avgtex, new Coord(0, sz.y - 12));
                    } else {
                        g.image(Config.qualitywhole ? quality.avgsvwholetex : quality.avgsvtex, new Coord(0, sz.y - 12));
                    }
                }
            }

            boolean studylefttimedisplayed = false;
            if (Config.showstudylefttime && quality != null && quality.curio && item.meter > 0) {
                if (item.timelefttex == null) {
                    item.updatetimelefttex();
                }

                if (item.timelefttex != null) {
                    g.image(item.timelefttex, Coord.z);
                    studylefttimedisplayed = true;
                }
            }

            if (!studylefttimedisplayed && item.meter > 0 && Config.itempercentage && item.metertex != null) {
                g.image(item.metertex, Coord.z);
            }
        } else {
            g.image(missing.layer(Resource.imgc).tex(), Coord.z, sz);
        }
    }

    public boolean mousedown(Coord c, int btn) {
        if (btn == 1) {
            if (ui.modctrl && ui.modmeta)
                wdgmsg("drop-identical", this.item);
            else if (ui.modshift && ui.modmeta) {
                wdgmsg("transfer-identical", this.item);
            }
            else if (ui.modshift)
                item.wdgmsg("transfer", c);
            else if (ui.modctrl)
                item.wdgmsg("drop", c);
            else
                item.wdgmsg("take", c);
            return (true);
        } else if (btn == 3) {
            if (ui.modmeta)
                wdgmsg("transfer-identical", this.item);
            else
                item.wdgmsg("iact", c, ui.modflags());
            return (true);
        }
        return (false);
    }

    public boolean drop(Coord cc, Coord ul) {
        return (false);
    }

    public boolean iteminteract(Coord cc, Coord ul) {
        item.wdgmsg("itemact", ui.modflags());
        return (true);
    }

    public void destroy() {
        super.destroy();
        Curiosity ci = null;
        try {
            ci = ItemInfo.find(Curiosity.class, item.info());
            if (ci != null && item.meter >= 99) {
                Resource.Tooltip tt = item.resource().layer(Resource.Tooltip.class);
                if (tt != null)
                    gameui().syslog.append(tt.t + " LP: " + ci.exp, Color.LIGHT_GRAY);

                if (Config.autostudy) {
                    Window invwnd = gameui().getwnd("Inventory");
                    Resource res = item.resource();
                    if (res != null) {
                        for (Widget invwdg = invwnd.lchild; invwdg != null; invwdg = invwdg.prev) {
                            if (invwdg instanceof Inventory) {
                                Inventory inv = (Inventory) invwdg;
                                for (Widget witm = inv.lchild; witm != null; witm = witm.prev) {
                                    if (witm instanceof WItem) {
                                        GItem ngitm = ((WItem) witm).item;
                                        Resource nres = ngitm.resource();
                                        if (nres != null && nres.name.equals(res.name)) {
                                            ngitm.wdgmsg("take", witm.c);
                                            ((Inventory) parent).drop(Coord.z, c);
                                            break;
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Loading l) {
        }

        if (Config.studyalarm && ci != null && item.meter >= 99)
            Audio.play(studyalarmsfx, Config.studyalarmvol);
    }
}
