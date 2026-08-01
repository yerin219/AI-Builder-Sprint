const hasText = (value) =>
    typeof value === "string" && value.trim().length > 0;

const isSelectedPurchaseItem = (item) => item?.selected !== false;

const isPositiveInteger = (value) => {
    const quantity = Number(value);
    return Number.isInteger(quantity) && quantity > 0;
};

const createPurchaseItems = (purchaseItems) => {
    if (!Array.isArray(purchaseItems)) return [];

    return purchaseItems
        .filter((item) => item && typeof item === "object")
        .map((item) => ({
            name: typeof item.name === "string" ? item.name : "",
            quantity: item.quantity ?? "",
            selected: true,
        }));
};

export function createFrontForm(candidate = {}) {
    return {
        memoryDate: candidate.memoryDate ?? "",
        storeName: candidate.storeName ?? "",
        eventName: candidate.eventName ?? "",
        venue: candidate.venue ?? "",
        seat: candidate.seat ?? "",
        ocrText: candidate.ocrText ?? "",
        purchaseItems: createPurchaseItems(candidate.purchaseItems),
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

    if (documentType === "RECEIPT") {
        if (!hasText(form.storeName)) {
            return "상호명을 입력해주세요.";
        }

        const selectedItems = Array.isArray(form.purchaseItems)
            ? form.purchaseItems.filter(isSelectedPurchaseItem)
            : [];

        if (selectedItems.some((item) => !hasText(item?.name))) {
            return "선택한 구매 항목의 이름을 입력해주세요.";
        }

        if (selectedItems.some((item) => !isPositiveInteger(item?.quantity))) {
            return "구매 항목 수량은 1 이상의 정수로 입력해주세요.";
        }
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
        const purchaseItems = Array.isArray(form.purchaseItems)
            ? form.purchaseItems
                .filter(isSelectedPurchaseItem)
                .map((item) => ({
                    name: item.name.trim(),
                    quantity: Number(item.quantity),
                }))
            : [];

        return {
            memoryDate,
            front: {
                storeName: form.storeName.trim(),
                purchaseItems,
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
