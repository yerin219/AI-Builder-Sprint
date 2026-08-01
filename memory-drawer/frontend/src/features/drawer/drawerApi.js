import { request } from "../../api/http.js";

export async function fetchDrawers(options = {}) {
  const data = await request("/drawers", options)
  if (!Array.isArray(data?.drawers)) throw new Error("서랍 목록 응답 형식이 올바르지 않습니다.")
  return data.drawers
}

export async function fetchCardsByYear(year, options = {}) {
  const data = await request(`/drawers/${encodeURIComponent(year)}/cards`, options)
  if (!Array.isArray(data?.cards)) throw new Error("카드 목록 응답 형식이 올바르지 않습니다.")
  return data
}

export async function fetchCardDetail(cardId, options = {}) {
  const data = await request(`/cards/${encodeURIComponent(cardId)}`, options)
  if (!data?.cardId || !data?.front || !data?.back) throw new Error("카드 상세 응답 형식이 올바르지 않습니다.")
  return data
}
