package com.example.tokosembakoberkahjaya.api;

import com.example.tokosembakoberkahjaya.model.ApiResult;
import com.example.tokosembakoberkahjaya.model.Category;
import com.example.tokosembakoberkahjaya.model.CategoryRequest;
import com.example.tokosembakoberkahjaya.model.Dashboard;
import com.example.tokosembakoberkahjaya.model.Product;
import com.example.tokosembakoberkahjaya.model.ProductRequest;
import com.example.tokosembakoberkahjaya.model.Supplier;
import com.example.tokosembakoberkahjaya.model.SupplierRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

public interface ApiService {
    @GET("dashboard.php")
    Call<Dashboard> getDashboard();

    @GET("products.php")
    Call<List<Product>> getProducts(
            @Query("search") String search,
            @Query("category_id") Integer categoryId,
            @Query("supplier_id") Integer supplierId,
            @Query("stock_min") Integer stockMin,
            @Query("stock_max") Integer stockMax,
            @Query("low_stock") Integer lowStock
    );

    @POST("products.php")
    Call<ApiResult> createProduct(@Body ProductRequest body);

    @PUT("products.php")
    Call<ApiResult> updateProduct(@Query("id") int id, @Body ProductRequest body);

    @DELETE("products.php")
    Call<ApiResult> deleteProduct(@Query("id") int id);

    @GET("categories.php")
    Call<List<Category>> getCategories();

    @POST("categories.php")
    Call<ApiResult> createCategory(@Body CategoryRequest body);

    @PUT("categories.php")
    Call<ApiResult> updateCategory(@Query("id") int id, @Body CategoryRequest body);

    @DELETE("categories.php")
    Call<ApiResult> deleteCategory(@Query("id") int id);

    @GET("suppliers.php")
    Call<List<Supplier>> getSuppliers();

    @POST("suppliers.php")
    Call<ApiResult> createSupplier(@Body SupplierRequest body);

    @PUT("suppliers.php")
    Call<ApiResult> updateSupplier(@Query("id") int id, @Body SupplierRequest body);

    @DELETE("suppliers.php")
    Call<ApiResult> deleteSupplier(@Query("id") int id);
}
