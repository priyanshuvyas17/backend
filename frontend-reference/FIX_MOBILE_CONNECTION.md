# 📱 Mobile Connection Troubleshooting Guide

Your backend is correctly configured (`0.0.0.0:8080`) and the health endpoint is active. 
Since the Simulator works but the Real Device fails, the issue is **Network Security** or **Firewall**.

Follow these steps to fix the "Offline" status on your real device.

---

## 1. 🛡️ Firewall (Most Common Cause)
The macOS Firewall often blocks incoming connections from other devices (like your phone).

**Action:**
1. Open **System Settings** -> **Network** -> **Firewall**.
2. **Turn Off Firewall** temporarily.
3. Restart the backend server (`./mvnw spring-boot:run`).
4. Kill and restart the Expo app on your phone.

*If this works, you can turn the firewall back on and explicitly allow `java` or `mvn` connections.*

---

## 2. 📶 Wi-Fi Isolation
Your phone and computer MUST be on the **exact same Wi-Fi network**.
*   **Corporate/University Wi-Fi:** Often blocks device-to-device communication (Client Isolation).
*   **Action:** Use a **Personal Hotspot** or a private Home Wi-Fi.

---

## 3. 🔓 Cleartext Traffic (Android & iOS)
By default, modern mobile OSs block HTTP (non-HTTPS) traffic to IP addresses.

### **For Expo (app.json / app.config.js)**
Add this to your `app.json` to allow cleartext traffic:

```json
{
  "expo": {
    "plugins": [
      [
        "expo-build-properties",
        {
          "android": {
            "usesCleartextTraffic": true
          },
          "ios": {
            "useFrameworks": "static"
          }
        }
      ]
    ]
  }
}
```

### **For Bare React Native (Android)**
Edit `android/app/src/main/AndroidManifest.xml`:
```xml
<application
    ...
    android:usesCleartextTraffic="true">
```

### **For Bare React Native (iOS)**
Edit `ios/YourApp/Info.plist`:
```xml
<key>NSAppTransportSecurity</key>
<dict>
    <key>NSAllowsArbitraryLoads</key>
    <true/>
</dict>
```

---

## 4. 🍎 iOS Local Network Permission
On iOS 14+, the app needs permission to access devices on your local network.
*   **Action:** Go to **Settings -> Privacy & Security -> Local Network**. Ensure your Expo Go app (or your custom app) is **Enabled**.

---

## 5. 🔍 Verify Backend Visibility
Run this command on your Mac to confirm the server is reachable:
```bash
# Check if port 8080 is open to the world (*)
lsof -i :8080 | grep LISTEN
```
*Expected Output:* `TCP *:http-alt (LISTEN)` (The `*` is crucial).
