package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.stage.Stage;

import javax.swing.JOptionPane;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AddNewStockController {

    @FXML
    private ComboBox<String> nameField;

    @FXML
    private ComboBox<String> categoryField;

    @FXML
    private TextField quantityField;

    @FXML
    private ComboBox<String> unitField;

    @FXML
    private Button btnSaveStock;

    private boolean existingStock = false;  // flag to determine update vs insert

    @FXML
    private void initialize() {
        nameField.setEditable(true);
        loadStockNames();
        loadCategories();
        loadUnits();
        setupAutoLoadDetails();

        // Add all nav buttons to a list for easier management
        navButtons = new ArrayList<>();
        navButtons.add(homeBtn);
        navButtons.add(stocksBtn1);
        navButtons.add(recipeBtn1);
        navButtons.add(dishBtn1);
        navButtons.add(addNewStockBtn1);
        navButtons.add(addNewRecipeBtn1);
        navButtons.add(addNewDishBtn1);
        navButtons.add(logoutBtn);
    }

    private void loadStockNames() {
        String query = "SELECT DISTINCT IngredientsName FROM Ingredients";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            nameField.getItems().clear();
            while (rs.next()) {
                nameField.getItems().add(rs.getString("IngredientsName"));
            }
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    private void loadCategories() {
        String query = "SELECT DISTINCT Category FROM Ingredients";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            categoryField.getItems().clear();
            while (rs.next()) {
                categoryField.getItems().add(rs.getString("Category"));
            }
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    private void loadUnits() {
        String query = "SELECT DISTINCT UnitType FROM Ingredients";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            unitField.getItems().clear();
            while (rs.next()) {
                unitField.getItems().add(rs.getString("UnitType"));
            }
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    private void setupAutoLoadDetails() {
        nameField.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.trim().isEmpty()) {
                checkAndLoadStock(newVal.trim());
            }
        });

        nameField.setOnAction(event -> {
            String selected = nameField.getValue();
            if (selected != null && !selected.trim().isEmpty()) {
                checkAndLoadStock(selected.trim());
            }
        });
    }

    private void checkAndLoadStock(String stockName) {
        String query = "SELECT * FROM Ingredients WHERE IngredientsName = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, stockName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Stock exists — Load and mark as update
                existingStock = true;
                categoryField.setValue(rs.getString("Category"));
                quantityField.setText(String.valueOf(rs.getInt("Quantity")));
                unitField.setValue(rs.getString("UnitType"));
            } else {
                // Stock does not exist — Prepare for insert
                existingStock = false;
                categoryField.setValue(null);
                quantityField.clear();
                unitField.setValue(null);
            }
        } catch (SQLException e) {
            showError("Database error while checking stock: " + e.getMessage());
        }
    }

    @FXML
    private void saveStock(ActionEvent event) {
        String name = nameField.getEditor().getText().trim();
        String category = categoryField.getValue();
        String quantity = quantityField.getText();
        String unitType = unitField.getValue();

        if (name.isEmpty() || category == null || quantity.isEmpty() || unitType == null) {
            showError("Please fill in all fields.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (existingStock) {
                // UPDATE
                String updateQuery = "UPDATE Ingredients SET Category = ?, Quantity = ?, UnitType = ? WHERE IngredientsName = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
                    pstmt.setString(1, category);
                    pstmt.setInt(2, Integer.parseInt(quantity));
                    pstmt.setString(3, unitType);
                    pstmt.setString(4, name);
                    pstmt.executeUpdate();
                    showSuccess("Stock updated successfully!");
                }
            } else {
                // INSERT
                String insertQuery = "INSERT INTO Ingredients (IngredientsName, Category, Quantity, UnitType) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                    pstmt.setString(1, name);
                    pstmt.setString(2, category);
                    pstmt.setInt(3, Integer.parseInt(quantity));
                    pstmt.setString(4, unitType);
                    pstmt.executeUpdate();
                    showSuccess("New stock added successfully!");
                    nameField.getItems().add(name); // add to combo box
                }
            }
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        } catch (NumberFormatException e) {
            showError("Quantity must be a valid number.");
        }
    }

    @FXML
    private void clearFields(ActionEvent event) {
        nameField.setValue(null);
        nameField.getEditor().clear();
        categoryField.setValue(null);
        quantityField.clear();
        unitField.setValue(null);
        existingStock = false;
    }

    @FXML
    private void deleteStock(ActionEvent event) {
        String stockName = nameField.getEditor().getText().trim();
        if (stockName.isEmpty()) {
            showError("Please enter or select a stock name to delete.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {

            // Check dependencies in recipes and transactions
            String checkRecipe = "SELECT COUNT(*) FROM RECIPES WHERE IngredientsName = ?";
            String checkTransaction = "SELECT COUNT(*) FROM TRANSACTIONS WHERE IngredientsName = ?";
            int recipeCount = 0, transactionCount = 0;

            try (PreparedStatement recipeStmt = conn.prepareStatement(checkRecipe)) {
                recipeStmt.setString(1, stockName);
                ResultSet rs = recipeStmt.executeQuery();
                if (rs.next()) recipeCount = rs.getInt(1);
            }

            try (PreparedStatement txnStmt = conn.prepareStatement(checkTransaction)) {
                txnStmt.setString(1, stockName);
                ResultSet rs = txnStmt.executeQuery();
                if (rs.next()) transactionCount = rs.getInt(1);
            }

            if (recipeCount > 0 || transactionCount > 0) {
                int confirm = JOptionPane.showConfirmDialog(null,
                        "This ingredient is used in:\n"
                                + (recipeCount > 0 ? "- Recipes\n" : "")
                                + (transactionCount > 0 ? "- Transactions\n" : "")
                                + "\nDo you want to delete ALL traces of this ingredient?",
                        "Confirm Deletion", JOptionPane.YES_NO_OPTION);

                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }

                // Delete from transactions_ingredients
                if (transactionCount > 0) {
                    String deleteTxn = "DELETE FROM TRANSACTIONS WHERE IngredientsName = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(deleteTxn)) {
                        stmt.setString(1, stockName);
                        stmt.executeUpdate();
                    }
                }

                // Delete from recipes_ingredients
                if (recipeCount > 0) {
                    String deleteRecipe = "DELETE FROM RECIPES WHERE IngredientsName = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(deleteRecipe)) {
                        stmt.setString(1, stockName);
                        stmt.executeUpdate();
                    }
                }
            }

            // Delete from Ingredients
            String deleteIngredient = "DELETE FROM Ingredients WHERE IngredientsName = ?";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteIngredient)) {
                deleteStmt.setString(1, stockName);
                int rowsAffected = deleteStmt.executeUpdate();

                if (rowsAffected > 0) {
                    showSuccess("Ingredient \"" + stockName + "\" and all references deleted.");
                    clearFields(null);
                } else {
                    showError("Ingredient not found or already deleted.");
                }
            }

        } catch (SQLException e) {
            showError("Database error during deletion: " + e.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(null, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    @FXML
    private Button homeBtn;
    @FXML
    private Button stocksBtn1;
    @FXML
    private Button recipeBtn1;
    @FXML
    private Button dishBtn1;
    @FXML
    private Button addNewStockBtn1;
    @FXML
    private Button addNewRecipeBtn1;
    @FXML
    private Button addNewDishBtn1;
    @FXML
    private Button logoutBtn;

    private List<Button> navButtons;


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
        navigate(event, "Stock.fxml", "Mr. Are Level Up Cuisine | Stock Page");
        setActiveButton(stocksBtn1);
    }

    @FXML
    private void onRecipeBtnClick(ActionEvent event) {
        navigate(event, "Recipe.fxml", "Mr. Are Level Up Cuisine | Recipe Page");
        setActiveButton(recipeBtn1);
    }

    @FXML
    private void onDishBtnClick(ActionEvent event) {
        navigate(event, "Dishes.fxml", "Mr. Are Level Up Cuisine | Dishes Page");
        setActiveButton(dishBtn1);
    }

    @FXML
    private void onAddNewStockBtnClick(ActionEvent event) {
        navigate(event, "AddNewStock.fxml", "Mr. Are Level Up Cuisine | Add Stock Page");
        setActiveButton(addNewStockBtn1);
    }

    @FXML
    private void onAddNewRecipeBtnClick(ActionEvent event) {
        navigate(event, "AddNewRecipe.fxml", "Mr. Are Level Up Cuisine | Add Recipe Page");
        setActiveButton(addNewRecipeBtn1);
    }

    @FXML
    private void onAddNewDishBtnClick(ActionEvent event) {
        navigate(event, "AddNewDish.fxml", "Mr. Are Level Up Cuisine | Add Dish Page");
        setActiveButton(addNewDishBtn1);
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
