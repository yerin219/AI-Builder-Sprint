import {
    getAccessToken,
    removeAccessToken,
} from "../utils/tokenStorage.js";

const API_BASE_URL = import.meta.env?.VITE_API_BASE_URL || "/api";

export function resolveApiUrl(path) {
    if (/^https?:\/\//i.test(path)) {
        return path;
    }

    const baseUrl = API_BASE_URL.replace(/\/$/, "");
    const normalizedPath = path.startsWith("/") ? path : `/${path}`;
    if (normalizedPath === baseUrl || normalizedPath.startsWith(`${baseUrl}/`)) {
        return normalizedPath;
    }
    return `${baseUrl}${normalizedPath}`;
}

export class ApiError extends Error {
    constructor(status, code, message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}

export async function request(path, options = {}) {
    const {
        method = "GET",
        body,
        auth = true,
        signal,
    } = options;

    const headers = {};

    // 로그인 후 필요한 API에 토큰 자동 추가
    if (auth) {
        const token = getAccessToken();

        if (token) {
            headers.Authorization = `Bearer ${token}`;
        }
    }

    // FormData면 브라우저가 multipart boundary를 자동 설정해야 하므로
    // Content-Type을 직접 넣지 않음
    if (body && !(body instanceof FormData)) {
        headers["Content-Type"] = "application/json";
    }

    try {
        const response = await fetch(resolveApiUrl(path), {
            method,
            headers,
            signal,
            body:
                body instanceof FormData
                    ? body
                    : body
                        ? JSON.stringify(body)
                        : undefined,
        });

        const result = await response.json().catch(() => null);

        if (!response.ok || result?.success !== true) {
            if (response.status === 401) {
                removeAccessToken();
            }

            throw new ApiError(
                response.status,
                result?.code,
                result?.message || "요청 처리 중 오류가 발생했습니다.",
            );
        }

        return result?.data;
    } catch (error) {
        if (error instanceof ApiError) {
            throw error;
        }

        if (error?.name === "AbortError") {
            throw error;
        }

        throw new ApiError(
            0,
            "NETWORK_ERROR",
            "서버에 연결할 수 없습니다. 백엔드 실행 상태를 확인해주세요.",
        );
    }
}
