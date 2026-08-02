import { useEffect, useMemo, useState } from 'react'
import openDrawerInterior from '../../assets/open-drawer-interior.png'
import { fetchCardsByYear } from './drawerApi'
import AuthorizedImage from './AuthorizedImage'
import { createDrawerLayout, formatMemoryDate, getCardTitle, getFrontImageUrl } from './drawerViewUtils'
import './DrawerCardList.css'

function DrawerCardList({ year, onBack, onCardsLoaded, onSelectCard }) {
  const [cards, setCards] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [reloadKey, setReloadKey] = useState(0)
  const [selectedCardId, setSelectedCardId] = useState(null)
  const arrangedCards = useMemo(() => createDrawerLayout(cards), [cards])

  useEffect(() => {
    const controller = new AbortController()

    async function loadCards() {
      setIsLoading(true)
      setErrorMessage('')

      try {
        const data = await fetchCardsByYear(year, { signal: controller.signal })
        setCards(data.cards)
        setSelectedCardId(null)
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

      {!isLoading && !errorMessage && (
        <section className="open-drawer" aria-label={`${year}년 열린 기억 서랍`}>
          <div
            className={`open-drawer__scene${selectedCardId ? ' has-selection' : ''}`}
            onClick={() => setSelectedCardId(null)}
          >
            <img className="open-drawer__background" src={openDrawerInterior} alt="" aria-hidden="true" />
            <div className="open-drawer__surface">
              {arrangedCards.map(({ card, placement }) => (
                <DrawerMemoryPaper
                  key={card.cardId}
                  card={card}
                  placement={placement}
                  isSelected={selectedCardId === card.cardId}
                  onClick={() => {
                    if (selectedCardId === card.cardId) {
                      onSelectCard?.(card.cardId)
                      return
                    }
                    setSelectedCardId(card.cardId)
                  }}
                />
              ))}

              {cards.length === 0 && (
                <div className="open-drawer__empty">
                  <strong>아직 비어 있는 서랍이에요.</strong>
                  <span>{year}년의 종이 기억을 담아보세요.</span>
                </div>
              )}
            </div>
          </div>

          <p className="open-drawer__count">
            총 {cards.length}장의 기억
            {cards.length > arrangedCards.length && ` · 최근 ${arrangedCards.length}장 펼침`}
          </p>
          {cards.length > 0 && (
            <p className="open-drawer__hint">
              {selectedCardId
                ? '선택한 종이를 한 번 더 누르면 자세히 볼 수 있어요.'
                : '꺼내 보고 싶은 종이를 눌러보세요.'}
            </p>
          )}
        </section>
      )}

    </main>
  )
}

function DrawerMemoryPaper({ card, placement, isSelected, onClick }) {
  const documentType = card.documentType?.toLowerCase() || 'memory'
  const title = getCardTitle(card)
  const paperStyle = {
    '--paper-x': `${placement.x}%`,
    '--paper-y': `${placement.y}%`,
    '--paper-rotation': `${placement.rotation}deg`,
    '--paper-z': placement.zIndex,
  }

  function handleClick(event) {
    event.stopPropagation()
    onClick()
  }

  return (
    <button
      className={`drawer-paper drawer-paper--${documentType}${isSelected ? ' is-selected' : ''}`}
      type="button"
      style={paperStyle}
      onClick={handleClick}
      aria-pressed={isSelected}
      aria-label={`${title}, ${isSelected ? '한 번 더 눌러 상세 보기' : '꺼내 보기'}`}
    >
      {card.documentType === 'RECEIPT' && <ReceiptPaper card={card} />}
      {card.documentType === 'TICKET' && <TicketPaper card={card} />}
      {card.documentType === 'LETTER' && <LetterPaper card={card} />}
    </button>
  )
}

function ReceiptPaper({ card }) {
  const purchaseItems = Array.isArray(card.front?.purchaseItems) ? card.front.purchaseItems : []

  return (
    <span className="drawer-paper__content drawer-paper__content--receipt">
      <span className="drawer-paper__eyebrow">RECEIPT</span>
      <strong>{card.front?.storeName || '이름 없는 영수증'}</strong>
      <span className="drawer-paper__date">{formatMemoryDate(card.memoryDate)}</span>
      <span className="drawer-paper__rule" aria-hidden="true" />
      {purchaseItems.slice(0, 3).map((item, index) => (
        <span className="drawer-paper__receipt-row" key={`${item.name}-${index}`}>
          <span>{item.name}</span>
          <b>×{item.quantity}</b>
        </span>
      ))}
      {purchaseItems.length > 3 && <span className="drawer-paper__more">외 {purchaseItems.length - 3}개</span>}
    </span>
  )
}

function TicketPaper({ card }) {
  return (
    <span className="drawer-paper__content drawer-paper__content--ticket">
      <span className="drawer-paper__ticket-main">
        <strong>{card.front?.eventName || '이름 없는 티켓'}</strong>
        <span>{formatMemoryDate(card.memoryDate)}</span>
        <span>{card.front?.venue}</span>
        {card.front?.seat && <span>좌석 · {card.front.seat}</span>}
      </span>
      <span className="drawer-paper__ticket-stub" aria-hidden="true">
        <span className="drawer-paper__barcode" />
      </span>
    </span>
  )
}

function LetterPaper({ card }) {
  const text = getCardTitle(card)
  const imageUrl = getFrontImageUrl(card)

  return (
    <span className="drawer-paper__content drawer-paper__content--letter">
      <span className="drawer-paper__date">{formatMemoryDate(card.memoryDate)}</span>
      <strong>소중한 편지</strong>
      {imageUrl && <AuthorizedImage className="drawer-paper__letter-image" imageUrl={imageUrl} alt="" />}
      <span className="drawer-paper__letter-text">{text}</span>
    </span>
  )
}

export default DrawerCardList
