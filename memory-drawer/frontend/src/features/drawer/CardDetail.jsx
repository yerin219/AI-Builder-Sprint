import { useEffect, useMemo, useState } from 'react'
import { deleteCard, fetchCardDetail, updateCard } from './drawerApi'
import AuthorizedImage from './AuthorizedImage'
import { DOCUMENT_LABELS, formatMemoryDate, getFrontImageUrl, getImageUrl } from './drawerViewUtils'
import './CardDetail.css'

function getTicketTextDensity(front) {
  const textLength = [front?.eventName, front?.venue, front?.seat]
    .filter(Boolean)
    .join('')
    .replace(/\s/g, '').length

  if (textLength <= 32) return 'standard'
  if (textLength <= 48) return 'compact'
  if (textLength <= 70) return 'dense'
  return 'tiny'
}

function CardDetail({ cardId, cardsInYear, onBack, onSelectCard }) {
  const [card, setCard] = useState(null)
  const [isFront, setIsFront] = useState(true)
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [reloadKey, setReloadKey] = useState(0)
  const [isEditing, setIsEditing] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [actionMessage, setActionMessage] = useState('')

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
        setIsEditing(false)
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

  async function handleUpdate(payload) {
    setIsSubmitting(true)
    setActionMessage('')

    try {
      await updateCard(card.cardId, payload)
      setIsEditing(false)
      setActionMessage('카드를 수정했어요.')
      setReloadKey((key) => key + 1)
    } catch (error) {
      setActionMessage(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleDelete() {
    if (!window.confirm('이 추억 카드를 삭제할까요? 삭제한 카드와 사진은 복구할 수 없어요.')) return

    setIsSubmitting(true)
    setActionMessage('')
    try {
      await deleteCard(card.cardId)
      onBack?.()
    } catch (error) {
      setActionMessage(error.message)
      setIsSubmitting(false)
    }
  }

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

          <div className="card-management-actions" role="group" aria-label="카드 관리">
            <button type="button" onClick={() => setIsEditing((current) => !current)} disabled={isSubmitting}>
              {isEditing ? '수정 취소' : '수정하기'}
            </button>
            <button type="button" className="card-management-actions__delete" onClick={handleDelete} disabled={isSubmitting}>
              삭제하기
            </button>
          </div>

          {actionMessage && <p className="card-management-message" role="status">{actionMessage}</p>}

          {isEditing && (
            <CardEditForm
              card={card}
              isSubmitting={isSubmitting}
              onCancel={() => setIsEditing(false)}
              onSubmit={handleUpdate}
            />
          )}
        </>
      )}
    </main>
  )
}

function getInitialForm(card) {
  return {
    memoryDate: card.memoryDate || '',
    companions: Array.isArray(card.back?.companions) ? card.back.companions.join(', ') : '',
    weather: card.back?.weather || '',
    mood: card.back?.mood || '',
    storeName: card.front?.storeName || '',
    ocrText: card.front?.ocrText || '',
    eventName: card.front?.eventName || '',
    venue: card.front?.venue || '',
    seat: card.front?.seat || '',
    diaryText: card.back?.diaryText || '',
    title: card.back?.title || '',
    memoryText: card.back?.memoryText || '',
    answers: Array.isArray(card.back?.answers)
      ? card.back.answers.map(({ questionId, question, answer }) => ({ questionId, question, answer: answer || '' }))
      : [],
  }
}

