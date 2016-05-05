package haven;


public class AreaMine implements Runnable {
    private MapView mv;
    private Coord a, b, c, d;
    private boolean terminate = false;
    private static final String[] miningtools = new String[] {
            "Pickaxe", "Stone Axe", "Metal Axe", "Battleaxe of the Twelfth Bay"};

    public AreaMine(Coord a, Coord b, MapView mv) {
        this.a = a;
        this.b = b;
        this.d = new Coord(a.x, b.y);
        this.c = new Coord(b.x, a.y);
        this.mv = mv;
    }

    public void run() {
        MCache map = mv.glob.map;

        MCache.Overlay ol;
        synchronized (map.grids) {
            ol = mv.glob.map.new Overlay(a, b, 1 << 18);
        }
        mv.enol(18);

        Coord pc = mv.player().rc.div(11);

        // mining direction
        boolean xaxis = pc.y <= a.y && pc.y >= b.y || pc.y <= b.y && pc.y >= a.y;

        // closest corner
        Coord cls = a;
        double clsdist = a.dist(pc);
        if (b.dist(pc) < clsdist) {
            cls = b;
            clsdist = b.dist(pc);
        }
        if (d.dist(pc) < clsdist) {
            cls = d;
            clsdist = d.dist(pc);
        }
        if (c.dist(pc) < clsdist) {
            cls = c;
        }

        // opposite corner
        Coord opp = cls;
        if (a.x == b.x || a.y == b.y) {     // 1xN area
            if (a.x != opp.x || a.y != opp.y)
                opp = a;
            else if (b.x != opp.x || b.y != opp.y)
                opp = b;
        } else {                            // MxN area
            if (a.x != opp.x && a.y != opp.y)
                opp = a;
            else if (b.x != opp.x && b.y != opp.y)
                opp = b;
            else if (d.x != opp.x && d.y != opp.y)
                opp = d;
            else if (c.x != opp.x && c.y != opp.y)
                opp = c;
        }

        int xstep = opp.x > cls.x ? 1 : -1;
        int ystep = opp.y > cls.y ? 1 : -1;

        int h = Math.abs(cls.y - opp.y);
        int w = Math.abs(cls.x - opp.x);

        Coord[] path = new Coord[(w + 1) * (h + 1)];

        int pi = 0;
        if (xaxis) {    // mine along x axis
            for (int i = 0; i <= w && i >= 0 - w; i += xstep) {
                int revi = 0;
                for (int j = 0; j <= h && j >= 0 - h; j += ystep) {
                    // reverse current segment's coordinates order every other transition
                    path[i % 2 != 0 && i != 0 ? pi + h - revi : pi] = new Coord(cls.x + i, cls.y + j);
                    pi++;
                    revi += 2;
                }
            }
        } else {        // mine along y axis
            for (int j = 0; j <= h && j >= 0 - h; j += ystep) {
                int revi = 0;
                for (int i = 0; i <= w && i >= 0 - w; i += xstep) {
                    // reverse current segment's coordinates order every other transition
                    path[j % 2 != 0 && j != 0 ? pi + w - revi : pi] = new Coord(cls.x + i, cls.y + j);
                    pi++;
                    revi += 2;
                }
            }
        }

        mine:
        for (int i = 0; i < path.length; i++) {
            if (terminate)
                break mine;

            GameUI gui = HavenPanel.lui.root.findchild(GameUI.class);
            try {
                haven.automation.Utils.drinkTillFull(gui, 70, 84);
            } catch (InterruptedException e) {
                break mine;
            }

            Coord tc = path[i];
            int t = map.gettile(tc);
            Resource res = map.tilesetr(t);
            if (res == null || (!res.name.startsWith("gfx/tiles/rocks/") && !res.name.equals("gfx/tiles/cave")))
                continue;

            // stop if energy < 1500%
            IMeter.Meter nrj = gui.getmeter("nrj", 0);
            if (nrj.a < 30)
                break mine;

            if (terminate)
                break mine;

            // discard mining cursor so we could move
            mv.wdgmsg("click", Coord.z, tc.mul(11).add(5, 5), 3, 0);

            mv.pfLeftClick(tc.mul(11).add(5, 5), "mine");

            try {
                mv.pfthread.join();
            } catch (InterruptedException e) {
                break mine;
            }

            while (!terminate) {
                if (!Config.dropore && gui.maininv.getFreeSpace() == 0) {
                    if (gui.vhand != null)
                        mv.wdgmsg("drop", Coord.z, gui.map.player().rc, 0);

                    mv.wdgmsg("click", Coord.z, tc.mul(11).add(5, 5), 3, 0);
                    mv.wdgmsg("click", Coord.z, gui.map.player().rc, 1, 0);
                    break mine;
                }

                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    break mine;
                }

                if (terminate)
                    break mine;

                // check if tile is done
                t = map.gettile(tc);
                res = map.tilesetr(t);
                if (res != null) {
                    if (res.name.equals("gfx/tiles/mine"))
                        break;
                }

                // check if mining tool is equipped
                Equipory e = gui.getequipory();
                WItem l = e.quickslots[6];
                WItem r = e.quickslots[7];
                boolean notool = true;

                if (l != null) {
                    String lname = l.item.getname();
                    for (String tool : miningtools) {
                        if (lname.contains(tool)){
                            notool = false;
                            break;
                        }
                    }
                }
                if (r != null) {
                    String rname = r.item.getname();
                    for (String tool : miningtools) {
                        if (rname.contains(tool)){
                            notool = false;
                            break;
                        }
                    }
                }

                if (notool)
                    break mine;

                // otherwise if we are out of stamina - repeat
                IMeter.Meter stam = gui.getmeter("stam", 0);
                if (stam.a <= 30) {
                    i--;
                    break;
                }

                if (!Config.dropore && gui.maininv.getFreeSpace() == 0) {
                    if (gui.vhand != null)
                        mv.wdgmsg("drop", Coord.z, gui.map.player().rc, 0);

                    mv.wdgmsg("click", Coord.z, tc.mul(11).add(5, 5), 3, 0);
                    mv.wdgmsg("click", Coord.z, gui.map.player().rc, 1, 0);
                    break mine;
                }
            }
        }

        mv.disol(18);
        synchronized (map.grids) {
            ol.destroy();
        }
    }

    public void terminate() {
        terminate = true;
    }
}