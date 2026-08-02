export const DOCUMENT_LABELS = {
  RECEIPT: '영수증',
  TICKET: '티켓',
  LETTER: '손편지',
}

const DRAWER_SLOTS = [
  { x: 26, y: 18, rotation: -7 },
  { x: 50, y: 20, rotation: 4 },
  { x: 74, y: 19, rotation: -3 },
  { x: 28, y: 40, rotation: 3 },
  { x: 72, y: 41, rotation: 7 },
  { x: 50, y: 52, rotation: -5 },
  { x: 26, y: 65, rotation: -3 },
  { x: 74, y: 66, rotation: 4 },
  { x: 46, y: 79, rotation: 6 },
  { x: 58, y: 33, rotation: -7 },
  { x: 40, y: 31, rotation: 5 },
  { x: 52, y: 67, rotation: -2 },
]

const MAX_DRAWER_CARDS = 10

function hashCardId(cardId) {
  const value = typeof cardId === 'string' ? cardId : ''
  let hash = 2166136261

  for (const character of value) {
    hash ^= character.codePointAt(0)
    hash = Math.imul(hash, 16777619)
  }

  return hash >>> 0
}

function getLayoutSeed(card) {
  const apiSeed = Number(card?.layoutSeed)
  return Number.isSafeInteger(apiSeed) && apiSeed >= 0
    ? apiSeed
    : hashCardId(card?.cardId)
}

function getSlotDistance(firstSlot, secondSlot) {
  const horizontalDistance = (firstSlot.x - secondSlot.x) / 24
  const verticalDistance = (firstSlot.y - secondSlot.y) / 61

  return (horizontalDistance ** 2) + (verticalDistance ** 2)
}

function selectSpreadOutSlot(seed, occupiedSlots) {
  if (occupiedSlots.size === 0) return seed % DRAWER_SLOTS.length

  const availableSlots = DRAWER_SLOTS
    .map((slot, index) => ({ slot, index }))
    .filter(({ index }) => !occupiedSlots.has(index))
    .map(({ slot, index }) => ({
      index,
      minimumDistance: Math.min(
        ...Array.from(occupiedSlots, (occupiedIndex) => (
          getSlotDistance(slot, DRAWER_SLOTS[occupiedIndex])
        )),
      ),
    }))

  const greatestDistance = Math.max(
    ...availableSlots.map(({ minimumDistance }) => minimumDistance),
  )
  const furthestSlots = availableSlots.filter(({ minimumDistance }) => (
    Math.abs(minimumDistance - greatestDistance) < Number.EPSILON
  ))

  return furthestSlots[seed % furthestSlots.length].index
}

export function createDrawerLayout(cards, limit = MAX_DRAWER_CARDS) {
  if (!Array.isArray(cards) || cards.length === 0) return []

  const normalizedLimit = Math.min(DRAWER_SLOTS.length, Math.max(1, limit))
  const visibleCards = cards.slice(-normalizedLimit)
  const occupiedSlots = new Set()

  return visibleCards.map((card, index) => {
    const seed = getLayoutSeed(card)
    const slotIndex = selectSpreadOutSlot(seed, occupiedSlots)
    occupiedSlots.add(slotIndex)

    const slot = DRAWER_SLOTS[slotIndex]
    const rotationJitter = ((seed >>> 8) % 5) - 2

    return {
      card,
      placement: {
        x: slot.x,
        y: slot.y,
        rotation: slot.rotation + rotationJitter,
        zIndex: index + 1,
      },
    }
  })
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
