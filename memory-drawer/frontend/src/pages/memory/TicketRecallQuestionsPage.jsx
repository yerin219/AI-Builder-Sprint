import { useMemo, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { generateTicketRecallTitle } from "../../api/ticketRecall";
import "./TicketRecall.css";

export default function TicketRecallQuestionsPage() {
    const { draftId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();
    const questionData = location.state?.questionData;
    const frontConfirmed = location.state?.frontConfirmed;
    const questions = useMemo(
        () => [...(questionData?.questions || [])].sort((first, second) => first.order - second.order),
        [questionData],
    );
    const [answers, setAnswers] = useState(() =>
        questions.map((question) => ({ questionId: question.questionId, answer: "" })),
    );
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    if (!frontConfirmed || !questionData || questions.length !== 3) {
        return (
            <main className="mobile-page ticket-recall-page">
                <h1>회상 질문 정보가 없습니다.</h1>
                <p>티켓 유형 선택부터 다시 진행해주세요.</p>
                <button className="ticket-primary-button" onClick={() => navigate(`/memories/${draftId}/back`)}>기록 방식 선택으로 이동</button>
            </main>
        );
    }

    const handleAnswerChange = (questionId, value) => {
        setAnswers((previous) => previous.map((answer) => (
            answer.questionId === questionId ? { ...answer, answer: value } : answer
        )));
    };

    const handleTitleGeneration = async (event) => {
        event.preventDefault();
        setError("");

        const normalizedAnswers = answers.map(({ questionId, answer }) => ({
            questionId,
            answer: answer.trim() || null,
        }));

        if (!normalizedAnswers.some(({ answer }) => answer)) {
            setError("세 질문 중 하나 이상에 답변해주세요.");
            return;
        }

        setLoading(true);

        try {
            const titleData = await generateTicketRecallTitle(draftId, normalizedAnswers);

            navigate(`/memories/${draftId}/ticket-recall/title`, {
                state: {
                    frontConfirmed,
                    ticketSubtype: questionData.ticketSubtype,
                    questions,
                    answers: normalizedAnswers,
                    titleCandidate: titleData.titleCandidate,
                },
            });
        } catch (err) {
            if (err.code === "AI_002") {
                navigate(`/memories/${draftId}/ticket-recall/title`, {
                    state: {
                        frontConfirmed,
                        questions,
                        answers: normalizedAnswers,
                        titleCandidate: "",
                        titleGenerationFailed: true,
                    },
                });
                return;
            }

            const messages = {
                TICKET_003: "답변을 확인해주세요. 질문 세 개 중 하나 이상에 답해야 해요.",
                DRAFT_001: "임시 기록을 찾을 수 없습니다.",
                TICKET_002: "티켓 앞면을 확정한 뒤 진행할 수 있어요.",
            };

            setError(messages[err.code] || err.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <main className="mobile-page ticket-recall-page">
            <button className="text-back-button" onClick={() => navigate(-1)}>← 돌아가기</button>
            <p className="ticket-recall-eyebrow">AI 회상 질문</p>
            <h1>그날의 기억을<br />천천히 떠올려보세요.</h1>
            <p className="ticket-recall-description">모든 질문이 표시돼요. 답하고 싶은 질문에만 작성해도 괜찮아요.</p>

            <form className="recall-question-form" onSubmit={handleTitleGeneration}>
                {questions.map((question, index) => (
                    <label key={question.questionId}>
                        <span>{index + 1}. {question.text}</span>
                        <textarea value={answers[index]?.answer || ""} onChange={(event) => handleAnswerChange(question.questionId, event.target.value)} rows="4" placeholder="기억나는 만큼 자유롭게 적어주세요." />
                    </label>
                ))}
                {error && <p className="form-error">{error}</p>}
                <button className="ticket-primary-button" disabled={loading}>
                    {loading ? "제목을 만드는 중..." : "제목 한 줄 만들기"}
                </button>
            </form>

            {/* TODO(API 6 백엔드 연동 확인): questions는 서버 질문 은행의 세 문항을 order 순서로 내려줘야 하며, 프론트는 질문 문구를 요청에 다시 보내지 않는다. */}
        </main>
    );
}
