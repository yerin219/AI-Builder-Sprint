// frontend/src/App.jsx

import { useState } from 'react'
import { login } from './api/auth'
import {
  analyzeMemoryDraft,
  confirmDocumentType,
  confirmFront,
} from './api/memoryDrafts'
import './App.css'

function App() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const [accessToken, setAccessToken] = useState(
    () => sessionStorage.getItem('accessToken') || '',
  )

  const [imageFile, setImageFile] = useState(null)
  const [analyzeResult, setAnalyzeResult] = useState(null)
  const [documentType, setDocumentType] = useState('')
  const [frontResult, setFrontResult] = useState(null)
  const [frontForm, setFrontForm] = useState({})
  const [confirmResult, setConfirmResult] = useState(null)

  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')

  function handleRequestError(error) {
    if (error.status === 401) {
      sessionStorage.removeItem('accessToken')
      setAccessToken('')
    }

    setMessage(
      `[${error.code || error.status || 'ERROR'}] ${error.message}`,
    )
  }

  async function handleLogin(event) {
    event.preventDefault()

    setLoading(true)
    setMessage('')

    try {
      const data = await login(email, password)

      sessionStorage.setItem(
        'accessToken',
        data.accessToken,
      )

      setAccessToken(data.accessToken)
      setMessage('로그인에 성공했습니다.')
    } catch (error) {
      handleRequestError(error)
    } finally {
      setLoading(false)
    }
  }

  async function handleAnalyze(event) {
    event.preventDefault()

    if (!imageFile) {
      setMessage('분석할 이미지를 선택해주세요.')
      return
    }

    if (imageFile.size > 10 * 1024 * 1024) {
      setMessage('이미지는 10MB 이하만 업로드할 수 있습니다.')
      return
    }

    setLoading(true)
    setMessage('')
    setAnalyzeResult(null)
    setDocumentType('')
    setFrontResult(null)
    setFrontForm({})
    setConfirmResult(null)

    try {
      const data = await analyzeMemoryDraft(
        accessToken,
        imageFile,
      )

      sessionStorage.setItem('draftId', data.draftId)

      setAnalyzeResult(data)
      setDocumentType(
        data.suggestedDocumentType || '',
      )

      setMessage('API3 문서 분석에 성공했습니다.')
    } catch (error) {
      handleRequestError(error)
    } finally {
      setLoading(false)
    }
  }

  async function handleConfirmDocumentType() {
    if (!analyzeResult?.draftId) {
      setMessage('API3 이미지 분석을 먼저 실행해주세요.')
      return
    }

    if (!documentType) {
      setMessage('문서 유형을 선택해주세요.')
      return
    }

    setLoading(true)
    setMessage('')

    try {
      const data = await confirmDocumentType(
        accessToken,
        analyzeResult.draftId,
        documentType,
      )

      const candidate = data.frontCandidate || {}

      const normalizedCandidate = Object.fromEntries(
        Object.entries(candidate).map(([key, value]) => [
          key,
          value ?? '',
        ]),
      )

      sessionStorage.setItem(
        'documentType',
        documentType,
      )

      setFrontResult(data)
      setFrontForm({
        ...normalizedCandidate,
        memoryDate: normalizedCandidate.memoryDate || '',
      })
      setConfirmResult(null)

      setMessage('API4 문서 유형 확정에 성공했습니다.')
    } catch (error) {
      handleRequestError(error)
    } finally {
      setLoading(false)
    }
  }

  function updateFrontField(fieldName, value) {
    setFrontForm((previous) => ({
      ...previous,
      [fieldName]: value,
    }))
  }

  function createConfirmFrontPayload() {
    if (documentType === 'RECEIPT') {
      return {
        memoryDate: frontForm.memoryDate,
        front: {
          storeName: frontForm.storeName?.trim(),
        },
      }
    }

    if (documentType === 'LETTER') {
      return {
        memoryDate: frontForm.memoryDate,
        front: {
          ocrText: frontForm.ocrText?.trim(),
        },
      }
    }

    if (documentType === 'TICKET') {
      return {
        memoryDate: frontForm.memoryDate,
        front: {
          eventName: frontForm.eventName?.trim(),
          venue: frontForm.venue?.trim(),
          seat: frontForm.seat?.trim() || null,
        },
      }
    }

    throw new Error('지원하지 않는 문서 유형입니다.')
  }

  function validateFrontForm() {
    if (!frontForm.memoryDate) {
      return '기억 날짜를 입력해주세요.'
    }

    if (
      documentType === 'RECEIPT' &&
      !frontForm.storeName?.trim()
    ) {
      return '가게 이름을 입력해주세요.'
    }

    if (
      documentType === 'LETTER' &&
      !frontForm.ocrText?.trim()
    ) {
      return '편지 내용을 입력해주세요.'
    }

    if (
      documentType === 'TICKET' &&
      !frontForm.eventName?.trim()
    ) {
      return '행사 이름을 입력해주세요.'
    }

    if (
      documentType === 'TICKET' &&
      !frontForm.venue?.trim()
    ) {
      return '장소를 입력해주세요.'
    }

    return null
  }

  async function handleConfirmFront() {
    if (!frontResult?.draftId) {
      setMessage('API4 문서 유형 확정을 먼저 진행해주세요.')
      return
    }

    const validationMessage = validateFrontForm()

    if (validationMessage) {
      setMessage(validationMessage)
      return
    }

    setLoading(true)
    setMessage('')

    try {
      const payload = createConfirmFrontPayload()

      const data = await confirmFront(
        accessToken,
        frontResult.draftId,
        payload,
      )

      setConfirmResult(data)
      setMessage('API5 카드 앞면 확정에 성공했습니다.')
    } catch (error) {
      handleRequestError(error)
    } finally {
      setLoading(false)
    }
  }

  function handleLogout() {
    sessionStorage.removeItem('accessToken')
    sessionStorage.removeItem('draftId')
    sessionStorage.removeItem('documentType')

    setAccessToken('')
    setEmail('')
    setPassword('')
    setImageFile(null)
    setAnalyzeResult(null)
    setDocumentType('')
    setFrontResult(null)
    setFrontForm({})
    setConfirmResult(null)
    setMessage('로그아웃했습니다.')
  }

  return (
    <main className="setup">
      <p className="eyebrow">Memory Drawer</p>
      <h1>API 3·4·5 연결 테스트</h1>

      {!accessToken ? (
        <form onSubmit={handleLogin}>
          <div>
            <label htmlFor="email">이메일</label>

            <input
              id="email"
              type="email"
              value={email}
              onChange={(event) =>
                setEmail(event.target.value)
              }
              required
            />
          </div>

          <div>
            <label htmlFor="password">비밀번호</label>

            <input
              id="password"
              type="password"
              value={password}
              maxLength={10}
              onChange={(event) =>
                setPassword(event.target.value)
              }
              required
            />
          </div>

          <button type="submit" disabled={loading}>
            {loading ? '로그인 중...' : '로그인'}
          </button>
        </form>
      ) : (
        <>
          <p>로그인되었습니다.</p>

          <button
            type="button"
            onClick={handleLogout}
          >
            로그아웃
          </button>

          <hr />

          <h2>API3 이미지 분석</h2>

          <form onSubmit={handleAnalyze}>
            <input
              type="file"
              accept="image/*"
              onChange={(event) => {
                const selectedFile =
                  event.target.files?.[0] || null

                setImageFile(selectedFile)
                setAnalyzeResult(null)
                setDocumentType('')
                setFrontResult(null)
                setFrontForm({})
                setConfirmResult(null)
                setMessage('')
              }}
            />

            <button
              type="submit"
              disabled={loading || !imageFile}
            >
              {loading ? '분석 중...' : '이미지 분석'}
            </button>
          </form>
        </>
      )}

      {message && <p>{message}</p>}

      {analyzeResult && (
        <section>
          <hr />

          <h2>API3 응답</h2>

          <p>
            draftId:{' '}
            <strong>{analyzeResult.draftId}</strong>
          </p>

          <p>
            추천 문서 유형:{' '}
            <strong>
              {analyzeResult.suggestedDocumentType ||
                '직접 선택 필요'}
            </strong>
          </p>

          <p>
            현재 상태:{' '}
            <strong>{analyzeResult.draftStatus}</strong>
          </p>

          <p>
            다음 작업:{' '}
            <strong>{analyzeResult.nextAction}</strong>
          </p>

          <pre>
            {JSON.stringify(analyzeResult, null, 2)}
          </pre>

          <hr />

          <h2>API4 문서 유형 확정</h2>

          <div>
            <label htmlFor="documentType">
              문서 유형
            </label>

            <select
              id="documentType"
              value={documentType}
              disabled={Boolean(frontResult)}
              onChange={(event) => {
                setDocumentType(event.target.value)
                setFrontResult(null)
                setFrontForm({})
                setConfirmResult(null)
              }}
            >
              <option value="">
                유형을 선택하세요
              </option>

              <option value="RECEIPT">
                영수증
              </option>

              <option value="LETTER">
                손편지
              </option>

              <option value="TICKET">
                티켓
              </option>
            </select>
          </div>

          <button
            type="button"
            disabled={
              loading ||
              !documentType ||
              Boolean(frontResult)
            }
            onClick={handleConfirmDocumentType}
          >
            {frontResult
              ? '문서 유형 확정 완료'
              : loading
                ? '정보 추출 중...'
                : '문서 유형 확정'}
          </button>
        </section>
      )}

      {frontResult && (
        <section>
          <hr />

          <h2>API5 카드 앞면 확인</h2>

          <p>
            문서 유형:{' '}
            <strong>{documentType}</strong>
          </p>

          <p>
            현재 상태:{' '}
            <strong>{frontResult.draftStatus}</strong>
          </p>

          <div>
            <label htmlFor="memoryDate">
              기억 날짜
            </label>

            <input
              id="memoryDate"
              type="date"
              value={frontForm.memoryDate || ''}
              onChange={(event) =>
                updateFrontField(
                  'memoryDate',
                  event.target.value,
                )
              }
              required
            />
          </div>

          {documentType === 'RECEIPT' && (
            <div>
              <label htmlFor="storeName">
                가게 이름
              </label>

              <input
                id="storeName"
                type="text"
                value={frontForm.storeName || ''}
                onChange={(event) =>
                  updateFrontField(
                    'storeName',
                    event.target.value,
                  )
                }
                required
              />
            </div>
          )}

          {documentType === 'LETTER' && (
            <div>
              <label htmlFor="ocrText">
                편지 내용
              </label>

              <textarea
                id="ocrText"
                value={frontForm.ocrText || ''}
                onChange={(event) =>
                  updateFrontField(
                    'ocrText',
                    event.target.value,
                  )
                }
                required
              />
            </div>
          )}

          {documentType === 'TICKET' && (
            <>
              <div>
                <label htmlFor="eventName">
                  행사 이름
                </label>

                <input
                  id="eventName"
                  type="text"
                  value={frontForm.eventName || ''}
                  onChange={(event) =>
                    updateFrontField(
                      'eventName',
                      event.target.value,
                    )
                  }
                  required
                />
              </div>

              <div>
                <label htmlFor="venue">
                  장소
                </label>

                <input
                  id="venue"
                  type="text"
                  value={frontForm.venue || ''}
                  onChange={(event) =>
                    updateFrontField(
                      'venue',
                      event.target.value,
                    )
                  }
                  required
                />
              </div>

              <div>
                <label htmlFor="seat">
                  좌석
                </label>

                <input
                  id="seat"
                  type="text"
                  value={frontForm.seat || ''}
                  onChange={(event) =>
                    updateFrontField(
                      'seat',
                      event.target.value,
                    )
                  }
                />
              </div>
            </>
          )}

          <button
            type="button"
            disabled={loading}
            onClick={handleConfirmFront}
          >
            {loading
              ? '확정 중...'
              : confirmResult
                ? '카드 앞면 다시 확정'
                : '카드 앞면 확정'}
          </button>

          <h3>API4 응답</h3>

          <pre>
            {JSON.stringify(frontResult, null, 2)}
          </pre>
        </section>
      )}

      {confirmResult && (
        <section>
          <hr />

          <h2>API5 완료</h2>

          <p>카드 앞면이 확정되었습니다.</p>

          <p>
            draftId:{' '}
            <strong>{confirmResult.draftId}</strong>
          </p>

          <p>
            현재 상태:{' '}
            <strong>{confirmResult.draftStatus}</strong>
          </p>

          <p>
            다음 작업:{' '}
            <strong>{confirmResult.nextAction}</strong>
          </p>

          <pre>
            {JSON.stringify(confirmResult, null, 2)}
          </pre>
        </section>
      )}
    </main>
  )
}

export default App
