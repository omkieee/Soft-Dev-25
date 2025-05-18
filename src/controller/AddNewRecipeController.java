package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import view.RecipeIngredients;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AddNewRecipeController {

    @FXML private ComboBox<String> dishField;
    @FXML private ComboBox<String> sizeField;
    @FXML private ComboBox<String> ingredientsField;
    @FXML private TextField unitTypeField;
    @FXML private TextField quantityField;
    @FXML private Label dishLabel;
    @FXML private Label sizeName;
    @FXML private TableView<RecipeIngredients> recipeTable;
    @FXML private TableColumn<RecipeIngredients, String> colIngredient;
    @FXML private TableColumn<RecipeIngredients, String> colQuantity;
    @FXML private TableColumn<RecipeIngredients, String> colUnit;

    private final ObservableList<RecipeIngredients> recipeList = FXCollections.observableArrayList();

    public void initialize() {
        loadDishNames();
        loadIngredients();

        colIngredient.setCellValueFactory(new PropertyValueFactory<>("ingredient"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unitType"));

        recipeTable.setItems(recipeList);

        // Handle dish selection
        dishField.setOnAction(event -> {
            String selectedDish = dishField.getValue();
            if (selectedDish != null) {
                dishLabel.setText(selectedDish); // Update dish label
                loadSizeForDish(); // Load sizes for the selected dish
            }
            loadRecipe(); // Try loading the recipe (even if size isn't selected yet)
        });

        // Handle size selection
        sizeField.setOnAction(event -> {
            String selectedSize = sizeField.getValue();
            if (selectedSize != null) {
                sizeName.setText(selectedSize); // Update size label
            }
            loadRecipe(); // Load recipe when both dish and size are selected
        });

        // Handle ingredients selection
        ingredientsField.setOnAction(event -> {
            String selectedIngredient = ingredientsField.getValue();
            if (selectedIngredient != null) {
                loadUnitType(selectedIngredient);
            }
        });

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

    private void loadDishNames() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT ProductName FROM Dish")) {

            ObservableList<String> dishNames = FXCollections.observableArrayList();
            while (rs.next()) {
                dishNames.add(rs.getString("ProductName"));
            }
            dishField.setItems(dishNames);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadSizeForDish() {
        String selectedDish = dishField.getValue();
        if (selectedDish == null) return;

        String query = "SELECT DISTINCT Size FROM Dish WHERE ProductName = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, selectedDish);
            ResultSet rs = stmt.executeQuery();

            ObservableList<String> sizeOptions = FXCollections.observableArrayList();
            while (rs.next()) {
                sizeOptions.add(rs.getString("Size"));
            }
            sizeField.setItems(sizeOptions);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadIngredients() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT IngredientsName FROM Ingredients")) {

            ObservableList<String> ingredients = FXCollections.observableArrayList();
            while (rs.next()) {
                ingredients.add(rs.getString("IngredientsName"));
            }
            ingredientsField.setItems(ingredients);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadUnitType(String ingredientName) {
        String query = "SELECT UnitType FROM Ingredients WHERE IngredientsName = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, ingredientName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                unitTypeField.setText(rs.getString("UnitType"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSaveRecipe() {
        String dish = dishField.getValue();
        String size = sizeField.getValue();
        String ingredient = ingredientsField.getValue();
        String quantityStr = quantityField.getText();
        String unit = unitTypeField.getText();

        if (dish == null || size == null || ingredient == null || quantityStr.isEmpty() || unit == null || unit.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "All fields including dish and size must be filled!");
            alert.show();
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            double newQuantity = Double.parseDouble(quantityStr);

            // Check if the ingredient already exists for this dish and size
            String checkQuery = "SELECT COUNT(*) FROM Recipes WHERE ProductName = ? AND Size = ? AND IngredientsName = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, dish);
            checkStmt.setString(2, size);
            checkStmt.setString(3, ingredient);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                // Ingredient exists, update quantity and unit
                String updateQuery = "UPDATE Recipes SET Quantity = ?, UnitType = ? WHERE ProductName = ? AND Size = ? AND IngredientsName = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                updateStmt.setString(1, String.valueOf(newQuantity));
                updateStmt.setString(2, unit);
                updateStmt.setString(3, dish);
                updateStmt.setString(4, size);
                updateStmt.setString(5, ingredient);
                updateStmt.executeUpdate();
            } else {
                // Ingredient doesn't exist, insert new row
                String insertQuery = "INSERT INTO Recipes (ProductName, Size, IngredientsName, Quantity, UnitType) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
                insertStmt.setString(1, dish);
                insertStmt.setString(2, size);
                insertStmt.setString(3, ingredient);
                insertStmt.setString(4, String.valueOf(newQuantity));
                insertStmt.setString(5, unit);
                insertStmt.executeUpdate();
            }

            loadRecipe(); // Refresh the table

        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save the recipe.");
            alert.show();
        }
    }

    @FXML
    private void loadRecipe() {
        String dish = dishField.getValue();
        String size = sizeField.getValue();

        if (dish == null || size == null) {
            dishLabel.setText("Please select both dish and size.");
            recipeList.clear();
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String recipeQuery = "SELECT IngredientsName, Quantity, UnitType FROM Recipes WHERE ProductName = ? AND Size = ?";
            PreparedStatement stmt = conn.prepareStatement(recipeQuery);
            stmt.setString(1, dish);
            stmt.setString(2, size);
            ResultSet rs = stmt.executeQuery();

            recipeList.clear();
            while (rs.next()) {
                recipeList.add(new RecipeIngredients(
                        rs.getString("IngredientsName"),
                        rs.getString("Quantity"),
                        rs.getString("UnitType")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void clearRecipe() {
        String dish = dishField.getValue();
        String size = sizeField.getValue();

        if (dish == null || size == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Please select both dish and size.");
            alert.show();
            return;
        }

        String deleteQuery = "DELETE FROM Recipes WHERE ProductName = ? AND Size = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {

            stmt.setString(1, dish);
            stmt.setString(2, size);
            stmt.executeUpdate();
            recipeList.clear();
            dishLabel.setText("Recipe cleared.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void clearIngredient() {
        String dish = dishField.getValue();
        String size = sizeField.getValue();
        String ingredient = ingredientsField.getValue();

        if (dish == null || size == null || ingredient == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Please select dish, size, and ingredient.");
            alert.show();
            return;
        }

        String deleteQuery = "DELETE FROM Recipes WHERE ProductName = ? AND Size = ? AND IngredientsName = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {

            stmt.setString(1, dish);
            stmt.setString(2, size);
            stmt.setString(3, ingredient);
            stmt.executeUpdate();
            loadRecipe();

        } catch (SQLException e) {
            e.printStackTrace();
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
