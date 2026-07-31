const TICKET_RECALL_KEY_PREFIX = "memory-drawer-ticket-recall-";

export const saveTicketRecall = (draftId, ticketRecall) => {
    sessionStorage.setItem(
        `${TICKET_RECALL_KEY_PREFIX}${draftId}`,
        JSON.stringify(ticketRecall),
    );
};

export const getTicketRecall = (draftId) => {
    const savedRecall = sessionStorage.getItem(
        `${TICKET_RECALL_KEY_PREFIX}${draftId}`,
    );

    if (!savedRecall) return null;

    try {
        return JSON.parse(savedRecall);
    } catch {
        return null;
    }
};

export const removeTicketRecall = (draftId) => {
    sessionStorage.removeItem(`${TICKET_RECALL_KEY_PREFIX}${draftId}`);
};
