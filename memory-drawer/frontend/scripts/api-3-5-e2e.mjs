import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { basename, extname } from "node:path";

const apiBaseUrl = process.env.E2E_API_BASE_URL || "http://localhost:8080/api";
const imagePath = process.env.E2E_IMAGE_PATH;

if (!imagePath) {
    throw new Error("E2E_IMAGE_PATH에 테스트 이미지 경로를 지정해주세요.");
}

const imageBytes = await readFile(imagePath);
const imageName = basename(imagePath);
const imageMimeTypes = {
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".png": "image/png",
    ".webp": "image/webp",
};
const imageMimeType = imageMimeTypes[extname(imagePath).toLowerCase()];

if (!imageMimeType) {
    throw new Error("E2E 이미지는 JPEG, PNG 또는 WebP 형식이어야 합니다.");
}

async function callApi(
    path,
    { method = "GET", token, body, expectedStatus = 200 } = {},
) {
    const headers = {};

    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }

    if (body && !(body instanceof FormData)) {
        headers["Content-Type"] = "application/json";
    }

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

    if (expectedStatus >= 200 && expectedStatus < 300) {
        assert.equal(result?.success, true, `${method} ${path}: success must be true`);
    } else {
        assert.equal(result?.success, false, `${method} ${path}: success must be false`);
    }

    return result;
}

function createImageForm() {
    const formData = new FormData();
    formData.append(
        "image",
        new Blob([imageBytes], { type: imageMimeType }),
        imageName,
    );
    return formData;
}

const email = `api-3-5-e2e-${Date.now()}@example.com`;
const password = "1234567890";

await callApi("/auth/signup", {
    method: "POST",
    expectedStatus: 201,
    body: { email, password },
});

const loginResult = await callApi("/auth/login", {
    method: "POST",
    body: { email, password },
});
const accessToken = loginResult.data.accessToken;

const unauthorized = await callApi("/memory-drafts/analyze", {
    method: "POST",
    expectedStatus: 401,
    body: createImageForm(),
});
assert.equal(unauthorized.code, "AUTH_001");

const finalValues = {
    RECEIPT: {
        memoryDate: "2026-07-12",
        front: { storeName: "E2E 테스트 상점" },
    },
    TICKET: {
        memoryDate: "2026-07-12",
        front: {
            eventName: "E2E 테스트 공연",
            venue: "E2E 테스트 공연장",
            seat: null,
        },
    },
    LETTER: {
        memoryDate: "2026-07-12",
        front: { ocrText: "E2E 테스트 손편지 내용" },
    },
};

const summaries = [];

for (const documentType of ["RECEIPT", "TICKET", "LETTER"]) {
    const analyzed = await callApi("/memory-drafts/analyze", {
        method: "POST",
        token: accessToken,
        body: createImageForm(),
    });
    assert.equal(analyzed.data.draftStatus, "TYPE_PENDING");
    const { draftId } = analyzed.data;

    const typeConfirmed = await callApi(
        `/memory-drafts/${draftId}/document-type/confirm`,
        {
            method: "POST",
            token: accessToken,
            body: { documentType },
        },
    );
    assert.equal(typeConfirmed.data.documentType, documentType);
    assert.equal(typeConfirmed.data.draftStatus, "FRONT_PENDING");
    assert.equal(typeConfirmed.data.nextAction, "CONFIRM_FRONT");

    const frontConfirmed = await callApi(
        `/memory-drafts/${draftId}/front/confirm`,
        {
            method: "PUT",
            token: accessToken,
            body: finalValues[documentType],
        },
    );
    assert.equal(frontConfirmed.data.draftStatus, "FRONT_CONFIRMED");
    assert.equal(frontConfirmed.data.nextAction, "WRITE_BACK");

    const reconfirmed = await callApi(
        `/memory-drafts/${draftId}/front/confirm`,
        {
            method: "PUT",
            token: accessToken,
            body: finalValues[documentType],
        },
    );
    assert.equal(reconfirmed.data.draftStatus, "FRONT_CONFIRMED");

    const duplicateTypeConfirmation = await callApi(
        `/memory-drafts/${draftId}/document-type/confirm`,
        {
            method: "POST",
            token: accessToken,
            expectedStatus: 409,
            body: { documentType },
        },
    );
    assert.equal(duplicateTypeConfirmation.code, "DRAFT_002");

    summaries.push({
        documentType,
        draftId,
        api3: analyzed.data.draftStatus,
        api4: typeConfirmed.data.draftStatus,
        api5: frontConfirmed.data.draftStatus,
        nextAction: frontConfirmed.data.nextAction,
        reconfirm: reconfirmed.data.draftStatus,
    });
}

const receiptDraftId = summaries.find(
    ({ documentType }) => documentType === "RECEIPT",
).draftId;
const invalidFront = await callApi(
    `/memory-drafts/${receiptDraftId}/front/confirm`,
    {
        method: "PUT",
        token: accessToken,
        expectedStatus: 400,
        body: {
            memoryDate: "2026-07-12",
            front: { storeName: "   " },
        },
    },
);
assert.equal(invalidFront.code, "VALIDATION_001");

console.log(JSON.stringify({
    success: true,
    positiveFlows: summaries,
    negativeFlows: {
        noToken: unauthorized.code,
        duplicateApi4: "DRAFT_002",
        missingApi5Value: invalidFront.code,
    },
}, null, 2));
