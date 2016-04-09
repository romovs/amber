package haven.pathfinder;


import haven.*;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Map {
    public final static byte CELL_FREE = 0;
    public final static byte CELL_BLK = 1 << 1;
    public final static byte CELL_WP = 1 << 2;
    public final static byte CELL_SRC = 1 << 3;
    public final static byte CELL_DST = 1 << 4;
    public final static byte CELL_TO = 1 << 6;

    private final static int origintile = 44;
    public final static int origin = origintile * 11;
    public final static int sz = origin * 2;
    public static int plbbox = 3;
    private final static int way = plbbox + 2;
    private final static int clr = way + 1;
    private final static int concaveclr = 2;
    private final static int tomaxside = 33;
    private final static int mapborder = 4;

    private final static int tbbax = -2;
    private final static int tbbay = -2;
    private final static int tbbbx = 2;
    private final static int tbbby = 2;

    private final byte[][] map = new byte[sz][sz];
    private final TraversableObstacle[][] pomap = new TraversableObstacle[sz][sz];
    private final ArrayList<TraversableObstacle> tocandidates = new ArrayList<TraversableObstacle>(300);
    private Coord plc;
    private Coord endc;
    private final MCache mcache;
    private Vertex vxstart;
    private Vertex vxend;

    private Dbg dbg;
    private final static boolean DEBUG = false;
    public final static boolean DEBUG_TIMINGS = false;

    public Map(Coord plc, Coord endc, MCache mcache) {
        this.plc = plc;
        this.endc = endc;
        this.mcache = mcache;
        dbg = new Dbg(DEBUG);
        dbg.init();
    }

    private void initGeography() {
        Coord pltc = new Coord(plc.x / 11, plc.y / 11);

        int dx = (int) (plc.x / 11.0d * 11.0d - pltc.x * 11) - 5;
        int dy = (int) (plc.y / 11.0d * 11.0d - pltc.y * 11) - 5;

        for (int x = -origintile; x < origintile; x++) {
            for (int y = -origintile; y < origintile; y++) {
                int t = mcache.gettile(pltc.sub(x, y));
                Resource res = mcache.tilesetr(t);
                if (res == null)
                    continue;

                String name = res.name;
                if (!name.equals("gfx/tiles/deep") &&
                        !name.equals("gfx/tiles/cave") &&
                        !name.equals("gfx/tiles/nil") &&
                        !name.startsWith("gfx/tiles/rocks/"))
                    continue;

                int gcx = origin - (x * 11) - dx;
                int gcy = origin - (y * 11) - dy;

                // exclude destination tile
                if (endc.x < gcx + tbbax + plbbox && endc.x > gcx + tbbax - plbbox &&
                     endc.y < gcy + tbbby + plbbox && endc.y > gcy + tbbay - plbbox)   {
                    continue;
                }

                // bounding box
                Coord ca = new Coord(gcx + tbbax - plbbox, gcy + tbbay - plbbox);
                Coord cb = new Coord(gcx + tbbbx + plbbox, gcy + tbbay - plbbox);
                Coord cc = new Coord(gcx + tbbbx + plbbox, gcy + tbbby + plbbox);
                Coord cd = new Coord(gcx + tbbax - plbbox, gcy + tbbby + plbbox);

                // calculate waypoints located on the angular bisector of the corner
                int wax = ca.x - 1;
                int way = ca.y - 1;
                int wbx = cb.x + 1;
                int wby = cb.y - 1;
                int wcx = cc.x + 1;
                int wcy = cc.y + 1;
                int wdx = cd.x - 1;
                int wdy = cd.y + 1;

                // exclude tiles near map edges so we won't need to do bounds checks all over the place
                if (wax - mapborder < 0 || way - mapborder < 0 || wax + mapborder >= sz || way + mapborder >= sz ||
                        wbx - mapborder < 0 || wby - mapborder < 0 || wbx + mapborder >= sz || wby + mapborder >= sz ||
                        wcx - mapborder < 0 || wcy - mapborder < 0 || wcx + mapborder >= sz || wcy + mapborder >= sz ||
                        wdx - mapborder < 0 || wdy - mapborder < 0 || wdx + mapborder >= sz || wdy + mapborder >= sz)
                    continue;

                // plot bounding box
                Utils.plotTile(map, ca, cb, cd);

                if (map[wax][way] == CELL_FREE)
                    map[wax][way] = CELL_WP;
                if (map[wbx][wby] == CELL_FREE)
                    map[wbx][wby] = CELL_WP;
                if (map[wcx][wcy] == CELL_FREE)
                    map[wcx][wcy] = CELL_WP;
                if (map[wdx][wdy] == CELL_FREE)
                    map[wdx][wdy] = CELL_WP;

                dbg.rect(ca.x, ca.y, cb.x, cb.y, cc.x, cc.y, cd.x, cd.y, Color.CYAN);
            }
        }

        // outline map edges. FIXME
        for (int i = 0; i < sz; i++) {
            map[i][mapborder] = CELL_BLK;
            map[i][sz - mapborder] = CELL_BLK;
            map[mapborder][i] = CELL_BLK;
            map[sz - mapborder][i] = CELL_BLK;

            dbg.dot(i, mapborder, Color.CYAN);
            dbg.dot(i, sz - mapborder, Color.CYAN);
            dbg.dot(mapborder, i, Color.CYAN);
            dbg.dot(sz - mapborder, i, Color.CYAN);
        }
    }

    public void addGob(Gob gob) {
        GobHitbox.BBox bbox = GobHitbox.getBBox(gob, false);
        if (bbox == null)
            return;

        Coord bboxa = bbox.a;
        Coord bboxb = bbox.b;

        // gob coordinate relative to the origin (player's location)
        int gcx = origin - (plc.x - gob.rc.x);
        int gcy = origin - (plc.y - gob.rc.y);

        // since non 90 degrees incremental rotation is wonky we slightly increase the bounding box for such gobs
        // FIXME: but really should rotate around pixel's center
        int rotadj = 0;
        if (gob.a != 0 && gob.a != Math.PI && gob.a != Math.PI / 2.0 && gob.a != (3 * Math.PI) / 2) {
            rotadj = 1;
        }
        Coord ca, cb, cc, cd, wa, wb, wc, wd, clra, clrb, clrc, clrd;

        // do not rotate square gobs
        // FIXME: rotating rasters realiably to mach actual bounding boxes is very tricky. need better way...
        if (Math.abs(bboxa.x) + Math.abs(bboxb.x) == Math.abs(bboxa.y) + Math.abs(bboxb.y) && rotadj == 0) {
            // bounding box
            ca = new Coord(gcx + bboxa.x - plbbox, gcy + bboxa.y - plbbox);
            cb = new Coord(gcx + bboxb.x + plbbox, gcy + bboxa.y - plbbox);
            cc = new Coord(gcx + bboxb.x + plbbox, gcy + bboxb.y + plbbox);
            cd = new Coord(gcx + bboxa.x - plbbox, gcy + bboxb.y + plbbox);

            // calculate waypoints located on the angular bisector of the corner
            wa = new Coord(gcx + bboxa.x - way, gcy + bboxa.y - way);
            wb = new Coord(gcx + bboxb.x + way, gcy + bboxa.y - way);
            wc = new Coord(gcx + bboxb.x + way, gcy + bboxb.y + way);
            wd = new Coord(gcx + bboxa.x - way, gcy + bboxb.y + way);

            // calculate TO clearance vertices
            clra = new Coord(gcx + bboxa.x - clr, gcy + bboxa.y - clr);
            clrb = new Coord(gcx + bboxb.x + clr, gcy + bboxa.y - clr);
            clrc = new Coord(gcx + bboxb.x + clr, gcy + bboxb.y + clr);
            clrd = new Coord(gcx + bboxa.x - clr, gcy + bboxb.y + clr);
        } else {
            // rotate the bounding box.
            // FIXME: should rotate around pixel's center
            double cos = Math.cos(gob.a);
            double sin = Math.sin(gob.a);
            ca = Utils.rotate(gcx + bboxa.x - plbbox, gcy + bboxa.y - plbbox, gcx, gcy, cos, sin);
            cb = Utils.rotate(gcx + bboxb.x + plbbox, gcy + bboxa.y - plbbox, gcx, gcy, cos, sin);
            cc = Utils.rotate(gcx + bboxb.x + plbbox, gcy + bboxb.y + plbbox, gcx, gcy, cos, sin);
            cd = Utils.rotate(gcx + bboxa.x - plbbox, gcy + bboxb.y + plbbox, gcx, gcy, cos, sin);

            // calculate waypoints located on the angular bisector of the corner
            wa = Utils.rotate(gcx + bboxa.x - way - rotadj, gcy + bboxa.y - way - rotadj, gcx, gcy, cos, sin);
            wb = Utils.rotate(gcx + bboxb.x + way + rotadj, gcy + bboxa.y - way - rotadj, gcx, gcy, cos, sin);
            wc = Utils.rotate(gcx + bboxb.x + way + rotadj, gcy + bboxb.y + way + rotadj, gcx, gcy, cos, sin);
            wd = Utils.rotate(gcx + bboxa.x - way - rotadj, gcy + bboxb.y + way + rotadj, gcx, gcy, cos, sin);

            // calculate TO clearance vertices
            clra = Utils.rotate(gcx + bboxa.x - clr - rotadj, gcy + bboxa.y - clr - rotadj, gcx, gcy, cos, sin);
            clrb = Utils.rotate(gcx + bboxb.x + clr - rotadj, gcy + bboxa.y - clr - rotadj, gcx, gcy, cos, sin);
            clrc = Utils.rotate(gcx + bboxb.x + clr + rotadj, gcy + bboxb.y + clr + rotadj, gcx, gcy, cos, sin);
            clrd = Utils.rotate(gcx + bboxa.x - clr - rotadj, gcy + bboxb.y + clr + rotadj, gcx, gcy, cos, sin);
        }

        // exclude gobs near map edges so we won't need to do bounds checks all over the place
        if (wa.x - mapborder < 0 || wa.y - mapborder < 0 || wa.x + mapborder >= sz || wa.y + mapborder >= sz ||
                wb.x - mapborder < 0 || wb.y - mapborder < 0 || wb.x + mapborder >= sz || wb.y + mapborder >= sz ||
                wc.x - mapborder < 0 || wc.y - mapborder < 0 || wc.x + mapborder >= sz || wc.y + mapborder >= sz ||
                wd.x - mapborder < 0 || wd.y - mapborder < 0 || wd.x + mapborder >= sz || wd.y + mapborder >= sz)
            return;

        if (map[wa.x][wa.y] == CELL_FREE)
            map[wa.x][wa.y] = CELL_WP;
        if (map[wb.x][wb.y] == CELL_FREE)
            map[wb.x][wb.y] = CELL_WP;
        if (map[wc.x][wc.y] == CELL_FREE)
            map[wc.x][wc.y] = CELL_WP;
        if (map[wd.x][wd.y] == CELL_FREE)
            map[wd.x][wd.y] = CELL_WP;

        // plot bounding box
        HashMap<Integer, Utils.MinMax> raster = Utils.plotRect(map, ca, cb, cc, cd, CELL_BLK);

        // store traversable obstacles candidates
        if (bboxb.x <= tomaxside && bboxb.y <= tomaxside)
            tocandidates.add(new TraversableObstacle(wa, wb, wc, wd, clra, clrb, clrc, clrd, raster));

        dbg.rect(ca.x, ca.y, cb.x, cb.y, cc.x, cc.y, cd.x, cd.y, Color.CYAN);
    }

    public void excludeGob(Gob gob) {
        GobHitbox.BBox bbox = GobHitbox.getBBox(gob, false);
        if (bbox == null)
            return;

        Coord bboxa = bbox.a;
        Coord bboxb = bbox.b;

        // gob coordinate relative to the origin (player's location)
        int gcx = origin - (plc.x - gob.rc.x);
        int gcy = origin - (plc.y - gob.rc.y);

        // rotate the bounding box.
        // FIXME: should rotate around pixel's center
        double cos = Math.cos(gob.a);
        double sin = Math.sin(gob.a);
        Coord ca = Utils.rotate(gcx + bboxa.x - plbbox, gcy + bboxa.y - plbbox, gcx, gcy, cos, sin);
        Coord cb = Utils.rotate(gcx + bboxb.x + plbbox, gcy + bboxa.y - plbbox, gcx, gcy, cos, sin);
        Coord cc = Utils.rotate(gcx + bboxb.x + plbbox, gcy + bboxb.y + plbbox, gcx, gcy, cos, sin);
        Coord cd = Utils.rotate(gcx + bboxa.x - plbbox, gcy + bboxb.y + plbbox, gcx, gcy, cos, sin);

        // exclude the gob if it's near map edges so we won't need to do bounds checks all later on
        if (ca.x - mapborder < 0 || ca.y - mapborder < 0 || ca.x + mapborder >= sz || ca.y + mapborder >= sz ||
                cb.x - mapborder < 0 || cb.y - mapborder < 0 || cb.x + mapborder >= sz || cb.y + mapborder >= sz ||
                cc.x - mapborder < 0 || cc.y - mapborder < 0 || cc.x + mapborder >= sz || cc.y + mapborder >= sz ||
                cd.x - mapborder < 0 || cd.y - mapborder < 0 || cd.x + mapborder >= sz || cd.y + mapborder >= sz)
            return;

        Utils.plotRect(map, ca, cb, cc, cd, CELL_FREE);
        dbg.rect(ca.x, ca.y, cb.x, cb.y, cc.x, cc.y, cd.x, cd.y, Color.PINK);
    }

    private void sanitizeWaypoints() {
        for (int i = 0; i < sz; i++) {
            for (int j = 0; j < sz; j++) {
                if (map[i][j] != CELL_WP)
                    continue;

                // remove concave and blocked vertices
                // FIXME: slightly misbehaves with rotated rectangles
                if ((map[i + concaveclr][j] & (CELL_BLK | CELL_TO)) != 0 ||
                        (map[i - concaveclr][j] & (CELL_BLK | CELL_TO)) != 0 ||
                        (map[i][j + concaveclr] & (CELL_BLK | CELL_TO)) != 0 ||
                        (map[i][j - concaveclr] & (CELL_BLK | CELL_TO)) != 0) {
                    map[i][j] = CELL_FREE;
                    continue;
                }
                dbg.dot(i, j, Color.RED);
            }

        /*for (int i = 0; i < sz - 2*plbbox; ) {
            for (int j = 0; j < sz - 2*plbbox; ) {
                int cx =  0;
                int cy =  0;
                int cnt = 0;
                for (int x = 0; x < 2*plbbox; x++) {
                    for (int y = 0; y < 2*plbbox; y++) {
                        if (map[i+x][j+y] == CELL_WP) {
                            cx += i + x;
                            cy += j + y;
                            cnt++;
                        }
                    }
                }

                if (cnt == 0) {
                    j++;
                    continue;
                }

                cx /= cnt;
                cy /= cnt;

                if (doesCollide(cx, cy)) {
                    // find another wp closes to geocenter
                    System.out.println("Geo center collision");
                    break;

                }

                for (int x = 0; x < 2*plbbox; x++) {
                    for (int y = 0; y < 2*plbbox; y++) {
                        if (map[i+x][j+y] == CELL_WP) {
                            map[i+x][j+y] = CELL_FREE;
                        }
                    }
                }
                map[cx][cy] = CELL_WP;
                dbg.dot(cx, cy, Color.YELLOW);

                j+= 2*plbbox;
            }
            i++;
        }
        */
        }
    }

    // identifies all obstacles which can be navigated around
    // TODO: include convex polygons?
    // TODO: yeah this implementation is BAD. needs redoing... flood-fill based approach perhaps?
    private void identTraversableObstacles() {
        for (TraversableObstacle sm : tocandidates) {
            if (!Utils.isVisible(map, dbg, sm.clra.x, sm.clra.y, sm.clrb.x, sm.clrb.y, (byte) (CELL_BLK | CELL_TO)) ||
                    !Utils.isVisible(map, dbg, sm.clrb.x, sm.clrb.y, sm.clrc.x, sm.clrc.y, (byte) (CELL_BLK | CELL_TO)) ||
                    !Utils.isVisible(map, dbg, sm.clrc.x, sm.clrc.y, sm.clrd.x, sm.clrd.y, (byte) (CELL_BLK | CELL_TO)) ||
                    !Utils.isVisible(map, dbg, sm.clrd.x, sm.clrd.y, sm.clra.x, sm.clra.y, (byte) (CELL_BLK | CELL_TO)))
                continue;

            map[sm.wa.x][sm.wa.y] = CELL_FREE;
            map[sm.wb.x][sm.wb.y] = CELL_FREE;
            map[sm.wc.x][sm.wc.y] = CELL_FREE;
            map[sm.wd.x][sm.wd.y] = CELL_FREE;

            for (int y : sm.raster.keySet()) {
                Utils.MinMax mm = sm.raster.get(y);
                for (int x = mm.min; x <= mm.max; x++) {
                    map[x][y] = Map.CELL_TO;
                    pomap[x][y] = sm;
                }
            }
        }
    }

    private List<Vertex> getVertices() {
        List<Vertex> vertices = new ArrayList<Vertex>(300);

        vxstart = new Vertex(origin, origin);
        vertices.add(vxstart);
        map[origin][origin] = CELL_SRC;
        dbg.dot(origin, origin, Color.GREEN);

        vxend = new Vertex(endc.x, endc.y);
        vertices.add(vxend);
        map[endc.x][endc.y] = CELL_DST;
        dbg.dot(endc.x, endc.y, Color.BLUE);

        for (int i = 0; i < sz; i++) {
            for (int j = 0; j < sz; j++) {
                if (map[i][j] == CELL_WP)
                    vertices.add(new Vertex(i, j));
            }
        }

        return vertices;
    }

    private void buildVisGraph(List<Vertex> vertices, byte block) {
        int visedges = 0;
        int edges = 0;
        for (Vertex vert : vertices) {
            for (Vertex vert2 : vertices) {
                if (vert == vert2)
                    continue;

                edges++;
                if (Utils.isVisible(map, dbg, vert.x, vert.y, vert2.x, vert2.y, block)) {
                    visedges++;
                    int dx = vert.x - vert2.x;
                    int dy = vert.y - vert2.y;
                    vert.edges.add(new Edge(vert, vert2, Math.sqrt(dx * dx + dy * dy)));
                }
            }
        }
        if (DEBUG_TIMINGS)
            System.out.println("          Edges: " + visedges + " / " + edges + " (vxs: " + vertices.size() + ")");
    }

    private List<Vertex> recalcVertices(Iterable<Edge> path) {
        List<Vertex> vertices = new ArrayList<Vertex>();
        boolean pathclear = true;

        Iterator<Edge> it = path.iterator();
        while (it.hasNext()) {
            Edge e = it.next();
            dbg.line(e.src.x, e.src.y, e.dest.x, e.dest.y, Color.MAGENTA);

            Set<TraversableObstacle> obs = Utils.getObstructions(pomap, e.src.x, e.src.y, e.dest.x, e.dest.y);
            for (TraversableObstacle o : obs) {
                vertices.add(new Vertex(o.wa.x, o.wa.y));
                vertices.add(new Vertex(o.wb.x, o.wb.y));
                vertices.add(new Vertex(o.wc.x, o.wc.y));
                vertices.add(new Vertex(o.wd.x, o.wd.y));

                vertices.add(new Vertex((o.wa.x + o.wb.x) / 2, (o.wa.y + o.wb.y) / 2));
                vertices.add(new Vertex((o.wb.x + o.wc.x) / 2, (o.wb.y + o.wc.y) / 2));
                vertices.add(new Vertex((o.wc.x + o.wd.x) / 2, (o.wc.y + o.wd.y) / 2));
                vertices.add(new Vertex((o.wd.x + o.wa.x) / 2, (o.wd.y + o.wa.y) / 2));

                dbg.dot(o.wa.x, o.wa.y, Color.PINK);
                dbg.dot(o.wb.x, o.wb.y, Color.PINK);
                dbg.dot(o.wc.x, o.wc.y, Color.PINK);
                dbg.dot(o.wd.x, o.wd.y, Color.PINK);

                dbg.dot((o.wa.x + o.wb.x) / 2, (o.wa.y + o.wb.y) / 2, Color.PINK);
                dbg.dot((o.wb.x + o.wc.x) / 2, (o.wb.y + o.wc.y) / 2, Color.PINK);
                dbg.dot((o.wc.x + o.wd.x) / 2, (o.wc.y + o.wd.y) / 2, Color.PINK);
                dbg.dot((o.wd.x + o.wa.x) / 2, (o.wd.y + o.wa.y) / 2, Color.PINK);
                pathclear = false;
            }

            if (e.src.x == origin && e.src.y == origin)
                continue;

            vertices.add(new Vertex(e.src.x, e.src.y));
        }

        return pathclear ? null : vertices;
    }

    private Iterable<Edge> findPath() {
        Iterable<Edge> path = new AStar().route(vxstart, vxend);

        List<Vertex> vertices = recalcVertices(path);
        if (vertices == null)
            return path;

        vxstart = new Vertex(origin, origin);
        vertices.add(vxstart);
        vxend = new Vertex(endc.x, endc.y);
        vertices.add(vxend);

        buildVisGraph(vertices, (byte) (CELL_BLK | CELL_TO));

        return new AStar().route(vxstart, vxend);
    }

    public Iterable<Edge> main() {
        long start = System.nanoTime();
        initGeography();
        if (DEBUG_TIMINGS)
            System.out.println("            Geography: " + (double) (System.nanoTime() - start) / 1000000.0 + " ms.");

        start = System.nanoTime();
        identTraversableObstacles();
        if (DEBUG_TIMINGS)
            System.out.println("Traversable Obstacles: " + (double) (System.nanoTime() - start) / 1000000.0 + " ms.");

        start = System.nanoTime();
        sanitizeWaypoints();
        if (DEBUG_TIMINGS)
            System.out.println("Vertices Sanitization: " + (double) (System.nanoTime() - start) / 1000000.0 + " ms.");

        // clear area around starting position in case char is on the bounding box boundary
        if (map[origin][origin-1] == CELL_BLK)
            map[origin][origin-1] = CELL_FREE;
        if (map[origin-1][origin-1] == CELL_BLK)
            map[origin-1][origin-1] = CELL_FREE;
        if (map[origin+1][origin-1] == CELL_BLK)
            map[origin+1][origin-1] = CELL_FREE;
        if (map[origin-1][origin] == CELL_BLK)
            map[origin-1][origin] = CELL_FREE;
        if (map[origin+1][origin] == CELL_BLK)
            map[origin+1][origin] = CELL_FREE;
        if (map[origin-1][origin+1] == CELL_BLK)
            map[origin-1][origin+1] = CELL_FREE;
        if (map[origin][origin+1] == CELL_BLK)
            map[origin][origin+1] = CELL_FREE;
        if (map[origin+1][origin+1] == CELL_BLK)
            map[origin+1][origin+1] = CELL_FREE;


        // test if direct path is clear
        if (Utils.isVisible(map, dbg, origin, origin, endc.x, endc.y, (byte) (CELL_BLK | CELL_TO))) {
            List<Edge> clearpath = new ArrayList<>(1);
            clearpath.add(new Edge(new Vertex(origin, origin), new Vertex(endc.x, endc.y), 0));
            if (DEBUG_TIMINGS)
                System.out.println("!!!Clear path found!!!");
            return clearpath;
        }

        // test if direct path blocked only by traversable obstacles
        if (Utils.isVisible(map, dbg, origin, origin, endc.x, endc.y, CELL_BLK)) {
            if (DEBUG_TIMINGS)
                System.out.println("   !!!Only TO block!!!");

            Set<TraversableObstacle> obs = Utils.getObstructions(pomap, origin, origin, endc.x, endc.y);
            List<Vertex> tovertexes = new ArrayList<>();

            vxstart = new Vertex(origin, origin);
            tovertexes.add(vxstart);
            vxend = new Vertex(endc.x, endc.y);
            tovertexes.add(vxend);

            for (TraversableObstacle o : obs) {
                tovertexes.add(new Vertex(o.wa.x, o.wa.y));
                tovertexes.add(new Vertex(o.wb.x, o.wb.y));
                tovertexes.add(new Vertex(o.wc.x, o.wc.y));
                tovertexes.add(new Vertex(o.wd.x, o.wd.y));

                tovertexes.add(new Vertex((o.wa.x + o.wb.x) / 2, (o.wa.y + o.wb.y) / 2));
                tovertexes.add(new Vertex((o.wb.x + o.wc.x) / 2, (o.wb.y + o.wc.y) / 2));
                tovertexes.add(new Vertex((o.wc.x + o.wd.x) / 2, (o.wc.y + o.wd.y) / 2));
                tovertexes.add(new Vertex((o.wd.x + o.wa.x) / 2, (o.wd.y + o.wa.y) / 2));

                dbg.dot(o.wa.x, o.wa.y, Color.PINK);
                dbg.dot(o.wb.x, o.wb.y, Color.PINK);
                dbg.dot(o.wc.x, o.wc.y, Color.PINK);
                dbg.dot(o.wd.x, o.wd.y, Color.PINK);

                dbg.dot((o.wa.x + o.wb.x) / 2, (o.wa.y + o.wb.y) / 2, Color.PINK);
                dbg.dot((o.wb.x + o.wc.x) / 2, (o.wb.y + o.wc.y) / 2, Color.PINK);
                dbg.dot((o.wc.x + o.wd.x) / 2, (o.wc.y + o.wd.y) / 2, Color.PINK);
                dbg.dot((o.wd.x + o.wa.x) / 2, (o.wd.y + o.wa.y) / 2, Color.PINK);
            }

            start = System.nanoTime();
            buildVisGraph(tovertexes, CELL_BLK);
            if (DEBUG_TIMINGS)
                System.out.println("     Visibility Graph: " + (double) (System.nanoTime() - start) / 1000000.0 + " ms.");

            start = System.nanoTime();
            Iterable<Edge> path = findPath();
            if (DEBUG_TIMINGS)
                System.out.println("              Routing: " + (double) (System.nanoTime() - start) / 1000000.0 + " ms.");

            Iterator<Edge> it = path.iterator();
            while (it.hasNext()) {
                Edge e = it.next();
                dbg.line(e.src.x, e.src.y, e.dest.x, e.dest.y, Color.ORANGE);
                dbg.dot(e.src.x, e.src.y, Color.BLUE);
                dbg.dot(e.dest.x, e.dest.y, Color.BLUE);
            }

            return path;
        }

        //---------------------------------------------------------------------------------
        start = System.nanoTime();
        List<Vertex> vertices = getVertices();
        if (DEBUG_TIMINGS)
            System.out.println("   Vertices Retrieval: " + (double) (System.nanoTime() - start) / 1000000.0 + " ms.");

        start = System.nanoTime();
        buildVisGraph(vertices, CELL_BLK);
        if (DEBUG_TIMINGS)
            System.out.println("     Visibility Graph: " + (double) (System.nanoTime() - start) / 1000000.0 + " ms.");

        start = System.nanoTime();
        Iterable<Edge> path = findPath();
        if (DEBUG_TIMINGS)
            System.out.println("              Routing: " + (double) (System.nanoTime() - start) / 1000000.0 + " ms.");

        Iterator<Edge> it = path.iterator();
        while (it.hasNext()) {
            Edge e = it.next();
            dbg.line(e.src.x, e.src.y, e.dest.x, e.dest.y, Color.ORANGE);
            dbg.dot(e.src.x, e.src.y, Color.BLUE);
            dbg.dot(e.dest.x, e.dest.y, Color.BLUE);
        }

        return path;
    }

    public boolean isOriginBlocked() {
        return map[origin][origin] == CELL_BLK || map[origin][origin] == CELL_TO;
    }

    // 3 pixels away from origin
    public Pair<Integer, Integer> getFreeLocation() {
        if (map[origin + 3][origin] == CELL_FREE)
            return new Pair<Integer, Integer>(origin + 3, origin);
        else if (map[origin - 3][origin] == CELL_FREE)
            return new Pair<Integer, Integer>(origin - 3, origin);
        else if (map[origin][origin + 3] == CELL_FREE)
            return new Pair<Integer, Integer>(origin, origin + 3);
        else if (map[origin][origin - 3] == CELL_FREE)
            return new Pair<Integer, Integer>(origin, origin - 3);

        return null;
    }


    public void dbgdump() {
        dbg.save();
        Dbg dbg = new Dbg(DEBUG);
        dbg.init();
        dbg.fill(map);
    }
}
