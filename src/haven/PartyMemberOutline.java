package haven;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.media.opengl.GL;

public class PartyMemberOutline extends Sprite {
    private final GLState mat;
    private final VertexBuf.VertexArray posa;
    private final VertexBuf.NormalArray nrma;
    private final ShortBuffer eidx; 
    private final Float height;
    private Coord lc;


    protected PartyMemberOutline(Owner owner, Color color, Float height) {
        super(owner, null);
        this.height = height;
        float rad = 50 / 10.0F;
        int i = Math.max(24, (int)(Math.PI * 2 * rad / 11.0D));
        FloatBuffer posa = Utils.mkfbuf(i * 3);
        FloatBuffer nrma = Utils.mkfbuf(i * 3);
        ShortBuffer eidx = Utils.mksbuf(i);
        for (int j = 0; j < i; j++) {
            float sin = (float)Math.sin(Math.PI * 2 * j / i);
            float cos = (float)Math.cos(Math.PI * 2 * j / i);
            posa.put(j * 3, cos * rad).put(j * 3 + 1, sin * rad).put(j * 3 + 2, 0.1f);
            nrma.put(j * 3, cos).put(j * 3 + 1, sin).put(j * 3 + 2, 0.0F);
            eidx.put(j, (short) j);
        }
        this.posa = new VertexBuf.VertexArray(posa);
        this.nrma = new VertexBuf.NormalArray(nrma);
        this.eidx = eidx;
        this.mat = new States.ColState(color);
    }

    @Override
    public void draw(GOut g) {
        g.state(mat);
        g.apply();
        this.posa.bind(g, false);
        this.nrma.bind(g, false);
        this.eidx.rewind();
        g.gl.glLineWidth(4.0F);
        g.gl.glDrawElements(GL.GL_LINE_LOOP, this.eidx.capacity(), GL.GL_UNSIGNED_SHORT, this.eidx);
        this.posa.unbind(g);
        this.nrma.unbind(g);
    }

    @Override
    public boolean setup(RenderList d) {
        d.prepo(Rendered.eyesort);
        d.prepo(Material.nofacecull);
        Location.goback(d.state(), "gobx");
        d.state().put(States.color, null);
        return true;
    }

    @Override
    public boolean tick(int dt) {
        Coord c = ((Gob)this.owner).rc;
        if ((this.lc == null) || (!this.lc.equals(c)))
        {
            setz(this.owner.glob(), c);
            this.lc = c;
        }
        return false;
    }

    private void setz(Glob glob, Coord c)
    {
        FloatBuffer posa = this.posa.data;
        try {
            float z = glob.map.getcz(c.x, c.y);
            for (int j = 0; j < this.posa.size(); j++) {
                float tz = glob.map.getcz(c.x + posa.get(j * 3), c.y - posa.get(j * 3 + 1)) - z;
                posa.put(j * 3 + 2, tz + height);
            }
        }
        catch (Loading e) {}
    }
}