package view;

import javafx.beans.property.*;

public class OrderDetails {

    private final StringProperty productName = new SimpleStringProperty();
    private final StringProperty size = new SimpleStringProperty();
    private final IntegerProperty quantity = new SimpleIntegerProperty();
    private final DoubleProperty subtotalPrice = new SimpleDoubleProperty();

    public OrderDetails(String productName, String size, int quantity, double subtotalPrice) {
        this.productName.set(productName);
        this.size.set(size);
        this.quantity.set(quantity);
        this.subtotalPrice.set(subtotalPrice);
    }

    // Getters
    public String getProductName() { return productName.get(); }
    public String getSize() { return size.get(); }
    public int getQuantity() { return quantity.get(); }
    public double getSubtotalPrice() { return subtotalPrice.get(); }

    // Properties for JavaFX TableView binding
    public StringProperty productNameProperty() { return productName; }
    public StringProperty sizeProperty() { return size; }
    public IntegerProperty quantityProperty() { return quantity; }
    public DoubleProperty subtotalPriceProperty() { return subtotalPrice; }
}
