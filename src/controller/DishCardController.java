package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import controller.Dish;

import java.io.IOException;

public class DishCardController {

    @FXML
    private Label dishName;

    @FXML
    private Label status;

    @FXML
    private Label price;

    @FXML
    private Label size;

    @FXML
    private Button btnadd;

    private Dish dish;
    private VBox orderContainer;
    private PosController posController;

    @FXML
    private Label statusLabel;


    public void setDishData(Dish dish, VBox orderContainer) {
        dishName.setText(dish.getName());
        size.setText(dish.getSize());
        price.setText("₱" + String.format("%.2f", dish.getPrice()));
        this.orderContainer = orderContainer;
        this.dish = dish;
        status.setText(dish.getStatus());

        if ("Out of Stock".equalsIgnoreCase(dish.getStatus())) {
            status.setStyle("-fx-text-fill: #d50000;");
        }

    }


    public void setPosController(PosController posController) {
        this.posController = posController;
    }

    @FXML
    private void handleAddButton() {
        if ("Out of Stock".equalsIgnoreCase(dish.getStatus())) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Out of Stock");
            alert.setHeaderText(null);
            alert.setContentText("This dish is currently out of stock and cannot be added.");
            alert.showAndWait();
            return;
        }

        try {
            for (javafx.scene.Node node : orderContainer.getChildren()) {
                if (node instanceof AnchorPane) {
                    OrderCardController existingController = (OrderCardController) node.getProperties().get("controller");
                    if (existingController != null && existingController.getOrderDishName().equals(dish.getName())) {
                        existingController.incrementQuantity();
                        posController.updateTotalPrice();
                        return;
                    }
                }
            }

            // If the dish does not exist in the order, create a new order card
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/OrderCard.fxml"));
            AnchorPane newOrderCard = loader.load();

            OrderCardController controller = loader.getController();
            controller.setOrderDetails(dish.getName(), dish.getSize(), dish.getPrice(), 1);  // Set quantity to 1
            controller.setParent(orderContainer, newOrderCard);
            controller.setPosController(posController);

            newOrderCard.getProperties().put("controller", controller);
            orderContainer.getChildren().add(newOrderCard);

            posController.updateTotalPrice();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
