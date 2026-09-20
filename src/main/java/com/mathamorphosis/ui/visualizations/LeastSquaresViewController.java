package com.mathamorphosis.ui.visualizations;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for least_squares_view.fxml.
 * Owns all Least Squares Regression logic, driven by FXML-injected nodes.
 */
public class LeastSquaresViewController {

    // ── FXML-injected nodes ──────────────────────────────────────────────────
    @FXML private Pane         gridPane;
    @FXML private Label        instructionLabel;
    @FXML private ToggleButton userGuessToggle;
    @FXML private Button       calcBestFitBtn;
    @FXML private ToggleButton showErrorsToggle;

    @FXML private Label livePointCount;
    @FXML private Label liveEquationLabel;
    @FXML private Label liveLeastSquareSumLabel;
    
    @FXML private TextField coordXInput;
    @FXML private TextField coordYInput;
    @FXML private Button    plotBtn;
    @FXML private Button    resetBtn;
    @FXML private Button    resetCoordBtn;

    // ── Coordinate system constants ──────────────────────────────────────────
    private static final double WIDTH   = 1000;
    private static final double HEIGHT  = 680;
    private static final double MARGIN  = 60;
    private static final double GRAPH_W = WIDTH  - 2 * MARGIN;
    private static final double GRAPH_H = HEIGHT - 2 * MARGIN;
    private static final double X_MAX   = 20;
    private static final double Y_MAX   = 20;

    // ── State ────────────────────────────────────────────────────────────────
    private final List<Circle>    dataPoints   = new ArrayList<>();
    private final List<Line>      errorLines   = new ArrayList<>();

    private Line    userGuessLine  = null;
    private Line    bestFitLine    = null;
    private boolean bestFitActive  = false;
    private int     userGuessClicks = 0;
    private double  guessX1, guessY1;
    
    private Text    hoverText;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        drawAxesAndGrid();
        gridPane.setOnMouseClicked(this::handleGridClick);

