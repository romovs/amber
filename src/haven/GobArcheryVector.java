package haven;

import javax.media.opengl.GL2;
import java.awt.*;

public class GobArcheryVector extends Sprite {
    private static final float[] friend = Utils.c2fa(new Color(78, 154, 6));
    private static final float[] foe = Utils.c2fa(new Color(164, 0, 6));
    private final static int DISTANCE = 400;
    private float[] clr;
    private final Gob followGob;

    public GobArcheryVector(Gob pl, Gob followGob) {
        super(pl, null);
        this.followGob = followGob;
    }

    public boolean setup(RenderList rl) {
        Gob gob = (Gob) owner;
        Location.goback(rl.state(), "gobx");
        rl.prepo(States.xray);
        KinInfo ki = gob.getattr(KinInfo.class);
        clr = gob.isplayer() || ki != null && ki.group != 2 && ki.group != 0 ? friend : foe;
        rl.prepo(States.vertexcolor);
        return true;
    }

    public void draw(GOut g) {
        Gob gob = (Gob) owner;
        Coord3f pc = gob.getrc();
        double a = followGob != null ? followGob.a : gob.a;
        float x = (float) (DISTANCE * Math.cos(a));
        float y = (float) (DISTANCE * Math.sin(-a));
        float z = gob.glob.map.getcz(pc.x + x, pc.y + y) - pc.z;

        g.apply();
        BGL gl = g.gl;
        gl.glPushAttrib(GL2.GL_ENABLE_BIT);

        gl.glLineStipple(8, (short) 0xAAAA);
        gl.glEnable(GL2.GL_LINE_STIPPLE);
        gl.glLineWidth(4.0F);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);

        gl.glBegin(GL2.GL_LINES);
        gl.glColor4fv(clr, 0);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(x, y, z);
        gl.glEnd();

        gl.glLineStipple(8, (short) 0x5555);

        gl.glBegin(GL2.GL_LINES);
        gl.glColor3f(0.0f, 0.0f, 0.0f);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(x, y, z);
        gl.glEnd();

        gl.glPopAttrib();
    }
}
