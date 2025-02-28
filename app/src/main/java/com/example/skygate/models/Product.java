package com.example.skygate.models;

import java.io.Serializable;

;

public class Product implements Serializable {
    private String name;
    private int soldQuantity;

    // Empty constructor for Firebase
    public Product() {
    }

    public Product(String name, int soldQuantity) {
        this.name = name != null ? name : "N/A";
        this.soldQuantity = soldQuantity;
    }

    public String getName() {
        return name != null ? name : "N/A";
    }

    public void setName(String name) {
        this.name = name != null ? name : "N/A";
    }

    public int getSoldQuantity() {
        return soldQuantity;
    }

    public void setSoldQuantity(int soldQuantity) {
        this.soldQuantity = soldQuantity;
    }
}