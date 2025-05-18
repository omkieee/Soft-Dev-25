package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.Node;


import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import javafx.stage.Stage;
import view.Dish;

import javax.swing.*;

public class DishesController {

    @FXML
    private TableView<Dish> dishTable;

    @FXML
    private TableColumn<Dish, String> nameColumn;

    @FXML
    private TableColumn<Dish, String> categoryColumn;

    @FXML
    private TableColumn<Dish, Double> priceColumn;

    @FXML
    private TableColumn<Dish, String> sizeColumn;

    @FXML
    private TableColumn<Dish, Integer> servingsColumn;

    @FXML
    private ComboBox<String> filterBox;

    // ObservableList to hold data for the TableView
    private ObservableList<Dish> dishList = FXCollections.observableArrayList();

    @FXML
    private TextField searchBox;


    // Method to apply filters based on category and search
    private void applyFilters() {
        String categoryFilter = filterBox.getValue(); // Get the selected category
        String searchText = searchBox.getText().toLowerCase(); // Get the search text and convert it to lowercase

        // Filter the list of dishes based on the category and search text
        ObservableList<Dish> filteredList = FXCollections.observableArrayList();

        for (Dish dish : dishList) {
            boolean matchesCategory = categoryFilter.equals("All") || dish.getCategory().equals(categoryFilter);
            boolean matchesSearch = dish.getName().toLowerCase().contains(searchText);

            if (matchesCategory && matchesSearch) {
                filteredList.add(dish);
            }
        }

        // Update the table with the filtered list
        dishTable.setItems(filteredList);
    }

    // Method to initialize the controller
    @FXML
    public void initialize() {
        setupTableColumns();
        loadDataFromDatabase();
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

        // Apply filter when category is changed
        filterBox.setOnAction(event -> applyFilters());

        searchBox.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters(); // Call your filtering method
        });
    }

    @FXML
    private void handleEditButtonClick(javafx.event.ActionEvent event) {
        try {
            // Load the AddNewDish view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/AddNewDish.fxml"));
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

    // Set up table columns with corresponding properties from the Dish model class
    private void setupTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
        servingsColumn.setCellValueFactory(new PropertyValueFactory<>("servings"));

        // Bind the ObservableList to the TableView
        dishTable.setItems(dishList);
    }

    private void loadCategories() {
        String query = "SELECT DISTINCT Category FROM dish";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            filterBox.getItems().clear();

            // Add the "All" option
            filterBox.getItems().add("All");

            // Add the categories from the database
            while (rs.next()) {
                filterBox.getItems().add(rs.getString("Category"));
            }

            // Set the default selection to "All"
            filterBox.setValue("All");

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    // Load data from the database into the ObservableList
    private void loadDataFromDatabase() {
        Connection connection = DatabaseConnection.getConnection();
        if (connection != null) {
            try {
                Statement statement = connection.createStatement();
                String query = "SELECT ProductName, Category, Price, Size, Servings FROM Dish"; // Adjust table and column names as needed

                ResultSet resultSet = statement.executeQuery(query);

                while (resultSet.next()) {
                    // Create a Dish object for each row in the result set
                    Dish dish = new Dish(
                            resultSet.getString("ProductName"),
                            resultSet.getString("Category"),
                            resultSet.getDouble("Price"),
                            resultSet.getString("Size"),
                            resultSet.getInt("Servings")
                    );

                    // Add the Dish object to the ObservableList
                    dishList.add(dish);
                }

                connection.close(); // Close connection after use

            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Failed to connect to the database.");
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


    private void navigate(javafx.event.ActionEvent event, String fxmlFile, String title) {
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
    private void onHomeBtnClick(javafx.event.ActionEvent event) {
        navigate(event, "homeFrame.fxml", "Mr. Are Level Up Cuisine | Home Page");
    }

    @FXML
    private void onStocksBtnClick(javafx.event.ActionEvent event) {
        navigate(event, "Stock.fxml", "Mr. Are Level Up Cuisine | Stock Page");
    }

    @FXML
    private void onRecipeBtnClick(javafx.event.ActionEvent event) {
        navigate(event, "Recipe.fxml", "Mr. Are Level Up Cuisine | Recipe Page");
    }

    @FXML
    private void onDishBtnClick(javafx.event.ActionEvent event) {
        navigate(event, "Dishes.fxml", "Mr. Are Level Up Cuisine | Dishes Page");
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
        navigate(event, "homeFrameStaff.fxml", "Mr. Are Level Up Cuisine | Home Page");
    }

    @FXML
    private void onAddNewStockBtnClick(javafx.event.ActionEvent event) {
        navigate(event, "AddNewStock.fxml", "Mr. Are Level Up Cuisine | Add Stock Page");
    }

    @FXML
    private void onAddNewRecipeBtnClick(javafx.event.ActionEvent event) {
        navigate(event, "AddNewRecipe.fxml", "Mr. Are Level Up Cuisine | Add Recipe Page");
    }

    @FXML
    private void onAddNewDishBtnClick(javafx.event.ActionEvent event) {
        navigate(event, "AddNewDish.fxml", "Mr. Are Level Up Cuisine | Add Dish Page");
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
