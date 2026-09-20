package com.mathamorphosis.ui.visualizations;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.Locale;

/**
 * Controller for matrix_transform_view.fxml.
 * Visualises how a 2×2 matrix transforms the 2-D plane:
 *   – static Cartesian grid, transformed grid, basis vectors,
 *     determinant parallelogram, and a reference shape ('M').
 * Supports preset matrices and smooth animation between states.
 */
public class MatrixTransformViewController {

    // ── FXML-injected nodes ─────────────────────────────────────────────────
    @FXML private Canvas    canvas;
    @FXML private StackPane canvasContainer;

    @FXML private TextField valA;
    @FXML private TextField valB;
    @FXML private TextField valC;
    @FXML private TextField valD;

    @FXML private Button identityBtn;
    @FXML private Button rotate45Btn;
    @FXML private Button shearBtn;
    @FXML private Button scale2xBtn;
    @FXML private Button scaleBtn;
    @FXML private Button collapseBtn;
    @FXML private Button recenterBtn;
    @FXML private Button animateBtn;

    @FXML private HBox      scalePopupBox;
    @FXML private TextField scaleFactorInput;
    @FXML private Button    applyScaleBtn;
    @FXML private Button    closeScalePopupBtn;

    @FXML private Label determinantLabel;
    @FXML private Label iHatLabel;
    @FXML private Label jHatLabel;

    // ── Current (live) matrix values ────────────────────────────────────────
    private double a = 1, b = 0, c = 0, d = 1;

    // ── Drag interaction state ──────────────────────────────────────────────
    private int draggedVector = 0; // 0 = none, 1 = Vector-1, 2 = Vector-2
    private double lastMouseX, lastMouseY;

    // ── Animation state ─────────────────────────────────────────────────────
    private double srcA, srcB, srcC, srcD;       // start of interpolation
    private double tgtA, tgtB, tgtC, tgtD;       // end   of interpolation
    private boolean animating = false;
    private long    animStartNanos;
    private static final long ANIM_DURATION_NS = 1_500_000_000L; // 1.5 s

    // ── Rendering helpers & camera zoom/pan ─────────────────────────────────
    private GraphicsContext gc;
    private AnimationTimer  renderTimer;
    private static final double DEFAULT_SCALE = 60.0;
    private double scale = DEFAULT_SCALE;
    private double cx;
    private double cy;
    private boolean customOrigin = false;

    // ── Lifecycle ───────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        gc = canvas.getGraphicsContext2D();

        cx = canvas.getWidth() / 2.0;
        cy = canvas.getHeight() / 2.0;

        // Bind canvas size to its container
        canvas.widthProperty().bind(canvasContainer.widthProperty());
        canvas.heightProperty().bind(canvasContainer.heightProperty());
        canvas.widthProperty().addListener((o, ov, nv) -> {
            if (!customOrigin) {
                cx = nv.doubleValue() / 2.0;
            }
            draw();
        });
        canvas.heightProperty().addListener((o, ov, nv) -> {
            if (!customOrigin) {
                cy = nv.doubleValue() / 2.0;
            }
            draw();
        });

        // Mouse scroll zoom logic
        canvas.setOnScroll(e -> {
            hideScalePopup();
            double deltaY = e.getDeltaY();
            if (deltaY > 0) {
                scale *= 1.1;
            } else if (deltaY < 0) {
                scale /= 1.1;
            }
            scale = Math.max(5.0, Math.min(scale, 300.0));
            draw();
        });

        // Preset buttons
        identityBtn.setOnAction(e -> { hideScalePopup(); setTarget(1, 0, 0, 1); });
        rotate45Btn.setOnAction(e -> {
            hideScalePopup();
            double cos45 = Math.cos(Math.PI / 4);
            double sin45 = Math.sin(Math.PI / 4);
            multiply(cos45, -sin45, sin45, cos45);
        });
        shearBtn.setOnAction(e -> { hideScalePopup(); multiply(1, 1, 0, 1); });

