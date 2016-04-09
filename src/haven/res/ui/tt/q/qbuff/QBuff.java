package haven.res.ui.tt.q.qbuff;

import haven.ItemInfo;
import haven.Resource;

import java.awt.image.BufferedImage;


public class QBuff extends ItemInfo.Tip {
    public final BufferedImage icon;
    public final String name;
    public final String origName;
    public final double q;
    public static final Layout.ID<Table> lid = new Tid();
    public static final Layout.ID<Summary> sid = new Sid();

    public QBuff(Owner owner, BufferedImage icon, String name, double q) {
        super(owner);
        this.icon = icon;
        this.origName = name;
        this.name = Resource.getLocString(Resource.l10nLabel, name);
        this.q = q;
    }

    public void prepare(Layout layout) {
        layout.intern(lid).ql.add(this);
    }

    public Tip shortvar() {
        return new ShortTip(this, this.owner);
    }
}
