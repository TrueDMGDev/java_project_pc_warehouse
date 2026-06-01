package com.pcwarehouse;

import com.pcwarehouse.controller.LoginController;
import com.pcwarehouse.controller.MainController;
import com.pcwarehouse.db.DatabaseMigrator;
import com.pcwarehouse.model.UserSession;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;

public class Main extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;
        DatabaseMigrator.migrate();
        stage.setTitle("PC Warehouse");
        stage.setMinWidth(960);
        stage.setMinHeight(720);
        showLogin();
        stage.show();
    }

    public void showLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/pcwarehouse/view/login-view.fxml"));
        Parent root = loader.load();

        LoginController controller = loader.getController();
        controller.setLoginSuccessHandler(this::showDashboardUnchecked);

        Scene scene = new Scene(root, 1040, 760);
        scene.getStylesheets().add(Main.class.getResource("/com/pcwarehouse/style/app.css").toExternalForm());
        primaryStage.setScene(scene);
    }

    public void showDashboard(UserSession session) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/pcwarehouse/view/main-view.fxml"));
        Parent root = loader.load();

        MainController controller = loader.getController();
        controller.setSession(session);
        controller.setLogoutHandler(this::showLoginUnchecked);

        Scene scene = new Scene(root, 1360, 860);
        scene.getStylesheets().add(Main.class.getResource("/com/pcwarehouse/style/app.css").toExternalForm());
        primaryStage.setScene(scene);
    }

    private void showLoginUnchecked() {
        try {
            showLogin();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void showDashboardUnchecked(UserSession session) {
        try {
            showDashboard(session);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
