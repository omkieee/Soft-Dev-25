package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReportController {

    @FXML
    private Button homeBtn;
    private Button IncomeBtn;
    private Button ExpenseBtn;
    private Button ProfitBtn;
    private Button ReportBtn;

    private List<Button> navButtons;

    @FXML
    public void initialize() {
        // Add all nav buttons to a list for easier management
        navButtons = new ArrayList<>();
        navButtons.add(homeBtn);
        navButtons.add(IncomeBtn);
        navButtons.add(ExpenseBtn);
        navButtons.add(ProfitBtn);
        navButtons.add(ReportBtn);
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
    private void onIncomeBtnClick(ActionEvent event) {
        navigate(event, "1Income.fxml", "Mr. Are Level Up Cuisine | Stock Page");
        setActiveButton(IncomeBtn);
    }

    @FXML
    private void onExpenseBtnClick(ActionEvent event) {
        navigate(event, "2Profit.fxml", "Mr. Are Level Up Cuisine | Recipe Page");
        setActiveButton(ExpenseBtn);
    }

    @FXML
    private void onProfitBtnClick(ActionEvent event) {
        navigate(event, "3Income.fxml", "Mr. Are Level Up Cuisine | Dishes Page");
        setActiveButton(ProfitBtn);
    }

    @FXML
    private void onReportBtnClick(ActionEvent event) {
        navigate(event, "4Overview.fxml", "Mr. Are Level Up Cuisine | Add Stock Page");
        setActiveButton(ReportBtn);
    }

}
