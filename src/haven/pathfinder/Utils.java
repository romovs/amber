package haven.pathfinder;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import haven.Coord;


public class Utils {

    public static Coord rotate(int x, int y, float pivotx, float pivoty, double cos, double sin) {
        float fx = x;
        float fy = y;
        fx -= pivotx;
        fy -= pivoty;
        return new Coord((int) Math.round((fx * cos - fy * sin) + pivotx), (int) Math.round((fx * sin + fy * cos) + pivoty));
    }

    public static class MinMax {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
    }

    public static HashMap<Integer, MinMax> plotRect(byte[][] map, Coord ca, Coord cb, Coord cc, Coord cd, byte celltype) {
        HashMap<Integer, MinMax> raster = new HashMap<Integer, MinMax>();

        // Very naive implementation for plotting rotated rectangles:
        //  1. Draw frame using Bresenham's line algorithm. saving min-max X coordinates pairs along the way.
        //  2. Fill the interior using min-max X coordinate pairs.
        Utils.plotLine(map, raster, ca.x, ca.y, cb.x, cb.y, celltype);
        Utils.plotLine(map, raster, cb.x, cb.y, cc.x, cc.y, celltype);
        Utils.plotLine(map, raster, cd.x, cd.y, cc.x, cc.y, celltype);
        Utils.plotLine(map, raster, ca.x, ca.y, cd.x, cd.y, celltype);

        for (int y : raster.keySet()) {
            MinMax mm = raster.get(y);
            for (int x = mm.min; x < mm.max; x++)
                map[x][y] = celltype;
        }

        return raster;
    }

    public static HashMap<Integer, MinMax> plotRotRect(byte[][] map, Dbg dbg, Coord a, Coord b, int gcx, int gcy, double cos, double sin) {
        HashMap<Integer, MinMax> raster = new HashMap<Integer, MinMax>();

        float pivotx = 0.5f;
        float pivoty = -0.5f;

        for (int x = b.x; x <= a.x; x++) {
            for (int y = b.y; y <= a.y; y++) {

                Coord p = rotate(x, y, pivotx, pivoty, cos, sin);

                map[p.x + gcx][p.y + gcy] = Map.CELL_BLK;
                dbg.dot(p.x + gcx, p.y + gcy, Color.CYAN);
            }
        }

        return raster;
    }

    public static void plotTile(byte[][] map, Coord ca, Coord cb, Coord cd) {
        for (int x = ca.x; x <= cb.x; x++) {
            for (int y = ca.y; y <= cd.y; y++) {
                map[x][y] = Map.CELL_BLK;
            }
        }
    }

    private static void plotLine(byte[][] map, HashMap<Integer, MinMax> vx, int x1, int y1, int x2, int y2, byte val) {
        int d = 0;
        int dy = Math.abs(y2 - y1);
        int dx = Math.abs(x2 - x1);
        int dy2 = (dy << 1);
        int dx2 = (dx << 1);
        int ex = x1 < x2 ? 1 : -1;
        int ey = y1 < y2 ? 1 : -1;

        if (dy <= dx) {
            for (; ; ) {
                map[x1][y1] = val;

                MinMax minmax = vx.get(y1);
                if (minmax == null) {
                    minmax = new MinMax();
                    vx.put(y1, minmax);
                }
                if (x1 < minmax.min)
                    minmax.min = x1;
                if (x1 > minmax.max)
                    minmax.max = x1;

                if (x1 == x2)
                    break;
                x1 += ex;
                d += dy2;
                if (d > dx) {
                    y1 += ey;
                    d -= dx2;
                }
            }
        } else {
            for (; ; ) {
                map[x1][y1] = val;

                MinMax minmax = vx.get(y1);
                if (minmax == null) {
                    minmax = new MinMax();
                    vx.put(y1, minmax);
                }
                if (x1 < minmax.min)
                    minmax.min = x1;
                if (x1 > minmax.max)
                    minmax.max = x1;

                if (y1 == y2)
                    break;
                y1 += ey;
                d += dx2;
                if (d > dy) {
                    x1 += ex;
                    d -= dy2;
                }
            }
        }
    }

