import { request } from "./http.js";

export function signup({ email, password }) {
    return request("/auth/signup", {
        method: "POST",
        auth: false,
        body: {
            email: email.trim(),
            password,
        },
    });
}

export function login({ email, password }) {
    return request("/auth/login", {
        method: "POST",
        auth: false,
        body: {
            email: email.trim(),
            password,
        },
    });
}
