package haven;

import java.awt.image.BufferedImage;

public class CombatHelper {
    public static int getAttackType(BufferedImage image) {
        int type = 0;
        int w = image.getWidth();
        int h = image.getHeight();
        Coord[] corners = {
            new Coord(0, 0),
            new Coord(0, h - 1),
            new Coord(w - 1, h - 1),
            new Coord(w - 1, 0)
        };
        for (Coord corner : corners) {
            int rgb = image.getRGB(corner.x, corner.y);
            type |= AttackType.getByColor(rgb);
        }
        return type;
    }
}

