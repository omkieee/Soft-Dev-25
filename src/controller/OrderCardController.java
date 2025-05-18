package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

public class OrderCardController {

    @FXML
    private Label orderDishName;

    @FXML
    private Label orderDishPrice;

    @FXML
    private Label orderQuantity;

    @FXML
    private Label orderDishSize;

    private VBox orderContainer;
    private AnchorPane selfCard;
    private PosController posController;

    public void setOrderDetails(String name, String size, double price, int quantity) {
        orderDishName.setText(name);
        orderDishSize.setText(size);
        orderDishPrice.setText("₱" + String.format("%.2f", price));
        orderQuantity.setText(String.valueOf(quantity));
    }

    public void setParent(VBox container, AnchorPane selfCard) {
        this.orderContainer = container;
        this.selfCard = selfCard;
    }

    public void setPosController(PosController posController) {
        this.posController = posController;
    }

    public String getOrderDishName() {
        return orderDishName.getText();
    }

    public void incrementQuantity() {
        int qty = Integer.parseInt(orderQuantity.getText());
        orderQuantity.setText(String.valueOf(qty + 1));
        posController.updateTotalPrice();
    }

    @FXML
    private void handleRemoveButton() {
        int currentQty = Integer.parseInt(orderQuantity.getText());

        if (currentQty > 1) {
            orderQuantity.setText(String.valueOf(currentQty - 1));
            posController.updateTotalPrice();
        } else {
            orderContainer.getChildren().remove(selfCard); // Remove card if qty is 1
            posController.updateTotalPrice();
        }
    }

    public double getSubtotal() {
        int qty = Integer.parseInt(orderQuantity.getText());
        double price = Double.parseDouble(orderDishPrice.getText().replace("₱", ""));
        return qty * price;
    }

    public int getQuantity() {
        return Integer.parseInt(orderQuantity.getText());
    }

    public double getPrice() {
        return Double.parseDouble(orderDishPrice.getText().replace("₱", ""));
    }

    public String getDishSize() {
        if (orderDishSize != null) {
            return orderDishSize.getText();
        }
        return "";  // Or some default value if orderDishSize is null
    }


}