        hoverText = new Text();
        hoverText.setFill(Color.WHITE);
        hoverText.setFont(Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 14));
        hoverText.setVisible(false);
        hoverText.setEffect(new DropShadow(3, Color.BLACK));
        gridPane.getChildren().add(hoverText);

        userGuessToggle.setOnAction(e -> {
            if (!userGuessToggle.isSelected()) {
                clearUserGuess();
                instructionLabel.setText("Guess line cancelled.");
            } else {
                userGuessClicks = 0;
                instructionLabel.setText("Click two points on the grid to draw your guess.");
            }
        });

        calcBestFitBtn.setOnAction(e -> calculateBestFit());
        calcBestFitBtn.setOnMouseEntered(e -> calcBestFitBtn.setStyle(
            "-fx-font-size: 15px; -fx-text-fill: #ffffff; -fx-font-weight: bold;" +
            "-fx-background-color: #5ba8e0; -fx-padding: 12px 24px;" +
            "-fx-border-color: #5ba8e0; -fx-border-radius: 8px;" +
            "-fx-border-width: 2px; -fx-cursor: hand;"
        ));
        calcBestFitBtn.setOnMouseExited(e -> calcBestFitBtn.setStyle(
            "-fx-font-size: 15px; -fx-text-fill: #5ba8e0; -fx-font-weight: bold;" +
            "-fx-background-color: #22224a; -fx-padding: 12px 24px;" +
            "-fx-border-color: #5ba8e0; -fx-border-radius: 8px;" +
            "-fx-border-width: 2px; -fx-cursor: hand;"
        ));

        plotBtn.setOnAction(e -> {
            try {
                double mx = Double.parseDouble(coordXInput.getText());
                double my = Double.parseDouble(coordYInput.getText());
                if (mx >= 0 && mx <= X_MAX && my >= 0 && my <= Y_MAX) {
                    addPoint(mx, my);
                } else {
                    instructionLabel.setText("Coordinates must be between 0 and 20.");
                }
            } catch (NumberFormatException ex) {
                instructionLabel.setText("Invalid coordinate format.");
            }
        });
        plotBtn.setOnMouseEntered(e -> plotBtn.setStyle(
            "-fx-background-color: #38bdf8; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand;"
        ));
        plotBtn.setOnMouseExited(e -> plotBtn.setStyle(
            "-fx-background-color: #5ba8e0; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand;"
        ));

        if (resetBtn != null) {
            resetBtn.setOnAction(e -> reset());
            resetBtn.setOnMouseEntered(e -> resetBtn.setStyle(
                "-fx-font-size: 15px; -fx-text-fill: #ffffff; -fx-font-weight: bold;" +
                "-fx-background-color: #f43f5e; -fx-padding: 12px 24px;" +
                "-fx-border-color: #f43f5e; -fx-border-radius: 8px;" +
                "-fx-border-width: 2px; -fx-cursor: hand;"
            ));
            resetBtn.setOnMouseExited(e -> resetBtn.setStyle(
                "-fx-font-size: 15px; -fx-text-fill: #f43f5e; -fx-font-weight: bold;" +
                "-fx-background-color: #22224a; -fx-padding: 12px 24px;" +
                "-fx-border-color: #f43f5e; -fx-border-radius: 8px;" +
                "-fx-border-width: 2px; -fx-cursor: hand;"
            ));
        }

        if (resetCoordBtn != null) {
            resetCoordBtn.setOnAction(e -> reset());
            resetCoordBtn.setOnMouseEntered(e -> resetCoordBtn.setStyle(
                "-fx-background-color: #e11d48; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand;"
            ));
            resetCoordBtn.setOnMouseExited(e -> resetCoordBtn.setStyle(
                "-fx-background-color: #f43f5e; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand;"
            ));
        }

        showErrorsToggle.setOnAction(e -> updateVisuals());
    }

    // ── Grid / Axes ──────────────────────────────────────────────────────────

    private void drawAxesAndGrid() {
        for (int i = 0; i <= X_MAX; i++) {
            double sx = toScreenX(i);
            Line vLine = new Line(sx, MARGIN, sx, HEIGHT - MARGIN);
            vLine.setStroke(Color.web("#32325a", 0.5));
            gridPane.getChildren().add(vLine);
            if (i % 5 == 0 && i > 0) {
                Text lbl = new Text(String.valueOf(i));
                lbl.setFill(Color.web("#6868a0"));
                lbl.setFont(Font.font("Segoe UI", 12));
                lbl.setX(sx - 5); lbl.setY(HEIGHT - MARGIN + 20);
                gridPane.getChildren().add(lbl);
            }
        }
        for (int i = 0; i <= Y_MAX; i++) {
            double sy = toScreenY(i);
            Line hLine = new Line(MARGIN, sy, WIDTH - MARGIN, sy);
            hLine.setStroke(Color.web("#32325a", 0.5));
            gridPane.getChildren().add(hLine);
            if (i % 5 == 0 && i > 0) {
                Text lbl = new Text(String.valueOf(i));
                lbl.setFill(Color.web("#6868a0"));
                lbl.setFont(Font.font("Segoe UI", 12));
                lbl.setX(MARGIN - 25); lbl.setY(sy + 5);
                gridPane.getChildren().add(lbl);
            }
        }
        Line xAxis = new Line(MARGIN, HEIGHT - MARGIN, WIDTH - MARGIN + 10, HEIGHT - MARGIN);
        xAxis.setStroke(Color.web("#6868a0")); xAxis.setStrokeWidth(2);
        Line yAxis = new Line(MARGIN, HEIGHT - MARGIN, MARGIN, MARGIN - 10);
        yAxis.setStroke(Color.web("#6868a0")); yAxis.setStrokeWidth(2);
        gridPane.getChildren().addAll(xAxis, yAxis);
    }

    // ── Interaction ──────────────────────────────────────────────────────────

    private void handleGridClick(MouseEvent event) {
        if (event.getTarget() instanceof Circle) return;

        double mx = toMathX(event.getX());
        double my = toMathY(event.getY());
        if (mx < 0 || mx > X_MAX || my < 0 || my > Y_MAX) return;

        if (userGuessToggle.isSelected()) {
            if (userGuessClicks == 0) {
                guessX1 = event.getX(); guessY1 = event.getY();
                userGuessClicks++;
                instructionLabel.setText("First point set. Click a second point.");
            } else {
                clearUserGuess();
                userGuessLine = new Line(guessX1, guessY1, event.getX(), event.getY());
                userGuessLine.setStroke(Color.web("#b0b0d0"));
                userGuessLine.setStrokeWidth(3);
                userGuessLine.getStrokeDashArray().addAll(10d, 10d);
                userGuessLine.setEffect(new DropShadow(5, Color.BLACK));
                gridPane.getChildren().add(userGuessLine);
                extendLine(userGuessLine);
                userGuessClicks = 0;
                userGuessToggle.setSelected(false);
                instructionLabel.setText("Guess line drawn! Now calculate the true best fit.");
            }
            return;
        }
        addPoint(mx, my);
    }

    private void extendLine(Line line) {
        double x1 = line.getStartX(), y1 = line.getStartY();
        double x2 = line.getEndX(),   y2 = line.getEndY();
        if (x1 == x2) return;
        double m = (y2 - y1) / (x2 - x1);
        double b = y1 - m * x1;
        line.setStartX(MARGIN);           line.setStartY(m * MARGIN + b);
        line.setEndX(WIDTH - MARGIN);     line.setEndY(m * (WIDTH - MARGIN) + b);
    }

    private void clearUserGuess() {
        if (userGuessLine != null) {
            gridPane.getChildren().remove(userGuessLine);
            userGuessLine = null;
        }
    }

    private void reset() {
        // Remove plotted data points from the grid
        gridPane.getChildren().removeAll(dataPoints);
        dataPoints.clear();

        // Clear residual error lines
        gridPane.getChildren().removeAll(errorLines);
        errorLines.clear();

        // Clear mathematical best fit line
        if (bestFitLine != null) {
            gridPane.getChildren().remove(bestFitLine);
            bestFitLine = null;
        }
        bestFitActive = false;

        // Clear user guess line and reset toggles
        clearUserGuess();
        userGuessClicks = 0;
        userGuessToggle.setSelected(false);
        showErrorsToggle.setSelected(false);

        // Reset input coordinates
        coordXInput.setText("0");
        coordYInput.setText("0");

        // Hide hover tooltip
        if (hoverText != null) {
            hoverText.setVisible(false);
        }

        // Reset live stats labels
        livePointCount.setText("Data Points: 0");
        liveEquationLabel.setText("ŷ = —");
        if (liveLeastSquareSumLabel != null) {
            liveLeastSquareSumLabel.setText("Sum: —");
        }
        instructionLabel.setText("Click anywhere on the grid to add data points. Drag them to adjust.");
    }

    private void addPoint(double mathX, double mathY) {
        double sx = toScreenX(mathX), sy = toScreenY(mathY);

        Circle point = new Circle(sx, sy, 8, Color.web("#facc15"));
        point.setStroke(Color.WHITE);
        point.setStrokeWidth(2);
        point.setCursor(javafx.scene.Cursor.HAND);
        point.setEffect(new DropShadow(8, Color.web("#facc15")));

        point.setOnMouseEntered(e -> {
            point.setRadius(10);
            hoverText.setText(String.format("(%.2f, %.2f)", toMathX(point.getCenterX()), toMathY(point.getCenterY())));
            hoverText.setX(point.getCenterX() + 15);
            hoverText.setY(point.getCenterY() - 15);
            hoverText.toFront();
            hoverText.setVisible(true);
        });
        point.setOnMouseExited(e  -> {
            point.setRadius(8);
            hoverText.setVisible(false);
        });
        point.setOnMouseDragged(e -> {
            double nx = Math.max(MARGIN, Math.min(WIDTH - MARGIN,  e.getX()));
            double ny = Math.max(MARGIN, Math.min(HEIGHT - MARGIN, e.getY()));
            point.setCenterX(nx); point.setCenterY(ny);
            hoverText.setText(String.format("(%.2f, %.2f)", toMathX(nx), toMathY(ny)));
            hoverText.setX(nx + 15);
            hoverText.setY(ny - 15);
            hoverText.toFront();
            updateVisuals();
        });

        dataPoints.add(point);
        gridPane.getChildren().add(point);

        // Pop-in animation
        point.setRadius(0);
        new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(point.radiusProperty(), 8))).play();

        livePointCount.setText("Data Points: " + dataPoints.size());
        updateVisuals();
    }

    private void calculateBestFit() {
        if (dataPoints.size() < 2) {
            instructionLabel.setText("Please add at least 2 points first!");
            return;
        }
        bestFitActive = true;
        if (userGuessToggle.isSelected()) { userGuessToggle.setSelected(false); clearUserGuess(); }
        instructionLabel.setText("Mathematical Best Fit calculated! Try dragging points to see it react.");
        updateVisuals();
    }

    private void updateVisuals() {
        livePointCount.setText("Data Points: " + dataPoints.size());
        if (!bestFitActive || dataPoints.size() < 2) return;

        List<Double> mXs = new ArrayList<>(), mYs = new ArrayList<>();
        double sumX = 0, sumY = 0;
        for (Circle c : dataPoints) {
            double mx = toMathX(c.getCenterX()), my = toMathY(c.getCenterY());
            mXs.add(mx); mYs.add(my); sumX += mx; sumY += my;
        }

        double meanX = sumX / dataPoints.size(), meanY = sumY / dataPoints.size();
        double num = 0, den = 0;
        for (int i = 0; i < dataPoints.size(); i++) {
            num += (mXs.get(i) - meanX) * (mYs.get(i) - meanY);
            den += Math.pow(mXs.get(i) - meanX, 2);
        }
        double m = den == 0 ? 0 : num / den;
        double b = meanY - m * meanX;

        liveEquationLabel.setText(String.format("ŷ = %.4fx + %.4f", m, b));

        double ssr = 0;
        for (int i = 0; i < dataPoints.size(); i++) {
            double predictedY = m * mXs.get(i) + b;
            ssr += Math.pow(mYs.get(i) - predictedY, 2);
        }
        if (liveLeastSquareSumLabel != null) {
            liveLeastSquareSumLabel.setText(String.format("Sum: %.4f", ssr));
        }

        if (bestFitLine == null) {
            bestFitLine = new Line();
            bestFitLine.setStroke(Color.web("#5ba8e0"));
            bestFitLine.setStrokeWidth(5);
            bestFitLine.setEffect(new DropShadow(15, Color.web("#5ba8e0")));
            gridPane.getChildren().add(bestFitLine);
        }
        bestFitLine.setStartX(toScreenX(0));     bestFitLine.setStartY(toScreenY(b));
        bestFitLine.setEndX(toScreenX(X_MAX));   bestFitLine.setEndY(toScreenY(m * X_MAX + b));

        gridPane.getChildren().removeAll(errorLines);
        errorLines.clear();

        if (showErrorsToggle.isSelected()) {
            double startX = toScreenX(0);
            double startY = toScreenY(b);
            double endX = toScreenX(X_MAX);
            double endY = toScreenY(m * X_MAX + b);
            double dx = endX - startX;
            double dy = endY - startY;
            double len2 = dx * dx + dy * dy;

            for (Circle c : dataPoints) {
                double cx = c.getCenterX(), cy = c.getCenterY();
                
                double px = cx, py = cy;
                if (len2 > 0) {
                    double t = ((cx - startX) * dx + (cy - startY) * dy) / len2;
                    px = startX + t * dx;
                    py = startY + t * dy;
                }

                Line errLine = new Line(cx, cy, px, py);
                errLine.setStroke(Color.web("#f43f5e"));
                errLine.setStrokeWidth(2);
                errLine.getStrokeDashArray().addAll(5d, 5d);
                errorLines.add(errLine);
            }
            gridPane.getChildren().addAll(errorLines);
            bestFitLine.toFront();
            dataPoints.forEach(javafx.scene.Node::toFront);
            if (hoverText != null) hoverText.toFront();
        }
    }

    // ── Coordinate helpers ───────────────────────────────────────────────────

    private double toScreenX(double mathX) { return MARGIN + (mathX / X_MAX) * GRAPH_W; }
    private double toScreenY(double mathY) { return HEIGHT - MARGIN - (mathY / Y_MAX) * GRAPH_H; }
    private double toMathX(double sx)      { return ((sx - MARGIN) / GRAPH_W) * X_MAX; }
    private double toMathY(double sy)      { return ((HEIGHT - MARGIN - sy) / GRAPH_H) * Y_MAX; }
}
