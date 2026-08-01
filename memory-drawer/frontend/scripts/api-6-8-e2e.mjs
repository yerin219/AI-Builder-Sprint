import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { basename, extname } from "node:path";
import { deflateSync } from "node:zlib";

const apiBaseUrl = process.env.E2E_API_BASE_URL || "http://localhost:18080/api";
const imagePath = process.env.E2E_IMAGE_PATH;
const imageMimeTypes = {
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".png": "image/png",
    ".webp": "image/webp",
};
const imageMimeType = imagePath
    ? imageMimeTypes[extname(imagePath).toLowerCase()]
    : "image/png";

if (imagePath && !imageMimeType) {
    throw new Error("E2E 이미지는 JPEG, PNG 또는 WebP 형식이어야 합니다.");
}

const imageBytes = imagePath
    ? await readFile(imagePath)
    : createSyntheticTicketPng();
const imageName = imagePath ? basename(imagePath) : "synthetic-ticket.png";

function createSyntheticTicketPng() {
    const glyphs = {
        "0": ["01110", "10001", "10011", "10101", "11001", "10001", "01110"],
        "1": ["00100", "01100", "00100", "00100", "00100", "00100", "01110"],
        "2": ["01110", "10001", "00001", "00010", "00100", "01000", "11111"],
        "3": ["11110", "00001", "00001", "01110", "00001", "00001", "11110"],
        "6": ["00110", "01000", "10000", "11110", "10001", "10001", "01110"],
        "7": ["11111", "00001", "00010", "00100", "01000", "01000", "01000"],
        "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
        "B": ["11110", "10001", "10001", "11110", "10001", "10001", "11110"],
        "C": ["01111", "10000", "10000", "10000", "10000", "10000", "01111"],
        "D": ["11110", "10001", "10001", "10001", "10001", "10001", "11110"],
        "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
        "I": ["11111", "00100", "00100", "00100", "00100", "00100", "11111"],
        "K": ["10001", "10010", "10100", "11000", "10100", "10010", "10001"],
        "N": ["10001", "11001", "10101", "10011", "10001", "10001", "10001"],
        "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
        "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
        "S": ["01111", "10000", "10000", "01110", "00001", "00001", "11110"],
        "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
        "U": ["10001", "10001", "10001", "10001", "10001", "10001", "01110"],
        "V": ["10001", "10001", "10001", "10001", "10001", "01010", "00100"],
        "-": ["00000", "00000", "00000", "11111", "00000", "00000", "00000"],
        " ": ["00000", "00000", "00000", "00000", "00000", "00000", "00000"],
    };
    const width = 800;
    const height = 320;
    const scale = 5;
    const pixels = Buffer.alloc(width * height * 3, 255);

    const drawText = (text, startX, startY) => {
        let x = startX;
        for (const character of text) {
            const glyph = glyphs[character] || glyphs[" "];
            glyph.forEach((row, rowIndex) => {
                [...row].forEach((pixel, columnIndex) => {
                    if (pixel !== "1") return;
                    for (let dy = 0; dy < scale; dy += 1) {
                        for (let dx = 0; dx < scale; dx += 1) {
                            const px = x + columnIndex * scale + dx;
                            const py = startY + rowIndex * scale + dy;
                            const offset = (py * width + px) * 3;
                            pixels[offset] = 15;
                            pixels[offset + 1] = 23;
                            pixels[offset + 2] = 42;
                        }
                    }
                });
            });
            x += 6 * scale;
        }
    };

    drawText("TICKET CONCERT", 70, 45);
    drawText("DATE 2026-07-31", 70, 120);
    drawText("VENUE BUSAN", 70, 195);

    const raw = Buffer.alloc((width * 3 + 1) * height);
    for (let row = 0; row < height; row += 1) {
        const targetOffset = row * (width * 3 + 1);
        raw[targetOffset] = 0;
        pixels.copy(raw, targetOffset + 1, row * width * 3, (row + 1) * width * 3);
    }

    const ihdr = Buffer.alloc(13);
    ihdr.writeUInt32BE(width, 0);
    ihdr.writeUInt32BE(height, 4);
    ihdr[8] = 8;
    ihdr[9] = 2;
    return Buffer.concat([
        Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
        pngChunk("IHDR", ihdr),
        pngChunk("IDAT", deflateSync(raw)),
        pngChunk("IEND", Buffer.alloc(0)),
    ]);
}

function pngChunk(type, data) {
    const typeBytes = Buffer.from(type, "ascii");
    const length = Buffer.alloc(4);
    length.writeUInt32BE(data.length);
    const crc = Buffer.alloc(4);
    crc.writeUInt32BE(crc32(Buffer.concat([typeBytes, data])));
    return Buffer.concat([length, typeBytes, data, crc]);
}