function CardEditForm({ card, isSubmitting, onCancel, onSubmit }) {
  const [form, setForm] = useState(() => getInitialForm(card))
  const [validationMessage, setValidationMessage] = useState('')
  const isTicket = card.documentType === 'TICKET'
  const isRecall = isTicket && card.back?.writingMode === 'AI_RECALL'

  function changeField(name, value) {
    setForm((current) => ({ ...current, [name]: value }))
  }

  function changeAnswer(index, value) {
    setForm((current) => ({
      ...current,
      answers: current.answers.map((answer, answerIndex) => (
        answerIndex === index ? { ...answer, answer: value } : answer
      )),
    }))
  }

  function buildPayload() {
    const companions = form.companions
      .split(',')
      .map((name) => name.trim())
      .filter(Boolean)
    const back = {
      companions,
      weather: form.weather.trim(),
      mood: form.mood.trim(),
    }
    let front

    if (card.documentType === 'RECEIPT') {
      front = { storeName: form.storeName.trim() }
      back.diaryText = form.diaryText.trim() || null
    } else if (card.documentType === 'LETTER') {
      front = { ocrText: form.ocrText.trim() }
      back.diaryText = form.diaryText.trim() || null
    } else {
      front = {
        eventName: form.eventName.trim(),
        venue: form.venue.trim(),
        seat: form.seat.trim() || null,
      }
      back.writingMode = card.back.writingMode
      back.title = form.title.trim()

      if (isRecall) {
        back.answers = form.answers.map(({ questionId, answer }) => ({
          questionId,
          answer: answer.trim() || null,
        }))
      } else {
        back.memoryText = form.memoryText.trim()
      }
    }

    return { memoryDate: form.memoryDate, front, back }
  }

  function handleSubmit(event) {
    event.preventDefault()
    const payload = buildPayload()

    if (!payload.memoryDate || !payload.back.weather || !payload.back.mood) {
      setValidationMessage('추억 날짜, 날씨, 기분을 모두 입력해주세요.')
      return
    }
    if (card.documentType === 'RECEIPT' && !payload.front.storeName) {
      setValidationMessage('영수증의 장소를 입력해주세요.')
      return
    }
    if (card.documentType === 'LETTER' && !payload.front.ocrText) {
      setValidationMessage('손쪽지 내용을 입력해주세요.')
      return
    }
    if (isTicket && (!payload.front.eventName || !payload.front.venue || !payload.back.title)) {
      setValidationMessage('티켓 제목, 장소, 추억 제목을 모두 입력해주세요.')
      return
    }
    if (isTicket && !isRecall && !payload.back.memoryText) {
      setValidationMessage('추억을 입력해주세요.')
      return
    }
    if (isRecall && !payload.back.answers.some(({ answer }) => answer)) {
      setValidationMessage('AI 회상 답변을 하나 이상 입력해주세요.')
      return
    }

    setValidationMessage('')
    onSubmit(payload)
  }

  return (
    <section className="card-edit-form" aria-labelledby="card-edit-title">
      <h2 id="card-edit-title">카드 수정</h2>
      <p>사진은 그대로 유지되고, 텍스트 정보와 날짜만 수정할 수 있어요.</p>
      <form onSubmit={handleSubmit}>
        <div className="card-edit-form__grid">
          <label>추억 날짜<input type="date" value={form.memoryDate} onChange={(event) => changeField('memoryDate', event.target.value)} required /></label>
          <label>함께한 사람<input value={form.companions} onChange={(event) => changeField('companions', event.target.value)} placeholder="쉼표로 구분, 혼자면 비워두세요" /></label>
          <label>날씨<input value={form.weather} onChange={(event) => changeField('weather', event.target.value)} required /></label>
          <label>기분<input value={form.mood} onChange={(event) => changeField('mood', event.target.value)} required /></label>
        </div>

        {card.documentType === 'RECEIPT' && (
          <>
            <label>장소<input value={form.storeName} onChange={(event) => changeField('storeName', event.target.value)} required /></label>
            <label>자유 기록<textarea value={form.diaryText} onChange={(event) => changeField('diaryText', event.target.value)} rows="5" /></label>
          </>
        )}

        {card.documentType === 'LETTER' && (
          <>
            <label>손쪽지 내용<textarea value={form.ocrText} onChange={(event) => changeField('ocrText', event.target.value)} rows="6" required /></label>
            <label>자유 기록<textarea value={form.diaryText} onChange={(event) => changeField('diaryText', event.target.value)} rows="5" /></label>
          </>
        )}

        {isTicket && (
          <>
            <label>티켓 제목<input value={form.eventName} onChange={(event) => changeField('eventName', event.target.value)} required /></label>
            <label>장소<input value={form.venue} onChange={(event) => changeField('venue', event.target.value)} required /></label>
            <label>좌석<input value={form.seat} onChange={(event) => changeField('seat', event.target.value)} /></label>
            <label>추억 제목<input value={form.title} onChange={(event) => changeField('title', event.target.value)} required /></label>
            {isRecall ? form.answers.map((answer, index) => (
              <label key={answer.questionId}>질문 {index + 1}. {answer.question}<textarea value={answer.answer} onChange={(event) => changeAnswer(index, event.target.value)} rows="4" /></label>
            )) : <label>추억<textarea value={form.memoryText} onChange={(event) => changeField('memoryText', event.target.value)} rows="6" required /></label>}
          </>
        )}

        {validationMessage && <p className="card-edit-form__error" role="alert">{validationMessage}</p>}
        <div className="card-edit-form__actions">
          <button type="button" onClick={onCancel} disabled={isSubmitting}>취소</button>
          <button type="submit" className="is-selected" disabled={isSubmitting}>{isSubmitting ? '저장 중...' : '수정 저장'}</button>
        </div>
      </form>
    </section>
  )
}

