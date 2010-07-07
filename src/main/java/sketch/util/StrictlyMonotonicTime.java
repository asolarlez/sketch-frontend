package sketch.util;

public class StrictlyMonotonicTime {
    public double lastTime = 0;
    public double subCtr = 0;
    public final double precision;

    /**
     * @param precision
     *            A fraction representing displayed rounded digits, e.g. 0.001 if you want
     *            to print out times with "%.3f" and have the times strictly monotonic
     *            (even if not 100% accurate).
     */
    public StrictlyMonotonicTime(double precision) {
        this.precision = precision;
    }

    public double getTime() {
        final double time = ((double) System.currentTimeMillis()) / 1000.;
        assert time >= lastTime : "system clock not monotonic!";
        if (lastTime == time) {
            subCtr = subCtr + precision;

            // avoid excessive slew
            if (subCtr - lastTime > 0.5) {
                try {
                    Thread.sleep((long) (1000 * (subCtr - lastTime)));
                } catch (InterruptedException e) {}
            }
        } else {
            subCtr = time;
            lastTime = time;
        }
        return subCtr;
    }
}
