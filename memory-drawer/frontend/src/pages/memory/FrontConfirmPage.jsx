import { useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { confirmFront } from "../../api/drafts";

export default function FrontConfirmPage() {
    const { draftId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();

    // DocumentTypePage에서 API 4 호출 후 넘긴 데이터
    const extraction = location.state?.extraction;
    const documentType = extraction?.documentType;
    const candidate = extraction?.frontCandidate;

    const [form, setForm] = useState({
        memoryDate: candidate?.memoryDate ?? "",
        storeName: candidate?.storeName ?? "",
        eventName: candidate?.eventName ?? "",
        venue: candidate?.venue ?? "",
        seat: candidate?.seat ?? "",
        ocrText: candidate?.ocrText ?? "",
    });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    if (!extraction) {
        return (
            <main className="mobile-page">
                <h1>추출 결과가 없습니다.</h1>
                <p>사진 분석부터 다시 진행해주세요.</p>
                <button onClick={() => navigate("/memories/new")}>
                    사진 선택으로 이동
                </button>
            </main>
        );
    }

    const handleChange = (event) => {
        const { name, value } = event.target;

        setForm((previous) => ({
            ...previous,
            [name]: value,
        }));
    };

    const makePayload = () => {
        // 영수증
        if (documentType === "RECEIPT") {
            return {
                memoryDate: form.memoryDate,
                front: {
                    storeName: form.storeName,
                },
            };
        }

        // 티켓
        if (documentType === "TICKET") {
            return {
                memoryDate: form.memoryDate,
                front: {
                    eventName: form.eventName,
                    venue: form.venue,
                    // 좌석은 선택값: 빈 문자열이 아니라 null 전송
                    seat: form.seat.trim() || null,
                },
            };
        }

        // 손편지
        return {
            memoryDate: form.memoryDate,
            front: {
                ocrText: form.ocrText,
            },
        };
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError("");

        try {
            setLoading(true);

            // API 5: 수정한 앞면 정보 최종 확정
            const frontConfirmed = await confirmFront(draftId, makePayload());

            // 다음 작업: 카드 뒷면 작성 화면
            navigate(`/memories/${draftId}/back`, {
                state: { frontConfirmed },
            });
        } catch (err) {
            const messages = {
                VALIDATION_001: "필수 정보를 모두 입력하고 날짜를 확인해주세요.",
                DRAFT_001: "임시 기록을 찾을 수 없습니다.",
                DRAFT_002: "문서 유형을 먼저 확정해야 합니다.",
            };

            setError(messages[err.code] || err.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <main className="mobile-page">
            <button onClick={() => navigate(-1)}>←</button>
            <h1>정보 확인 및 수정</h1>

            <form onSubmit={handleSubmit}>
                <label>
                    날짜
                    <input
                        type="date"
                        name="memoryDate"
                        value={form.memoryDate}
                        onChange={handleChange}
                        required
                    />
                </label>

                {documentType === "RECEIPT" && (
                    <label>
                        상호명
                        <input
                            name="storeName"
                            placeholder="상호명을 입력해주세요"
                            value={form.storeName}
                            onChange={handleChange}
                            required
                        />
                    </label>
                )}

                {documentType === "TICKET" && (
                    <>
                        <label>
                            행사명
                            <input
                                name="eventName"
                                placeholder="행사명을 입력해주세요"
                                value={form.eventName}
                                onChange={handleChange}
                                required
                            />
                        </label>

                        <label>
                            장소
                            <input
                                name="venue"
                                placeholder="장소를 입력해주세요"
                                value={form.venue}
                                onChange={handleChange}
                                required
                            />
                        </label>

                        <label>
                            좌석 <small>(선택)</small>
                            <input
                                name="seat"
                                placeholder="좌석을 입력해주세요"
                                value={form.seat}
                                onChange={handleChange}
                            />
                        </label>
                    </>
                )}

                {documentType === "LETTER" && (
                    <label>
                        손편지 내용
                        <textarea
                            name="ocrText"
                            placeholder="인식된 내용을 확인하고 수정해주세요"
                            value={form.ocrText}
                            onChange={handleChange}
                            rows="8"
                            required
                        />
                    </label>
                )}

                {error && <p className="form-error">{error}</p>}

                <button disabled={loading}>
                    {loading ? "저장 중..." : "맞아요"}
                </button>
            </form>
        </main>
    );
}