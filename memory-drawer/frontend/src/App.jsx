import { Navigate, Route, Routes, useLocation, useParams } from "react-router-dom";
import CardSavePage from "./features/card-save/CardSavePage";
import LoginPage from "./pages/auth/LoginPage";
import SignupPage from "./pages/auth/SignupPage";
import HomePage from "./pages/HomePage";
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
      <Route path="/home" element={<HomePage />} />
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

function CardSaveRoute() {
  const { draftId } = useParams();
  const location = useLocation();
  const frontConfirmed = location.state?.frontConfirmed;
  const ticketRecall = location.state?.ticketRecall || getTicketRecall(draftId);

  if (!frontConfirmed?.documentType) {
    return (
      <main className="mobile-page ticket-recall-page">
        <h1>저장할 카드 정보가 없습니다.</h1>
        <p>앞면 확정 단계부터 다시 진행해주세요.</p>
      </main>
    );
  }

  return (
    <CardSavePage
      draftId={draftId}
      documentType={frontConfirmed.documentType}
      ticketRecall={ticketRecall}
    />
  );
}

export default App;
