import { apiRequest } from './http'

export function analyzeMemoryDraft(token, imageFile) {
  const formData = new FormData()

  formData.append('image', imageFile)

  return apiRequest('/memory-drafts/analyze', {
    method: 'POST',
    token,
    body: formData,
  })
}

export function confirmDocumentType(
  token,
  draftId,
  documentType,
) {
  return apiRequest(
    `/memory-drafts/${draftId}/document-type/confirm`,
    {
      method: 'POST',
      token,
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        documentType,
      }),
    },
  )
}

export function confirmFront(token, draftId, payload) {
  return apiRequest(
    `/memory-drafts/${draftId}/front/confirm`,
    {
      method: 'PUT',
      token,
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    },
  )
}