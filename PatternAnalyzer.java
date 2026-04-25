import java.util.List;

/**
 * ============================================================
 *  INHERITANCE — Child Class 2: Pattern Analyzer
 *
 *  Inherits from BaseMonitor and specializes in rhythmic
 *  consistency (keystroke interval standard deviation).
 *  A sudden change in typing rhythm — even at the same speed —
 *  is a strong indicator of a different user.
 *
 *  POLYMORPHISM: Treated uniformly with SpeedAnalyzer through
 *  the List<AnalyticTask> interface in the orchestrator.
 * ============================================================
 */
public class PatternAnalyzer extends BaseMonitor {

    private static final double VARIANCE_THRESHOLD = 0.35;

    private double anomalyScore = 0.0;

    // Tracks digraph (two-key) rhythm deviation
    private double rhythmDeviation = 0.0;

    public PatternAnalyzer(BiometricData data) {
        super(data);    // Inherit shared biometric data
    }

    // ── Hook implementation ──────────────────────────────────
    @Override
    protected void onKeystrokeReceived(long timestamp) {
        if (!baselineEstablished) return;

        // Compare inter-key interval standard deviation
        double baselineStdDev = baseline.getKeyIntervalStdDev();
        double currentStdDev  = data.getKeyIntervalStdDev();

        if (baselineStdDev <= 0) {
            anomalyScore = 0.0;
            return;
        }

        // Also compare mean interval shift (overall tempo change)
        double baselineMean = baseline.getAverageKeyInterval();
        double currentMean  = data.getAverageKeyInterval();

        double stdDevDeviation = Math.abs(currentStdDev - baselineStdDev) / baselineStdDev;
        double meanDeviation   = baselineMean > 0
                ? Math.abs(currentMean - baselineMean) / baselineMean
                : 0.0;

        // Weighted combination: rhythm (60%) + mean tempo (40%)
        rhythmDeviation = 0.6 * stdDevDeviation + 0.4 * meanDeviation;
        anomalyScore    = Math.min(rhythmDeviation, 1.0);
    }

    // ── AnalyticTask contract ────────────────────────────────
    @Override
    public double getAnomalyScore() { return anomalyScore; }

    @Override
    public String getEngineName() { return "Pattern Analyzer"; }

    @Override
    public void reset() {
        super.reset();
        anomalyScore    = 0.0;
        rhythmDeviation = 0.0;
    }

    /** True when rhythm deviation exceeds alert threshold. */
    public boolean isRhythmAlertActive() {
        return baselineEstablished && anomalyScore > VARIANCE_THRESHOLD;
    }

    /** Expose raw rhythm deviation for detailed forensic logging. */
    public double getRhythmDeviation() { return rhythmDeviation; }
}
