package com.mathamorphosis.ui.visualizations;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controller for riemann_view.fxml.
 * Owns all Riemann Sum Explorer logic, driven by FXML-injected nodes.
 */
public class RiemannViewController {

    // ── FXML-injected nodes ──────────────────────────────────────────────────
    @FXML private Pane         canvasPane;
    @FXML private Canvas       canvas;
    @FXML private Slider       nSlider;
    @FXML private Label        nValueLabel;

    @FXML private HBox         presetsBox;
    @FXML private TextField    funcField;
    @FXML private ComboBox<String> typeBox;
    @FXML private TextField    aField;
    @FXML private TextField    bField;

    @FXML private Label        estValueLabel;
    @FXML private Label        actValueLabel;
    @FXML private Label        errorValueLabel;

    @FXML private ToggleButton modeToggle;
    @FXML private Button       startBtn;
    @FXML private Button       pauseBtn;
    @FXML private Button       restartBtn;

    // ── State ────────────────────────────────────────────────────────────────
    private GraphicsContext gc;
    private double aValue      = 0.0;
    private double bValue      = 10.0;
    private String functionStr = "0.2*x^2+3";
    private Expression expression;
    private String parseError  = null;   // non-null while the typed text cannot be parsed
    private String lastProblemSignature = null;   // avoids re-laying-out an unchanged error card every frame

    private AnimationTimer timer;
    private boolean isRunning  = false;

    // ── Preset functions ─────────────────────────────────────────────────────
    private static final String[] PRESETS      = {"0.2*x^2+3", "sin(x)+2", "cos(x/2)+2", "sqrt(x)+1", "x^3/50+2"};
    private static final String[] PRESET_NAMES = {"Parabola",  "Sine Wave","Cosine Wave", "Square Root","Cubic"};

    // ── Design constants ─────────────────────────────────────────────────────
    private static final String BG_DEEP     = "#1a1a2e";
    private static final String BG_INPUT    = "#2c2c4a";
    private static final String ACCENT      = "#8ab4d4";
    private static final String ACCENT_WARM = "#d4aa7d";
    private static final String STAT_EST    = "#d4887a";
    private static final String STAT_ACT    = "#7dc4a8";
    private static final String STAT_ERR    = "#b0a0d4";
    private static final String TEXT_MUTED  = "#8888aa";
    private static final String BORDER      = "#3c3c60";
    private static final String CURVE_COLOR = "#e0e0f0";
    private static final String RECT_FILL   = "#8ab4d4";

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        gc = canvas.getGraphicsContext2D();

