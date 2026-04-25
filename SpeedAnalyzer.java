/**
 * ============================================================
 *  INHERITANCE — Child Class 1: Speed Analyzer
 *
 *  Inherits shared biometric data and baseline management from
 *  BaseMonitor. Focuses exclusively on WPM / KPS deviation.
 *
 *  POLYMORPHISM: Stored in List<AnalyticTask> alongside
 *  PatternAnalyzer. The orchestrator calls processKeyStroke()
 *  on each without knowing the concrete type.
 * ============================================================
 */
public class SpeedAnalyzer extends BaseMonitor {

    // Rolling window for short-term speed calculation (last N intervals)
    private static final int WINDOW_SIZE = 10;

    // Variance threshold that triggers a speed alert (35% = 0.35)
    private static final double VARIANCE_THRESHOLD = 0.35;

    private double anomalyScore = 0.0;

    public SpeedAnalyzer(BiometricData data) {
        super(data);    // Call base constructor — Inheritance in action
    }

    // ── Hook implementation ──────────────────────────────────
    @Override
    protected void onKeystrokeReceived(long timestamp) {
        if (!baselineEstablished) return;   // still in calibration phase

        // Compare current rolling KPS against baseline KPS
        double baselineKps = baseline.getKeysPerSecond();
        double currentKps  = data.getKeysPerSecond();

        if (baselineKps <= 0) return;

        double deviation = Math.abs(currentKps - baselineKps) / baselineKps;
        // Normalize: clamp to [0, 1]
        anomalyScore = Math.min(deviation, 1.0);
    }

    // ── AnalyticTask contract ────────────────────────────────
    @Override
    public double getAnomalyScore() { return anomalyScore; }

    @Override
    public String getEngineName() { return "Speed Analyzer"; }

    @Override
    public void reset() {
        super.reset();
        anomalyScore = 0.0;
    }

    /** Returns true when speed variance exceeds the defined threshold. */
    public boolean isSpeedAlertActive() {
        return baselineEstablished && anomalyScore > VARIANCE_THRESHOLD;
    }
}
