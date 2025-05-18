package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import view.LoginForm;

public class ChartController {

    @FXML private LineChart<String, Number> lineChart;  // LineChart element in FXML
    @FXML private CategoryAxis monthAxis;  // X-Axis for months
    @FXML private NumberAxis valueAxis;  // Y-Axis for values
    @FXML private ComboBox<String> yearComboBox;  // ComboBox for selecting year
    @FXML private Button overviewRefreshData;  // Refresh Button
    @FXML private Label businessYearLabel;  // Label for "Business Year"

    @FXML
    private Button homeBtn;
    @FXML
    private Button IncomeBtn;
    @FXML
    private Button ExpenseBtn;
    @FXML
    private Button ProfitBtn;

    private List<Button> navButtons;

    private Map<String, Double> monthlyProfit = new HashMap<>();  // Storing monthly profit data

    // Initialize method to populate ComboBox and set up event handlers
    public void initialize() {
        populateYearComboBox();

        // Set action for year selection
        yearComboBox.setOnAction(e -> refreshData());

        // Set chart title and axis labels
        lineChart.setTitle("Monthly Profit Overview");
        monthAxis.setLabel("Month");
        valueAxis.setLabel("Profit (₱)");

        // Rotate month labels for better readability
        monthAxis.setTickLabelRotation(45);

        monthAxis.setTickLabelFont(Font.font("SansSerif", FontWeight.BOLD, 25));
        valueAxis.setTickLabelFont(Font.font("SansSerif", FontWeight.BOLD, 17));

        // Load data on initialization
        refreshData();

        navButtons = new ArrayList<>();
        navButtons.add(homeBtn);
        navButtons.add(IncomeBtn);
        navButtons.add(ExpenseBtn);
        navButtons.add(ProfitBtn);
    }

    // Method to populate ComboBox with year options
    private void populateYearComboBox() {
        int currentYear = java.time.Year.now().getValue();
        ObservableList<String> years = FXCollections.observableArrayList();

        for (int year = currentYear - 5; year <= currentYear + 1; year++) {
            years.add(String.valueOf(year));
        }

        yearComboBox.setItems(years);
        yearComboBox.setValue(String.valueOf(currentYear)); // Set current year as default
    }


    // Method to handle the refresh button's action
    @FXML
    private void refreshData() {
        String selectedYear = yearComboBox.getValue();
        int year = Integer.parseInt(selectedYear);

        System.out.println("ChartController: Refreshing data for year " + selectedYear);

        // First try to get data from ProfitController's static cache
        if (ProfitDataCache.hasProfitData(year)) {
            System.out.println("Loading from cache for year: " + selectedYear);

            // Get data from cache
            Map<String, Double> cachedProfits = ProfitDataCache.getProfitDataForYear(year);

            // Check if we got valid data
            if (cachedProfits != null && !cachedProfits.isEmpty()) {
                System.out.println("Successfully loaded profit data from cache");

                // Clear existing data and add new data
                monthlyProfit.clear();
                monthlyProfit.putAll(cachedProfits);

                // Debug: print out what we got from cache
                cachedProfits.forEach((month, profit) ->
                        System.out.printf("From cache - Month: %s → Profit: ₱%.2f%n", month, profit)
                );

                // Update the chart with new data
                updateChart();
            } else {
                System.out.println("Cache returned empty data. Falling back to database.");
                // Fall back to database
                loadProfitData(selectedYear);
            }
        } else {
            System.out.println("No cache data for year " + selectedYear + ". Loading from database.");
            // No cached data, load from database
            loadProfitData(selectedYear);
        }
    }

