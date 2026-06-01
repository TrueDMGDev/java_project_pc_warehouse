package com.pcwarehouse.controller;

import com.pcwarehouse.model.LoginResult;
import com.pcwarehouse.model.UserSession;
import com.pcwarehouse.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.function.Consumer;

public class LoginController {

    private final AuthService authService = new AuthService();
    private Consumer<UserSession> loginSuccessHandler;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label loginStatusLabel;

    public void setLoginSuccessHandler(Consumer<UserSession> loginSuccessHandler) {
        this.loginSuccessHandler = loginSuccessHandler;
    }

    @FXML
    private void initialize() {
        usernameField.setOnAction(event -> passwordField.requestFocus());
        passwordField.setOnAction(event -> handleLogin());
        Platform.runLater(usernameField::requestFocus);
    }

    @FXML
    private void handleLogin() {
        LoginResult result = authService.authenticate(usernameField.getText(), passwordField.getText());
        loginStatusLabel.setText(result.message());

        if (result.success() && loginSuccessHandler != null) {
            loginSuccessHandler.accept(result.session());
            return;
        }

        passwordField.clear();
        passwordField.requestFocus();
    }
}
