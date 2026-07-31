import { useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { saveTicketRecall } from "../../utils/ticketRecallStorage";
import "./TicketRecall.css";

export default function TicketRecallTitlePage() {
    const { draftId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();
    const recallState = location.state;
    const [title, setTitle] = useState(recallState?.titleCandidate || "");
    const [isConfirmed, setIsConfirmed] = useState(false);
    const [error, setError] = useState("");

    if (!recallState?.frontConfirmed || !Array.isArray(recallState.answers)) {
        return (
            <main className="mobile-page ticket-recall-page">
                <h1>제목 정보가 없습니다.</h1>
                <p>회상 질문부터 다시 진행해주세요.</p>
                <button className="ticket-primary-button" onClick={() => navigate(`/memories/${draftId}/back`)}>기록 방식 선택으로 이동</button>
            </main>
        );
    }

    const handleConfirm = () => {
        const finalTitle = title.trim();

        if (!finalTitle) {
            setError("제목을 입력해주세요.");
            return;
        }

        saveTicketRecall(draftId, {
            writingMode: "AI_RECALL",
            title: finalTitle,
            answers: recallState.answers,
        });
        setIsConfirmed(true);
        setError("");
    };

    return (
        <main className="mobile-page ticket-recall-page">
            <button className="text-back-button" onClick={() => navigate(-1)}>← 돌아가기</button>
            <p className="ticket-recall-eyebrow">제목 최종 확인</p>
            <h1>이 제목으로<br />기억을 남길까요?</h1>
            <p className="ticket-recall-description">
                {recallState.titleGenerationFailed
                    ? "제목을 만들지 못했어요. 기억에 남는 말로 직접 제목을 입력해주세요."
                    : "AI가 제안한 제목은 언제든 직접 수정할 수 있어요."}
            </p>

            <label className="title-editor">
                제목
                <input value={title} onChange={(event) => setTitle(event.target.value)} placeholder="제목을 입력해주세요" />
            </label>

            {error && <p className="form-error">{error}</p>}
            {!isConfirmed ? (
                <button className="ticket-primary-button" onClick={handleConfirm}>제목 확정하기</button>
            ) : (
                <section className="ticket-recall-complete" aria-live="polite">
                    <strong>제목을 확정했어요.</strong>
                    <p>이제 동행인·날씨·기분을 입력한 뒤 추억 카드를 저장하면 돼요.</p>
                </section>
            )}

            {/* TODO(API 7 화면 연결): 확정한 title과 answers는 sessionStorage에 보관했다. 카드 저장 화면에서 draftId를 기준으로 이 값을 읽어 AI_RECALL 저장 요청에 사용하며, ticketSubtype는 API 7 요청에 다시 보내지 않는다. */}
        </main>
    );
}
