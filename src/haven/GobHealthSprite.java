package haven;

import java.awt.*;


public class GobHealthSprite extends Sprite {
    private static final Text.Foundry gobhpf = new Text.Foundry(Text.sansb, 12).aa(true);
    private static final Tex hlt0 = Text.renderstroked("25%", new Color(255, 225, 225), Color.BLACK, gobhpf).tex();
    private static final Tex hlt1 = Text.renderstroked("50%", new Color(255, 225, 225), Color.BLACK, gobhpf).tex();
    private static final Tex hlt2 = Text.renderstroked("75%", new Color(255, 225, 225), Color.BLACK, gobhpf).tex();
    public int val;
    private Tex tex;

    GLState.Buffer buf;

    public GobHealthSprite(int val) {
        super(null, null);
        update(val);
    }

    public void draw(GOut g) {
        Matrix4f cam = new Matrix4f(), wxf = new Matrix4f(), mv = new Matrix4f();
        mv.load(cam.load(buf.get(PView.cam).fin(Matrix4f.id))).mul1(wxf.load(buf.get(PView.loc).fin(Matrix4f.id)));
        Coord3f s = buf.get(PView.proj).toscreen(mv.mul4(Coord3f.o), buf.get(PView.wnd).sz());
        g.image(tex, new Coord((int) s.x - 15, (int) s.y - 20));
    }

    public boolean setup(RenderList rl) {
        rl.prepo(last);
        buf = rl.state().copy();
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
