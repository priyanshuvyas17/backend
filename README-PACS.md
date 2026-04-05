# X-Ray Pro 5000 - Hospital PACS Workflow System

Complete radiology workflow: Patient Registration → Study Creation → Acquisition Console → Image Capture → DICOM Conversion → Orthanc PACS → OHIF Viewer.

## Architecture

```
React Native App (X-Ray Pro 5000)
        ↓
Spring Boot Backend API
        ↓
DICOM Processing Service (dcm4che)
        ↓
Orthanc PACS Server
        ↓
DICOM Storage
        ↓
OHIF Viewer
```

## Prerequisites

- **Java 17**
- **PostgreSQL** (or MySQL with `spring.profiles.active=mysql`)
- **Orthanc PACS** - [Download](https://www.orthanc-server.com/download.php) or run via Docker
- **Node.js 18+** (for React Native app)

## Quick Start

### 1. Start Orthanc PACS

```bash
# Using Docker (recommended)
docker run -p 8042:8042 -p 4242:4242 jodogne/orthanc

# Or install Orthanc and run locally
# Default: http://localhost:8042
```

### 2. Create PostgreSQL Database

```sql
CREATE DATABASE xray_pacs;
CREATE USER xray_user WITH PASSWORD 'xray_pass';
GRANT ALL PRIVILEGES ON DATABASE xray_pacs TO xray_user;
```

Update `application.properties` if needed:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/xray_pacs
spring.datasource.username=xray_user
spring.datasource.password=xray_pass
```

### 3. Start Spring Boot Backend

```bash
cmvn spring-boot:rund backend

```

Backend runs at **http://localhost:8080**

### 4. Start React Native Mobile App

```bash
cd xray-pro-mobile
npm start
```

Then press `i` for iOS or `a` for Android. Update `src/config/ApiConfig.ts` with your machine's IP for device/emulator access:
```typescript
export const API_BASE_URL = 'http://YOUR_IP:8080';
```

## API Endpoints

| Method | Endpoint | Description |
|-------|----------|-------------|
| POST | `/api/patients` | Register patient |
| GET | `/api/patients` | List patients |
| GET | `/api/patients/{uid}/records` | Patient records |
| POST | `/api/studies` | Create study |
| POST | `/api/studies/{uid}/finish` | Finish study |
| POST | `/api/dicom/convert-and-store` | Convert image to DICOM, store in PACS |
| GET | `/api/pacs/status` | PACS health check |

## Radiology Workflow

1. **Patient Registration** - Enter Patient Name, ID, Age, Gender, Body Part, Study Type → POST /api/patients
2. **Create Study** - Auto-generate Study UID, link to Patient → POST /api/studies
3. **Acquisition Console** - Select exposure plan (Skull PA, Chest PA, Nasal Bones), view kVp/mAs/DAP
4. **Expose** - Simulate capture (camera or gallery)
5. **Image Preview** - Retake or Accept
6. **Accept** - Attach metadata, convert to DICOM, store in Orthanc
7. **Finish Study** - Mark completed, navigate to Patient Records
8. **PACS Viewer** - OHIF with Zoom, Pan, Window Level, Measurements

## Error Handling

- **PACS not reachable** - 503, message: "PACS is not reachable"
- **No studies found** - Returns `{ "message": "No studies available.", "records": [] }`
- **DICOM conversion failure** - 500, errorCode: DICOM_CONVERSION_FAILURE
- **Network timeout** - 503, Orthanc timeout configurable via `orthanc.timeout-seconds`

## Database Tables

- `patients` - patient_uid, patient_name, age, gender, body_part_examined, study_type
- `studies` - study_instance_uid, patient_id, modality, study_date, study_status
- `series` - series_instance_uid, study_id, body_part_examined, modality
- `images` - sop_instance_uid, series_id, file_path, file_size

## OHIF Viewer Setup

For full DICOM viewer with Zoom/Pan/Window Level:

1. Install [Orthanc DICOMweb plugin](https://book.orthanc-server.com/plugins/dicom-web.html)
2. DICOMweb URL: `http://localhost:8042/dicom-web`
3. OHIF viewer: `https://viewer.ohif.org/viewer?url=http://localhost:8042/dicom-web`

## Configuration

```properties
# application.properties
orthanc.url=http://localhost:8042
orthanc.timeout-seconds=30
xray.upload-dir=uploads
```
