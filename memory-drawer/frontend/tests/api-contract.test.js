import assert from "node:assert/strict";
import { beforeEach, describe, it } from "node:test";

class MemoryStorage {
    constructor() {
        this.values = new Map();
    }

    getItem(key) {
        return this.values.get(key) ?? null;
    }

    setItem(key, value) {
        this.values.set(key, String(value));
    }

    removeItem(key) {
        this.values.delete(key);
    }

    clear() {
        this.values.clear();
    }
}

globalThis.sessionStorage = new MemoryStorage();

const { analyzeImage, confirmDocumentType, confirmFront } = await import(
    "../src/api/drafts.js"
);
const { login } = await import("../src/api/auth.js");
const { ApiError } = await import("../src/api/http.js");
const { setAccessToken, getAccessToken } = await import(
    "../src/utils/tokenStorage.js"
);

const successResponse = (data) => ({
    ok: true,
    status: 200,
    json: async () => ({ success: true, data }),
});

describe("API 3·4·5 request contracts", () => {
    beforeEach(() => {
        sessionStorage.clear();
        setAccessToken("test-access-token");
    });

    it("API 3 sends POST multipart/form-data with the image part", async () => {
        let captured;
        globalThis.fetch = async (url, options) => {
            captured = { url, options };
            return successResponse({ draftId: "draft-3" });
        };

        const image = new Blob(["image-data"], { type: "image/jpeg" });
        const result = await analyzeImage(image);

        assert.equal(captured.url, "/api/memory-drafts/analyze");
        assert.equal(captured.options.method, "POST");
        assert.equal(captured.options.headers.Authorization, "Bearer test-access-token");
        assert.equal(captured.options.headers["Content-Type"], undefined);
        assert.ok(captured.options.body instanceof FormData);
        assert.equal(captured.options.body.get("image").size, image.size);
        assert.deepEqual(result, { draftId: "draft-3" });
    });

    it("API 4 sends the selected document type to the draft endpoint", async () => {
        let captured;
        globalThis.fetch = async (url, options) => {
            captured = { url, options };
            return successResponse({ draftStatus: "FRONT_PENDING" });
        };

        await confirmDocumentType("draft-4", "TICKET");

        assert.equal(
            captured.url,
            "/api/memory-drafts/draft-4/document-type/confirm",
        );
        assert.equal(captured.options.method, "POST");
        assert.equal(captured.options.headers["Content-Type"], "application/json");
        assert.deepEqual(JSON.parse(captured.options.body), {
            documentType: "TICKET",
        });
    });

    it("API 5 sends PUT with the final front values", async () => {
        let captured;
        globalThis.fetch = async (url, options) => {
            captured = { url, options };
            return successResponse({
                draftStatus: "FRONT_CONFIRMED",
                nextAction: "WRITE_BACK",
            });
        };

        const payload = {
            memoryDate: "2026-07-12",
            front: {
                eventName: "공연",
                venue: "부산",
                seat: null,
            },
        };

        const result = await confirmFront("draft-5", payload);

        assert.equal(
            captured.url,
            "/api/memory-drafts/draft-5/front/confirm",
        );
        assert.equal(captured.options.method, "PUT");
        assert.deepEqual(JSON.parse(captured.options.body), payload);
        assert.equal(result.draftStatus, "FRONT_CONFIRMED");
        assert.equal(result.nextAction, "WRITE_BACK");
    });
});

describe("authentication and common error contracts", () => {
    beforeEach(() => {
        sessionStorage.clear();
    });

    it("sends the API-spec email field without an auth header", async () => {
        let captured;
        setAccessToken("must-not-be-sent");
        globalThis.fetch = async (url, options) => {
            captured = { url, options };
            return successResponse({ accessToken: "issued-token" });
        };

        await login({ email: "  user@example.com ", password: "1234567890" });

        assert.equal(captured.url, "/api/auth/login");
        assert.equal(captured.options.headers.Authorization, undefined);
        assert.deepEqual(JSON.parse(captured.options.body), {
            email: "user@example.com",
            password: "1234567890",
        });
    });

    it("removes the stored token and exposes the error code on 401", async () => {
        setAccessToken("expired-token");
        globalThis.fetch = async () => ({
            ok: false,
            status: 401,
            json: async () => ({
                success: false,
                code: "AUTH_001",
                message: "인증 토큰이 없거나 유효하지 않습니다.",
            }),
        });

        await assert.rejects(
            () => confirmDocumentType("draft", "RECEIPT"),
            (error) =>
                error instanceof ApiError &&
                error.status === 401 &&
                error.code === "AUTH_001",
        );
        assert.equal(getAccessToken(), null);
    });
});
