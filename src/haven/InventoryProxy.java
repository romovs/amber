package haven;

public class InventoryProxy extends Widget implements DTarget {
    private final Inventory target;
    private boolean locked;
    

    public InventoryProxy(Inventory target) {
	super(target.sz);
	this.target = target;
    }

    @Override
    public void draw(GOut g) {
	target.draw(g);
    }

    @Override
    public void draw(GOut g, boolean strict) {
	target.draw(g, strict);
    }

    @Override
    public boolean mousedown(Coord c, int button) {
    	if (locked)
    		return false;
	return target.mousedown(c, button);
    }

    @Override
    public boolean mouseup(Coord c, int button) {
	return target.mouseup(c, button);
    }

    @Override
    public boolean mousewheel(Coord c, int amount) {
	return target.mousewheel(c, amount);
    }

    @Override
    public void mousemove(Coord c) {
	target.mousemove(c);
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
	WItem item = itemat(c);
	return (item != null) ? item.tooltip(c.sub(item.c), (prev == this) ? item : prev) : null;
    }

    private WItem itemat(Coord c) {
	for (WItem item : target.children(WItem.class)) {
	    if (c.isect(xlate(item.c, true), item.sz)) {
		return item;
	    }
	}
	return null;
    }
    
    public void setLocked(boolean value) {
        locked = value;
    }

    @Override
    public boolean drop(Coord cc, Coord ul) {
	return target.drop(cc, ul);
    }

    @Override
    public boolean iteminteract(Coord cc, Coord ul) {
	return target.iteminteract(cc, ul);
    }
}