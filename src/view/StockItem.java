package view;

import javafx.beans.property.*;

public class StockItem {
    private final StringProperty name;
    private final StringProperty category;
    private final IntegerProperty quantity;
    private final StringProperty unit;
    private final StringProperty updates;
    private final DoubleProperty price;

    public StockItem(String name, String category, int quantity, String unit, String updates, double price) {
        this.name = new SimpleStringProperty(name);
        this.category = new SimpleStringProperty(category);
        this.quantity = new SimpleIntegerProperty(quantity);
        this.unit = new SimpleStringProperty(unit);
        this.updates = new SimpleStringProperty(updates);
        this.price = new SimpleDoubleProperty(price);
    }

    // Name
    public String getName() { return name.get(); }
    public void setName(String value) { name.set(value); }
    public StringProperty nameProperty() { return name; }

    // Category
    public String getCategory() { return category.get(); }
    public void setCategory(String value) { category.set(value); }
    public StringProperty categoryProperty() { return category; }

    // Quantity
    public int getQuantity() { return quantity.get(); }
    public void setQuantity(int value) { quantity.set(value); }
    public IntegerProperty quantityProperty() { return quantity; }

    // Unit
    public String getUnit() { return unit.get(); }
    public void setUnit(String value) { unit.set(value); }
    public StringProperty unitProperty() { return unit; }

    // Updates
    public String getUpdates() { return updates.get(); }
    public void setUpdates(String value) { updates.set(value); }
    public StringProperty updatesProperty() { return updates; }

    // Price
    public double getPrice() { return price.get(); }
    public void setPrice(double value) { price.set(value); }
    public DoubleProperty priceProperty() { return price; }
}
