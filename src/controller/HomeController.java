package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import view.LoginForm;
import controller.LoginController;

import java.io.IOException;

public class HomeController {

    @FXML
    private Button inventoryBtn;

    @FXML
    private Button posBtn;

    @FXML
    private void initialize() {
        inventoryBtn.setOnAction(this::handleInventory);
        posBtn.setOnAction(this::handlePos);
    }

    @FXML
    private void onAddNewUserClick(ActionEvent event) {
        String userRole = LoginForm.getUserRole();

        if ("Admin".equals(userRole)) {
            openNewWindow(event, "/view/AddNewUser.fxml", "Add New User");
        } else if ("Staff".equals(userRole)) {
            showAlert(Alert.AlertType.ERROR, "Error", "Access Denied. Admin access only!");
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "User role not recognized.");
        }
    }

    @FXML
    private void onLogoutBtnClick(ActionEvent event) {
        // Run logout in a background thread
        new Thread(() -> {
            LoginController.logLogoutTime();

            // Then return to UI thread to switch scene
            javafx.application.Platform.runLater(() -> {
                openNewWindow(event, "/view/loginForm.fxml", "Mr. Are Level Up Cuisine | Login Page");
            });
        }).start();
    }

    @FXML
    private void handleInventory(ActionEvent event) {
        String userRole = LoginForm.getUserRole();
        String fxmlFile;
        String title = "Mr. Are Level Up Cuisine | Stock Page";

        if ("Admin".equals(userRole)) {
            fxmlFile = "/view/Stock.fxml";
        } else if ("Staff".equals(userRole)) {
            fxmlFile = "/view/StockStaff.fxml";
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "User role not recognized.");
            return;
        }

        openNewWindow(event, fxmlFile, title);
    }

    @FXML
    private void handlePos(ActionEvent event) {
        String userRole = LoginForm.getUserRole();
        String fxmlFile;
        String title = "Mr. Are Level Up Cuisine | POS Page";

        if ("Admin".equals(userRole)) {
            fxmlFile = "/view/Pos.fxml";
        } else if ("Staff".equals(userRole)) {
            fxmlFile = "/view/Pos.fxml";
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "User role not recognized.");
            return;
        }

        openNewWindow(event, fxmlFile, title);
    }

    @FXML
    private void handleSales(ActionEvent event) {
        String userRole = LoginForm.getUserRole();
        String fxmlFile;
        String title = "Mr. Are Level Up Cuisine | Stock Page";

        if ("Admin".equals(userRole)) {
            fxmlFile = "/view/4Overview.fxml";
        } else if ("Staff".equals(userRole)) {
            fxmlFile = "/view/4Overview.fxml";
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "User role not recognized.");
            return;
        }

        openNewWindow(event, fxmlFile, title);
    }

    private void openNewWindow(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.setTitle(title);

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load the page: " + fxmlPath);
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
