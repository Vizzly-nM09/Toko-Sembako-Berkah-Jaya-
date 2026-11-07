package com.example.tokosembakoberkahjaya.model;

import com.google.gson.annotations.SerializedName;

public class Product {
    public int id;
    public String name;

    @SerializedName("category_id")
    public Integer categoryId;

    @SerializedName("category_name")
    public String categoryName;

    @SerializedName("supplier_id")
    public Integer supplierId;

    @SerializedName("supplier_name")
    public String supplierName;

    public int stock;

    @SerializedName("min_stock")
    public int minStock;

    public double price;

    @SerializedName("image_url")
    public String imageUrl;

    @SerializedName("is_low_stock")
    public boolean lowStock;
}