        // Bind canvas size to its parent Pane
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());
        canvas.widthProperty().addListener(o -> draw(getMappedN()));
        canvas.heightProperty().addListener(o -> draw(getMappedN()));

        // Populate ComboBox
        typeBox.getItems().addAll("Definite", "Indefinite");
        typeBox.setValue("Definite");
        styleComboBox(typeBox);
        typeBox.valueProperty().addListener((obs, o, nv) -> draw(getMappedN()));

        // Build preset buttons
        buildPresets();

        // Wire slider
        nSlider.valueProperty().addListener((obs, o, nv) -> {
            int n = getMappedN();
            nValueLabel.setText(nSlider.getValue() >= 99.9 ? "n = ∞" : "n = " + n);
            draw(n);
        });

        // Wire text fields
        funcField.textProperty().addListener((obs, o, nv) -> updateFunction(nv));
        aField.textProperty().addListener((obs, o, nv) -> {
            try { aValue = Double.parseDouble(nv); draw(getMappedN()); } catch (Exception ignored) {}
        });
        bField.textProperty().addListener((obs, o, nv) -> {
            try { bValue = Double.parseDouble(nv); draw(getMappedN()); } catch (Exception ignored) {}
        });

        // Wire playback controls
        stylePlaybackButtons();
        modeToggle.setOnAction(e -> handleModeToggle());
        startBtn.setOnAction(e -> handleStart());
        pauseBtn.setOnAction(e -> handlePause());
        restartBtn.setOnAction(e -> handleRestart());

        // Init
        initTimer();
        updateFunction(functionStr);
    }

    // ── Preset buttons ───────────────────────────────────────────────────────

    private void buildPresets() {
        final javafx.scene.control.Button[] presetBtns = new javafx.scene.control.Button[PRESETS.length];
        final int[] activePreset = {-1};

        for (int i = 0; i < PRESETS.length; i++) {
            final int idx = i;
            javafx.scene.control.Button pb = new javafx.scene.control.Button(PRESET_NAMES[i]);
            presetBtns[idx] = pb;

            final String BASE = presetBaseStyle();
            final String LIT  = presetLitStyle();

            pb.setStyle(BASE);
            pb.setOnMouseEntered(e -> pb.setStyle(LIT));
            pb.setOnMouseExited(e  -> pb.setStyle(activePreset[0] == idx ? LIT : BASE));
            pb.setOnAction(e -> {
                activePreset[0] = idx;
                for (int j = 0; j < presetBtns.length; j++) {
                    presetBtns[j].setStyle(j == idx ? presetLitStyle() : presetBaseStyle());
                }
                funcField.setText(PRESETS[idx]);
                updateFunction(PRESETS[idx]);
            });
            presetsBox.getChildren().add(pb);
        }
    }

    private String presetBaseStyle() {
        return "-fx-background-color:" + BG_INPUT + "; -fx-text-fill:" + ACCENT + ";" +
               "-fx-border-color:" + ACCENT + "; -fx-border-radius:6; -fx-background-radius:6;" +
               "-fx-font-size:11px; -fx-font-weight:bold; -fx-padding:5 12; -fx-cursor:hand;" +
               "-fx-effect: dropshadow(gaussian, transparent, 0, 0, 0, 0);";
    }

    private String presetLitStyle() {
        return "-fx-background-color:" + ACCENT + "; -fx-text-fill:#0a0a14;" +
               "-fx-border-color:" + ACCENT + "; -fx-border-radius:6; -fx-background-radius:6;" +
               "-fx-font-size:11px; -fx-font-weight:bold; -fx-padding:5 12; -fx-cursor:hand;" +
               "-fx-effect: dropshadow(gaussian, " + ACCENT + ", 10, 0.55, 0, 0);";
    }

    // ── Playback ─────────────────────────────────────────────────────────────

    private void stylePlaybackButtons() {
        styleControlButton(startBtn,   STAT_ACT);
        styleControlButton(pauseBtn,   ACCENT_WARM);
        styleControlButton(restartBtn, STAT_EST);
    }

    private void styleControlButton(Button btn, String color) {
        final String BASE  = "-fx-background-color:" + BG_INPUT + "; -fx-text-fill:" + color + ";" +
                "-fx-border-color:" + color + "; -fx-border-radius:6; -fx-background-radius:6;" +
                "-fx-font-size:11px; -fx-font-weight:bold; -fx-padding:6 8; -fx-cursor:hand;" +
                "-fx-effect: dropshadow(gaussian, transparent, 0, 0, 0, 0);";
        final String HOVER = "-fx-background-color:" + color + "; -fx-text-fill:#0a0a14;" +
                "-fx-border-color:" + color + "; -fx-border-radius:6; -fx-background-radius:6;" +
                "-fx-font-size:11px; -fx-font-weight:bold; -fx-padding:6 8; -fx-cursor:hand;" +
                "-fx-effect: dropshadow(gaussian, " + color + ", 12, 0.6, 0, 0);";
        btn.setStyle(BASE);
        btn.setMinWidth(72);
        btn.setOnMouseEntered(e -> { if (!btn.isDisabled()) btn.setStyle(HOVER); });
        btn.setOnMouseExited(e  -> { if (!btn.isDisabled()) btn.setStyle(BASE);  });
    }

    private void handleModeToggle() {
        if (modeToggle.isSelected()) {
            modeToggle.setText("Auto-Play Mode");
            modeToggle.setStyle(
                "-fx-background-color:" + BORDER + "; -fx-text-fill: #f0f0f8;" +
                "-fx-border-color:" + ACCENT + "; -fx-border-radius:4; -fx-background-radius:4;" +
                "-fx-font-size:13px; -fx-padding:7 14; -fx-cursor:hand;"
            );
            nSlider.setDisable(true);
            startBtn.setDisable(false);
            pauseBtn.setDisable(false);
            restartBtn.setDisable(false);
        } else {
            modeToggle.setText("Manual Mode");
            modeToggle.setStyle(
                "-fx-background-color:transparent; -fx-text-fill:" + TEXT_MUTED + ";" +
                "-fx-border-color:" + BORDER + "; -fx-border-radius:4; -fx-background-radius:4;" +
                "-fx-font-size:13px; -fx-padding:7 14; -fx-cursor:hand;"
            );
            nSlider.setDisable(false);
            startBtn.setDisable(true);
            pauseBtn.setDisable(true);
            restartBtn.setDisable(true);
            if (timer != null && isRunning) { timer.stop(); isRunning = false; }
        }
    }

    private void handleStart() {
        if (timer != null && !isRunning) {
            if (nSlider.getValue() >= 100) nSlider.setValue(0);
            timer.start();
            isRunning = true;
        }
    }

    private void handlePause() {
        if (timer != null && isRunning) { timer.stop(); isRunning = false; }
    }

    private void handleRestart() {
        nSlider.setValue(0);
        if (timer != null && !isRunning) { timer.start(); isRunning = true; }
    }

    // ── Timer ────────────────────────────────────────────────────────────────

    private void initTimer() {
        timer = new AnimationTimer() {
            private long lastUpdate = 0;
            @Override public void handle(long now) {
                if (lastUpdate == 0) { lastUpdate = now; return; }
                double dt = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;
                if (dt > 0.1) dt = 0.016;
                double cur = nSlider.getValue();
                if (cur < 100) {
                    nSlider.setValue(cur + dt * 10);
                } else {
                    this.stop(); isRunning = false;
                }
            }
            @Override public void stop() { super.stop(); lastUpdate = 0; }
        };
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private int getMappedN() {
        double t = nSlider.getValue() / 100.0;
        return Math.max(1, Math.min(200, (int)(1 + 199 * Math.pow(t, 3))));
    }

    private String insertImplicitMultiplication(String func) {
        func = func.replaceAll("\\s+", "");
        // The lookbehinds keep digits that belong to a function name (log10, log2, expm1) from being split
        func = func.replaceAll("(?<![0-9])(?<!log)(?<!expm)(\\d+(?:\\.\\d+)?)(?=(x|sin|cos|tan|log|exp|sqrt|pi|\\())", "$1*");
        func = func.replaceAll("(x|\\))(?=(x|\\d|sin|cos|tan|log|exp|sqrt|pi|e|\\())", "$1*");
        return func;
    }

    private void updateFunction(String newFunc) {
        try {
            String processed = insertImplicitMultiplication(newFunc == null ? "" : newFunc);
            if (processed.isEmpty())
                throw new IllegalArgumentException("Type a function of x to get started.");

            Expression built = new ExpressionBuilder(processed).variables("x").build();
            ValidationResult check = built.validate(false);   // catches things like "2+" up front
            if (!check.isValid())
                throw new IllegalArgumentException(String.join("; ", check.getErrors()));
            try {
                built.setVariable("x", 1.0).evaluate();   // structural sanity check (e.g. rejects "()")
            } catch (ArithmeticException ignored) {
                // e.g. 1/(x-1) at x = 1: a domain problem, reported later together with its location
            }

            expression = built;
            parseError = null;
        } catch (Exception e) {
            // Previously this was swallowed silently, leaving the old graph on screen.
            expression = null;
            parseError = readableParseMessage(e);
        }
        functionStr = newFunc;
        draw(getMappedN());
    }

    private String readableParseMessage(Exception e) {
        if (e instanceof NumberFormatException)
            return "A number is not written correctly (check the decimal points).";
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) return "Check the brackets and operators.";
        msg = msg.replaceAll("\\s+in expression '.*'\\s*$", "");   // the user can already see what they typed
        if (msg.equals("Too many operators"))
            return "An operator is missing a number or bracket next to it (for example \"2+\" or \"*x\").";
        return msg;
    }

    private double f(double x) {
        if (expression == null) return Double.NaN;
        try {
            return expression.setVariable("x", x).evaluate();
        } catch (ArithmeticException e) {
            return Double.POSITIVE_INFINITY;
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private enum FunctionState { VALID, DIVERGENT, ERROR }

    /** Result of scanning f over [a, b]; badX is the first x where f was undefined / infinite. */
    private record FunctionCheck(FunctionState state, double badX) {}

    private FunctionCheck checkFunctionState(double a, double b) {
        if (expression == null) return new FunctionCheck(FunctionState.ERROR, Double.NaN);
        if (b < a) { double tmp = a; a = b; b = tmp; }
        int N = 1000;
        double dx = (b - a) / N;
        if (dx == 0) dx = 1e-9;
        boolean hasInfinity = false;
        double firstInfinite = Double.NaN;
        for (int i = 0; i <= N; i++) {
            double x = a + i * dx;
            double y = f(x);
            if (Double.isNaN(y)) return new FunctionCheck(FunctionState.ERROR, x);
            if (Double.isInfinite(y) && !hasInfinity) { hasInfinity = true; firstInfinite = x; }
        }
        for (int i = 0; i < N; i++) {
            double x = a + i * dx + dx / 2.0;
            double y = f(x);
            if (Double.isNaN(y)) return new FunctionCheck(FunctionState.ERROR, x);
            if (Double.isInfinite(y) && !hasInfinity) { hasInfinity = true; firstInfinite = x; }
        }
        return hasInfinity ? new FunctionCheck(FunctionState.DIVERGENT, firstInfinite)
                           : new FunctionCheck(FunctionState.VALID, Double.NaN);
    }

    private double calculateActualArea(double a, double b) {
        int N = 10000;
        double dx = (b - a) / N, sum = 0;
        for (int i = 0; i < N; i++) sum += f(a + i * dx + dx / 2.0) * dx;
        return sum;
    }

    // ── ComboBox styling ─────────────────────────────────────────────────────

    private void styleComboBox(ComboBox<String> box) {
        javafx.util.Callback<ListView<String>, ListCell<String>> cf = lv ->
            new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); } else {
                        setText(item);
                        setStyle("-fx-text-fill:" + ACCENT + "; -fx-background-color:" + BG_INPUT + ";");
                        setOnMouseEntered(e -> setStyle("-fx-text-fill:#fff; -fx-background-color:" + BORDER + ";"));
                        setOnMouseExited(e  -> setStyle("-fx-text-fill:" + ACCENT + "; -fx-background-color:" + BG_INPUT + ";"));
                    }
                }
            };
        box.setButtonCell(cf.call(null));
        box.setCellFactory(cf);
    }

    // ── Drawing ──────────────────────────────────────────────────────────────

    private void draw(int n) {
        double W = canvas.getWidth(), H = canvas.getHeight();

        // The typed text can't be parsed: say so (instead of leaving a stale or blank plot).
        if (expression == null) {
            if (parseError == null || W <= 0 || H <= 0) return;   // not initialised yet / no size yet
            if (!modeToggle.isSelected()) nSlider.setDisable(true);
            estValueLabel.setText("Error");
            actValueLabel.setText("Invalid expression");
            errorValueLabel.setText("N/A");
            drawProblem(W, H, STAT_EST,
                "Can't read this expression",
                parseError,
                "Use x as the variable, e.g. 0.2*x^2+3 or sin(x)+2. "
                + "Supported: + - * / ^  sin  cos  tan  sqrt  log  exp  abs  pi  e");
            return;
        }

        if (W <= 0 || H <= 0) return;

        FunctionCheck check = checkFunctionState(aValue, bValue);
        FunctionState state = check.state();
        String interval = "[" + fmtNum(Math.min(aValue, bValue)) + ", " + fmtNum(Math.max(aValue, bValue)) + "]";

        if (state == FunctionState.ERROR) {
            if (!modeToggle.isSelected()) nSlider.setDisable(true);
            estValueLabel.setText("Not integrable");
            actValueLabel.setText("Undefined");
            errorValueLabel.setText("N/A");
            drawProblem(W, H, STAT_EST,
                "Not integrable on " + interval,
                "f(x) has no real value near x \u2248 " + fmtNum(check.badX())
                    + " (for example a square root or logarithm of a negative number, or 0/0).",
                "Try bounds that keep the interval inside the region where f(x) is defined.");
            return;
        } else if (state == FunctionState.DIVERGENT) {
            if (!modeToggle.isSelected()) nSlider.setDisable(true);
            estValueLabel.setText("Not integrable");
            actValueLabel.setText("\u221E");
            errorValueLabel.setText("N/A");
            drawProblem(W, H, ACCENT_WARM,
                "Not integrable on " + interval,
                "f(x) blows up to infinity near x \u2248 " + fmtNum(check.badX())
                    + ", so it is unbounded on this interval and the rectangles cannot approximate a finite area.",
                "Try bounds that stay away from x \u2248 " + fmtNum(check.badX()) + ", or pick a different function.");
            return;
        } else {
            if (!modeToggle.isSelected()) nSlider.setDisable(false);
        }
        lastProblemSignature = null;   // a normal plot is about to overwrite any earlier message

        final double PAD_L = 60, PAD_R = 30, PAD_T = 30, PAD_B = 40;
        final double plotW = W - PAD_L - PAD_R;
        final double plotH = H - PAD_T - PAD_B;

        gc.setFill(Color.web(BG_DEEP));
        gc.fillRect(0, 0, W, H);
        gc.setFill(Color.web("#20203a", 1.0));
        gc.fillRect(PAD_L, PAD_T, plotW, plotH);

        double rangeX = bValue - aValue;
        if (rangeX <= 0) rangeX = 1;

        boolean isIndefinite = "Indefinite".equals(typeBox.getValue());

        double minY = 0, maxY = 0;
        for (int i = 0; i <= 200; i++) {
            double x = aValue + i * rangeX / 200.0, y = f(x);
            if (y > maxY) maxY = y;
            if (y < minY) minY = y;
        }
        if (isIndefinite) {
            double integral = 0, dx2 = rangeX / 200.0;
            for (int i = 0; i <= 200; i++) {
                integral += f(aValue + i * dx2) * dx2;
                if (integral > maxY) maxY = integral;
                if (integral < minY) minY = integral;
            }
        }
        if (maxY == minY) { maxY += 5; minY -= 5; }
        double padding = (maxY - minY) * 0.1;
        maxY += padding; minY -= padding;
        double rangeY = maxY - minY;

        double scaleX = plotW / rangeX;
        double scaleY = plotH / rangeY;
        double pY0    = PAD_T + maxY * scaleY;

        // Grid
        gc.setStroke(Color.web("#2a2a35", 0.8));
        gc.setLineWidth(0.5);
        for (int i = 0; i <= 6; i++) {
            gc.strokeLine(PAD_L, PAD_T + i * plotH / 6, PAD_L + plotW, PAD_T + i * plotH / 6);
            gc.strokeLine(PAD_L + i * plotW / 6, PAD_T, PAD_L + i * plotW / 6, PAD_T + plotH);
        }

        // Axes
        gc.setStroke(Color.web("#3a3a48")); gc.setLineWidth(1);
        double clampY0 = Math.max(PAD_T, Math.min(PAD_T + plotH, pY0));
        gc.strokeLine(PAD_L, clampY0, PAD_L + plotW, clampY0);
        double pX0 = PAD_L + (0 - aValue) * scaleX;
        if (pX0 >= PAD_L && pX0 <= PAD_L + plotW)
            gc.strokeLine(pX0, PAD_T, pX0, PAD_T + plotH);

        // Tick labels
        gc.setFill(Color.web(TEXT_MUTED));
        gc.setFont(Font.font("Monospace", 11));
        for (int i = 0; i <= 5; i++) {
            gc.fillText(String.format("%.1f", aValue + i * rangeX / 5.0), PAD_L + i * plotW / 5.0 - 10, PAD_T + plotH + 16);
        }
        for (int i = 0; i <= 4; i++) {
            gc.fillText(String.format("%.1f", maxY - i * rangeY / 4.0), 2, PAD_T + i * plotH / 4.0 + 4);
        }

        // Bound labels
        gc.setFill(Color.web(ACCENT_WARM));
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        gc.fillText(String.format("a=%.1f", aValue), PAD_L,              PAD_T + plotH + 30);
        gc.fillText(String.format("b=%.1f", bValue), PAD_L + plotW - 50, PAD_T + plotH + 30);

        // Riemann rectangles
        double dx = rangeX / n, estimatedArea = 0;
        double progress = (n - 1.0) / 199.0;
        double alpha = 0.18 + 0.07 * (1.0 - progress);
        gc.setFill(Color.web(RECT_FILL, alpha));
        gc.setStroke(Color.web(RECT_FILL, 0.35 + 0.25 * (1.0 - progress)));
        gc.setLineWidth(n < 60 ? 0.8 : 0.2);
        for (int i = 0; i < n; i++) {
            double x0 = aValue + i * dx, xMid = x0 + dx / 2.0, y = f(xMid);
            estimatedArea += y * dx;
            double px  = PAD_L + (x0 - aValue) * scaleX;
            double pw  = dx * scaleX;
            double py0b = PAD_T + maxY * scaleY;
            double pyY  = PAD_T + (maxY - y) * scaleY;
            double rectY = Math.min(py0b, pyY), rectH = Math.abs(py0b - pyY);
            gc.fillRect(px, rectY, pw, rectH);
            if (n < 80) gc.strokeRect(px, rectY, pw, rectH);
        }

        // Curve f(x)
        gc.setStroke(Color.web(CURVE_COLOR));
        gc.setEffect(null); gc.setLineWidth(1.8);
        gc.beginPath();
        boolean first = true;
        for (double x = aValue; x <= bValue; x += rangeX / 400.0) {
            double px = PAD_L + (x - aValue) * scaleX;
            double py = PAD_T + (maxY - f(x)) * scaleY;
            if (py < PAD_T || py > PAD_T + plotH) { first = true; continue; }
            if (first) { gc.moveTo(px, py); first = false; } else gc.lineTo(px, py);
        }
        gc.stroke(); gc.setEffect(null);

        // Indefinite integral F(x)
        if (isIndefinite) {
            gc.setStroke(Color.web(ACCENT_WARM, 0.8));
            gc.setEffect(null); gc.setLineWidth(1.5); gc.setLineDashes(6, 4);
            double cumF = 0, step = rangeX / 400.0;
            first = true;
            for (double x = aValue; x <= bValue; x += step) {
                double px = PAD_L + (x - aValue) * scaleX;
                double py = PAD_T + (maxY - cumF) * scaleY;
                if (py < PAD_T || py > PAD_T + plotH) { cumF += f(x) * step; first = true; continue; }
                if (first) { gc.moveTo(px, py); first = false; } else gc.lineTo(px, py);
                cumF += f(x) * step;
            }
            gc.stroke(); gc.setLineDashes(0); gc.setEffect(null);
            gc.setFill(Color.web(ACCENT_WARM, 0.8));
            gc.setFont(Font.font("Monospace", FontWeight.NORMAL, 11));
            gc.fillText("F(x) = ∫f(t)dt", PAD_L + plotW - 130, PAD_T + 32);
        }

        // f(x) legend
        gc.setFill(Color.web("#40e0d0"));
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 15));
        gc.fillText("f(x) = " + functionStr, PAD_L + 10, PAD_T + 20);

        // Stats
        double actualArea = calculateActualArea(aValue, bValue);
        if (nSlider.getValue() >= 99.9 || n >= 200) estimatedArea = actualArea;
        double error = Math.abs(actualArea - estimatedArea);
        estValueLabel.setText(String.format("%.4f", estimatedArea));
        actValueLabel.setText(String.format("≈ %.4f (Numerical)", actualArea));
        errorValueLabel.setText(String.format("%.4f", error));

        // Fade estimated-area colour from terracotta → sage as n grows
        Color estColor = interpolateColor(Color.web(STAT_EST), Color.web(STAT_ACT), progress);
        estValueLabel.setStyle("-fx-text-fill: " + toHex(estColor) + "; -fx-font-size:18px; -fx-font-weight:bold;");
    }

    // ── "Problem" message card (unintegrable / undefined / unparsable function) ──

    private String fmtNum(double v) {
        if (Double.isNaN(v)) return "?";
        if (Math.abs(v) < 1e-9) return "0";
        return String.format(Locale.ROOT, "%.4f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private double measure(String text, Font font) {
        Text t = new Text(text);
        t.setFont(font);
        return t.getLayoutBounds().getWidth();
    }

    /** Word-wraps text to maxWidth; over-long single words are broken by character. */
    private List<String> wrapText(String text, Font font, double maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            while (measure(word, font) > maxWidth && word.length() > 1) {
                int cut = word.length() - 1;
                while (cut > 1 && measure(word.substring(0, cut), font) > maxWidth) cut--;
                if (line.length() > 0) { lines.add(line.toString()); line.setLength(0); }
                lines.add(word.substring(0, cut));
                word = word.substring(cut);
            }
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && measure(candidate, font) > maxWidth) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    /**
     * Replaces the plot with an explanatory card centred on the canvas, so the user sees WHY
     * nothing is drawn instead of an empty panel.
     */
    private void drawProblem(double W, double H, String accent, String title, String detail, String hint) {
        String signature = W + "x" + H + "|" + functionStr + "|" + title + "|" + detail + "|" + hint;
        if (signature.equals(lastProblemSignature)) return;   // already on screen (e.g. auto-play ticks)
        lastProblemSignature = signature;

        final double PAD_L = 60, PAD_R = 30, PAD_T = 30, PAD_B = 40;
        final double plotW = Math.max(0, W - PAD_L - PAD_R);
        final double plotH = Math.max(0, H - PAD_T - PAD_B);

        gc.save();
        gc.setEffect(null);
        gc.setLineDashes(0);

        // Same backdrop as a normal plot, so the panel doesn't look broken/empty
        gc.setFill(Color.web(BG_DEEP));
        gc.fillRect(0, 0, W, H);
        gc.setFill(Color.web("#20203a"));
        gc.fillRect(PAD_L, PAD_T, plotW, plotH);

        // Keep the f(x) legend so it is clear what was entered
        gc.setFill(Color.web("#40e0d0"));
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 15));
        gc.fillText("f(x) = " + functionStr, PAD_L + 10, PAD_T + 20);

        final double inner = 22, badgeR = 13, gap = 12;
        final double cardW = Math.min(580, plotW - 40);
        if (cardW > 2 * inner + 100) {
            Font titleFont = Font.font("System", FontWeight.BOLD, 20);
            Font bodyFont  = Font.font("System", FontWeight.NORMAL, 14);
            Font hintFont  = Font.font("System", FontPosture.ITALIC, 13);

            double textW = cardW - 2 * inner;
            List<String> titleLines  = wrapText(title,  titleFont, textW - 2 * badgeR - 12);
            List<String> detailLines = wrapText(detail, bodyFont,  textW);
            List<String> hintLines   = hint == null ? List.of() : wrapText(hint, hintFont, textW);

            final double titleLH = 26, bodyLH = 20, hintLH = 18;
            double cardH = inner + titleLines.size() * titleLH
                         + gap + detailLines.size() * bodyLH
                         + (hintLines.isEmpty() ? 0 : gap + hintLines.size() * hintLH)
                         + inner;
            double cardX = PAD_L + (plotW - cardW) / 2.0;
            double cardY = PAD_T + Math.max(36, (plotH - cardH) / 2.0);

            // Card
            gc.setFill(Color.web("#14142a", 0.96));
            gc.fillRoundRect(cardX, cardY, cardW, cardH, 12, 12);
            gc.setStroke(Color.web(accent, 0.85));
            gc.setLineWidth(1.5);
            gc.strokeRoundRect(cardX, cardY, cardW, cardH, 12, 12);
            gc.setFill(Color.web(accent));
            gc.fillRoundRect(cardX, cardY, 5, cardH, 4, 4);

            // "!" badge + title
            double x = cardX + inner;
            double y = cardY + inner;
            gc.setFill(Color.web(accent));
            gc.fillOval(x, y + 1, 2 * badgeR, 2 * badgeR);
            gc.setFill(Color.web("#0a0a14"));
            gc.setFont(Font.font("System", FontWeight.BOLD, 17));
            gc.fillText("!", x + badgeR - 3, y + badgeR + 7);

            gc.setFill(Color.web("#f0f0f8"));
            gc.setFont(titleFont);
            double textX = x + 2 * badgeR + 12;
            double ty = y + 20;
            for (String ln : titleLines) { gc.fillText(ln, textX, ty); ty += titleLH; }

            // Explanation
            double by = y + titleLines.size() * titleLH + gap + 14;
            gc.setFill(Color.web("#d0d0e8"));
            gc.setFont(bodyFont);
            for (String ln : detailLines) { gc.fillText(ln, x, by); by += bodyLH; }

            // Hint
            if (!hintLines.isEmpty()) {
                by += gap - 2;
                gc.setFill(Color.web(TEXT_MUTED));
                gc.setFont(hintFont);
                for (String ln : hintLines) { gc.fillText(ln, x, by); by += hintLH; }
            }
        }
        gc.restore();
    }

    // ── Colour utilities ─────────────────────────────────────────────────────

    private Color interpolateColor(Color c1, Color c2, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
            c1.getRed()   + (c2.getRed()   - c1.getRed())   * t,
            c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t,
            c1.getBlue()  + (c2.getBlue()  - c1.getBlue())  * t,
            1.0
        );
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x",
            (int)(c.getRed() * 255), (int)(c.getGreen() * 255), (int)(c.getBlue() * 255));
    }
}
