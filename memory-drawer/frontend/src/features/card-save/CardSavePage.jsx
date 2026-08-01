import { useState } from 'react'
import { removeFrontConfirmed } from '../../utils/draftStorage.js'
import { removeTicketRecall, removeTicketRecallFlow } from '../../utils/ticketRecallStorage.js'
import { CardSaveError, saveCard } from './cardSaveApi.js'
import './CardSavePage.css'

const SUPPORTED_DOCUMENT_TYPES = new Set(['RECEIPT', 'LETTER', 'TICKET'])
const DOCUMENT_LABELS = {
  RECEIPT: '영수증',
  TICKET: '티켓',
  LETTER: '손편지',
}
const SAVE_ERROR_MESSAGES = {
  VALIDATION_001: '입력한 내용을 다시 확인해주세요.',
  DRAFT_001: '저장할 임시 기록을 찾을 수 없습니다. 처음부터 다시 진행해주세요.',
  DRAFT_003: '앞면이 아직 확정되지 않았거나 이미 저장된 기록입니다.',
  IMAGE_002: '추가 사진의 용량이 제한을 초과했습니다.',
  IMAGE_003: '지원하지 않는 추가 사진 형식입니다.',
  CARD_003: '카드를 저장하지 못했습니다. 잠시 후 다시 시도해주세요.',
}

function splitCompanions(value) {
  return value.split(',').map((companion) => companion.trim()).filter(Boolean)
}

function PreviewField({ label, value }) {
  const displayValue = Array.isArray(value) ? value.join(', ') : value
  if (displayValue === null || displayValue === undefined || displayValue === '') return null

  return <div><dt>{label}</dt><dd>{displayValue}</dd></div>
}

function PurchaseItemsPreview({ items }) {
  const purchaseItems = Array.isArray(items)
    ? items.filter((item) => (
      item
      && typeof item.name === 'string'
      && item.name.trim()
      && Number.isInteger(Number(item.quantity))
      && Number(item.quantity) > 0
    ))
    : []

  if (purchaseItems.length === 0) return null

  return (
    <div className="card-preview__purchase-items">
      <dt>구매 항목</dt>
      <dd>
        <ul>
          {purchaseItems.map(({ name, quantity }, index) => (
            <li key={`${name}-${quantity}-${index}`}>
              <span>{name}</span>
              <strong>× {Number(quantity)}</strong>
            </li>
          ))}
        </ul>
      </dd>
    </div>
  )
}

