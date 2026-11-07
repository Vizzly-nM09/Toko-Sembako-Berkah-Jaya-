package com.example.tokosembakoberkahjaya.model;

import com.google.gson.annotations.SerializedName;

public class ProductRequest {
    public String name;

    @SerializedName("category_id")
    public Integer categoryId;

    @SerializedName("supplier_id")
    public Integer supplierId;

    public int stock;

    @SerializedName("min_stock")
    public int minStock;

    public double price;

    public ProductRequest(String name, Integer categoryId, Integer supplierId,
                          int stock, int minStock, double price) {
        this.name = name;
        this.categoryId = categoryId;
        this.supplierId = supplierId;
        this.stock = stock;
        this.minStock = minStock;
        this.price = price;
    }
}
