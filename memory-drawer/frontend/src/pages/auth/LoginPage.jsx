import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { login } from "../../api/auth";
import { setAccessToken } from "../../utils/tokenStorage";

export default function LoginPage() {
    const navigate = useNavigate();
    const location = useLocation();

    const [form, setForm] = useState({
        email: location.state?.email || "",
        password: "",
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [message] = useState(location.state?.message || "");

    const handleChange = (event) => {
        const { name, value } = event.target;

        setForm((previous) => ({
            ...previous,
            [name]: value,
        }));
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError("");

        if (form.email.trim().length === 0) {
            setError("이메일을 입력해주세요.");
            return;
        }

        if (form.password.trim().length === 0) {
            setError("비밀번호를 입력해주세요.");
            return;
        }

        if (form.password.length > 10) {
            setError("비밀번호는 최대 10자까지 입력할 수 있습니다.");
            return;
        }

        try {
            setLoading(true);

            const data = await login(form);

            setAccessToken(data.accessToken);

            navigate(location.state?.from?.pathname || "/home", { replace: true });
        } catch (err) {
            const messages = {
                AUTH_002: "이메일 또는 비밀번호가 일치하지 않습니다.",
                VALIDATION_001: "이메일 형식과 비밀번호를 확인해주세요.",
                NETWORK_ERROR: "백엔드 서버가 실행 중인지 확인해주세요.",
            };

            setError(messages[err.code] || err.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <main className="mobile-page auth-page">
            <section className="auth-card">
                <div className="auth-intro">
                    <div className="drawer-logo" aria-hidden="true">
                        <div className="drawer-logo-top" />
                        <div className="drawer-logo-body">
                            <span />
                        </div>
                    </div>

                    <h1>기억서랍</h1>
                    <p>기억서랍에서 추억을 보관하세요.</p>
                </div>

                <form className="auth-form" onSubmit={handleSubmit}>
                    <label className="auth-field">
                        <span className="field-icon" aria-hidden="true">✉</span>
                        <input
                            type="email"
                            name="email"
                            placeholder="이메일"
                            value={form.email}
                            onChange={handleChange}
                            autoComplete="email"
                            maxLength={320}
                            required
                        />
                    </label>

                    <label className="auth-field">
                        <span className="field-icon" aria-hidden="true">♙</span>
                        <input
                            type="password"
                            name="password"
                            placeholder="비밀번호"
                            value={form.password}
                            onChange={handleChange}
                            maxLength={10}
                            required
                        />
                    </label>

                    {message && <p className="form-success">{message}</p>}
                    {error && <p className="form-error">{error}</p>}

                    <button className="auth-submit" type="submit" disabled={loading}>
                        {loading ? "로그인 중..." : "로그인"}
                    </button>
                </form>

                <div className="auth-divider">
                    <span>또는</span>
                </div>

                <p className="auth-link">
                    계정이 없으신가요? <Link to="/signup">회원가입</Link>
                </p>
            </section>
        </main>
    );
}
