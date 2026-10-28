# TH1 — Môi trường, hợp đồng dữ liệu và tích hợp dịch vụ SOAP kế thừa

| Tuần | Thời lượng | CLO | Sản phẩm nộp |
|---|---|---|---|
| 6 | 5 tiết (1 buổi tại phòng máy) | CLO2, CLO3 | Kho Git: XSD + ca kiểm chứng, client sinh từ WSDL, `legacy-catalog-adapter` + kiểm thử, CI xanh |

**Tình huống.** Bộ phận kho của BookStore đang chạy một dịch vụ SOAP cũ `BookCatalogService` (document/literal,
SOAP 1.1). Ứng dụng di động mới chỉ nói JSON/REST. Nhóm bạn không được sửa hệ cũ, chỉ được **đọc hợp đồng, gọi và bọc** nó bằng một tầng chống ăn mòn (anti-corruption layer) để lỗi và kiểu dữ liệu SOAP không "rò" sang hệ mới.


## Mục tiêu

1. Kiểm tra và chuẩn hoá môi trường JDK 21, Maven wrapper, Docker, Git (CLO7).
2. Đặc tả kiểu `Isbn13`, `Money`, `OrderNumber` bằng XML Schema và kiểm chứng tài liệu XML bằng `jakarta.xml.bind` (CLO2).
3. Đọc WSDL 1.1 (types, message, portType, binding, service) và thông điệp SOAP/`soap:Fault` thật (CLO2).
4. Sinh client từ WSDL bằng `jaxws-maven-plugin` 4.0.5, gọi `GetBook`/`SearchBooks`, ghi log thông điệp (CLO3).
5. Viết `legacy-catalog-adapter` (REST, cổng 8090) chuyển `soap:Fault` → Problem Details RFC 9457, có timeout (CLO3).
6. Viết kiểm thử tự động cho cả nhánh thành công và nhánh lỗi; `./mvnw -B verify` xanh trên CI (CLO3).

## Điều kiện chuẩn bị

> **Ubuntu (môi trường khuyến nghị ở phòng máy).** Cài JDK 21, Docker Engine + Compose v2, Git. Trên Ubuntu dùng nguyên văn các khối lệnh `bash`; chạy `chmod +x mvnw scripts/*.sh` rồi `./scripts/kiem_tra_moi_truong.sh` trước khi bắt đầu.