    // Fallback method to load profit data based on selected year directly from database
    private void loadProfitData(String selectedYear) {
        monthlyProfit.clear();

        // Initialize all months with 0 profit
        for (Month month : Month.values()) {
            monthlyProfit.put(month.toString(), 0.0);
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                // Handle connection failure
                System.err.println("Database connection failed.");
                return;
            }

            // Calculate income
            calculateIncomeFromDatabase(conn, selectedYear);

            // Calculate expenses
            calculateExpensesFromDatabase(conn, selectedYear);

            // Now update the chart with calculated data
            updateChart();

            // Also cache this data for future use
            int year = Integer.parseInt(selectedYear);
            ProfitDataCache.storeProfitData(year, new HashMap<>(monthlyProfit));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Helper method to calculate income from database
    private void calculateIncomeFromDatabase(Connection conn, String selectedYear) throws SQLException {
        Map<String, Double> monthlyIncome = new HashMap<>();

        // Initialize all months with 0 income
        for (Month month : Month.values()) {
            monthlyIncome.put(month.name(), 0.0);
        }

        // Query to fetch income data based on year from the database
        String query = "SELECT OrderID, OrderDate, TotalPrice FROM Orders WHERE OrderID IS NOT NULL " +
                "AND Year(OrderDate) = ?\n";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, selectedYear);
            try (ResultSet rs = stmt.executeQuery()) {
                // Process result set
                while (rs.next()) {
                    // Skip records with "(New)" OrderID
                    String orderId = rs.getString("OrderID");
                    if (orderId != null && orderId.equals("(New)")) {
                        continue;
                    }

                    try {
                        java.sql.Date orderDate = rs.getDate("OrderDate");
                        // Skip records with null dates
                        if (orderDate != null) {
                            double price = 0.0;

                            // Safely get TotalPrice - handle potential parsing errors
                            try {
                                price = rs.getDouble("TotalPrice");
                            } catch (SQLException e) {
                                System.err.println("Error parsing TotalPrice for OrderID: " + orderId);
                                System.err.println(e.getMessage());
                                continue;
                            }

                            String monthName = Month.of(orderDate.toLocalDate().getMonthValue()).name();
                            monthlyIncome.merge(monthName, price, Double::sum);
                        }
                    } catch (SQLException e) {
                        // Handle date parsing errors
                        System.err.println("Error processing OrderDate for OrderID: " + orderId);
                        System.err.println(e.getMessage());
                    }
                }
            }
        }

