import { useState } from 'react'
import CardDetail from './features/drawer/CardDetail'
import DrawerCardList from './features/drawer/DrawerCardList'
import DrawerHome from './features/drawer/DrawerHome'

function App() {
  const [screen, setScreen] = useState('DRAWERS')
  const [selectedYear, setSelectedYear] = useState(null)
  const [selectedCardId, setSelectedCardId] = useState(null)
  const [yearCards, setYearCards] = useState([])

  function openYear(year) {
    setSelectedYear(year)
    setScreen('CARDS')
  }

  function openCard(cardId) {
    setSelectedCardId(cardId)
    setScreen('CARD_DETAIL')
  }

  if (screen === 'CARDS') {
    return (
      <DrawerCardList
        year={selectedYear}
        onBack={() => setScreen('DRAWERS')}
        onCardsLoaded={setYearCards}
        onSelectCard={openCard}
      />
    )
  }

  if (screen === 'CARD_DETAIL') {
    return (
      <CardDetail
        cardId={selectedCardId}
        cardsInYear={yearCards}
        onBack={() => setScreen('CARDS')}
        onSelectCard={setSelectedCardId}
      />
    )
  }

  return <DrawerHome onSelectYear={openYear} />
}

export default App
