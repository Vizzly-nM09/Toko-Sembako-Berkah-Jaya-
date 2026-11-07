package com.example.tokosembakoberkahjaya;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.tokosembakoberkahjaya.api.ApiClient;
import com.example.tokosembakoberkahjaya.api.ApiService;
import com.example.tokosembakoberkahjaya.model.Category;
import com.example.tokosembakoberkahjaya.model.Dashboard;
import com.example.tokosembakoberkahjaya.model.Product;
import com.example.tokosembakoberkahjaya.model.Supplier;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import retrofit2.Response;

public class ApiContractTest {
    @BeforeClass
    public static void requireApi() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(
                    ApiClient.BASE_URL + "health.php").openConnection();
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            Assume.assumeTrue("API tidak berjalan di " + ApiClient.BASE_URL,
                    connection.getResponseCode() == 200);
        } catch (Exception e) {
            Assume.assumeNoException("API tidak berjalan di " + ApiClient.BASE_URL, e);
        }
    }

    @Test
    public void readsAllListEndpoints() throws Exception {
        ApiService api = ApiClient.service();

        Response<Dashboard> dashboard = api.getDashboard().execute();
        assertTrue(dashboard.isSuccessful());
        assertNotNull(dashboard.body());
        assertTrue(dashboard.body().totalProducts > 0);

        Response<List<Product>> products = api.getProducts(null, null, null, null, null, null).execute();
        assertTrue(products.isSuccessful());
        assertNotNull(products.body());
        assertFalse(products.body().isEmpty());
        Product product = products.body().get(0);
        assertNotNull(product.name);
        assertTrue(product.minStock >= 0);
        assertNotNull(product.categoryName);

        Response<List<Category>> categories = api.getCategories().execute();
        assertTrue(categories.isSuccessful());
        assertNotNull(categories.body());
        assertFalse(categories.body().isEmpty());

        Response<List<Supplier>> suppliers = api.getSuppliers().execute();
        assertTrue(suppliers.isSuccessful());
        assertNotNull(suppliers.body());
        assertFalse(suppliers.body().isEmpty());
    }
}
