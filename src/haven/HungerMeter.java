package haven;

public class HungerMeter extends Widget {
    private static final Tex bg = Resource.loadtex("hud/meter/hungermeter");

    private final CharWnd.GlutMeter glut;

    public HungerMeter(CharWnd.GlutMeter glut) {
        super(IMeter.fsz);
        this.glut = glut;
    }

    @Override
    public void draw(GOut g) {
    	if (glut.bg == null)
    		return;
        Coord isz = IMeter.msz;
        Coord off = IMeter.off;
        g.chcolor(glut.bg);
        g.frect(off, isz);
        g.chcolor(glut.fg);
        g.frect(off, new Coord((int) Math.round(isz.x * (glut.glut - Math.floor(glut.glut))), isz.y));
        g.chcolor();
        g.image(bg, Coord.z);
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        return glut.tooltip(c, prev);
    }
}