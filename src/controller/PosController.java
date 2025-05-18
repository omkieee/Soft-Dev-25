package controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import view.LoginForm;
import view.OrderItem;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PosController {

    @FXML
    private AnchorPane menuSection;

    @FXML
    private VBox orderContainer;

    @FXML
    private Label total;

    @FXML
    private Button btnBack, btnAll, btnPork, btnChicken, btnBeef, btnSeafood;

    @FXML
    private Label dateTimeLabel;

    @FXML
    private TextField searchBox;


    private DoubleProperty totalPrice = new SimpleDoubleProperty(0.0);

    @FXML
    private void initialize() {
        loadDishes("All");
        startDateTimeUpdater();
        searchBox.textProperty().addListener((observable, oldValue, newValue) -> {
            filterDishesBySearch(newValue);
        });

        // Category filters
        btnAll.setOnAction(e -> loadDishes("All"));
        btnPork.setOnAction(e -> loadDishes("Pork"));
        btnChicken.setOnAction(e -> loadDishes("Chicken"));
        btnBeef.setOnAction(e -> loadDishes("Beef"));
    }

    private void filterDishesBySearch(String searchText) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT ProductName, Price, Size FROM Dish WHERE ProductName LIKE ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, "%" + searchText + "%");

            ResultSet rs = stmt.executeQuery();
            FlowPane dishContainer = new FlowPane(10, 10);
            dishContainer.setPrefWrapLength(1384);

            while (rs.next()) {
                String productName = rs.getString("ProductName");
                double price = rs.getDouble("Price");
                String size = rs.getString("Size");

                // Determine availability based on ingredients
                boolean inStock = true;
                PreparedStatement recipeStmt = conn.prepareStatement(
                        "SELECT r.IngredientsName, r.Quantity, i.Quantity AS Stock " +
                                "FROM Recipes r JOIN Ingredients i ON r.IngredientsName = i.IngredientsName " +
                                "WHERE r.ProductName = ?"
                );
                recipeStmt.setString(1, productName);
                ResultSet recipeRs = recipeStmt.executeQuery();

                while (recipeRs.next()) {
                    double stock = recipeRs.getDouble("Stock");
                    if (stock < 1000) {
                        inStock = false;
                        break;
                    }
                }

                Dish dish = new Dish(productName, price); // Servings not used
                dish.setSize(size);
                dish.setStatus(inStock ? "In Stock" : "Out of Stock");

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DishCard.fxml"));
                AnchorPane dishCard = loader.load();
                DishCardController controller = loader.getController();
                controller.setDishData(dish, orderContainer);
                controller.setPosController(this);

                dishContainer.getChildren().add(dishCard);
            }

            menuSection.getChildren().clear();
            menuSection.getChildren().add(dishContainer);

        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    private String checkDishStockStatus(Connection conn, String productName) throws SQLException {
        String recipeQuery = "SELECT IngredientsName, Quantity FROM Recipes WHERE ProductName = ?";
        try (PreparedStatement stmt = conn.prepareStatement(recipeQuery)) {
            stmt.setString(1, productName);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String ingredient = rs.getString("IngredientsName");

                String ingredientStockQuery = "SELECT Quantity FROM Ingredients WHERE IngredientsName = ?";
                try (PreparedStatement ingredientStmt = conn.prepareStatement(ingredientStockQuery)) {
                    ingredientStmt.setString(1, ingredient);
                    ResultSet stockRs = ingredientStmt.executeQuery();

                    if (stockRs.next()) {
                        double available = stockRs.getDouble("Quantity");
                        if (available < 1000.0) return "Out Of Stock";
                    }
                }
            }
        }
        return "In Stock";
    }



    private void startDateTimeUpdater() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            dateTimeLabel.setText(LocalDateTime.now().format(formatter));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    public void updateTotalPrice() {
        double sum = 0;
        for (Node node : orderContainer.getChildren()) {
            if (node instanceof AnchorPane) {
                OrderCardController c = (OrderCardController) node.getProperties().get("controller");
                if (c != null) sum += c.getSubtotal();
            }
        }
        total.setText("₱" + String.format("%.2f", sum));
        totalPrice.set(sum); // updates totalLabel as well
    }

    public double calculateTotalPrice() {
        double sum = 0;
        for (Node node : orderContainer.getChildren()) {
            if (node instanceof AnchorPane) {
                OrderCardController c = (OrderCardController) node.getProperties().get("controller");
                if (c != null) sum += c.getSubtotal();
            }
        }
        return sum;
    }

    private void loadDishes(String category) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = category == null || category.equalsIgnoreCase("All")
                    ? "SELECT ProductName, Price, Size FROM Dish"
                    : "SELECT ProductName, Price, Size FROM Dish WHERE Category = ?";

            PreparedStatement stmt = conn.prepareStatement(query);
            if (category != null && !category.equalsIgnoreCase("All")) {
                stmt.setString(1, category);
            }

            ResultSet rs = stmt.executeQuery();
            FlowPane dishContainer = new FlowPane(10, 10);
            dishContainer.setPrefWrapLength(1384);

            while (rs.next()) {
                String productName = rs.getString("ProductName");
                double price = rs.getDouble("Price");
                String size = rs.getString("Size");

                Dish dish = new Dish(productName, price);
                dish.setSize(size);

                // NEW: Determine stock status based on ingredients
                dish.setStatus(checkDishStockStatus(conn, productName));

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DishCard.fxml"));
                AnchorPane dishCard = loader.load();
                DishCardController controller = loader.getController();
                controller.setDishData(dish, orderContainer);
                controller.setPosController(this);

                dishContainer.getChildren().add(dishCard);
            }

            menuSection.getChildren().clear();
            menuSection.getChildren().add(dishContainer);

        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    private String getUnitTypeForIngredient(Connection conn, String ingredientName) throws SQLException {
        String query = "SELECT UnitType FROM Ingredients WHERE IngredientsName = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, ingredientName);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString("UnitType") : "";
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
    private void onOrderBtnClick(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/OrderHistory.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.setTitle("Recent Orders");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSaveOrder() {
        try (Connection conn = DatabaseConnection.getConnection()) {

            // 1. Insert into Orders table
            String insertOrderSql = "INSERT INTO Orders (OrderDate, TotalPrice, Username) VALUES (?, ?, ?)";
            PreparedStatement orderStmt = conn.prepareStatement(insertOrderSql, new String[]{"OrderID"});
            Timestamp now = new Timestamp(System.currentTimeMillis());
            orderStmt.setTimestamp(1, now);
            orderStmt.setDouble(2, calculateTotalPrice());
            orderStmt.setString(3, LoginForm.getUserName());
            orderStmt.executeUpdate();

            // 2. Get generated OrderID
            ResultSet generatedKeys = orderStmt.getGeneratedKeys();
            if (!generatedKeys.next()) {
                throw new SQLException("Creating order failed, no ID obtained.");
            }
            int orderId = generatedKeys.getInt(1);

            // 3. Prepare statements
            PreparedStatement itemStmt = conn.prepareStatement(
                    "INSERT INTO OrdersItem (OrderID, ProductName, Quantity, SubtotalPrice) VALUES (?, ?, ?, ?)"
            );
            PreparedStatement updateDishStmt = conn.prepareStatement(
                    "UPDATE Dish SET Servings = Servings - ? WHERE ProductName = ?"
            );
            PreparedStatement recipeStmt = conn.prepareStatement(
                    "SELECT IngredientsName, Quantity FROM Recipes WHERE ProductName = ?"
            );
            PreparedStatement updateIngredientStmt = conn.prepareStatement(
                    "UPDATE Ingredients SET Quantity = Quantity - ? WHERE IngredientsName = ?"
            );
            PreparedStatement transactionStmt = conn.prepareStatement(
                    "INSERT INTO Transactions (Username, IngredientsName, Quantity, UnitType, DateTime, TotalCost, Type) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)"
            );

            List<OrderItem> itemList = FXCollections.observableArrayList();

            // 4. Process each item in order
            for (Node node : orderContainer.getChildren()) {
                if (!(node instanceof AnchorPane)) continue;
                OrderCardController controller = (OrderCardController) node.getProperties().get("controller");
                if (controller == null) continue;

                String productName = controller.getOrderDishName();
                int qtyOrdered = controller.getQuantity();
                double subtotalPrice = controller.getSubtotal();
                String size = controller.getDishSize(); // assuming you added this method

                // Insert item into OrdersItem
                itemStmt.setInt(1, orderId);
                itemStmt.setString(2, productName);
                itemStmt.setInt(3, qtyOrdered);
                itemStmt.setDouble(4, subtotalPrice);
                itemStmt.executeUpdate();

                // Update Dish servings
                updateDishStmt.setInt(1, qtyOrdered);
                updateDishStmt.setString(2, productName);
                updateDishStmt.executeUpdate();

                // Add to list for popup
                itemList.add(new OrderItem(productName, size, qtyOrdered, "₱" + String.format("%.2f", subtotalPrice)));

                // Process ingredients
                recipeStmt.setString(1, productName);
                ResultSet recipeRs = recipeStmt.executeQuery();
                while (recipeRs.next()) {
                    String ingredientName = recipeRs.getString("IngredientsName");
                    double qtyPerServing = recipeRs.getDouble("Quantity");
                    double totalToDeduct = qtyPerServing * qtyOrdered;

                    // Update stock
                    updateIngredientStmt.setDouble(1, totalToDeduct);
                    updateIngredientStmt.setString(2, ingredientName);
                    updateIngredientStmt.executeUpdate();

                    // Log transaction
                    transactionStmt.setString(1, LoginForm.getUserName());
                    transactionStmt.setString(2, ingredientName);
                    transactionStmt.setDouble(3, totalToDeduct);
                    transactionStmt.setString(4, getUnitTypeForIngredient(conn, ingredientName));
                    transactionStmt.setTimestamp(5, now);
                    transactionStmt.setDouble(6, 0.00);
                    transactionStmt.setString(7, "POS");
                    transactionStmt.executeUpdate();
                }
            }

            // 5. Show notification popup
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/PosNotification.fxml"));
            AnchorPane popupRoot = loader.load();
            PosNotificationController notif = loader.getController();
            notif.loadOrderSummary(orderId, now, calculateTotalPrice(), itemList);
            notif.setOnOkAction(this::resetPOS);

            Stage popupStage = new Stage();
            popupStage.setTitle("Notification");
            popupStage.setScene(new Scene(popupRoot));
            popupStage.setResizable(false);
            popupStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetPOS(Void unused) {
        orderContainer.getChildren().clear();
        total.setText("₱0.00");
        totalPrice.set(0.0);
    }

    private void resetPOS() {
        orderContainer.getChildren().clear();
        total.setText("₱0.00");
        totalPrice.set(0.0);
    }
}
