import { request } from "./http";

export function signup({ loginId, password }) {
    return request("/auth/signup", {
        method: "POST",
        auth: false,
        body: {
            loginId,
            password,
        },
    });
}

export function login({ loginId, password }) {
    return request("/auth/login", {
        method: "POST",
        auth: false,
        body: {
            loginId,
            password,
        },
    });
}