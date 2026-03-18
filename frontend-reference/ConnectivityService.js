import axios from 'axios';

// Configuration
const CONFIG = {
    // IMPORTANT: When running on a real device, use your machine's LAN IP, not localhost!
    // Example: 'http://192.168.1.100:8080'
    // The user provided: 'http://10.171.43.31:8080'
    BASE_URL: 'http://10.171.43.31:8080', 
    TIMEOUT: 3000, // 3 seconds timeout for quick feedback
    POLL_INTERVAL: 5000 // Check every 5 seconds
};

/**
 * Connectivity Service
 * Handles heartbeat checks to the backend to verify "Online" status.
 */
class ConnectivityService {
    constructor() {
        this.isOnline = false;
        this.listeners = [];
        this.timer = null;
    }

    /**
     * Start the polling mechanism
     */
    startPolling() {
        this.checkStatus();
        this.timer = setInterval(() => this.checkStatus(), CONFIG.POLL_INTERVAL);
    }

    /**
     * Stop polling (e.g., when app goes to background)
     */
    stopPolling() {
        if (this.timer) {
            clearInterval(this.timer);
            this.timer = null;
        }
    }

    /**
     * Subscribe to status changes
     * @param {Function} callback - (isOnline) => void
     */
    subscribe(callback) {
        this.listeners.push(callback);
        // Immediately notify current status
        callback(this.isOnline);
        return () => {
            this.listeners = this.listeners.filter(cb => cb !== callback);
        };
    }

    notifyListeners(status) {
        if (this.isOnline !== status) {
            this.isOnline = status;
            this.listeners.forEach(cb => cb(this.isOnline));
            console.log(`[Connectivity] System is now ${status ? 'ONLINE' : 'OFFLINE'}`);
        }
    }

    async checkStatus() {
        try {
            // We use a dedicated, public health endpoint
            // This avoids 401/403 errors if the token is expired, 
            // ensuring we only measure network/server availability.
            const response = await axios.get(`${CONFIG.BASE_URL}/api/health`, {
                timeout: CONFIG.TIMEOUT,
                validateStatus: (status) => status === 200
            });

            if (response.data && response.data.status === 'ONLINE') {
                this.notifyListeners(true);
            } else {
                this.notifyListeners(false);
            }
        } catch (error) {
            // Detailed logging to diagnose "Real Device" issues
            console.error('[Connectivity] Check failed:', {
                message: error.message,
                code: error.code,
                url: `${CONFIG.BASE_URL}/api/health`
            });
            
            this.notifyListeners(false);
        }
    }
}

export const connectivityService = new ConnectivityService();
