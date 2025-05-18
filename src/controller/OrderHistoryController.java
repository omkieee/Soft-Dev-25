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
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import view.Order;
import view.OrderDetails;
import controller.DatabaseConnection;

import java.io.IOException;
import java.sql.*;

public class OrderHistoryController {

    @FXML
    private TableView<Order> orderTable;
    @FXML
    private TableColumn<Order, Integer> idColumn;
    @FXML
    private TableColumn<Order, String> dateColumn;
    @FXML
    private TableColumn<Order, Double> totalColumn;
    @FXML
    private TableColumn<Order, Void> viewOrderColumn;

    @FXML
    private TableView<OrderDetails> itemsTable;
    @FXML
    private TableColumn<OrderDetails, String> nameColumn;
    @FXML
    private TableColumn<OrderDetails, String> sizeColumn;
    @FXML
    private TableColumn<OrderDetails, Integer> quantityColumn;
    @FXML
    private TableColumn<OrderDetails, Double> priceColumn;

    @FXML
    private Label orderID;
    @FXML
    private Label totalPrice;

    @FXML
    public void initialize() {
        setupOrderTable();
        loadOrders();
    }

    @FXML
    private void onPosBtnClick(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/Pos.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.setTitle("Mr. Are Level Up Cuisine | Pos Page");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupOrderTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));

        addViewOrderButton();
    }

    private void addViewOrderButton() {
        viewOrderColumn.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("View Order");

            {
                btn.getStyleClass().add("navButton2");
                btn.setOnAction(event -> {
                    Order selectedOrder = getTableView().getItems().get(getIndex());
                    loadOrderItems(selectedOrder.getOrderId());
                    orderID.setText("Order ID: " + selectedOrder.getOrderId());
                    totalPrice.setText("Total Price: P" + selectedOrder.getTotalPrice());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                }
            }
        });
    }

    private void loadOrders() {
        ObservableList<Order> orderList = FXCollections.observableArrayList();

        String sql = "SELECT * FROM orders ORDER BY OrderDate DESC"; // or OrderID DESC

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                orderList.add(new Order(
                        rs.getInt("OrderID"),
                        rs.getString("OrderDate"),
                        rs.getDouble("TotalPrice")
                ));
            }

            orderTable.setItems(orderList);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadOrderItems(int orderId) {
        ObservableList<OrderDetails> itemList = FXCollections.observableArrayList();

        String sql = "SELECT * FROM ordersItem WHERE OrderID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                itemList.add(new OrderDetails(
                        rs.getString("ProductName"),
                        rs.getString("Size"),
                        rs.getInt("Quantity"),
                        rs.getDouble("SubtotalPrice")
                ));
            }

            nameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
            sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
            quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
            priceColumn.setCellValueFactory(new PropertyValueFactory<>("subtotalPrice"));


            itemsTable.setItems(itemList);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

