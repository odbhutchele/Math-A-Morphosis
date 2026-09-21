package com.mathamorphosis.ui;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DashboardController {

    @FXML
    private StackPane rootPane;

    private static final double CARD_W = 340;
    private static final double CARD_H = 150;

    private Consumer<String> onModuleSelect;

    public void setOnModuleSelect(Consumer<String> onModuleSelect) {
        this.onModuleSelect = onModuleSelect;
    }

    @FXML
    public void initialize() {
        // ── Animated grid background ─────────────────────────────────────────
        Canvas bgCanvas = new Canvas();
        bgCanvas.widthProperty().bind(rootPane.widthProperty());
        bgCanvas.heightProperty().bind(rootPane.heightProperty());
        GraphicsContext bgGc = bgCanvas.getGraphicsContext2D();

        final double[] bgTime = {0};
        AnimationTimer bgTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                bgTime[0] += 0.008;
                drawDashboardBackground(bgGc,
                        bgCanvas.getWidth(), bgCanvas.getHeight(), bgTime[0]);
            }
        };
        bgTimer.start();

        // ── Content layer ────────────────────────────────────────────────────
        VBox content = new VBox(24);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24, 30, 30, 30));

        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        Label title = new Label("Math-A-Morphosis");
        title.setStyle("-fx-font-size: 38px; -fx-font-weight: bold; -fx-text-fill: #eaeaf5;");
        Label subtitle = new Label("A Visual Mathematics Learning Studio");
        subtitle.setStyle("-fx-font-size: 15px; -fx-text-fill: #6868a0;");
        header.getChildren().addAll(title, subtitle);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(22);
        grid.setVgap(18);

        grid.add(makeCard("Number Theory",       "Sieve of Eratosthenes",                "#9b72d4", "NUMBER_THEORY",  "number_theory"), 0, 0);
        grid.add(makeCard("Calculus",            "Riemann Sum Convergence",              "#5ba8e0", "CALCULUS",        "calculus"),      1, 0);
        grid.add(makeCard("Linear Algebra",      "Interactive Vector Projections",       "#4ab8c4", "LINEAR_ALGEBRA",  "linear_algebra"),0, 1);
        grid.add(makeCard("Statistics",          "Least Squares Regression Sandbox",     "#d4a84b", "LEAST_SQUARES",   "statistics"),    1, 1);
        grid.add(makeCard("Trigonometry",        "Unit Circle Unroller",                 "#4cbf95", "UNIT_CIRCLE",     "trigonometry"),  0, 2);
        grid.add(makeCard("Algebra",             "2D Graphing Calculator",               "#d46b6b", "GRAPHING_CALC",   "algebra"),       1, 2);
        grid.add(makeCard("Signal Processing",   "Fourier Series Epicycles",             "#9b72d4", "FOURIER_SERIES",  "fourier"),       0, 3);
        grid.add(makeCard("Mathematical Marvels","The Chaos Game: Order from Randomness","#d4a84b", "CHAOS_GAME",      "chaos"),         1, 3);
        grid.add(makeCard("Computational Geometry","Jarvis March Convex Hull",           "#5ba8e0", "CONVEX_HULL",     "convex_hull"),   0, 4);
        grid.add(makeCard("Matrix",               "Linear Transformation",                "#4ab8c4", "MATRIX_TRANSFORM","matrix"),        1, 4);

        content.getChildren().addAll(header, grid);

        // ── ScrollPane wrapper ───────────────────────────────────────────────
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle(
                "-fx-background: transparent;" +
                "-fx-background-color: transparent;" +
                "-fx-viewport-background-color: transparent;" +
                "-fx-border-width: 0;"
        );

        rootPane.getChildren().addAll(bgCanvas, scrollPane);
        rootPane.setStyle("-fx-background-color: #0c0c1e;");

        // Stop bg timer when removed from scene
        rootPane.sceneProperty().addListener((obs, o, n) -> {
            if (n == null) bgTimer.stop();
        });
    }

    // ── Global background: subtle animated dot-grid ──────────────────────────

    private void drawDashboardBackground(GraphicsContext gc, double W, double H, double t) {
        gc.setFill(Color.web("#0c0c1e"));
        gc.fillRect(0, 0, W, H);

        // Fine grid lines
        gc.setStroke(Color.web("#1a1a3a", 0.8));
        gc.setLineWidth(0.5);
        double spacing = 40;
        for (double x = 0; x < W; x += spacing) gc.strokeLine(x, 0, x, H);
        for (double y = 0; y < H; y += spacing) gc.strokeLine(0, y, W, y);

        // Pulsing grid-intersection dots
        gc.setLineWidth(1);
        for (double x = 0; x < W; x += spacing) {
            for (double y = 0; y < H; y += spacing) {
                double pulse = 0.12 + 0.08 * Math.sin(t + x * 0.05 + y * 0.05);
                gc.setFill(Color.web("#5ba8e0", pulse));
                gc.fillOval(x - 1.5, y - 1.5, 3, 3);
            }
        }
    }

    // ── Card factory ─────────────────────────────────────────────────────────

    private StackPane makeCard(String titleText, String descText,
                               String accentHex, String moduleKey, String animType) {

        Color accent = Color.web(accentHex);

        Canvas anim = new Canvas(CARD_W, CARD_H);
        anim.setOpacity(0);
        GraphicsContext gc = anim.getGraphicsContext2D();

        AnimState state = new AnimState();

        AnimationTimer timer = new AnimationTimer() {
            @Override public void handle(long now) {
                state.t += 0.018;
                drawCardAnimation(gc, animType, accent, state);
            }
        };

        Label titleLbl = new Label(titleText);
        titleLbl.setStyle(
            "-fx-font-size:19px; -fx-font-weight:bold; -fx-text-fill:" + accentHex + ";" +
            "-fx-effect: dropshadow(three-pass-box, rgba(12,12,34,0.95), 8, 0, 0, 1);"
        );
        Label descLbl = new Label(descText);
        descLbl.setStyle(
            "-fx-font-size:13px; -fx-text-fill:#8888aa; -fx-wrap-text:true;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(12,12,34,0.95), 8, 0, 0, 1);"
        );
        VBox text = new VBox(6, titleLbl, descLbl);
        text.setAlignment(Pos.CENTER_LEFT);
        text.setPadding(new Insets(20));
        text.setMaxWidth(CARD_W);
        text.setPickOnBounds(false);

        StackPane card = new StackPane(anim, text);
        card.setPrefSize(CARD_W, CARD_H);
        card.setMinSize(CARD_W, CARD_H);
        card.setMaxSize(CARD_W, CARD_H);
        card.setStyle(
            "-fx-background-color:#14142a;" +
            "-fx-background-radius:12px;" +
            "-fx-border-color:" + accentHex + ";" +
            "-fx-border-radius:12px;" +
            "-fx-border-width:1.5px;" +
            "-fx-cursor:hand;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 4);"
        );
        card.setAlignment(Pos.CENTER_LEFT);

        card.setOnMouseEntered(e -> {
            state.reset();
            timer.start();
            card.setStyle(
                "-fx-background-color:#0c0c22;" +
                "-fx-background-radius:12px;" +
                "-fx-border-color:" + accentHex + ";" +
                "-fx-border-radius:12px;" +
                "-fx-border-width:2.5px;" +
                "-fx-cursor:hand;" +
                "-fx-effect: dropshadow(three-pass-box, " + toRgba(accent, 0.5) + ", 22, 0, 0, 0);"
            );
            fadeCanvas(anim, 0, 1, 250);
        });
        card.setOnMouseExited(e -> {
            timer.stop();
            card.setStyle(
                "-fx-background-color:#14142a;" +
                "-fx-background-radius:12px;" +
                "-fx-border-color:" + accentHex + ";" +
                "-fx-border-radius:12px;" +
                "-fx-border-width:1.5px;" +
                "-fx-cursor:hand;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 4);"
            );
            fadeCanvas(anim, 1, 0, 200);
        });

        card.setOnMouseClicked(e -> {
            if (onModuleSelect != null) onModuleSelect.accept(moduleKey);
        });

        return card;
    }

    // ── Per-card animation dispatcher ─────────────────────────────────────────

    private void drawCardAnimation(GraphicsContext gc, String type, Color accent, AnimState s) {
        gc.clearRect(0, 0, CARD_W, CARD_H);
        switch (type) {
            case "number_theory"  -> drawNumberTheory(gc, accent, s);
            case "calculus"       -> drawCalculus(gc, accent, s);
            case "linear_algebra" -> drawLinearAlgebra(gc, accent, s);
            case "statistics"     -> drawStatistics(gc, accent, s);
            case "trigonometry"   -> drawTrigonometry(gc, accent, s);
            case "algebra"        -> drawAlgebra(gc, accent, s);
            case "fourier"        -> drawFourier(gc, accent, s);
            case "chaos"          -> drawChaos(gc, accent, s);
            case "convex_hull"    -> drawConvexHullAnim(gc, accent, s);
            case "matrix"         -> drawMatrixAnim(gc, accent, s);
        }
    }

    // ── 1. Number Theory ─────────────────────────────────────────────────────
    private void drawNumberTheory(GraphicsContext gc, Color accent, AnimState s) {
        if (s.primes == null) {
            s.primes = new ArrayList<>();
            int[] ps = {2,3,5,7,11,13,17,19,23,29,31,37,41,43,47,53,59,61,67,71,73,79,83,89,97,101,103,107,109,113};
            java.util.Random rng = new java.util.Random(42);
            for (int p : ps) {
                double[] d = { rng.nextDouble()*CARD_W, rng.nextDouble()*CARD_H,
                               (rng.nextDouble()-0.5)*0.4, (rng.nextDouble()-0.5)*0.4,
                               p, 9 + rng.nextDouble()*8 };
                s.primes.add(d);
            }
        }
        for (double[] p : s.primes) {
            p[0] += p[2]; p[1] += p[3];
            if (p[0] < -20) p[0] = CARD_W + 10;
            if (p[0] > CARD_W + 20) p[0] = -10;
            if (p[1] < -20) p[1] = CARD_H + 10;
            if (p[1] > CARD_H + 20) p[1] = -10;
            double alpha = 0.12 + 0.1 * Math.sin(s.t * 1.2 + p[4] * 0.3);
            gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
            gc.setFont(Font.font("Monospace", FontWeight.BOLD, p[5]));
            gc.fillText(String.valueOf((int)p[4]), p[0], p[1]);
        }
        double dotSpacing = 18;
        int col = 0, row = 0;
        double startX = CARD_W - 90, startY = 8;
        for (int n = 2; n <= 60; n++) {
            double dx = startX + col * dotSpacing;
            double dy = startY + row * dotSpacing;
            boolean isPrime = isPrime(n);
            double a = isPrime ? (0.35 + 0.2 * Math.sin(s.t * 2 + n * 0.5)) : 0.07;
            gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), a));
            gc.fillOval(dx - 3, dy - 3, isPrime ? 7 : 4, isPrime ? 7 : 4);
            col++;
            if (col > 4) { col = 0; row++; }
        }
    }

    private boolean isPrime(int n) {
        if (n < 2) return false;
        for (int i = 2; i * i <= n; i++) if (n % i == 0) return false;
        return true;
    }

    // ── 2. Calculus ──────────────────────────────────────────────────────────
    private void drawCalculus(GraphicsContext gc, Color accent, AnimState s) {
        // Brightness ramp: starts faded, gradually brightens, capped so it never overpowers text
        double ramp = Math.min(1.0, s.t / 2.0);
        double brightness = 0.18 + 0.47 * (ramp * ramp * (3 - 2 * ramp));

        int n = Math.max(2, (int)(2 + 18 * ((Math.sin(s.t * 0.5) + 1) / 2.0)));
        double padL = 30, padR = 20, padT = 20, padB = 30;
        double pW = CARD_W - padL - padR, pH = CARD_H - padT - padB;
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.2 * brightness));
        gc.setLineWidth(1);
        gc.strokeLine(padL, padT + pH, padL + pW, padT + pH);
        gc.strokeLine(padL, padT, padL, padT + pH);
        for (int i = 0; i < n; i++) {
            double xMid = (i + 0.5) / n;
            double y = 0.15 + 0.7 * Math.sin(xMid * Math.PI);
            double rx = padL + i * pW / n;
            double rw = pW / n - 1;
            double rh = y * pH;
            double alpha = (0.10 + 0.07 / n) * brightness;
            gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
            gc.fillRect(rx, padT + pH - rh, rw, rh);
            gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.25 * brightness));
            gc.setLineWidth(0.5);
            gc.strokeRect(rx, padT + pH - rh, rw, rh);
        }
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.55 * brightness));
        gc.setLineWidth(2);
        gc.beginPath();
        for (int i = 0; i <= 100; i++) {
            double xr = i / 100.0;
            double yr = 0.15 + 0.7 * Math.sin(xr * Math.PI);
            double sx = padL + xr * pW;
            double sy = padT + pH - yr * pH;
            if (i == 0) gc.moveTo(sx, sy); else gc.lineTo(sx, sy);
        }
        gc.stroke();
    }

    // ── 3. Linear Algebra ────────────────────────────────────────────────────
    private void drawLinearAlgebra(GraphicsContext gc, Color accent, AnimState s) {
        double cx = CARD_W * 0.72, cy = CARD_H * 0.5, r = 55;
        double axA = cx + r * Math.cos(s.t * 0.7);
        double ayA = cy + r * Math.sin(s.t * 0.7);
        double axB = cx + r * Math.cos(0.8);
        double ayB = cy + r * Math.sin(0.8);
        double dot = (axA-cx)*(axB-cx) + (ayA-cy)*(ayB-cy);
        double bLen2 = (axB-cx)*(axB-cx) + (ayB-cy)*(ayB-cy);
        double projX = cx + dot / bLen2 * (axB - cx);
        double projY = cy + dot / bLen2 * (ayB - cy);
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.18));
        gc.setLineWidth(1); gc.setLineDashes(4, 4);
        gc.strokeLine(axA, ayA, projX, projY);
        gc.setLineDashes();
        gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.5));
        gc.fillOval(projX - 4, projY - 4, 8, 8);
        drawArrow(gc, cx, cy, axA, ayA, accent, 0.7, 2.5);
        drawArrow(gc, cx, cy, axB, ayB, accent, 0.35, 1.5);
        gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.5));
        gc.fillOval(cx - 4, cy - 4, 8, 8);
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.07));
        gc.setLineWidth(0.5);
        for (double gx = 10; gx < CARD_W; gx += 22) gc.strokeLine(gx, 0, gx, CARD_H);
        for (double gy = 5; gy < CARD_H; gy += 22) gc.strokeLine(0, gy, CARD_W, gy);
    }

    private void drawArrow(GraphicsContext gc, double x1, double y1,
                           double x2, double y2, Color c, double alpha, double lw) {
        gc.setStroke(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
        gc.setLineWidth(lw);
        gc.strokeLine(x1, y1, x2, y2);
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double al = 10;
        gc.setFill(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
        gc.fillPolygon(
            new double[]{x2, x2 - al*Math.cos(angle-0.4), x2 - al*Math.cos(angle+0.4)},
            new double[]{y2, y2 - al*Math.sin(angle-0.4), y2 - al*Math.sin(angle+0.4)}, 3);
    }

    // ── 4. Statistics ────────────────────────────────────────────────────────
    private void drawStatistics(GraphicsContext gc, Color accent, AnimState s) {
        if (s.points == null) {
            s.points = new ArrayList<>();
            java.util.Random rng = new java.util.Random(7);
            for (int i = 0; i < 22; i++) {
                double xv = rng.nextDouble();
                double yv = 0.2 + 0.6 * xv + (rng.nextDouble() - 0.5) * 0.25;
                s.points.add(new double[]{xv, yv});
            }
        }
        double padL = 25, padT = 18, pW = CARD_W - padL - 15, pH = CARD_H - padT - 22;
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.2));
        gc.setLineWidth(1);
        gc.strokeLine(padL, padT + pH, padL + pW, padT + pH);
        gc.strokeLine(padL, padT, padL, padT + pH);
        double prog = Math.min(1.0, s.t / 3.0);
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.5 * prog));
        gc.setLineWidth(2);
        gc.strokeLine(padL, padT + pH * (1 - (0.2 + 0.0)*prog),
                padL + pW * prog, padT + pH * (1 - (0.2 + 0.6)*prog));
        for (int i = 0; i < s.points.size(); i++) {
            double[] pt = s.points.get(i);
            double appear = Math.min(1.0, Math.max(0, s.t - i * 0.12));
            double sx = padL + pt[0] * pW;
            double sy = padT + pH - pt[1] * pH;
            double alpha = 0.55 * appear;
            gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
            gc.fillOval(sx - 4, sy - 4, 8, 8);
            double predY = padT + pH - (0.2 + 0.6 * pt[0]) * pH;
            gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.15 * appear));
            gc.setLineWidth(1);
            gc.strokeLine(sx, sy, sx, predY);
        }
    }

    // ── 5. Trigonometry ──────────────────────────────────────────────────────
    private void drawTrigonometry(GraphicsContext gc, Color accent, AnimState s) {
        // Brightness ramp: starts faded, gradually brightens, capped so it never overpowers text
        double ramp = Math.min(1.0, s.t / 2.0);
        double brightness = 0.18 + 0.47 * (ramp * ramp * (3 - 2 * ramp));

        double cx = CARD_W * 0.68, cy = CARD_H * 0.5, r = 52;
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.2 * brightness));
        gc.setLineWidth(1);
        gc.strokeLine(cx - r - 8, cy, cx + r + 8, cy);
        gc.strokeLine(cx, cy - r - 8, cx, cy + r + 8);
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.3 * brightness));
        gc.setLineWidth(1.5);
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
        double angle = s.t * 0.9;
        double hx = cx + r * Math.cos(angle);
        double hy = cy - r * Math.sin(angle);
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.75 * brightness));
        gc.setLineWidth(2);
        gc.strokeLine(cx, cy, hx, hy);
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.4 * brightness));
        gc.setLineDashes(4, 4); gc.setLineWidth(1.2);
        gc.strokeLine(hx, hy, hx, cy);
        gc.setLineDashes();
        gc.setStroke(new Color(accent.getRed()+0.1, accent.getGreen(), accent.getBlue(), 0.3 * brightness));
        gc.setLineDashes(4, 4); gc.setLineWidth(1.2);
        gc.strokeLine(hx, cy, cx, cy);
        gc.setLineDashes();
        gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.85 * brightness));
        gc.fillOval(hx - 5, hy - 5, 10, 10);
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.4 * brightness));
        gc.setLineWidth(1.5);
        gc.beginPath();
        for (int i = 0; i <= 80; i++) {
            double t2 = angle - i * 0.08;
            double wx = (cx - r - 15) - i * 1.2;
            double wy = cy - Math.sin(t2) * r * 0.7;
            if (wx < 8) break;
            if (i == 0) gc.moveTo(wx, wy); else gc.lineTo(wx, wy);
        }
        gc.stroke();
        gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.45 * brightness));
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        gc.fillText("θ", cx + 12, cy - 5);
    }

    // ── 6. Algebra ───────────────────────────────────────────────────────────
    private void drawAlgebra(GraphicsContext gc, Color accent, AnimState s) {
        // Brightness ramp: starts faded, gradually brightens, capped so it never overpowers text
        double ramp = Math.min(1.0, s.t / 2.0);
        double brightness = 0.18 + 0.47 * (ramp * ramp * (3 - 2 * ramp));

        double padL = 30, padT = 18, pW = CARD_W - padL - 15, pH = CARD_H - padT - 25;
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.2 * brightness));
        gc.setLineWidth(1);
        gc.strokeLine(padL, padT + pH, padL + pW, padT + pH);
        gc.strokeLine(padL, padT, padL, padT + pH);
        gc.strokeLine(padL + pW / 2, padT, padL + pW / 2, padT + pH);
        double shift = Math.sin(s.t * 0.6) * 0.3;
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.6 * brightness));
        gc.setLineWidth(2);
        gc.beginPath();
        for (int i = 0; i <= 100; i++) {
            double xr = i / 100.0;
            double xm = (xr - 0.5 - shift);
            double yr = Math.min(1, 4 * xm * xm);
            double sx = padL + xr * pW;
            double sy = padT + yr * pH;
            if (i == 0) gc.moveTo(sx, sy); else gc.lineTo(sx, sy);
        }
        gc.stroke();
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.3 * brightness));
        gc.setLineWidth(1.5);
        gc.beginPath();
        for (int i = 0; i <= 100; i++) {
            double xr = i / 100.0;
            double yr = 0.5 - 0.4 * Math.sin((xr * 2 * Math.PI) + s.t);
            double sx = padL + xr * pW;
            double sy = padT + yr * pH;
            if (i == 0) gc.moveTo(sx, sy); else gc.lineTo(sx, sy);
        }
        gc.stroke();
        String[] eqs = {"y=x²","f(x)","ax+b","y=|x|"};
        for (int i = 0; i < eqs.length; i++) {
            double ey = (padT + 12) + i * 26 + 8 * Math.sin(s.t * 0.5 + i * 1.3);
            double alpha = (0.12 + 0.07 * Math.sin(s.t + i)) * brightness;
            gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
            gc.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
            gc.fillText(eqs[i], 5, ey);
        }
    }

    // ── 7. Fourier ───────────────────────────────────────────────────────────
    private void drawFourier(GraphicsContext gc, Color accent, AnimState s) {
        // Brightness ramp: starts faded, gradually brightens, capped so it never overpowers text
        double ramp = Math.min(1.0, s.t / 2.0);
        double brightness = 0.18 + 0.47 * (ramp * ramp * (3 - 2 * ramp));

        double midY = CARD_H * 0.5;
        double ampScale = CARD_H * 0.35;
        int harmonics = 5;
        for (int k = 1; k <= harmonics; k++) {
            double alpha = (0.08 + 0.04 / k) * brightness;
            gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
            gc.setLineWidth(1);
            gc.beginPath();
            for (int i = 0; i <= (int)CARD_W; i++) {
                double x = i;
                double y = midY - (ampScale / k) * Math.sin(k * (x / CARD_W) * Math.PI * 4 + s.t * k * 0.6);
                if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
            }
            gc.stroke();
        }
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.65 * brightness));
        gc.setLineWidth(2.2);
        gc.beginPath();
        for (int i = 0; i <= (int)CARD_W; i++) {
            double x = i;
            double y = midY;
            for (int k = 1; k <= harmonics; k += 2) {
                y -= (ampScale / k) * Math.sin(k * (x / CARD_W) * Math.PI * 4 + s.t * 0.5);
            }
            if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
        }
        gc.stroke();
        double ecx = CARD_W * 0.85, ecy = CARD_H * 0.5, er = 22;
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.25 * brightness));
        gc.setLineWidth(1);
        gc.strokeOval(ecx - er, ecy - er, er * 2, er * 2);
        double ehx = ecx + er * Math.cos(s.t);
        double ehy = ecy + er * Math.sin(s.t);
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.55 * brightness));
        gc.setLineWidth(1.8);
        gc.strokeLine(ecx, ecy, ehx, ehy);
        gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.7 * brightness));
        gc.fillOval(ehx - 4, ehy - 4, 8, 8);
    }

    // ── 8. Chaos ─────────────────────────────────────────────────────────────
    private void drawChaos(GraphicsContext gc, Color accent, AnimState s) {
        if (s.chaosPoints == null) {
            s.chaosPoints = new ArrayList<>();
            double px = CARD_W * 0.5, py = CARD_H * 0.1;
            double[] vx = {CARD_W*0.15, CARD_W*0.85, CARD_W*0.5};
            double[] vy = {CARD_H*0.92, CARD_H*0.92, CARD_H*0.08};
            java.util.Random rng = new java.util.Random(17);
            for (int i = 0; i < 1800; i++) {
                int v = rng.nextInt(3);
                px = (px + vx[v]) / 2;
                py = (py + vy[v]) / 2;
                s.chaosPoints.add(new double[]{px, py});
            }
        }
        int visible = Math.min(s.chaosPoints.size(), (int)(s.t * 60));
        for (int i = 0; i < visible; i++) {
            double[] pt = s.chaosPoints.get(i);
            double alpha = 0.25 + 0.1 * Math.sin(s.t * 1.5 + i * 0.02);
            gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
            gc.fillRect(pt[0], pt[1], 1.8, 1.8);
        }
        if (visible >= s.chaosPoints.size()) s.t = 0;
    }

    // ── 9. Convex Hull ───────────────────────────────────────────────────────
    private void drawConvexHullAnim(GraphicsContext gc, Color accent, AnimState s) {
        int numHull = 7;
        double[] hx = new double[numHull];
        double[] hy = new double[numHull];
        
        double cx = CARD_W / 2.0;
        double cy = CARD_H / 2.0;
        
        // Generate moving hull vertices
        for (int i = 0; i < numHull; i++) {
            double baseAngle = i * (Math.PI * 2.0 / numHull);
            // Add a slow overall rotation and slight individual oscillation
            double angle = baseAngle + s.t * 0.3 + 0.2 * Math.sin(s.t * 0.8 + i);
            double radiusX = 65 + 15 * Math.sin(s.t * 1.1 + i * 2);
            double radiusY = 45 + 10 * Math.cos(s.t * 0.9 + i * 3);
            
            hx[i] = cx + radiusX * Math.cos(angle);
            hy[i] = cy + radiusY * Math.sin(angle);
        }
        
        // Generate some moving inner points
        int numInner = 12;
        gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.15));
        for (int i = 0; i < numInner; i++) {
            double ix = cx + 30 * Math.cos(s.t * 0.5 + i * 2.4);
            double iy = cy + 20 * Math.sin(s.t * 0.7 + i * 3.1);
            gc.fillOval(ix - 3, iy - 3, 6, 6);
        }
        
        // Draw the hull edges
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.35));
        gc.setLineWidth(1.8);
        gc.strokePolygon(hx, hy, numHull);
        
        // Draw the hull vertices
        gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.5));
        for (int i = 0; i < numHull; i++) {
            gc.fillOval(hx[i] - 3.5, hy[i] - 3.5, 7, 7);
        }
        
        // Draw a simulated "sweeping" dashed ray that rotates
        double rayAngle = s.t * 1.5;
        double rLength = 90;
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.2));
        gc.setLineDashes(4, 4);
        gc.setLineWidth(1.5);
        gc.strokeLine(cx, cy, cx + rLength * Math.cos(rayAngle), cy + rLength * Math.sin(rayAngle));
        gc.setLineDashes();
    }

    // ── 10. Matrix: Plane Flattening Animation ──────────────────────────────
    private void drawMatrixAnim(GraphicsContext gc, Color accent, AnimState s) {
        // Shift center toward the right side of the card so it doesn't obstruct left-aligned text
        double cx = CARD_W * 0.68, cy = CARD_H * 0.50;
        double scale = 16;

        // Brightness ramp: starts very soft and faded, slowly gains brightness over 2.2s, never overpowering
        double ramp = Math.min(1.0, s.t / 2.2);
        // Smooth ease curve capped at a subtle maximum (0.65 max)
        double brightness = 0.20 + 0.45 * (ramp * ramp * (3 - 2 * ramp));

        // Periodic flattening cycle: oscillates between 2D open grid and completely flattened 1D line
        // flattenFactor: 0.0 = full 2D grid, 1.0 = completely flattened onto 1D line
        double flattenFactor = 0.5 - 0.5 * Math.cos(s.t * 1.3);

        // Gentle drift angle so the transformation is dynamic
        double theta = s.t * 0.2;
        double cosT = Math.cos(theta);
        double sinT = Math.sin(theta);

        // Basis vector i-hat
        double ma = cosT;
        double mc = sinT;

        // Basis vector j-hat: rotates toward i-hat until collinear at flattenFactor = 1.0
        // When flattenFactor = 0, j-hat is orthogonal at theta + PI/2
        // When flattenFactor = 1, j-hat aligns with i-hat at theta
        double jAngle = theta + (Math.PI / 2.0) * (1.0 - flattenFactor);
        double jLen = 1.0 - 0.25 * flattenFactor;
        double mb = jLen * Math.cos(jAngle);
        double md = jLen * Math.sin(jAngle);

        // 1) Faint static background grid
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.04 * brightness));
        gc.setLineWidth(0.5);
        for (int i = -6; i <= 6; i++) {
            gc.strokeLine(cx + i * scale, cy - 55, cx + i * scale, cy + 55);
            gc.strokeLine(cx - 65, cy + i * scale, cx + 65, cy + i * scale);
        }

        // 2) Transformed grid lines flattening in real time
        gc.setStroke(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.16 * brightness));
        gc.setLineWidth(0.8);
        int range = 5;
        for (int i = -range; i <= range; i++) {
            // Vertical lines (constant u, parameterized by v)
            double x1 = cx + (ma * i + mb * (-range)) * scale;
            double y1 = cy - (mc * i + md * (-range)) * scale;
            double x2 = cx + (ma * i + mb * range) * scale;
            double y2 = cy - (mc * i + md * range) * scale;
            gc.strokeLine(x1, y1, x2, y2);

            // Horizontal lines (constant v, parameterized by u)
            double hx1 = cx + (ma * (-range) + mb * i) * scale;
            double hy1 = cy - (mc * (-range) + md * i) * scale;
            double hx2 = cx + (ma * range + mb * i) * scale;
            double hy2 = cy - (mc * range + md * i) * scale;
            gc.strokeLine(hx1, hy1, hx2, hy2);
        }

        // 3) Determinant parallelogram (shrinks to 0 area when flattened)
        double ox = cx, oy = cy;
        double ix = cx + ma * scale * 1.8, iy = cy - mc * scale * 1.8;
        double jx = cx + mb * scale * 1.8, jy = cy - md * scale * 1.8;
        double ijx = ix + mb * scale * 1.8, ijy = iy - md * scale * 1.8;

        gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.08 * brightness * (1.0 - flattenFactor)));
        gc.fillPolygon(new double[]{ox, ix, ijx, jx}, new double[]{oy, iy, ijy, jy}, 4);

        // 4) Basis vectors (i-hat in emerald green, j-hat in rose red)
        gc.setStroke(new Color(0.30, 0.75, 0.58, 0.45 * brightness));
        gc.setLineWidth(1.8);
        gc.strokeLine(ox, oy, ix, iy);

        gc.setStroke(new Color(0.93, 0.27, 0.27, 0.45 * brightness));
        gc.setLineWidth(1.8);
        gc.strokeLine(ox, oy, jx, jy);

        // 5) Origin dot
        gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.45 * brightness));
        gc.fillOval(cx - 2.5, cy - 2.5, 5, 5);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static class AnimState {
        double t = 0;
        List<double[]> primes;
        List<double[]> points;
        List<double[]> chaosPoints;

        void reset() {
            t = 0;
        }
    }

    private String toRgba(Color c, double alpha) {
        return String.format("rgba(%d,%d,%d,%.2f)",
            (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255), alpha);
    }

    private void fadeCanvas(Canvas c, double from, double to, long durationMs) {
        final long[] start = {-1};
        c.setOpacity(from);
        AnimationTimer ft = new AnimationTimer() {
            @Override public void handle(long now) {
                if (start[0] < 0) start[0] = now;
                double prog = Math.min(1.0, (now - start[0]) / (durationMs * 1_000_000.0));
                c.setOpacity(from + (to - from) * prog);
                if (prog >= 1.0) this.stop();
            }
        };
        ft.start();
    }
}
