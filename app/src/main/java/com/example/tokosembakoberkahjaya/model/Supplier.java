package com.example.tokosembakoberkahjaya.model;

import com.google.gson.annotations.SerializedName;

public class Supplier {
    public int id;
    public String name;
    public String phone;
    public String address;

    @SerializedName("product_count")
    public int productCount;
}
