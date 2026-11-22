import com.nurx.client.Mod;

/** Runs without Minecraft: javac -d /tmp/nurx-test src/main/java/com/nurx/client/Mod.java tests/ModSmokeTest.java && java -cp /tmp/nurx-test ModSmokeTest */
public final class ModSmokeTest {
    public static void main(String[] args) {
        Mod m = new Mod("Example", "Test module", Mod.Category.COMBAT, false, 82)
                .slider("Range", 3.0, 2.0, 3.2, 0.1);
        if (Math.abs(m.value("Range") - 3.0) > 0.00001) throw new AssertionError("bad default");
        m.settings().get(0).set(999);
        if (Math.abs(m.value("Range") - 3.2) > 0.00001) throw new AssertionError("upper limit");
        m.settings().get(0).set(-999);
        if (Math.abs(m.value("Range") - 2.0) > 0.00001) throw new AssertionError("lower limit");
        System.out.println("Mod settings smoke test passed");
    }
}
