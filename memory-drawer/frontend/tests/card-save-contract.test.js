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
const { saveCard } = await import("../src/features/card-save/cardSaveApi.js");
const { setAccessToken } = await import("../src/utils/tokenStorage.js");

const successResponse = (data) => ({
    ok: true,
    status: 201,
    json: async () => ({ success: true, data }),
});

const captureRequest = () => {
    let captured;
    globalThis.fetch = async (url, options) => {
        captured = { url, options };
        return successResponse({ cardId: "saved-card" });
    };
    return () => captured;
};

const parseCardPart = async (formData) => {
    const cardPart = formData.get("card");
    assert.ok(cardPart instanceof Blob);
    assert.equal(cardPart.type, "application/json");
    return JSON.parse(await cardPart.text());
};

describe("API 7 card save multipart contracts", () => {
    beforeEach(() => {
        sessionStorage.clear();
        setAccessToken("test-access-token");
    });

    afterEach(() => {
        globalThis.fetch = originalFetch;
        sessionStorage.clear();
    });

    it("saves a DIRECT ticket as a card JSON part without ticketSubtype", async () => {
        const getCaptured = captureRequest();
        const back = {
            companions: ["현수"],
            weather: "맑음",
            mood: "벅참",
            writingMode: "DIRECT",
            title: "여름밤의 공연",
            memoryText: "마지막 앙코르까지 함께 노래했다.",
        };

        await saveCard({ draftId: "direct-draft", back, backPhotos: [] });

        const captured = getCaptured();
        assert.equal(captured.url, "/api/cards");
        assert.equal(captured.options.method, "POST");
        assert.equal(captured.options.headers.Authorization, "Bearer test-access-token");
        assert.equal(captured.options.headers["Content-Type"], undefined);
        assert.ok(captured.options.body instanceof FormData);
        assert.deepEqual(await parseCardPart(captured.options.body), {
            draftId: "direct-draft",
            back,
        });
        assert.equal(captured.options.body.getAll("backPhotos").length, 0);
        assert.ok(!("ticketSubtype" in back));
    });

    it("saves an AI_RECALL ticket with answer IDs and no memoryText", async () => {
        const getCaptured = captureRequest();
        const back = {
            companions: ["현수"],
            weather: "맑음",
            mood: "벅참",
            writingMode: "AI_RECALL",
            title: "함께 부른 마지막 앙코르",
            answers: [
                {
                    questionId: "CONCERT_PERFORMANCE_1",
                    answer: "마지막 곡을 함께 불렀다.",
                },
                {
                    questionId: "CONCERT_PERFORMANCE_2",
                    answer: null,
                },
                {
                    questionId: "CONCERT_PERFORMANCE_3",
                    answer: "앙코르곡이 기억에 남는다.",
                },
            ],
        };

        await saveCard({ draftId: "recall-draft", back, backPhotos: [] });

        const captured = getCaptured();
        const card = await parseCardPart(captured.options.body);
        assert.deepEqual(card, { draftId: "recall-draft", back });
        assert.equal(card.back.answers.length, 3);
        assert.equal(card.back.answers[1].answer, null);
        assert.ok(!("ticketSubtype" in card.back));
        assert.ok(!("memoryText" in card.back));
    });

    it("repeats the optional backPhotos multipart field", async () => {
        const getCaptured = captureRequest();
        const photos = [
            new Blob(["first"], { type: "image/jpeg" }),
            new Blob(["second"], { type: "image/png" }),
        ];

        await saveCard({
            draftId: "receipt-draft",
            back: {
                companions: [],
                weather: "맑음",
                mood: "행복",
                diaryText: "기억에 남는 식사",
            },
            backPhotos: photos,
        });

        const captured = getCaptured();
        const uploadedPhotos = captured.options.body.getAll("backPhotos");
        assert.equal(uploadedPhotos.length, 2);
        assert.deepEqual(
            uploadedPhotos.map((photo) => photo.type),
            ["image/jpeg", "image/png"],
        );
    });
});
