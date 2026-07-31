const TOKEN_KEY = "memory-drawer-access-token";

export function setAccessToken(token) {
    sessionStorage.setItem(TOKEN_KEY, token);
}

export function getAccessToken() {
    return sessionStorage.getItem(TOKEN_KEY);
}

export function removeAccessToken() {
    sessionStorage.removeItem(TOKEN_KEY);
}