package com.nurx.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Pure Java connected-components clustering. Never invents positions outside supplied input. */
public final class ClusterFinder {
    private ClusterFinder() { }

    public record Point(int x, int y, int z) { }
    public record Cluster(int x, int y, int z, int size) { }

    public static List<Cluster> find(List<Point> points, int linkRadius, int minCount) {
        List<Cluster> clusters = new ArrayList<>();
        boolean[] visited = new boolean[points.size()];
        long max = (long) linkRadius * linkRadius;
        for (int seed = 0; seed < points.size(); seed++) {
            if (visited[seed]) continue;
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(seed);
            visited[seed] = true;
            long totalX = 0, totalY = 0, totalZ = 0;
            int count = 0;
            while (!queue.isEmpty()) {
                int index = queue.removeFirst();
                Point p = points.get(index);
                totalX += p.x(); totalY += p.y(); totalZ += p.z(); count++;
                for (int other = 0; other < points.size(); other++) {
                    if (visited[other]) continue;
                    Point q = points.get(other);
                    long dx = (long) p.x() - q.x(), dy = (long) p.y() - q.y(), dz = (long) p.z() - q.z();
                    if (dx * dx + dy * dy + dz * dz <= max) {
                        visited[other] = true;
                        queue.addLast(other);
                    }
                }
            }
            if (count >= minCount) {
                clusters.add(new Cluster((int) Math.round((double) totalX / count),
                        (int) Math.round((double) totalY / count), (int) Math.round((double) totalZ / count), count));
            }
        }
        clusters.sort((a, b) -> Integer.compare(b.size(), a.size()));
        return clusters;
    }
}
