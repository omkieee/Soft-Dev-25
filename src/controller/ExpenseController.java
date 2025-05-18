package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ExpenseController {
    @FXML private TableView<ExpenseRecord> expenseTable;
    @FXML private TableColumn<ExpenseRecord, String> materialColumn;
    @FXML private TableColumn<ExpenseRecord, Double> priceColumn;
    @FXML private TextField totalExpensesField;

    private final DateTimeFormatter dbDateFormat = DateTimeFormatter.ofPattern("MM/dd/yy");
    private final DateTimeFormatter fullDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void initialize() {
        // Set up table columns
        materialColumn.setCellValueFactory(new PropertyValueFactory<>("material"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));

        // Format price column as currency
        priceColumn.setCellFactory(tc -> new javafx.scene.control.TableCell<ExpenseRecord, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("₱%,.2f", price));
            }
        });

        loadExpenseData();
    }

    private void loadExpenseData() {
        ObservableList<ExpenseRecord> expenseData = FXCollections.observableArrayList();
        double totalExpenses = 0.0;

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                totalExpensesField.setText("Database connection failed");
                return;
            }

            // Query to get ingredient transactions data
            // Since the total cost in the DB shows ₱0.00 for most entries,
            // we'll calculate expense based on quantity and a simulated cost per unit
            String query = "SELECT IngredientsName, Quantity, UnitType, DateTime, TotalCost " +
                    "FROM Transactions " +
                    "WHERE IngredientsName IS NOT NULL";

            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                // Map to track total expense per ingredient
                Map<String, Double> materialExpenses = new HashMap<>();

                // Process results
                while (rs.next()) {
                    try {
                        String ingredient = rs.getString("IngredientsName");

                        // Skip null ingredients or empty strings
                        if (ingredient == null || ingredient.trim().isEmpty()) {
                            continue;
                        }

                        double quantity = rs.getDouble("Quantity");
                        String unit = rs.getString("UnitType");
                        String dateTime = rs.getString("DateTime");

                        // Get actual cost if available, otherwise calculate estimated cost
                        double cost = 0.0;
                        String costStr = rs.getString("TotalCost");

                        // Try to parse the cost, handle ₱ symbol if present
                        if (costStr != null && !costStr.trim().isEmpty()) {
                            // Remove the peso sign and any commas
                            costStr = costStr.replace("₱", "").replace(",", "").trim();

                            try {
                                cost = Double.parseDouble(costStr);
                            } catch (NumberFormatException e) {
                                // If parsing fails, estimate cost based on ingredient
                                cost = estimateCost(ingredient, quantity, unit);
                            }
                        } else {
                            // Estimate cost based on ingredient
                            cost = estimateCost(ingredient, quantity, unit);
                        }

                        // Add to the map, summing up expenses for the same ingredient
                        materialExpenses.merge(ingredient, cost, Double::sum);
                        totalExpenses += cost;

                    } catch (SQLException e) {
                        System.err.println("Error processing expense record: " + e.getMessage());
                    }
                }

                // Convert map to table data
                materialExpenses.forEach((material, expense) -> {
                    expenseData.add(new ExpenseRecord(material, expense));
                });

                // Sort by price (highest first)
                expenseData.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            totalExpensesField.setText("Error loading data: " + e.getMessage());
            return;
        }

        // Update UI
        expenseTable.setItems(expenseData);
        totalExpensesField.setText(String.format("₱%,.2f", totalExpenses));
    }

    // Helper method to estimate cost based on ingredient type and quantity
    private double estimateCost(String ingredient, double quantity, String unit) {
        // These are estimated prices based on common market values
        // In a real system, these would come from a pricing database
        switch (ingredient.toLowerCase()) {
            case "chicken":
                return quantity * (unit.equals("g") ? 0.20 : 50.00); // ₱200 per kg
            case "pork":
                return quantity * (unit.equals("g") ? 0.25 : 60.00); // ₱250 per kg
            case "onion":
                return quantity * (unit.equals("g") ? 0.15 : 30.00); // ₱150 per kg
            case "soy sauce":
                return quantity * (unit.equals("cups") ? 20.00 : 0.10); // ₱20 per cup
            default:
                return quantity * (unit.equals("g") ? 0.10 : 25.00); // Default cost
        }
    }

    // Model class for table data
    public static class ExpenseRecord {
        private final String material;
        private final double price;

        public ExpenseRecord(String material, double price) {
            this.material = material;
            this.price = price;
        }

        public String getMaterial() { return material; }
        public double getPrice() { return price; }
    }
}