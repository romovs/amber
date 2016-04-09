package haven.res.ui.tt.q.qbuff;

import haven.ItemInfo;

final class Sid implements ItemInfo.Layout.ID<Summary> {
    Sid() {
    }

    public Summary make() {
        return new Summary();
    }
}
