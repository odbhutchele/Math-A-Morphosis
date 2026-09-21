package com.mathamorphosis.ui.visualizations;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

/**
 * Controller for vector_view.fxml.
 * Owns all Vector Projection logic, driven by FXML-injected nodes.
 */
public class VectorViewController {

    // ── FXML-injected nodes ──────────────────────────────────────────────────
    @FXML private Canvas canvas;
    @FXML private Button startBtn;
    @FXML private Button pauseBtn;
    @FXML private Button restartBtn;

    @FXML private Label vectorALabel;
    @FXML private Label vectorBLabel;
    @FXML private Label dotProductLabel;
    @FXML private Label projLabel;

    // ── Constants ────────────────────────────────────────────────────────────
    private static final double WIDTH    = 800;
    private static final double HEIGHT   = 600;
    private static final double ORIGIN_X = WIDTH  / 2;
    private static final double ORIGIN_Y = HEIGHT / 2;

    // ── State ────────────────────────────────────────────────────────────────
    private GraphicsContext gc;
    private AnimationTimer  timer;
    private boolean isRunning = false;

    private double bx = 200, by = 0;     // Vector B (anchor)
    private double ax = 100, ay = -150;  // Vector A (draggable / rotating)

    private boolean draggingA = false;
    private boolean draggingB = false;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        gc = canvas.getGraphicsContext2D();

        // Mouse interaction on canvas
        canvas.setOnMousePressed(e -> {
            double mx = e.getX(), my = e.getY();
            double distA = Math.hypot(mx - (ORIGIN_X + ax), my - (ORIGIN_Y + ay));
            double distB = Math.hypot(mx - (ORIGIN_X + bx), my - (ORIGIN_Y + by));
            if (distB < distA) { draggingB = true;  draggingA = false; }
            else               { draggingA = true;  draggingB = false; }
        });
        canvas.setOnMouseReleased(e -> { draggingA = false; draggingB = false; });
        canvas.setOnMouseDragged(e -> {
            if (draggingB) { bx = e.getX() - ORIGIN_X; by = e.getY() - ORIGIN_Y; }
            else           { ax = e.getX() - ORIGIN_X; ay = e.getY() - ORIGIN_Y; }
            if (isRunning && timer != null) { timer.stop(); isRunning = false; }
            draw();
        });

        // Playback controls
        startBtn.setOnAction(e -> {
            if (timer != null && !isRunning) { timer.start(); isRunning = true; }
        });
        pauseBtn.setOnAction(e -> {
            if (timer != null && isRunning) { timer.stop(); isRunning = false; }
        });
        restartBtn.setOnAction(e -> {
            ax = 100; ay = -150;
            draw();
            if (timer != null && !isRunning) { timer.start(); isRunning = true; }
        });

        initTimer();
        draw();
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
                double angle = Math.atan2(ay, ax);
                double mag   = Math.sqrt(ax * ax + ay * ay);
                double next  = angle + dt * 1.5;
                ax = mag * Math.cos(next);
                ay = mag * Math.sin(next);
                draw();
            }
            @Override public void stop() { super.stop(); lastUpdate = 0; }
        };
    }

    // ── Draw ─────────────────────────────────────────────────────────────────

    private void draw() {
        gc.setFill(Color.web("#14142a"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Grid
        gc.setStroke(Color.web("#1c1c38")); gc.setLineWidth(1);
        for (int i = 0; i < WIDTH;  i += 50) gc.strokeLine(i, 0, i, HEIGHT);
        for (int i = 0; i < HEIGHT; i += 50) gc.strokeLine(0, i, WIDTH, i);

        // Axes
        gc.setStroke(Color.web("#32325a")); gc.setLineWidth(2);
        gc.strokeLine(0, ORIGIN_Y, WIDTH, ORIGIN_Y);
        gc.strokeLine(ORIGIN_X, 0, ORIGIN_X, HEIGHT);

        // Calculations
        double dot    = ax * bx + ay * by;
        double bMagSq = bx * bx + by * by;
        if (bMagSq < 1e-6) bMagSq = 1e-6;
        double scalar = dot / bMagSq;
        double projX  = scalar * bx;
        double projY  = scalar * by;

        boolean blueDominant = Math.abs(scalar) >= 0.999;

        if (blueDominant) {
            // Projection behind B
            gc.setLineWidth(8);
            gc.setStroke(dot < 0 ? Color.web("#ef4444") : Color.web("#eab308"));
            gc.strokeLine(ORIGIN_X, ORIGIN_Y, ORIGIN_X + projX, ORIGIN_Y + projY);
            gc.setLineWidth(4);
            gc.setStroke(Color.web("#5ba8e0"));
            drawArrow(ORIGIN_X, ORIGIN_Y, ORIGIN_X + bx, ORIGIN_Y + by);
        } else {
            gc.setLineWidth(8);
            gc.setStroke(Color.web("#5ba8e0"));
            drawArrow(ORIGIN_X, ORIGIN_Y, ORIGIN_X + bx, ORIGIN_Y + by);
            gc.setLineWidth(4);
            gc.setStroke(dot < 0 ? Color.web("#ef4444") : Color.web("#eab308"));
            gc.strokeLine(ORIGIN_X, ORIGIN_Y, ORIGIN_X + projX, ORIGIN_Y + projY);
        }

        // Handle on B tip
        gc.setFill(Color.WHITE);
        gc.fillOval(ORIGIN_X + bx - 8, ORIGIN_Y + by - 8, 16, 16);

        // Dashed drop line
        gc.setLineWidth(2); gc.setStroke(Color.web("#6868a0")); gc.setLineDashes(10);
        gc.strokeLine(ORIGIN_X + ax, ORIGIN_Y + ay, ORIGIN_X + projX, ORIGIN_Y + projY);
        gc.setLineDashes(0);

        // Vector A
        gc.setLineWidth(4); gc.setStroke(Color.web("#f97316"));
        drawArrow(ORIGIN_X, ORIGIN_Y, ORIGIN_X + ax, ORIGIN_Y + ay);

        // Handle on A tip
        gc.setFill(Color.WHITE);
        gc.fillOval(ORIGIN_X + ax - 8, ORIGIN_Y + ay - 8, 16, 16);

        // Update live labels
        vectorALabel.setText(String.format("A = (%.1f, %.1f)", ax, -ay));
        vectorBLabel.setText(String.format("B = (%.1f, %.1f)", bx, -by));
        dotProductLabel.setText(String.format("A · B = %.1f", dot));
        projLabel.setText(String.format("proj = (%.1f, %.1f)", projX, -projY));
    }

    private void drawArrow(double x1, double y1, double x2, double y2) {
        gc.strokeLine(x1, y1, x2, y2);
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double len   = 15;
        gc.strokeLine(x2, y2, x2 - len * Math.cos(angle - Math.PI / 6), y2 - len * Math.sin(angle - Math.PI / 6));
        gc.strokeLine(x2, y2, x2 - len * Math.cos(angle + Math.PI / 6), y2 - len * Math.sin(angle + Math.PI / 6));
    }
}
