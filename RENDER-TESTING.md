# Render Deployment – Test Instructions

## URLs to Open in Browser (GET)

| URL | Expected Response |
|-----|-------------------|
| `https://your-app.onrender.com/` | `Backend is running 🚀` |
| `https://your-app.onrender.com/health` | `{"status":"UP","service":"backend","timestamp":"..."}` |
| `https://your-app.onrender.com/test` | `{"status":"OK","message":"Backend is reachable",...}` |
| `https://your-app.onrender.com/api/dicom/ping` | `DICOM API LIVE 🚀` |
| `https://your-app.onrender.com/welcome` | JSON with status, service, message |

## Endpoints Requiring POST (Postman / curl)

| Endpoint | Method | Content-Type | Example |
|----------|--------|--------------|---------|
| `/api/dicom/convert-and-store` | POST | multipart/form-data | See curl below |

## Example curl Commands

```bash
# Root (browser or curl)
curl https://your-app.onrender.com/

# Health check
curl https://your-app.onrender.com/health

# DICOM ping
curl https://your-app.onrender.com/api/dicom/ping

# DICOM convert-and-store (POST with form data)
curl -X POST https://your-app.onrender.com/api/dicom/convert-and-store \
  -F "file=@/path/to/image.png" \
  -F "patientName=John Doe" \
  -F "patientId=P001" \
  -F "studyUid=1.2.3.4.5" \
  -F "modality=XRAY" \
  -F "bodyPartExamined=CHEST" \
  -F "studyDate=2025-03-19"
```

## Local Testing (port 10000)

```bash
# Start app
./mvnw spring-boot:run

# Or with Docker
docker build -t xray-backend . && docker run -p 10000:10000 -e PORT=10000 xray-backend

# Test
curl http://localhost:10000/
curl http://localhost:10000/health
curl http://localhost:10000/api/dicom/ping
```

## React Native / Mobile App

- CORS allows all origins and methods.
- Use the same base URL as above.
- GET endpoints work from `fetch()` or `axios`.
- POST `/api/dicom/convert-and-store` requires `FormData` with the listed fields.
