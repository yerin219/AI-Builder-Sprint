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