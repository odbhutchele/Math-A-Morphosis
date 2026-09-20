package com.mathamorphosis.ui;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StartupScreenController {

    @FXML
    private StackPane rootPane;

    private Canvas canvas;
    private GraphicsContext gc;
    private AnimationTimer timer;
    private List<Particle> particles = new ArrayList<>();
    private final int NUM_PARTICLES = 75;
    private boolean initializedParticles = false;
    private double lastW = 0, lastH = 0;
    
    private final String[] SYMBOLS = {"∫", "Σ", "π", "∞", "√", "θ", "Δ", "λ", "φ", "∂"};
    private final Color[] PALETTE = {
        Color.web("#4cbf95"), // Emerald/Sage Green
        Color.web("#5ba8e0"), // Sky Blue
        Color.web("#d4a84b"), // Warm Gold/Amber
        Color.web("#9b72d4"), // Violet/Purple
        Color.web("#d46b6b"), // Terracotta/Rose
        Color.web("#4ab8c4")  // Teal
    };
    private Random random = new Random();
    private Runnable onStart;

    @FXML
    public void initialize() {
        // 1. Background Animation Canvas bound to container size
        canvas = new Canvas();
        canvas.widthProperty().bind(rootPane.widthProperty());
        canvas.heightProperty().bind(rootPane.heightProperty());
        gc = canvas.getGraphicsContext2D();

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double w = canvas.getWidth();
                double h = canvas.getHeight();
                if (w > 0 && h > 0) {
                    draw(w, h);
                }
            }
        };
        timer.start();

        // 2. Foreground UI
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setMouseTransparent(true); // Let clicks pass through to StackPane

        Label title = new Label("Math-A-Morphosis");
        title.getStyleClass().add("header-text");
        title.setStyle("-fx-font-size: 56px;");

        Label prompt = new Label("Click to start visualizing");
        prompt.getStyleClass().add("subheader-text");
        prompt.setStyle("-fx-font-size: 22px; -fx-text-fill: #5ba8e0;");

        // Pulse Animation for the prompt
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(prompt.opacityProperty(), 1.0)),
                new KeyFrame(Duration.seconds(1), new KeyValue(prompt.opacityProperty(), 0.3)),
                new KeyFrame(Duration.seconds(2), new KeyValue(prompt.opacityProperty(), 1.0))
        );
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();

        contentBox.getChildren().addAll(title, prompt);

        rootPane.getChildren().addAll(canvas, contentBox);

        // 3. Transition interaction
        rootPane.setOnMouseClicked(e -> {
            // Disable clicks immediately
            rootPane.setDisable(true);
            timer.stop();
            
            // Fade out the entire StackPane
            FadeTransition ft = new FadeTransition(Duration.millis(800), rootPane);
            ft.setFromValue(1.0);
            ft.setToValue(0.0);
            ft.setOnFinished(evt -> {
                if (onStart != null) onStart.run();
            });
            ft.play();
        });
    }

    public void setOnStart(Runnable onStart) {
        this.onStart = onStart;
    }

    private void initParticles(double w, double h) {
        particles = new ArrayList<>();
        double cx = w / 2.0;
        double cy = h / 2.0;
        double maxR = Math.hypot(cx, cy) * 1.05;

        for (int i = 0; i < NUM_PARTICLES; i++) {
            // Distribute evenly across all angles (360 degrees) and full radial distance
            double angle = (2.0 * Math.PI * i) / NUM_PARTICLES + (random.nextDouble() - 0.5) * (2.0 * Math.PI / NUM_PARTICLES);
            // Sqrt of random gives uniform spatial 2D disc area distribution across all quadrants
            double r = Math.sqrt(random.nextDouble()) * maxR;
            double px = cx + r * Math.cos(angle);
            double py = cy + r * Math.sin(angle);

            // Clamp softly inside screen boundaries
            px = Math.max(15, Math.min(w - 15, px));
            py = Math.max(15, Math.min(h - 15, py));

            particles.add(new Particle(px, py, cx, cy));
        }
    }

    private void draw(double width, double height) {
        if (!initializedParticles || Math.abs(width - lastW) > 100 || Math.abs(height - lastH) > 100) {
            initParticles(width, height);
            lastW = width;
            lastH = height;
            initializedParticles = true;
        }

        gc.setFill(Color.web("#0c0c1e"));
        gc.fillRect(0, 0, width, height);

        // Update particles
        for (Particle p : particles) {
            p.update(width, height);
        }

        // Draw connections
        gc.setLineWidth(1);
        for (int i = 0; i < particles.size(); i++) {
            for (int j = i + 1; j < particles.size(); j++) {
                Particle p1 = particles.get(i);
                Particle p2 = particles.get(j);
                double dx = p1.x - p2.x;
                double dy = p1.y - p2.y;
                double dist = Math.sqrt(dx * dx + dy * dy);

                if (dist < 155) {
                    double alpha = (1.0 - (dist / 155.0)) * 0.42;
                    gc.setStroke(new Color(p1.color.getRed(), p1.color.getGreen(), p1.color.getBlue(), alpha));
                    gc.strokeLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }

        // Draw particles
        for (Particle p : particles) {
            if (p.symbol != null) {
                gc.setFill(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), 0.88));
                gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
                gc.fillText(p.symbol, p.x - 11, p.y + 8);
            } else {
                gc.setFill(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), 0.8));
                gc.fillOval(p.x - p.radius, p.y - p.radius, p.radius * 2, p.radius * 2);
            }
        }
    }

    private class Particle {
        double x, y;
        double vx, vy;
        double radius;
        String symbol;
        Color color;

        Particle(double x, double y, double cx, double cy) {
            reset(x, y, cx, cy);
        }

        void reset(double x, double y, double cx, double cy) {
            this.x = x;
            this.y = y;

            double dx = x - cx;
            double dy = y - cy;
            double angle = Math.atan2(dy, dx);
            if (Math.abs(dx) < 1e-4 && Math.abs(dy) < 1e-4) {
                angle = random.nextDouble() * 2 * Math.PI;
            }

            // Radial outward velocity spreading outward from the central zone
            double speed = 0.45 + random.nextDouble() * 0.65;
            double perpDrift = (random.nextDouble() - 0.5) * 0.25;
            this.vx = Math.cos(angle) * speed - Math.sin(angle) * perpDrift;
            this.vy = Math.sin(angle) * speed + Math.cos(angle) * perpDrift;

            this.radius = random.nextDouble() * 2.5 + 1.5;
            this.color = PALETTE[random.nextInt(PALETTE.length)];

            if (random.nextDouble() < 0.22) {
                this.symbol = SYMBOLS[random.nextInt(SYMBOLS.length)];
            } else {
                this.symbol = null;
            }
        }

        void update(double w, double h) {
            x += vx;
            y += vy;

            double cx = w / 2.0;
            double cy = h / 2.0;

            // When a particle flows past the screen border, respawn from the central zone
            double margin = 40;
            if (x < -margin || x > w + margin || y < -margin || y > h + margin) {
                double spawnAngle = random.nextDouble() * 2 * Math.PI;
                double spawnR = 25 + random.nextDouble() * 110;
                double sx = cx + spawnR * Math.cos(spawnAngle);
                double sy = cy + spawnR * Math.sin(spawnAngle);
                reset(sx, sy, cx, cy);
            }
        }
    }
}
