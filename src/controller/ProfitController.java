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
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import view.LoginForm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static controller.LoginController.userRole;

public class ProfitController {
    @FXML private TableView<ProfitRecord> profitTableView;
    @FXML private TableColumn<ProfitRecord, String> monthColumn;
    @FXML private TableColumn<ProfitRecord, Double> incomeColumn;
    @FXML private TableColumn<ProfitRecord, Double> expenseColumn;
    @FXML private TableColumn<ProfitRecord, Double> profitColumn;
    @FXML private TextField totalProfitField;
    @FXML private ComboBox<Integer> yearComboBox; // Changed from TextField to ComboBox for better UX

    private final Map<String, Double> monthlyIncome = new HashMap<>();
    private final Map<String, Double> monthlyExpenses = new HashMap<>();
    private double totalIncome = 0.0;
    private double totalExpenses = 0.0;
    private int selectedYear;

    @FXML
    private Button homeBtn;
    @FXML
    private Button IncomeBtn;
    @FXML
    private Button ExpenseBtn;
    @FXML
    private Button ProfitBtn;

    private List<Button> navButtons;

    @FXML
    public void initialize() {
        // Set up table columns
        monthColumn.setCellValueFactory(new PropertyValueFactory<>("month"));
        incomeColumn.setCellValueFactory(new PropertyValueFactory<>("income"));
        expenseColumn.setCellValueFactory(new PropertyValueFactory<>("expense"));
        profitColumn.setCellValueFactory(new PropertyValueFactory<>("profit"));

        // Format currency columns
        configureCurrencyColumn(incomeColumn);
        configureCurrencyColumn(expenseColumn);
        configureCurrencyColumn(profitColumn);

        // Initialize year selection
        initializeYearComboBox();

        // Load and display data
        loadData();

        navButtons = new ArrayList<>();
        navButtons.add(homeBtn);
        navButtons.add(IncomeBtn);
        navButtons.add(ExpenseBtn);
        navButtons.add(ProfitBtn);
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
            loadData();
        });
    }

    private void configureCurrencyColumn(TableColumn<ProfitRecord, Double> column) {
        column.setCellFactory(tc -> new TableCell<ProfitRecord, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                setText(empty || amount == null ? null : String.format("₱%,.2f", amount));
            }
        });
    }

    @FXML
    public void refreshData() {
        loadData();
    }

    private void loadData() {
        loadIncomeData();
        loadExpenseData();
        calculateProfit();
    }

    private void loadIncomeData() {
        monthlyIncome.clear();
        totalIncome = 0.0;

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                totalProfitField.setText("Database connection failed");
                return;
            }

            // Modified query to filter by year
            String query = "SELECT OrderID, OrderDate, TotalPrice FROM Orders WHERE OrderID IS NOT NULL " +
                    "AND Year(OrderDate) = ?\n";

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, String.valueOf(selectedYear));

                try (ResultSet rs = stmt.executeQuery()) {
                    // Initialize all months with 0 income
                    for (Month month : Month.values()) {
                        monthlyIncome.put(month.toString(), 0.0);
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

                                String monthName = Month.of(orderDate.toLocalDate().getMonthValue()).toString();
                                monthlyIncome.merge(monthName, price, Double::sum);
                                totalIncome += price;
                            }
                        } catch (SQLException e) {
                            // Handle date parsing errors
                            System.err.println("Error processing OrderDate for OrderID: " + orderId);
                            System.err.println(e.getMessage());
                        }
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            totalProfitField.setText("Error loading income data: " + e.getMessage());
        }
    }

    private void loadExpenseData() {
        monthlyExpenses.clear();
        totalExpenses = 0.0;

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                totalProfitField.setText("Database connection failed");
                return;
            }

            // Modified query to filter by year
            String query = "SELECT IngredientsName, Quantity, UnitType, DateTime, TotalCost " +
                    "FROM Transactions " +
                    "WHERE IngredientsName IS NOT NULL";

            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                for (Month month : Month.values()) {
                    monthlyExpenses.put(month.toString(), 0.0);
                }

                while (rs.next()) {
                    try {
                        String ingredient = rs.getString("IngredientsName");
                        if (ingredient == null || ingredient.trim().isEmpty()) continue;

                        double quantity = rs.getDouble("Quantity");
                        String unit = rs.getString("UnitType");
                        String dateTimeStr = rs.getString("DateTime");

                        // Skip if no date or wrong year
                        if (dateTimeStr == null || !isFromSelectedYear(dateTimeStr)) {
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
                                cost = estimateCost(ingredient, quantity, unit);
                            }
                        } else {
                            cost = estimateCost(ingredient, quantity, unit);
                        }

                        // Default to JANUARY if date can't be parsed
                        String monthName = "JANUARY";

                        if (dateTimeStr != null && !dateTimeStr.trim().isEmpty()) {
                            String datePart = dateTimeStr.split(" ")[0];
                            LocalDate parsedDate = parseDate(datePart);

                            if (parsedDate != null) {
                                monthName = parsedDate.getMonth().toString();
                            } else {
                                System.err.println("Error parsing DateTime: " + dateTimeStr);
                            }
                        }

                        monthlyExpenses.merge(monthName, cost, Double::sum);
                        totalExpenses += cost;

                    } catch (SQLException e) {
                        System.err.println("Error processing expense record: " + e.getMessage());
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            totalProfitField.setText("Error loading expense data: " + e.getMessage());
        }
    }

    // Helper method to check if a date string is from the selected year
    private boolean isFromSelectedYear(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return false;
        }

        String datePart = dateTimeStr.split(" ")[0];
        LocalDate parsedDate = parseDate(datePart);

        return parsedDate != null && parsedDate.getYear() == selectedYear;
    }

    // Helper method to parse dates with multiple formats
    private LocalDate parseDate(String datePart) {
        // Try multiple formats
        DateTimeFormatter[] formats = {
                DateTimeFormatter.ofPattern("MM/dd/yy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("M/d/yy"), // in case of single-digit month/day
                DateTimeFormatter.ofPattern("MM/dd/yyyy")
        };

        for (DateTimeFormatter fmt : formats) {
            try {
                return LocalDate.parse(datePart, fmt);
            } catch (DateTimeParseException ignored) {}
        }

        return null;
    }

    // Helper method to estimate cost based on ingredient type and quantity
    // Reused from ExpenseController to maintain consistency
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

    private void calculateProfit() {
        ObservableList<ProfitRecord> profitData = FXCollections.observableArrayList();
        double totalProfit = 0.0;

        // Create profit records for each month
        for (Month month : Month.values()) {
            String monthName = month.toString();
            double income = monthlyIncome.getOrDefault(monthName, 0.0);
            double expense = monthlyExpenses.getOrDefault(monthName, 0.0);
            double profit = income - expense;

            profitData.add(new ProfitRecord(monthName, income, expense, profit));
            totalProfit += profit;
        }

        // Update UI
        profitTableView.setItems(profitData);
        totalProfitField.setText(String.format("₱%,.2f", totalProfit));

        cacheCalculatedProfitData();

    }

    private void cacheCalculatedProfitData() {
        Map<String, Double> profitMap = new HashMap<>();

        // Extract profit values from table items
        for (ProfitRecord record : profitTableView.getItems()) {
            profitMap.put(record.getMonth(), record.getProfit());
        }

        // Store in the cache
        ProfitDataCache.storeProfitData(selectedYear, profitMap);
    }

    @FXML
    public void exportToExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Profit Report");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );
        fileChooser.setInitialFileName("Profit_Report_" + selectedYear + ".xlsx");

        File file = fileChooser.showSaveDialog(profitTableView.getScene().getWindow());
        if (file != null) {
            try {
                createExcelReport(file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText("Profit report has been exported successfully to:\n" + file.getAbsolutePath());
                alert.showAndWait();
            } catch (IOException e) {
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
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create sheet
            Sheet sheet = workbook.createSheet("Profit Report");

            // Create header row styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Create title row and add title
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Profit Report for Year " + selectedYear);
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));

            // Create headers (row 2)
            Row headerRow = sheet.createRow(2);
            String[] headers = {"Month", "Income (₱)", "Expense (₱)", "Profit (₱)"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create currency style
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("₱#,##0.00"));
            currencyStyle.setBorderBottom(BorderStyle.THIN);
            currencyStyle.setBorderTop(BorderStyle.THIN);
            currencyStyle.setBorderLeft(BorderStyle.THIN);
            currencyStyle.setBorderRight(BorderStyle.THIN);

            // Create normal cell style
            CellStyle normalStyle = workbook.createCellStyle();
            normalStyle.setBorderBottom(BorderStyle.THIN);
            normalStyle.setBorderTop(BorderStyle.THIN);
            normalStyle.setBorderLeft(BorderStyle.THIN);
            normalStyle.setBorderRight(BorderStyle.THIN);

            // Add data rows
            ObservableList<ProfitRecord> data = profitTableView.getItems();
            int rowNum = 3;
            double totalIncome = 0, totalExpense = 0, totalProfit = 0;

            for (ProfitRecord record : data) {
                Row row = sheet.createRow(rowNum++);

                Cell monthCell = row.createCell(0);
                monthCell.setCellValue(record.getMonth());
                monthCell.setCellStyle(normalStyle);

                Cell incomeCell = row.createCell(1);
                incomeCell.setCellValue(record.getIncome());
                incomeCell.setCellStyle(currencyStyle);
                totalIncome += record.getIncome();

                Cell expenseCell = row.createCell(2);
                expenseCell.setCellValue(record.getExpense());
                expenseCell.setCellStyle(currencyStyle);
                totalExpense += record.getExpense();

                Cell profitCell = row.createCell(3);
                profitCell.setCellValue(record.getProfit());
                profitCell.setCellStyle(currencyStyle);
                totalProfit += record.getProfit();
            }

            // Add total row
            Row totalRow = sheet.createRow(rowNum);

            CellStyle totalLabelStyle = workbook.createCellStyle();
            totalLabelStyle.setFont(headerFont);
            totalLabelStyle.setBorderBottom(BorderStyle.THIN);
            totalLabelStyle.setBorderTop(BorderStyle.THIN);
            totalLabelStyle.setBorderLeft(BorderStyle.THIN);
            totalLabelStyle.setBorderRight(BorderStyle.THIN);

            Cell totalLabelCell = totalRow.createCell(0);
            totalLabelCell.setCellValue("TOTAL");
            totalLabelCell.setCellStyle(totalLabelStyle);

            CellStyle totalValueStyle = workbook.createCellStyle();
            totalValueStyle.setFont(headerFont);
            totalValueStyle.setDataFormat(format.getFormat("₱#,##0.00"));
            totalValueStyle.setBorderBottom(BorderStyle.THIN);
            totalValueStyle.setBorderTop(BorderStyle.THIN);
            totalValueStyle.setBorderLeft(BorderStyle.THIN);
            totalValueStyle.setBorderRight(BorderStyle.THIN);

            Cell totalIncomeCell = totalRow.createCell(1);
            totalIncomeCell.setCellValue(totalIncome);
            totalIncomeCell.setCellStyle(totalValueStyle);

            Cell totalExpenseCell = totalRow.createCell(2);
            totalExpenseCell.setCellValue(totalExpense);
            totalExpenseCell.setCellStyle(totalValueStyle);

            Cell totalProfitCell = totalRow.createCell(3);
            totalProfitCell.setCellValue(totalProfit);
            totalProfitCell.setCellStyle(totalValueStyle);

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to file
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                workbook.write(outputStream);
            }
        }
    }

    // Model class for table data
    public static class ProfitRecord {
        private final String month;
        private final double income;
        private final double expense;
        private final double profit;

        public ProfitRecord(String month, double income, double expense, double profit) {
            this.month = month;
            this.income = income;
            this.expense = expense;
            this.profit = profit;
        }

        public String getMonth() { return month; }
        public double getIncome() { return income; }
        public double getExpense() { return expense; }
        public double getProfit() { return profit; }
    }


}