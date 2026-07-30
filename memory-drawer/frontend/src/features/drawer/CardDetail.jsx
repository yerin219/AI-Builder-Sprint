import { useEffect, useMemo, useState } from 'react'
import { fetchCardDetail } from './drawerApi'
import { DOCUMENT_LABELS, formatMemoryDate, getImageUrl } from './drawerViewUtils'
import './CardDetail.css'

function CardDetail({ cardId, cardsInYear, onBack, onSelectCard }) {
  const [card, setCard] = useState(null)
  const [isFront, setIsFront] = useState(true)
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  const currentCardIndex = useMemo(
    () => cardsInYear.findIndex((item) => item.cardId === cardId),
    [cardId, cardsInYear],
  )
  const previousCard = currentCardIndex > 0 ? cardsInYear[currentCardIndex - 1] : null
  const nextCard = currentCardIndex >= 0 && currentCardIndex < cardsInYear.length - 1
    ? cardsInYear[currentCardIndex + 1]
    : null

  useEffect(() => {
    const controller = new AbortController()

    async function loadCard() {
      setIsLoading(true)
      setErrorMessage('')
      setIsFront(true)

      try {
        const loadedCard = await fetchCardDetail(cardId, { signal: controller.signal })
        setCard(loadedCard)
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

    loadCard()
    return () => controller.abort()
  }, [cardId, reloadKey])

  return (
    <main className="card-detail-page">
      <header className="card-detail-page__header">
        <button className="back-button" type="button" onClick={onBack} aria-label="카드 목록으로 돌아가기">‹</button>
        <p>{card ? DOCUMENT_LABELS[card.documentType] || '추억 카드' : '추억 카드'}</p>
      </header>

      {isLoading && <p className="drawer-view__status">카드를 꺼내오는 중이에요.</p>}

      {!isLoading && errorMessage && (
        <section className="drawer-view__message" role="alert">
          <p>{errorMessage}</p>
          <button type="button" onClick={() => setReloadKey((key) => key + 1)}>다시 시도</button>
        </section>
      )}

      {!isLoading && !errorMessage && card && (
        <>
          <section className="card-flip" aria-label="추억 카드 앞뒷면">
            <div className="card-flip__tabs" role="group" aria-label="카드 면 선택">
              <button type="button" className={isFront ? 'is-selected' : ''} onClick={() => setIsFront(true)} aria-pressed={isFront}>앞면</button>
              <button type="button" className={!isFront ? 'is-selected' : ''} onClick={() => setIsFront(false)} aria-pressed={!isFront}>뒷면</button>
            </div>

            {isFront ? <CardFront card={card} /> : <CardBack card={card} />}
          </section>

          <nav className="card-navigation" aria-label="같은 연도의 다른 카드">
            <button type="button" disabled={!previousCard} onClick={() => onSelectCard?.(previousCard.cardId)}>‹ 이전 카드</button>
            <span>{currentCardIndex >= 0 ? `${currentCardIndex + 1} / ${cardsInYear.length}` : ''}</span>
            <button type="button" disabled={!nextCard} onClick={() => onSelectCard?.(nextCard.cardId)}>다음 카드 ›</button>
          </nav>
        </>
      )}

      {/* TODO(카드 전환 방식 확정): 현재는 버튼으로 앞·뒷면을 전환하며, 최종 디자인에서 제스처가 필요하면 접근 가능한 버튼은 유지한다. */}
    </main>
  )
}

function CardFront({ card }) {
  const imageUrl = getImageUrl(card.front?.frontImageUrl)
  const isTicket = card.documentType === 'TICKET'
  const isReceipt = card.documentType === 'RECEIPT'

  return (
    <article className="memory-card memory-card--front">
      <p className="memory-card__date">{formatMemoryDate(card.memoryDate)}</p>
      {imageUrl && <img className="memory-card__front-image" src={imageUrl} alt="저장한 원본 종이 기록" />}
      {isReceipt && <h1>{card.front.storeName}</h1>}
      {isTicket && (
        <div className="memory-card__front-fields">
          <h1>{card.front.eventName}</h1>
          <p>{card.front.venue}</p>
          {card.front.seat && <p>좌석 · {card.front.seat}</p>}
        </div>
      )}
      {card.documentType === 'LETTER' && <p className="memory-card__letter-text">{card.front.ocrText}</p>}
    </article>
  )
}

function CardBack({ card }) {
  const { back } = card
  const isRecall = card.documentType === 'TICKET' && back.writingMode === 'AI_RECALL'
  const photoUrls = Array.isArray(back.backPhotoUrls) ? back.backPhotoUrls : []
  const answeredQuestions = Array.isArray(back.answers)
    ? back.answers.filter(({ answer }) => typeof answer === 'string' && answer.trim())
    : []

  return (
    <article className="memory-card memory-card--back">
      <dl className="memory-card__metadata">
        <div><dt>함께한 사람</dt><dd>{back.companions?.length ? back.companions.join(', ') : '혼자'}</dd></div>
        <div><dt>날씨</dt><dd>{back.weather}</dd></div>
        <div><dt>기분</dt><dd>{back.mood}</dd></div>
      </dl>

      {card.documentType !== 'TICKET' && back.diaryText && <p className="memory-card__diary">{back.diaryText}</p>}
      {card.documentType === 'TICKET' && (
        <section className="memory-card__ticket-memory">
          <h1>{back.title}</h1>
          {back.writingMode === 'DIRECT' && <p>{back.memoryText}</p>}
          {isRecall && answeredQuestions.map(({ questionId, question, answer }) => (
            <div className="memory-card__answer" key={questionId}>
              <h2>{question}</h2>
              <p>{answer}</p>
            </div>
          ))}
        </section>
      )}

      {photoUrls.length > 0 && (
        <div className="memory-card__photos" aria-label="추가 사진">
          {photoUrls.map((photoUrl, index) => (
            <img key={`${photoUrl}-${index}`} src={getImageUrl(photoUrl)} alt={`추가 사진 ${index + 1}`} loading="lazy" />
          ))}
        </div>
      )}
    </article>
  )
}

export default CardDetail
