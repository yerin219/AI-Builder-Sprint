import { useNavigate } from "react-router-dom";

const drawers = [
    { year: "2025", count: 12 },
    { year: "2024", count: 8 },
    { year: "2023", count: 15 },
    { year: "2022", count: 6 },
];

export default function HomePage() {
    const navigate = useNavigate();

    return (
        <main className="mobile-page home-page">
            <header className="home-header">
                <button className="icon-button" aria-label="메뉴">
                    ☰
                </button>

                <div className="home-title">
                    <h1>기억서랍</h1>
                    <p>추억을 담아, 오래도록 간직하세요.</p>
                </div>

                <button className="icon-button" aria-label="알림">
                    ●
                </button>
            </header>

            <button
                className="add-memory-button"
                onClick={() => navigate("/memories/new")}
            >
                <span>＋</span> 기억 담기
            </button>

            <section className="drawer-stack" aria-label="연도별 서랍">
                {drawers.map((drawer, index) => (
                    <button className="drawer" key={drawer.year}>
                        {index === 0 && (
                            <div className="drawer-papers" aria-hidden="true">
                                <span />
                                <span />
                                <span />
                            </div>
                        )}

                        <div className="drawer-label">{drawer.year}</div>
                        <div className="drawer-handle" />
                        <p>▣ {drawer.count}장</p>
                    </button>
                ))}
            </section>

            <nav className="bottom-nav">
                <button className="active">▰<span>서랍</span></button>
                <button>▤<span>카드</span></button>
                <button>●<span>마이페이지</span></button>
            </nav>
        </main>
    );
}