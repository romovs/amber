package haven.automation;


import haven.*;

public class LightWithTorch implements Runnable {
    private GameUI gui;
    private Gob gob;
    private static final int TIMEOUT_ACT = 3000;

    public LightWithTorch(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                Resource res = gob.getres();
                if (res != null &&
                        (res.name.equals("gfx/terobjs/oven") ||
                        res.name.equals("gfx/terobjs/smelter") ||
                        res.name.equals("gfx/terobjs/steelcrucible") ||
                        res.name.equals("gfx/terobjs/kiln"))) {
                    if (this.gob == null)
                        this.gob = gob;
                    else if (gob.rc.dist(gui.map.player().rc) < this.gob.rc.dist(gui.map.player().rc))
                        this.gob = gob;
                }
            }
        }

        try {
            if (gob == null) {
                gui.error("No ovens/smelters/steelboxes/kilns found.");
                return;
            }

            Equipory e = gui.getequipory();
            WItem l = e.quickslots[6];
            WItem r = e.quickslots[7];

            boolean noltorch = true;
            boolean nortorch = true;

            if (l != null) {
                String lname = l.item.getname();
                if (lname.contains("Lit Torch"))
                    noltorch = false;
            }
            if (r != null) {
                String rname = r.item.getname();
                if (rname.contains("Lit Torch"))
                    nortorch = false;
            }

            if (noltorch && nortorch) {
                gui.error("No lit torch is equipped.");
                return;
            }

            WItem w = e.quickslots[noltorch ? 7 : 6];
            w.mousedown(new Coord(w.sz.x / 2, w.sz.y / 2), 1);

            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                e.wdgmsg("drop", noltorch ? 7 : 6);
                return;
            }

            gui.map.wdgmsg("itemact", Coord.z, gob.rc, 0, 0, (int) gob.id, gob.rc, 0, -1);

            if (!Utils.waitForProgressFinish(gui, TIMEOUT_ACT, "Oops something went wrong. Timeout when trying to light with torch.")) {
                e.wdgmsg("drop", noltorch ? 7 : 6);
                return;
            }

            e.wdgmsg("drop", noltorch ? 7 : 6);
        } catch (InterruptedException ie) {
        }
    }
}
