package haven.res.ui.tt.q.qbuff;

import haven.ItemInfo;

final class Tid implements ItemInfo.Layout.ID<Table> {
    Tid() {
    }

    public Table make() {
        return new Table();
    }
}