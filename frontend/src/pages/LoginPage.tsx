import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowRight, LogIn, User, KeyRound } from "lucide-react";
import { useAuth } from "../lib/mockAuth";
import { api, setAuthToken } from "../lib/api";

export default function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [account, setAccount] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleLogin = async () => {
    setError("");
    if (!account || account.length < 3) { setError("用户名至少 3 位"); return; }
    if (!password || password.length < 4) { setError("密码至少 4 位"); return; }
    setLoading(true);
    try {
      const res = await api.login(account, password);
      setAuthToken(res.token);
      login({ userId: res.userId, username: res.username, role: res.role });
      navigate("/documents", { replace: true });
    } catch (e: any) {
      setError(e.message || "登录失败");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-6">
      <div className="w-full max-w-[420px] glass p-10" style={{ animation: "fade-in 300ms ease-out" }}>
        <div className="flex flex-col items-center gap-3 mb-8">
          <div className="w-14 h-14 rounded-2xl bg-[var(--color-accent)] flex items-center justify-center">
            <LogIn size={26} className="text-[var(--color-bg-root)]" />
          </div>
          <h2 className="serif text-2xl font-semibold text-[var(--color-ink)]">登录</h2>
          <p className="text-xs text-[var(--color-ink-faint)]">管理员 Pumluda / 普通用户随意注册</p>
        </div>

        <div className="space-y-3">
          <div className="relative">
            <User size={15} className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--color-ink-faint)] pointer-events-none" />
            <input type="text" value={account} onChange={(e) => setAccount(e.target.value)}
              placeholder="用户名" className="input input-has-icon"
              onKeyDown={(e) => e.key === "Enter" && handleLogin()} />
          </div>
          <div className="relative">
            <KeyRound size={15} className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--color-ink-faint)] pointer-events-none" />
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
              placeholder="密码" className="input input-has-icon"
              onKeyDown={(e) => e.key === "Enter" && handleLogin()} />
          </div>
          {error && (
            <p className="text-xs text-[var(--color-danger)] bg-[rgba(200,122,106,0.1)] px-3 py-2 rounded-xl">{error}</p>
          )}
          <button onClick={handleLogin} disabled={loading}
            className="pill pill--dark w-full justify-center py-3 text-sm shadow-lg mt-4">
            {loading ? "登录中..." : <span className="flex items-center gap-2">登录 <ArrowRight size={15} /></span>}
          </button>
        </div>
      </div>
    </div>
  );
}