package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;

public class IncomeController {
    @FXML private TableView<IncomeRecord> incomeTable;
    @FXML private TableColumn<IncomeRecord, String> monthColumn;
    @FXML private TableColumn<IncomeRecord, Double> amountColumn;
    @FXML private TextField totalIncomeField;

    public void initialize() {
        // Set up table columns
        monthColumn.setCellValueFactory(new PropertyValueFactory<>("month"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));

        // Format amount column as currency
        amountColumn.setCellFactory(tc -> new javafx.scene.control.TableCell<IncomeRecord, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                setText(empty || amount == null ? null : String.format("₱%,.2f", amount));
            }
        });

        loadIncomeData();
    }

    private void loadIncomeData() {
        ObservableList<IncomeRecord> incomeData = FXCollections.observableArrayList();
        double totalIncome = 0.0;

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                totalIncomeField.setText("Database connection failed");
                return;
            }

            // Modified query to handle the type conversion issue
            // Use parameterized query and check data type of OrderID
            String query = "SELECT OrderID, OrderDate, TotalPrice FROM Orders WHERE OrderID IS NOT NULL";

            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                Map<Integer, Double> monthlyIncome = new HashMap<>();

                // Initialize all months with 0 income
                for (int i = 1; i <= 12; i++) {
                    monthlyIncome.put(i, 0.0);
                }

                // Process results
                while (rs.next()) {
                    // Skip records with "(New)" OrderID
                    String orderId = rs.getString("OrderID");
                    if (orderId != null && orderId.equals("(New)")) {
                        continue;
                    }

                    try {
                        Date orderDate = rs.getDate("OrderDate");
                        // Skip records with null dates
                        if (orderDate != null) {
                            double price = 0.0;

                            // Safely get TotalPrice - handle potential parsing errors
                            try {
                                price = rs.getDouble("TotalPrice");
                            } catch (SQLException e) {
                                // Log the error but continue processing other records
                                System.err.println("Error parsing TotalPrice for OrderID: " + orderId);
                                System.err.println(e.getMessage());
                                continue;
                            }

                            int month = orderDate.toLocalDate().getMonthValue();
                            monthlyIncome.merge(month, price, Double::sum);
                            totalIncome += price;
                        }
                    } catch (SQLException e) {
                        // Handle date parsing errors
                        System.err.println("Error processing OrderDate for OrderID: " + orderId);
                        System.err.println(e.getMessage());
                        continue;
                    }
                }

                // Convert to table data
                monthlyIncome.forEach((month, income) -> {
                    incomeData.add(new IncomeRecord(Month.of(month).toString(), income));
                });

                // Sort by month
                incomeData.sort((a, b) ->
                        Month.valueOf(a.getMonth()).compareTo(Month.valueOf(b.getMonth())));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            totalIncomeField.setText("Error loading data: " + e.getMessage());
            return;
        }

        // Update UI
        incomeTable.setItems(incomeData);
        totalIncomeField.setText(String.format("₱%,.2f", totalIncome));
    }

    // Model class for table data
    public static class IncomeRecord {
        private final String month;
        private final double amount;

        public IncomeRecord(String month, double amount) {
            this.month = month;
            this.amount = amount;
        }

        public String getMonth() { return month; }
        public double getAmount() { return amount; }
    }
}