import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ============================================================
 *  ENCAPSULATION — Forensics Logger
 *
 *  All file I/O internals are hidden behind a clean API.
 *  External callers simply call logAnomaly() / logInfo();
 *  they never touch the PrintWriter directly.
 * ============================================================
 */
public class ForensicsLogger {

    private static final String LOG_FILE = "security_breach_report.log";
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // Singleton — one log file per session
    private static ForensicsLogger instance;

    private PrintWriter writer;

    private ForensicsLogger() {
        try {
            // true = append mode; preserves history across restarts
            writer = new PrintWriter(new FileWriter(LOG_FILE, true));
            writeHeader();
        } catch (IOException e) {
            System.err.println("[ForensicsLogger] Cannot open log file: " + e.getMessage());
        }
    }

    public static synchronized ForensicsLogger getInstance() {
        if (instance == null) instance = new ForensicsLogger();
        return instance;
    }

    // ── Public API ───────────────────────────────────────────
    public void logInfo(String message) {
        write("INFO   ", message);
    }

    public void logAnomaly(String engine, double score, double wpm, double kps) {
        String msg = String.format(
                "ENGINE=%-20s | SCORE=%.4f | WPM=%.1f | KPS=%.2f",
                engine, score, wpm, kps);
        write("ANOMALY", msg);
        flush();
    }

    public void logBreach(String details) {
        write("BREACH ", "*** HIGH-RISK ALERT *** " + details);
        flush();
    }

    public void logBaselineEstablished(double avgKps, double avgInterval) {
        write("BASELINE",
                String.format("Baseline frozen | KPS=%.2f | AvgInterval=%.2f ns",
                        avgKps, avgInterval));
        flush();
    }

    public void close() {
        if (writer != null) {
            write("INFO   ", "Session closed.");
            writer.println("═".repeat(90));
            writer.flush();
            writer.close();
        }
    }

    // ── Private helpers ──────────────────────────────────────
    private void write(String level, String message) {
        if (writer == null) return;
        writer.printf("[%s] [%s] %s%n",
                LocalDateTime.now().format(FMT), level, message);
    }

    private void flush() {
        if (writer != null) writer.flush();
    }

    private void writeHeader() {
        writer.println();
        writer.println("═".repeat(90));
        writer.printf("  BEHAVIORAL BIOMETRICS SECURITY REPORT  —  Session: %s%n",
                LocalDateTime.now().format(FMT));
        writer.println("═".repeat(90));
        writer.flush();
    }
}
