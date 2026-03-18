# DICOM Upload & Preview – Fixes Summary

## Backend (Spring Boot)

### 1. SecurityConfig.java
- **Path:** `src/main/java/com/xray/backend/security/SecurityConfig.java`
- **Changes:**
  - Added `/api/preview/**` and `/api/scan-info/**` to `permitAll()` so preview and scan-info work without JWT.
  - Added `/api/upload/**` for consistency.
  - Replaced `sendError()` in `AuthenticationEntryPoint` and `AccessDeniedHandler` with JSON body responses so API clients get `{"status":"error","message":"Unauthorized","errorCode":"UNAUTHORIZED"}` instead of the Whitelabel HTML page.

### 2. JwtAuthenticationFilter.java
- **Path:** `src/main/java/com/xray/backend/security/JwtAuthenticationFilter.java`
- **Changes:**
  - Skip JWT for `/api/scan-info` and `/error` so these paths are not blocked.

### 3. UploadController.java
- **Path:** `src/main/java/com/xray/backend/controller/UploadController.java`
- **Changes:**
  - Added `POST /api/upload` (same behavior as `/api/upload-legacy`) so the app can use `POST /api/upload` with multipart `file`.
  - Response includes `fileName` (stored name) – **use this for the preview URL**, not the original filename.

### 4. ErrorController.java (new)
- **Path:** `src/main/java/com/xray/backend/controller/ErrorController.java`
- **Purpose:** Explicit `GET /error` mapping that returns JSON so any forward to `/error` does not show the Whitelabel page.

### 5. ImageStorageService.java
- **Path:** `src/main/java/com/xray/backend/service/ImageStorageService.java`
- **Changes:**
  - Allowed extensions extended to `dcm`, `dcn`, `jpg`, `jpeg`, `png`.
  - DICOM preview generation (and `previewPath`) only for `dcm`/`dcn`; JPG/PNG are served as-is by the preview endpoint.

### Endpoints (no auth required)
- `GET /system/info` – system info (existing).
- `POST /api/upload` – multipart upload; returns `fileName` for preview URL (added/fixed).
- `GET /api/preview/{fileName}` – image/preview (existing; now permitted in security).
- `GET /api/scan-info/{fileName}` – scan info JSON (existing; now permitted).

---

## Frontend (React Native / Expo)

Reference implementation is under **`frontend/`** in this repo. Copy or adapt into your Expo app.

### 1. ApiConfig.ts
- **Path (in your app):** e.g. `src/config/ApiConfig.ts` or `frontend/ApiConfig.ts`
- Set `API_BASE_URL` to your backend (e.g. `http://<local-ip>:8080`).
- Use `getPreviewUrl(fileName)` with **encoded** filename: `encodeURIComponent(fileName)` is used inside.

### 2. apiService.ts
- **Path (in your app):** e.g. `src/services/apiService.ts`
- `uploadFile(uri, fileName, mimeType)` → calls `POST /api/upload`, returns `{ fileName, ... }`.
- **Important:** Use the returned `fileName` (stored name) for the preview URL, not the display/original name.

### 3. UploadScreen.tsx
- **Path (in your app):** e.g. `src/screens/UploadScreen.tsx`
- After upload, set preview state from `response.fileName` and pass that to `ScanPreviewCard` and `DicomViewer`.
- Preview URL is built as: `${API_BASE_URL}/api/preview/${encodeURIComponent(fileName)}`.
- Debug: `console.log("Preview URL:", previewUrl);` is included.

### 4. ScanPreviewCard.tsx
- **Path (in your app):** e.g. `src/components/ScanPreviewCard.tsx`
- Uses `<Image source={{ uri: previewUrl }} />` with the preview URL from `ApiConfig.getPreviewUrl(fileName)`.
- Handles loading and error state so “Loading preview…” disappears when the image loads or errors.

### 5. DicomViewer.tsx
- **Path (in your app):** e.g. `src/components/DicomViewer.tsx`
- WebView with `originWhitelist={["*"]}`, `javaScriptEnabled`, `domStorageEnabled`, and `source={{ html: viewerHtml }}`.
- Viewer HTML loads the **preview image** from the same preview URL (backend returns PNG for DICOM).
- Avoids “Viewer libraries did not load correctly” by not depending on external DICOM JS libs in the WebView; uses a simple image tag. For full DICOM tools (window/level, zoom, pan), you would load Cornerstone in the WebView separately.

### Filename encoding
- Always use the **stored** `fileName` from the upload response in the preview URL.
- Build URL as:  
  `const previewUrl = \`${API_BASE_URL}/api/preview/${encodeURIComponent(fileName)}\`;`

---

## Commands to restart and verify

### Backend
```bash
cd /Users/vyaspriyanshu/Desktop/backend
mvn spring-boot:run
```

### Verify (replace `<local-ip>` with your machine IP, e.g. `10.171.43.31`)
- System info:  
  `http://<local-ip>:8080/system/info`
- Preview (use a real stored filename from an upload):  
  `http://<local-ip>:8080/api/preview/<fileName>`

### Frontend (Expo)
```bash
cd <your-expo-app-root>
npx expo start
```
Set `API_BASE_URL` in ApiConfig to `http://<local-ip>:8080`.

---

## Test workflow

1. **Upload DICOM** – Select file → Upload → “Upload Successful” and response includes `fileName`.
2. **Preview loads** – Card shows the preview image (no endless “Loading preview…”).
3. **Viewer opens** – “Tap to view” opens the WebView with the same preview image.
4. **Zoom/pan** – If you add touch/gesture handling or a full DICOM viewer (e.g. Cornerstone) in the WebView, zoom/pan can work there.

---

## File paths to change (checklist)

**Backend (already updated in this repo):**
- `src/main/java/com/xray/backend/security/SecurityConfig.java`
- `src/main/java/com/xray/backend/security/JwtAuthenticationFilter.java`
- `src/main/java/com/xray/backend/controller/UploadController.java`
- `src/main/java/com/xray/backend/controller/ErrorController.java` (new)
- `src/main/java/com/xray/backend/service/ImageStorageService.java`

**Frontend (reference in `frontend/`; copy into your app and set API_BASE_URL):**
- `frontend/ApiConfig.ts`
- `frontend/apiService.ts`
- `frontend/UploadScreen.tsx`
- `frontend/ScanPreviewCard.tsx`
- `frontend/DicomViewer.tsx`
