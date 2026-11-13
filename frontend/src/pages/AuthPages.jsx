import { useState } from 'react'
import { ArrowRight, KeyRound, ShieldCheck, Train, UserRound } from 'lucide-react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import useAuth from '../auth/useAuth'
import { Alert, Button, Card, TextField } from '../components/Common'
import api, { errorMessage, responseData } from '../lib/api'

function AuthFrame({ title, subtitle, children }) {
  return (
    <div className="mx-auto grid min-h-[calc(100vh-150px)] max-w-7xl items-center gap-8 px-4 py-10 sm:px-6 lg:grid-cols-[1fr_0.9fr] lg:px-8">
      <div className="hidden overflow-hidden rounded-[2rem] bg-ink p-10 text-white shadow-card lg:block">
        <div className="mb-16 grid h-14 w-14 place-items-center rounded-2xl bg-accent text-ink"><Train size={30} /></div>
        <p className="mb-3 text-xs font-bold uppercase tracking-[0.2em] text-accent">Every journey begins somewhere</p>
        <h2 className="max-w-lg font-display text-4xl font-bold leading-tight">A little less planning. A lot more going.</h2>
        <p className="mt-4 max-w-md text-sm leading-7 text-white/65">Keep your trip details close, find the right train, and get on your way with Trackline.</p>
        <div className="mt-10 flex items-center gap-3 text-sm text-white/75"><ShieldCheck size={18} className="text-accent" /> Secure sign-in for your reservation account</div>
      </div>
      <Card className="mx-auto w-full max-w-xl p-6 sm:p-9">
        <div className="mb-7">
          <div className="mb-5 grid h-11 w-11 place-items-center rounded-xl bg-brand-50 text-brand-700 lg:hidden"><Train size={24} /></div>
          <h1 className="font-display text-3xl font-bold tracking-tight text-ink">{title}</h1>
          <p className="mt-2 text-sm leading-6 text-muted">{subtitle}</p>
        </div>
        {children}
      </Card>
    </div>
  )
}

export function LoginPage() {
  const { login } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: location.state?.email || '', password: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      const user = await login(form)
      const returnTo = location.state?.from
      const requestedPath = returnTo
        ? `${returnTo.pathname}${returnTo.search || ''}${returnTo.hash || ''}`
        : null
      navigate(requestedPath || (user.role === 'ADMIN' ? '/admin' : '/dashboard'), { replace: true })
    } catch (requestError) {
      setError(errorMessage(requestError))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthFrame title="Welcome back" subtitle="Sign in to pick up where your next journey starts.">
      <form className="space-y-5" onSubmit={submit}>
        {error && <Alert>{error}</Alert>}
        <TextField
          label="Email address"
          name="email"
          type="email"
          autoComplete="email"
          required
          maxLength={254}
          value={form.email}
          onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
          placeholder="you@example.com"
        />
        <TextField
          label="Password"
          name="password"
          type="password"
          autoComplete="current-password"
          required
          maxLength={72}
          value={form.password}
          onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
          placeholder="Enter your password"
        />
        <Button type="submit" className="w-full" disabled={busy}>
          <KeyRound size={17} /> {busy ? 'Signing in…' : 'Sign in'}
        </Button>
      </form>
      <p className="mt-6 text-center text-sm text-muted">
        New to Trackline? <Link to="/register" className="font-bold text-brand-700 hover:text-brand-900">Create an account</Link>
      </p>
    </AuthFrame>
  )
}

export function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ fullName: '', email: '', phoneNumber: '', password: '' })
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [busy, setBusy] = useState(false)

  function update(field, value) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  async function submit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')
    setBusy(true)
    try {
      const user = responseData(await api.post('/api/auth/register', form))
      setSuccess(`Account created for ${user.email}. Sign in to continue.`)
      window.setTimeout(() => navigate('/login', { replace: true, state: { email: user.email } }), 900)
    } catch (requestError) {
      setError(errorMessage(requestError))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthFrame title="Create your account" subtitle="One account for all your journeys and reservation details.">
      <form className="space-y-4" onSubmit={submit}>
        {error && <Alert>{error}</Alert>}
        {success && <Alert tone="success">{success}</Alert>}
        <TextField label="Full name" name="fullName" autoComplete="name" required maxLength={120} value={form.fullName} onChange={(event) => update('fullName', event.target.value)} placeholder="Your full name" />
        <TextField label="Email address" name="email" type="email" autoComplete="email" required maxLength={254} value={form.email} onChange={(event) => update('email', event.target.value)} placeholder="you@example.com" />
        <TextField
          label="Phone number"
          name="phoneNumber"
          type="tel"
          autoComplete="tel"
          required
          minLength={7}
          maxLength={20}
          pattern="[0-9+() -]{7,20}"
          value={form.phoneNumber}
          onChange={(event) => update('phoneNumber', event.target.value)}
          placeholder="+91 98765 43210"
          hint="Use 7–20 digits, spaces, or + ( ) - characters."
        />
        <TextField
          label="Password"
          name="password"
          type="password"
          autoComplete="new-password"
          required
          minLength={8}
          maxLength={72}
          value={form.password}
          onChange={(event) => update('password', event.target.value)}
          placeholder="At least 8 characters"
          hint="8–72 characters."
        />
        <Button type="submit" className="mt-2 w-full" disabled={busy}>
          <UserRound size={17} /> {busy ? 'Creating account…' : 'Create account'} <ArrowRight size={16} />
        </Button>
      </form>
      <p className="mt-6 text-center text-sm text-muted">
        Already have an account? <Link to="/login" className="font-bold text-brand-700 hover:text-brand-900">Sign in</Link>
      </p>
    </AuthFrame>
  )
}
