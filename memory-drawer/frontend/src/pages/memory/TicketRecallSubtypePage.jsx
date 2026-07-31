import { useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { confirmTicketSubtypeAndGetQuestions } from "../../api/ticketRecall";
import {
    getTicketRecallFlow,
    saveTicketRecallFlow,
} from "../../utils/ticketRecallStorage.js";
import "./TicketRecall.css";

const SUBTYPES = [
    { value: "CONCERT_PERFORMANCE", label: "콘서트·공연", description: "공연과 음악을 관람한 티켓" },
    { value: "MOVIE", label: "영화", description: "영화를 관람한 티켓" },
    { value: "EXHIBITION", label: "전시", description: "전시를 관람한 티켓" },
];

export default function TicketRecallSubtypePage() {
    const { draftId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();
    const savedFlow = getTicketRecallFlow(draftId);
    const subtypeSuggestion = location.state?.subtypeSuggestion || savedFlow?.subtypeSuggestion;
    const frontConfirmed = location.state?.frontConfirmed || savedFlow?.frontConfirmed;
    const [isManualSelection, setIsManualSelection] = useState(
        subtypeSuggestion?.requiresManualSelection ?? true,
    );
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    if (!subtypeSuggestion || !frontConfirmed) {
        return (
            <main className="mobile-page ticket-recall-page">
                <h1>티켓 유형 정보가 없습니다.</h1>
                <p>AI 질문 선택부터 다시 진행해주세요.</p>
                <button className="ticket-primary-button" onClick={() => navigate(`/memories/${draftId}/back`)}>기록 방식 선택으로 이동</button>
            </main>
        );
    }

    const suggestedSubtype = SUBTYPES.find(
        (subtype) => subtype.value === subtypeSuggestion.suggestedTicketSubtype,
    );

    const handleSubtypeConfirm = async (ticketSubtype) => {
        setLoading(true);
        setError("");

        try {
            const questionData = await confirmTicketSubtypeAndGetQuestions(draftId, ticketSubtype);

            saveTicketRecallFlow(draftId, {
                frontConfirmed,
                subtypeSuggestion,
                questionData,
            });

            navigate(`/memories/${draftId}/ticket-recall/questions`, {
                state: { frontConfirmed, questionData },
            });
        } catch (err) {
            const messages = {
                TICKET_001: "지원하지 않는 티켓 유형입니다.",
                DRAFT_001: "임시 기록을 찾을 수 없습니다.",
                TICKET_002: "티켓 앞면을 확정한 뒤 진행할 수 있어요.",
            };

            setError(messages[err.code] || err.message);
        } finally {
            setLoading(false);
        }
    };

    if (!isManualSelection && suggestedSubtype) {
        return (
            <main className="mobile-page ticket-recall-page">
                <button className="text-back-button" onClick={() => navigate(-1)}>← 돌아가기</button>
                <p className="ticket-recall-eyebrow">티켓 세부 유형</p>
                <h1>{suggestedSubtype.label}으로<br />인식했어요. 맞나요?</h1>
                <section className="subtype-suggestion-card">
                    <strong>{suggestedSubtype.label}</strong>
                    <span>{suggestedSubtype.description}</span>
                </section>
                {error && <p className="form-error">{error}</p>}
                <button className="ticket-primary-button" disabled={loading} onClick={() => handleSubtypeConfirm(suggestedSubtype.value)}>
                    {loading ? "질문을 불러오는 중..." : "맞아요"}
                </button>
                <button className="ticket-secondary-button" disabled={loading} onClick={() => setIsManualSelection(true)}>다른 유형 선택</button>
            </main>
        );
    }

    return (
        <main className="mobile-page ticket-recall-page">
            <button className="text-back-button" onClick={() => navigate(-1)}>← 돌아가기</button>
            <p className="ticket-recall-eyebrow">티켓 세부 유형</p>
            <h1>어떤 티켓인가요?</h1>
            <p className="ticket-recall-description">유형에 맞는 질문을 모두 보여드릴게요.</p>
            <section className="subtype-list" aria-label="티켓 세부 유형 선택">
                {SUBTYPES.map((subtype) => (
                    <button key={subtype.value} type="button" className="subtype-option" disabled={loading} onClick={() => handleSubtypeConfirm(subtype.value)}>
                        <strong>{subtype.label}</strong>
                        <span>{subtype.description}</span>
                    </button>
                ))}
            </section>
            {error && <p className="form-error">{error}</p>}
        </main>
    );
}
