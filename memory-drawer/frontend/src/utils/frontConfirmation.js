const hasText = (value) =>
    typeof value === "string" && value.trim().length > 0;

export function createFrontForm(candidate = {}) {
    return {
        memoryDate: candidate.memoryDate ?? "",
        storeName: candidate.storeName ?? "",
        eventName: candidate.eventName ?? "",
        venue: candidate.venue ?? "",
        seat: candidate.seat ?? "",
        ocrText: candidate.ocrText ?? "",
    };
}

export function getNextPathAfterFrontConfirmation(documentType, draftId) {
    if (documentType === "TICKET") {
        return `/memories/${draftId}/back`;
    }

    return `/memories/${draftId}/save`;
}

export function validateFrontForm(documentType, form) {
    if (!hasText(form.memoryDate)) {
        return "날짜를 입력해주세요.";
    }

    if (documentType === "RECEIPT" && !hasText(form.storeName)) {
        return "상호명을 입력해주세요.";
    }

    if (documentType === "TICKET") {
        if (!hasText(form.eventName)) {
            return "행사명을 입력해주세요.";
        }

        if (!hasText(form.venue)) {
            return "장소를 입력해주세요.";
        }
    }

    if (documentType === "LETTER" && !hasText(form.ocrText)) {
        return "손편지 내용을 입력해주세요.";
    }

    if (!["RECEIPT", "TICKET", "LETTER"].includes(documentType)) {
        return "지원하지 않는 문서 유형입니다.";
    }

    return null;
}

export function buildFrontPayload(documentType, form) {
    const validationError = validateFrontForm(documentType, form);

    if (validationError) {
        throw new Error(validationError);
    }

    const memoryDate = form.memoryDate.trim();

    if (documentType === "RECEIPT") {
        return {
            memoryDate,
            front: {
                storeName: form.storeName.trim(),
            },
        };
    }

    if (documentType === "TICKET") {
        return {
            memoryDate,
            front: {
                eventName: form.eventName.trim(),
                venue: form.venue.trim(),
                seat: form.seat?.trim() || null,
            },
        };
    }

    return {
        memoryDate,
        front: {
            ocrText: form.ocrText.trim(),
        },
    };
}
