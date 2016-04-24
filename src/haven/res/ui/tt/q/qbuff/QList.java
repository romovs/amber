package haven.res.ui.tt.q.qbuff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import haven.ItemInfo;

public abstract class QList extends ItemInfo.Tip {
    final List<QBuff> ql = new ArrayList<>();

    QList() {
        super(null);
    }

    void sort() {
        Collections.sort(this.ql, (a, b) -> a.origName.compareTo(b.origName));
    }
}