        Button activeScaleBtn = scaleBtn != null ? scaleBtn : scale2xBtn;
        if (activeScaleBtn != null) {
            activeScaleBtn.setText("Scale");
            activeScaleBtn.setOnAction(e -> toggleScalePopup());
        }

        if (applyScaleBtn != null) {
            applyScaleBtn.setOnAction(e -> applyScale());
            applyScaleBtn.setOnMouseEntered(e -> applyScaleBtn.setStyle("-fx-background-color: #5bd0dc; -fx-text-fill: #0c0c1e; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 4 10;"));
            applyScaleBtn.setOnMouseExited(e -> applyScaleBtn.setStyle("-fx-background-color: #4ab8c4; -fx-text-fill: #0c0c1e; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 4 10;"));
        }
        if (closeScalePopupBtn != null) {
            closeScalePopupBtn.setOnAction(e -> hideScalePopup());
            closeScalePopupBtn.setOnMouseEntered(e -> closeScalePopupBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #eaeaf5; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 4 6;"));
            closeScalePopupBtn.setOnMouseExited(e -> closeScalePopupBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8080b0; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 4 6;"));
        }
        if (scaleFactorInput != null) {
            scaleFactorInput.setOnAction(e -> applyScale());
            scaleFactorInput.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    hideScalePopup();
                }
            });
            scaleFactorInput.textProperty().addListener((obs, oldV, newV) -> {
                scaleFactorInput.setStyle("-fx-background-color: #0c0c1e; -fx-text-fill: #4ab8c4; -fx-font-size: 13px; -fx-font-weight: bold; -fx-font-family: 'Courier New', monospace; -fx-border-color: #4ab8c4; -fx-border-radius: 5; -fx-background-radius: 5; -fx-alignment: CENTER;");
            });
        }

        // Close scale popup when clicking outside
        if (scalePopupBox != null) {
            scalePopupBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                        if (scalePopupBox != null && scalePopupBox.isVisible()) {
                            Button currentBtn = scaleBtn != null ? scaleBtn : scale2xBtn;
                            Bounds popupBounds = scalePopupBox.localToScene(scalePopupBox.getBoundsInLocal());
                            boolean inPopup = popupBounds.contains(e.getSceneX(), e.getSceneY());
                            boolean inBtn = currentBtn != null && currentBtn.localToScene(currentBtn.getBoundsInLocal()).contains(e.getSceneX(), e.getSceneY());
                            if (!inPopup && !inBtn) {
                                hideScalePopup();
                            }
                        }
                    });
                }
            });
        }

        collapseBtn.setOnAction(e -> { hideScalePopup(); multiply(1, 2, 0.5, 1); });

        if (recenterBtn != null) {
            recenterBtn.setOnAction(e -> {
                hideScalePopup();
                recenterCamera();
            });
        }

        animateBtn.setOnAction(e -> { hideScalePopup(); animateToFields(); });

        setupMouseInteraction();

        // Start render loop
        renderTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (animating) {
                    double elapsed = (now - animStartNanos);
                    double t = Math.min(1.0, elapsed / ANIM_DURATION_NS);
                    // Smooth ease-in-out
                    double s = t < 0.5
                            ? 2 * t * t
                            : 1 - 2 * (1 - t) * (1 - t);
                    a = srcA + (tgtA - srcA) * s;
                    b = srcB + (tgtB - srcB) * s;
                    c = srcC + (tgtC - srcC) * s;
                    d = srcD + (tgtD - srcD) * s;

                    // Sync text fields every frame so column values roll dynamically
                    updateFields();

                    if (t >= 1.0) {
                        animating = false;
                        a = tgtA; b = tgtB; c = tgtC; d = tgtD;
                        updateFields();
                    }
                }
                draw();
            }
        };
        renderTimer.start();

        // Stop timer when removed from scene
        canvas.sceneProperty().addListener((obs, o, n) -> {
            if (n == null) renderTimer.stop();
        });

        draw();
    }

    // ── Coordinate mapping & camera ─────────────────────────────────────────
    //  Matrix layout (column-major basis vectors):
    //    | a  b |    column 1 (a,c) = î-hat (green arrow, X-Basis)
    //    | c  d |    column 2 (b,d) = ĵ-hat (red arrow,   Y-Basis)
    //
    //  Cartesian (x,y) → screen:  screen = center + M·v * scale
    //  JavaFX Y-axis is inverted, hence the minus sign in mapY.

    private void recenterCamera() {
        scale = DEFAULT_SCALE;
        cx = canvas.getWidth() / 2.0;
        cy = canvas.getHeight() / 2.0;
        customOrigin = false;
        draw();
    }

    private double mapX(double x, double y) {
        return centerX() + (a * x + b * y) * scale;
    }

    private double mapY(double x, double y) {
        return centerY() - (c * x + d * y) * scale;
    }

    /** Inverse mapping (screen to Cartesian math coordinates). */
    double inverseMapX(double screenX) {
        return (screenX - centerX()) / scale;
    }

    double inverseMapY(double screenY) {
        return -(screenY - centerY()) / scale;
    }

    /** Identity mapping — for the static background grid. */
    private double rawX(double x) { return centerX() + x * scale; }
    private double rawY(double y) { return centerY() - y * scale; }

    private double centerX() {
        if (!customOrigin && cx == 0 && canvas.getWidth() > 0) {
            cx = canvas.getWidth() / 2.0;
        }
        return cx;
    }

    private double centerY() {
        if (!customOrigin && cy == 0 && canvas.getHeight() > 0) {
            cy = canvas.getHeight() / 2.0;
        }
        return cy;
    }

    // ── Draw ────────────────────────────────────────────────────────────────

    private void draw() {
        double W = canvas.getWidth();
        double H = canvas.getHeight();
        if (W <= 0 || H <= 0) return;

        // Clear
        gc.setFill(Color.web("#0c0c1e"));
        gc.fillRect(0, 0, W, H);

        drawStaticGrid(W, H);
        drawTransformedGrid();
        drawDeterminantParallelogram();
        drawReferenceShape();
        drawBasisVectors();
        updateDiagnostics();
    }

    // 1) Static Cartesian grid --------------------------------------------------
    private void drawStaticGrid(double W, double H) {
        gc.setStroke(Color.web("#1a1a3a", 0.6));
        gc.setLineWidth(0.5);

        double D = Math.hypot(W, H) / 2.0;
        int r = (int) Math.min(80, Math.max(12, Math.ceil(D / scale)));

        for (int i = -r; i <= r; i++) {
            double sx = rawX(i);
            gc.strokeLine(sx, 0, sx, H);
            double sy = rawY(i);
            gc.strokeLine(0, sy, W, sy);
        }
        // Axes
        gc.setStroke(Color.web("#32325a"));
        gc.setLineWidth(1.2);
        gc.strokeLine(0, centerY(), W, centerY());
        gc.strokeLine(centerX(), 0, centerX(), H);
    }

    // 2) Transformed grid -------------------------------------------------------
    private void drawTransformedGrid() {
        double W = canvas.getWidth();
        double H = canvas.getHeight();
        if (W <= 0 || H <= 0) return;

        gc.setStroke(Color.web("#4ab8c4", 0.35));
        gc.setLineWidth(1);

        double D = Math.hypot(W, H) / 2.0;
        double R = D / Math.max(scale, 5.0);

        double len1 = Math.hypot(a, c); // length of i-hat
        double len2 = Math.hypot(b, d); // length of j-hat

        // Dynamically compute lines needed based on vector lengths so under-scaling fills the view
        int uCount = (len1 > 1e-4) ? (int) Math.min(80, Math.max(10, Math.ceil(R / len1))) : 0;
        int vCount = (len2 > 1e-4) ? (int) Math.min(80, Math.max(10, Math.ceil(R / len2))) : 0;

        // The span (extent) of each line along the other vector
        int uSpan = (len1 > 1e-4) ? (int) Math.min(100, Math.max(10, Math.ceil(R / len1) + 2)) : 10;
        int vSpan = (len2 > 1e-4) ? (int) Math.min(100, Math.max(10, Math.ceil(R / len2) + 2)) : 10;

        // Vertical lines in transformed space (constant-u, parameterized by v)
        if (uCount > 0) {
            for (int i = -uCount; i <= uCount; i++) {
                gc.strokeLine(mapX(i, -vSpan), mapY(i, -vSpan), mapX(i, vSpan), mapY(i, vSpan));
            }
        }
        // Horizontal lines in transformed space (constant-v, parameterized by u)
        if (vCount > 0) {
            for (int j = -vCount; j <= vCount; j++) {
                gc.strokeLine(mapX(-uSpan, j), mapY(-uSpan, j), mapX(uSpan, j), mapY(uSpan, j));
            }
        }
    }

    // 3) Determinant parallelogram ----------------------------------------------
    private void drawDeterminantParallelogram() {
        double ox = mapX(0, 0), oy = mapY(0, 0);
        double ix = mapX(1, 0), iy = mapY(1, 0);
        double jx = mapX(0, 1), jy = mapY(0, 1);
        double ijx = mapX(1, 1), ijy = mapY(1, 1);

        gc.setFill(Color.web("#4ab8c4", 0.12));
        gc.fillPolygon(
                new double[]{ox, ix, ijx, jx},
                new double[]{oy, iy, ijy, jy}, 4);
        gc.setStroke(Color.web("#4ab8c4", 0.3));
        gc.setLineWidth(1.2);
        gc.strokePolygon(
                new double[]{ox, ix, ijx, jx},
                new double[]{oy, iy, ijy, jy}, 4);
    }

    // 4) Reference shape — letter 'M' ------------------------------------------
    private void drawReferenceShape() {
        // Define 'M' as a series of (x, y) coordinates in standard space
        double[][] mPts = {
                {-0.5, 0}, {-0.5, 1.2}, {-0.25, 0.6}, {0, 1.2}, {0, 0},  // right stroke
                {0, 1.2}, {0.25, 0.6}, {0.5, 1.2}, {0.5, 0}
        };

        // Shift the M so it sits nicely (centred around 2,1)
        double offX = 2.0, offY = 0.5;

        gc.setStroke(Color.web("#d4a84b", 0.85));
        gc.setLineWidth(2.5);
        gc.beginPath();
        for (int i = 0; i < mPts.length; i++) {
            double sx = mapX(mPts[i][0] + offX, mPts[i][1] + offY);
            double sy = mapY(mPts[i][0] + offX, mPts[i][1] + offY);
            if (i == 0) gc.moveTo(sx, sy); else gc.lineTo(sx, sy);
        }
        gc.stroke();

        // Also draw vertices
        gc.setFill(Color.web("#d4a84b", 0.6));
        for (double[] pt : mPts) {
            double sx = mapX(pt[0] + offX, pt[1] + offY);
            double sy = mapY(pt[0] + offX, pt[1] + offY);
            gc.fillOval(sx - 3, sy - 3, 6, 6);
        }
    }

    // 5) Basis vectors (bold arrows) -------------------------------------------
    private void drawBasisVectors() {
        double ox = mapX(0, 0), oy = mapY(0, 0);

        // î-hat → green
        double ix = mapX(1, 0), iy = mapY(1, 0);
        drawBoldArrow(ox, oy, ix, iy, Color.web("#4cbf95"), 3.5);

        // ĵ-hat → red
        double jx = mapX(0, 1), jy = mapY(0, 1);
        drawBoldArrow(ox, oy, jx, jy, Color.web("#ef4444"), 3.5);

        // Origin dot
        gc.setFill(Color.WHITE);
        gc.fillOval(ox - 4, oy - 4, 8, 8);

        // Draggable handle visual cues at basis vector tips (semi-transparent, radius 6)
        gc.setFill(Color.web("#fde047", 0.75));
        gc.fillOval(ix - 6, iy - 6, 12, 12);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.5);
        gc.strokeOval(ix - 6, iy - 6, 12, 12);

        gc.setFill(Color.web("#fde047", 0.75));
        gc.fillOval(jx - 6, jy - 6, 12, 12);
        gc.setStroke(Color.WHITE);
        gc.strokeOval(jx - 6, jy - 6, 12, 12);
    }

    private void drawBoldArrow(double x1, double y1, double x2, double y2,
                               Color color, double lineWidth) {
        gc.setStroke(color);
        gc.setLineWidth(lineWidth);
        gc.strokeLine(x1, y1, x2, y2);

        // Arrow head
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double headLen = 14;
        gc.setFill(color);
        gc.fillPolygon(
                new double[]{
                        x2,
                        x2 - headLen * Math.cos(angle - 0.35),
                        x2 - headLen * Math.cos(angle + 0.35)
                },
                new double[]{
                        y2,
                        y2 - headLen * Math.sin(angle - 0.35),
                        y2 - headLen * Math.sin(angle + 0.35)
                }, 3);
    }

    // ── Diagnostics ─────────────────────────────────────────────────────────

    private void updateDiagnostics() {
        double det = a * d - b * c;
        determinantLabel.setText(String.format("det = %.2f  (Area Scale: %.2f)", det, Math.abs(det)));
        if (Math.abs(det) < 1e-6) {
            determinantLabel.setStyle(
                    "-fx-text-fill: #ef4444; -fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: 'Courier New', monospace;");
        } else {
            determinantLabel.setStyle(
                    "-fx-text-fill: #4cbf95; -fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: 'Courier New', monospace;");
        }

        iHatLabel.setText(String.format("î  → (%.2f, %.2f)", a, c));
        jHatLabel.setText(String.format("ĵ  → (%.2f, %.2f)", b, d));
    }

    // ── Mouse interaction ───────────────────────────────────────────────────

    private void setupMouseInteraction() {
        final double HIT_RADIUS = 15.0;

        canvas.setOnMousePressed(e -> {
            hideScalePopup();
            double v1x = mapX(1, 0);
            double v1y = mapY(1, 0);
            double v2x = mapX(0, 1);
            double v2y = mapY(0, 1);

            double d1 = Math.hypot(e.getX() - v1x, e.getY() - v1y);
            double d2 = Math.hypot(e.getX() - v2x, e.getY() - v2y);

            if (d1 <= HIT_RADIUS && (d1 <= d2)) {
                draggedVector = 1;
                animating = false;
            } else if (d2 <= HIT_RADIUS) {
                draggedVector = 2;
                animating = false;
            } else {
                draggedVector = 0;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (draggedVector == 1) {
                a = inverseMapX(e.getX());
                c = inverseMapY(e.getY());
                updateFields();
                draw();
            } else if (draggedVector == 2) {
                b = inverseMapX(e.getX());
                d = inverseMapY(e.getY());
                updateFields();
                draw();
            } else {
                double dx = e.getX() - lastMouseX;
                double dy = e.getY() - lastMouseY;
                cx += dx;
                cy += dy;
                customOrigin = true;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                draw();
            }
        });

        canvas.setOnMouseReleased(e -> draggedVector = 0);
    }

    // ── Scale popup controls ────────────────────────────────────────────────

    private void toggleScalePopup() {
        if (scalePopupBox == null) return;
        if (scalePopupBox.isVisible()) {
            hideScalePopup();
        } else {
            showScalePopup();
        }
    }

    private void showScalePopup() {
        if (scalePopupBox == null) return;
        Button currentBtn = scaleBtn != null ? scaleBtn : scale2xBtn;
        if (currentBtn != null && currentBtn.getParent() != null) {
            currentBtn.getParent().toFront();
        }
        scalePopupBox.toFront();

        scalePopupBox.autosize();
        if (currentBtn != null) {
            double btnW = currentBtn.getWidth();
            double popW = scalePopupBox.getWidth();
            double popH = scalePopupBox.getHeight();
            scalePopupBox.setLayoutX((btnW - popW) / 2.0);
            scalePopupBox.setLayoutY(-popH - 8.0);
        }

        scalePopupBox.setVisible(true);
        scalePopupBox.setOpacity(0.0);
        scalePopupBox.setTranslateY(6.0);

        FadeTransition ft = new FadeTransition(Duration.millis(160), scalePopupBox);
        ft.setToValue(1.0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(160), scalePopupBox);
        tt.setToY(0.0);
        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.play();

        if (scaleFactorInput != null) {
            scaleFactorInput.requestFocus();
            scaleFactorInput.selectAll();
        }
    }

    private void hideScalePopup() {
        if (scalePopupBox == null || !scalePopupBox.isVisible()) return;
        FadeTransition ft = new FadeTransition(Duration.millis(120), scalePopupBox);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> {
            scalePopupBox.setVisible(false);
            scalePopupBox.setOpacity(1.0);
            scalePopupBox.setTranslateY(0.0);
        });
        ft.play();
    }

    private void applyScale() {
        if (scaleFactorInput == null) return;
        String text = scaleFactorInput.getText().trim();
        try {
            String[] parts = text.split("[,\\s]+");
            double sx, sy;
            if (parts.length == 1) {
                sx = Double.parseDouble(parts[0]);
                sy = sx;
            } else if (parts.length >= 2) {
                sx = Double.parseDouble(parts[0]);
                sy = Double.parseDouble(parts[1]);
            } else {
                return;
            }
            multiply(sx, 0, 0, sy);
            hideScalePopup();
        } catch (NumberFormatException e) {
            scaleFactorInput.setStyle("-fx-background-color: #0c0c1e; -fx-text-fill: #ef4444; -fx-font-size: 13px; -fx-font-weight: bold; -fx-font-family: 'Courier New', monospace; -fx-border-color: #ef4444; -fx-border-radius: 5; -fx-background-radius: 5; -fx-alignment: CENTER;");
        }
    }

    // ── Animation / presets ─────────────────────────────────────────────────

    /** Start an animated interpolation to the currently entered field values. */
    private void animateToFields() {
        double ta = parseOr(valA, a);
        double tb = parseOr(valB, b);
        double tc = parseOr(valC, c);
        double td = parseOr(valD, d);
        startAnimation(ta, tb, tc, td);
    }

    /**
     * Multiply the current matrix [a,b; c,d] by the given target matrix [ta,tb; tc,td]
     * and animate to the result. This makes presets composable (relative transforms).
     */
    private void multiply(double ta, double tb, double tc, double td) {
        double newA = a * ta + b * tc;
        double newB = a * tb + b * td;
        double newC = c * ta + d * tc;
        double newD = c * tb + d * td;
        setTarget(newA, newB, newC, newD);
    }

    /** Set target via preset and animate. */
    private void setTarget(double ta, double tb, double tc, double td) {
        valA.setText(fmt(ta));
        valB.setText(fmt(tb));
        valC.setText(fmt(tc));
        valD.setText(fmt(td));
        startAnimation(ta, tb, tc, td);
    }

    private void startAnimation(double ta, double tb, double tc, double td) {
        srcA = a; srcB = b; srcC = c; srcD = d;
        tgtA = ta; tgtB = tb; tgtC = tc; tgtD = td;
        animating = true;
        animStartNanos = System.nanoTime();
    }

    private void updateFields() {
        valA.setText(fmt(a));
        valB.setText(fmt(b));
        valC.setText(fmt(c));
        valD.setText(fmt(d));
    }

    // ── Utilities ───────────────────────────────────────────────────────────

    private static double parseOr(TextField tf, double fallback) {
        try {
            return Double.parseDouble(tf.getText().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String fmt(double v) {
        if (v == (long) v) return String.valueOf((long) v);
        return String.format(Locale.US, "%.2f", v);
    }
}
