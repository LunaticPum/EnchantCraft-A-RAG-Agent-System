import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { ArrowRight, UserPlus, User, KeyRound, Mail } from "lucide-react";
import { api } from "../lib/api";

export default function RegisterPage() {
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [email, setEmail] = useState("");
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");
  const [loading, setLoading] = useState(false);

  const handleRegister = async () => {
    setError(""); setOk("");
    if (!username || username.length < 3) { setError("用户名至少 3 位"); return; }
    if (!password || password.length < 4) { setError("密码至少 4 位"); return; }
    setLoading(true);
    try {
      await api.register(username, password, email);
      setOk("注册成功，即将跳转登录...");
      setTimeout(() => navigate("/login", { replace: true }), 1500);
    } catch (e: any) { setError(e.message || "注册失败"); }
    finally { setLoading(false); }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-6">
      <div className="w-full max-w-[420px] glass p-10" style={{ animation: "fade-in 300ms ease-out" }}>
        <div className="flex flex-col items-center gap-3 mb-8">
          <div className="w-14 h-14 rounded-2xl bg-[var(--color-accent)] flex items-center justify-center">
            <UserPlus size={26} className="text-[var(--color-bg-root)]" />
          </div>
          <h2 className="serif text-2xl font-semibold text-[var(--color-ink)]">注册</h2>
          <p className="text-xs text-[var(--color-ink-faint)]">创建普通用户账户（每日10次检索+10轮对话）</p>
        </div>
        <div className="space-y-3">
          <div className="relative">
            <User size={15} className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--color-ink-faint)] pointer-events-none" />
            <input type="text" value={username} onChange={(e) => setUsername(e.target.value)}
              placeholder="用户名" className="input input-has-icon" />
          </div>
          <div className="relative">
            <KeyRound size={15} className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--color-ink-faint)] pointer-events-none" />
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
              placeholder="密码" className="input input-has-icon" />
          </div>
          <div className="relative">
            <Mail size={15} className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--color-ink-faint)] pointer-events-none" />
            <input type="text" value={email} onChange={(e) => setEmail(e.target.value)}
              placeholder="邮箱（选填）" className="input input-has-icon" />
          </div>
          {error && <p className="text-xs text-[var(--color-danger)] bg-[rgba(200,122,106,0.1)] px-3 py-2 rounded-xl">{error}</p>}
          {ok && <p className="text-xs text-[var(--color-pass)] bg-[rgba(52,211,153,0.1)] px-3 py-2 rounded-xl">{ok}</p>}
          <button onClick={handleRegister} disabled={loading}
            className="pill pill--dark w-full justify-center py-3 text-sm shadow-lg mt-4">
            {loading ? "注册中..." : <span className="flex items-center gap-2">注册 <ArrowRight size={15} /></span>}
          </button>
          <p className="text-center text-[11px] text-[var(--color-ink-faint)] pt-2">
            已有账号？<Link to="/login" className="text-[var(--color-accent)] hover:underline">去登录</Link>
          </p>
        </div>
      </div>
    </div>
  );
}