    // Bresenham's supercover - http://lifc.univ-fcomte.fr/home/~ededu/projects/bresenham/
    public static boolean isVisible(byte[][] map, Dbg dbg, int x1, int y1, int x2, int y2, byte block) {
        int i;               // loop counter
        int ystep, xstep;    // the step on y and x axis
        int error;           // the error accumulated during the increment
        int errorprev;       // *vision the previous value of the error variable
        int y = y1, x = x1;  // the line points
        int ddy, ddx;        // compulsory variables: the double values of dy and dx
        int dx = x2 - x1;
        int dy = y2 - y1;

        if (dy < 0) {
            ystep = -1;
            dy = -dy;
        } else {
            ystep = 1;
        }

        if (dx < 0) {
            xstep = -1;
            dx = -dx;
        } else {
            xstep = 1;
        }
        ddy = 2 * dy;  // work with double values for full precision
        ddx = 2 * dx;
        if (ddx >= ddy) {  // first octant (0 <= slope <= 1)
            // compulsory initialization (even for errorprev, needed when dx==dy)
            errorprev = error = dx;  // start in the middle of the square
            for (i = 0; i < dx; i++) {  // do not use the first point (already done)
                x += xstep;
                error += ddy;
                if (error > ddx) {  // increment y if AFTER the middle ( > )
                    y += ystep;
                    error -= ddx;
                    // three cases (octant == right->right-top for directions below):
                    if (error + errorprev < ddx) { // bottom square also
                        if ((map[x][y - ystep] & block) != 0)
                            return false;
                    } else if (error + errorprev > ddx) {  // left square also
                        if ((map[x - xstep][y] & block) != 0)
                            return false;
                    } else {  // corner: bottom and left squares also
                        if ((map[x][y - ystep] & block) != 0 || (map[x - xstep][y] & block) != 0)
                            return false;
                    }
                }
                if ((map[x][y] & block) != 0)
                    return false;
                errorprev = error;
            }
        } else {  // the same as above
            errorprev = error = dy;
            for (i = 0; i < dy; i++) {
                y += ystep;
                error += ddx;
                if (error > ddy) {
                    x += xstep;
                    error -= ddy;
                    if (error + errorprev < ddy) {
                        if ((map[x - xstep][y] & block) != 0)
                            return false;
                    } else if (error + errorprev > ddy) {
                        if ((map[x][y - ystep] & block) != 0)
                            return false;
                    } else {
                        if ((map[x - xstep][y] & block) != 0 || (map[x][y - ystep] & block) != 0)
                            return false;
                    }
                }
                if ((map[x][y] & block) != 0)
                    return false;
                errorprev = error;
            }
        }

        return true;
    }

