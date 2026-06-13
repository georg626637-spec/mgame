public class Camera {
    public double yaw, pitch;
    public double fov = Math.PI / 2.5;

    public void rotate(double dyaw, double dpitch) {
        yaw += dyaw;
        pitch -= dpitch;
        pitch = Math.max(-Math.PI / 2 + 0.05, Math.min(Math.PI / 2 - 0.05, pitch));
    }

    public double forwardX() { return Math.sin(yaw) * Math.cos(pitch); }
    public double forwardY() { return Math.sin(pitch); }
    public double forwardZ() { return Math.cos(yaw) * Math.cos(pitch); }
    public double rightX() { return Math.cos(yaw); }
    public double rightZ() { return -Math.sin(yaw); }
    public double upX() { return -Math.sin(yaw) * Math.sin(pitch); }
    public double upY() { return Math.cos(pitch); }
    public double upZ() { return -Math.cos(yaw) * Math.sin(pitch); }
}
