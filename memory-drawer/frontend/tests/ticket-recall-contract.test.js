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
    suggestTicketSubtype,
    confirmTicketSubtypeAndGetQuestions,
    generateTicketRecallTitle,
} = await import("../src/api/ticketRecall.js");
const { setAccessToken } = await import("../src/utils/tokenStorage.js");
const {
    getTicketRecallFlow,
    removeTicketRecallFlow,
    saveTicketRecallFlow,
} = await import("../src/utils/ticketRecallStorage.js");

const successResponse = (data) => ({
    ok: true,
    status: 200,
    json: async () => ({ success: true, data }),
});

describe("API 6 ticket recall request contracts", () => {
    beforeEach(() => {
        sessionStorage.clear();
        setAccessToken("test-access-token");
    });

    afterEach(() => {
        globalThis.fetch = originalFetch;
        sessionStorage.clear();
    });

    it("API 6.1 sends POST without a request body", async () => {
        let captured;
        globalThis.fetch = async (url, options) => {
            captured = { url, options };
            return successResponse({
                suggestedTicketSubtype: "CONCERT_PERFORMANCE",
                requiresManualSelection: false,
                nextAction: "CONFIRM_TICKET_SUBTYPE",
            });
        };

        const result = await suggestTicketSubtype("draft-6-1");

        assert.equal(
            captured.url,
            "/api/memory-drafts/draft-6-1/ticket-recall/subtype-suggestion",
        );
        assert.equal(captured.options.method, "POST");
        assert.equal(captured.options.headers.Authorization, "Bearer test-access-token");
        assert.equal(captured.options.headers["Content-Type"], undefined);
        assert.equal(captured.options.body, undefined);
        assert.equal(result.suggestedTicketSubtype, "CONCERT_PERFORMANCE");
    });

    it("API 6.2 sends only the confirmed ticket subtype", async () => {
        let captured;
        globalThis.fetch = async (url, options) => {
            captured = { url, options };
            return successResponse({
                ticketSubtype: "CONCERT_PERFORMANCE",
                questions: [],
            });
        };

        await confirmTicketSubtypeAndGetQuestions(
            "draft-6-2",
            "CONCERT_PERFORMANCE",
        );

        assert.equal(
            captured.url,
            "/api/memory-drafts/draft-6-2/ticket-recall/questions",
        );
        assert.equal(captured.options.method, "POST");
        assert.equal(captured.options.headers["Content-Type"], "application/json");
        assert.deepEqual(JSON.parse(captured.options.body), {
            ticketSubtype: "CONCERT_PERFORMANCE",
        });
    });

    it("API 6.3 sends three question IDs and preserves unanswered null values", async () => {
        let captured;
        globalThis.fetch = async (url, options) => {
            captured = { url, options };
            return successResponse({ titleCandidate: "함께 부른 마지막 앙코르" });
        };

        const answers = [
            {
                questionId: "CONCERT_PERFORMANCE_1",
                answer: "마지막 곡을 함께 부르던 순간",
            },
            {
                questionId: "CONCERT_PERFORMANCE_2",
                answer: null,
            },
            {
                questionId: "CONCERT_PERFORMANCE_3",
                answer: "앙코르곡",
            },
        ];

        await generateTicketRecallTitle("draft-6-3", answers);

        assert.equal(
            captured.url,
            "/api/memory-drafts/draft-6-3/ticket-recall/title",
        );
        assert.equal(captured.options.method, "POST");
        const parsedBody = JSON.parse(captured.options.body);
        assert.deepEqual(parsedBody, { answers });
        assert.equal(parsedBody.answers.length, 3);
        assert.equal(parsedBody.answers[1].answer, null);
        assert.ok(parsedBody.answers.every((answer) => !("text" in answer)));
    });

    it("restores and clears draft-scoped in-progress recall state", () => {
        const flowState = {
            questionData: {
                ticketSubtype: "MOVIE",
                questions: [
                    { questionId: "MOVIE_1", order: 1, text: "질문" },
                ],
            },
            answers: [{ questionId: "MOVIE_1", answer: "작성 중인 답변" }],
        };

        saveTicketRecallFlow("draft-flow", flowState);
        assert.deepEqual(getTicketRecallFlow("draft-flow"), flowState);

        removeTicketRecallFlow("draft-flow");
        assert.equal(getTicketRecallFlow("draft-flow"), null);
    });

    it("discards corrupted in-progress recall state", () => {
        sessionStorage.setItem("memory-drawer-ticket-recall-flow-corrupt", "{not-json}");

        assert.equal(getTicketRecallFlow("corrupt"), null);
        assert.equal(
            sessionStorage.getItem("memory-drawer-ticket-recall-flow-corrupt"),
            null,
        );
    });
});