function CardSavePage({ draftId, documentType, frontConfirmed, ticketRecall, onSaved, onOpenDrawer }) {
  const [companionInput, setCompanionInput] = useState('')
  const [companions, setCompanions] = useState([])
  const [weather, setWeather] = useState('')
  const [mood, setMood] = useState('')
  const [diaryText, setDiaryText] = useState('')
  const [backPhotos, setBackPhotos] = useState([])
  const writingMode = ticketRecall ? 'AI_RECALL' : 'DIRECT'
  const [title, setTitle] = useState(ticketRecall?.title || ticketRecall?.titleCandidate || '')
  const [memoryText, setMemoryText] = useState('')
  const [formError, setFormError] = useState('')
  const [saveError, setSaveError] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [savedCard, setSavedCard] = useState(null)
  const [previewBack, setPreviewBack] = useState(null)

  const isTicket = documentType === 'TICKET'
  const isRecallFlow = isTicket && Boolean(ticketRecall)
  const documentLabel = documentType === 'LETTER' ? '손편지' : isTicket ? '티켓' : '영수증'
  const isSupportedDocument = SUPPORTED_DOCUMENT_TYPES.has(documentType)
  const hasDiaryOrPhoto = diaryText.trim().length > 0 || backPhotos.length > 0
  const recallAnswers = ticketRecall?.answers || []
  const hasRecallAnswers = recallAnswers.length === 3
    && new Set(recallAnswers.map(({ questionId }) => questionId)).size === 3
    && recallAnswers.some(({ answer }) => typeof answer === 'string' && answer.trim())

  function addCompanions() {
    const newCompanions = splitCompanions(companionInput)
    if (newCompanions.length === 0) return

    setCompanions((current) => [...current, ...newCompanions.filter((name) => !current.includes(name))])
    setCompanionInput('')
  }

  function handleCompanionKeyDown(event) {
    if (event.key === 'Enter') {
      event.preventDefault()
      addCompanions()
    }
  }

  function handlePhotoChange(event) {
    const selectedPhotos = Array.from(event.target.files || [])
    setBackPhotos((current) => [...current, ...selectedPhotos])
    event.target.value = ''
  }

  function buildBack() {
    const submittedCompanions = [
      ...companions,
      ...splitCompanions(companionInput).filter((name) => !companions.includes(name)),
    ]
    const commonBack = {
      companions: submittedCompanions,
      weather: weather.trim(),
      mood: mood.trim(),
    }

    if (!isTicket) {
      return { ...commonBack, diaryText: diaryText.trim() || null }
    }

    if (writingMode === 'DIRECT') {
      return {
        ...commonBack,
        writingMode: 'DIRECT',
        title: title.trim(),
        memoryText: memoryText.trim(),
      }
    }

    return {
      ...commonBack,
      writingMode: 'AI_RECALL',
      title: title.trim(),
      answers: recallAnswers.map(({ questionId, answer }) => ({
        questionId,
        answer: typeof answer === 'string' && answer.trim() ? answer.trim() : null,
      })),
    }
  }

  function getSaveErrorMessage(error) {
    if (error instanceof CardSaveError && SAVE_ERROR_MESSAGES[error.code]) {
      return SAVE_ERROR_MESSAGES[error.code]
    }

    return error.message
  }

  function validateTicket() {
    if (writingMode === 'DIRECT' && (!title.trim() || !memoryText.trim())) {
      return '제목과 추억을 모두 입력해주세요.'
    }

    if (writingMode === 'AI_RECALL' && (!hasRecallAnswers || !title.trim())) {
      return 'AI 회상 질문의 답변과 확정할 제목이 필요합니다.'
    }

    return ''
  }

  function validateForm() {
    if (!draftId) return '앞면이 확정된 기록에서만 카드를 저장할 수 있습니다.'
    if (!isSupportedDocument) return '지원하지 않는 기록 유형입니다.'
    if (!weather.trim() || !mood.trim()) return '날씨와 기분을 입력해주세요.'
    if (!isTicket && !hasDiaryOrPhoto) return '자유 기록 또는 추가 사진 중 하나 이상을 입력해주세요.'
    return isTicket ? validateTicket() : ''
  }

  function handlePreview(event) {
    event.preventDefault()
    setFormError('')
    setSaveError('')

    const validationError = validateForm()
    if (validationError) {
      setFormError(validationError)
      return
    }

    setPreviewBack(buildBack())
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  async function handleSave() {
    if (!previewBack) return

    setIsSaving(true)
    setSaveError('')
    try {
      const saved = await saveCard({
        draftId,
        back: previewBack,
        backPhotos: isTicket ? [] : backPhotos,
      })
      removeFrontConfirmed(draftId)
      removeTicketRecall(draftId)
      removeTicketRecallFlow(draftId)
      setSavedCard(saved)
      onSaved?.(saved)
    } catch (error) {
      setSaveError(getSaveErrorMessage(error))
    } finally {
      setIsSaving(false)
    }
  }

  if (savedCard) {
    return (
      <main className="card-save-page">
        <section className="save-result" aria-live="polite">
          <p className="save-result__eyebrow">기억서랍</p>
          <h1>추억 카드를 저장했어요.</h1>
          <p>{savedCard.year}년 서랍에서 이 카드를 다시 꺼내볼 수 있어요.</p>
          <dl className="save-result__details">
            <div><dt>기록 유형</dt><dd>{DOCUMENT_LABELS[savedCard.documentType] || savedCard.documentType}</dd></div>
            <div><dt>추억 날짜</dt><dd>{savedCard.memoryDate}</dd></div>
          </dl>
          <button className="primary-button" type="button" onClick={() => onOpenDrawer?.(savedCard.year)}>
            {savedCard.year}년 서랍에서 확인하기
          </button>
        </section>
      </main>
    )
  }

  if (previewBack) {
    const front = frontConfirmed?.front || {}
    const answeredRecall = Array.isArray(previewBack.answers)
      ? previewBack.answers.filter(({ answer }) => answer)
      : []

    return (
      <main className="card-save-page">
        <section className="card-preview">
          <header className="card-preview__header">
            <p className="card-save-form__eyebrow">최종 미리보기</p>
            <h1>앞면과 뒷면을 확인해주세요.</h1>
            <p>비어 있는 선택 항목은 카드에 표시되지 않아요.</p>
          </header>

          <div className="card-preview__grid">
            <article className="card-preview__side">
              <span>앞면</span>
              <h2>{documentLabel}</h2>
              <dl className="card-preview__fields">
                <PreviewField label="기억 날짜" value={frontConfirmed?.memoryDate} />
                {documentType === 'RECEIPT' && <PreviewField label="가게 이름" value={front.storeName} />}
                {documentType === 'RECEIPT' && <PurchaseItemsPreview items={front.purchaseItems} />}
                {documentType === 'TICKET' && <PreviewField label="행사명" value={front.eventName} />}
                {documentType === 'TICKET' && <PreviewField label="장소" value={front.venue} />}
                {documentType === 'TICKET' && <PreviewField label="좌석" value={front.seat} />}
                {documentType === 'LETTER' && <PreviewField label="편지 내용" value={front.ocrText} />}
              </dl>
            </article>

            <article className="card-preview__side">
              <span>뒷면</span>
              <h2>그날의 기억</h2>
              <dl className="card-preview__fields">
                <PreviewField label="함께한 사람" value={previewBack.companions.length ? previewBack.companions : '혼자'} />
                <PreviewField label="날씨" value={previewBack.weather} />
                <PreviewField label="기분" value={previewBack.mood} />
                <PreviewField label="제목" value={previewBack.title} />
                <PreviewField label="추억" value={previewBack.memoryText} />
                <PreviewField label="일기" value={previewBack.diaryText} />
                {!isTicket && backPhotos.length > 0 && <PreviewField label="추가 사진" value={`${backPhotos.length}장`} />}
              </dl>
              {answeredRecall.length > 0 && (
                <ol className="card-preview__answers">
                  {answeredRecall.map(({ questionId, answer }, index) => <li key={questionId}><strong>회상 답변 {index + 1}</strong><p>{answer}</p></li>)}
                </ol>
              )}
            </article>
          </div>

          {saveError && <p className="form-error" role="alert">{saveError}</p>}
          <div className="card-preview__actions">
            <button className="secondary-button" type="button" disabled={isSaving} onClick={() => setPreviewBack(null)}>수정하기</button>
            <button className="primary-button" type="button" disabled={isSaving} onClick={handleSave}>{isSaving ? '저장 중...' : '이대로 저장하기'}</button>
          </div>
        </section>
      </main>
    )
  }

  return (
    <main className="card-save-page">
      <form className="card-save-form" onSubmit={handlePreview}>
        <header className="card-save-form__header">
          <p className="card-save-form__eyebrow">추가 정보 입력</p>
          <h1>{documentLabel} 뒷면을 채워주세요.</h1>
          <p>그날의 기억은 사용자가 직접 남겨요.</p>
        </header>

        <section className="form-section" aria-labelledby="companion-label">
          <label id="companion-label" htmlFor="companion-input">동행인</label>
          <div className="field-row">
            <input id="companion-input" value={companionInput} onChange={(event) => setCompanionInput(event.target.value)} onKeyDown={handleCompanionKeyDown} placeholder="이름을 입력하고 추가하세요" />
            <button className="secondary-button" type="button" onClick={addCompanions}>추가</button>
          </div>
          <p className="field-help">혼자였다면 비워두세요. 여러 명은 쉼표로 구분할 수 있어요.</p>
          {companions.length > 0 && <ul className="tag-list" aria-label="입력한 동행인">{companions.map((companion) => <li key={companion}>{companion}<button type="button" onClick={() => setCompanions((current) => current.filter((name) => name !== companion))} aria-label={`${companion} 삭제`}>×</button></li>)}</ul>}
        </section>

        <section className="form-section field-grid">
          <div><label htmlFor="weather">날씨 <span aria-hidden="true">*</span></label><input id="weather" value={weather} onChange={(event) => setWeather(event.target.value)} placeholder="예: 맑음" required /></div>
          <div><label htmlFor="mood">기분 <span aria-hidden="true">*</span></label><input id="mood" value={mood} onChange={(event) => setMood(event.target.value)} placeholder="예: 행복" required /></div>
        </section>

        {isTicket ? (
          <>
            {isRecallFlow ? (
              <section className="form-section recall-summary">
                <span className="form-label">기록 방식</span>
                <p>AI 회상 질문으로 정리한 기억을 저장합니다.</p>
              </section>
            ) : (
              <section className="form-section">
                <span className="form-label">기록 방식</span>
                <div className="writing-mode" role="group" aria-label="티켓 기록 방식">
                  <button className="writing-mode__button writing-mode__button--selected" type="button">직접 기록하기</button>
                </div>
                <p className="field-help">AI 질문 방식은 이전 기록 방식 선택 화면에서 고를 수 있어요.</p>
              </section>
            )}
            <section className="form-section">
              <label htmlFor="ticket-title">제목 <span aria-hidden="true">*</span></label>
              <input id="ticket-title" value={title} onChange={(event) => setTitle(event.target.value)} placeholder="제목을 입력해주세요" required />
            </section>
            {writingMode === 'DIRECT' ? (
              <section className="form-section"><label htmlFor="memory-text">추억 <span aria-hidden="true">*</span></label><textarea id="memory-text" value={memoryText} onChange={(event) => setMemoryText(event.target.value)} placeholder="이날의 추억을 자유롭게 기록해주세요." rows="6" required /></section>
            ) : (
              <section className="form-section recall-summary"><span className="form-label">AI 회상 답변</span>{hasRecallAnswers ? <p>API 6에서 작성한 세 질문의 답변을 카드에 함께 저장합니다.</p> : <p>API 6의 질문·답변·제목 후보를 받은 뒤 저장할 수 있습니다.</p>}</section>
            )}
          </>
        ) : (
          <>
            <section className="form-section"><label htmlFor="diary-text">자유 기록</label><textarea id="diary-text" value={diaryText} onChange={(event) => setDiaryText(event.target.value)} placeholder="이날의 추억을 자유롭게 기록해주세요." rows="6" /></section>
            <section className="form-section"><span className="form-label">추가 사진</span><label className="photo-picker" htmlFor="back-photos"><span aria-hidden="true">＋</span>사진 추가하기</label><input id="back-photos" type="file" accept="image/*" multiple onChange={handlePhotoChange} /><p className="field-help">자유 기록 또는 추가 사진 중 하나 이상이 필요해요.</p>{backPhotos.length > 0 && <ul className="photo-list" aria-label="추가한 사진">{backPhotos.map((photo, index) => <li key={`${photo.name}-${photo.lastModified}-${index}`}><span>{photo.name}</span><button type="button" onClick={() => setBackPhotos((current) => current.filter((_, photoIndex) => photoIndex !== index))}>삭제</button></li>)}</ul>}</section>
          </>
        )}

        {(formError || saveError) && <p className="form-error" role="alert">{formError || saveError}</p>}
        <button className="primary-button" type="submit">앞·뒷면 미리보기</button>
      </form>
    </main>
  )
}

export default CardSavePage
