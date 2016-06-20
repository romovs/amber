package haven;

import java.awt.*;


public class GobHealthSprite extends Sprite {
    private static final Text.Foundry gobhpf = new Text.Foundry(Text.sans.deriveFont(Font.BOLD), Config.fontsizeglobal * 12 / 10).aa(true);
    private static final Tex hlt0 = Text.renderstroked("25%", new Color(255, 227, 168), Color.BLACK, gobhpf).tex();
    private static final Tex hlt1 = Text.renderstroked("50%", new Color(255, 227, 168), Color.BLACK, gobhpf).tex();
    private static final Tex hlt2 = Text.renderstroked("75%", new Color(255, 227, 168), Color.BLACK, gobhpf).tex();
    public int val;
    private Tex tex;
    private static Matrix4f cam = new Matrix4f();
    private static Matrix4f wxf = new Matrix4f();
    private static Matrix4f mv = new Matrix4f();
    private Projection proj;
    private Coord wndsz;
    private Location.Chain loc;
    private Camera camp;

    public GobHealthSprite(int val) {
        super(null, null);
        update(val);
    }

    public void draw(GOut g) {
        mv.load(cam.load(camp.fin(Matrix4f.id))).mul1(wxf.load(loc.fin(Matrix4f.id)));
        Coord3f s = proj.toscreen(mv.mul4(Coord3f.o), wndsz);
        g.image(tex, new Coord((int) s.x - 15, (int) s.y - 20));
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
        switch (val - 1) {
            case 0:
                tex = hlt0;
                break;
            case 1:
                tex = hlt1;
                break;
            case 2:
                tex = hlt2;
                break;
        }
    }
}
