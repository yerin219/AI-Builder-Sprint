import { useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { confirmFront } from "../../api/drafts";
import { saveFrontConfirmed } from "../../utils/draftStorage";
import {
    buildFrontPayload,
    createFrontForm,
    getNextPathAfterFrontConfirmation,
    validateFrontForm,
} from "../../utils/frontConfirmation";

export default function FrontConfirmPage() {
    const { draftId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();

    // DocumentTypePage에서 API 4 호출 후 넘긴 데이터
    const extraction = location.state?.extraction;
    const documentType = extraction?.documentType;
    const candidate = extraction?.frontCandidate;

    const [form, setForm] = useState(() => createFrontForm(candidate));

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

    const handlePurchaseItemChange = (index, field, value) => {
        setForm((previous) => ({
            ...previous,
            purchaseItems: previous.purchaseItems.map((item, itemIndex) =>
                itemIndex === index ? { ...item, [field]: value } : item,
            ),
        }));
    };

    const addPurchaseItem = () => {
        setForm((previous) => ({
            ...previous,
            purchaseItems: [
                ...previous.purchaseItems,
                { name: "", quantity: 1, selected: true },
            ],
        }));
    };

    const removePurchaseItem = (index) => {
        setForm((previous) => ({
            ...previous,
            purchaseItems: previous.purchaseItems.filter(
                (_, itemIndex) => itemIndex !== index,
            ),
        }));
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError("");

        const validationError = validateFrontForm(documentType, form);

        if (validationError) {
            setError(validationError);
            return;
        }

        try {
            setLoading(true);

            // API 5: 수정한 앞면 정보 최종 확정
            const frontConfirmed = await confirmFront(
                draftId,
                buildFrontPayload(documentType, form),
            );

            // 새로고침해도 API 6에서 앞면 확정 결과를 사용할 수 있게 저장
            saveFrontConfirmed(draftId, frontConfirmed);

            // 티켓은 뒷면 작성 방식을 선택하고, 나머지는 카드 저장 화면으로 이동
            navigate(getNextPathAfterFrontConfirmation(documentType, draftId), {
                state: { frontConfirmed },
            });

        } catch (err) {
            const messages = {
                VALIDATION_001: "필수 정보를 모두 입력하고 날짜를 확인해주세요.",
                DRAFT_001: "임시 기록을 찾을 수 없습니다.",
                DRAFT_002: "문서 유형을 먼저 확정해야 합니다.",
                DRAFT_004: "다른 사용자의 임시 기록에는 접근할 수 없습니다.",
            };

            setError(messages[err.code] || err.message);
        } finally {
            setLoading(false);
        }
    };

    const selectedPurchaseItemCount = form.purchaseItems.filter(
        (item) => item.selected !== false,
    ).length;

    return (
        <main className="mobile-page">
            <button className="page-back-button" onClick={() => navigate(-1)}>←</button>
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
                    <>
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

                        <section className="purchase-items-editor" aria-labelledby="purchase-items-title">
                            <div className="purchase-items-editor__header">
                                <div>
                                    <h2 id="purchase-items-title">구매 항목</h2>
                                    <p>카드 앞면에 남길 메뉴나 상품만 선택해주세요.</p>
                                </div>
                                <button type="button" onClick={addPurchaseItem}>항목 추가</button>
                            </div>

                            {form.purchaseItems.length > 0 ? (
                                <ul className="purchase-item-list">
                                    {form.purchaseItems.map((item, index) => (
                                        <li
                                            className={item.selected === false ? "is-unselected" : ""}
                                            key={index}
                                        >
                                            <label className="purchase-item__selection">
                                                <input
                                                    type="checkbox"
                                                    checked={item.selected !== false}
                                                    onChange={(event) => handlePurchaseItemChange(
                                                        index,
                                                        "selected",
                                                        event.target.checked,
                                                    )}
                                                />
                                                <span>카드에 저장</span>
                                            </label>

                                            <div className="purchase-item__fields">
                                                <label>
                                                    <span>항목명</span>
                                                    <input
                                                        value={item.name}
                                                        placeholder="예: 아이스 아메리카노"
                                                        onChange={(event) => handlePurchaseItemChange(
                                                            index,
                                                            "name",
                                                            event.target.value,
                                                        )}
                                                        disabled={item.selected === false}
                                                        required={item.selected !== false}
                                                    />
                                                </label>
                                                <label>
                                                    <span>수량</span>
                                                    <input
                                                        type="number"
                                                        min="1"
                                                        step="1"
                                                        inputMode="numeric"
                                                        value={item.quantity}
                                                        onChange={(event) => handlePurchaseItemChange(
                                                            index,
                                                            "quantity",
                                                            event.target.value,
                                                        )}
                                                        disabled={item.selected === false}
                                                        required={item.selected !== false}
                                                    />
                                                </label>
                                            </div>

                                            <button
                                                className="purchase-item__remove"
                                                type="button"
                                                onClick={() => removePurchaseItem(index)}
                                            >
                                                삭제
                                            </button>
                                        </li>
                                    ))}
                                </ul>
                            ) : (
                                <p className="purchase-items-editor__empty">
                                    인식된 구매 항목이 없어요. 필요한 항목을 직접 추가할 수 있어요.
                                </p>
                            )}

                            {selectedPurchaseItemCount === 0 && (
                                <p className="purchase-items-editor__notice" role="status">
                                    선택한 항목이 없어 구매 항목 없이 저장돼요.
                                </p>
                            )}
                        </section>
                    </>
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
                    <>
                        <section className="form-section">
                            <h2>누가 누구에게 쓴 편지인가요?</h2>
                            <p>AI 인식 결과가 비어 있거나 다르면 직접 수정해주세요.</p>
                            <label>
                                쓴 사람
                                <input
                                    name="sender"
                                    placeholder="예: 엄마"
                                    value={form.sender}
                                    onChange={handleChange}
                                />
                            </label>
                            <label>
                                받는 사람
                                <input
                                    name="recipient"
                                    placeholder="예: 지은에게"
                                    value={form.recipient}
                                    onChange={handleChange}
                                />
                            </label>
                        </section>
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
                    </>
                )}

                {error && <p className="form-error">{error}</p>}

                <button className="choice-action-button" disabled={loading}>
                    {loading ? "저장 중..." : "맞아요"}
                </button>
            </form>
        </main>
    );
}
