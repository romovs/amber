package purus;

import java.awt.Color;

import javax.media.opengl.GL2;

import haven.BGL;
import haven.Config;
import haven.Coord;
import haven.GOut;
import haven.Gob;
import haven.RenderList;
import haven.Sprite;
import haven.States;

public class CustomHitbox extends Sprite {
	
	public float size;

	public int red;
	public int green;
	public int blue;
    private final int tx, ty, bx, by;
    private int mode;
    
    public CustomHitbox(Gob gob, Coord ac, Coord bc, boolean fill) {
        super(gob, null);
        mode = fill ? GL2.GL_QUADS : GL2.GL_LINE_LOOP;
        tx = ac.x;
        ty = ac.y;
        bx = bc.x;
        by = bc.y;
        red = (int) Config.hidered;
        green = (int) Config.hidegreen;
        blue = (int) Config.hideblue;
    }

    public boolean setup(RenderList rl) {
        rl.prepo(new States.ColState(new Color(red, green, blue, 200)));
        return true;
    }

    public void draw(GOut g) {
        g.state(new States.ColState(new Color(red, green, blue, 200)));
        g.apply();
        BGL gl = g.gl;
        gl.glBegin(mode);
        gl.glVertex3f(tx, ty, 1);
        gl.glVertex3f(bx, ty, 1);
        gl.glVertex3f(bx, by, 1);
        gl.glVertex3f(tx, by, 1);
        gl.glEnd();
    }
}
