const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

export class CardSaveError extends Error {
  constructor(message, code) {
    super(message)
    this.code = code
  }
}

export async function saveCard({ draftId, back, backPhotos }) {
  const formData = new FormData()
  
formData.append('card',
  new Blob([JSON.stringify({ draftId, back })], { type: 'application/json',}),)
  
  backPhotos.forEach((photo) => {
    formData.append('backPhotos', photo)
  })

  const response = await fetch(`${API_BASE_URL}/cards`, {
    method: 'POST',
    headers: { Accept: 'application/json' },
    body: formData,
  })
  const payload = await response.json().catch(() => null)

  if (!response.ok || !payload?.success) {
    throw new CardSaveError(
      payload?.message || '카드를 저장하지 못했습니다. 잠시 후 다시 시도해주세요.',
      payload?.code,
    )
  }

  return payload.data
}
