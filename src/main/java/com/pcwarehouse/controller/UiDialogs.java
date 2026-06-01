package com.pcwarehouse.controller;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

final class UiDialogs {

    private UiDialogs() {
    }

    static boolean confirmForceDelete(String title, String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        return alert.showAndWait()
                .filter(ButtonType.OK::equals)
                .isPresent();
    }
}
