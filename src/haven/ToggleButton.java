package haven;

import java.awt.Graphics;
import java.awt.image.BufferedImage;


public class ToggleButton extends SSWidget {
    BufferedImage up, down;
    boolean pressed;
    UI.Grab d = null;

    public ToggleButton(BufferedImage up, BufferedImage down) {
        super(Utils.imgsz(up));
        this.up = up;
        this.down = down;
    }

    public ToggleButton(String up, String down, boolean pressed) {
        this(Resource.loadimg(up), Resource.loadimg(down));
        this.pressed = pressed;
        render();
    }

    public void render() {
        clear();
        Graphics g = graphics();
        g.drawImage(pressed ? down : up, 0, 0, null);
        update();
    }

    public boolean checkhit(Coord c) {
        if (!c.isect(Coord.z, sz))
            return false;
        if (up.getRaster().getNumBands() < 4)
            return true;
        return (up.getRaster().getSample(c.x, c.y, 3) >= 128);
    }

   public void click() {
       wdgmsg("activate");
    }

    public boolean mousedown(Coord c, int button) {
        if (button != 1)
            return false;
        if (!checkhit(c))
            return false;
        d = ui.grabmouse(this);
        pressed = !pressed;
        render();
        click();
        return true;
    }

    public Object tooltip(Coord c, Widget prev) {
        if (!checkhit(c))
            return null;
        return (super.tooltip(c, prev));
    }
}
