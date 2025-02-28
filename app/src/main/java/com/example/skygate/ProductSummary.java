package com.example.skygate;

public class ProductSummary {
    private String product;
    private int totalQuantity;
    private double totalAmount;

    public ProductSummary(String product, int totalQuantity, double totalAmount) {
        this.product = product;
        this.totalQuantity = totalQuantity;
        this.totalAmount = totalAmount;
    }

    public void addQuantity(int quantity) {
        this.totalQuantity += quantity;
    }

    public void addAmount(double amount) {
        this.totalAmount += amount;
    }

    public String getProduct() { return product; }
    public int getTotalQuantity() { return totalQuantity; }
    public double getTotalAmount() { return totalAmount; }
}