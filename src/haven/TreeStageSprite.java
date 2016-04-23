package haven;

import java.awt.*;


public class TreeStageSprite extends Sprite {
    private static final Text.Foundry fndr = new Text.Foundry(Text.sansb, 12).aa(true);
    private static final Tex[] treestg = new Tex[90];
    private static final Color stagecolor = new Color(235, 235, 235);//new Color(235, 235, 235);
    public int val;
    private Tex tex;
    GLState.Buffer buf;

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
        Matrix4f cam = new Matrix4f(), wxf = new Matrix4f(), mv = new Matrix4f();
        mv.load(cam.load(buf.get(PView.cam).fin(Matrix4f.id))).mul1(wxf.load(buf.get(PView.loc).fin(Matrix4f.id)));
        Coord3f s = buf.get(PView.proj).toscreen(mv.mul4(Coord3f.o), buf.get(PView.wnd).sz());
        g.image(tex, new Coord((int) s.x - 8, (int) s.y - 20));
    }

    public boolean setup(RenderList rl) {
        rl.prepo(last);
        buf = rl.state().copy();
        return true;
    }

    public void update(int val) {
        this.val = val;
        tex = treestg[val - 10];
    }
}
