package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import controller.LoginController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NavbarSalesController {

    @FXML
    private Button homeBtn;
    @FXML
    private Button IncomeBtn;
    @FXML
    private Button ExpenseBtn;
    @FXML
    private Button ProfitBtn;

    private List<Button> navButtons;

    @FXML
    public void initialize() {
        // Add all nav buttons to a list for easier management
        navButtons = new ArrayList<>();
        navButtons.add(homeBtn);
        navButtons.add(IncomeBtn);
        navButtons.add(ExpenseBtn);
        navButtons.add(ProfitBtn);
    }


    private void navigate(ActionEvent event, String fxmlFile, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/" + fxmlFile)); // Update the path!
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onHomeBtnClick(ActionEvent event) {
        navigate(event, "homeFrame.fxml", "Mr. Are Level Up Cuisine | Home Page");
    }

    @FXML
    private void onIncomeBtnClick(ActionEvent event) {
        navigate(event, "3Income.fxml", "Mr. Are Level Up Cuisine | Stock Page");
    }

    @FXML
    private void onExpenseBtnClick(ActionEvent event) {
        navigate(event, "4Overview.fxml", "Mr. Are Level Up Cuisine | Recipe Page");
    }

    @FXML
    private void onProfitBtnClick(ActionEvent event) {
        navigate(event, "2Profit.fxml", "Mr. Are Level Up Cuisine | Dishes Page");
    }

    @FXML
    private void onLogoutBtnClick(ActionEvent event) {
        // Run logout in a background thread
        new Thread(() -> {
            LoginController.logLogoutTime();

            // Then return to UI thread to switch scene
            javafx.application.Platform.runLater(() -> {
                navigate(event, "loginForm.fxml", "Mr. Are Level Up Cuisine | Login Page");
            });
        }).start();
    }
}
