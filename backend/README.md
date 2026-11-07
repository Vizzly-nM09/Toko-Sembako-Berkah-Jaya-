# Toko Sembako Berkah Jaya — PHP Backend

REST API untuk tugas kelompok Chapter 10. Backend ini melayani aplikasi
Android Toko Sembako dengan data produk, kategori, supplier, dashboard,
pencarian, filter, dan peringatan stok menipis.

## Prasyarat

- Podman container MySQL: `mysql-local` (sudah berjalan)
- PHP 8.3 dengan ekstensi `pdo_mysql`
- Python 3 (dipakai oleh `tests/smoke.sh` untuk membaca JSON)
- Android Studio + HP Android dengan USB debugging

## 1. Start MySQL

```bash
podman start mysql-local
podman ps
```

Container memakai volume `mysql_data`, jadi data tetap ada setelah container
dihentikan. Restart policy-nya kosong, jadi jalankan `podman start` setelah
reboot.

## 2. Buat database, user, dan tabel

File `backend/sql/00_setup.sql` berisi password user aplikasi dan
**di-ignore git**. Untuk mesin baru, salin contohnya dulu:

```bash
cp backend/sql/00_setup.example.sql backend/sql/00_setup.sql
```

Buka `backend/sql/00_setup.sql`, ganti `CHANGE_ME_APP_PASSWORD` dengan
password lokal pilihan Anda. Password yang sama harus dipakai di
`backend/api/config.php`.

Jalankan dari root repo (ganti `<ROOT_PASSWORD>` dengan password root
container MySQL Anda):

```bash
podman exec -i mysql-local mysql -uroot -p'<ROOT_PASSWORD>' < backend/sql/00_setup.sql
podman exec -i mysql-local mysql -uroot -p'<ROOT_PASSWORD>' < backend/sql/01_schema.sql
```

Atau buka Beekeeper Studio, connect ke `127.0.0.1:3306` sebagai `root`,
lalu jalankan isi kedua file SQL tersebut.

Verifikasi:

```bash
podman exec mysql-local mysql -uroot -p'<ROOT_PASSWORD>' -e "USE db_toko; SHOW TABLES; SELECT * FROM products;"
```

Database: `db_toko`. User aplikasi: `toko_app`@`%`. Password root container
tidak disimpan di repo; password user aplikasi hanya ada di file lokal yang
di-ignore.

## 3. Install PHP

```bash
sudo apt update
sudo apt install -y php php-mysql
php -v
php -m | grep -i pdo_mysql
```

Kalau `pdo_mysql` belum muncul, install juga `php8.3-mysql` (nama paket
mengikuti versi PHP yang terpasang).

## 4. Konfigurasi

`backend/api/config.php` sudah dibuat untuk lokal dan **di-ignore git**.
Untuk anggota kelompok lain, salin contohnya:

```bash
cp backend/api/config.example.php backend/api/config.php
```

Lalu sesuaikan `host`, `port`, `name`, `user`, dan `pass`. Nilai `pass` harus
sama dengan password yang Anda tulis di `backend/sql/00_setup.sql`.

## 5. Jalankan API

Dari root repo:

```bash
php -S 0.0.0.0:8000 -t backend
```

Base URL: `http://127.0.0.1:8000/api/`

Tes cepat:

```bash
curl -s http://127.0.0.1:8000/api/health.php
curl -s http://127.0.0.1:8000/api/dashboard.php
curl -s http://127.0.0.1:8000/api/products.php
```

## 6. Smoke test

Di terminal lain, selagi server berjalan:

```bash
bash backend/tests/smoke.sh
```

Script ini membuat kategori/supplier/produk sementara, menguji CRUD, filter,
low-stock, validasi error, dan menghapus datanya lagi.

## 7. USB debugging dari HP

Gunakan adb dari Android SDK, bukan `/usr/bin/adb` yang versinya lebih lama:

```bash
export PATH="$HOME/Android/Sdk/platform-tools:$PATH"
adb devices -l
adb reverse tcp:8000 tcp:8000
adb reverse --list
```

Buka di Chrome HP:

```text
http://127.0.0.1:8000/api/dashboard.php
```

Kalau JSON dashboard muncul, HP sudah terhubung ke backend lewat kabel USB.
`adb reverse` hilang setiap kali kabel dicabut atau adb di-restart, jadi
jalankan ulang setelah reconnect.

## 8. Base URL untuk Android

```java
private static final String BASE_URL = "http://127.0.0.1:8000/api/";
```

Tambahkan di `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />

<application
    ...
    android:usesCleartextTraffic="true">
```

`10.0.2.2` hanya untuk emulator. Untuk HP fisik dengan USB, pakai
`127.0.0.1` + `adb reverse`.

## Endpoint

| Method | Path | Keterangan |
|---|---|---|
| GET | `/api/health.php` | Cek koneksi database |
| GET | `/api/dashboard.php` | Ringkasan dashboard |
| GET | `/api/products.php` | List produk; support `search`, `category_id`, `supplier_id`, `stock_min`, `stock_max`, `low_stock=1` |
| POST | `/api/products.php` | Tambah produk |
| PUT | `/api/products.php?id=1` | Update produk |
| DELETE | `/api/products.php?id=1` | Hapus produk |
| GET | `/api/categories.php` | List kategori + jumlah produk |
| POST | `/api/categories.php` | Tambah kategori |
| PUT | `/api/categories.php?id=1` | Update kategori |
| DELETE | `/api/categories.php?id=1` | Hapus kategori |
| GET | `/api/suppliers.php` | List supplier + jumlah produk |
| POST | `/api/suppliers.php` | Tambah supplier |
| PUT | `/api/suppliers.php?id=1` | Update supplier |
| DELETE | `/api/suppliers.php?id=1` | Hapus supplier |

Contoh:

```bash
curl -s 'http://127.0.0.1:8000/api/products.php?low_stock=1'
curl -s -X POST http://127.0.0.1:8000/api/categories.php \
  -H 'Content-Type: application/json' \
  -d '{"name":"Snack"}'
```

## Troubleshooting

- **`config.php belum dibuat`** — copy `config.example.php` ke `config.php`.
- **`SQLSTATE[HY000] [2002] Connection refused`** — container MySQL belum
  jalan; `podman start mysql-local`.
- **`SQLSTATE[HY000] [1045] Access denied`** — cek user/password di
  `config.php`; pastikan `00_setup.sql` sudah dijalankan.
- **`The server requested authentication method unknown`** — PHP terlalu
  lama; PHP 8.3 mendukung `caching_sha2_password` yang dipakai MySQL 26.
- **HP tidak bisa connect** — cek `adb devices`, ulangi `adb reverse`, dan
  pastikan server PHP masih berjalan.
- **`CLEARTEXT communication not permitted`** — tambahkan
  `android:usesCleartextTraffic="true"` pada `<application>`.
