/**
 * ============================================================
 *  INHERITANCE — Abstract Base Monitor
 *
 *  Defines shared state and common behavior for ALL analyzers.
 *  Child classes (SpeedAnalyzer, PatternAnalyzer) extend this
 *  and provide their own concrete implementations of the
 *  abstract methods declared here.
 *
 *  Template-Method pattern: processKeyStroke() calls the
 *  abstract hook onKeystrokeReceived() so subclasses can
 *  plug in their logic without overriding the whole pipeline.
 * ============================================================
 */
public abstract class BaseMonitor implements AnalyticTask {

    // Shared, protected biometric store (subclasses can read)
    protected final BiometricData data;

    // Frozen baseline captured after the calibration window
    protected BiometricData baseline = null;

    // Whether calibration is complete
    protected boolean baselineEstablished = false;

    // Track previous keystroke timestamp (ns) for interval math
    private long lastKeyTimestamp = -1L;

    /**
     * All subclasses must call super(data) to inject the shared
     * data object — ensures both analyzers see the same stream.
     */
    protected BaseMonitor(BiometricData data) {
        this.data = data;
    }

    // ── Template Method ──────────────────────────────────────
    @Override
    public final void processKeyStroke(long timestamp) {
        // Update shared biometric store
        data.setSessionStart(timestamp);
        data.incrementKeystrokes();

        if (lastKeyTimestamp > 0) {
            long intervalNs = timestamp - lastKeyTimestamp;
            data.addKeyInterval(intervalNs);
            // Convert to ms for flight time (approximate; no key-up events here)
            data.addFlightTime(intervalNs / 1_000_000);
        }
        lastKeyTimestamp = timestamp;

        // Delegate to the concrete subclass hook
        onKeystrokeReceived(timestamp);
    }

    /**
     * Hook for subclasses to react to each keystroke.
     * Called after the base class has updated shared state.
     */
    protected abstract void onKeystrokeReceived(long timestamp);

    // ── Baseline management (shared across subclasses) ───────
    /** Called by the orchestrator once the calibration window expires. */
    public void freezeBaseline() {
        this.baseline = data.snapshot();
        this.baselineEstablished = true;
    }

    public boolean isBaselineEstablished() { return baselineEstablished; }

    @Override
    public void reset() {
        baseline = null;
        baselineEstablished = false;
        lastKeyTimestamp = -1L;
    }
}