> **Chạy trên Windows PowerShell.** PowerShell không chạy tệp nằm trong thư mục hiện hành nếu thiếu tiền tố `.\`. Mọi lệnh `./mvnw …` trong bài viết thành `.\mvnw.cmd …`; mọi `./scripts/<tên>.sh` thành `.\scripts\<tên>.ps1`; biến môi trường dạng `TEN=giatri lenh` viết thành `$env:TEN="giatri"; lenh`. JDK phải là **21** (`java -version` báo 17 thì cài Temurin 21 và đặt lại `JAVA_HOME`, PATH, rồi mở lại cửa sổ PowerShell).


- JDK 21 (Eclipse Temurin), Docker Engine 24+/Docker Desktop (Windows dùng WSL2), Git 2.40+, một công cụ gọi API
  (curl, Postman, Bruno hoặc SoapUI). Maven **không cần cài** — dùng `./mvnw` (Windows PowerShell: `.\mvnw.cmd`).
- Đã đọc bài giảng tuần 2 (XML/XSD, SOAP, WSDL, mẫu tích hợp hệ kế thừa).
- Phòng máy: nên có bộ nhớ đệm `.m2` hoặc Nexus nội bộ; lần build đầu tải khoảng 150 MB thư viện.

## Cấu trúc kho (starter)

```
bookstore/
├── pom.xml                       # Spring Boot 4.1.2, JDK 21, các phiên bản ghim (mục 8 đặc tả)
├── bookstore-contracts/          # XSD BookStore + lớp JAXB sinh tự động + kiểm thử kiểm chứng
├── legacy-bookcatalog-soap/      # dịch vụ SOAP kế thừa — CUNG CẤP SẴN, không sửa (có Dockerfile)
├── legacy-catalog-adapter/       # REST bọc SOAP — phần bạn hiện thực
├── compose.legacy.yaml           # chạy dịch vụ SOAP bằng Docker (cổng 8091)
├── scripts/                      # kiểm tra môi trường, gọi SOAP bằng curl/PowerShell
└── .github/workflows/ci.yml      # ./mvnw -B verify
```

Các vị trí cần làm được đánh dấu `TODO(TH1-1)` … `TODO(TH1-8)`, khớp số bước trong tài liệu này. Tìm nhanh:

```bash
grep -rn "TODO(TH1-" --include=*.java --include=*.xsd --include=*.sh .
```

## Khung thời gian 5 tiết (250 phút)

| Thời lượng | Hoạt động | Ghi chú |
|---|---|---|
| 15 phút | Ôn nhanh lý thuyết liên quan | WSDL 5 thành phần, cấu trúc SOAP Fault, mẫu adapter/tầng chống ăn mòn (LT tuần 2) |
| 20 phút | PHẦN 1 — Môi trường, kho Git | TODO(TH1-1) |
| 35 phút | PHẦN 2 — XML Schema và kiểm chứng | TODO(TH1-2), TODO(TH1-3) |
| 30 phút | PHẦN 3 — Chạy và đọc dịch vụ SOAP kế thừa | Docker, curl |
| 35 phút | PHẦN 4 — Client sinh từ WSDL, log thông điệp | TODO(TH1-4), TODO(TH1-5) |
| 55 phút | PHẦN 5 — Adapter REST, dịch soap:Fault → RFC 9457 | TODO(TH1-6), TODO(TH1-7) |
| 25 phút | PHẦN 6 — Kiểm thử tự động, CI | TODO(TH1-8) |
| 20 phút | Vấn đáp/chấm tại chỗ (3 phút/sinh viên, chọn ngẫu nhiên câu hỏi ở cuối tài liệu) | Giảng viên mở CI của sinh viên |
| 15 phút | Review chéo pull request theo cặp + giao phần về nhà | Mỗi người để lại ≥ 2 nhận xét có căn cứ |


---

## PHẦN 1 — Môi trường và kho Git (20 phút)

**Bước 1.1.** Tạo kho Git của bạn từ `starter/` (giảng viên phát).

```bash
cd starter
git init -b main && git add . && git commit -m "TH1: khởi tạo từ starter"
git checkout -b th1
```

**Bước 1.2 — `TODO(TH1-1)`.** Mở `scripts/kiem_tra_moi_truong.sh`, bổ sung đoạn cảnh báo khi các cổng của học phần
(8080–8083, 8090, 8091, 8180, 8761, 8888) đang bị chiếm, rồi chạy:

```bash
chmod +x mvnw scripts/*.sh            # Linux/macOS (Windows bỏ qua)
./scripts/kiem_tra_moi_truong.sh
```

```powershell
# Windows PowerShell
.\scripts\kiem_tra_moi_truong.ps1
```

**Kết quả mong đợi:** mọi dòng `[OK]`, JDK ≥ 21 (script báo `[LỖI]` nếu JDK dưới 21 hoặc Docker Engine chưa chạy); nếu có `[CẢNH BÁO] cổng … đang bận` thì ghi lại để dùng biến
môi trường đổi cổng ở các bước sau (ví dụ `LEGACY_SOAP_PORT=18091`).

**Bước 1.3.** Build thử toàn bộ starter (chưa chạy kiểm thử):

```bash
./mvnw -q -DskipTests package          # Windows PowerShell: .\mvnw.cmd -q -DskipTests package
```

**Kết quả mong đợi:** lệnh kết thúc không lỗi; thư mục `bookstore-contracts/target/generated-sources/xjc` và
`legacy-catalog-adapter/target/generated-sources/wsimport` đã có mã Java sinh ra.

## PHẦN 2 — Hợp đồng dữ liệu XML Schema (35 phút)

**Bước 2.1.** Đọc `bookstore-contracts/src/main/resources/xsd/bookstore-catalog-v1.xsd` và
`bookstore-order-v1.xsd`. Đối chiếu với mục 9.3–9.4 của đặc tả: namespace `http://bookstore.edu.vn/catalog/v1`
(tiền tố `bs`) chứa `Book`, `Money`, `Isbn13`, `CurrencyCode`, `ErrorDetail`; namespace `…/order/v1` (`ord`)
chứa `Order`, `OrderLine`, `OrderStatus`.

**Bước 2.2 — `TODO(TH1-2)`.** Hoàn thiện ràng buộc:

- `Isbn13`: `xs:pattern` = `97[89][0-9]{10}`;
- `Amount` (dùng trong `Money`): `minInclusive 0`, `fractionDigits 2`;
- `OrderNumber`: `ORD-[0-9]{4}-[0-9]{6}` (ví dụ `ORD-2026-000417`).

**Bước 2.3 — `TODO(TH1-3)`.** Trong `XsdValidationTest`, bỏ `@Disabled` và bổ sung tệp mẫu
`order-invalid-status.xml` vào danh sách ca vi phạm. Chạy:

```bash
./mvnw -pl bookstore-contracts verify
```

**Kết quả mong đợi:**

```
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
```

Mỗi tệp `book-invalid-*.xml`/`order-invalid-*.xml` vi phạm **đúng một** ràng buộc; tệp `book-not-well-formed.xml`
báo lỗi *đúng dạng* (well-formed) chứ không phải lỗi *hợp lệ* (valid). Lớp `Book` sinh ra dùng `BigDecimal`
cho số tiền — không bao giờ dùng `double`.

> **Lưu ý kỹ thuật.** Bộ công cụ truyền thống dùng `javax.xml.bind` có sẵn trong JDK 8. Từ JDK 11 các gói này đã
> bị gỡ khỏi JDK; bộ hiện hành dùng `jakarta.xml.bind` 4.0 (thư viện `jaxb-runtime`) và plugin
> `org.jvnet.jaxb:jaxb-maven-plugin`. Khái niệm (XSD → lớp Java, marshal/unmarshal, kiểm chứng) không đổi.

## PHẦN 3 — Chạy và đọc dịch vụ SOAP kế thừa (30 phút)

**Bước 3.1.** Chạy `BookCatalogService` bằng Docker (ảnh build từ `legacy-bookcatalog-soap/Dockerfile`):

```bash
docker compose -f compose.legacy.yaml up -d --build
# cổng 8091 bận:  LEGACY_SOAP_PORT=18091 docker compose -f compose.legacy.yaml up -d --build
# Linux/macOS
curl -s "http://localhost:8091/ws/catalog?wsdl" | head -20
```

```powershell
docker compose -f compose.legacy.yaml up -d --build
curl.exe -s "http://localhost:8091/ws/catalog?wsdl" | Select-Object -First 20
```

**Bước 3.2.** Mở WSDL, xác định năm thành phần: `types` (nhập `bookstore-catalog-v1.xsd`), `message`,
`portType BookCatalogPortType` (phần trừu tượng), `binding BookCatalogSoapBinding` với
`soapAction="http://bookstore.edu.vn/catalog/GetBook"` (phần cụ thể), `service`/`port` với địa chỉ
`http://localhost:8091/ws/catalog`.

**Bước 3.3.** Gọi trực tiếp bằng thông điệp SOAP viết tay:

```bash
./scripts/goi_soap.sh GetBook scripts/soap/GetBook.xml
./scripts/goi_soap.sh GetBook scripts/soap/GetBook-notfound.xml
./scripts/goi_soap.sh SearchBooks scripts/soap/SearchBooks.xml
```

```powershell
.\scripts\goi_soap.ps1 GetBook scripts\soap\GetBook.xml
```

**Kết quả mong đợi** (rút gọn, lấy từ lần chạy thử của bộ học liệu):

```xml
<S:Envelope ...><S:Body><GetBookResponse xmlns="http://bookstore.edu.vn/catalog/messages/v1" ...>
  <book><ns2:isbn>9780134685991</ns2:isbn><ns2:title>Effective Java</ns2:title>…
  <ns2:price><ns2:amount>285000.00</ns2:amount><ns2:currency>VND</ns2:currency></ns2:price></book>
</GetBookResponse></S:Body></S:Envelope>

<S:Fault><faultcode>S:Client</faultcode><faultstring>Không tìm thấy sách ISBN 9789999999999</faultstring>
  <detail><bs:ErrorDetail><bs:code>BOOK_NOT_FOUND</bs:code>…</bs:ErrorDetail></detail></S:Fault>
```

Ghi lại: HTTP status của một `soap:Fault` là **500** dù lỗi do bên gọi (`soap:Client`). ISBN đặc biệt
`9790000000000` mô phỏng lỗi CSDL cũ (`soap:Server`, mã `BACKEND_ERROR`). Biến `LEGACY_DELAY_MS` làm dịch vụ
chậm có chủ đích (dùng ở PHẦN 5).

## PHẦN 4 — Client sinh từ WSDL và log thông điệp (35 phút)

**Bước 4.1.** Mở `legacy-catalog-adapter/pom.xml`, đọc cấu hình `jaxws-maven-plugin` (goal `wsimport`,
`packageName vn.edu.bookstore.legacy.adapter.soap.generated`, `wsdlLocation /wsdl/BookCatalogService.wsdl`).
WSDL được **sao vào kho** (như khi đối tác gửi hợp đồng), không tải qua mạng lúc build. Xem lớp sinh ra:

```bash
ls legacy-catalog-adapter/target/generated-sources/wsimport/vn/edu/bookstore/legacy/adapter/soap/generated/
```

**Kết quả mong đợi:** `BookCatalogPortType`, `BookCatalogService`, `BookCatalogFault` (ngoại lệ có mô hình,
`getFaultInfo()` trả `ErrorDetail`), `GetBookRequest/Response`, `SearchBooksRequest/Response`, `Book`, `Money`…

**Bước 4.2 — `TODO(TH1-4)`.** Trong `LegacySoapClientConfig`, ép `port` sang `BindingProvider` và đặt
`ENDPOINT_ADDRESS_PROPERTY` = `bookstore.legacy.endpoint`, `com.sun.xml.ws.connect.timeout` và
`com.sun.xml.ws.request.timeout` (mili giây) từ `LegacySoapProperties`. Không viết cứng địa chỉ.

**Bước 4.3 — `TODO(TH1-5)`.** Hoàn thiện `SoapLoggingHandler` (đọc `MESSAGE_OUTBOUND_PROPERTY`, ghi
`SOAPMessage.writeTo`) và gắn vào handler chain khi `bookstore.legacy.log-messages=true`. Chạy thử chế độ demo:

```bash
./mvnw -q -DskipTests install
./mvnw -pl legacy-catalog-adapter spring-boot:run -Dspring-boot.run.profiles=demo
```

```powershell
# Windows PowerShell: tham số -D có dấu chấm phải đặt trong nháy kép
.\mvnw.cmd -q -DskipTests install
.\mvnw.cmd -pl legacy-catalog-adapter spring-boot:run "-Dspring-boot.run.profiles=demo"
```

**Kết quả mong đợi:** log có các khối `SOAP → gửi:` và `SOAP ← nhận:` chứa nguyên văn Envelope, và dòng
`GetBook sách không tồn tại → Không tìm thấy sách ISBN 9789999999999`.

## PHẦN 5 — `legacy-catalog-adapter`: tầng chống ăn mòn REST → SOAP (55 phút)

**Bước 5.1 — `TODO(TH1-6)`.** Hiện thực `LegacyCatalogClient.getBook/search` và `LegacyBookController`:
`GET /api/v1/legacy/books/{isbn}` (kiểm tra mẫu ISBN **trước** khi gọi hệ cũ) và
`GET /api/v1/legacy/books?q=micro&limit=10`. Ánh xạ sang `BookView` — không trả lớp sinh từ WSDL ra ngoài.

**Bước 5.2 — `TODO(TH1-7)`.** Dịch lỗi theo bảng:

| Tình huống ở hệ cũ | Ngoại lệ phía client | HTTP | `type` RFC 9457 |
|---|---|---|---|
| `soap:Client` + `BOOK_NOT_FOUND` | `BookCatalogFault` | 404 | `https://api.bookstore.vn/problems/book-not-found` |
| `soap:Client` + `INVALID_ISBN` / `INVALID_REQUEST` | `BookCatalogFault` | 400 | `…/validation-failed` |
| `soap:Server` (`BACKEND_ERROR`, mã lạ) | `BookCatalogFault` / `SOAPFaultException` | 502 | `…/upstream-unavailable` |
| Hết thời gian chờ | `WebServiceException` (cause `SocketTimeoutException`) | 504 | `…/upstream-unavailable` |
| Không kết nối được | `WebServiceException` | 503 | `…/upstream-unavailable` |

Gợi ý: với fault **có mô hình** trong WSDL, Metro trả `BookCatalogFault` và **không giữ** `faultcode`; vì vậy
dựa vào mã nghiệp vụ trong `bs:ErrorDetail`. Thêm thuộc tính `legacyCode` vào Problem Details để truy vết.

**Bước 5.3.** Chạy adapter trỏ vào container SOAP và gọi thử:

```bash
java -jar legacy-catalog-adapter/target/legacy-catalog-adapter-1.0.0-SNAPSHOT.jar
# cổng SOAP khác: BOOKSTORE_LEGACY_ENDPOINT=http://localhost:18091/ws/catalog java -jar …
curl -s -w '\nHTTP %{http_code} %{content_type}\n' localhost:8090/api/v1/legacy/books/9780134685991
curl -s -w '\nHTTP %{http_code} %{content_type}\n' localhost:8090/api/v1/legacy/books/9789999999999
curl -s -w '\nHTTP %{http_code}\n' "localhost:8090/api/v1/legacy/books?q=micro"
```

```powershell
$env:BOOKSTORE_LEGACY_ENDPOINT = "http://localhost:8091/ws/catalog"
java -jar legacy-catalog-adapter\target\legacy-catalog-adapter-1.0.0-SNAPSHOT.jar
Invoke-RestMethod http://localhost:8090/api/v1/legacy/books/9780134685991
```

**Kết quả mong đợi** (đầu ra thật của bản lời giải):

```
{"isbn":"9780134685991","title":"Effective Java","author":"Joshua Bloch","authorId":42,"publishedYear":2018,
 "price":{"amount":285000.00,"currency":"VND"},"source":"legacy-soap"}
HTTP 200 application/json
{"detail":"Không tìm thấy sách ISBN 9789999999999","instance":"/api/v1/legacy/books/9789999999999","status":404,
 "title":"Không tìm thấy sách","type":"https://api.bookstore.vn/problems/book-not-found","legacyCode":"BOOK_NOT_FOUND"}
HTTP 404 application/problem+json
```

`/api/v1/legacy/books/12345` → 400 `validation-failed`; `/api/v1/legacy/books/9790000000000` → 502
`upstream-unavailable` với `legacyCode: BACKEND_ERROR`; `?q=micro` → 2 sách.

## PHẦN 6 — Kiểm thử tự động và CI (25 phút)

**Bước 6.1 — `TODO(TH1-8)`.** Bổ sung kiểm thử trong `LegacyBookControllerTest` cho các nhánh lỗi ở bước 5.2. Kiểm
thử khởi động **dịch vụ SOAP thật** ngay trong JVM (`LegacySoapTestServer`, cổng ngẫu nhiên) — không cần Docker.
Thêm hai lớp kiểm thử: hệ cũ chậm (dịch vụ SOAP trễ 2 s, `bookstore.legacy.read-timeout=500ms` → 504) và hệ cũ tắt
(trỏ tới cổng không có ai nghe → 503).

```bash
./mvnw -B verify
```

**Kết quả mong đợi:** `BUILD SUCCESS`; bản lời giải có 14 (contracts) + 6 (SOAP) + 9 (adapter) = 29 ca kiểm thử.

**Bước 6.2.** Đẩy nhánh, mở pull request `th1` → `main`; job `ci` trên GitHub Actions phải xanh. Dọn container:

```bash
# Lần đầu: tạo kho TRỐNG trên GitHub (không tích README), rồi khai báo origin
git remote add origin https://github.com/<tai-khoan>/<ten-kho>.git
git push -u origin main
git push -u origin th1
docker compose -f compose.legacy.yaml down
```



## Lỗi thường gặp

| Hiện tượng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `package jakarta.xml.bind does not exist` / dùng `javax.xml.bind` | Mã/ví dụ cũ cho JDK 8 | Dùng `jakarta.*`, thư viện `jaxb-runtime` 4.0.x |
| `wsimport` báo không tìm thấy `bookstore-catalog-v1.xsd` | WSDL nhập XSD theo đường dẫn tương đối | Đặt XSD cùng thư mục `src/main/resources/wsdl` |
| `fatal: 'origin' does not appear to be a git repository` khi `git push` | Kho trên máy chưa nối với kho GitHub | Tạo kho **trống** trên GitHub, rồi `git remote add origin https://github.com/<tai-khoan>/<ten-kho>.git`, `git push -u origin main`, `git push -u origin th1` |
| `head : The term 'head' is not recognized` (hoặc `grep`, `cat`, `chmod`) trong PowerShell | Gõ lệnh của khối `bash` (Linux/macOS) vào PowerShell | Dùng khối `powershell` ngay bên dưới, hoặc đổi `head -20` thành `Select-Object -First 20`, `grep` thành `Select-String` |
| `compose file "…\legacy-bookcatalog-soap\compose.legacy.yaml" is invalid … cannot find the file` | Chạy `docker compose` trong thư mục con | `cd ..` về thư mục `starter` (nơi có `compose.legacy.yaml`) rồi chạy lại |
| `Bind for 0.0.0.0:8091 failed: port is already allocated` | Cổng bận | `LEGACY_SOAP_PORT=18091 …` và `BOOKSTORE_LEGACY_ENDPOINT=…:18091/ws/catalog` |
| Adapter treo lâu rồi 500 | Chưa đặt timeout, chưa bắt `WebServiceException` | Làm lại `TODO(TH1-4)`, `TODO(TH1-7)` |
| `cvc-pattern-valid` với ISBN đúng | Có khoảng trắng/xuống dòng trong phần tử | Không định dạng giá trị bên trong phần tử khi viết mẫu XML |
| Kiểm thử 404 nhận 502 | Dựa vào `faultcode` thay vì mã `ErrorDetail` | Xem gợi ý bước 5.2 |
| Windows: `./mvnw: Permission denied` trong Git Bash/WSL | Mất quyền thực thi khi giải nén | `git update-index --chmod=+x mvnw` hoặc dùng `mvnw.cmd` |
| Windows: `mvnw.cmd : The term 'mvnw.cmd' is not recognized …` | PowerShell không chạy lệnh trong thư mục hiện hành khi thiếu `.\` | Gõ `.\mvnw.cmd -q -DskipTests package` |
| `[ERROR] No goals have been specified for this build` | Chạy `.\mvnw.cmd` mà không ghi goal (pha vòng đời) | Ghi đủ lệnh: `.\mvnw.cmd -q -DskipTests package` (các buổi sau: `.\mvnw.cmd verify`) |
| `Unknown lifecycle phase ".run.profiles=demo"` (PowerShell) | PowerShell cắt tham số `-D…` tại dấu chấm | Đặt trong nháy kép: `"-Dspring-boot.run.profiles=demo"` |
| `curl: option -w …` lỗi hoặc ra bảng PowerShell | Trong Windows PowerShell 5.1 `curl` là bí danh của `Invoke-WebRequest` | Gõ `curl.exe` |
| `java -version` báo 21 nhưng Maven vẫn báo `release version 21 not supported` | Maven dùng `JAVA_HOME` (còn trỏ JDK 17), không dùng `java` trong PATH | `$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21…'` (đúng tên thư mục trên máy), lưu lâu dài bằng `[Environment]::SetEnvironmentVariable('JAVA_HOME', $env:JAVA_HOME, 'User')`, mở lại PowerShell; kiểm tra `.\mvnw.cmd -v` phải báo `Java version: 21` |
| Chữ tiếng Việt trong kết quả script bị lỗi (`Kiá»ƒm tra mÃ´i…`) | Windows PowerShell 5.1 đọc tệp `.ps1` UTF-8 không BOM theo bảng mã ANSI | Dùng script bản cập nhật (UTF-8 có BOM) hoặc chạy bằng PowerShell 7 (`pwsh`) |
| `release version 21 not supported` / `invalid target release: 21` | Máy đang dùng JDK 17 | Cài Temurin 21, đặt `JAVA_HOME` và PATH trỏ tới JDK 21, mở lại terminal, kiểm `java -version` |
| Script báo `[LỖI] Docker Engine` | Docker Desktop chưa chạy (client có nhưng Engine tắt) | Mở Docker Desktop, đợi trạng thái *Engine running* rồi chạy lại script |

## Checklist chấm (thang 10, theo rubric mục 3.1)

| Tiêu chí (trọng số) | Kiểm tra cụ thể | Điểm tối đa |
|---|---|---|
| Chức năng và kiểm thử (40%) | CI xanh; XSD đủ 3 ràng buộc; adapter trả đúng 200/400/404/502/503/504; có ca kiểm thử nhánh lỗi | 4,0 |
| Hợp đồng và thiết kế dịch vụ (25%) | Không lộ lớp sinh từ WSDL ra REST; cấu hình ngoài (`bookstore.legacy.*`); XSD có namespace/kiểu tái sử dụng | 2,5 |
| Xử lý lỗi, bảo mật, độ bền (20%) | Problem Details đúng `type`, có `legacyCode`, có timeout; không in stack trace ra client | 2,0 |
| Vấn đáp và tài liệu (15%) | Trả lời được 2 câu hỏi; mô tả PR ghi rõ phần có hỗ trợ AI | 1,5 |


## Về nhà và mở rộng

1. Viết `docs/tich_hop_ke_thua.md` (½ trang): sơ đồ adapter, bảng ánh xạ lỗi, đề xuất lộ trình
   rút dần (strangler fig) để thay `BookCatalogService` bằng `catalog-service` (chuẩn bị cho TH2). Tiêu chí:
   có sơ đồ, nêu ≥ 2 rủi ro.
3.  Thêm binding JAXB để `orderDate` ánh xạ sang `OffsetDateTime` thay vì `XMLGregorianCalendar`.
4.  Thêm header WS-Security UsernameToken vào lời gọi SOAP bằng một `SOAPHandler` thứ hai và giải thích
   vì sao vẫn cần TLS.
