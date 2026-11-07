package com.example.tokosembakoberkahjaya.model;

import com.google.gson.annotations.SerializedName;

public class Dashboard {
    @SerializedName("total_products")
    public int totalProducts;

    @SerializedName("total_stock")
    public int totalStock;

    @SerializedName("low_stock_count")
    public int lowStockCount;

    @SerializedName("total_categories")
    public int totalCategories;

    @SerializedName("total_suppliers")
    public int totalSuppliers;

    @SerializedName("inventory_value")
    public double inventoryValue;
}
