import { useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { suggestTicketSubtype } from "../../api/ticketRecall";
import { getFrontConfirmed } from "../../utils/draftStorage";
import "./TicketRecall.css";

export default function TicketBackModePage() {
    const { draftId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();
    const frontConfirmed =
        location.state?.frontConfirmed || getFrontConfirmed(draftId);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    if (!frontConfirmed) {
        return <FlowResetPage navigate={navigate} />;
    }

    if (frontConfirmed.documentType !== "TICKET") {
        return (
            <main className="mobile-page ticket-recall-page">
                <button className="text-back-button" onClick={() => navigate(-1)}>← 돌아가기</button>
                <h1>카드 뒷면 작성</h1>
                <p>AI 회상 질문은 티켓에서만 사용할 수 있어요.</p>
                <p className="ticket-recall-note">
                    영수증과 손편지의 뒷면 작성은 API 7 카드 저장 화면에서 이어집니다.
                </p>
            </main>
        );
    }

    const handleRecallStart = async () => {
        setLoading(true);
        setError("");

        try {
            const subtypeSuggestion = await suggestTicketSubtype(draftId);

            navigate(`/memories/${draftId}/ticket-recall/subtype`, {
                state: { frontConfirmed, subtypeSuggestion },
            });
        } catch (err) {
            const messages = {
                DRAFT_001: "임시 기록을 찾을 수 없습니다. 사진 선택부터 다시 진행해주세요.",
                TICKET_002: "티켓 앞면을 확정한 뒤 AI 질문을 사용할 수 있어요.",
                AI_002: "티켓 유형을 분석하지 못했습니다. 잠시 후 다시 시도해주세요.",
            };

            setError(messages[err.code] || err.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <main className="mobile-page ticket-recall-page">
            <button className="text-back-button" onClick={() => navigate(-1)}>← 돌아가기</button>
            <p className="ticket-recall-eyebrow">티켓 뒷면 작성</p>
            <h1>어떤 방식으로<br />기억을 남길까요?</h1>
            <p className="ticket-recall-description">
                직접 기록하거나, 질문을 따라 그날의 기억을 떠올릴 수 있어요.
            </p>

            <section className="ticket-mode-list" aria-label="티켓 기록 방식">
                <button type="button" className="ticket-mode-card" onClick={() => navigate(`/memories/${draftId}/save`, { state: { frontConfirmed } })}>
                    <span aria-hidden="true">✎</span>
                    <strong>직접 기록하기</strong>
                    <small>제목과 추억을 자유롭게 작성해요.</small>
                </button>
                <button type="button" className="ticket-mode-card ticket-mode-card--accent" disabled={loading} onClick={handleRecallStart}>
                    <span aria-hidden="true">✦</span>
                    <strong>{loading ? "질문을 준비하는 중..." : "AI 질문으로 떠올리기"}</strong>
                    <small>질문을 따라 차분히 기억을 정리해요.</small>
                </button>
            </section>

            {error && <p className="form-error">{error}</p>}

            {/* TODO(API 5 복구 흐름): 현재 앞면 확정 응답은 location.state로 전달된다. 새로고침 복구용 임시기록 조회 API가 정해지면 이 화면도 복구 처리한다. */}
        </main>
    );
}

function FlowResetPage({ navigate }) {
    return (
        <main className="mobile-page ticket-recall-page">
            <h1>앞면 확정 정보가 없습니다.</h1>
            <p>사진 분석부터 다시 진행해주세요.</p>
            <button className="ticket-primary-button" onClick={() => navigate("/memories/new")}>사진 선택으로 이동</button>
        </main>
    );
}
