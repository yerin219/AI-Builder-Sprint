import './App.css'

function App() {
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api'

  return (
    <main className="setup">
      <p className="eyebrow">Memory Drawer</p>
      <h1>프론트엔드 개발환경이 준비되었습니다.</h1>
      <p>React와 Vite로 실행되는 초기 프로젝트입니다.</p>
      <p className="api-base">API 기본 경로: {apiBaseUrl}</p>
    </main>
  )
}

export default App
