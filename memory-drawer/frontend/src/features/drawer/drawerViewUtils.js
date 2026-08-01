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

export function getDaysAgo(memoryDate, now = new Date()) {
  if (typeof memoryDate !== 'string' || !(now instanceof Date) || Number.isNaN(now.getTime())) {
    return null
  }

  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(memoryDate)
  if (!match) return null

  const [, yearText, monthText, dayText] = match
  const year = Number(yearText)
  const month = Number(monthText)
  const day = Number(dayText)
  const memory = Date.UTC(year, month - 1, day)
  const parsedMemory = new Date(memory)

  if (
    parsedMemory.getUTCFullYear() !== year
    || parsedMemory.getUTCMonth() !== month - 1
    || parsedMemory.getUTCDate() !== day
  ) {
    return null
  }

  const today = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate())
  return Math.floor((today - memory) / (1000 * 60 * 60 * 24))
}

export function getCardTitle(card) {
  if (card.documentType === 'RECEIPT') return card.front?.storeName || '이름 없는 영수증'
  if (card.documentType === 'TICKET') return card.front?.eventName || '이름 없는 티켓'
  if (card.documentType === 'LETTER') {
    const ocrText = typeof card.front?.ocrText === 'string'
      ? card.front.ocrText.trim().replace(/\s+/g, ' ')
      : ''
    return ocrText || '내용 없는 손편지'
  }
  return '기록'
}

export function getImageUrl(imageUrl) {
  if (typeof imageUrl !== 'string' || !imageUrl.trim()) return null

  const normalizedUrl = imageUrl.trim()
  return normalizedUrl.startsWith('/files/cards/') ? normalizedUrl : null
}
