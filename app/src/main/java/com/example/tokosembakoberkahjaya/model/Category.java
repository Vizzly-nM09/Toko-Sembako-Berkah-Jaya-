package com.example.tokosembakoberkahjaya.model;

import com.google.gson.annotations.SerializedName;

public class Category {
    public int id;
    public String name;

    @SerializedName("product_count")
    public int productCount;
}
