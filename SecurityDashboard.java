import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * ============================================================
 *  MAIN GUI — Security Dashboard (Java Swing)
 *
 *  All GUI updates happen on the Event Dispatch Thread (EDT)
 *  via SwingUtilities.invokeLater() — required for thread safety.
 *
 *  Architecture summary:
 *    • AnalyticTask   — Abstraction (interface)
 *    • BaseMonitor    — Inheritance (abstract base)
 *    • SpeedAnalyzer  — Inheritance (child 1)
 *    • PatternAnalyzer— Inheritance (child 2)
 *    • BiometricData  — Encapsulation
 *    • AnalysisEngine — Polymorphism + Concurrency
 *    • SecurityDashboard — Swing GUI entry point
 * ============================================================
 */
public class SecurityDashboard extends JFrame {

    // ── Colors ───────────────────────────────────────────────
    private static final Color BG_DARK      = new Color(10, 14, 26);
    private static final Color BG_PANEL     = new Color(16, 22, 40);
    private static final Color BG_CARD      = new Color(22, 30, 52);
    private static final Color ACCENT_BLUE  = new Color(0, 180, 255);
    private static final Color GREEN        = new Color(0, 230, 130);
    private static final Color YELLOW       = new Color(255, 200, 0);
    private static final Color RED          = new Color(255, 55, 90);
    private static final Color TEXT_PRIMARY = new Color(220, 230, 255);
    private static final Color TEXT_DIM     = new Color(100, 120, 170);

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    // ── UI Components ─────────────────────────────────────────
    private JPanel      riskStatusPanel;
    private JLabel      riskLabel;
    private JLabel      wpmLabel, kpsLabel, anomalyLabel;
    private JLabel      speedScoreLabel, patternScoreLabel;
    private JLabel      baselineStatusLabel;
    private JTextArea   captureZone;
    private JTextArea   securityLog;
    private JButton     resetButton;
    private JProgressBar anomalyBar;

    // ── Backend ───────────────────────────────────────────────
    private final AnalysisEngine engine;

