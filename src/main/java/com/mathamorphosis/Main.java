package com.mathamorphosis;

import com.mathamorphosis.ui.DashboardController;
import com.mathamorphosis.ui.ModuleLayoutController;
import com.mathamorphosis.ui.StartupScreenController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    private Stage primaryStage;
    private Scene mainScene;
    private StackPane rootNode;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Math-A-Morphosis");
        this.primaryStage.setMaximized(true);

        rootNode = new StackPane();
        mainScene = new Scene(rootNode, 1280, 720);

        // Load CSS
        String css = getClass().getResource("/styles/theme.css").toExternalForm();
        mainScene.getStylesheets().add(css);

        // F11 toggles true fullscreen (covers taskbar and title bar)
        mainScene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F11) {
                this.primaryStage.setFullScreen(!this.primaryStage.isFullScreen());
            }
        });

        // Hide the default "Press ESC to exit fullscreen" overlay text
        this.primaryStage.setFullScreenExitHint("");

        showStartupScreen();

        this.primaryStage.setScene(mainScene);
        this.primaryStage.show();
    }

    private void showStartupScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/startup_screen.fxml"));
            Node view = loader.load();
            StartupScreenController controller = loader.getController();
            controller.setOnStart(this::showDashboard);
            rootNode.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Node view = loader.load();
            DashboardController controller = loader.getController();
            controller.setOnModuleSelect(this::loadModule);
            rootNode.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadModule(String moduleId) {
        try {
            FXMLLoader layoutLoader = new FXMLLoader(getClass().getResource("/fxml/module_layout.fxml"));
            Node moduleLayout = layoutLoader.load();
            ModuleLayoutController layoutController = layoutLoader.getController();
            layoutController.setOnBack(this::showDashboard);

            String fxmlFile = "";
            switch (moduleId) {
                case "NUMBER_THEORY": fxmlFile = "/fxml/sieve_view.fxml"; break;
                case "CALCULUS": fxmlFile = "/fxml/riemann_view.fxml"; break;
                case "LINEAR_ALGEBRA": fxmlFile = "/fxml/vector_view.fxml"; break;
                case "LEAST_SQUARES": fxmlFile = "/fxml/least_squares_view.fxml"; break;
                case "UNIT_CIRCLE": fxmlFile = "/fxml/unit_circle_view.fxml"; break;
                case "GRAPHING_CALC": fxmlFile = "/fxml/graphing_calculator_view.fxml"; break;
                case "FOURIER_SERIES": fxmlFile = "/fxml/fourier_series_view.fxml"; break;
                case "CHAOS_GAME": fxmlFile = "/fxml/chaos_game_view.fxml"; break;
                case "CONVEX_HULL": fxmlFile = "/fxml/convex_hull_view.fxml"; break;
                case "MATRIX_TRANSFORM": fxmlFile = "/fxml/matrix_transform_view.fxml"; break;
            }

            if (!fxmlFile.isEmpty()) {
                FXMLLoader viewLoader = new FXMLLoader(getClass().getResource(fxmlFile));
                Node view = viewLoader.load();
                layoutController.setContent(view);
            }

            rootNode.getChildren().setAll(moduleLayout);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
