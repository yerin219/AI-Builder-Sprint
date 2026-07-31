import { request } from "./http";

export const suggestTicketSubtype = (draftId) =>
    request(`/memory-drafts/${draftId}/ticket-recall/subtype-suggestion`, {
        method: "POST",
    });

export const confirmTicketSubtypeAndGetQuestions = (draftId, ticketSubtype) =>
    request(`/memory-drafts/${draftId}/ticket-recall/questions`, {
        method: "POST",
        body: { ticketSubtype },
    });

export const generateTicketRecallTitle = (draftId, answers) =>
    request(`/memory-drafts/${draftId}/ticket-recall/title`, {
        method: "POST",
        body: { answers },
    });
