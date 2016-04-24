package haven;

import java.awt.Color;

public class DamageSprite extends Sprite implements PView.Render2D {
    public static final int ID = -1000;
    private static final Text.Furnace dfrn = new PUtils.BlurFurn(new Text.Foundry(Text.sans, 14, new Color(251, 78, 78)).aa(true), 1, 1, new Color(188, 0, 0));
    private static final Text.Furnace afrn = new PUtils.BlurFurn(new Text.Foundry(Text.sans, 14, new Color(76, 202, 98)).aa(true), 1, 1, new Color(0, 142, 24));
    public int dmg, arm;
    private Tex dmgtex, armtex;
    private Gob gob;
    private static int ywinfix = Config.iswindows ? 2 : 0;

    public DamageSprite(int dmg, boolean isarmor, Gob gob) {
        super(null, null);
        if (isarmor) {
            this.arm = dmg;
            this.armtex = afrn.render(dmg + "").tex();
            this.dmgtex = dfrn.render("").tex();
        } else {
            this.dmg = dmg;
            this.dmgtex = dfrn.render(dmg + "").tex();
            this.armtex = afrn.render("").tex();
        }
        this.gob = gob;
    }

    public DamageSprite(int dmg, int arm, Gob gob) {
        super(null, null);
        this.arm = arm;
        this.armtex = afrn.render(arm + "").tex();
        this.dmg = dmg;
        this.dmgtex = dfrn.render(dmg + "").tex();
        this.gob = gob;
    }

    public void draw2d(GOut g) {
        Coord dmgsz = dmgtex.sz();
        Coord armsz = armtex.sz();

        int xoff = 0;
        if (dmg > 0)
            xoff = dmgsz.x / 2;
        if (arm > 0)
            xoff += armsz.x / 2;

        Coord pos = gob.sc.add((int) (gob.sczu.x * 15f - xoff), (int) (gob.sczu.y * 15f - 40));

        int armxoff = 0;
        if (dmg > 0) {
            g.chcolor(35, 35, 35, 192);
            g.frect(pos.add(-1, 1 + ywinfix), dmgsz.add(2, -3 - ywinfix));
            g.chcolor();
            g.image(dmgtex, pos);
            armxoff = dmgsz.x + 2;
        }
        if (arm > 0) {
            g.chcolor(35, 35, 35, 192);
            g.frect(pos.add(-1 + armxoff, 1 + ywinfix), armsz.add(2, -3 - ywinfix));
            g.chcolor();
            g.image(armtex, pos.add(armxoff, 0));
        }
    }

    public boolean setup(RenderList var1) {
        return true;
    }

    public void update(int dmg, boolean isarmor) {
        if (isarmor) {
            this.arm += dmg;
            this.armtex = afrn.render(this.arm + "").tex();
        } else {
            this.dmg += dmg;
            this.dmgtex = dfrn.render(this.dmg + "").tex();
        }
    }
}
