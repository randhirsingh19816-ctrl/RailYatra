import { useEffect, useState } from 'react'
import { ArrowRight, Building2, CalendarDays, Ticket, Train, UsersRound } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Alert, Card, Loading, PageHeading } from '../components/Common'
import api, { errorMessage, responseData } from '../lib/api'

function MetricCard({ label, value, icon: Icon, href, unavailable = false }) {
  const content = (
    <div className={`flex items-start justify-between gap-3 p-5 ${href ? 'transition hover:bg-brand-50/60' : ''}`}>
      <div>
        <p className="text-sm text-muted">{label}</p>
        <p className="mt-2 text-3xl font-bold text-ink">{unavailable ? '—' : value ?? '—'}</p>
        <p className="mt-2 text-xs text-muted">{unavailable ? 'No matching API is currently available' : href ? 'Open management' : 'Management endpoint connected'}</p>
      </div>
      <span className={`grid h-10 w-10 place-items-center rounded-xl ${unavailable ? 'bg-slate-100 text-slate-500' : 'bg-brand-50 text-brand-700'}`}><Icon size={20} /></span>
    </div>
  )
  return <Card>{href ? <Link to={href} className="block">{content}</Link> : content}</Card>
}

export default function AdminOverviewPage() {
  const [counts, setCounts] = useState({ trains: null, stations: null })
  const [errors, setErrors] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    Promise.all([
      api.get('/api/admin/trains', { params: { page: 0, size: 1, sortBy: 'trainName', direction: 'asc' } }),
      api.get('/api/admin/stations', { params: { page: 0, size: 1, sortBy: 'stationName', direction: 'asc' } }),
    ])
      .then(([trainsEnvelope, stationsEnvelope]) => {
        if (!active) return
        setCounts({
          trains: responseData(trainsEnvelope)?.totalElements ?? 0,
          stations: responseData(stationsEnvelope)?.totalElements ?? 0,
        })
      })
      .catch((requestError) => { if (active) setErrors([errorMessage(requestError)]) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  return (
    <>
      <PageHeading eyebrow="Admin console" title="Overview" description="Manage the train network and its route information." />
      {errors.map((error) => <Alert key={error} className="mb-5">{error}</Alert>)}
      {loading ? <Card><Loading label="Loading management overview…" /></Card> : (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          <MetricCard label="Trains" value={counts.trains} icon={Train} href="/admin/trains" />
          <MetricCard label="Stations" value={counts.stations} icon={Building2} href="/admin/stations" />
          <MetricCard label="Routes" value="Manage" icon={Ticket} href="/admin/routes" />
          <MetricCard label="Schedules" icon={CalendarDays} unavailable />
          <MetricCard label="Bookings" icon={Ticket} unavailable />
          <MetricCard label="Users" icon={UsersRound} unavailable />
        </div>
      )}
      <div className="mt-6 rounded-2xl border border-amber-200 bg-amber-50 p-5">
        <p className="font-semibold text-amber-950">Some admin data is not exposed yet.</p>
        <p className="mt-1 text-sm leading-6 text-amber-900">The current backend does not provide schedule management, all-bookings, user-list, or dashboard-statistics endpoints. These cards are intentionally not populated with sample data.</p>
        <Link to="/admin/schedules" className="mt-3 inline-flex items-center gap-1 text-sm font-bold text-amber-900 hover:underline">View unavailable modules <ArrowRight size={14} /></Link>
      </div>
    </>
  )
}
