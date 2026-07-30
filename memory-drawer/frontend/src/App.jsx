import CardSavePage from './features/card-save/CardSavePage'

function App() {
  const searchParams = new URLSearchParams(window.location.search)
  const draftId = searchParams.get('draftId') || ''
  const documentType = searchParams.get('documentType') || 'RECEIPT'
  const ticketRecall = window.history.state?.ticketRecall

  return (
    <CardSavePage
      draftId={draftId}
      documentType={documentType}
      ticketRecall={ticketRecall}
    />
  )
}

export default App
