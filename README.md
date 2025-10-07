## Stocks Dashboard (Spring Boot)

Layanan untuk menampilkan data harga saham dari file CSV, menyediakan API list (sort/filter), data grafik, dan ekspor PDF berisi tabel + grafik + meta filter.

### Sumber Data
- Letakkan file CSV di root proyek (contoh: `msequity.csv`, `trequity.csv`).
- Kolom wajib: `date,ticker,open,high,low,close,volume`.

### ENV
- File `.env` (opsional):
```
APP_PORT=8080
CSV_ROOT=.
EXPORT_DIR=./exports
PDF_TITLE=Stocks Dashboard
```
Konfigurasi juga bisa via `application.properties` atau environment variable.

### Menjalankan (Maven)
```bash
mvn spring-boot:run
# atau build jar
mvn -DskipTests package
java -jar target/*.jar
```

### Menjalankan (Docker)
Build image:
```bash
docker build -t stocks-dashboard:latest .
```
Jalankan dengan mount folder CSV (root proyek) ke dalam container `/data`:
```bash
docker run --rm -p 8080:8080 \
  -e APP_PORT=8080 \
  -e CSV_ROOT=/data \
  -e EXPORT_DIR=/app/exports \
  -e PDF_TITLE="Stocks Dashboard" \
  -v %cd%:/data \
  -v %cd%/exports:/app/exports \
  stocks-dashboard:latest
```
Catatan: Pada Linux/macOS ganti `%cd%` menjadi `$(pwd)`.

### Endpoint
- List Data (sort, filter):
  - `GET /api/stocks/list`
  - Query:
    - `tickers` (opsional, koma-separated, contoh: `BBRI,BBCA`)
    - `startDate` (opsional, ISO `yyyy-MM-dd`)
    - `endDate` (opsional, ISO `yyyy-MM-dd`)
    - `sortBy` (default `date`, opsi: `date,ticker,open,high,low,close,volume`)
    - `sortDir` (`asc|desc`, default `asc`)

- Data Grafik (line/multi-series Close):
  - `GET /api/stocks/chart`
  - Query: `tickers,startDate,endDate` (sama seperti di atas)
  - Response: `{ categories: string[], series: { [ticker]: number[] } }`

- Ekspor PDF (judul, tanggal, filter, grafik, tabel):
  - `GET /api/stocks/export/pdf`
  - Query: `tickers,startDate,endDate,sortBy,sortDir`
  - Menghasilkan file PDF dan menyimpan salinan ke folder `exports`.

### Empty/Error State
- Jika filter tidak menemukan baris, endpoint mengembalikan pesan yang jelas.


