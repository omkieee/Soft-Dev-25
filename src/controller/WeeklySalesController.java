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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.Date;
import java.util.Locale;

import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import view.LoginForm;

import java.io.FileOutputStream;


public class WeeklySalesController {
    @FXML private TableView<WeeklySalesRecord> salesTable;
    @FXML private TableColumn<WeeklySalesRecord, String> weekColumn;
    @FXML private TableColumn<WeeklySalesRecord, String> dateRangeColumn;
    @FXML private TableColumn<WeeklySalesRecord, Double> amountColumn;
    @FXML private TextField totalSalesField;
    @FXML private ComboBox<Integer> yearComboBox;

    private final Map<Integer, Double> weeklySales = new HashMap<>();
    private final Map<Integer, String> weekRanges = new HashMap<>();
    private int selectedYear;
    private double totalSales = 0.0;

    @FXML
    private Button homeBtn;
    @FXML
    private Button IncomeBtn;
    @FXML
    private Button ExpenseBtn;
    @FXML
    private Button ProfitBtn;

    private List<Button> navButtons;

    private final WeekFields weekFields = WeekFields.of(Locale.getDefault());

    public void initialize() {
        // Set up table columns
        weekColumn.setCellValueFactory(new PropertyValueFactory<>("weekNumber"));
        dateRangeColumn.setCellValueFactory(new PropertyValueFactory<>("dateRange"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));

        // Format amount column as currency
        amountColumn.setCellFactory(tc -> new javafx.scene.control.TableCell<WeeklySalesRecord, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                setText(empty || amount == null ? null : String.format("₱%,.2f", amount));
            }
        });

        // Initialize year selection
        initializeYearComboBox();

        // Load initial data
        loadSalesData();

        navButtons = new ArrayList<>();
        navButtons.add(homeBtn);
        navButtons.add(IncomeBtn);
        navButtons.add(ExpenseBtn);
        navButtons.add(ProfitBtn);
    }

    private void initializeYearComboBox() {
        // Populate the year dropdown with reasonable range (past 5 years to current year)
        int currentYear = Year.now().getValue();
        ObservableList<Integer> years = FXCollections.observableArrayList();
        for (int year = currentYear - 5; year <= currentYear; year++) {
            years.add(year);
        }
        yearComboBox.setItems(years);
        yearComboBox.setValue(currentYear); // Default to current year
        selectedYear = currentYear;

        // Add change listener to reload data when year changes
        yearComboBox.setOnAction(event -> {
            selectedYear = yearComboBox.getValue();
            loadSalesData();
        });
    }

    @FXML
    public void refreshData() {
        loadSalesData();
    }

    @FXML
    public void exportToExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Weekly Sales Report");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );
        fileChooser.setInitialFileName("Weekly_Sales_Report_" + selectedYear + ".xlsx");

        File file = fileChooser.showSaveDialog(salesTable.getScene().getWindow());
        if (file != null) {
            try {

                createExcelReport(file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText("Weekly sales report has been exported successfully to:\n" + file.getAbsolutePath());
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

    private void loadSalesData() {
        weeklySales.clear();
        weekRanges.clear();
        totalSales = 0.0;

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                totalSalesField.setText("Database connection failed");
                return;
            }

            // Query sales data with year filter
            String query = "SELECT OrderID, OrderDate, TotalPrice FROM Orders " +
                    "WHERE OrderID IS NOT NULL AND Year(OrderDate) = ?";

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, String.valueOf(selectedYear));

                try (ResultSet rs = stmt.executeQuery()) {
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
                                    System.err.println("Error parsing TotalPrice for OrderID: " + orderId);
                                    System.err.println(e.getMessage());
                                    continue;
                                }

                                LocalDate localDate = ((java.sql.Date) orderDate).toLocalDate();

                                // Only process records from the selected year
                                if (localDate.getYear() == selectedYear) {
                                    // Get week number (1-53)
                                    int weekNumber = localDate.get(weekFields.weekOfWeekBasedYear());

                                    // Calculate week date range if we haven't already
                                    if (!weekRanges.containsKey(weekNumber)) {
                                        weekRanges.put(weekNumber, calculateWeekRange(localDate));
                                    }

                                    // Add to weekly sales
                                    weeklySales.merge(weekNumber, price, Double::sum);
                                    totalSales += price;
                                }
                            }
                        } catch (SQLException e) {
                            System.err.println("Error processing OrderDate for OrderID: " + orderId);
                            System.err.println(e.getMessage());
                        }
                    }
                }
            }

            // Update UI with loaded data
            updateSalesTable();

        } catch (SQLException e) {
            e.printStackTrace();
            totalSalesField.setText("Error loading data: " + e.getMessage());
        }
    }

    private String calculateWeekRange(LocalDate date) {
        // Find first day of the week
        LocalDate firstDay = date.with(DayOfWeek.MONDAY);

        // Find last day of the week
        LocalDate lastDay = date.with(DayOfWeek.SUNDAY);

        // Format dates as "MMM d" (Jan 1)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d");
        return firstDay.format(formatter) + " - " + lastDay.format(formatter);
    }

    private void updateSalesTable() {
        ObservableList<WeeklySalesRecord> salesData = FXCollections.observableArrayList();

        // Create records for each week
        for (int week = 1; week <= 53; week++) { // Weeks 1-53 in a year
            Double amount = weeklySales.getOrDefault(week, 0.0);
            String dateRange = weekRanges.getOrDefault(week, "");

            // Only add weeks that have sales or date ranges
            if (amount > 0 || !dateRange.isEmpty()) {
                salesData.add(new WeeklySalesRecord("Week " + week, dateRange, amount));
            }
        }

        // Sort by week number
        salesData.sort(Comparator.comparing(WeeklySalesRecord::getWeekNumber));

        // Update UI
        salesTable.setItems(salesData);
        totalSalesField.setText(String.format("₱%,.2f", totalSales));
    }

    private void createExcelReport(File file) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Weekly Sales Report");

        // Create header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Week");
        headerRow.createCell(1).setCellValue("Date Range");
        headerRow.createCell(2).setCellValue("Sales Amount");

        // Get data from the table
        ObservableList<WeeklySalesRecord> data = salesTable.getItems();
        for (int i = 0; i < data.size(); i++) {
            WeeklySalesRecord record = data.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(record.getWeekNumber());
            row.createCell(1).setCellValue(record.getDateRange());
            row.createCell(2).setCellValue(record.getAmount());
        }

        // Auto-size columns
        for (int i = 0; i < 3; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write to file
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            workbook.write(outputStream);
        }

        workbook.close();
    }



    // Model class for table data
    public static class WeeklySalesRecord {
        private final String weekNumber;
        private final String dateRange;
        private final double amount;

        public WeeklySalesRecord(String weekNumber, String dateRange, double amount) {
            this.weekNumber = weekNumber;
            this.dateRange = dateRange;
            this.amount = amount;
        }

        public String getWeekNumber() { return weekNumber; }
        public String getDateRange() { return dateRange; }
        public double getAmount() { return amount; }
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