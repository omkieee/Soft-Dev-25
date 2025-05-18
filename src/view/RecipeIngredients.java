package view;

public class RecipeIngredients {
    private String ingredientsName;
    private String quantity;
    private String unitType;

    public RecipeIngredients(String ingredientsName, String quantity, String unitType) {
        this.ingredientsName = ingredientsName;
        this.quantity = quantity;
        this.unitType = unitType;
    }

    public String getIngredient() {
        return ingredientsName;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getUnitType() {
        return unitType;
    }
}
