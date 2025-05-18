package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;
import view.Recipe;
import view.Ingredient;

import javax.swing.*;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecipeController {

    @FXML
    private TableView<Recipe> dishTable;

    @FXML
    private TableColumn<Recipe, String> productNameColumn;

    @FXML
    private TableColumn<Recipe, String> categoryColumn;

    @FXML
    private TableColumn<Recipe, Void> actionColumn;

    @FXML
    private ComboBox<String> filterBox;

    @FXML
    private Label recipeName;

    @FXML
    private Label sizeName; // NEW: Label to show dish size

    @FXML
    private TableView<Ingredient> recipeTable;

    @FXML
    private TableColumn<Ingredient, String> nameColumn;

    @FXML
    private TableColumn<Ingredient, Double> amountColumn;

    @FXML
    private TableColumn<Ingredient, String> unitColumn;

    @FXML
    private TableColumn<Recipe, String> sizeColumn;

    @FXML
    private TextField searchBox;

    @FXML
    private Label totalQuantity;

    private ObservableList<Ingredient> ingredientList = FXCollections.observableArrayList();
    private ObservableList<Recipe> recipeList = FXCollections.observableArrayList();

    public void initialize() {
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));

        addViewRecipeButtonToTable();
        loadRecipeData();
        loadCategories();

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

        filterBox.setOnAction(event -> applyFilters());
        searchBox.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void applyFilters() {
        String categoryFilter = filterBox.getValue(); // Get the selected category
        String searchText = searchBox.getText().toLowerCase().trim(); // Get the search text and convert it to lowercase

        // Filter the list of recipes based on the category and search text
        ObservableList<Recipe> filteredList = FXCollections.observableArrayList();

        for (Recipe recipe : recipeList) {
            // Normalize category filter for case-insensitive matching
            boolean matchesCategory = categoryFilter == null || categoryFilter.equals("All") || recipe.getCategory().toLowerCase().contains(categoryFilter.toLowerCase());

            // Normalize search text for case-insensitive matching
            boolean matchesSearch = recipe.getProductName().toLowerCase().contains(searchText);

            if (matchesCategory && matchesSearch) {
                filteredList.add(recipe);
            }
        }

        // Update the table with the filtered list
        dishTable.setItems(filteredList);
    }

    @FXML
    private void handleEditButtonClick(javafx.event.ActionEvent event) {
        try {
            // Load the AddNewDish view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/AddNewRecipe.fxml"));
            Parent root = loader.load();

            // Get the current stage (window) from the event source
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Set the new scene
            Scene scene = new Scene(root);
            stage.setScene(scene);

            // Optional: Set a title for the new stage
            stage.setTitle("Add New Dish");

            // Show the stage
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void loadCategories() {
        String query = "SELECT DISTINCT Category FROM dish";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            filterBox.getItems().clear();
            filterBox.getItems().add("All");  // Add "All" to the ComboBox

            while (rs.next()) {
                filterBox.getItems().add(rs.getString("Category"));
            }

            // Set default filter to "All" if no category is selected
            if (filterBox.getValue() == null) {
                filterBox.setValue("All");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void loadRecipeData() {
        recipeList.clear();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT ProductName, Category, Size FROM Dish")) {

            while (rs.next()) {
                String productName = rs.getString("ProductName");
                String category = rs.getString("Category");
                String size = rs.getString("Size");

                recipeList.add(new Recipe(productName, category, size));
            }

            // Initially display all the data before filtering
            dishTable.setItems(recipeList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addViewRecipeButtonToTable() {
        Callback<TableColumn<Recipe, Void>, TableCell<Recipe, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Recipe, Void> call(final TableColumn<Recipe, Void> param) {
                return new TableCell<>() {

                    private final Button btn = new Button("View Recipe");

                    {
                        btn.getStyleClass().add("navButton2");

                        btn.setOnAction(event -> {
                            Recipe selectedRecipe = getTableView().getItems().get(getIndex());
                            loadRecipeTable(selectedRecipe);
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                            setStyle("-fx-alignment: CENTER;");
                        }
                    }
                };
            }
        };

        actionColumn.setCellFactory(cellFactory);
    }

    private void loadRecipeTable(Recipe recipe) {
        String selectedProductName = recipe.getProductName();
        recipeName.setText(selectedProductName); // Set label for dish name
        sizeName.setText(recipe.getSize());      // Set label for dish size

        ingredientList.clear();

        try (Connection conn = DatabaseConnection.getConnection()) {

            // Get ingredients from 'Recipes' table
            String recipeQuery = "SELECT IngredientsName, Quantity, UnitType FROM Recipes WHERE ProductName = ?";
            try (PreparedStatement recipeStmt = conn.prepareStatement(recipeQuery)) {
                recipeStmt.setString(1, selectedProductName);
                ResultSet rs = recipeStmt.executeQuery();

                while (rs.next()) {
                    String name = rs.getString("IngredientsName");
                    double quantity = rs.getDouble("Quantity");
                    String unit = rs.getString("UnitType");

                    ingredientList.add(new Ingredient(name, quantity, unit));
                }
            }

            // Setup columns and load data
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
            amountColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
            unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));
            recipeTable.setItems(ingredientList);

        } catch (Exception e) {
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
    private void onStocksBtnClick(ActionEvent event) {
        navigate(event, "Stock.fxml", "Mr. Are Level Up Cuisine | Stock Page");
    }

    @FXML
    private void onRecipeBtnClick(ActionEvent event) {
        navigate(event, "Recipe.fxml", "Mr. Are Level Up Cuisine | Recipe Page");
    }

    @FXML
    private void onDishBtnClick(ActionEvent event) {
        navigate(event, "Dishes.fxml", "Mr. Are Level Up Cuisine | Dishes Page");
    }

    @FXML
    private void onAddNewStockBtnClick(ActionEvent event) {
        navigate(event, "AddNewStock.fxml", "Mr. Are Level Up Cuisine | Add Stock Page");
    }

    @FXML
    private void onAddNewRecipeBtnClick(ActionEvent event) {
        navigate(event, "AddNewRecipe.fxml", "Mr. Are Level Up Cuisine | Add Recipe Page");
    }

    @FXML
    private void onAddNewDishBtnClick(ActionEvent event) {
        navigate(event, "AddNewDish.fxml", "Mr. Are Level Up Cuisine | Add Dish Page");}

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


    @FXML
    private void onStocksStaffBtnClick(javafx.event.ActionEvent event) {
        navigate(event, "StockStaff.fxml", "Mr. Are Level Up Cuisine | Stock Page");
    }

    @FXML
    private void onRecipeStaffBtnClick(javafx.event.ActionEvent event) {
        navigate(event, "RecipeStaff.fxml", "Mr. Are Level Up Cuisine | Recipe Page");
    }

    @FXML
    private void onDishStaffBtnClick(javafx.event.ActionEvent event) {
        navigate(event, "DishesStaff.fxml", "Mr. Are Level Up Cuisine | Dishes Page");
    }

    @FXML
    private void onHomeStaffBtnClick(javafx.event.ActionEvent event) {
        navigate(event, "homeFrameStaff.fxml", "Mr. Are Level Up Cuisine | Home Page");}
}
