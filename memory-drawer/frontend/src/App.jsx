import { useState } from "react";
import { Navigate, Route, Routes, useLocation, useNavigate, useParams } from "react-router-dom";
import CardSavePage from "./features/card-save/CardSavePage";
import CardDetail from "./features/drawer/CardDetail";
import DrawerCardList from "./features/drawer/DrawerCardList";
import DrawerHome from "./features/drawer/DrawerHome";
import LoginPage from "./pages/auth/LoginPage";
import SignupPage from "./pages/auth/SignupPage";
import ImageSelectPage from "./pages/memory/ImageSelectPage";
import ImagePreviewPage from "./pages/memory/ImagePreviewPage";
import DocumentTypePage from "./pages/memory/DocumentTypePage";
import FrontConfirmPage from "./pages/memory/FrontConfirmPage";
import TicketBackModePage from "./pages/memory/TicketBackModePage";
import TicketRecallSubtypePage from "./pages/memory/TicketRecallSubtypePage";
import TicketRecallQuestionsPage from "./pages/memory/TicketRecallQuestionsPage";
import TicketRecallTitlePage from "./pages/memory/TicketRecallTitlePage";
import { getTicketRecall } from "./utils/ticketRecallStorage";

function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/home" element={<DrawerHomeRoute />} />
      <Route path="/drawers/:year" element={<DrawerCardListRoute />} />
      <Route path="/cards/:cardId" element={<CardDetailRoute />} />
      <Route path="/memories/new" element={<ImageSelectPage />} />
      <Route path="/memories/preview" element={<ImagePreviewPage />} />
      <Route path="/memories/:draftId/type" element={<DocumentTypePage />} />
      <Route path="/memories/:draftId/front" element={<FrontConfirmPage />} />
      <Route path="/memories/:draftId/back" element={<TicketBackModePage />} />
      <Route path="/memories/:draftId/ticket-recall/subtype" element={<TicketRecallSubtypePage />} />
      <Route path="/memories/:draftId/ticket-recall/questions" element={<TicketRecallQuestionsPage />} />
      <Route path="/memories/:draftId/ticket-recall/title" element={<TicketRecallTitlePage />} />
      <Route path="/memories/:draftId/save" element={<CardSaveRoute />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

function DrawerHomeRoute() {
  const navigate = useNavigate();
  return <DrawerHome onCreateMemory={() => navigate("/memories/new")} onSelectYear={(year) => navigate(`/drawers/${year}`)} />;
}

function DrawerCardListRoute() {
  const { year } = useParams();
  const navigate = useNavigate();
  const [cards, setCards] = useState([]);
  return <DrawerCardList year={Number(year)} onBack={() => navigate("/home")} onCardsLoaded={setCards} onSelectCard={(cardId) => navigate(`/cards/${cardId}`, { state: { cardsInYear: cards } })} />;
}

function CardDetailRoute() {
  const { cardId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const cardsInYear = location.state?.cardsInYear || [];
  return <CardDetail cardId={cardId} cardsInYear={cardsInYear} onBack={() => navigate(-1)} onSelectCard={(nextCardId) => navigate(`/cards/${nextCardId}`, { state: { cardsInYear } })} />;
}

function CardSaveRoute() {
  const { draftId } = useParams();
  const location = useLocation();
  const frontConfirmed = location.state?.frontConfirmed;
  const ticketRecall = location.state?.ticketRecall || getTicketRecall(draftId);

  if (!frontConfirmed?.documentType) {
    return <main className="mobile-page ticket-recall-page"><h1>저장할 카드 정보가 없습니다.</h1><p>앞면 확정 단계부터 다시 진행해주세요.</p></main>;
  }

  return <CardSavePage draftId={draftId} documentType={frontConfirmed.documentType} ticketRecall={ticketRecall} />;
}

export default App;
