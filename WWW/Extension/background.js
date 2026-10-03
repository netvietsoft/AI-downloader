/**
 * Background Service Worker: AI Video Downloader Extension
 */

const BACKEND_URL = 'http://localhost:5101';

chrome.runtime.onInstalled.addListener(() => {
    console.log('[AIDownloader Extension] Service Worker installed successfully!');
});

// Lắng nghe thông điệp từ Content Script hoặc Popup
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    if (request.type === 'GET_USER_STATUS') {
        chrome.storage.local.get(['userToken', 'userEmail'], async (data) => {
            try {
                const res = await fetch(`${BACKEND_URL}/api/extension/status?userId=${data.userToken || ''}&email=${data.userEmail || ''}`);
                const status = await res.json();
                sendResponse({ success: true, data: status });
            } catch (err) {
                sendResponse({ success: false, error: err.message });
            }
        });
        return true; // Trả lời bất đồng bộ
    }

    if (request.type === 'SET_AUTH_DATA') {
        chrome.storage.local.set({
            userToken: request.token,
            userEmail: request.email,
            userName: request.name,
            isPro: request.isPro
        }, () => {
            sendResponse({ success: true });
        });
        return true;
    }

    if (request.type === 'CLEAR_AUTH') {
        chrome.storage.local.clear(() => {
            sendResponse({ success: true });
        });
        return true;
    }
});
