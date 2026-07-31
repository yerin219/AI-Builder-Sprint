import { Navigate, Route, Routes } from "react-router-dom";
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

function App() {
  return (
    <Routes>
      {/* 처음 접속하면 로그인 화면 */}
      <Route path="/" element={<Navigate to="/login" replace />} />

      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />

      {/* 로그인 후 보여줄 홈 화면 */}
      <Route path="/home" element={<HomePage />} />

      <Route
        path="/memories/new"
        element={<ImageSelectPage />}
      />

      <Route
        path="/memories/preview"
        element={<ImagePreviewPage />}
      />

      <Route
        path="/memories/:draftId/type"
        element={<DocumentTypePage />}
      />

      <Route
        path="/memories/:draftId/front"
        element={<FrontConfirmPage />}
      />

      <Route
        path="/memories/:draftId/back"
        element={<TicketBackModePage />}
      />

      <Route
        path="/memories/:draftId/ticket-recall/subtype"
        element={<TicketRecallSubtypePage />}
      />

      <Route
        path="/memories/:draftId/ticket-recall/questions"
        element={<TicketRecallQuestionsPage />}
      />

      <Route
        path="/memories/:draftId/ticket-recall/title"
        element={<TicketRecallTitlePage />}
      />

      {/* 없는 주소로 들어가면 로그인으로 이동 */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

export default App;
