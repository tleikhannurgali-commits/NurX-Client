import com.nurx.client.AuraMath;

public final class AuraMathTest {
    private static void equal(double actual, double expected) {
        if (Math.abs(actual - expected) > 0.0001)
            throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) {
        equal(AuraMath.wrap(190), -170);
        equal(AuraMath.wrap(-190), 170);
        equal(AuraMath.yaw(0, 0, 0, -4), -180);
        equal(AuraMath.yaw(0, 0, 0, 4), 0);
        equal(AuraMath.pitch(0, 2, 0, 0, 2, 5), 0);
        equal(AuraMath.approachYaw(170, -170, 5), 175);
        equal(AuraMath.approachYaw(-170, 170, 5), -175);
        equal(AuraMath.approachPitch(89, 100, 5), 90);
        if (!AuraMath.withinFov(179, -179, 20)) throw new AssertionError("wraparound FOV");
        if (AuraMath.withinFov(0, 60, 90)) throw new AssertionError("FOV too large");
        System.out.println("AuraMathTest OK");
    }
}
