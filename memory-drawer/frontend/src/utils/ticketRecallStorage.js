const TICKET_RECALL_KEY_PREFIX = "memory-drawer-ticket-recall-";
const TICKET_RECALL_FLOW_KEY_PREFIX = "memory-drawer-ticket-recall-flow-";

const readJson = (key) => {
    const savedValue = sessionStorage.getItem(key);

    if (!savedValue) return null;

    try {
        return JSON.parse(savedValue);
    } catch {
        sessionStorage.removeItem(key);
        return null;
    }
};

export const saveTicketRecall = (draftId, ticketRecall) => {
    sessionStorage.setItem(
        `${TICKET_RECALL_KEY_PREFIX}${draftId}`,
        JSON.stringify(ticketRecall),
    );
};

export const getTicketRecall = (draftId) => {
    return readJson(`${TICKET_RECALL_KEY_PREFIX}${draftId}`);
};

export const removeTicketRecall = (draftId) => {
    sessionStorage.removeItem(`${TICKET_RECALL_KEY_PREFIX}${draftId}`);
};

export const saveTicketRecallFlow = (draftId, flowState) => {
    sessionStorage.setItem(
        `${TICKET_RECALL_FLOW_KEY_PREFIX}${draftId}`,
        JSON.stringify(flowState),
    );
};

export const getTicketRecallFlow = (draftId) => {
    return readJson(`${TICKET_RECALL_FLOW_KEY_PREFIX}${draftId}`);
};

export const removeTicketRecallFlow = (draftId) => {
    sessionStorage.removeItem(`${TICKET_RECALL_FLOW_KEY_PREFIX}${draftId}`);
};
