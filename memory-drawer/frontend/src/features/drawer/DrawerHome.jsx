import { useEffect, useState } from 'react'
import { fetchDrawers } from './drawerApi'
import './DrawerHome.css'

function DrawerHome({ onCreateMemory, onSelectYear }) {
  const [drawers, setDrawers] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function loadDrawers() {
      setIsLoading(true)
      setErrorMessage('')

      try {
        const loadedDrawers = await fetchDrawers({ signal: controller.signal })
        setDrawers(loadedDrawers)
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

    loadDrawers()
    return () => controller.abort()
  }, [reloadKey])

  return (
    <main className="drawer-home">
      <header className="drawer-home__header">
        <p className="drawer-home__brand">기억서랍</p>
        <h1>꺼내 보고 싶은<br />기억을 골라보세요.</h1>
        <p>실제 추억 날짜를 기준으로 차곡차곡 담아두었어요.</p>
      </header>

      <section className="drawer-home__content" aria-live="polite">
        {isLoading && <p className="drawer-home__status">서랍을 불러오는 중이에요.</p>}

        {!isLoading && errorMessage && (
          <section className="drawer-home__message" role="alert">
            <p>{errorMessage}</p>
            <button type="button" onClick={() => setReloadKey((key) => key + 1)}>다시 시도</button>
          </section>
        )}

        {!isLoading && !errorMessage && drawers.length === 0 && (
          <section className="drawer-home__message drawer-home__message--empty">
            <span aria-hidden="true">✦</span>
            <h2>아직 담긴 기억이 없어요.</h2>
            <p>첫 번째 종이 기록을 추억 카드로 남겨보세요.</p>
          </section>
        )}

        {!isLoading && !errorMessage && drawers.length > 0 && (
          <ul className="drawer-list" aria-label="연도별 서랍 목록">
            {drawers.map(({ year, cardCount }) => (
              <li key={year}>
                <button type="button" className="drawer-item" onClick={() => onSelectYear?.(year)}>
                  <span className="drawer-item__tape">{year}</span>
                  <span className="drawer-item__label">{year}년의 기억</span>
                  <span className="drawer-item__count">카드 {cardCount}장</span>
                  <span className="drawer-item__arrow" aria-hidden="true">›</span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      <button type="button" className="memory-create-button" onClick={() => onCreateMemory?.()}>
        <span aria-hidden="true">＋</span> 기억 담기
      </button>

      {/* TODO(API 8.2 연동): 연도를 누르면 해당 연도의 카드 목록 화면으로 이동하도록 onSelectYear에 라우팅을 연결한다. */}
      {/* TODO(이미지 분석 흐름 연동): 기억 담기 버튼은 이미지 선택·분석 화면이 구현되면 onCreateMemory에 이동 로직을 연결한다. */}
    </main>
  )
}

export default DrawerHome
