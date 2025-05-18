package view;

public class Dish {
    private String name;
    private String category;
    private double price;
    private String size;
    private int servings;

    public Dish(String name, String category, double price, String size, int servings) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.size = size;
        this.servings = servings;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public int getServings() { return servings; }
    public void setServings(int servings) { this.servings = servings; }
}
