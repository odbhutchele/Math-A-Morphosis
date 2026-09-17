package com.mathamorphosis.ui.visualizations;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ConvexHullViewController {

    @FXML private Canvas canvas;
    @FXML private Button randomizeBtn;
    @FXML private Button clearBtn;
    @FXML private Button startResetBtn;
    @FXML private Button stepBtn;
    @FXML private Button playPauseBtn;
    @FXML private Slider speedSlider;

    @FXML private Label statusBadgeLabel;
    @FXML private Label stepTitleLabel;
    @FXML private Label stepDescLabel;
    @FXML private Label totalPointsLabel;
    @FXML private Label hullVerticesLabel;

    private List<Point> points = new ArrayList<>();
    private List<Point> hull = new ArrayList<>();
    
    private enum State { IDLE, FIND_MIN_X, SCANNING, EDGE_COMMIT, COMPLETE }
    private State currentState = State.IDLE;
    
    private Point activeAnchor = null;
    private Point candidatePoint = null;
    private Point bestNextPoint = null;
    private int candidateIndex = 0;
    
    private boolean isPlaying = false;
    private long lastStepTime = 0;
    private AnimationTimer timer;
    private Random random = new Random();

    public void initialize() {
        // Canvas setup
        canvas.setOnMouseClicked(e -> {
            if (currentState == State.IDLE) {
                if (e.getX() >= 40 && e.getX() <= canvas.getWidth() - 40 &&
                    e.getY() >= 40 && e.getY() <= canvas.getHeight() - 40) {
                    points.add(new Point(e.getX(), e.getY()));
                    updateMetrics();
                    draw();
                }
            }
        });

        // Buttons
        randomizeBtn.setOnAction(e -> {
            if (currentState != State.IDLE) resetAlgorithm();
            for (int i = 0; i < 15; i++) {
                double x = 40 + random.nextDouble() * (canvas.getWidth() - 80);
                double y = 40 + random.nextDouble() * (canvas.getHeight() - 80);
                points.add(new Point(x, y));
            }
            updateMetrics();
            draw();
        });

        clearBtn.setOnAction(e -> {
            points.clear();
            resetAlgorithm();
            updateMetrics();
            draw();
        });

        startResetBtn.setOnAction(e -> {
            if (currentState == State.IDLE) {
                if (points.size() < 3) {
                    setStatus("ERROR", "Need Points", "At least 3 points are required.", Color.RED);
                    return;
                }
                currentState = State.FIND_MIN_X;
                setStatus("RUNNING", "Initialization", "Finding the leftmost point...", Color.web("#5ba8e0"));
                startResetBtn.setText("⏹ Reset");
                stepNext();
            } else {
                resetAlgorithm();
                startResetBtn.setText("▶ Start / Reset");
            }
            draw();
        });

        stepBtn.setOnAction(e -> {
            if (currentState != State.IDLE && currentState != State.COMPLETE) {
                stepNext();
            }
        });

        playPauseBtn.setOnAction(e -> {
            if (currentState == State.IDLE || currentState == State.COMPLETE) return;
            isPlaying = !isPlaying;
            playPauseBtn.setText(isPlaying ? "⏸ Pause" : "⏯ Play / Pause");
        });

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (isPlaying && currentState != State.IDLE && currentState != State.COMPLETE) {
                    long delay = (long) (1_000_000_000.0 / speedSlider.getValue());
                    if (now - lastStepTime >= delay) {
                        stepNext();
                        lastStepTime = now;
                    }
                }
                draw();
            }
        };
        timer.start();
        
        draw();
    }
    
    private void resetAlgorithm() {
        isPlaying = false;
        playPauseBtn.setText("⏯ Play / Pause");
        hull.clear();
        currentState = State.IDLE;
        activeAnchor = null;
        candidatePoint = null;
        bestNextPoint = null;
        candidateIndex = 0;
        setStatus("READY", "Initialization", "Click within the dashed rectangle on the canvas to add points or generate random ones. Then click Start to wrap them!", Color.web("#5ba8e0"));
        updateMetrics();
    }

    private void stepNext() {
        switch (currentState) {
            case FIND_MIN_X:
                Point leftmost = points.get(0);
                for (Point p : points) {
                    if (p.x < leftmost.x) leftmost = p;
                }
                activeAnchor = leftmost;
                hull.add(activeAnchor);
                
                bestNextPoint = points.get(0) == activeAnchor ? points.get(1) : points.get(0);
                candidateIndex = 0;
                currentState = State.SCANNING;
                setStatus("RUNNING", "Scanning", "Starting to sweep rays to find the next point.", Color.web("#5ba8e0"));
                updateMetrics();
                break;
                
            case SCANNING:
                if (candidateIndex < points.size()) {
                    candidatePoint = points.get(candidateIndex);
                    
                    if (candidatePoint != activeAnchor) {
                        double cross = crossProduct(activeAnchor, bestNextPoint, candidatePoint);
                        if (bestNextPoint == activeAnchor || cross > 0 || (cross == 0 && distanceSq(activeAnchor, candidatePoint) > distanceSq(activeAnchor, bestNextPoint))) {
                            bestNextPoint = candidatePoint;
                        }
                        double candAngle = getAngle(activeAnchor, candidatePoint);
                        double bestAngle = getAngle(activeAnchor, bestNextPoint);
                        setStatus("RUNNING", "Evaluating Candidates", 
                            "Testing Candidate (" + String.format("%.1f°", candAngle) + ") vs Current Best (" + String.format("%.1f°", bestAngle) + ")", 
                            Color.web("#d4a84b"));
                    }
                    candidateIndex++;
                } else {
                    currentState = State.EDGE_COMMIT;
                    setStatus("RUNNING", "Edge Commitment", "Found most clockwise point. Committing edge.", Color.web("#4cbf95"));
                }
                break;
                
            case EDGE_COMMIT:
                if (bestNextPoint == hull.get(0)) {
                    currentState = State.COMPLETE;
                    isPlaying = false;
                    playPauseBtn.setText("⏯ Play / Pause");
                    setStatus("COMPLETE", "Hull Complete", "Wrapped all points! Total vertices: " + hull.size(), Color.web("#9b72d4"));
                } else {
                    hull.add(bestNextPoint);
                    activeAnchor = bestNextPoint;
                    bestNextPoint = activeAnchor;
                    candidateIndex = 0;
                    currentState = State.SCANNING;
                    setStatus("RUNNING", "Edge Added", "Added point to hull. Searching for next edge...", Color.web("#5ba8e0"));
                }
                updateMetrics();
                break;
                
            default:
                break;
        }
        draw();
    }
    
    private void setStatus(String badge, String title, String desc, Color color) {
        statusBadgeLabel.setText(badge);
        statusBadgeLabel.setStyle("-fx-background-color: #22224a; -fx-text-fill: " + toHex(color) + "; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 6;");
        stepTitleLabel.setText(title);
        stepTitleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + toHex(color) + ";");
        stepDescLabel.setText(desc);
    }
    
    private void updateMetrics() {
        totalPointsLabel.setText(String.valueOf(points.size()));
        hullVerticesLabel.setText(String.valueOf(hull.size()));
    }

    private void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#0c0c1e"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // Draw valid plotting area boundary
        gc.setStroke(Color.web("#32325a"));
        gc.setLineWidth(1);
        gc.setLineDashes(5);
        gc.strokeRect(40, 40, canvas.getWidth() - 80, canvas.getHeight() - 80);
        gc.setLineDashes();

        // Draw edges
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(3);
        for (int i = 0; i < hull.size() - 1; i++) {
            Point p1 = hull.get(i);
            Point p2 = hull.get(i+1);
            gc.strokeLine(p1.x, p1.y, p2.x, p2.y);
        }
        
        // Draw completion line
        if (currentState == State.COMPLETE && hull.size() > 2) {
            Point p1 = hull.get(hull.size() - 1);
            Point p2 = hull.get(0);
            gc.strokeLine(p1.x, p1.y, p2.x, p2.y);
        }

        // Scanning phase rays and angles
        if (currentState == State.SCANNING && activeAnchor != null) {
            if (bestNextPoint != null && bestNextPoint != activeAnchor) {
                gc.setStroke(Color.web("#4cbf95"));
                gc.setLineWidth(2);
                gc.setLineDashes();
                gc.strokeLine(activeAnchor.x, activeAnchor.y, bestNextPoint.x, bestNextPoint.y);
                
                gc.setFill(Color.web("#4cbf95"));
                gc.fillText(String.format("%.1f°", getAngle(activeAnchor, bestNextPoint)), bestNextPoint.x + 10, bestNextPoint.y - 10);
            }
            
            if (candidatePoint != null && candidatePoint != activeAnchor) {
                // Sweeping radial vector
                gc.setStroke(Color.web("#ffa500"));
                gc.setLineWidth(1);
                gc.setLineDashes(5);
                gc.strokeLine(activeAnchor.x, activeAnchor.y, candidatePoint.x, candidatePoint.y);
                gc.setLineDashes();
                
                gc.setFill(Color.web("#ffa500"));
                gc.fillText(String.format("%.1f°", getAngle(activeAnchor, candidatePoint)), candidatePoint.x + 10, candidatePoint.y - 10);
            }
        }

        // Draw points
        for (Point p : points) {
            gc.setFill(Color.web("#6868a0"));
            if (hull.contains(p)) {
                gc.setFill(Color.WHITE);
            }
            if (currentState == State.SCANNING) {
                if (p == bestNextPoint && p != activeAnchor) {
                    double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 150.0);
                    gc.setFill(Color.web("#4cbf95", 0.5 + 0.5 * pulse)); // Pulsing green
                } else if (p == candidatePoint && p != activeAnchor) {
                    gc.setFill(Color.web("#ffa500")); // Orange
                }
            }
            gc.fillOval(p.x - 6, p.y - 6, 12, 12);
        }
        
        // Draw active anchor highlight
        if (activeAnchor != null && currentState != State.COMPLETE) {
            gc.setStroke(Color.web("#5ba8e0", 0.6));
            gc.setLineWidth(2);
            gc.strokeOval(activeAnchor.x - 12, activeAnchor.y - 12, 24, 24);
        }
    }
    
    private double crossProduct(Point o, Point a, Point b) {
        return (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x);
    }
    
    private double distanceSq(Point a, Point b) {
        return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y);
    }
    
    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private double getAngle(Point anchor, Point target) {
        double deg = Math.toDegrees(Math.atan2(-(target.y - anchor.y), target.x - anchor.x));
        return deg < 0 ? deg + 360 : deg;
    }

    private static class Point {
        double x, y;
        Point(double x, double y) { this.x = x; this.y = y; }
    }
}
