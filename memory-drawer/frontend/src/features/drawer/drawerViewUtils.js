export const DOCUMENT_LABELS = {
  RECEIPT: '영수증',
  TICKET: '티켓',
  LETTER: '손편지',
}

export function formatMemoryDate(memoryDate) {
  if (typeof memoryDate !== 'string') return ''

  const [year, month, day] = memoryDate.split('-')
  if (!year || !month || !day) return memoryDate

  return `${year}. ${Number(month)}. ${Number(day)}.`
}

export function getCardTitle(card) {
  if (card.documentType === 'RECEIPT') return card.front?.storeName || '이름 없는 영수증'
  if (card.documentType === 'TICKET') return card.front?.eventName || '이름 없는 티켓'
  return '손편지'
}

export function getImageUrl(imageUrl) {
  if (typeof imageUrl !== 'string' || !imageUrl.trim()) return null

  return imageUrl
}
