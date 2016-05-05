package haven;

import java.awt.Color;

public class FepMeter extends Widget {
    private static final Tex bg = Resource.loadtex("hud/meter/fepmeter");

    private final CharWnd.FoodMeter food;

    public FepMeter(CharWnd.FoodMeter food) {
        super(IMeter.fsz);
        this.food = food;
    }

    @Override
    public void draw(GOut g) {
        Coord isz = IMeter.msz;
        Coord off = IMeter.off;
        g.chcolor(0, 0, 0, 255);
        g.frect(off, isz);
        g.chcolor();
        double x = 0;
        int w = isz.x;
        for(CharWnd.FoodMeter.El el : food.els) {
            int l = (int)Math.floor((x / food.cap) * w);
            int r = (int)Math.floor(((x += el.a) / food.cap) * w);
            try {
                Color col = el.ev().col;
                g.chcolor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 255));
                g.frect(off.add(l, 0), new Coord(r - l, isz.y));
            } catch(Loading e) {
            }
        }
        g.chcolor();
        g.image(bg, Coord.z);
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        return food.tooltip(c, prev);
    }
}