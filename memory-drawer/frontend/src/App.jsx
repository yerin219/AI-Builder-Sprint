import { useEffect, useState } from "react";
import { Link, Navigate, Route, Routes, useLocation, useNavigate, useParams } from "react-router-dom";
import memoryDrawerLogo from "./assets/branding/memory-drawer-logo.png";
import "./App.css";
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
import RequireAuth from "./routes/RequireAuth";
import { getFrontConfirmed } from "./utils/draftStorage";
import { getTicketRecall } from "./utils/ticketRecallStorage";
import { getAccessToken, removeAccessToken } from "./utils/tokenStorage";

function App() {
  const location = useLocation();
  const navigate = useNavigate();
  const [isHelpOpen, setIsHelpOpen] = useState(false);
  const isPublicPage = location.pathname === "/" || location.pathname === "/login" || location.pathname === "/signup";
  const showLogout = !isPublicPage && Boolean(getAccessToken());
  const help = getPageHelp(location.pathname);

  useEffect(() => {
    setIsHelpOpen(false);
  }, [location.pathname]);

  function handleLogout() {
    removeAccessToken();
    navigate("/login", { replace: true });
  }

  return (
    <div className="app-shell">
      <header className={`app-brand-header${showLogout ? " app-brand-header--authenticated" : ""}`}>
        <button
          className="app-brand-header__help"
          type="button"
          aria-label="현재 화면 도움말"
          aria-haspopup="dialog"
          onClick={() => setIsHelpOpen(true)}
        >
          ?
        </button>
        <Link className="app-brand-header__logo-link" to="/home" aria-label="기억서랍 홈으로 이동">
          <img
            className="app-brand-header__logo"
            src={memoryDrawerLogo}
            alt="Memory Drawer"
          />
        </Link>
        {showLogout && (
          <button className="app-brand-header__logout" type="button" onClick={handleLogout}>
            로그아웃
          </button>
        )}
      </header>

      {isHelpOpen && (
        <div className="app-help-overlay" role="presentation" onMouseDown={() => setIsHelpOpen(false)}>
          <section
            className="app-help-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="app-help-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <button
              className="app-help-dialog__close"
              type="button"
              aria-label="도움말 닫기"
              onClick={() => setIsHelpOpen(false)}
            >
              ×
            </button>
            <span className="app-help-dialog__mark" aria-hidden="true">?</span>
            <h2 id="app-help-title">{help.title}</h2>
            <p>{help.description}</p>
            <button className="app-help-dialog__confirm" type="button" onClick={() => setIsHelpOpen(false)}>
              알겠어요
            </button>
          </section>
        </div>
      )}

      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route element={<RequireAuth />}>
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
        </Route>
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </div>
  );
}

