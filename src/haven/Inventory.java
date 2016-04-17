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

public class Inventory extends Widget implements DTarget {
    public static final Tex invsq = Resource.loadtex("gfx/hud/invsq");
    public static final Coord sqsz = new Coord(33, 33);
    Coord isz;
    Map<GItem, WItem> wmap = new HashMap<GItem, WItem>();

    @RName("inv")
    public static class $_ implements Factory {
        public Widget create(Widget parent, Object[] args) {
            return (new Inventory((Coord) args[0]));
        }
    }

    public void draw(GOut g) {
        Coord c = new Coord();
        for (c.y = 0; c.y < isz.y; c.y++) {
            for (c.x = 0; c.x < isz.x; c.x++) {
                g.image(invsq, c.mul(sqsz));
            }
        }
        super.draw(g);
    }

    public Inventory(Coord sz) {
        super(invsq.sz().add(new Coord(-1, -1)).mul(sz).add(new Coord(1, 1)));
        isz = sz;
    }

    public boolean mousewheel(Coord c, int amount) {
        if (ui.modshift) {
            Inventory minv = getparent(GameUI.class).maininv;
            if (minv != this) {
                if (amount < 0)
                    wdgmsg("invxf", minv.wdgid(), 1);
                else if (amount > 0)
                    minv.wdgmsg("invxf", this.wdgid(), 1);
            }
        }
        return (true);
    }

    public void addchild(Widget child, Object... args) {
        add(child);
        Coord c = (Coord) args[0];
        if (child instanceof GItem) {
            GItem i = (GItem) child;
            wmap.put(i, add(new WItem(i), c.mul(sqsz).add(1, 1)));
        }
    }

    public void cdestroy(Widget w) {
        super.cdestroy(w);
        if (w instanceof GItem) {
            GItem i = (GItem) w;
            ui.destroy(wmap.remove(i));
        }
    }

    public boolean drop(Coord cc, Coord ul) {
        wdgmsg("drop", ul.add(sqsz.div(2)).div(invsq.sz()));
        return (true);
    }

