import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { analyzeImage } from "../../api/drafts";
import { saveDraftId } from "../../utils/draftStorage";

export default function ImagePreviewPage() {
    const location = useLocation();
    const navigate = useNavigate();
    const file = location.state?.file;

    const [previewUrl, setPreviewUrl] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        if (!file) {
            navigate("/memories/new", { replace: true });
            return;
        }

        const url = URL.createObjectURL(file);
        setPreviewUrl(url);

        return () => URL.revokeObjectURL(url);
    }, [file, navigate]);

    const handleAnalyze = async () => {
        setLoading(true);
        setError("");

        try {
            const analysis = await analyzeImage(file);

            // 이후 API 4, 5, 9에서도 사용할 draftId 저장
            saveDraftId(analysis.draftId);

            navigate(`/memories/${analysis.draftId}/type`, {
                state: { analysis },
            });
        } catch (err) {
            const messages = {
                IMAGE_001: "이미지를 선택해주세요.",
                IMAGE_002: "이미지 용량이 너무 큽니다.",
                IMAGE_003: "지원하지 않는 이미지 형식입니다.",
                DOCUMENT_001: "문서 내용을 분석할 수 없습니다. 더 선명한 사진을 선택해주세요.",
                AI_001: "문서 분석 서비스 오류입니다. 다시 시도해주세요.",
                AI_002: "문서 유형 판단 서비스 오류입니다. 다시 시도해주세요.",
            };

            setError(messages[err.code] || err.message);
        } finally {
            setLoading(false);
        }
    };

    if (!file) return null;

    if (loading) {
        return (
            <main className="mobile-page loading-page">
                <p className="loading-page__message">잠시만 기다려주세요:)</p>
            </main>
        );
    }

    return (
        <main className="mobile-page">
            <button className="page-back-button" onClick={() => navigate(-1)}>←</button>
            <h1>사진 확인</h1>

            <img className="document-preview" src={previewUrl} alt="선택한 문서" />

            {error && <p className="form-error">{error}</p>}

            <button onClick={() => navigate("/memories/new")}>
                다시 선택
            </button>

            <button onClick={handleAnalyze}>
                이 사진으로 분석하기
            </button>
        </main>
    );
}
