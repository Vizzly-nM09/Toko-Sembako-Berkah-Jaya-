package com.example.tokosembakoberkahjaya;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tokosembakoberkahjaya.api.ApiClient;
import com.example.tokosembakoberkahjaya.api.ApiService;
import com.example.tokosembakoberkahjaya.model.ApiResult;
import com.example.tokosembakoberkahjaya.model.Category;
import com.example.tokosembakoberkahjaya.model.CategoryRequest;
import com.example.tokosembakoberkahjaya.model.Dashboard;
import com.example.tokosembakoberkahjaya.model.Product;
import com.example.tokosembakoberkahjaya.model.ProductRequest;
import com.example.tokosembakoberkahjaya.model.Supplier;
import com.example.tokosembakoberkahjaya.model.SupplierRequest;
import com.google.gson.Gson;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends Activity {

    private ApiService api;
    private LinearLayout content;

    private final ArrayList<Product> products = new ArrayList<>();
    private final ArrayList<Category> categories = new ArrayList<>();
    private final ArrayList<Supplier> suppliers = new ArrayList<>();

    private Dashboard dashboard;
    private String currentPage = "dashboard";
    private int pendingLoads = 0;
    private int loadGeneration = 0;
    private boolean loadError = false;
    private boolean firstLoadDone = false;

    private int dark;
    private int grey;
    private int green;
    private int red;
    private int divider;
    private int surface;

    private Button navDashboard;
    private Button navProducts;
    private Button navCategories;
    private Button navSuppliers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        content = findViewById(R.id.contentContainer);
        api = ApiClient.service();

        dark = getColor(R.color.ink);
        grey = getColor(R.color.text_muted);
        green = getColor(R.color.brand_green_accent);
        red = getColor(R.color.error);
        divider = getColor(R.color.divider);
        surface = getColor(R.color.surface);

        navDashboard = findViewById(R.id.btnDashboard);
        navProducts = findViewById(R.id.btnProducts);
        navCategories = findViewById(R.id.btnCategories);
        navSuppliers = findViewById(R.id.btnSuppliers);

        navDashboard.setOnClickListener(v -> openPage("dashboard"));
        navProducts.setOnClickListener(v -> openPage("products"));
        navCategories.setOnClickListener(v -> openPage("categories"));
        navSuppliers.setOnClickListener(v -> openPage("suppliers"));

        updateNavSelection();
        loadAll();
    }

    private void loadAll() {
        final int generation = ++loadGeneration;
        firstLoadDone = false;
        loadError = false;
        pendingLoads = 4;

        showLoadingPage();

        api.getDashboard().enqueue(new Callback<Dashboard>() {
            @Override
            public void onResponse(Call<Dashboard> call, Response<Dashboard> response) {
                if (generation != loadGeneration) return;
                if (response.isSuccessful() && response.body() != null) {
                    dashboard = response.body();
                } else {
                    loadError = true;
                }
                finishLoad(generation);
            }

            @Override
            public void onFailure(Call<Dashboard> call, Throwable t) {
                if (generation != loadGeneration) return;
                loadError = true;
                finishLoad(generation);
            }
        });

        api.getProducts(null, null, null, null, null, null).enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (generation != loadGeneration) return;
                if (response.isSuccessful() && response.body() != null) {
                    products.clear();
                    products.addAll(response.body());
                } else {
                    loadError = true;
                }
                finishLoad(generation);
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                if (generation != loadGeneration) return;
                loadError = true;
                finishLoad(generation);
            }
        });

        api.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (generation != loadGeneration) return;
                if (response.isSuccessful() && response.body() != null) {
                    categories.clear();
                    categories.addAll(response.body());
                } else {
                    loadError = true;
                }
                finishLoad(generation);
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                if (generation != loadGeneration) return;
                loadError = true;
                finishLoad(generation);
            }
        });

        api.getSuppliers().enqueue(new Callback<List<Supplier>>() {
            @Override
            public void onResponse(Call<List<Supplier>> call, Response<List<Supplier>> response) {
                if (generation != loadGeneration) return;
                if (response.isSuccessful() && response.body() != null) {
                    suppliers.clear();
                    suppliers.addAll(response.body());
                } else {
                    loadError = true;
                }
                finishLoad(generation);
            }

            @Override
            public void onFailure(Call<List<Supplier>> call, Throwable t) {
                if (generation != loadGeneration) return;
                loadError = true;
                finishLoad(generation);
            }
        });
    }

    private void finishLoad(int generation) {
        if (generation != loadGeneration) return;

        pendingLoads--;
        if (pendingLoads > 0) return;

        firstLoadDone = true;
        if (loadError) {
            showConnectionError();
        } else {
            refreshCurrentPage();
        }
    }

    private void openPage(String page) {
        currentPage = page;
        updateNavSelection();
        if (!firstLoadDone) {
            showLoadingPage();
            return;
        }
        if (loadError) {
            showConnectionError();
            return;
        }
        refreshCurrentPage();
    }

    private void updateNavSelection() {
        navDashboard.setSelected("dashboard".equals(currentPage));
        navProducts.setSelected("products".equals(currentPage));
        navCategories.setSelected("categories".equals(currentPage));
        navSuppliers.setSelected("suppliers".equals(currentPage));
    }

    private void refreshCurrentPage() {
        updateNavSelection();
        switch (currentPage) {
            case "products":
                renderProductsPage();
                break;
            case "categories":
                renderCategoriesPage();
                break;
            case "suppliers":
                renderSuppliersPage();
                break;
            default:
                renderDashboard();
                break;
        }
    }

    private void showLoadingPage() {
        content.removeAllViews();

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(0, dp(72), 0, 0);

        ProgressBar progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        progress.setIndeterminateTintList(ColorStateList.valueOf(green));
        box.addView(progress);

        TextView title = new TextView(this);
        title.setText("Memuat data...");
        title.setTextColor(dark);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(16), 0, 0);
        box.addView(title);

        TextView detail = new TextView(this);
        detail.setText("Menghubungi " + ApiClient.BASE_URL);
        detail.setTextColor(grey);
        detail.setTextSize(13);
        detail.setGravity(Gravity.CENTER);
        detail.setPadding(0, dp(6), 0, 0);
        box.addView(detail);

        content.addView(box, matchWrap(0, 0));
    }

    private void showConnectionError() {
        content.removeAllViews();

        LinearLayout card = makeCard();

        TextView title = new TextView(this);
        title.setText("Tidak bisa terhubung ke server");
        title.setTextColor(dark);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        card.addView(title);

        TextView detail = new TextView(this);
        detail.setText("Aplikasi belum bisa mengambil data dari API.");
        detail.setTextColor(grey);
        detail.setTextSize(14);
        detail.setPadding(0, dp(6), 0, dp(12));
        card.addView(detail);

        TextView check = new TextView(this);
        check.setText("Periksa:");
        check.setTextColor(dark);
        check.setTextSize(14);
        check.setTypeface(null, Typeface.BOLD);
        card.addView(check);

        addTextTo(card, "1. PHP API berjalan di " + ApiClient.BASE_URL, dark, 13);
        addTextTo(card, "2. adb reverse tcp:8000 tcp:8000 sudah dijalankan", dark, 13);
        addTextTo(card, "3. HP terhubung lewat USB debugging", dark, 13);

        Button retry = makeButton("Coba Lagi");
        retry.setOnClickListener(v -> loadAll());
        card.addView(retry, matchWrap(12, 0));

        content.addView(card, matchWrap(0, 0));
    }

    private void renderDashboard() {
        currentPage = "dashboard";
        content.removeAllViews();
        addHeading("Dashboard");
        addText("Ringkasan stok toko", grey, 14, 0, 12);

        if (dashboard != null) {
            LinearLayout summary = makeCard();
            addSummaryRow(summary, "Total Produk", String.valueOf(dashboard.totalProducts), false);
            addDivider(summary);
            addSummaryRow(summary, "Total Stok Barang", String.valueOf(dashboard.totalStock), false);
            addDivider(summary);
            addSummaryRow(summary, "Produk Stok Menipis", String.valueOf(dashboard.lowStockCount),
                    dashboard.lowStockCount > 0);
            addDivider(summary);
            addSummaryRow(summary, "Kategori", String.valueOf(dashboard.totalCategories), false);
            addDivider(summary);
            addSummaryRow(summary, "Supplier", String.valueOf(dashboard.totalSuppliers), false);
            addDivider(summary);
            addSummaryRow(summary, "Nilai Inventori", rupiah(dashboard.inventoryValue), false);
            content.addView(summary, matchWrap(0, 4));
        }

        addHeading("Peringatan Stok");
        boolean found = false;
        LinearLayout warning = makeCard();
        for (Product product : products) {
            if (product.lowStock) {
                if (!found) {
                    warning.setBackgroundResource(R.drawable.bg_card_warning);
                }
                TextView item = new TextView(this);
                item.setText("• " + product.name + ", tersisa " + product.stock
                        + " item (min. " + product.minStock + ")");
                item.setTextColor(red);
                item.setTextSize(14);
                item.setPadding(0, dp(4), 0, dp(4));
                warning.addView(item);
                found = true;
            }
        }
        if (!found) {
            addTextTo(warning, "Belum ada produk dengan stok menipis.", grey, 14);
        }
        content.addView(warning, matchWrap(0, 4));

        addHeading("Daftar Produk");
        if (products.isEmpty()) {
            addEmptyState(content, "Belum ada produk",
                    "Tambahkan produk pertama lewat halaman Produk.");
            return;
        }
        for (Product product : products) {
            addProductRow(product, false);
        }
    }

    private void renderProductsPage() {
        currentPage = "products";
        content.removeAllViews();
        addHeading("Manajemen Produk");
        addText("Tambah, ubah, atau hapus data produk.", grey, 14, 0, 12);

        Button add = makeButton("+ Tambah Produk");
        add.setOnClickListener(v -> showProductDialog(null));
        content.addView(add, matchWrap(0, 12));

        EditText search = field("Cari nama produk...");
        search.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_search, 0, 0, 0);
        search.setCompoundDrawablePadding(dp(8));
        content.addView(search, matchWrap(0, 8));

        LinearLayout filterRow = new LinearLayout(this);
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        filterRow.setGravity(Gravity.CENTER_VERTICAL);

        Button filterToggle = smallButton("Filter");
        filterRow.addView(filterToggle);

        TextView resultCount = new TextView(this);
        resultCount.setTextColor(grey);
        resultCount.setTextSize(13);
        resultCount.setGravity(Gravity.END);
        filterRow.addView(resultCount, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        content.addView(filterRow, matchWrap(0, 8));

        LinearLayout filters = makeCard();
        filters.setVisibility(View.GONE);

        addFormLabel(filters, "Kategori");
        EditText categoryFilter = field("Semua kategori");
        filters.addView(categoryFilter, matchWrap(0, 0));

        addFormLabel(filters, "Supplier");
        EditText supplierFilter = field("Semua supplier");
        filters.addView(supplierFilter, matchWrap(0, 0));

        LinearLayout stockRow = new LinearLayout(this);
        stockRow.setOrientation(LinearLayout.HORIZONTAL);
        stockRow.setBaselineAligned(false);

        LinearLayout minBox = new LinearLayout(this);
        minBox.setOrientation(LinearLayout.VERTICAL);
        addFormLabel(minBox, "Stok minimum");
        EditText minStock = field("0");
        minStock.setInputType(InputType.TYPE_CLASS_NUMBER);
        minBox.addView(minStock, matchWrap(0, 0));

        LinearLayout maxBox = new LinearLayout(this);
        maxBox.setOrientation(LinearLayout.VERTICAL);
        addFormLabel(maxBox, "Stok maksimum");
        EditText maxStock = field("0");
        maxStock.setInputType(InputType.TYPE_CLASS_NUMBER);
        maxBox.addView(maxStock, matchWrap(0, 0));

        LinearLayout.LayoutParams minParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        minParams.setMarginEnd(dp(8));
        stockRow.addView(minBox, minParams);
        stockRow.addView(maxBox, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        filters.addView(stockRow, matchWrap(0, 0));

        Button lowToggle = smallButton("Hanya stok menipis");
        filters.addView(lowToggle, matchWrap(8, 0));

        content.addView(filters, matchWrap(0, 8));

        filterToggle.setOnClickListener(v -> {
            boolean show = filters.getVisibility() != View.VISIBLE;
            filters.setVisibility(show ? View.VISIBLE : View.GONE);
            filterToggle.setText(show ? "Tutup Filter" : "Filter");
        });

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        content.addView(list, matchWrap(0, 0));

        final boolean[] lowOnly = {false};
        Runnable refresh = () -> {
            list.removeAllViews();
            String keyword = search.getText().toString().trim().toLowerCase(Locale.ROOT);
            String category = categoryFilter.getText().toString().trim().toLowerCase(Locale.ROOT);
            String supplier = supplierFilter.getText().toString().trim().toLowerCase(Locale.ROOT);
            String minText = minStock.getText().toString().trim();
            String maxText = maxStock.getText().toString().trim();

            int shown = 0;
            for (Product product : products) {
                boolean matches = safe(product.name).toLowerCase(Locale.ROOT).contains(keyword)
                        && safe(product.categoryName).toLowerCase(Locale.ROOT).contains(category)
                        && safe(product.supplierName).toLowerCase(Locale.ROOT).contains(supplier);

                if (!minText.isEmpty()) {
                    try {
                        matches = matches && product.stock >= Integer.parseInt(minText);
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (!maxText.isEmpty()) {
                    try {
                        matches = matches && product.stock <= Integer.parseInt(maxText);
                    } catch (NumberFormatException ignored) {
                    }
                }

                if (matches && (!lowOnly[0] || product.lowStock)) {
                    addProductRowTo(list, product, true);
                    shown++;
                }
            }

            resultCount.setText(shown + " produk");

            if (shown == 0) {
                if (products.isEmpty()) {
                    addEmptyState(list, "Belum ada produk",
                            "Tambahkan produk pertama lewat tombol di atas.");
                } else {
                    addEmptyState(list, "Produk tidak ditemukan",
                            "Ubah kata kunci atau filter.");
                }
            }
        };

        TextWatcher watcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refresh.run();
            }

            public void afterTextChanged(Editable s) {
            }
        };

        search.addTextChangedListener(watcher);
        categoryFilter.addTextChangedListener(watcher);
        supplierFilter.addTextChangedListener(watcher);
        minStock.addTextChangedListener(watcher);
        maxStock.addTextChangedListener(watcher);

        lowToggle.setOnClickListener(v -> {
            lowOnly[0] = !lowOnly[0];
            lowToggle.setText(lowOnly[0] ? "Tampilkan semua stok" : "Hanya stok menipis");
            refresh.run();
        });

        refresh.run();
    }

    private void renderCategoriesPage() {
        currentPage = "categories";
        content.removeAllViews();
        addHeading("Manajemen Kategori");
        addText("Kelola data kategori toko.", grey, 14, 0, 12);

        Button add = makeButton("+ Tambah Kategori");
        add.setOnClickListener(v -> showCategoryDialog(null));
        content.addView(add, matchWrap(0, 12));

        if (categories.isEmpty()) {
            addEmptyState(content, "Belum ada kategori",
                    "Tambahkan kategori pertama lewat tombol di atas.");
            return;
        }

        LinearLayout card = makeCard();
        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            LinearLayout row = makeRow();

            LinearLayout textBox = makeColumn();

            TextView name = new TextView(this);
            name.setText(category.name);
            name.setTextColor(dark);
            name.setTextSize(15);
            name.setTypeface(null, Typeface.BOLD);
            textBox.addView(name);

            TextView count = new TextView(this);
            count.setText(category.productCount + " produk");
            count.setTextColor(grey);
            count.setTextSize(12);
            count.setPadding(0, dp(2), 0, 0);
            textBox.addView(count);

            row.addView(textBox, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            Button edit = smallButton("Ubah");
            edit.setOnClickListener(v -> showCategoryDialog(category));
            Button delete = dangerButton("Hapus");
            delete.setOnClickListener(v -> confirmDeleteCategory(category));
            row.addView(edit);
            row.addView(delete, wrapStart(6));

            card.addView(row, matchWrap(0, 0));
            if (i < categories.size() - 1) {
                addDivider(card);
            }
        }
        content.addView(card, matchWrap(0, 0));
    }

    private void renderSuppliersPage() {
        currentPage = "suppliers";
        content.removeAllViews();
        addHeading("Manajemen Supplier");
        addText("Kelola data supplier toko.", grey, 14, 0, 12);

        Button add = makeButton("+ Tambah Supplier");
        add.setOnClickListener(v -> showSupplierDialog(null));
        content.addView(add, matchWrap(0, 12));

        if (suppliers.isEmpty()) {
            addEmptyState(content, "Belum ada supplier",
                    "Tambahkan supplier pertama lewat tombol di atas.");
            return;
        }

        for (Supplier supplier : suppliers) {
            LinearLayout card = makeCard();

            TextView name = new TextView(this);
            name.setText(supplier.name);
            name.setTextColor(dark);
            name.setTextSize(16);
            name.setTypeface(null, Typeface.BOLD);
            card.addView(name);

            TextView count = new TextView(this);
            count.setText(supplier.productCount + " produk");
            count.setTextColor(grey);
            count.setTextSize(12);
            count.setPadding(0, dp(2), 0, 0);
            card.addView(count);

            String detail = "Telepon: " + (isBlank(supplier.phone) ? "-" : supplier.phone)
                    + "\nAlamat: " + (isBlank(supplier.address) ? "-" : supplier.address);
            TextView info = new TextView(this);
            info.setText(detail);
            info.setTextColor(grey);
            info.setTextSize(13);
            info.setPadding(0, dp(6), 0, 0);
            card.addView(info);

            LinearLayout buttons = new LinearLayout(this);
            buttons.setOrientation(LinearLayout.HORIZONTAL);
            buttons.setGravity(Gravity.END);
            Button edit = smallButton("Ubah");
            edit.setOnClickListener(v -> showSupplierDialog(supplier));
            Button delete = dangerButton("Hapus");
            delete.setOnClickListener(v -> confirmDeleteSupplier(supplier));
            buttons.addView(edit);
            buttons.addView(delete, wrapStart(6));
            card.addView(buttons, matchWrap(8, 0));

            content.addView(card, matchWrap(0, 10));
        }
    }

    private void showProductDialog(Product product) {
        boolean editing = product != null;

        LinearLayout form = newForm();

        EditText name = addField(form, "Nama produk", "Contoh: Beras Premium 5 kg");
        EditText stock = addField(form, "Jumlah stok", "0");
        stock.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText minStock = addField(form, "Stok minimum", "5");
        minStock.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText price = addField(form, "Harga satuan", "0");
        price.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        addFormLabel(form, "Kategori");
        Spinner categorySpinner = styledSpinner();
        ArrayList<String> categoryLabels = new ArrayList<>();
        categoryLabels.add("Tanpa kategori");
        for (Category category : categories) {
            categoryLabels.add(category.name);
        }
        setSpinnerItems(categorySpinner, categoryLabels);
        form.addView(categorySpinner, matchWrap(0, 0));

        addFormLabel(form, "Supplier");
        Spinner supplierSpinner = styledSpinner();
        ArrayList<String> supplierLabels = new ArrayList<>();
        supplierLabels.add("Tanpa supplier");
        for (Supplier supplier : suppliers) {
            supplierLabels.add(supplier.name);
        }
        setSpinnerItems(supplierSpinner, supplierLabels);
        form.addView(supplierSpinner, matchWrap(0, 0));

        if (editing) {
            name.setText(product.name);
            stock.setText(String.valueOf(product.stock));
            minStock.setText(String.valueOf(product.minStock));
            price.setText(String.valueOf(product.price));
            selectCategory(categorySpinner, product.categoryId);
            selectSupplier(supplierSpinner, product.supplierId);
        } else {
            minStock.setText("5");
        }

        showFormDialog(editing ? "Ubah Produk" : "Tambah Produk", form, dialog -> {
            String productName = name.getText().toString().trim();
            if (productName.isEmpty()) {
                name.setError("Nama produk wajib diisi");
                name.requestFocus();
                return;
            }

            int stockValue;
            try {
                stockValue = parseInt(stock.getText().toString(), 0);
            } catch (NumberFormatException e) {
                stock.setError("Stok harus berupa angka");
                stock.requestFocus();
                return;
            }

            int minStockValue;
            try {
                minStockValue = parseInt(minStock.getText().toString(), 5);
            } catch (NumberFormatException e) {
                minStock.setError("Stok minimum harus berupa angka");
                minStock.requestFocus();
                return;
            }

            double priceValue;
            try {
                priceValue = parseDouble(price.getText().toString(), 0);
            } catch (NumberFormatException e) {
                price.setError("Harga harus berupa angka");
                price.requestFocus();
                return;
            }

            if (stockValue < 0) {
                stock.setError("Stok tidak boleh negatif");
                stock.requestFocus();
                return;
            }
            if (minStockValue < 0) {
                minStock.setError("Stok minimum tidak boleh negatif");
                minStock.requestFocus();
                return;
            }
            if (priceValue < 0) {
                price.setError("Harga tidak boleh negatif");
                price.requestFocus();
                return;
            }

            ProductRequest request = new ProductRequest(
                    productName,
                    selectedCategoryId(categorySpinner),
                    selectedSupplierId(supplierSpinner),
                    stockValue,
                    minStockValue,
                    priceValue
            );

            if (editing) {
                api.updateProduct(product.id, request).enqueue(new Callback<ApiResult>() {
                    @Override
                    public void onResponse(Call<ApiResult> call, Response<ApiResult> response) {
                        if (response.isSuccessful()) {
                            dialog.dismiss();
                            toast(messageOf(response.body(), "Produk berhasil diperbarui."));
                            loadAll();
                        } else {
                            toast(apiError(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResult> call, Throwable t) {
                        toast(connectionError(t));
                    }
                });
            } else {
                api.createProduct(request).enqueue(new Callback<ApiResult>() {
                    @Override
                    public void onResponse(Call<ApiResult> call, Response<ApiResult> response) {
                        if (response.isSuccessful()) {
                            dialog.dismiss();
                            toast(messageOf(response.body(), "Produk berhasil ditambahkan."));
                            loadAll();
                        } else {
                            toast(apiError(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResult> call, Throwable t) {
                        toast(connectionError(t));
                    }
                });
            }
        });
    }

    private void showCategoryDialog(Category category) {
        boolean editing = category != null;

        LinearLayout form = newForm();
        EditText name = addField(form, "Nama kategori", "Contoh: Makanan Pokok");
        if (editing) {
            name.setText(category.name);
        }

        showFormDialog(editing ? "Ubah Kategori" : "Tambah Kategori", form, dialog -> {
            String categoryName = name.getText().toString().trim();
            if (categoryName.isEmpty()) {
                name.setError("Nama kategori wajib diisi");
                name.requestFocus();
                return;
            }

            CategoryRequest request = new CategoryRequest(categoryName);
            Call<ApiResult> call = editing
                    ? api.updateCategory(category.id, request)
                    : api.createCategory(request);

            call.enqueue(new Callback<ApiResult>() {
                @Override
                public void onResponse(Call<ApiResult> call, Response<ApiResult> response) {
                    if (response.isSuccessful()) {
                        dialog.dismiss();
                        toast(messageOf(response.body(), "Kategori berhasil disimpan."));
                        loadAll();
                    } else {
                        toast(apiError(response));
                    }
                }

                @Override
                public void onFailure(Call<ApiResult> call, Throwable t) {
                    toast(connectionError(t));
                }
            });
        });
    }

    private void showSupplierDialog(Supplier supplier) {
        boolean editing = supplier != null;

        LinearLayout form = newForm();
        EditText name = addField(form, "Nama supplier", "Contoh: CV Sumber Makmur");
        EditText phone = addField(form, "Telepon (opsional)", "Contoh: 0812 3456 7890");
        phone.setInputType(InputType.TYPE_CLASS_PHONE);
        EditText address = addField(form, "Alamat (opsional)", "Contoh: Jl. Raya No. 10");
        address.setSingleLine(false);
        address.setMinLines(2);
        address.setGravity(Gravity.TOP | Gravity.START);
        address.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        if (editing) {
            name.setText(supplier.name);
            phone.setText(supplier.phone == null ? "" : supplier.phone);
            address.setText(supplier.address == null ? "" : supplier.address);
        }

        showFormDialog(editing ? "Ubah Supplier" : "Tambah Supplier", form, dialog -> {
            String supplierName = name.getText().toString().trim();
            if (supplierName.isEmpty()) {
                name.setError("Nama supplier wajib diisi");
                name.requestFocus();
                return;
            }

            String phoneValue = phone.getText().toString().trim();
            String addressValue = address.getText().toString().trim();
            SupplierRequest request = new SupplierRequest(
                    supplierName,
                    phoneValue.isEmpty() ? null : phoneValue,
                    addressValue.isEmpty() ? null : addressValue
            );

            Call<ApiResult> call = editing
                    ? api.updateSupplier(supplier.id, request)
                    : api.createSupplier(request);

            call.enqueue(new Callback<ApiResult>() {
                @Override
                public void onResponse(Call<ApiResult> call, Response<ApiResult> response) {
                    if (response.isSuccessful()) {
                        dialog.dismiss();
                        toast(messageOf(response.body(), "Supplier berhasil disimpan."));
                        loadAll();
                    } else {
                        toast(apiError(response));
                    }
                }

                @Override
                public void onFailure(Call<ApiResult> call, Throwable t) {
                    toast(connectionError(t));
                }
            });
        });
    }

    private void confirmDeleteProduct(Product product) {
        new AlertDialog.Builder(this)
                .setMessage("Hapus " + product.name + "?")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Hapus", (dialog, which) -> deleteProduct(product))
                .show();
    }

    private void confirmDeleteCategory(Category category) {
        new AlertDialog.Builder(this)
                .setMessage("Hapus kategori " + category.name + "?")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Hapus", (dialog, which) -> deleteCategory(category))
                .show();
    }

    private void confirmDeleteSupplier(Supplier supplier) {
        new AlertDialog.Builder(this)
                .setMessage("Hapus supplier " + supplier.name + "?")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Hapus", (dialog, which) -> deleteSupplier(supplier))
                .show();
    }

    private void deleteProduct(Product product) {
        api.deleteProduct(product.id).enqueue(new Callback<ApiResult>() {
            @Override
            public void onResponse(Call<ApiResult> call, Response<ApiResult> response) {
                if (response.isSuccessful()) {
                    toast(messageOf(response.body(), "Produk berhasil dihapus."));
                    loadAll();
                } else {
                    toast(apiError(response));
                }
            }

            @Override
            public void onFailure(Call<ApiResult> call, Throwable t) {
                toast(connectionError(t));
            }
        });
    }

    private void deleteCategory(Category category) {
        api.deleteCategory(category.id).enqueue(new Callback<ApiResult>() {
            @Override
            public void onResponse(Call<ApiResult> call, Response<ApiResult> response) {
                if (response.isSuccessful()) {
                    toast(messageOf(response.body(), "Kategori berhasil dihapus."));
                    loadAll();
                } else {
                    toast(apiError(response));
                }
            }

            @Override
            public void onFailure(Call<ApiResult> call, Throwable t) {
                toast(connectionError(t));
            }
        });
    }

    private void deleteSupplier(Supplier supplier) {
        api.deleteSupplier(supplier.id).enqueue(new Callback<ApiResult>() {
            @Override
            public void onResponse(Call<ApiResult> call, Response<ApiResult> response) {
                if (response.isSuccessful()) {
                    toast(messageOf(response.body(), "Supplier berhasil dihapus."));
                    loadAll();
                } else {
                    toast(apiError(response));
                }
            }

            @Override
            public void onFailure(Call<ApiResult> call, Throwable t) {
                toast(connectionError(t));
            }
        });
    }

    private interface FormHandler {
        void onSave(AlertDialog dialog);
    }

    private void showFormDialog(String title, LinearLayout form, FormHandler handler) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(form, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(scroll)
                .setNegativeButton("Batal", null)
                .setPositiveButton("Simpan", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setTextColor(green);
            positive.setAllCaps(false);
            positive.setOnClickListener(v -> handler.onSave(dialog));

            Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negative.setTextColor(grey);
            negative.setAllCaps(false);
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog);
            dialog.getWindow().setDimAmount(0.45f);
        }

        dialog.show();
    }

    private void selectCategory(Spinner spinner, Integer categoryId) {
        if (categoryId == null) return;
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).id == categoryId) {
                spinner.setSelection(i + 1);
                return;
            }
        }
    }

    private void selectSupplier(Spinner spinner, Integer supplierId) {
        if (supplierId == null) return;
        for (int i = 0; i < suppliers.size(); i++) {
            if (suppliers.get(i).id == supplierId) {
                spinner.setSelection(i + 1);
                return;
            }
        }
    }

    private Integer selectedCategoryId(Spinner spinner) {
        int position = spinner.getSelectedItemPosition();
        if (position <= 0 || position > categories.size()) return null;
        return categories.get(position - 1).id;
    }

    private Integer selectedSupplierId(Spinner spinner) {
        int position = spinner.getSelectedItemPosition();
        if (position <= 0 || position > suppliers.size()) return null;
        return suppliers.get(position - 1).id;
    }

    private String apiError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                ApiResult result = new Gson().fromJson(response.errorBody().string(), ApiResult.class);
                if (result != null && result.error != null && !result.error.isEmpty()) {
                    return result.error;
                }
            }
        } catch (Exception ignored) {
        }
        return "Permintaan gagal (HTTP " + response.code() + ")";
    }

    private String messageOf(ApiResult result, String fallback) {
        return result != null && result.message != null && !result.message.isEmpty()
                ? result.message
                : fallback;
    }

    private String connectionError(Throwable t) {
        String detail = t.getMessage();
        return "Tidak bisa terhubung ke API. Pastikan server PHP dan adb reverse aktif."
                + (detail == null ? "" : "\n" + detail);
    }

    private void addProductRow(Product product, boolean actions) {
        addProductRowTo(content, product, actions);
    }

    private void addProductRowTo(LinearLayout parent, Product product, boolean actions) {
        LinearLayout card = makeCard();

        TextView title = new TextView(this);
        title.setText(product.name);
        title.setTextColor(dark);
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        card.addView(title);

        if (product.lowStock) {
            TextView badge = new TextView(this);
            badge.setText("Stok menipis");
            badge.setTextColor(red);
            badge.setTextSize(11);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setBackgroundResource(R.drawable.bg_status_low);
            badge.setPadding(dp(8), dp(3), dp(8), dp(3));

            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            badgeParams.topMargin = dp(6);
            card.addView(badge, badgeParams);
        }

        String detail = "Stok: " + product.stock + " (min. " + product.minStock + ")"
                + "\nHarga: " + rupiah(product.price)
                + "\nKategori: " + (isBlank(product.categoryName) ? "-" : product.categoryName)
                + "  •  Supplier: " + (isBlank(product.supplierName) ? "-" : product.supplierName);

        TextView info = new TextView(this);
        info.setText(detail);
        info.setTextColor(grey);
        info.setTextSize(13);
        info.setPadding(0, dp(5), 0, 0);
        card.addView(info);

        if (actions) {
            LinearLayout buttons = new LinearLayout(this);
            buttons.setOrientation(LinearLayout.HORIZONTAL);
            buttons.setGravity(Gravity.END);

            Button edit = smallButton("Ubah");
            edit.setOnClickListener(v -> showProductDialog(product));
            Button delete = dangerButton("Hapus");
            delete.setOnClickListener(v -> confirmDeleteProduct(product));

            buttons.addView(edit);
            buttons.addView(delete, wrapStart(6));
            card.addView(buttons, matchWrap(10, 0));
        }

        parent.addView(card, matchWrap(0, 10));
    }

    private void addSummaryRow(LinearLayout parent, String label, String value, boolean highlight) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(grey);
        labelView.setTextSize(14);
        row.addView(labelView, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(highlight ? red : dark);
        valueView.setTextSize(18);
        valueView.setTypeface(null, Typeface.BOLD);
        row.addView(valueView);

        parent.addView(row, matchWrap(0, 0));
    }

    private void addDivider(LinearLayout parent) {
        View line = new View(this);
        line.setBackgroundColor(divider);
        parent.addView(line, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
    }

    private void addEmptyState(LinearLayout parent, String title, String detail) {
        LinearLayout card = makeCard();

        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextColor(dark);
        heading.setTextSize(15);
        heading.setTypeface(null, Typeface.BOLD);
        card.addView(heading);

        TextView text = new TextView(this);
        text.setText(detail);
        text.setTextColor(grey);
        text.setTextSize(13);
        text.setPadding(0, dp(4), 0, 0);
        card.addView(text);

        parent.addView(card, matchWrap(0, 10));
    }

    private void addHeading(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(dark);
        text.setTextSize(20);
        text.setTypeface(null, Typeface.BOLD);
        text.setPadding(0, dp(12), 0, dp(6));
        content.addView(text);
    }

    private void addText(String value, int color, int size, int top, int bottom) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(color);
        text.setTextSize(size);
        text.setPadding(0, dp(top), 0, dp(bottom));
        content.addView(text);
    }

    private void addTextTo(LinearLayout parent, String value, int color, int size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(color);
        text.setTextSize(size);
        text.setPadding(0, dp(6), 0, dp(6));
        parent.addView(text);
    }

    private void addFormLabel(LinearLayout form, String value) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextColor(dark);
        label.setTextSize(13);
        label.setTypeface(null, Typeface.BOLD);
        label.setPadding(0, dp(12), 0, dp(6));
        form.addView(label);
    }

    private EditText addField(LinearLayout form, String label, String hint) {
        addFormLabel(form, label);
        EditText input = field(hint);
        form.addView(input, matchWrap(0, 0));
        return input;
    }

    private EditText field(String hint) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(hint);
        input.setTextColor(dark);
        input.setHintTextColor(grey);
        input.setTextSize(15);
        input.setBackgroundResource(R.drawable.bg_input);
        input.setBackgroundTintList(null);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setMinHeight(dp(48));
        return input;
    }

    private LinearLayout newForm() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), 0);
        return form;
    }

    private Spinner styledSpinner() {
        Spinner spinner = new Spinner(this);
        spinner.setBackgroundResource(R.drawable.bg_spinner);
        spinner.setBackgroundTintList(null);
        spinner.setPadding(dp(12), dp(10), dp(44), dp(10));
        spinner.setMinimumHeight(dp(48));
        spinner.setPopupBackgroundDrawable(new ColorDrawable(surface));
        return spinner;
    }

    private void setSpinnerItems(Spinner spinner, ArrayList<String> labels) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private Button makeButton(String label) {
        Button button = baseButton(label);
        button.setTextSize(15);
        button.setTextColor(getColor(R.color.white));
        button.setBackgroundResource(R.drawable.bg_button_primary);
        button.setMinHeight(dp(48));
        button.setPadding(dp(18), 0, dp(18), 0);
        return button;
    }

    private Button smallButton(String label) {
        Button button = baseButton(label);
        button.setTextSize(13);
        button.setTextColor(dark);
        button.setBackgroundResource(R.drawable.bg_button_secondary);
        button.setMinHeight(dp(40));
        button.setPadding(dp(14), 0, dp(14), 0);
        return button;
    }

    private Button dangerButton(String label) {
        Button button = baseButton(label);
        button.setTextSize(13);
        button.setTextColor(red);
        button.setBackgroundResource(R.drawable.bg_button_danger);
        button.setMinHeight(dp(40));
        button.setPadding(dp(14), 0, dp(14), 0);
        return button;
    }

    private Button baseButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTypeface(null, Typeface.BOLD);
        button.setMinWidth(0);
        button.setBackgroundTintList(null);
        button.setStateListAnimator(null);
        return button;
    }

    private LinearLayout makeCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        return card;
    }

    private LinearLayout makeRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));
        return row;
    }

    private LinearLayout makeColumn() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        return column;
    }

    private LinearLayout.LayoutParams matchWrap(int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(top), 0, dp(bottom));
        return params;
    }

    private LinearLayout.LayoutParams wrapStart(int start) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(dp(start));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String rupiah(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"));
        format.setMaximumFractionDigits(0);
        return format.format(amount);
    }

    private int parseInt(String text, int fallback) {
        if (text == null || text.trim().isEmpty()) return fallback;
        return Integer.parseInt(text.trim());
    }

    private double parseDouble(String text, double fallback) {
        if (text == null || text.trim().isEmpty()) return fallback;
        return Double.parseDouble(text.trim());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
