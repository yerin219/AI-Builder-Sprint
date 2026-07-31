import { request } from '../../api/http.js'

export class CardSaveError extends Error {
  constructor(message, code) {
    super(message)
    this.code = code
  }
}

export async function saveCard({ draftId, back, backPhotos }) {
  const formData = new FormData()
  formData.append(
    'card',
    new Blob([JSON.stringify({ draftId, back })], { type: 'application/json' }),
  )

  backPhotos.forEach((photo) => {
    formData.append('backPhotos', photo)
  })

  try {
    return await request('/cards', {
      method: 'POST',
      body: formData,
    })
  } catch (error) {
    throw new CardSaveError(error.message, error.code)
  }
}
