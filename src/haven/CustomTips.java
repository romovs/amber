package haven;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class CustomTips {
    static BufferedImage itemTooltip(GItem gi, WItem.ItemTip tip) {
        ArrayList<BufferedImage> tips = new ArrayList<>();
        tips.add(tip.img);

        if (gi.finishedTime > System.currentTimeMillis()) 
            tips.add(Text.render("Time Left: " + Utils.timeLeft(gi.finishedTime)).img);
        	 else if (gi.lmeter1 > 0)
            tips.add(Text.render("Time Left: Calculating...").img);
        // Doesnt display inv object res file name, i dont think this is  needed
           /* try {
                Resource r = gi.resource();
                if (r != null)
                    tips.add(Text.render("R: " + r.name).img);
                GSprite s = gi.sprite();
                if (s != null && s.getname() != null)
                    tips.add(Text.render("S: " + s.getname()).img);
            } catch (Loading ignored) {
            } */

        return ItemInfo.catimgs(0, tips.toArray(new BufferedImage[tips.size()]));
    }
}