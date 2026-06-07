package util;

public class Timer {
    private long startNanos;

    public void start() {
        startNanos = System.nanoTime();
    }

    public long elapsedNanos() {
        return System.nanoTime() - startNanos;
    }

    public static double toMillis(long nanos) {
        return nanos / 1_000_000.0;
    }
}