function CardFront({ card }) {
  const imageUrl = getFrontImageUrl(card)
  const isTicket = card.documentType === 'TICKET'
  const isReceipt = card.documentType === 'RECEIPT'
  const isLetter = card.documentType === 'LETTER'
  const ticketTextDensity = isTicket ? getTicketTextDensity(card.front) : ''

  return (
    <article className={`memory-card memory-card--front${card.documentType === 'LETTER' ? ' memory-card--letter' : ''}${card.documentType === 'TICKET' ? ' memory-card--ticket' : ''}${card.documentType === 'RECEIPT' ? ' memory-card--receipt' : ''}`}>
      {!isTicket && <p className="memory-card__date">{formatMemoryDate(card.memoryDate)}</p>}
      {imageUrl && <AuthorizedImage className="memory-card__front-image" imageUrl={imageUrl} alt="배경을 제거한 손편지" />}
      {isReceipt && <h1>{card.front.storeName}</h1>}
      {isTicket && (
        <div className={`memory-card__front-fields memory-card__front-fields--${ticketTextDensity}`}>
          <h1>{card.front.eventName}</h1>
          <p className="memory-card__ticket-date">{formatMemoryDate(card.memoryDate)}</p>
          <p>{card.front.venue}</p>
          {card.front.seat && <p>좌석 · {card.front.seat}</p>}
        </div>
      )}
      {isLetter && <p className="memory-card__letter-text">{card.front.ocrText}</p>}
    </article>
  )
}

function splitTicketAnswer(answer) {
  const characters = Array.from((answer || '답변을 남기지 않았어요.').trim())
  const pages = []

  while (characters.length > 0) {
    pages.push(characters.splice(0, 36).join(''))
  }

  return pages.length > 0 ? pages : ['답변을 남기지 않았어요.']
}

function getTicketAnswerDensity(question, answer) {
  const length = `${question || ''}${answer || ''}`.replace(/\s/g, '').length

  if (length <= 42) return 'spacious'
  if (length <= 76) return 'standard'
  if (length <= 120) return 'compact'
  return 'dense'
}

