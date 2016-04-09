import haven.Coord;
import haven.Indir;
import haven.PUtils;
import haven.Resource;
import haven.RichText;
import haven.Text;
import haven.CharWnd.Constipations;
import haven.ItemInfo.Tip;
import haven.Resource.Image;

import java.awt.image.BufferedImage;

class SatiateTip extends Tip {
    Satiate satiate;
    Indir icon;
    double val;

    SatiateTip(Satiate satiate, Owner owner, Indir icon, double val) {
        super(owner);
        this.satiate = satiate;
        this.icon = icon;
        this.val = val;
    }

    public BufferedImage longtip() {
        String satiateStr = Resource.getLocString(Resource.l10nLabel, "Satiate ");
        BufferedImage sat = Text.render(satiateStr).img;
        int h = sat.getHeight();
        BufferedImage ico = PUtils.convolvedown(((Image) ((Resource) this.icon.get()).layer(Resource.imgc)).img, new Coord(h, h), Constipations.tflt);
        String byStr = Resource.getLocString(Resource.l10nLabel, " by $col[255,128,128]{%d%%}");
        BufferedImage by = RichText.render(String.format(byStr, new Object[]{Integer.valueOf((int) Math.round((1.0D - this.val) * 100.0D))}), 0, new Object[0]).img;
        return catimgsh(0, new BufferedImage[]{sat, ico, by});
    }
}
