package controller;

public class Dish {
    private String name;
    private double price;
    private String size;
    private String status; // NEW FIELD

    public Dish(String name, double price) {
        this.name = name;
        this.price = price;
    }

    // Getters and setters
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
