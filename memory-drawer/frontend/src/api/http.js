const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || '/api'

export async function apiRequest(
  path,
  { token, headers = {}, ...options } = {},
) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      ...(token
        ? { Authorization: `Bearer ${token}` }
        : {}),
      ...headers,
    },
  })

  const result = await response.json().catch(() => null)

  if (!response.ok || result?.success === false) {
    const error = new Error(
      result?.message || '요청 처리에 실패했습니다.',
    )

    error.status = response.status
    error.code = result?.code

    throw error
  }

  return result.data
}