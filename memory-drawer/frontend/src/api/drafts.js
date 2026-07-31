import { request } from './http'

export const analyzeImage = (file) => {
    const formData = new FormData()
    formData.append('image', file)

    return request('/memory-drafts/analyze', {
        method: 'POST',
        body: formData,
    })
}

export const confirmDocumentType = (draftId, documentType) =>
    request(`/memory-drafts/${draftId}/document-type/confirm`, {
        method: 'POST',
        body: { documentType },
    })

export const confirmFront = (draftId, payload) =>
    request(`/memory-drafts/${draftId}/front/confirm`, {
        method: 'PUT',
        body: payload,
    })