    public static boolean trace(byte[][] map, Dbg dbg, int x1, int y1, int x2, int y2, byte block) {
        int i;               // loop counter
        int ystep, xstep;    // the step on y and x axis
        int error;           // the error accumulated during the increment
        int errorprev;       // *vision the previous value of the error variable
        int y = y1, x = x1;  // the line points
        int ddy, ddx;        // compulsory variables: the double values of dy and dx
        int dx = x2 - x1;
        int dy = y2 - y1;

        if (dy < 0) {
            ystep = -1;
            dy = -dy;
        } else {
            ystep = 1;
        }

        if (dx < 0) {
            xstep = -1;
            dx = -dx;
        } else {
            xstep = 1;
        }
        ddy = 2 * dy;  // work with double values for full precision
        ddx = 2 * dx;
        if (ddx >= ddy) {  // first octant (0 <= slope <= 1)
            // compulsory initialization (even for errorprev, needed when dx==dy)
            errorprev = error = dx;  // start in the middle of the square
            for (i = 0; i < dx; i++) {  // do not use the first point (already done)
                x += xstep;
                error += ddy;
                if (error > ddx) {  // increment y if AFTER the middle ( > )
                    y += ystep;
                    error -= ddx;
                    // three cases (octant == right->right-top for directions below):
                    if (error + errorprev < ddx) { // bottom square also
                        dbg.dot(x, y - ystep, Color.DARK_GRAY);

                    } else if (error + errorprev > ddx) {  // left square also
                        dbg.dot(x - xstep, y, Color.DARK_GRAY);

                    } else {  // corner: bottom and left squares also
                        dbg.dot(x, y - ystep, Color.DARK_GRAY);
                        dbg.dot(x - xstep, y, Color.DARK_GRAY);

                    }
                }
                dbg.dot(x, y, Color.DARK_GRAY);
                errorprev = error;
            }
        } else {  // the same as above
            errorprev = error = dy;
            for (i = 0; i < dy; i++) {
                y += ystep;
                error += ddx;
                if (error > ddy) {
                    x += xstep;
                    error -= ddy;
                    if (error + errorprev < ddy) {
                        dbg.dot(x - xstep, y, Color.DARK_GRAY);
                    } else if (error + errorprev > ddy) {
                        dbg.dot(x, y - ystep, Color.DARK_GRAY);

                    } else {
                        dbg.dot(x - xstep, y, Color.DARK_GRAY);

                        dbg.dot(x, y - ystep, Color.DARK_GRAY);

                    }
                }
                dbg.dot(x, y, Color.DARK_GRAY);

                errorprev = error;
            }
        }

        return true;
    }


    public static Set<TraversableObstacle> getObstructions(TraversableObstacle[][] pomap, int x1, int y1, int x2, int y2) {
        Set<TraversableObstacle> obs = new HashSet<TraversableObstacle>();
        int d = 0;
        int dy = Math.abs(y2 - y1);
        int dx = Math.abs(x2 - x1);
        int dy2 = (dy << 1);
        int dx2 = (dx << 1);
        int ex = x1 < x2 ? 1 : -1;
        int ey = y1 < y2 ? 1 : -1;

        if (dy <= dx) {
            for (; ; ) {
                for (int x = 1; x < Map.plbbox; x++) {
                    for (int y = 1; y < Map.plbbox; y++) {
                        if (pomap[x1 + x][y1 + y] != null)
                            obs.add(pomap[x1 + x][y1 + y]);
                        else if (pomap[x1 - x][y1 + y] != null)
                            obs.add(pomap[x1 - x][y1 + y]);
                        else if (pomap[x1 + x][y1 - y] != null)
                            obs.add(pomap[x1 + x][y1 - y]);
                        else if (pomap[x1 - x][y1 - y] != null)
                            obs.add(pomap[x1 - x][y1 - y]);
                    }
                }
                if (x1 == x2)
                    break;
                x1 += ex;
                d += dy2;
                if (d > dx) {
                    y1 += ey;
                    d -= dx2;
                }
            }
        } else {
            for (; ; ) {
                for (int x = 1; x < Map.plbbox; x++) {
                    for (int y = 1; y < Map.plbbox; y++) {
                        if (pomap[x1 + x][y1 + y] != null)
                            obs.add(pomap[x1 + x][y1 + y]);
                        else if (pomap[x1 - x][y1 + y] != null)
                            obs.add(pomap[x1 - x][y1 + y]);
                        else if (pomap[x1 + x][y1 - y] != null)
                            obs.add(pomap[x1 + x][y1 - y]);
                        else if (pomap[x1 - x][y1 - y] != null)
                            obs.add(pomap[x1 - x][y1 - y]);
                    }
                }
                if (y1 == y2)
                    break;
                y1 += ey;
                d += dx2;
                if (d > dy) {
                    x1 += ex;
                    d -= dy2;
                }
            }
        }

        return obs;
    }
}
