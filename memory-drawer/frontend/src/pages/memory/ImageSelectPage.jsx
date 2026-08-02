import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { normalizeImage } from "../../utils/imageProcessor";

export default function ImageSelectPage() {
    const navigate = useNavigate();
    const [error, setError] = useState("");

    const handleSelect = async (event) => {
        const file = event.target.files?.[0];
        if (!file) return;

        try {
            setError("");

            // 방향 보정 및 큰 이미지 크기 축소
            const normalizedFile = await normalizeImage(file);

            navigate("/memories/preview", {
                state: { file: normalizedFile },
            });
        } catch {
            setError("이미지를 준비하지 못했습니다. 다른 사진을 선택해주세요.");
        }
    };

    return (
        <main className="mobile-page memory-select-page">
            <button className="page-back-button" onClick={() => navigate(-1)}>←</button>
            <h1>사진 추가</h1>

            <label className="select-box">
                <span>📷</span>
                <strong>사진 촬영 (모바일)</strong>
                <input
                    hidden
                    type="file"
                    accept="image/*"
                    capture="environment"
                    onChange={handleSelect}
                />
            </label>

            <p className="memory-select-page__camera-note">
                카메라 촬영은 모바일 환경에서 지원됩니다. PC에서는 저장된 사진을 선택해주세요.
            </p>

            <label className="select-box">
                <span>🖼️</span>
                <strong>파일에서 선택하기</strong>
                <input
                    hidden
                    type="file"
                    accept="image/*"
                    onChange={handleSelect}
                />
            </label>

            {error && <p className="form-error">{error}</p>}
        </main>
    );
}
