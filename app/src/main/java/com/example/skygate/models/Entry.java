package com.example.skygate.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Entry implements Serializable {
    private String id;
    private String date;
    private int controlNumber;
    private String entryType;
    private String driver;
    private long createdAt;
    private int month;
    private List<Product> products;

    // Empty constructor for Firebase
    public Entry() {
        this.products = new ArrayList<>();
    }

    // Main constructor used by loadRecentEntries
    public Entry(String entryId, String entryDate, int controlNum, String entryType,
                 String driver, Long createdAtLong, List<Product> productList) {
        this.id = entryId;
        this.date = entryDate;
        this.controlNumber = controlNum;
        this.entryType = entryType;
        this.driver = driver;
        this.createdAt = createdAtLong != null ? createdAtLong : 0L;
        this.products = productList != null ? productList : new ArrayList<>();
    }

    // Constructor for creating entries with month
    public Entry(String id, String date, int controlNumber, String entryType,
                 String driver, long createdAt, int month) {
        this.id = id;
        this.date = date;
        this.controlNumber = controlNumber;
        this.entryType = entryType;
        this.driver = driver;
        this.createdAt = createdAt;
        this.month = month;
        this.products = new ArrayList<>();
    }

    // Constructor for list display
    public Entry(String id, String date, int controlNumber, String entryType,
                 String driver, int createdAt) {
        this.id = id;
        this.date = date;
        this.controlNumber = controlNumber;
        this.entryType = entryType;
        this.driver = driver;
        this.createdAt = createdAt;
        this.products = new ArrayList<>();
    }

    // Add a product to the entry
    public void addProduct(Product product) {
        if (products == null) {
            products = new ArrayList<>();
        }
        if (product != null) {
            products.add(product);
        }
    }

    // Get a specific product's details
    public Product getProduct(int index) {
        if (products != null && index >= 0 && index < products.size()) {
            return products.get(index);
        }
        return null;
    }

    // Get the names and sold quantities of all products (for spreadsheet-like display)
    public List<String> getProductDetailsList() {
        List<String> productDetails = new ArrayList<>();
        if (products != null) {
            for (Product product : products) {
                String details = product.getName() + " (" + product.getSoldQuantity() + ")";
                productDetails.add(details);
            }
        }
        return productDetails;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getControlNumber() {
        return controlNumber;
    }

    public void setControlNumber(int controlNumber) {
        this.controlNumber = controlNumber;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public List<Product> getProducts() {
        return products != null ? products : new ArrayList<>();
    }

    public void setProducts(List<Product> products) {
        this.products = products != null ? products : new ArrayList<>();
    }
}