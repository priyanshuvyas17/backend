# X-Ray Pro 5000 — Debugging Guide

## 1. Find Your Laptop's Local IP

### Mac
- **System Settings** → **Network** → **Wi-Fi** → **Details** → look for "IP Address"
- Or terminal: `ipconfig getifaddr en0` (Wi-Fi) or `ifconfig | grep "inet "`

### Windows
- **Settings** → **Network & Internet** → **Wi-Fi** → **Properties** → "IPv4 address"
- Or: `ipconfig` in CMD

### Linux
- `ip addr` or `hostname -I`

Example: `192.168.1.100`

---

## 2. Update Frontend API URL

Edit `xray-pro-mobile/src/config/api.ts`:

```typescript
export const API_BASE_URL = 'http://YOUR_IP:8080';  // e.g. http://192.168.1.100:8080
```

Or set env before starting Expo:
```bash
EXPO_PUBLIC_API_URL=http://192.168.1.100:8080 npx expo start
```

---

## 3. Test Backend from Phone Browser

1. Ensure phone and laptop are on the **same Wi-Fi**
2. On your phone, open browser and go to:
   - `http://YOUR_IP:8080/health` → should show `{"status":"OK"}`
   - `http://YOUR_IP:8080/test` → should show JSON with timestamp
3. If it fails: check firewall, ensure backend is running

---

## 4. Firewall (Allow Port 8080)

### Mac
```bash
# Allow Java/Spring Boot through firewall
System Settings → Network → Firewall → Options → Add Java/your IDE
# Or temporarily disable for testing
```

### Windows
```powershell
netsh advfirewall firewall add rule name="XRay Backend" dir=in action=allow protocol=TCP localport=8080
```

### Linux
```bash
sudo ufw allow 8080
sudo ufw reload
```

---

## 5. Backend Logs

Every request is logged:
```
[REQUEST] GET /health | IP: 192.168.1.105 | 200 application/json | 12ms
[REQUEST] GET /pacs/patients | IP: 192.168.1.105 | 200 application/json | 45ms
[PACS] getPatients failed: Connection refused
```

View logs:
- Terminal where `mvn spring-boot:run` is running
- Or check `logging.level.com.xray=DEBUG` in application.properties

---

## 6. Verify Backend Config

| Setting | Value | Purpose |
|---------|-------|---------|
| `server.address` | `0.0.0.0` | Bind to all interfaces (not just localhost) |
| `server.port` | `8080` | Backend port |
| CORS | `*` | Allow mobile app origins |

---

## 7. Connection Test Screen

In the app: **Patient Registration** → **Connection Test**

- Tests `GET /health` and `GET /test`
- Shows ✓ green if backend reachable, ✗ red if not
- Use **Retry** after fixing network

---

## 8. Common Issues

| Issue | Fix |
|-------|-----|
| "Network request failed" | Wrong IP in api.ts, or firewall blocking |
| "PACS Offline" | Orthanc not running, or wrong orthanc.url |
| App crashes on load | Check ErrorBoundary; ensure no direct Orthanc calls |
| CORS error | Backend CORS allows `*`; restart backend |
| 401 Unauthorized | Security is disabled; check SecurityConfig |

---

## 9. Quick Checklist

- [ ] Laptop and phone on same Wi-Fi
- [ ] Backend running: `mvn spring-boot:run`
- [ ] `http://YOUR_IP:8080/health` works in phone browser
- [ ] `api.ts` has correct IP
- [ ] Firewall allows port 8080
- [ ] Connection Test screen shows ✓
