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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import view.LoginForm;
import view.StockItem;


import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class StockController {

    @FXML
    private ComboBox<String> nameField;

    @FXML
    private TextField unitField;

    @FXML
    private TextField quantityField;

    @FXML
    private TextField priceField;  // Added price field

    @FXML
    private ComboBox<String> filterBox;

    @FXML
    private TableView<StockItem> stocksTable;

    @FXML
    private TableColumn<StockItem, String> nameColumn;

    @FXML
    private TableColumn<StockItem, String> categoryColumn;

    @FXML
    private TableColumn<StockItem, Integer> quantityColumn;

    @FXML
    private TableColumn<StockItem, String> unitColumn;

    @FXML
    private TableColumn<StockItem, String> updatesColumn;

    @FXML
    private TableColumn<StockItem, Double> priceColumn;  // Added price column

    @FXML
    private Button btnAddStock;

    @FXML
    private Button btnDeductStock;

    @FXML
    private TextField searchBox;

    private ObservableList<StockItem> masterStockList = FXCollections.observableArrayList();


    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yy");

    @FXML
    public void initialize() {
        setupTableColumns();
        loadIngredientsIntoComboBox();
        loadStockTable();
        loadCategories();
        highlightLowStock();


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

        btnAddStock.setOnAction(event -> saveStock());
        btnDeductStock.setOnAction(event -> deductStock());

        // Add listener for nameField to update unitField based on selected name
        nameField.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                loadUnitTypeForIngredient(newValue);
            }
        });

        filterBox.setOnAction(e -> {
            String selectedCategory = filterBox.getValue();
            if (selectedCategory != null && !selectedCategory.isEmpty()) {
                filterStockTableByCategory(selectedCategory);
            }
        });

        searchBox.textProperty().addListener((observable, oldValue, newValue) -> {
            filterStockTableBySearch(newValue);
        });

        priceColumn.setCellValueFactory(cellData -> cellData.getValue().priceProperty().asObject());

    }

    private void filterStockTableBySearch(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            stocksTable.setItems(masterStockList);
            highlightLowStock();
            return;
        }

        ObservableList<StockItem> filteredList = FXCollections.observableArrayList();

        for (StockItem item : masterStockList) {
            if (item.getName().toLowerCase().contains(searchText.toLowerCase())) {
                filteredList.add(item);
            }
        }

        stocksTable.setItems(filteredList);
        highlightLowStock();
    }


    private void filterStockTableByCategory(String category) {
        ObservableList<StockItem> allItems = FXCollections.observableArrayList();
        ObservableList<StockItem> filteredItems = FXCollections.observableArrayList();

        String query = "SELECT * FROM Ingredients";

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                StockItem item = new StockItem(
                        resultSet.getString("IngredientsName"),
                        resultSet.getString("Category"),
                        resultSet.getInt("Quantity"),
                        resultSet.getString("UnitType"),
                        "",
                        0.0
                );
                allItems.add(item);
            }

            for (StockItem item : allItems) {
                if (item.getCategory().equals(category)) {
                    filteredItems.add(item);
                }
            }

            // Sort so low-stock items appear at top
            filteredItems.sort((item1, item2) -> {
                boolean low1 = item1.getQuantity() < 1000;
                boolean low2 = item2.getQuantity() < 1000;
                if (low1 && !low2) return -1;
                else if (!low1 && low2) return 1;
                else return 0;
            });

            stocksTable.setItems(filteredItems);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "An error occurred while filtering stocks.");
        }
    }



    private void highlightLowStock() {
        stocksTable.setRowFactory(tv -> new TableRow<StockItem>() {
            @Override
            protected void updateItem(StockItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setStyle("");
                } else if (item.getQuantity() < 1000) {
                    setStyle("-fx-background-color: #ffcccc;");  // Light red
                } else {
                    setStyle("");  // Reset style for other rows
                }
            }
        });
    }



    private void setupTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));
        updatesColumn.setCellValueFactory(new PropertyValueFactory<>("updates"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));  // Set the price column
    }

    private void loadIngredientsIntoComboBox() {
        ObservableList<String> ingredientList = FXCollections.observableArrayList();

        String query = "SELECT IngredientsName FROM Ingredients";

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                ingredientList.add(resultSet.getString("IngredientsName"));
            }
            nameField.setItems(ingredientList);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadStockTable() {
        ObservableList<StockItem> stockList = FXCollections.observableArrayList();

        String query = "SELECT * FROM Ingredients";

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                stockList.add(new StockItem(
                        resultSet.getString("IngredientsName"),
                        resultSet.getString("Category"),
                        resultSet.getInt("Quantity"),
                        resultSet.getString("UnitType"),
                        "",
                        0.0 // Price is initially 0.0 (it will be updated later)
                ));
            }

            stocksTable.setItems(stockList);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void loadUnitTypeForIngredient(String ingredientName) {
        String query = "SELECT UnitType FROM Ingredients WHERE IngredientsName = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, ingredientName);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String unitType = resultSet.getString("UnitType");
                    unitField.setText(unitType); // Set the unit type in the unitField
                } else {
                    unitField.clear();  // Clear the field if no result found
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "An error occurred while loading the unit type.");
        }
    }


    @FXML
    private void saveStock() {
        String name = nameField.getValue();
        String unitType = unitField.getText();
        String quantityText = quantityField.getText();
        String priceText = priceField.getText();
        String username = LoginForm.getUserName();

        if (name == null || name.isEmpty() || unitType.isEmpty() || quantityText.isEmpty() || priceText.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please fill in all fields.");
            return;
        }

        int quantity;
        double price;
        try {
            quantity = Integer.parseInt(quantityText);
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Quantity and price must be valid numbers.");
            return;
        }

        String dateTimeNow = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Connection connection = null;
        PreparedStatement insertTransaction = null;
        PreparedStatement updateIngredient = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            // Insert into Transactions with Type = "Add"
            String insertTransactionQuery = "INSERT INTO Transactions (IngredientsName, Quantity, UnitType, DateTime, Username, TotalCost, Type) VALUES (?, ?, ?, ?, ?, ?, ?)";
            insertTransaction = connection.prepareStatement(insertTransactionQuery);
            insertTransaction.setString(1, name);
            insertTransaction.setInt(2, quantity);
            insertTransaction.setString(3, unitType);
            insertTransaction.setString(4, dateTimeNow);
            insertTransaction.setString(5, username);
            insertTransaction.setDouble(6, price);
            insertTransaction.setString(7, "Add");
            insertTransaction.executeUpdate();

            // Update Ingredients quantity
            String updateIngredientQuery = "UPDATE Ingredients SET Quantity = Quantity + ? WHERE IngredientsName = ?";
            updateIngredient = connection.prepareStatement(updateIngredientQuery);
            updateIngredient.setInt(1, quantity);
            updateIngredient.setString(2, name);
            updateIngredient.executeUpdate();

            connection.commit();

            showAlert(Alert.AlertType.INFORMATION, "Success", "Stock added successfully!");
            loadStockTable();
            updateTableWithChanges(name, quantity, price);

        } catch (SQLException e) {
            try {
                if (connection != null) connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "An error occurred while saving stock.");
        } finally {
            try {
                if (insertTransaction != null) insertTransaction.close();
                if (updateIngredient != null) updateIngredient.close();
                if (connection != null) connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void deductStock(){
        String name = nameField.getValue();
        String unitType = unitField.getText();
        String quantityText = quantityField.getText();
        String username = LoginForm.getUserName();

        if (name == null || name.isEmpty() || unitType.isEmpty() || quantityText.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please fill in all fields.");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityText);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Quantity must be a number.");
            return;
        }

        String dateTimeNow = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Connection connection = null;
        PreparedStatement insertTransaction = null;
        PreparedStatement updateIngredient = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            // Insert into Transactions with Type = "Deduct"
            String insertTransactionQuery = "INSERT INTO Transactions (IngredientsName, Quantity, UnitType, DateTime, Username, TotalCost, Type) VALUES (?, ?, ?, ?, ?, ?, ?)";
            insertTransaction = connection.prepareStatement(insertTransactionQuery);
            insertTransaction.setString(1, name);
            insertTransaction.setInt(2, -quantity);
            insertTransaction.setString(3, unitType);
            insertTransaction.setString(4, dateTimeNow);
            insertTransaction.setString(5, username);
            insertTransaction.setDouble(6, 0.0);
            insertTransaction.setString(7, "Deduct");
            insertTransaction.executeUpdate();

            // Update Ingredients quantity
            String updateIngredientQuery = "UPDATE Ingredients SET Quantity = Quantity - ? WHERE IngredientsName = ?";
            updateIngredient = connection.prepareStatement(updateIngredientQuery);
            updateIngredient.setInt(1, quantity);
            updateIngredient.setString(2, name);
            updateIngredient.executeUpdate();

            connection.commit();

            showAlert(Alert.AlertType.INFORMATION, "Success", "Stock deducted successfully!");
            loadStockTable();
            updateTableWithChanges(name, -quantity, 0.0);


        } catch (SQLException e) {
        } finally {
            try {
                if (insertTransaction != null) insertTransaction.close();
                if (updateIngredient != null) updateIngredient.close();
                if (connection != null) connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }



    private void updateTableWithChanges(String updatedName, int addedQuantity, double price) {
        for (StockItem item : stocksTable.getItems()) {
            if (item.getName().equals(updatedName)) {
                String updateText = (addedQuantity >= 0 ? "+" : "") + addedQuantity;
                item.setUpdates(updateText);
                item.setPrice(price);  // Update price in table
                break;
            }
        }
        stocksTable.refresh();
    }



    private void loadCategories() {
        String query = "SELECT DISTINCT Category FROM Ingredients";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            filterBox.getItems().clear();
            while (rs.next()) {
                filterBox.getItems().add(rs.getString("Category"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleEditButtonClick(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/AddNewStock.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);

            stage.show();
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
    public void exportToExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Stock Inventory Report");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );
        fileChooser.setInitialFileName("Stock_Inventory_Report.xlsx");

        File file = fileChooser.showSaveDialog(stocksTable.getScene().getWindow());
        if (file != null) {
            try {
                createExcelReport(file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText("Stock inventory report has been exported successfully to:\n" + file.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Error");
                alert.setHeaderText(null);
                alert.setContentText("Failed to export report: " + e.getMessage());
                alert.showAndWait();
                e.printStackTrace();
            }
        }
    }

    private void createExcelReport(File file) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Stock Inventory");

        // Header row
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Name");
        header.createCell(1).setCellValue("Category");
        header.createCell(2).setCellValue("Quantity");
        header.createCell(3).setCellValue("Unit");
        header.createCell(4).setCellValue("Updates");

        // Data rows
        ObservableList<StockItem> stockItems = stocksTable.getItems();
        for (int i = 0; i < stockItems.size(); i++) {
            StockItem item = stockItems.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(item.getName());
            row.createCell(1).setCellValue(item.getCategory());
            row.createCell(2).setCellValue(item.getQuantity());
            row.createCell(3).setCellValue(item.getUnit());
            row.createCell(4).setCellValue(item.getUpdates());
        }

        // Auto-size columns
        for (int i = 0; i <= 5; i++) {
            sheet.autoSizeColumn(i);
        }

        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            workbook.write(fileOut);
        }

        workbook.close();
    }


}