function TicketBackContent({ back }) {
  const isDirect = back.writingMode === 'DIRECT'
  const answers = Array.isArray(back.answers) && back.answers.length > 0
    ? back.answers
    : [{ question: '추억 기록', answer: back.memoryText }]
  const pages = answers.flatMap(({ question, answer }, questionIndex) => (
    splitTicketAnswer(answer).map((answerPart, answerPageIndex) => ({
      question,
      answer: answerPart,
      questionIndex,
      isContinuation: answerPageIndex > 0,
    }))
  ))
  const [selectedPage, setSelectedPage] = useState(0)
  const currentPage = Math.min(selectedPage, pages.length - 1)
  const page = pages[currentPage]
  const companions = back.companions?.length ? back.companions.join(', ') : '혼자'
  const answerDensity = getTicketAnswerDensity(page.question, page.answer)

  return (
    <section className={`memory-card__ticket-back-content memory-card__ticket-back-content--${answerDensity}`} aria-label="티켓 뒷면 회상 기록">
      <h1 title={back.title}>{back.title || '나의 추억'}</h1>
      <p className="memory-card__ticket-summary">
        <span>동행: {companions}</span>
        <span aria-hidden="true">|</span>
        <span>날씨: {back.weather || '-'}</span>
        <span aria-hidden="true">|</span>
        <span>기분: {back.mood || '-'}</span>
      </p>

      {isDirect ? (
        <section className="memory-card__ticket-page memory-card__ticket-page--direct" aria-label="직접 작성한 추억">
          <p>{back.memoryText || '기록한 추억이 없어요.'}</p>
        </section>
      ) : (
        <section className="memory-card__ticket-page" aria-live="polite">
          <h2>질문 {page.questionIndex + 1}{page.isContinuation ? ' (계속)' : ''}. {page.question || '추억을 떠올려보세요.'}</h2>
          <p>{page.answer}</p>
        </section>
      )}

      {!isDirect && pages.length > 1 && (
        <nav className="memory-card__ticket-page-navigation" aria-label="회상 질문 페이지 이동">
          <button type="button" onClick={() => setSelectedPage((index) => Math.max(0, index - 1))} disabled={currentPage === 0} aria-label="이전 질문">‹</button>
          <span>{currentPage + 1} / {pages.length}</span>
          <button type="button" onClick={() => setSelectedPage((index) => Math.min(pages.length - 1, index + 1))} disabled={currentPage === pages.length - 1} aria-label="다음 질문">›</button>
        </nav>
      )}
    </section>
  )
}

function CardBack({ card }) {
  const { back } = card
  const isLetter = card.documentType === 'LETTER'
  const isTicket = card.documentType === 'TICKET'
  const isReceipt = card.documentType === 'RECEIPT'
  const isRecall = card.documentType === 'TICKET' && back.writingMode === 'AI_RECALL'
  const photoUrls = Array.isArray(back.backPhotoUrls) ? back.backPhotoUrls : []
  const answeredQuestions = Array.isArray(back.answers)
    ? back.answers.filter(({ answer }) => typeof answer === 'string' && answer.trim())
    : []

  return (
    <article className={`memory-card memory-card--back${isLetter ? ' memory-card--letter' : ''}${isTicket ? ' memory-card--ticket' : ''}${isReceipt ? ' memory-card--receipt' : ''}`}>
      <dl className="memory-card__metadata">
        <div><dt>함께한 사람</dt><dd>{back.companions?.length ? back.companions.join(', ') : '혼자'}</dd></div>
        <div><dt>날씨</dt><dd>{back.weather}</dd></div>
        <div><dt>기분</dt><dd>{back.mood}</dd></div>
      </dl>

      {isTicket && <TicketBackContent back={back} />}

      {isReceipt && (back.diaryText || photoUrls.length > 0) && (
        <section className="memory-card__receipt-content" aria-label="영수증 뒷면 기록">
          {back.diaryText && <p className="memory-card__diary">{back.diaryText}</p>}
          {photoUrls.length > 0 && (
            <div className="memory-card__photos" aria-label="추가 사진">
              {photoUrls.map((photoUrl, index) => (
                <AuthorizedImage key={`${photoUrl}-${index}`} imageUrl={getImageUrl(photoUrl)} alt={`추가 사진 ${index + 1}`} loading="lazy" />
              ))}
            </div>
          )}
        </section>
      )}

      {!isReceipt && card.documentType !== 'TICKET' && back.diaryText && <p className="memory-card__diary">{back.diaryText}</p>}
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

      {!isReceipt && photoUrls.length > 0 && (
        <div className="memory-card__photos" aria-label="추가 사진">
          {photoUrls.map((photoUrl, index) => (
            <AuthorizedImage key={`${photoUrl}-${index}`} imageUrl={getImageUrl(photoUrl)} alt={`추가 사진 ${index + 1}`} loading="lazy" />
          ))}
        </div>
      )}
    </article>
  )
}

export default CardDetail
