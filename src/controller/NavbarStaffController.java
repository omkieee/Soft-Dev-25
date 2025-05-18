package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import view.LoginForm;
import controller.LoginController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NavbarStaffController {

    @FXML
    private Button homeBtn;
    @FXML
    private Button stocksBtn1;
    @FXML
    private Button recipeBtn1;
    @FXML
    private Button dishBtn1;

    @FXML
    private Button logoutBtn;

    private List<Button> navButtons;

    @FXML
    public void initialize() {
        // Add all nav buttons to a list for easier management
        navButtons = new ArrayList<>();
        navButtons.add(homeBtn);
        navButtons.add(stocksBtn1);
        navButtons.add(recipeBtn1);
        navButtons.add(dishBtn1);
        navButtons.add(logoutBtn);
    }

    private void setActiveButton(Button activeButton) {
        for (Button button : navButtons) {
            button.getStyleClass().remove("active");
        }
        if (!activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
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
        setActiveButton(homeBtn);
    }

    @FXML
    private void onStocksBtnClick(ActionEvent event) {
        navigate(event, "StockStaff.fxml", "Mr. Are Level Up Cuisine | Stock Page");
        setActiveButton(stocksBtn1);
    }

    @FXML
    private void onRecipeBtnClick(ActionEvent event) {
        navigate(event, "RecipeStaff.fxml", "Mr. Are Level Up Cuisine | Recipe Page");
        setActiveButton(recipeBtn1);
    }

    @FXML
    private void onDishBtnClick(ActionEvent event) {
        navigate(event, "DishesStaff.fxml", "Mr. Are Level Up Cuisine | Dishes Page");
        setActiveButton(dishBtn1);
    }

    @FXML
    private void onLogoutBtnClick(ActionEvent event) {
        // Run logout in a background thread
        new Thread(() -> {
            LoginController.logLogoutTime();

            // Then return to UI thread to switch scene
            javafx.application.Platform.runLater(() -> {
                navigate(event, "loginForm.fxml", "Mr. Are Level Up Cuisine | Login Page");
                setActiveButton(logoutBtn);
            });
        }).start();
    }
}
