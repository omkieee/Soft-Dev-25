package controller;

public class ProfitRecord {
    private String month;
    private double income;
    private double expense;
    private double profit;

    public ProfitRecord(String month, double income, double expense, double profit) {
        this.month = month;
        this.income = income;
        this.expense = expense;
        this.profit = profit;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public double getIncome() {
        return income;
    }

    public void setIncome(double income) {
        this.income = income;
    }

    public double getExpense() {
        return expense;
    }

    public void setExpense(double expense) {
        this.expense = expense;
    }

    public double getProfit() {
        return profit;
    }

    public void setProfit(double profit) {
        this.profit = profit;
    }
}
