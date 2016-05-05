package haven.pathfinder;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.imageio.ImageIO;

public class Dbg {
    private static boolean DEBUG = false;
    private BufferedImage img;
    private Graphics2D g;

    public Dbg(boolean enable) {
        this.DEBUG = enable;
    }

    public void init() {
        if (DEBUG) {
            img = new BufferedImage(Map.sz, Map.sz, BufferedImage.TYPE_INT_RGB);
            g = img.createGraphics();
        }
    }

    public void dot(int x, int y, Color clr) {
        if (DEBUG) {
            img.setRGB(x, y, clr.getRGB());
        }
    }

    public void rect(int ax, int ay, int bx, int by, int cx, int cy, int dx, int dy, Color clr) {
        if (DEBUG) {
            g.setColor(clr);
            g.drawLine(ax, ay, bx, by);
            g.drawLine(bx, by, cx, cy);
            g.drawLine(cx, cy, dx, dy);
            g.drawLine(dx, dy, ax, ay);
        }
    }

    public void line(int ax, int ay, int bx, int by, Color clr) {
        if (DEBUG) {
            g.setColor(clr);
            g.drawLine(ax, ay, bx, by);
        }
    }

    public void fill(byte[][] scenegrid) {
        if (DEBUG) {
            for (int i = 0; i < Map.sz; i++) {
                for (int j = 0; j < Map.sz; j++) {
                    if (scenegrid[i][j] == Map.CELL_BLK) {
                        g.setColor(Color.CYAN);
                        g.drawLine(i, j, i, j);
                    } else if (scenegrid[i][j] == Map.CELL_TO) {
                        g.setColor(Color.BLUE);
                        g.drawLine(i, j, i, j);
                    } else if (scenegrid[i][j] == Map.CELL_WP) {
                        g.setColor(Color.RED);
                        g.drawLine(i, j, i, j);
                    } else if (scenegrid[i][j] == 30) {
                        g.setColor(Color.WHITE);
                        g.drawLine(i, j, i, j);
                    }
                }
            }

            String time = new SimpleDateFormat("MMdd_HHmmss").format(Calendar.getInstance().getTime());
            try {
                new File("pf").mkdirs();
                ImageIO.write(img, "png", new File("pf/pf-" + time + "-map"));
            } catch (Exception e) {
            }
        }
    }

    public void save() {
        if (DEBUG) {
            String time = new SimpleDateFormat("MMdd_HHmmss").format(Calendar.getInstance().getTime());
            try {
                new File("pf").mkdirs();
                ImageIO.write(img, "png", new File("pf/pf-" + time));
            } catch (Exception e) {
            }
        }
    }
}
