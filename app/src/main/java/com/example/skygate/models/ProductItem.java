package com.example.skygate.models;

import java.io.Serializable;

public class ProductItem implements Serializable {
    private String product;
    private int in;
    private int out;
    private int sold;

    public ProductItem() {
        // Required empty constructor for Firebase
    }

    // Getters and Setters
    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }

    public int getIn() { return in; }
    public void setIn(int in) { this.in = in; }

    public int getOut() { return out; }
    public void setOut(int out) { this.out = out; }

    public int getSold() { return sold; }
    public void setSold(int sold) { this.sold = sold; }
}