package com.example.skygate;

public class StockItem {
    private String product;
    private int quantity;

    public StockItem(String product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public String getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }
}