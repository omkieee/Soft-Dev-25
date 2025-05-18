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

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.swing.JOptionPane;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AddNewDishController {

    @FXML
    private ComboBox<String> nameField;

    @FXML
    private ComboBox<String> categoryCombo;

    @FXML
    private TextField sizeField;

    @FXML
    private ComboBox<String> unitTypeField;

    @FXML
    private TextField priceField;

    @FXML
    private TextField servingsField;

    @FXML
    private Button btnSaveDish;

    @FXML
    private void initialize() {
        loadCategories();
        loadName();
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

        nameField.setOnAction(event -> loadDishDetails());
    }

    private void loadDishDetails() {
        String selectedName = nameField.getValue();
        if (selectedName == null || selectedName.isEmpty()) return;

        String query = "SELECT * FROM dish WHERE ProductName = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, selectedName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                categoryCombo.setValue(rs.getString("Category"));
                sizeField.setText(rs.getString("Size"));
                priceField.setText(String.valueOf(rs.getDouble("Price")));
                servingsField.setText(String.valueOf(rs.getInt("Servings")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to load dish details: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @FXML
    private void deleteDish(ActionEvent event) {
        String selectedName = nameField.getValue();

        if (selectedName == null || selectedName.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Please select a dish to delete.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(null,
                "Are you sure you want to delete this dish?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        String query = "DELETE FROM dish WHERE ProductName = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, selectedName);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(null, "Dish deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);

                // Reload dish names
                loadName();

                // Auto-select and reload the first available dish, if any
                if (!nameField.getItems().isEmpty()) {
                    nameField.getSelectionModel().selectFirst();
                    loadDishDetails(); // Refresh the form with the new selection
                } else {
                    clearFields(); // No more dishes, clear the form
                }

            } else {
                JOptionPane.showMessageDialog(null, "Dish not found or could not be deleted.", "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void loadCategories() {
        String query = "SELECT DISTINCT Category FROM dish";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            categoryCombo.getItems().clear();
            while (rs.next()) {
                categoryCombo.getItems().add(rs.getString("Category"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadName() {
        String query = "SELECT DISTINCT ProductName FROM dish";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            nameField.getItems().clear();
            while (rs.next()) {
                nameField.getItems().add(rs.getString("ProductName"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @FXML
    private void clearFields() {
        nameField.getSelectionModel().clearSelection();
        categoryCombo.getSelectionModel().clearSelection();
        sizeField.clear();
        unitTypeField.getSelectionModel().clearSelection();
        priceField.clear();
        servingsField.clear();
    }


    @FXML
    private void saveDish(ActionEvent event) {
        String name = nameField.getValue();
        String category = categoryCombo.getValue();
        String size = sizeField.getText();
        String price = priceField.getText();
        String servings = servingsField.getText();

        if (name.isEmpty() || category == null || size.isEmpty() || price.isEmpty() || servings.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Please fill in all fields.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {

            // Check if dish already exists
            String checkQuery = "SELECT COUNT(*) FROM dish WHERE ProductName = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setString(1, name);
                ResultSet rs = checkStmt.executeQuery();
                rs.next();
                int count = rs.getInt(1);

                String query;
                if (count > 0) {
                    // Show confirmation dialog before update
                    int confirm = JOptionPane.showConfirmDialog(null,
                            "Dish already exists. Do you want to update the existing dish?",
                            "Confirm Update",
                            JOptionPane.YES_NO_OPTION);

                    if (confirm != JOptionPane.YES_OPTION) {
                        JOptionPane.showMessageDialog(null, "Update cancelled.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    query = "UPDATE dish SET Category = ?, Size = ?, Price = ?, Servings = ? WHERE ProductName = ?";
                } else {
                    query = "INSERT INTO dish (Category, Size, Price, Servings, ProductName) VALUES (?, ?, ?, ?, ?)";
                }

                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, category);
                    pstmt.setString(2, size);

                    double parsedPrice = Double.parseDouble(price);
                    if (parsedPrice <= 0) {
                        JOptionPane.showMessageDialog(null, "Price must be greater than 0.", "Input Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    pstmt.setDouble(3, parsedPrice);
                    pstmt.setInt(4, Integer.parseInt(servings));
                    pstmt.setString(5, name); // ProductName (WHERE or VALUES)

                    pstmt.executeUpdate();

                    String message = (count > 0) ? "Dish updated successfully!" : "Dish added successfully!";
                    JOptionPane.showMessageDialog(null, message, "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid number format in Quantity, Price, or Servings.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
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
