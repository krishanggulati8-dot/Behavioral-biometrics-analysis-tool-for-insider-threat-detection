import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ============================================================
 *  ENCAPSULATION — Biometric Data Container
 *
 *  All raw biometric fields are declared PRIVATE.
 *  External code may only read via safe getters; mutation
 *  goes through validated setters or dedicated add-methods.
 *  This prevents external tampering with forensic data.
 * ============================================================
 */
public final class BiometricData {

    // ── Private fields ───────────────────────────────────────
    /** Time a key is held down (ms). */
    private final List<Long> dwellTimes   = new ArrayList<>();

    /** Time between consecutive key releases (ms). */
    private final List<Long> flightTimes  = new ArrayList<>();

    /** Raw inter-key intervals stored in nanoseconds. */
    private final List<Long> keyIntervals = new ArrayList<>();

    /** Total keystrokes captured so far. */
    private int totalKeystrokes = 0;

    /** Timestamp of the very first keystroke (ns). */
    private long sessionStart = -1L;

    // ── Package-private mutators (only monitors may write) ───
    void addDwellTime(long ms)   { if (ms > 0) dwellTimes.add(ms);   }
    void addFlightTime(long ms)  { if (ms > 0) flightTimes.add(ms);  }
    void addKeyInterval(long ns) { if (ns > 0) keyIntervals.add(ns); }

    void incrementKeystrokes()   { totalKeystrokes++; }

    void setSessionStart(long ns) {
        if (sessionStart < 0) sessionStart = ns;   // set once only
    }

    // ── Public read-only accessors ───────────────────────────
    public List<Long> getDwellTimes()   { return Collections.unmodifiableList(dwellTimes);   }
    public List<Long> getFlightTimes()  { return Collections.unmodifiableList(flightTimes);  }
    public List<Long> getKeyIntervals() { return Collections.unmodifiableList(keyIntervals); }
    public int   getTotalKeystrokes()   { return totalKeystrokes; }
    public long  getSessionStart()      { return sessionStart;    }

    /** Average dwell time in milliseconds, or 0 if no data. */
    public double getAverageDwellTime() {
        return dwellTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    /** Average flight time in milliseconds, or 0 if no data. */
    public double getAverageFlightTime() {
        return flightTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    /** Average inter-key interval in nanoseconds, or 0 if no data. */
    public double getAverageKeyInterval() {
        return keyIntervals.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    /** Standard deviation of key intervals (nanoseconds). */
    public double getKeyIntervalStdDev() {
        if (keyIntervals.size() < 2) return 0.0;
        double mean = getAverageKeyInterval();
        double variance = keyIntervals.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average().orElse(0.0);
        return Math.sqrt(variance);
    }

    /** Session elapsed time in seconds. */
    public double getElapsedSeconds() {
        if (sessionStart < 0) return 0.0;
        return (System.nanoTime() - sessionStart) / 1_000_000_000.0;
    }

    /** Current typing speed in keystrokes per second. */
    public double getKeysPerSecond() {
        double elapsed = getElapsedSeconds();
        return elapsed > 0 ? totalKeystrokes / elapsed : 0.0;
    }

    /** Approximate WPM (assuming average word = 5 characters). */
    public double getWordsPerMinute() {
        double elapsed = getElapsedSeconds();
        if (elapsed <= 0) return 0.0;
        return (totalKeystrokes / 5.0) / (elapsed / 60.0);
    }

    /** Deep-copy snapshot used to freeze the baseline. */
    public BiometricData snapshot() {
        BiometricData snap = new BiometricData();
        snap.dwellTimes.addAll(this.dwellTimes);
        snap.flightTimes.addAll(this.flightTimes);
        snap.keyIntervals.addAll(this.keyIntervals);
        snap.totalKeystrokes = this.totalKeystrokes;
        snap.sessionStart    = this.sessionStart;
        return snap;
    }

    /** Wipe all data (e.g., on user reset). */
    public void clear() {
        dwellTimes.clear();
        flightTimes.clear();
        keyIntervals.clear();
        totalKeystrokes = 0;
        sessionStart    = -1L;
    }
}
