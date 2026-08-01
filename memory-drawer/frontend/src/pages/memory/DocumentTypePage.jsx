import { useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { confirmDocumentType } from "../../api/drafts";

const DOCUMENT_TYPES = [
    { value: "RECEIPT", label: "영수증", icon: "🧾" },
    { value: "TICKET", label: "티켓", icon: "🎫" },
    { value: "LETTER", label: "손편지", icon: "✉️" },
];

export default function DocumentTypePage() {
    const { draftId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();

    // ImagePreviewPage에서 넘긴 분석 결과
    const analysis = location.state?.analysis;

    const [showManualSelection, setShowManualSelection] = useState(
        analysis?.requiresManualSelection ?? true,
    );
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    // 이 페이지를 새로고침하면 analysis 정보가 사라질 수 있음
    if (!analysis) {
        return (
            <main className="mobile-page">
                <h1>분석 결과가 없습니다.</h1>
                <p>사진 선택부터 다시 진행해주세요.</p>
                <button onClick={() => navigate("/memories/new")}>
                    사진 선택으로 이동
                </button>
            </main>
        );
    }

    const handleConfirm = async (documentType) => {
        setLoading(true);
        setError("");

        try {
            // API 4: 최종 문서 유형 확정
            const extraction = await confirmDocumentType(draftId, documentType);

            // 다음 작업(API 5: 추출값 수정 화면)으로 데이터 전달
            navigate(`/memories/${draftId}/front`, {
                state: { extraction },
            });
        } catch (err) {
            const messages = {
                DOCUMENT_002: "지원하지 않는 문서 유형입니다.",
                DOCUMENT_001: "문서 내용을 읽을 수 없습니다. 다른 사진으로 다시 시도해주세요.",
                DRAFT_001: "임시 기록을 찾을 수 없습니다. 사진부터 다시 선택해주세요.",
                DRAFT_002: "현재 단계에서는 문서 유형을 확정할 수 없습니다.",
                DRAFT_004: "다른 사용자의 임시 기록에는 접근할 수 없습니다.",
                AI_001: "정보 추출 서비스 오류입니다. 다시 시도해주세요.",
            };

            setError(messages[err.code] || err.message);
        } finally {
            setLoading(false);
        }
    };

    const suggestedType = DOCUMENT_TYPES.find(
        (type) => type.value === analysis.suggestedDocumentType,
    );

    // AI가 유형을 제안했고, 사용자가 아직 “아니에요”를 누르지 않은 경우
    if (!showManualSelection && suggestedType) {
        return (
            <main className="mobile-page document-type-result-page">
                <button className="page-back-button" onClick={() => navigate(-1)}>←</button>
                <h1>문서 인식 결과</h1>

                <section className="document-type-result-page__content">
                    <p className="document-type-result-page__question">{suggestedType.label}으로 인식했어요.<br />맞나요?</p>
                    <section className="document-type-card">
                        <span className="document-type-icon">{suggestedType.icon}</span>
                        <strong>{suggestedType.label}</strong>
                    </section>
                </section>

                {error && <p className="form-error">{error}</p>}

                <div className="document-type-result-page__actions">
                    <button
                        className="choice-action-button"
                        disabled={loading}
                        onClick={() => handleConfirm(suggestedType.value)}
                    >
                        {loading ? "확인 중..." : "맞아요"}
                    </button>

                    <button
                        className="choice-action-button"
                        disabled={loading}
                        onClick={() => setShowManualSelection(true)}
                    >
                        아니에요
                    </button>
                </div>
            </main>
        );
    }

    // AI가 판단하지 못했거나 “아니에요”를 누른 경우
    return (
        <main className="mobile-page">
            <button className="page-back-button" onClick={() => navigate(-1)}>←</button>
            <h1>문서 유형 선택</h1>
            <p>문서 유형을 선택해주세요.</p>

            <section className="document-type-list">
                {DOCUMENT_TYPES.map((type) => (
                    <button
                        key={type.value}
                        className="document-type-card"
                        disabled={loading}
                        onClick={() => handleConfirm(type.value)}
                    >
                        <span className="document-type-icon">{type.icon}</span>
                        <strong>{type.label}</strong>
                    </button>
                ))}
            </section>

            {error && <p className="form-error">{error}</p>}
        </main>
    );
}
