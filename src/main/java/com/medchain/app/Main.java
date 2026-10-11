package com.medchain.app;

import com.medchain.util.DBConnection;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
//import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main — JavaFX Application Entry Point
 *
 * The starting point of the entire MedChain application.
 * Initializes JavaFX, loads the Login screen, and handles
 * application shutdown cleanly.
 *
 * @author  MedChain Team
 * @version 1.0
 */
public class Main extends Application {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    // ── Screen Dimensions ─────────────────────────────────────────────────────
    public static final double LOGIN_WIDTH     = 900;
    public static final double LOGIN_HEIGHT    = 600;
    public static final double DASHBOARD_WIDTH  = 1100;
    public static final double DASHBOARD_HEIGHT = 720;

    // ── Primary Stage (shared across app) ─────────────────────────────────────
    private static Stage primaryStage;

    /**
     * JavaFX start method — called automatically when app launches.
     * Loads the Login screen as the first view.
     *
     * @param  stage  the primary window provided by JavaFX
     */
    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        // ── Rebuild blockchain from database on startup ───────────────────────
        try {
            com.medchain.blockchain.Blockchain.getInstance()
                .rebuildFromDatabase();
            LOGGER.info("✅ Blockchain restored from database.");
        } catch (Exception e) {
            LOGGER.warning("⚠️ Could not rebuild blockchain: " + e.getMessage());
        }

        // ── App window settings ───────────────────────────────────────────────
        primaryStage.setTitle("MedChain — Blockchain Medical Records");
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(500);

        // ── Handle window close ───────────────────────────────────────────────
        primaryStage.setOnCloseRequest(_ -> {
            LOGGER.info("Application closing — cleaning up resources.");
            DBConnection.closeConnection();
            Platform.exit();
            System.exit(0);
        });

        // ── Load Login screen ─────────────────────────────────────────────────
        loadScene("/fxml/Login.fxml", LOGIN_WIDTH, LOGIN_HEIGHT);
        primaryStage.show();
        LOGGER.info("✅ MedChain application started successfully.");
    }

    // ── Scene Navigation ──────────────────────────────────────────────────────

    /**
     * Loads and displays a new FXML scene on the primary stage.
     * Called by controllers to navigate between screens.
     *
     * @param  fxmlPath  path to FXML file (e.g. "/fxml/Login.fxml")
     * @param  width     window width
     * @param  height    window height
     */
    public static void loadScene(String fxmlPath,
                                  double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(
                Main.class.getResource(fxmlPath)
            );
            Scene scene = new Scene(loader.load(), width, height);

            // Load global CSS stylesheet
            scene.getStylesheets().add(
                Main.class.getResource("/css/styles.css").toExternalForm()
            );

            primaryStage.setScene(scene);
            primaryStage.setWidth(width);
            primaryStage.setHeight(height);
            primaryStage.centerOnScreen();

            LOGGER.info("✅ Scene loaded: " + fxmlPath);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,
                "❌ Failed to load scene: " + fxmlPath, e);
            throw new RuntimeException(
                "Failed to load scene: " + fxmlPath, e);
        }
    }

    /**
     * Returns the primary stage — used by controllers
     * to set window title after login.
     *
     * @return  primary Stage
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Application entry point.
     *
     * @param  args  command line arguments (not used)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
