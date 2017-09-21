package haven;

import java.awt.Color;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;


public class PlantStageSprite extends Sprite {
    private static final Color stagecolor = new Color(255, 227, 168);
    private static final Tex stgmaxtex = Text.renderstroked("\u25CF", new Color(254, 100, 100), Color.BLACK, Text.num12boldFnd).tex();
    private static final Tex stghrvtex = Text.renderstroked("\u25CF", new Color(201, 180, 0), Color.BLACK, Text.num12boldFnd).tex();
    private static final Tex[] stgtex = new Tex[]{
            Text.renderstroked("2", stagecolor, Color.BLACK, Text.num12boldFnd).tex(),
            Text.renderstroked("3", stagecolor, Color.BLACK, Text.num12boldFnd).tex(),
            Text.renderstroked("4", stagecolor, Color.BLACK, Text.num12boldFnd).tex(),
            Text.renderstroked("5", stagecolor, Color.BLACK, Text.num12boldFnd).tex(),
            Text.renderstroked("6", stagecolor, Color.BLACK, Text.num12boldFnd).tex()
    };
    public int stg;
    public int stgmax;
    private Tex tex;
    private static final Map<String, Tex> plantTex = new HashMap<>();
    private static final Text.Foundry gobhpf = new Text.Foundry(Text.sans, 14).aa(true);
    private static Matrix4f mv = new Matrix4f();
    private Projection proj;
    private Coord wndsz;
    private Location.Chain loc;
    private Camera camp;
    private final boolean multistg;

    public PlantStageSprite(int stg, int stgmax, boolean multistg) {
        super(null, null);
        this.multistg = multistg;
        update(stg, stgmax);
    }

    public void draw(GOut g) {
        float[] c = mv.load(camp.fin(Matrix4f.id)).mul1(loc.fin(Matrix4f.id)).homoc();
        Coord sc = proj.get2dCoord(c, wndsz);
        sc.x -= tex.sz().x/2;
        sc.y -= 10;
        g.image(tex, sc);
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

    public void update(int stg, int stgmax) {
        this.stg = stg;
        if(Config.showplantgrowstageastext) {
            String str = String.format("%d/%d", new Object[]{stg, stgmax});
            if (!plantTex.containsKey(str)) {
                plantTex.put(str, Text.renderstroked(str, stg >= stgmax ? Color.GREEN : Color.RED, Color.BLACK, gobhpf).tex());
            }
            tex = plantTex.get(str);
        } else {
            if (multistg && stg == stgmax - 1)
                tex = stghrvtex;
            else if (stg == stgmax)
                tex = stgmaxtex;
            else
                tex = stgtex[stg - 1];
        }
    }

    public Object staticp() {
        return CONSTANS;
    }
}