    public boolean iteminteract(Coord cc, Coord ul) {
        return (false);
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "sz") {
            isz = (Coord) args[0];
            resize(invsq.sz().add(new Coord(-1, -1)).mul(isz).add(new Coord(1, 1)));
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if(msg.equals("drop-identical")) {
            for (WItem item : getIdenticalItems((GItem) args[0]))
                item.item.wdgmsg("drop", Coord.z);
        } else if(msg.startsWith("transfer-identical")) {
            Window stockpile = gameui().getwnd("Stockpile");
            Window smelter = gameui().getwnd("Ore Smelter");
            Window kiln = gameui().getwnd("Kiln");
            if (stockpile == null || smelter != null || kiln != null) {
                List<WItem> items = getIdenticalItems((GItem) args[0]);
                Collections.sort(items, new Comparator<WItem>() {
                    public int compare(WItem a, WItem b) {
                        GItem.Quality aq = a.item.quality();
                        GItem.Quality bq = b.item.quality();
                        if (aq == null || bq == null)
                            return 0;
                        else if (aq.avg == bq.avg)
                            return 0;
                        else if (aq.avg > bq.avg)
                            return msg.endsWith("asc") ? 1 : -1;
                        else
                            return msg.endsWith("asc") ? -1 : 1;
                    }
                });
                for (WItem item : items)
                    item.item.wdgmsg("transfer", Coord.z);
            } else {
                for (Widget w = stockpile.lchild; w != null; w = w.prev) {
                    if (w instanceof ISBox) {
                        ISBox isb = (ISBox) w;
                        int freespace = isb.getfreespace();
                        for (WItem item : getIdenticalItems((GItem) args[0])) {
                            if (freespace-- <= 0)
                                break;
                            item.item.wdgmsg("take", new Coord(item.sz.x / 2, item.sz.y / 2));
                            isb.drop(null, null);
                        }
                        break;
                    }
                }
            }
        } else if (msg.equals("transfer")) {
            Window stockpile = gameui().getwnd("Stockpile");
            if (stockpile == null) {
                super.wdgmsg(sender, msg, args);
                return;
            }

            for (Widget w = stockpile.lchild; w != null; w = w.prev) {
                if (w instanceof ISBox) {
                    ISBox isb = (ISBox) w;
                    if (isb.getfreespace() <= 0)
                        return;
                    GItem gitem = (GItem)sender;
                    gitem.wdgmsg("take", args[0]);
                    isb.drop(null, null);
                    break;
                }
            }
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public List<WItem> getIdenticalItems(GItem item) {
        List<WItem> items = new ArrayList<WItem>();
        String name = item.spr().getname();
        String resname = item.resource().name;
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
                if (wdg instanceof WItem) {
                    String oname = ((WItem) wdg).item.spr().getname();
                    if (((WItem)wdg).item.resource().name.equals(resname) &&
                            (name == null || name != null && name.equals(oname)))
                        items.add((WItem)wdg);
                }
        }
        return items;
    }

    /* Following getItem* methods do partial matching of the name *on purpose*.
       Because when localization is turned on, original English name will be in the brackets
       next to the translation
    */
    public List<WItem> getItemsPartial(String... names) {
        List<WItem> items = new ArrayList<WItem>();
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                String wdgname = ((WItem)wdg).item.getname();
                for (String name : names) {
                    if (wdgname.contains(name)) {
                        items.add((WItem) wdg);
                        break;
                    }
                }
            }
        }
        return items;
    }

    public WItem getItemPartial(String name) {
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                String wdgname = ((WItem)wdg).item.getname();
                if (wdgname.contains(name))
                    return (WItem) wdg;
            }
        }
        return null;
    }

    public int getItemPartialCount(String name) {
        int count = 0;
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                String wdgname = ((WItem)wdg).item.getname();
                if (wdgname.contains(name))
                    count++;
            }
        }
        return count;
    }

    public int getFreeSpace() {
        int feespace = isz.x * isz.y;
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem)
                feespace -= (wdg.sz.x * wdg.sz.y) / (sqsz.x * sqsz.y);
        }
        return feespace;
    }

    public void drinkTillFull(int threshold) {
        GameUI gui = gameui();
        if (gameui().maininv.drink(threshold)) {
            try {
                Thread.sleep(500);
                do {
                    IMeter.Meter stam = gui.getmeter("stam", 0);
                    if (stam.a >= 90)
                        break;
                    Thread.sleep(10);
                    stam = gui.getmeter("stam", 0);
                    if (stam.a >= 90)
                        break;
                } while (gui.prog >= 0);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    public boolean drink(int threshold) {
        IMeter.Meter stam = gameui().getmeter("stam", 0);
        if (stam == null || stam.a > threshold)
            return false;

        List<WItem> containers = getItemsPartial("Waterskin", "Waterflask");

        // find hotkeyed water container
        WItem hotwater = null;
        for (WItem w : containers) {
            if (w.olcol != null && w.olcol.save != null) {
                hotwater = w;
                break;
            }
        }
        if (hotwater == null)
            return false;

        // find any additional containers and refill the hotkeyed one
        for (WItem w : containers) {
            if (w.olcol != null && w.olcol.save == null) {
                // break if full
                ItemInfo.Contents hotcnt = hotwater.item.getcontents();
                if (hotcnt != null) {
                    String name = hotwater.item.getname();
                    double fullcont = name.contains("Waterskin") ? 3.0D : 2.0D;
                    if (hotcnt.content == fullcont)
                        break;
                }

                ItemInfo.Contents cnt = w.item.getcontents();
                if (cnt != null && cnt.content > 0) {
                    w.item.wdgmsg("take", new Coord(w.item.sz.x / 2, w.item.sz.y / 2));
                    hotwater.item.wdgmsg("itemact", 0);
                    wdgmsg("drop", w.c.add(sqsz.div(2)).div(invsq.sz()));
                }
            }
        }

        // drink
        GameUI.Belt beltwdg = gameui().beltwdg;
        Indir<Resource>[] belt = gameui().belt;
        for (int s = 0; s < belt.length; s++) {
            Indir<Resource> indir = belt[s];
            if (indir != null) {
                try {
                    Resource res = indir.get();
                    if (res != null && (res.basename().equals("waterskin") || res.basename().equals("waterflask")))
                        beltwdg.keyact(s);
                } catch (Loading l) {
                }
            }
        }
        return true;
    }
}
