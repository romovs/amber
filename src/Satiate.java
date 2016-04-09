
import haven.Indir;
import haven.ItemInfo;
import haven.ItemInfo.InfoFactory;
import haven.ItemInfo.Owner;

public class Satiate implements InfoFactory {
    public Satiate() {
    }

    public ItemInfo build(Owner owner, Object... args) {
        Indir icon = owner.glob().sess.getres(((Integer) args[1]).intValue());
        double val = ((Number) args[2]).doubleValue();
        return new SatiateTip(this, owner, icon, val);
    }
}