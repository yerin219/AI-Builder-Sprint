const DRAFT_ID_KEY = "memory-drawer-draft-id";

export const saveDraftId = (draftId) => {
    sessionStorage.setItem(DRAFT_ID_KEY, draftId);
};

export const getDraftId = () => {
    return sessionStorage.getItem(DRAFT_ID_KEY);
};

export const removeDraftId = () => {
    sessionStorage.removeItem(DRAFT_ID_KEY);
};


export const saveFrontConfirmed = (draftId, frontConfirmed) => {
    const key = `memory-drawer-front-confirmed-${draftId}`;

    sessionStorage.setItem(key, JSON.stringify(frontConfirmed));
};

export const getFrontConfirmed = (draftId) => {
    const key = `memory-drawer-front-confirmed-${draftId}`;
    const value = sessionStorage.getItem(key);

    if (!value) return null;

    try {
        return JSON.parse(value);
    } catch {
        sessionStorage.removeItem(key);
        return null;
    }
};

export const removeFrontConfirmed = (draftId) => {
    sessionStorage.removeItem(
        `memory-drawer-front-confirmed-${draftId}`,
    );
};
