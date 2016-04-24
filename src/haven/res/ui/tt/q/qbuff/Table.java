package haven.res.ui.tt.q.qbuff;

import java.util.Iterator;

import haven.CompImage;
import haven.Coord;
import haven.Text;

public class Table extends QList {
    public Table() {
    }

    public int order() {
        return 10;
    }

    public void layout(Layout layout) {
        this.sort();
        CompImage ci = new CompImage();
        CompImage.Image[] icon = new CompImage.Image[this.ql.size()];
        CompImage.Image[] text = new CompImage.Image[this.ql.size()];
        CompImage.Image[] val = new CompImage.Image[this.ql.size()];
        int i = 0;

        for (Iterator it = this.ql.iterator(); it.hasNext(); ++i) {
            QBuff qb = (QBuff) it.next();
            icon[i] = CompImage.mk(qb.icon);
            text[i] = CompImage.mk(Text.render(qb.name + ":").img);
            val[i] = CompImage.mk(Text.render((double) ((int) qb.q) == qb.q ? String.format("%d", new Object[]{Integer.valueOf((int) qb.q)}) : String.format("%.1f", new Object[]{Double.valueOf(qb.q)})).img);
        }

        ci.table(Coord.z, new CompImage.Image[][]{icon, text, val}, new int[]{5, 15}, 0, new int[]{0, 0, 1});
        layout.cmp.add(ci, new Coord(0, layout.cmp.sz.y));
    }
}