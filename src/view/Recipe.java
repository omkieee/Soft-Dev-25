package view;

public class Recipe {
    private String size;
    private String productName;
    private String category;

    public Recipe(String productName, String category, String size) {
        this.productName = productName;
        this.category = category;
        this.size = size;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
