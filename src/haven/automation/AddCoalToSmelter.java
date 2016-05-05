package haven.automation;


import haven.Coord;
import haven.GItem;
import haven.GameUI;
import haven.Gob;
import haven.Resource;
import haven.WItem;

public class AddCoalToSmelter implements Runnable {
    private GameUI gui;
    private Gob smelter;
    private int count;
    private static final int TIMEOUT = 2000;
    private static final int HAND_DELAY = 8;

    public AddCoalToSmelter(GameUI gui, int count) {
        this.gui = gui;
        this.count = count;
    }

    @Override
    public void run() {
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                Resource res = gob.getres();
                if (res != null && res.name.contains("smelter")) {
                    if (smelter == null)
                        smelter = gob;
                    else if (gob.rc.dist(gui.map.player().rc) < smelter.rc.dist(gui.map.player().rc))
                        smelter = gob;
                }
            }
        }

        if (smelter == null) {
            gui.error("No smelters found");
            return;
        }

        WItem coalw = gui.maininv.getItemPartial("Coal");
        if (coalw == null) {
            gui.error("No coal found in the inventory");
            return;
        }
        GItem coal = coalw.item;

        coal.wdgmsg("take", new Coord(coal.sz.x / 2, coal.sz.y / 2));
        int timeout = 0;
        while (gui.hand.isEmpty()) {
            timeout += HAND_DELAY;
            if (timeout >= TIMEOUT) {
                gui.error("No coal found in the inventory");
                return;
            }
            try {
                Thread.sleep(HAND_DELAY);
            } catch (InterruptedException e) {
                return;
            }
        }
        coal = gui.vhand.item;

        for (; count > 0; count--) {
            gui.map.wdgmsg("itemact", Coord.z, smelter.rc, count == 1 ? 0 : 1, 0, (int) smelter.id, smelter.rc, 0, -1);
            timeout = 0;
            while (true) {
                WItem newcoal = gui.vhand;
                if (newcoal != null && newcoal.item != coal) {
                    coal = newcoal.item;
                    break;
                } else if (newcoal == null && count == 1) {
                    return;
                }

                timeout += HAND_DELAY;
                if (timeout >= TIMEOUT) {
                    gui.error("Not enough coal. Need to add " + (count - 1) + " more.");
                    return;
                }
                try {
                    Thread.sleep(HAND_DELAY);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}
