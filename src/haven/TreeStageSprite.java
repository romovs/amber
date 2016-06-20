package haven;

import java.awt.*;


public class TreeStageSprite extends Sprite {
    private static final Text.Foundry fndr = new Text.Foundry(Text.sans.deriveFont(Font.BOLD), Config.fontsizeglobal * 12 / 10).aa(true);
    private static final Tex[] treestg = new Tex[90];
    private static final Color stagecolor = new Color(255, 227, 168);//new Color(235, 235, 235);
    public int val;
    private Tex tex;
    private static Matrix4f cam = new Matrix4f();
    private static Matrix4f wxf = new Matrix4f();
    private static Matrix4f mv = new Matrix4f();
    private Projection proj;
    private Coord wndsz;
    private Location.Chain loc;
    private Camera camp;

    static {
        for (int i = 10; i < 100; i++) {
            treestg[i - 10] = Text.renderstroked(i + "", stagecolor, Color.BLACK, fndr).tex();
        }
    }

    public TreeStageSprite(int val) {
        super(null, null);
        update(val);
    }

    public void draw(GOut g) {
        mv.load(cam.load(camp.fin(Matrix4f.id))).mul1(wxf.load(loc.fin(Matrix4f.id)));
        Coord3f s = proj.toscreen(mv.mul4(Coord3f.o), wndsz);
        g.image(tex, new Coord((int) s.x - 8, (int) s.y - 20));
    }

    public boolean setup(RenderList rl) {
        rl.prepo(last);
        GLState.Buffer buf = rl.state();
        proj = buf.get(PView.proj);
        wndsz = buf.get(PView.wnd).sz();
        loc = buf.get(PView.loc);
        camp = buf.get(PView.cam);
        return true;
    }

    public void update(int val) {
        this.val = val;
        tex = treestg[val - 10];
    }

    public Object staticp() {
        return CONSTANS;
    }
}
