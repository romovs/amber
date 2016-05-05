package haven;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActList extends Listbox<ActList.ActItem> {
    private static final Text.Foundry font = new Text.Foundry(Text.fraktur, 15).aa(true);
    private final List<ActItem> items = new ArrayList<ActItem>();
    private final Map<Glob.Pagina, ActItem> map = new HashMap<Glob.Pagina, ActItem>();

    public ActList(int w, int h) {
        super(w, h, font.height() + 2);
    }

    public void add(Glob.Pagina pagina) {
        ActItem item = new ActItem(pagina);
        map.put(pagina, item);
        items.add(item);
    }

    public void remove(Glob.Pagina pagina) {
        ActItem item = map.remove(pagina);
        if (item != null)
            items.remove(item);
    }

    public void clear() {
        map.clear();
        items.clear();
    }

    public void sort(Comparator<ActItem> comparator) {
        Collections.sort(items, comparator);
    }

    @Override
    protected ActItem listitem(int i) {
        return items.get(i);
    }

    @Override
    protected int listitems() {
        return items.size();
    }

    @Override
    protected void drawbg(GOut g) {
        g.chcolor(0, 0, 0, 128);
        g.frect(Coord.z, sz);
        g.chcolor();
    }

    @Override
    protected void drawitem(GOut g, ActItem item, int i) {
        g.image(item.icon, Coord.z);
        g.aimage(item.name.tex(), new Coord(itemh + 5, itemh / 2), 0, 0.5);
    }

    public class ActItem {
        public final Glob.Pagina pagina;
        public final Text name;
        public final Tex icon;

        public ActItem(Glob.Pagina pagina) {
            this.pagina = pagina;
            this.name = font.render(this.pagina.act().name);
            this.icon = new TexI(PUtils.convolvedown(pagina.res.get().layer(Resource.imgc).img, new Coord(itemh, itemh), CharWnd.iconfilter));
        }
    }
}