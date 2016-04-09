package haven.res.ui.tt.q.qbuff;

import haven.ItemInfo;

class ShortTip extends ItemInfo.Tip {
    QBuff qb;
    ShortTip(QBuff var1, Owner var2) {
        super(var2);
        this.qb = var1;
    }

    public void prepare(Layout var1) {
        ((Summary)var1.intern(QBuff.sid)).ql.add(this.qb);
    }
}