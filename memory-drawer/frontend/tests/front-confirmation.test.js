import assert from "node:assert/strict";
import { describe, it } from "node:test";
import {
    buildFrontPayload,
    createFrontForm,
    getNextPathAfterFrontConfirmation,
    validateFrontForm,
} from "../src/utils/frontConfirmation.js";

const baseForm = {
    memoryDate: "2026-07-12",
    storeName: "상점",
    eventName: "공연",
    venue: "공연장",
    seat: "",
    ocrText: "편지 내용",
};

describe("API 5 type-specific payloads", () => {
    it("builds only the receipt fields", () => {
        assert.deepEqual(buildFrontPayload("RECEIPT", baseForm), {
            memoryDate: "2026-07-12",
            front: { storeName: "상점" },
        });
    });

    it("converts an empty ticket seat to null", () => {
        assert.deepEqual(buildFrontPayload("TICKET", baseForm), {
            memoryDate: "2026-07-12",
            front: {
                eventName: "공연",
                venue: "공연장",
                seat: null,
            },
        });
    });

    it("does not send frontImageMode for a letter", () => {
        const payload = buildFrontPayload("LETTER", baseForm);

        assert.deepEqual(payload, {
            memoryDate: "2026-07-12",
            front: { ocrText: "편지 내용" },
        });
        assert.equal("frontImageMode" in payload.front, false);
    });
});

describe("API 4 candidate normalization", () => {
    it("shows null extraction values as empty editable fields", () => {
        assert.deepEqual(
            createFrontForm({
                memoryDate: null,
                eventName: "공연",
                venue: null,
                seat: null,
            }),
            {
                memoryDate: "",
                storeName: "",
                eventName: "공연",
                venue: "",
                seat: "",
                ocrText: "",
            },
        );
    });
});

describe("API 5 success routing", () => {
    it("routes tickets to the back-mode choice", () => {
        assert.equal(
            getNextPathAfterFrontConfirmation("TICKET", "draft-id"),
            "/memories/draft-id/back",
        );
    });

    it("routes receipts and letters to card saving", () => {
        assert.equal(
            getNextPathAfterFrontConfirmation("RECEIPT", "receipt-id"),
            "/memories/receipt-id/save",
        );
        assert.equal(
            getNextPathAfterFrontConfirmation("LETTER", "letter-id"),
            "/memories/letter-id/save",
        );
    });
});

describe("API 5 required-field validation", () => {
    it("rejects whitespace-only receipt store names", () => {
        assert.equal(
            validateFrontForm("RECEIPT", { ...baseForm, storeName: "   " }),
            "상호명을 입력해주세요.",
        );
    });

    it("requires both ticket event name and venue", () => {
        assert.equal(
            validateFrontForm("TICKET", { ...baseForm, eventName: "" }),
            "행사명을 입력해주세요.",
        );
        assert.equal(
            validateFrontForm("TICKET", { ...baseForm, venue: "" }),
            "장소를 입력해주세요.",
        );
    });

    it("requires a memory date and letter text", () => {
        assert.equal(
            validateFrontForm("LETTER", { ...baseForm, memoryDate: "" }),
            "날짜를 입력해주세요.",
        );
        assert.equal(
            validateFrontForm("LETTER", { ...baseForm, ocrText: "" }),
            "손편지 내용을 입력해주세요.",
        );
    });
});
