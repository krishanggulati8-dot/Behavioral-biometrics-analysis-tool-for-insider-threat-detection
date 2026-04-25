import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * ============================================================
 *  POLYMORPHISM + CONCURRENCY — Analysis Engine
 *
 *  Stores analyzers as List<AnalyticTask> — the caller never
 *  needs to know whether it's a SpeedAnalyzer or a
 *  PatternAnalyzer; both are processed identically.
 *
 *  CONCURRENCY: Runs on a dedicated daemon thread. Keystroke
 *  events are placed into a BlockingQueue by the EDT (Event
 *  Dispatch Thread) and consumed here — decoupling UI from
 *  analysis so the GUI never stutters.
 * ============================================================
 */
public class AnalysisEngine implements Runnable {

    // Baseline calibration window: 15 seconds
    private static final long BASELINE_WINDOW_NS = 15L * 1_000_000_000L;
    private static final double ALERT_THRESHOLD   = 0.35;

    // ── Polymorphic list of all analysis engines ─────────────
    private final List<AnalyticTask>         analyzers = new ArrayList<>();

    // Shared data store (injected into both analyzers)
    private final BiometricData              data;

    // Thread-safe queue: EDT produces, this thread consumes
    private final BlockingQueue<Long>        eventQueue = new LinkedBlockingQueue<>();

    // Callbacks to push metrics back to the Swing GUI on the EDT
    private Consumer<AnalysisResult>         onResultCallback;

    // Forensic logger
    private final ForensicsLogger            logger;

    // Phase tracking
    private boolean baselineEstablished = false;
    private long    baselineStartNs     = -1L;
    private boolean running             = false;

    // References to concrete subclasses (for type-specific queries)
    private final SpeedAnalyzer   speedAnalyzer;
    private final PatternAnalyzer patternAnalyzer;

    public AnalysisEngine() {
        this.data           = new BiometricData();
        this.logger         = ForensicsLogger.getInstance();

        // Instantiate concrete analyzers, inject shared data
        this.speedAnalyzer   = new SpeedAnalyzer(data);
        this.patternAnalyzer = new PatternAnalyzer(data);

        // POLYMORPHISM: both stored under AnalyticTask interface
        analyzers.add(speedAnalyzer);
        analyzers.add(patternAnalyzer);

        logger.logInfo("Analysis Engine initialized with " + analyzers.size() + " engines.");
    }

    // ── Public API (called from EDT) ─────────────────────────
    /** Enqueue a keystroke event from the UI thread. Non-blocking. */
    public void submitKeystroke(long timestampNs) {
        eventQueue.offer(timestampNs);
    }

    public void setOnResultCallback(Consumer<AnalysisResult> cb) {
        this.onResultCallback = cb;
    }

    public void start() {
        running = true;
        Thread t = new Thread(this, "BiometricAnalysisThread");
        t.setDaemon(true);   // dies when the main window closes
        t.start();
    }

    public void stop() {
        running = false;
        logger.close();
    }

    public void resetBaseline() {
        data.clear();
        baselineEstablished = false;
        baselineStartNs     = -1L;
        // POLYMORPHISM: reset all analyzers via the interface
        for (AnalyticTask task : analyzers) task.reset();
        logger.logInfo("Baseline reset by user.");
    }

    // ── Runnable — Background Thread ─────────────────────────
    @Override
    public void run() {
        while (running) {
            try {
                // BlockingQueue.take() suspends the thread (no busy-wait)
                // until an event arrives — CPU-friendly concurrency.
                long timestamp = eventQueue.take();
                processEvent(timestamp);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // ── Core processing logic ─────────────────────────────────
    private void processEvent(long timestamp) {
        // Record session start
        if (baselineStartNs < 0) {
            baselineStartNs = timestamp;
            logger.logInfo("Calibration phase started.");
        }

        // POLYMORPHISM: dispatch to all analyzers through the interface
        for (AnalyticTask task : analyzers) {
            task.processKeyStroke(timestamp);
        }

        // Check if baseline window has elapsed
        if (!baselineEstablished) {
            long elapsed = timestamp - baselineStartNs;
            if (elapsed >= BASELINE_WINDOW_NS) {
                baselineEstablished = true;
                // Tell each BaseMonitor to freeze its snapshot
                speedAnalyzer.freezeBaseline();
                patternAnalyzer.freezeBaseline();
                logger.logBaselineEstablished(data.getKeysPerSecond(),
                        data.getAverageKeyInterval());
            }
        }

        // Build result and invoke callback on EDT
        buildAndEmitResult(timestamp);
    }

    private void buildAndEmitResult(long timestamp) {
        double speedScore   = speedAnalyzer.getAnomalyScore();
        double patternScore = patternAnalyzer.getAnomalyScore();
        // Weighted combined score: speed (50%) + pattern (50%)
        double combinedScore = (speedScore + patternScore) / 2.0;

        double wpm = data.getWordsPerMinute();
        double kps = data.getKeysPerSecond();

        RiskLevel risk;
        String logLine;

        if (!baselineEstablished) {
            long remaining = BASELINE_WINDOW_NS
                    - (timestamp - baselineStartNs);
            double remSec = remaining / 1_000_000_000.0;
            risk    = RiskLevel.EVALUATING;
            logLine = String.format("Calibrating... %.1fs remaining | WPM=%.1f", remSec, wpm);

        } else if (combinedScore > ALERT_THRESHOLD) {
            risk    = RiskLevel.UNAUTHORIZED;
            logLine = String.format("HIGH RISK | Score=%.2f | WPM=%.1f | KPS=%.2f",
                    combinedScore, wpm, kps);

            // Log anomaly details from each engine
            for (AnalyticTask task : analyzers) {
                if (task.getAnomalyScore() > ALERT_THRESHOLD) {
                    logger.logAnomaly(task.getEngineName(),
                            task.getAnomalyScore(), wpm, kps);
                }
            }
            logger.logBreach("Combined score=" + String.format("%.4f", combinedScore));

        } else {
            risk    = RiskLevel.AUTHORIZED;
            logLine = String.format("Normal | Score=%.2f | WPM=%.1f | KPS=%.2f",
                    combinedScore, wpm, kps);
        }

        AnalysisResult result = new AnalysisResult(
                wpm, kps, combinedScore,
                speedScore, patternScore,
                risk, logLine, baselineEstablished);

        if (onResultCallback != null) onResultCallback.accept(result);
    }

    // ── Enum: Risk Levels ────────────────────────────────────
    public enum RiskLevel { AUTHORIZED, EVALUATING, UNAUTHORIZED }

    // ── Result record (DTO) ───────────────────────────────────
    public static final class AnalysisResult {
        public final double    wpm, kps, combinedScore, speedScore, patternScore;
        public final RiskLevel risk;
        public final String    logLine;
        public final boolean   baselineEstablished;

        AnalysisResult(double wpm, double kps, double combinedScore,
                       double speedScore, double patternScore,
                       RiskLevel risk, String logLine,
                       boolean baselineEstablished) {
            this.wpm                 = wpm;
            this.kps                 = kps;
            this.combinedScore       = combinedScore;
            this.speedScore          = speedScore;
            this.patternScore        = patternScore;
            this.risk                = risk;
            this.logLine             = logLine;
            this.baselineEstablished = baselineEstablished;
        }
    }
}
