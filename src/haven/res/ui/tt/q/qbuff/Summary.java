package haven.res.ui.tt.q.qbuff;

import haven.CompImage;
import haven.Coord;
import haven.ItemInfo;
import haven.Text;

public class Summary extends QList {
    public Summary() {
    }

    public int order() {
        return 10;
    }

    public void layout(ItemInfo.Layout layout) {
        this.sort();
        CompImage ci = new CompImage();

        for (int i = 0; i < this.ql.size(); ++i) {
            QBuff qb = this.ql.get(i);
            String str = String.format(i < this.ql.size() - 1 ? "%,d, " : "%,d", new Object[]{Long.valueOf(Math.round(qb.q))});
            ci.add(qb.icon, new Coord(ci.sz.x, 0));
            ci.add(Text.render(str).img, new Coord(ci.sz.x, 0));
        }

        layout.cmp.add(ci, new Coord(layout.cmp.sz.x + 10, 0));
    }
}