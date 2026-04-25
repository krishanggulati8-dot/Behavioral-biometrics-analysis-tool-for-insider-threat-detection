/**
 * ============================================================
 *  ABSTRACTION — Core Interface
 *  Every analysis engine must implement this contract.
 *  This enforces a uniform API across all analyzers, hiding
 *  their internal complexity from the caller (Dashboard).
 * ============================================================
 */
public interface AnalyticTask {

    /**
     * Process a single keystroke event.
     * @param timestamp  System.nanoTime() of the key press (nanoseconds)
     */
    void processKeyStroke(long timestamp);

    /**
     * Compute and return a normalized anomaly score in [0.0, 1.0].
     * 0.0 = perfectly matches baseline; 1.0 = completely anomalous.
     */
    double getAnomalyScore();

    /**
     * Human-readable name of this analysis engine.
     */
    String getEngineName();

    /**
     * Reset the analyzer to a fresh state (called on baseline reset).
     */
    void reset();
}