        // Store income data temporarily - will be used to calculate profit
        for (Map.Entry<String, Double> entry : monthlyIncome.entrySet()) {
            // Initialize profit with income (expenses will be subtracted later)
            monthlyProfit.put(entry.getKey(), entry.getValue());
        }
    }

    // Helper method to calculate expenses from database
    private void calculateExpensesFromDatabase(Connection conn, String selectedYear) throws SQLException {
        Map<String, Double> monthlyExpenses = new HashMap<>();

        // Initialize all months with 0 expenses
        for (Month month : Month.values()) {
            monthlyExpenses.put(month.name(), 0.0);
        }

        // Query to fetch expense data
        String query = "SELECT IngredientsName, Quantity, UnitType, DateTime, TotalCost " +
                "FROM Transactions " +
                "WHERE IngredientsName IS NOT NULL";

        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                try {
                    String ingredient = rs.getString("IngredientsName");
                    if (ingredient == null || ingredient.trim().isEmpty()) continue;

                    String dateTimeStr = rs.getString("DateTime");

                    // Skip if no date or wrong year
                    if (dateTimeStr == null || !isFromSelectedYear(dateTimeStr, selectedYear)) {
                        continue;
                    }

                    // Parse cost safely
                    double cost = 0.0;
                    String costStr = rs.getString("TotalCost");
                    if (costStr != null && !costStr.trim().isEmpty()) {
                        costStr = costStr.replace("₱", "").replace(",", "").trim();
                        try {
                            cost = Double.parseDouble(costStr);
                        } catch (NumberFormatException e) {
                            double quantity = rs.getDouble("Quantity");
                            String unit = rs.getString("UnitType");
                            cost = estimateCost(ingredient, quantity, unit);
                        }
                    } else {
                        double quantity = rs.getDouble("Quantity");
                        String unit = rs.getString("UnitType");
                        cost = estimateCost(ingredient, quantity, unit);
                    }

                    // Default to JANUARY if date can't be parsed
                    String monthName = "JANUARY";

                    if (dateTimeStr != null && !dateTimeStr.trim().isEmpty()) {
                        java.time.LocalDate parsedDate = parseDate(dateTimeStr);

                        if (parsedDate != null) {
                            monthName = parsedDate.getMonth().name();
                        }
                    }

                    monthlyExpenses.merge(monthName, cost, Double::sum);

                } catch (SQLException e) {
                    System.err.println("Error processing expense record: " + e.getMessage());
                }
            }
        }

        // Calculate profit by subtracting expenses from income
        for (Map.Entry<String, Double> entry : monthlyExpenses.entrySet()) {
            String month = entry.getKey();
            Double expense = entry.getValue();

            // Get current profit (which is initially set to income)
            Double currentProfit = monthlyProfit.getOrDefault(month, 0.0);

            // Update profit by subtracting expense
            monthlyProfit.put(month, currentProfit - expense);
        }
    }

    // Helper method to check if a date string is from the selected year
    private boolean isFromSelectedYear(String dateTimeStr, String selectedYear) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return false;
        }

        String datePart = dateTimeStr.split(" ")[0];
        java.time.LocalDate parsedDate = parseDate(datePart);

        return parsedDate != null && String.valueOf(parsedDate.getYear()).equals(selectedYear);
    }

    // Helper method to parse dates with multiple formats
    private java.time.LocalDate parseDate(String datePart) {
        // Try multiple formats
        java.time.format.DateTimeFormatter[] formats = {
                java.time.format.DateTimeFormatter.ofPattern("MM/dd/yy"),
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                java.time.format.DateTimeFormatter.ofPattern("M/d/yy"), // in case of single-digit month/day
                java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy")
        };

        for (java.time.format.DateTimeFormatter fmt : formats) {
            try {
                return java.time.LocalDate.parse(datePart, fmt);
            } catch (java.time.format.DateTimeParseException ignored) {}
        }

        return null;
    }

    // Helper method to estimate cost based on ingredient type and quantity
    private double estimateCost(String ingredient, double quantity, String unit) {
        // These are estimated prices based on common market values
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

    // Method to update the chart with the loaded data
    private void updateChart() {
        System.out.println("=== updateChart() called ===");
        System.out.println("Monthly profit data to be displayed:");
        monthlyProfit.forEach((month, value) ->
                System.out.printf("Month: %s → Profit: ₱%.2f%n", month, value)
        );

        // Clear previous data
        lineChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Monthly Profits");

        // Ensure months are in correct order and use prettier display format
        String[] monthKeys = {"JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
                "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"};

        String[] displayMonths = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        for (int i = 0; i < monthKeys.length; i++) {
            String monthKey = monthKeys[i];
            String displayMonth = displayMonths[i];

            // Make sure we're using the right key format
            if (monthlyProfit.containsKey(monthKey)) {
                // Add data point to series with formatted month name
                Double profit = monthlyProfit.get(monthKey);
                series.getData().add(new XYChart.Data<>(displayMonth, profit));
                System.out.println("Added to chart: " + displayMonth + " → " + profit);
            } else {
                System.out.println("Warning: No profit data for month: " + monthKey);
                // Add zero for missing months
                series.getData().add(new XYChart.Data<>(displayMonth, 0.0));
            }
        }

        // Add series to chart
        lineChart.getData().add(series);

        // Add value labels and tooltips through Java since they can't be fully controlled via CSS
        addDataPointLabelsAndTooltips(series);
    }

    // Method to add value labels and tooltips to data points
    private void addDataPointLabelsAndTooltips(XYChart.Series<String, Number> series) {
        // Add tooltips and value labels for data points
        for (XYChart.Data<String, Number> data : series.getData()) {
            // Use a node property callback since JavaFX creates nodes asynchronously
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    // Add hover effect by swapping CSS classes
                    newNode.getStyleClass().add("chart-line-symbol");

                    newNode.setOnMouseEntered(e -> {
                        newNode.getStyleClass().add("chart-line-symbol-hover");
                    });

                    newNode.setOnMouseExited(e -> {
                        newNode.getStyleClass().remove("chart-line-symbol-hover");
                    });

                    // Add tooltip showing the exact value with proper formatting
                    java.text.NumberFormat currencyFormat = java.text.NumberFormat.getCurrencyInstance();
                    currencyFormat.setCurrency(java.util.Currency.getInstance("PHP"));
                    String formattedValue = currencyFormat.format(data.getYValue());

                    javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(
                            String.format("%s: %s", data.getXValue(), formattedValue)
                    );
                    javafx.scene.control.Tooltip.install(newNode, tooltip);

                    // Only add value labels for significant values
                    double y = ((Number) data.getYValue()).doubleValue();
                    if (Math.abs(y) > 1000) {
                        Text dataText = new Text(String.format("₱%,.0f", y));
                        dataText.getStyleClass().add("data-value-label");

                        // Position the text above the data point
                        javafx.scene.Group dataPointParent = (javafx.scene.Group) newNode.getParent();
                        dataPointParent.getChildren().add(dataText);

                        // Position text above the data point
                        dataText.setTranslateX(newNode.getLayoutX() - dataText.getBoundsInLocal().getWidth() / 2);
                        dataText.setTranslateY(newNode.getLayoutY() - 15);
                    }
                }
            });
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
        String userRole = LoginForm.getUserRole();
        String fxmlFile;
        String title = "Mr. Are Level Up Cuisine | Home Page";

        if ("Admin".equals(userRole)) {
            fxmlFile = "/view/homeFrame.fxml";
        } else if ("Staff".equals(userRole)) {
            fxmlFile = "/view/homeFrameStaff.fxml";
        } else {
            return;
        }

        openNewWindow(event, fxmlFile, title);
    }

    private void openNewWindow(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
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
    private void onIncomeBtnClick(ActionEvent event) {
        navigate(event, "3Income.fxml", "Mr. Are Level Up Cuisine | Weekly Sales Page");
    }

    @FXML
    private void onExpenseBtnClick(ActionEvent event) {
        navigate(event, "4Overview.fxml", "Mr. Are Level Up Cuisine | Overview Page");
    }

    @FXML
    private void onProfitBtnClick(ActionEvent event) {
        navigate(event, "2Profit.fxml", "Mr. Are Level Up Cuisine | Monthly Records Page");
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