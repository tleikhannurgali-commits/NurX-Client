import com.nurx.client.ClusterFinder;
import java.util.List;

public final class ClusterFinderTest {
    public static void main(String[] args) {
        List<ClusterFinder.Point> points = List.of(
                new ClusterFinder.Point(50, 60, 50),
                new ClusterFinder.Point(52, 61, 52),
                new ClusterFinder.Point(54, 61, 53),
                new ClusterFinder.Point(250, 63, 250),
                new ClusterFinder.Point(900, 63, 900));
        var clusters = ClusterFinder.find(points, 12, 3);
        if (clusters.size() != 1 || clusters.get(0).size() != 3)
            throw new AssertionError("Expected one cluster of 3: " + clusters);
        if (!ClusterFinder.find(points, 12, 4).isEmpty())
            throw new AssertionError("Minimum count was ignored");
        var border = ClusterFinder.find(List.of(
                new ClusterFinder.Point(30000000, 70, 30000000),
                new ClusterFinder.Point(29999999, 71, 29999999)), 12, 2);
        if (border.size() != 1 || border.get(0).x() != 30000000)
            throw new AssertionError("Overflow or bad center: " + border);
        System.out.println("PASS ClusterFinder basic / minimum / world-border tests");
    }
}
