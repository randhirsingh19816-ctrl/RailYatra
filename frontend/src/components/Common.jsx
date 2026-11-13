import { AlertCircle, ArrowLeft, ChevronLeft, ChevronRight, LoaderCircle } from 'lucide-react'

export function PageHeading({ eyebrow, title, description, action }) {
  return (
    <div className="mb-7 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        {eyebrow && <p className="mb-2 text-xs font-bold uppercase tracking-[0.18em] text-brand-600">{eyebrow}</p>}
        <h1 className="font-display text-3xl font-bold tracking-tight text-ink sm:text-4xl">{title}</h1>
        {description && <p className="mt-2 max-w-2xl text-sm leading-6 text-muted sm:text-base">{description}</p>}
      </div>
      {action}
    </div>
  )
}

export function Card({ children, className = '' }) {
  return <section className={`rounded-2xl border border-slate-200 bg-white shadow-card ${className}`}>{children}</section>
}

export function Button({ children, className = '', variant = 'primary', type = 'button', disabled = false, ...props }) {
  const styles = {
    primary: 'bg-brand-700 text-white hover:bg-brand-900 focus-visible:ring-brand-500',
    secondary: 'border border-slate-300 bg-white text-ink hover:border-brand-500 hover:bg-brand-50 focus-visible:ring-brand-500',
    danger: 'bg-rose-600 text-white hover:bg-rose-700 focus-visible:ring-rose-500',
    quiet: 'text-brand-700 hover:bg-brand-50 focus-visible:ring-brand-500',
  }
  return (
    <button
      type={type}
      disabled={disabled}
      className={`inline-flex min-h-11 items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 ${styles[variant]} ${className}`}
      {...props}
    >
      {children}
    </button>
  )
}

export function TextField({ label, hint, error, className = '', ...props }) {
  const id = props.id || props.name
  return (
    <div className={className}>
      <label htmlFor={id} className="mb-1.5 block text-sm font-semibold text-ink">{label}</label>
      <input
        id={id}
        className={`min-h-11 w-full rounded-xl border bg-white px-3.5 py-2.5 text-sm text-ink outline-none transition placeholder:text-slate-400 focus:border-brand-500 focus:ring-4 focus:ring-brand-100 ${error ? 'border-rose-400' : 'border-slate-300'}`}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${id}-error` : hint ? `${id}-hint` : undefined}
        {...props}
      />
      {error && <p id={`${id}-error`} className="mt-1 text-xs text-rose-700">{error}</p>}
      {!error && hint && <p id={`${id}-hint`} className="mt-1 text-xs text-muted">{hint}</p>}
    </div>
  )
}

export function SelectField({ label, children, className = '', ...props }) {
  const id = props.id || props.name
  return (
    <div className={className}>
      <label htmlFor={id} className="mb-1.5 block text-sm font-semibold text-ink">{label}</label>
      <select
        id={id}
        className="min-h-11 w-full rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm text-ink outline-none transition focus:border-brand-500 focus:ring-4 focus:ring-brand-100"
        {...props}
      >
        {children}
      </select>
    </div>
  )
}

export function Alert({ children, tone = 'error', className = '' }) {
  const tones = {
    error: 'border-rose-200 bg-rose-50 text-rose-800',
    warning: 'border-amber-200 bg-amber-50 text-amber-900',
    info: 'border-brand-100 bg-brand-50 text-brand-900',
    success: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  }
  return (
    <div role={tone === 'error' ? 'alert' : 'status'} className={`flex gap-2.5 rounded-xl border px-4 py-3 text-sm leading-6 ${tones[tone]} ${className}`}>
      {tone !== 'success' && <AlertCircle size={18} className="mt-0.5 shrink-0" aria-hidden="true" />}
      <div>{children}</div>
    </div>
  )
}

export function Loading({ label = 'Loading…' }) {
  return (
    <div className="flex items-center justify-center gap-2 py-12 text-sm text-muted" role="status">
      <LoaderCircle size={18} className="animate-spin" aria-hidden="true" />
      {label}
    </div>
  )
}

export function EmptyState({ title, children, action }) {
  return (
    <div className="px-6 py-12 text-center">
      <div className="mx-auto mb-4 grid h-12 w-12 place-items-center rounded-full bg-brand-50 text-brand-700">
        <ArrowLeft size={20} className="rotate-180" aria-hidden="true" />
      </div>
      <h3 className="font-semibold text-ink">{title}</h3>
      {children && <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-muted">{children}</p>}
      {action && <div className="mt-5">{action}</div>}
    </div>
  )
}

export function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null
  return (
    <div className="flex items-center justify-between border-t border-slate-100 px-5 py-4">
      <span className="text-sm text-muted">Page {page + 1} of {totalPages}</span>
      <div className="flex gap-2">
        <Button variant="secondary" className="min-h-9 px-3 py-1.5" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>
          <ChevronLeft size={16} aria-hidden="true" /> Previous
        </Button>
        <Button variant="secondary" className="min-h-9 px-3 py-1.5" disabled={page + 1 >= totalPages} onClick={() => onPageChange(page + 1)}>
          Next <ChevronRight size={16} aria-hidden="true" />
        </Button>
      </div>
    </div>
  )
}

export function StatusBadge({ value }) {
  const normalized = String(value || 'UNKNOWN').toUpperCase()
  const tone = normalized.includes('CONFIRM') || normalized === 'ACTIVE' || normalized === 'PROCESSED'
    ? 'bg-emerald-50 text-emerald-700'
    : normalized.includes('CANCEL') || normalized === 'INACTIVE' || normalized === 'REJECTED'
      ? 'bg-slate-100 text-slate-600'
      : normalized.includes('PENDING') || normalized === 'REQUESTED'
        ? 'bg-amber-50 text-amber-800'
        : 'bg-brand-50 text-brand-800'
  return <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-bold ${tone}`}>{normalized.replaceAll('_', ' ')}</span>
}

