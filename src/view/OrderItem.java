// view/OrderItem.java

package view;

import javafx.beans.property.*;

public class OrderItem {
    private final StringProperty dishName = new SimpleStringProperty();
    private final StringProperty size = new SimpleStringProperty();
    private final IntegerProperty quantity = new SimpleIntegerProperty();
    private final StringProperty price = new SimpleStringProperty();

    public OrderItem(String dishName, String size, int quantity, String price) {
        this.dishName.set(dishName);
        this.size.set(size);
        this.quantity.set(quantity);
        this.price.set(price);
    }

    public StringProperty dishNameProperty() { return dishName; }
    public StringProperty sizeProperty() { return size; }
    public IntegerProperty quantityProperty() { return quantity; }
    public StringProperty priceProperty() { return price; }

    public String getDishName() { return dishName.get(); }
    public String getSize() { return size.get(); }
    public int getQuantity() { return quantity.get(); }
    public String getPrice() { return price.get(); }
}
