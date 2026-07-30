import { useEffect, useState } from 'react'
import { fetchCardsByYear } from './drawerApi'
import { DOCUMENT_LABELS, formatMemoryDate, getCardTitle, getImageUrl } from './drawerViewUtils'
import './DrawerCardList.css'

function DrawerCardList({ year, onBack, onCardsLoaded, onSelectCard }) {
  const [cards, setCards] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function loadCards() {
      setIsLoading(true)
      setErrorMessage('')

      try {
        const data = await fetchCardsByYear(year, { signal: controller.signal })
        setCards(data.cards)
        onCardsLoaded?.(data.cards)
      } catch (error) {
        if (error.name !== 'AbortError') {
          setErrorMessage(error.message)
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false)
        }
      }
    }

    loadCards()
    return () => controller.abort()
  }, [year, reloadKey, onCardsLoaded])

  return (
    <main className="drawer-view">
      <header className="drawer-view__header">
        <button className="back-button" type="button" onClick={onBack} aria-label="서랍 목록으로 돌아가기">‹</button>
        <div>
          <p>기억서랍</p>
          <h1>{year}년의 기억</h1>
        </div>
      </header>

      {isLoading && <p className="drawer-view__status">카드를 불러오는 중이에요.</p>}

      {!isLoading && errorMessage && (
        <section className="drawer-view__message" role="alert">
          <p>{errorMessage}</p>
          <button type="button" onClick={() => setReloadKey((key) => key + 1)}>다시 시도</button>
        </section>
      )}

      {!isLoading && !errorMessage && cards.length === 0 && (
        <section className="drawer-view__message">
          <p>{year}년에 저장한 카드가 아직 없어요.</p>
        </section>
      )}

      {!isLoading && !errorMessage && cards.length > 0 && (
        <ul className="memory-card-list" aria-label={`${year}년 카드 목록`}>
          {cards.map((card) => {
            const imageUrl = getImageUrl(card.front?.frontImageUrl)

            return (
              <li key={card.cardId}>
                <button className="memory-card-list__item" type="button" onClick={() => onSelectCard?.(card.cardId)}>
                  <span className="memory-card-list__image" aria-hidden={!imageUrl}>
                    {imageUrl ? <img src={imageUrl} alt="" /> : <span>{DOCUMENT_LABELS[card.documentType]}</span>}
                  </span>
                  <span className="memory-card-list__body">
                    <span className="memory-card-list__type">{DOCUMENT_LABELS[card.documentType] || '기록'}</span>
                    <strong>{getCardTitle(card)}</strong>
                    <span>{formatMemoryDate(card.memoryDate)}</span>
                  </span>
                  <span className="memory-card-list__arrow" aria-hidden="true">›</span>
                </button>
              </li>
            )
          })}
        </ul>
      )}

      {/* TODO(정렬 정책 확정): API 명세의 날짜 정렬 방향이 확정되면 백엔드 응답 순서를 그대로 사용한다. */}
      {/* TODO(이미지 인증 방식 확정): frontImageUrl이 인증을 요구하면 백엔드와 합의한 접근 방식으로 이미지 요청을 연결한다. */}
    </main>
  )
}

export default DrawerCardList
