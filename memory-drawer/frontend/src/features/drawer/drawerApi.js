const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

export class DrawerApiError extends Error {
  constructor(message, code) {
    super(message)
    this.code = code
  }
}

export async function fetchDrawers({ signal } = {}) {
  // TODO(로그인 API 연동): 인증 방식이 확정되면 이 요청에 필요한 인증 정보를 함께 전송한다.
  const response = await fetch(`${API_BASE_URL}/drawers`, {
    headers: { Accept: 'application/json' },
    signal,
  })
  const payload = await response.json().catch(() => null)

  if (!response.ok || !payload?.success) {
    throw new DrawerApiError(
      payload?.message || '서랍을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.',
      payload?.code,
    )
  }

  if (!Array.isArray(payload?.data?.drawers)) {
    throw new DrawerApiError('서랍 목록 응답 형식이 올바르지 않습니다.')
  }

  return payload.data.drawers
}

export async function fetchCardsByYear(year, { signal } = {}) {
  const response = await fetch(`${API_BASE_URL}/drawers/${encodeURIComponent(year)}/cards`, {
    headers: { Accept: 'application/json' },
    signal,
  })
  const payload = await response.json().catch(() => null)

  if (!response.ok || !payload?.success) {
    throw new DrawerApiError(
      payload?.message || '카드 목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.',
      payload?.code,
    )
  }

  if (!Array.isArray(payload?.data?.cards)) {
    throw new DrawerApiError('카드 목록 응답 형식이 올바르지 않습니다.')
  }

  return payload.data
}

export async function fetchCardDetail(cardId, { signal } = {}) {
  const response = await fetch(`${API_BASE_URL}/cards/${encodeURIComponent(cardId)}`, {
    headers: { Accept: 'application/json' },
    signal,
  })
  const payload = await response.json().catch(() => null)

  if (!response.ok || !payload?.success) {
    throw new DrawerApiError(
      payload?.message || '카드를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.',
      payload?.code,
    )
  }

  if (!payload?.data?.cardId || !payload?.data?.front || !payload?.data?.back) {
    throw new DrawerApiError('카드 상세 응답 형식이 올바르지 않습니다.')
  }

  return payload.data
}
