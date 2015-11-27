package haven;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AreaClayDig implements Runnable {
    private MapView mv;
    private Coord a, b, c, d;
    private boolean terminate = false;
    private static final Set<String> diggingTools = new HashSet<String>(Arrays.asList(
            "Metal Shovel", "Wooden Shovel"));

    public AreaClayDig(Coord a, Coord b, MapView mv) {
        this.a = a;
        this.b = b;
        this.d = new Coord(a.x, b.y);
        this.c = new Coord(b.x, a.y);
        this.mv = mv;
    }

    public void run() {
        MCache map = mv.ui.sess.glob.map;
        MCache.Overlay ol = mv.glob.map.new Overlay(a, b, 1 << 18);
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

        dig:
        for (int i = 0; i < path.length; i++) {
            if (terminate)
                break dig;

            // drink
            GameUI gui = HavenPanel.lui.root.findchild(GameUI.class);
            if (gui.maininv != null) {
                if (gui.maininv.drink(80)) {
                    try {
                        Thread.sleep(1000);
                        do Thread.sleep(300); while (gui.prog >= 0);
                    } catch (InterruptedException e) {
                        break dig;
                    }
                }
            }

            Coord tc = path[i];
            int t = map.gettile(tc);
            Resource res = map.tilesetr(t);
            if (res == null || !res.name.startsWith("gfx/tiles/water"))
                break dig;

            // stop if energy < 1500%
            IMeter.Meter nrj = gui.getmeter("nrj", 0);
            if (nrj.a < 30)
                break dig;

            mv.wdgmsg("click", Coord.z, tc.mul(11), 1, 0);

            for(int j = 0; j < 20; j++) {
                try{
                    if(gui.maininv.spaceLeft() == 0){
                        gui.error("No space left in inventory");
                        break dig;
                    }
                    Coord initPos = gui.map.player().rc;
                    Thread.sleep(50);
                    if(gui.prog >= 0 || gui.map.player().getv()!=0){
                        j = 0;
                    }
                } catch (InterruptedException e) {
                    break dig;
                }

                if (terminate)
                    break dig;

                // check if digging tool is equipped
                Equipory e = gui.getequipory();
                WItem l = e.quickslots[6];
                WItem r = e.quickslots[7];
                boolean notool = true;
                if (l != null && diggingTools.contains(l.item.getname()))
                    notool = false;
                if (r != null && diggingTools.contains(r.item.getname()))
                    notool = false;

                if (notool)
                    break dig;

                // otherwise if we are out of stamina - repeat
                IMeter.Meter stam = gui.getmeter("stam", 0);
                if (stam.a <= 30) {
                    i--;
                    break;
                }
            }
        }

        mv.disol(18);
        ol.destroy();
    }

    public void terminate() {
        terminate = true;
    }
}

