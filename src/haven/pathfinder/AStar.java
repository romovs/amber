package haven.pathfinder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;


public class AStar {

    public Iterable<Edge> route(Vertex start, Vertex end) {
        Node dest = findShortestPath(new Path(start, end));
        LinkedList<Edge> path = new LinkedList<Edge>();
        if (dest != null) {
            while (dest.edge != null) {
                path.addFirst(dest.edge);
                dest = dest.prev;
            }
        }
        return path;
    }

    private Node findShortestPath(Path p) {
        int order = 0;

        Set<Node> closed = new HashSet<Node>();
        PriorityQueue<Node> open = new PriorityQueue<Node>();

        Node current = p.node(p.start);
        current.reset(null, null, 0, order++);
        open.add(current);

        while (!open.isEmpty()) {
            current = open.remove();

            if (current.vertex.equals(p.end))
                return current;

            closed.add(current);
            for (Edge edge : current.vertex.edges) {
                Node n = p.node(edge.dest);

                double g = current.g + edge.weight;
                double f = g + n.h;

                if (closed.contains(n) && f >= n.f())
                    continue;

                if (!open.contains(n) || f < n.f()) {
                    open.remove(n);
                    n.reset(current, edge, g, order++);
                    open.add(n);
                }
            }
        }
        return null;
    }

    private static class Path {
        private final Vertex start;
        private final Vertex end;
        private final Map<Vertex, Node> nodes;

        public Path(Vertex start, Vertex end) {
            this.start = start;
            this.end = end;
            nodes = new HashMap<Vertex, Node>();
        }

        private Node node(Vertex vertex) {
            Node n = nodes.get(vertex);
            if (n == null) {
                n = new Node(vertex, heuristic(vertex, end));
                nodes.put(vertex, n);
            }
            return n;
        }

        private double heuristic(Vertex a, Vertex b) {
            double dx = a.x - b.x;
            double dy = a.y - b.y;
            return Math.sqrt(dx * dx + dy * dy);
        }
    }

    private static class Node implements Comparable<Node> {
        private final Vertex vertex;
        private final double h;
        private Node prev;
        private Edge edge;
        private double g;
        private int order;

        public Node(Vertex vertex, double h) {
            this.vertex = vertex;
            this.h = h;
        }

        private void reset(Node prev, Edge edge, double g, int order) {
            this.prev = prev;
            this.edge = edge;
            this.g = g;
            this.order = order;
        }

        private double f() {
            return g + h;
        }

        @Override
        public int compareTo(Node n) {
            if (this == n)
                return 0;

            int diff = (int) Math.signum(f() - n.f());

            if (diff == 0)
                diff = (int) Math.signum(h - n.h);

            if (diff == 0)
                diff = order = n.order;

            return diff;
        }
    }
}