function crc32(bytes) {
    let crc = 0xffffffff;
    for (const byte of bytes) {
        crc ^= byte;
        for (let bit = 0; bit < 8; bit += 1) {
            crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0);
        }
    }
    return (crc ^ 0xffffffff) >>> 0;
}

async function callApi(
    path,
    { method = "GET", token, body, expectedStatus = 200 } = {},
) {
    const headers = {};
    if (token) headers.Authorization = `Bearer ${token}`;
    if (body && !(body instanceof FormData)) headers["Content-Type"] = "application/json";

    const response = await fetch(`${apiBaseUrl}${path}`, {
        method,
        headers,
        body:
            body instanceof FormData
                ? body
                : body
                    ? JSON.stringify(body)
                    : undefined,
    });
    const result = await response.json().catch(() => null);

    assert.equal(
        response.status,
        expectedStatus,
        `${method} ${path}: expected ${expectedStatus}, got ${response.status} (${result?.code || "NO_CODE"}: ${result?.message || "NO_MESSAGE"})`,
    );
    assert.equal(
        result?.success,
        expectedStatus >= 200 && expectedStatus < 300,
        `${method} ${path}: common success envelope mismatch`,
    );
    return result;
}

async function callImage(path, token) {
    const response = await fetch(`${apiBaseUrl}${path}`, {
        headers: { Authorization: `Bearer ${token}` },
    });
    assert.equal(response.status, 200, `GET ${path}: protected image must be available`);
    assert.match(response.headers.get("content-type") || "", /^image\//);
    const bytes = new Uint8Array(await response.arrayBuffer());
    assert.ok(bytes.length > 0, `GET ${path}: image body must not be empty`);
    return {
        contentType: response.headers.get("content-type"),
        byteLength: bytes.length,
    };
}

function imageForm() {
    const formData = new FormData();
    formData.append("image", new Blob([imageBytes], { type: imageMimeType }), imageName);
    return formData;
}

function cardForm(card) {
    const formData = new FormData();
    formData.append(
        "card",
        new Blob([JSON.stringify(card)], { type: "application/json" }),
    );
    return formData;
}

async function signupAndLogin(label) {
    const email = `api-6-8-e2e-${label}-${Date.now()}@example.com`;
    const password = "1234567890";
    await callApi("/auth/signup", {
        method: "POST",
        expectedStatus: 201,
        body: { email, password },
    });
    const login = await callApi("/auth/login", {
        method: "POST",
        body: { email, password },
    });
    assert.ok(login.data?.accessToken);
    return login.data.accessToken;
}

async function createFrontConfirmedTicket(token, suffix) {
    const analyzed = await callApi("/memory-drafts/analyze", {
        method: "POST",
        token,
        body: imageForm(),
    });
    assert.equal(analyzed.data.draftStatus, "TYPE_PENDING");

    const draftId = analyzed.data.draftId;
    const typeConfirmed = await callApi(
        `/memory-drafts/${draftId}/document-type/confirm`,
        {
            method: "POST",
            token,
            body: { documentType: "TICKET" },
        },
    );
    assert.equal(typeConfirmed.data.draftStatus, "FRONT_PENDING");

    const frontConfirmed = await callApi(
        `/memory-drafts/${draftId}/front/confirm`,
        {
            method: "PUT",
            token,
            body: {
                memoryDate: "2026-07-31",
                front: {
                    eventName: `E2E 테스트 공연 ${suffix}`,
                    venue: "E2E 테스트 공연장",
                    seat: null,
                },
            },
        },
    );
    assert.equal(frontConfirmed.data.draftStatus, "FRONT_CONFIRMED");
    return draftId;
}

const ownerToken = await signupAndLogin("owner");
const directDraftId = await createFrontConfirmedTicket(ownerToken, "DIRECT");
const recallDraftId = await createFrontConfirmedTicket(ownerToken, "AI_RECALL");

const directSaved = await callApi("/cards", {
    method: "POST",
    token: ownerToken,
    expectedStatus: 201,
    body: cardForm({
        draftId: directDraftId,
        back: {
            companions: [],
            weather: "맑음",
            mood: "기쁨",
            writingMode: "DIRECT",
            title: "E2E 직접 기록",
            memoryText: "직접 작성한 테스트 추억입니다.",
        },
    }),
});
assert.equal(directSaved.data.draftStatus, "SAVED");

const subtypeSuggestion = await callApi(
    `/memory-drafts/${recallDraftId}/ticket-recall/subtype-suggestion`,
    { method: "POST", token: ownerToken },
);
assert.equal(typeof subtypeSuggestion.data.requiresManualSelection, "boolean");
const ticketSubtype = subtypeSuggestion.data.suggestedTicketSubtype || "CONCERT_PERFORMANCE";

const questions = await callApi(
    `/memory-drafts/${recallDraftId}/ticket-recall/questions`,
    {
        method: "POST",
        token: ownerToken,
        body: { ticketSubtype },
    },
);
assert.equal(questions.data.ticketSubtype, ticketSubtype);
assert.equal(questions.data.questions.length, 3);
assert.deepEqual(questions.data.questions.map(({ order }) => order), [1, 2, 3]);

const answers = questions.data.questions.map(({ questionId }, index) => ({
    questionId,
    answer: index === 0 ? "가장 기억에 남는 E2E 테스트 순간입니다." : null,
}));

const invalidTitle = await callApi(
    `/memory-drafts/${recallDraftId}/ticket-recall/title`,
    {
        method: "POST",
        token: ownerToken,
        expectedStatus: 400,
        body: { answers: answers.slice(0, 2) },
    },
);
assert.equal(invalidTitle.code, "TICKET_003");

const title = await callApi(
    `/memory-drafts/${recallDraftId}/ticket-recall/title`,
    {
        method: "POST",
        token: ownerToken,
        body: { answers },
    },
);
assert.ok(title.data.titleCandidate?.trim());

const recallSaved = await callApi("/cards", {
    method: "POST",
    token: ownerToken,
    expectedStatus: 201,
    body: cardForm({
        draftId: recallDraftId,
        back: {
            companions: ["E2E 동행인"],
            weather: "맑음",
            mood: "벅참",
            writingMode: "AI_RECALL",
            title: title.data.titleCandidate,
            answers,
        },
    }),
});
assert.equal(recallSaved.data.draftStatus, "SAVED");

const duplicateSave = await callApi("/cards", {
    method: "POST",
    token: ownerToken,
    expectedStatus: 409,
    body: cardForm({
        draftId: directDraftId,
        back: {
            companions: [],
            weather: "맑음",
            mood: "기쁨",
            writingMode: "DIRECT",
            title: "중복 저장",
            memoryText: "중복 저장은 거절되어야 합니다.",
        },
    }),
});
assert.equal(duplicateSave.code, "DRAFT_003");

const drawers = await callApi("/drawers", { token: ownerToken });
assert.deepEqual(drawers.data.drawers, [{ year: 2026, cardCount: 2 }]);

const yearCards = await callApi("/drawers/2026/cards", { token: ownerToken });
assert.equal(yearCards.data.cards.length, 2);
assert.deepEqual(
    new Set(yearCards.data.cards.map(({ cardId }) => cardId)),
    new Set([directSaved.data.cardId, recallSaved.data.cardId]),
);

const directDetail = await callApi(`/cards/${directSaved.data.cardId}`, {
    token: ownerToken,
});
assert.equal(directDetail.data.back.writingMode, "DIRECT");
assert.equal(directDetail.data.back.memoryText, "직접 작성한 테스트 추억입니다.");

const recallDetail = await callApi(`/cards/${recallSaved.data.cardId}`, {
    token: ownerToken,
});
assert.equal(recallDetail.data.back.writingMode, "AI_RECALL");
assert.equal(recallDetail.data.back.ticketSubtype, ticketSubtype);
assert.equal(recallDetail.data.back.answers.length, 3);

const imageResult = await callImage(recallDetail.data.front.frontImageUrl, ownerToken);

const unauthenticatedImage = await callApi(recallDetail.data.front.frontImageUrl, {
    expectedStatus: 401,
});
assert.equal(unauthenticatedImage.code, "AUTH_001");

const otherUserToken = await signupAndLogin("other");
const forbiddenCard = await callApi(`/cards/${recallSaved.data.cardId}`, {
    token: otherUserToken,
    expectedStatus: 403,
});
assert.equal(forbiddenCard.code, "CARD_001");
const forbiddenImage = await callApi(recallDetail.data.front.frontImageUrl, {
    token: otherUserToken,
    expectedStatus: 403,
});
assert.equal(forbiddenImage.code, "CARD_001");

console.log(JSON.stringify({
    success: true,
    ticketRecall: {
        suggestion: subtypeSuggestion.data.suggestedTicketSubtype,
        manualSelection: subtypeSuggestion.data.requiresManualSelection,
        confirmedSubtype: ticketSubtype,
        questionCount: questions.data.questions.length,
        invalidAnswers: invalidTitle.code,
        titleGenerated: Boolean(title.data.titleCandidate),
    },
    cardSave: {
        directCardId: directSaved.data.cardId,
        recallCardId: recallSaved.data.cardId,
        duplicateSave: duplicateSave.code,
    },
    drawer: {
        years: drawers.data.drawers,
        cardCount: yearCards.data.cards.length,
        directMode: directDetail.data.back.writingMode,
        recallMode: recallDetail.data.back.writingMode,
    },
    protectedImage: imageResult,
    negativeFlows: {
        noImageToken: unauthenticatedImage.code,
        otherUsersCard: forbiddenCard.code,
        otherUsersImage: forbiddenImage.code,
    },
}, null, 2));