    // ── Entry Point ───────────────────────────────────────────
    public static void main(String[] args) {
        // Ensure GUI creation happens on the EDT
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new SecurityDashboard().setVisible(true);
        });
    }

    // ── Constructor ───────────────────────────────────────────
    public SecurityDashboard() {
        engine = new AnalysisEngine();
        initUI();
        wireBackend();
        engine.start();
        appendLog("System", "Behavioral Biometrics Engine started.");
        appendLog("System", "Begin typing to enter 15-second calibration phase.");
    }

    // ── UI Construction ───────────────────────────────────────
    private void initUI() {
        setTitle("Behavioral Biometrics Analysis Tool — Security Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 780);
        setLocationRelativeTo(null);
        setBackground(BG_DARK);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(BG_DARK);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        root.add(buildHeader(),         BorderLayout.NORTH);
        root.add(buildCenterPanel(),    BorderLayout.CENTER);
        root.add(buildLogPanel(),       BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ── Header ────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_PANEL);
        header.setBorder(new CompoundBorder(
                new LineBorder(ACCENT_BLUE, 1),
                new EmptyBorder(10, 16, 10, 16)));

        JLabel title = styledLabel("⬡  BEHAVIORAL BIOMETRICS SECURITY DASHBOARD", 18f, Font.BOLD, ACCENT_BLUE);
        JLabel sub   = styledLabel("Inside Threat Detection System  |  Real-Time Keystroke Analysis", 11f, Font.PLAIN, TEXT_DIM);

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 2));
        left.setOpaque(false);
        left.add(title);
        left.add(sub);

        baselineStatusLabel = styledLabel("● AWAITING INPUT", 12f, Font.BOLD, YELLOW);
        resetButton = new JButton("⟳  RESET BASELINE");
        styleButton(resetButton, ACCENT_BLUE);
        resetButton.addActionListener(e -> onResetClicked());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(baselineStatusLabel);
        right.add(resetButton);

        header.add(left,  BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    // ── Center: metrics + capture zone ───────────────────────
    private JPanel buildCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);

        center.add(buildMetricsRow(),   BorderLayout.NORTH);
        center.add(buildRiskAndCapture(), BorderLayout.CENTER);

        return center;
    }

    // Metrics row: WPM | KPS | Anomaly Score
    private JPanel buildMetricsRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 10, 0));
        row.setOpaque(false);

        wpmLabel     = styledLabel("0.0", 36f, Font.BOLD, GREEN);
        kpsLabel     = styledLabel("0.00", 36f, Font.BOLD, ACCENT_BLUE);
        anomalyLabel = styledLabel("0.000", 36f, Font.BOLD, YELLOW);

        row.add(buildMetricCard("WPM",   "Words Per Minute",     wpmLabel));
        row.add(buildMetricCard("KPS",   "Keys Per Second",      kpsLabel));
        row.add(buildMetricCard("SCORE", "Anomaly Score (0–1)",  anomalyLabel));

        return row;
    }

    private JPanel buildMetricCard(String title, String sub, JLabel valueLabel) {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(BG_CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(40, 60, 100), 1),
                new EmptyBorder(12, 16, 12, 16)));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0; g.anchor = GridBagConstraints.WEST;
        card.add(styledLabel(title, 11f, Font.BOLD, TEXT_DIM), g);
        g.gridy = 1; card.add(valueLabel, g);
        g.gridy = 2; card.add(styledLabel(sub, 10f, Font.PLAIN, TEXT_DIM), g);
        return card;
    }

    // Risk panel on the left + capture zone on the right
    private JSplitPane buildRiskAndCapture() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildRiskPanel(), buildCapturePanel());
        split.setDividerLocation(300);
        split.setDividerSize(6);
        split.setBorder(null);
        split.setOpaque(false);
        split.setBackground(BG_DARK);
        return split;
    }

    // Risk Status Panel
    private JPanel buildRiskPanel() {
        JPanel outer = new JPanel(new BorderLayout(0, 8));
        outer.setBackground(BG_PANEL);
        outer.setBorder(new CompoundBorder(
                new LineBorder(new Color(40, 60, 100), 1),
                new EmptyBorder(12, 12, 12, 12)));

        JLabel header = styledLabel("RISK STATUS", 12f, Font.BOLD, TEXT_DIM);

        riskStatusPanel = new JPanel(new BorderLayout());
        riskStatusPanel.setBackground(new Color(20, 40, 20));
        riskStatusPanel.setBorder(new EmptyBorder(20, 10, 20, 10));
        riskStatusPanel.setPreferredSize(new Dimension(240, 120));

        riskLabel = styledLabel("EVALUATING", 22f, Font.BOLD, YELLOW);
        riskLabel.setHorizontalAlignment(SwingConstants.CENTER);
        riskStatusPanel.add(riskLabel, BorderLayout.CENTER);

        // Anomaly progress bar
        anomalyBar = new JProgressBar(0, 100);
        anomalyBar.setValue(0);
        anomalyBar.setForeground(GREEN);
        anomalyBar.setBackground(new Color(30, 40, 60));
        anomalyBar.setStringPainted(true);
        anomalyBar.setString("Anomaly: 0%");
        anomalyBar.setFont(new Font("Monospaced", Font.PLAIN, 11));
        anomalyBar.setBorderPainted(false);
        anomalyBar.setPreferredSize(new Dimension(240, 24));

        // Sub-scores
        speedScoreLabel   = styledLabel("Speed Score:   0.000", 11f, Font.PLAIN, TEXT_DIM);
        patternScoreLabel = styledLabel("Pattern Score: 0.000", 11f, Font.PLAIN, TEXT_DIM);
        JPanel subScores  = new JPanel(new GridLayout(2, 1, 0, 4));
        subScores.setOpaque(false);
        subScores.add(speedScoreLabel);
        subScores.add(patternScoreLabel);

        outer.add(header,          BorderLayout.NORTH);
        outer.add(riskStatusPanel, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridLayout(3, 1, 0, 6));
        bottom.setOpaque(false);
        bottom.add(anomalyBar);
        bottom.add(speedScoreLabel);
        bottom.add(patternScoreLabel);
        outer.add(bottom, BorderLayout.SOUTH);

        return outer;
    }

    // Capture Zone (text input)
    private JPanel buildCapturePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PANEL);
        panel.setBorder(new CompoundBorder(
                new LineBorder(new Color(40, 60, 100), 1),
                new EmptyBorder(12, 12, 12, 12)));

        JLabel label = styledLabel("▶  CAPTURE ZONE — Begin typing here", 12f, Font.BOLD, TEXT_DIM);

        captureZone = new JTextArea();
        captureZone.setBackground(new Color(12, 18, 35));
        captureZone.setForeground(TEXT_PRIMARY);
        captureZone.setCaretColor(ACCENT_BLUE);
        captureZone.setFont(new Font("Monospaced", Font.PLAIN, 14));
        captureZone.setLineWrap(true);
        captureZone.setWrapStyleWord(true);
        captureZone.setBorder(new EmptyBorder(8, 8, 8, 8));
        captureZone.setToolTipText("Type here — every keystroke is analyzed");

        JScrollPane scroll = new JScrollPane(captureZone);
        scroll.setBorder(new LineBorder(new Color(40, 60, 100), 1));
        scroll.setBackground(BG_PANEL);

        panel.add(label,  BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ── Log Panel ─────────────────────────────────────────────
    private JPanel buildLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PANEL);
        panel.setPreferredSize(new Dimension(0, 170));
        panel.setBorder(new CompoundBorder(
                new LineBorder(new Color(40, 60, 100), 1),
                new EmptyBorder(8, 12, 8, 12)));

        JLabel header = styledLabel("■  LIVE SECURITY LOG", 11f, Font.BOLD, TEXT_DIM);

        securityLog = new JTextArea();
        securityLog.setBackground(new Color(8, 12, 22));
        securityLog.setForeground(new Color(0, 220, 120));
        securityLog.setFont(new Font("Monospaced", Font.PLAIN, 11));
        securityLog.setEditable(false);
        securityLog.setBorder(new EmptyBorder(4, 6, 4, 6));

        // Auto-scroll to bottom
        DefaultCaret caret = (DefaultCaret) securityLog.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JScrollPane scroll = new JScrollPane(securityLog);
        scroll.setBorder(null);
        scroll.setBackground(BG_PANEL);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ── Wire up backend callbacks and key listener ────────────
    private void wireBackend() {
        // Key listener on the capture zone: submit each keystroke to the engine
        captureZone.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // Ignore modifier-only presses
                if (e.getKeyCode() == KeyEvent.VK_SHIFT   ||
                    e.getKeyCode() == KeyEvent.VK_CONTROL ||
                    e.getKeyCode() == KeyEvent.VK_ALT     ||
                    e.getKeyCode() == KeyEvent.VK_META) return;

                engine.submitKeystroke(System.nanoTime());
            }
        });

        // Analysis result callback — always called on the analysis thread,
        // so we marshal the UI update back to the EDT via invokeLater()
        engine.setOnResultCallback(result ->
                SwingUtilities.invokeLater(() -> updateUI(result)));
    }

    // ── UI Updater (always runs on EDT) ───────────────────────
    private void updateUI(AnalysisEngine.AnalysisResult r) {
        // Metrics
        wpmLabel.setText(String.format("%.1f", r.wpm));
        kpsLabel.setText(String.format("%.2f", r.kps));
        anomalyLabel.setText(String.format("%.3f", r.combinedScore));
        speedScoreLabel.setText(  String.format("Speed Score:   %.3f", r.speedScore));
        patternScoreLabel.setText(String.format("Pattern Score: %.3f", r.patternScore));

        // Progress bar
        int pct = (int) (r.combinedScore * 100);
        anomalyBar.setValue(pct);
        anomalyBar.setString("Anomaly: " + pct + "%");

        // Baseline status
        if (!r.baselineEstablished) {
            baselineStatusLabel.setText("● CALIBRATING...");
            baselineStatusLabel.setForeground(YELLOW);
        } else {
            baselineStatusLabel.setText("● BASELINE ACTIVE");
            baselineStatusLabel.setForeground(GREEN);
        }

        // Risk panel
        switch (r.risk) {
            case AUTHORIZED -> {
                setRiskPanel(GREEN, "AUTHORIZED", new Color(5, 30, 15));
                anomalyBar.setForeground(GREEN);
                anomalyLabel.setForeground(GREEN);
            }
            case EVALUATING -> {
                setRiskPanel(YELLOW, "EVALUATING", new Color(30, 28, 5));
                anomalyBar.setForeground(YELLOW);
                anomalyLabel.setForeground(YELLOW);
            }
            case UNAUTHORIZED -> {
                setRiskPanel(RED, "UNAUTHORIZED", new Color(35, 5, 12));
                anomalyBar.setForeground(RED);
                anomalyLabel.setForeground(RED);
                appendLog("ALERT", r.logLine);   // Only log high-risk events
            }
        }
    }

    private void setRiskPanel(Color accent, String text, Color bg) {
        riskStatusPanel.setBackground(bg);
        riskLabel.setText(text);
        riskLabel.setForeground(accent);
        riskStatusPanel.setBorder(new LineBorder(accent, 2));
    }

    private void onResetClicked() {
        engine.resetBaseline();
        captureZone.setText("");
        securityLog.setText("");
        wpmLabel.setText("0.0");
        kpsLabel.setText("0.00");
        anomalyLabel.setText("0.000");
        anomalyBar.setValue(0);
        anomalyBar.setString("Anomaly: 0%");
        setRiskPanel(YELLOW, "EVALUATING", new Color(30, 28, 5));
        baselineStatusLabel.setText("● AWAITING INPUT");
        baselineStatusLabel.setForeground(YELLOW);
        appendLog("System", "Baseline reset. Begin typing to recalibrate.");
    }

    // ── Helpers ───────────────────────────────────────────────
    private void appendLog(String source, String message) {
        String line = String.format("[%s] [%-10s] %s%n",
                LocalTime.now().format(TIME_FMT), source, message);
        securityLog.append(line);
    }

    private JLabel styledLabel(String text, float size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", style, (int) size));
        l.setForeground(color);
        return l;
    }

    private void styleButton(JButton btn, Color accent) {
        btn.setBackground(new Color(20, 30, 55));
        btn.setForeground(accent);
        btn.setFont(new Font("Monospaced", Font.BOLD, 11));
        btn.setBorder(new CompoundBorder(
                new LineBorder(accent, 1),
                new EmptyBorder(5, 12, 5, 12)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(30, 50, 90)); }
            public void mouseExited (MouseEvent e) { btn.setBackground(new Color(20, 30, 55)); }
        });
    }
}
