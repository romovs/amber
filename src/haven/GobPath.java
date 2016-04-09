package haven;

import javax.media.opengl.GL2;
import java.awt.*;

public class GobPath extends Sprite {
    private static final Color plclr = new Color(233, 185, 110);
    public LinMove lm;
    private static final int vd = 44 * 11;

    public GobPath(Gob gob) {
        super(gob, null);
    }

    public boolean setup(RenderList rl) {
        Gob gob = (Gob) owner;
        try {
            Location.goback(rl.state(), "gobx");
        } catch (IllegalStateException ise) {
            // no gobx backlink for sling and catapult projectiles
            return false;
        }
        rl.prepo(States.xray);
        Color clr;
        if (gob.isplayer()) {
            clr = plclr;
        } else {
            KinInfo ki = gob.getattr(KinInfo.class);
            clr = ki != null ? BuddyWnd.gc[ki.group] : Color.WHITE;
        }
        rl.prepo(new States.ColState(clr));
        return true;
    }

    public void draw(GOut g) {
        if (lm == null)
            return;
        Gob gob = (Gob) owner;
        Coord3f pc = gob.getc();
        float x = lm.t.x - pc.x;
        float y = -lm.t.y + pc.y;
        float z = Math.sqrt(x * x + y * y) >= vd ? 0 : gob.glob.map.getcz(lm.t.x, lm.t.y) - pc.z;
        g.apply();
        BGL gl = g.gl;
        gl.glLineWidth(2.0F);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(x, y, z);
        gl.glEnd();
        gl.glDisable(GL2.GL_LINE_SMOOTH);
    }
}
