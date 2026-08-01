import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { signup } from "../../api/auth";

export default function SignupPage() {
    const navigate = useNavigate();

    const [form, setForm] = useState({
        email: "",
        password: "",
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

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

            await signup(form);

            navigate("/login", {
                replace: true,
                state: {
                    email: form.email.trim(),
                    message: "회원가입이 완료되었습니다. 로그인해주세요.",
                },
            });
        } catch (err) {
            const messages = {
                AUTH_003: "이미 사용 중인 이메일입니다.",
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

                    {error && <p className="form-error">{error}</p>}

                    <button className="auth-submit" type="submit" disabled={loading}>
                        {loading ? "가입 중..." : "회원가입"}
                    </button>
                </form>

                <div className="auth-divider">
                    <span>또는</span>
                </div>

                <p className="auth-link">
                    이미 계정이 있나요? <Link to="/login">로그인</Link>
                </p>
            </section>
        </main>
    );
}
