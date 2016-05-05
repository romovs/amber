package haven.resutil;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import haven.GLState;
import haven.GOut;
import haven.Location;
import haven.Material;
import haven.RenderList;
import haven.Rendered;
import haven.Sprite;
import haven.States;
import haven.States.ColState;
import haven.Utils;
import haven.VertexBuf.NormalArray;
import haven.VertexBuf.VertexArray;

public class BPRadSprite extends Sprite {
    static final GLState smat = new ColState(new Color(192, 0, 0, 80));
    final VertexArray posa;
    final NormalArray nrma;
    final ShortBuffer sidx;

    public BPRadSprite(float rad, float basez) {
        super(null, null);

        int per = Math.max(24, (int) (2 * Math.PI * (double) rad / 11.0D));
        FloatBuffer pa = Utils.mkfbuf(per * 3 * 2);
        FloatBuffer na = Utils.mkfbuf(per * 3 * 2);
        ShortBuffer sa = Utils.mksbuf(per * 6);

        for (int i = 0; i < per; ++i) {
            float s = (float) Math.sin(2 * Math.PI * (double) i / (double) per);
            float c = (float) Math.cos(2 * Math.PI * (double) i / (double) per);
            pa.put(i * 3 + 0, c * rad).put(i * 3 + 1, s * rad).put(i * 3 + 2, 10.0F);
            pa.put((per + i) * 3 + 0, c * rad).put((per + i) * 3 + 1, s * rad).put((per + i) * 3 + 2, basez);
            na.put(i * 3 + 0, c).put(i * 3 + 1, s).put(i * 3 + 2, 0.0F);
            na.put((per + i) * 3 + 0, c).put((per + i) * 3 + 1, s).put((per + i) * 3 + 2, 0.0F);
            int v = i * 6;
            sa.put(v + 0, (short) i).put(v + 1, (short) (i + per)).put(v + 2, (short) ((i + 1) % per));
            sa.put(v + 3, (short) (i + per)).put(v + 4, (short) ((i + 1) % per + per)).put(v + 5, (short) ((i + 1) % per));
        }

        this.posa = new VertexArray(pa);
        this.nrma = new NormalArray(na);
        this.sidx = sa;
    }

    public boolean setup(RenderList rl) {
        rl.prepo(Rendered.eyesort);
        rl.prepo(Material.nofacecull);
        Location.goback(rl.state(), "gobx");
        rl.state().put(States.color, null);
        return true;
    }

    public void draw(GOut g) {
        g.state(smat);
        g.apply();
        this.posa.bind(g, false);
        this.nrma.bind(g, false);
        this.sidx.rewind();
        g.gl.glDrawElements(4, this.sidx.capacity(), 5123, this.sidx);
        this.posa.unbind(g);
        this.nrma.unbind(g);
    }
}