function getPageHelp(pathname) {
  if (pathname === "/login" || pathname === "/") {
    return { title: "로그인", description: "가입한 이메일과 비밀번호를 입력하면 기억서랍을 열 수 있어요." };
  }
  if (pathname === "/signup") {
    return { title: "회원가입", description: "사용할 이메일과 비밀번호를 입력해 나만의 기억서랍을 만들어보세요." };
  }
  if (pathname === "/home") {
    return { title: "기억서랍", description: "연도 서랍을 누르면 저장한 카드를 볼 수 있어요. 새 기록은 ‘기억 담기’로 시작하세요." };
  }
  if (/^\/drawers\/\d+$/.test(pathname)) {
    return { title: "연도별 카드", description: "보고 싶은 카드를 누르면 앞면과 뒷면의 추억을 자세히 볼 수 있어요." };
  }
  if (/^\/cards\/[^/]+$/.test(pathname)) {
    return { title: "카드 상세", description: "앞면·뒷면을 바꿔 보고, 아래 버튼으로 내용을 수정하거나 카드를 삭제할 수 있어요." };
  }
  if (pathname === "/memories/new") {
    return { title: "기억 담기", description: "종이 기록을 카메라로 촬영하거나 앨범에서 이미지를 선택해주세요." };
  }
  if (pathname === "/memories/preview") {
    return { title: "사진 확인", description: "글자가 잘 보이는지 확인하세요. 흐리거나 잘렸다면 다른 사진을 선택할 수 있어요." };
  }
  if (/\/type$/.test(pathname)) {
    return { title: "기록 종류 확인", description: "AI가 찾은 기록 종류가 맞는지 확인하고, 다르면 직접 올바른 종류를 선택해주세요." };
  }
  if (/\/front$/.test(pathname)) {
    return { title: "앞면 확인", description: "추출된 날짜와 내용을 확인한 뒤 틀린 부분을 고치고 앞면을 확정해주세요." };
  }
  if (/\/back$/.test(pathname)) {
    return { title: "뒷면 작성", description: "추억을 직접 작성하거나 AI 질문으로 기억을 떠올리는 방법을 선택할 수 있어요." };
  }
  if (/\/ticket-recall\/subtype$/.test(pathname)) {
    return { title: "티켓 종류 확인", description: "공연·영화·전시 중 티켓과 가장 잘 맞는 종류를 선택해주세요." };
  }
  if (/\/ticket-recall\/questions$/.test(pathname)) {
    return { title: "기억 떠올리기", description: "세 가지 질문에 편하게 답해주세요. 답변을 바탕으로 추억 제목을 제안해드려요." };
  }
  if (/\/ticket-recall\/title$/.test(pathname)) {
    return { title: "제목 정하기", description: "제안된 제목을 그대로 사용하거나 내 마음에 들도록 직접 고쳐주세요." };
  }
  if (/\/save$/.test(pathname)) {
    return { title: "카드 저장", description: "함께한 사람·날씨·기분과 추억을 작성하고, 최종 내용을 확인한 뒤 저장해주세요." };
  }
  return { title: "도움말", description: "화면의 안내에 따라 기억을 차근차근 담아보세요." };
}

function DrawerHomeRoute() {
  const navigate = useNavigate();
  return <DrawerHome onCreateMemory={() => navigate("/memories/new")} onSelectYear={(year) => navigate(`/drawers/${year}`)} />;
}

function DrawerCardListRoute() {
  const { year } = useParams();
  const navigate = useNavigate();
  const [cards, setCards] = useState([]);
  const numericYear = Number(year);
  return <DrawerCardList year={numericYear} onBack={() => navigate("/home")} onCardsLoaded={setCards} onSelectCard={(cardId) => navigate(`/cards/${cardId}`, { state: { cardsInYear: cards, year: numericYear } })} />;
}

function CardDetailRoute() {
  const { cardId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const cardsInYear = location.state?.cardsInYear || [];
  const year = location.state?.year;
  return <CardDetail cardId={cardId} cardsInYear={cardsInYear} onBack={() => navigate(year ? `/drawers/${year}` : "/home", { replace: true })} onSelectCard={(nextCardId) => navigate(`/cards/${nextCardId}`, { replace: true, state: { cardsInYear, year } })} />;
}

function CardSaveRoute() {
  const { draftId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const frontConfirmed = location.state?.frontConfirmed || getFrontConfirmed(draftId);
  const ticketRecall = location.state?.ticketRecall || getTicketRecall(draftId);

  if (!frontConfirmed?.documentType) {
    return <main className="mobile-page ticket-recall-page"><h1>저장할 카드 정보가 없습니다.</h1><p>앞면 확정 단계부터 다시 진행해주세요.</p></main>;
  }

  return (
    <CardSavePage
      draftId={draftId}
      documentType={frontConfirmed.documentType}
      frontConfirmed={frontConfirmed}
      ticketRecall={ticketRecall}
      onOpenDrawer={(year) => navigate(`/drawers/${year}`, { replace: true })}
    />
  );
}

export default App;
