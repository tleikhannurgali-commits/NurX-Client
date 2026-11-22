package com.nurx.client;

/**
 * Rotation and target-choice math shared by the combat modules.
 * No Minecraft dependencies: intentionally testable with an ordinary JDK.
 */
public final class AuraMath {
    private AuraMath() { }

    public static float wrap(float angle) {
        float wrapped = angle % 360.0f;
        if (wrapped >= 180.0f) wrapped -= 360.0f;
        if (wrapped < -180.0f) wrapped += 360.0f;
        return wrapped;
    }

    public static float yaw(double fromX, double fromZ, double toX, double toZ) {
        return (float) (Math.toDegrees(Math.atan2(toZ - fromZ, toX - fromX)) - 90.0);
    }

    public static float pitch(double fromX, double fromY, double fromZ,
                              double toX, double toY, double toZ) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        return (float) -Math.toDegrees(Math.atan2(toY - fromY, Math.hypot(dx, dz)));
    }

    /** Move a rotation at most maxStep degrees without taking the long way around. */
    public static float approachYaw(float current, float desired, float maxStep) {
        float difference = wrap(desired - current);
        return current + clamp(difference, -maxStep, maxStep);
    }

    public static float approachPitch(float current, float desired, float maxStep) {
        return clamp(current + clamp(desired - current, -maxStep, maxStep), -90f, 90f);
    }

    public static boolean withinFov(float currentYaw, float desiredYaw, double degrees) {
        return Math.abs(wrap(desiredYaw - currentYaw)) <= degrees * 0.5;
    }

    private static float clamp(float value, float low, float high) {
        return Math.max(low, Math.min(high, value));
    }
}
