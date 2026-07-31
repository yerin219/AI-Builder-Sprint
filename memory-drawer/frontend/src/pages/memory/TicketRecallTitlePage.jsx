import { useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import {
    getTicketRecallFlow,
    removeTicketRecallFlow,
    saveTicketRecall,
    saveTicketRecallFlow,
} from "../../utils/ticketRecallStorage.js";
import "./TicketRecall.css";

export default function TicketRecallTitlePage() {
    const { draftId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();
    const recallState = location.state?.answers
        ? location.state
        : getTicketRecallFlow(draftId);
    const [title, setTitle] = useState(recallState?.titleCandidate || "");
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
        removeTicketRecallFlow(draftId);
        setError("");
        navigate(`/memories/${draftId}/save`, {
            state: {
                frontConfirmed: recallState.frontConfirmed,
                ticketRecall: {
                    writingMode: "AI_RECALL",
                    title: finalTitle,
                    answers: recallState.answers,
                },
            },
        });
    };

    const handleTitleChange = (value) => {
        setTitle(value);
        saveTicketRecallFlow(draftId, {
            ...recallState,
            titleCandidate: value,
        });
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
                <input value={title} onChange={(event) => handleTitleChange(event.target.value)} placeholder="제목을 입력해주세요" />
            </label>

            {error && <p className="form-error">{error}</p>}
            <button className="ticket-primary-button" onClick={handleConfirm}>제목 확정하기</button>
        </main>
    );
}
