package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import view.OrderItem;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class PosNotificationController {

    @FXML
    private Label orderID;

    @FXML
    private Label orderDate;

    @FXML
    private Label totalCost;

    @FXML
    private TableView<OrderItem> orderItems;

    @FXML
    private TableColumn<OrderItem, String> dishColumn;

    @FXML
    private TableColumn<OrderItem, String> sizeColumn;

    @FXML
    private TableColumn<OrderItem, Integer> quantityColumn;

    @FXML
    private TableColumn<OrderItem, String> priceColumn;

    private Consumer<Void> onOkAction;

    public void setOnOkAction(Consumer<Void> action) {
        this.onOkAction = action;
    }

    public void handleOk() {
        if (onOkAction != null) {
            onOkAction.accept(null);
        }
        ((Stage) orderID.getScene().getWindow()).close();
    }

    public void loadOrderSummary(int orderId, Timestamp timestamp, double total, List<OrderItem> items) {
        // Set values
        orderID.setText(String.valueOf(orderId));
        orderDate.setText(timestamp.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        totalCost.setText("₱" + String.format("%.2f", total));

        // Set up table columns
        dishColumn.setCellValueFactory(data -> data.getValue().dishNameProperty());
        sizeColumn.setCellValueFactory(data -> data.getValue().sizeProperty());
        quantityColumn.setCellValueFactory(data -> data.getValue().quantityProperty().asObject());
        priceColumn.setCellValueFactory(data -> data.getValue().priceProperty());

        // Load data
        ObservableList<OrderItem> observableItems = FXCollections.observableArrayList(items);
        orderItems.setItems(observableItems);
    }
}
