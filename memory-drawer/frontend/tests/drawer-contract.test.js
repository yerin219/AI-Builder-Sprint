import assert from "node:assert/strict";
import { afterEach, beforeEach, describe, it } from "node:test";

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

const originalFetch = globalThis.fetch;
const {
    fetchDrawers,
    fetchCardsByYear,
    fetchCardDetail,
} = await import("../src/features/drawer/drawerApi.js");
const { setAccessToken } = await import("../src/utils/tokenStorage.js");
const { resolveApiUrl } = await import("../src/api/http.js");
const {
    formatRelativeMemoryDate,
    getCardTitle,
    getDaysAgo,
    getImageUrl,
} = await import("../src/features/drawer/drawerViewUtils.js");

const successResponse = (data) => ({
    ok: true,
    status: 200,
    json: async () => ({ success: true, data }),
});

describe("API 8 drawer read contracts", () => {
    beforeEach(() => {
        sessionStorage.clear();
        setAccessToken("test-access-token");
    });

    afterEach(() => {
        globalThis.fetch = originalFetch;
        sessionStorage.clear();
    });

    it("loads the authenticated user's year drawers", async () => {
        let captured;
        const drawers = [{ year: 2026, cardCount: 2 }];
        globalThis.fetch = async (url, options) => {
            captured = { url, options };
            return successResponse({ drawers });
        };

        const result = await fetchDrawers();

        assert.equal(captured.url, "/api/drawers");
        assert.equal(captured.options.method, "GET");
        assert.equal(captured.options.headers.Authorization, "Bearer test-access-token");
        assert.equal(captured.options.body, undefined);
        assert.deepEqual(result, drawers);
    });

    it("loads cards from the selected year drawer", async () => {
        let captured;
        const data = {
            year: 2026,
            cards: [
                {
                    cardId: "card-1",
                    documentType: "TICKET",
                    memoryDate: "2026-07-25",
                    front: { eventName: "공연", frontImageUrl: "/files/cards/card-1/front" },
                },
            ],
        };
        globalThis.fetch = async (url, options) => {
            captured = { url, options };
            return successResponse(data);
        };

        const result = await fetchCardsByYear(2026);

        assert.equal(captured.url, "/api/drawers/2026/cards");
        assert.equal(captured.options.method, "GET");
        assert.deepEqual(result, data);
    });

    it("loads a card detail and URL-encodes its identifier", async () => {
        let captured;
        const detail = {
            cardId: "card/id",
            documentType: "TICKET",
            memoryDate: "2026-07-25",
            front: { eventName: "공연" },
            back: { writingMode: "DIRECT", title: "기억" },
        };
        globalThis.fetch = async (url, options) => {
            captured = { url, options };
            return successResponse(detail);
        };

        const result = await fetchCardDetail("card/id");

        assert.equal(captured.url, "/api/cards/card%2Fid");
        assert.equal(captured.options.method, "GET");
        assert.deepEqual(result, detail);
    });

    it("resolves protected image URLs through the API base path", () => {
        assert.equal(
            resolveApiUrl("/files/cards/card-1/front"),
            "/api/files/cards/card-1/front",
        );
        assert.throws(
            () => resolveApiUrl("https://cdn.example.com/card.jpg"),
            /외부 API URL은 허용되지 않습니다/,
        );
        assert.equal(getImageUrl("https://cdn.example.com/card.jpg"), null);
        assert.equal(getImageUrl("/files/cards/card-1/front"), "/files/cards/card-1/front");
    });

    it("preserves AbortError so route changes can cancel drawer requests", async () => {
        globalThis.fetch = async () => {
            throw new DOMException("aborted", "AbortError");
        };

        await assert.rejects(
            () => fetchDrawers({ signal: new AbortController().signal }),
            (error) => error?.name === "AbortError",
        );
    });
});

describe("memory date relative labels", () => {
    const augustFirst = new Date(2026, 7, 1, 12, 0, 0);

    it("calculates past and same-day memories from the device date", () => {
        assert.equal(getDaysAgo("2026-07-25", augustFirst), 7);
        assert.equal(getDaysAgo("2026-08-01", augustFirst), 0);
    });

    it("returns a negative value for a future memory and rejects invalid dates", () => {
        assert.equal(getDaysAgo("2026-08-03", augustFirst), -2);
        assert.equal(getDaysAgo("2026-02-30", augustFirst), null);
        assert.equal(getDaysAgo("not-a-date", augustFirst), null);
    });

    it("formats periods of at least 365 days as years and remaining days", () => {
        assert.equal(formatRelativeMemoryDate(334), "오늘로부터 334일 전");
        assert.equal(formatRelativeMemoryDate(365), "오늘로부터 1년 전");
        assert.equal(formatRelativeMemoryDate(399), "오늘로부터 1년 34일 전");
        assert.equal(formatRelativeMemoryDate(730), "오늘로부터 2년 전");
        assert.equal(formatRelativeMemoryDate(-399), "오늘로부터 1년 34일 후");
    });
});

describe("letter drawer text previews", () => {
    it("uses the normalized OCR text instead of an image title", () => {
        assert.equal(
            getCardTitle({
                documentType: "LETTER",
                front: { ocrText: "  사랑하는 어머니께\n오늘도 감사합니다.  " },
            }),
            "사랑하는 어머니께 오늘도 감사합니다.",
        );
    });
});
