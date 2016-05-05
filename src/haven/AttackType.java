package haven;

import java.awt.Color;

public class AttackType {
    public static final AttackType OPPRESSIVE = new AttackType(0x1, new Color(210, 5, 10));
    public static final AttackType STRIKING = new AttackType(0x2, new Color(30, 186, 103));
    public static final AttackType BACKHANDED = new AttackType(0x4, new Color(6, 30, 210));
    public static final AttackType SWEEPING = new AttackType(0x8, new Color(255, 240, 1));
    public static final AttackType[] All;

    static {
        All = new AttackType[] { OPPRESSIVE, STRIKING, BACKHANDED, SWEEPING };
    }

    public final int value;
    public final Color color;

    private AttackType(int value, Color color) {
        this.value = value;
        this.color = color;
    }

    public static int getByColor(int rgb) {
        for (AttackType type : All) {
            if (type.color.getRGB() == rgb)
                return type.value;
        }
        return 0;
    }
}