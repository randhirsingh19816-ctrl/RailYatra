import { useEffect, useState } from 'react'
import { ArrowDownUp, ArrowRight, CalendarDays, Clock3, Search } from 'lucide-react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { Alert, Button, Card, EmptyState, Loading, PageHeading, SelectField, TextField } from '../components/Common'
import api, { errorMessage, responseData } from '../lib/api'
import { formatDate, formatMoney } from '../lib/formatters'

function localDateValue() {
  const date = new Date()
  date.setMinutes(date.getMinutes() - date.getTimezoneOffset())
  return date.toISOString().slice(0, 10)
}

function SearchFields({ initial = {}, compact = false }) {
  const navigate = useNavigate()
  const [values, setValues] = useState({
    source: initial.source || '',
    destination: initial.destination || '',
    date: initial.date || localDateValue(),
  })
  const [formError, setFormError] = useState('')

  function submit(event) {
    event.preventDefault()
    const source = values.source.trim().toUpperCase()
    const destination = values.destination.trim().toUpperCase()
    if (!source || !destination) {
      setFormError('Enter both a source and destination station code or ID.')
      return
    }
    if (source.toLowerCase() === destination.toLowerCase()) {
      setFormError('Source and destination must be different.')
      return
    }
    setFormError('')
    const query = new URLSearchParams({ source, destination, date: values.date })
    navigate(`/trains/search?${query.toString()}`)
  }

  function swapStations() {
    setValues((current) => ({ ...current, source: current.destination, destination: current.source }))
  }

  return (
    <form onSubmit={submit} className={`rounded-2xl bg-white p-4 shadow-card sm:p-5 ${compact ? 'border border-slate-200' : ''}`}>
      {formError && <Alert className="mb-4">{formError}</Alert>}
      <div className="grid gap-3 md:grid-cols-[1fr_auto_1fr_0.85fr_auto] md:items-end">
        <TextField
          label="From"
          name="source"
          required
          maxLength={32}
          value={values.source}
          onChange={(event) => setValues((current) => ({ ...current, source: event.target.value }))}
          placeholder="Station code or ID"
          hint={!compact ? 'For example, NDLS or a station ID' : undefined}
        />
        <button type="button" onClick={swapStations} aria-label="Swap source and destination" className="hidden h-11 w-11 items-center justify-center rounded-full border border-slate-200 text-brand-700 transition hover:bg-brand-50 md:flex">
          <ArrowDownUp size={17} />
        </button>
        <TextField
          label="To"
          name="destination"
          required
          maxLength={32}
          value={values.destination}
          onChange={(event) => setValues((current) => ({ ...current, destination: event.target.value }))}
          placeholder="Station code or ID"
        />
        <TextField
          label="Journey date"
          name="date"
          type="date"
          required
          min={localDateValue()}
          value={values.date}
          onChange={(event) => setValues((current) => ({ ...current, date: event.target.value }))}
        />
        <Button type="submit" className="w-full md:w-auto"><Search size={17} /> Search</Button>
      </div>
      <p className="mt-3 text-xs leading-5 text-muted">
        Station suggestions are not available in the current backend API. Enter the station code or numeric station ID.
      </p>
    </form>
  )
}

export function HomePage() {
  const highlights = [
    { number: '01', title: 'Find your train', description: 'Search available services by station and travel date.' },
    { number: '02', title: 'Check the details', description: 'Compare classes, seat availability and estimated fare.' },
    { number: '03', title: 'Keep it all together', description: 'View your reservations and PNR details in one place.' },
  ]

  return (
    <>
      <section className="relative overflow-hidden bg-ink text-white">
        <div className="absolute -right-20 -top-32 h-[440px] w-[440px] rounded-full border border-white/10" />
        <div className="absolute -right-4 -top-16 h-[310px] w-[310px] rounded-full border border-white/10" />
        <div className="mx-auto grid max-w-7xl gap-10 px-4 pb-14 pt-14 sm:px-6 sm:pb-20 sm:pt-20 lg:grid-cols-[1fr_0.85fr] lg:items-center lg:px-8">
          <div className="relative z-10">
            <p className="mb-4 inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/5 px-3 py-1.5 text-xs font-semibold tracking-wide text-white/80">
              <span className="h-2 w-2 rounded-full bg-accent" /> A better way to get there
            </p>
            <h1 className="max-w-2xl font-display text-4xl font-bold leading-[1.08] tracking-tight sm:text-5xl lg:text-6xl">
              The journey is part of the <span className="text-accent">story.</span>
            </h1>
            <p className="mt-5 max-w-xl text-base leading-7 text-white/65 sm:text-lg">
              Find a train, check the fare and availability, and keep your reservation details close at hand.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to="/trains/search" className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-accent px-5 py-3 text-sm font-bold text-ink transition hover:bg-amber-300">
                Plan a journey <ArrowRight size={17} />
              </Link>
              <Link to="/pnr" className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-white/20 px-5 py-3 text-sm font-semibold text-white transition hover:bg-white/10">
                Check PNR status
              </Link>
            </div>
          </div>
          <div className="relative z-10">
            <div className="mb-3 flex items-center gap-2 text-sm font-semibold text-white/75"><Search size={17} className="text-accent" /> Start with a route</div>
            <SearchFields />
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-4 py-14 sm:px-6 lg:px-8 lg:py-20">
        <div className="mb-8 max-w-xl">
          <p className="mb-2 text-xs font-bold uppercase tracking-[0.18em] text-brand-600">Simple by design</p>
          <h2 className="font-display text-3xl font-bold tracking-tight text-ink">From search to station.</h2>
        </div>
        <div className="grid gap-4 md:grid-cols-3">
          {highlights.map((item) => (
            <Card key={item.number} className="p-6">
              <p className="mb-5 text-sm font-bold tracking-widest text-brand-600">{item.number}</p>
              <h3 className="font-display text-lg font-bold text-ink">{item.title}</h3>
              <p className="mt-2 text-sm leading-6 text-muted">{item.description}</p>
            </Card>
          ))}
        </div>
        <p className="mt-5 text-xs leading-5 text-muted">Reservations use the selected train-run ID. The backend currently records payment as pending and does not provide a payment-processing endpoint.</p>
      </section>
    </>
  )
}

function RouteDisplay({ source, destination, departureTime, arrivalTime, durationMinutes }) {
  const duration = durationMinutes
    ? `${Math.floor(durationMinutes / 60)}h ${durationMinutes % 60}m`
    : null
  return (
    <div className="grid grid-cols-[1fr_auto_1fr] items-center gap-4">
      <div>
        <p className="text-lg font-bold text-ink">{departureTime ? new Date(departureTime).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' }) : '—'}</p>
        <p className="mt-1 text-sm font-semibold text-ink">{source?.stationCode || '—'}</p>
        <p className="text-xs text-muted">{source?.stationName || 'Station'}</p>
      </div>
      <div className="flex min-w-20 flex-col items-center gap-1 text-xs text-muted">
        {duration && <span>{duration}</span>}
        <span className="route-line relative h-px w-full"><span className="absolute -right-0.5 -top-[4px] h-2 w-2 rounded-full bg-brand-600" /></span>
        <span className="whitespace-nowrap">Direct route</span>
      </div>
      <div className="text-right">
        <p className="text-lg font-bold text-ink">{arrivalTime ? new Date(arrivalTime).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' }) : '—'}</p>
        <p className="mt-1 text-sm font-semibold text-ink">{destination?.stationCode || '—'}</p>
        <p className="text-xs text-muted">{destination?.stationName || 'Station'}</p>
      </div>
    </div>
  )
}

function SearchResultCard({ train, date, source, destination }) {
  const navigate = useNavigate()
  const [classCode, setClassCode] = useState(train.availableClasses?.[0]?.classCode || '')
  const [availability, setAvailability] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function checkAvailability() {
    setError('')
    setAvailability(null)
    setBusy(true)
    try {
      const result = responseData(await api.get(`/api/trains/${train.trainId}/availability`, {
        params: { source, destination, date, classCode },
      }))
      setAvailability(result)
    } catch (requestError) {
      setError(errorMessage(requestError))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Card className="overflow-hidden">
      <div className="grid gap-5 p-5 sm:p-6 lg:grid-cols-[1fr_0.95fr] lg:items-center">
        <div>
          <div className="mb-4 flex flex-wrap items-center gap-2">
            <span className="rounded-lg bg-brand-50 px-2.5 py-1 text-xs font-bold text-brand-800">{train.trainNumber}</span>
            <h2 className="font-display text-lg font-bold text-ink">{train.trainName}</h2>
          </div>
          <RouteDisplay
            source={train.sourceStation}
            destination={train.destinationStation}
            departureTime={train.departureTime}
            arrivalTime={train.arrivalTime}
            durationMinutes={train.durationMinutes}
          />
          <p className="mt-4 flex items-center gap-1.5 text-xs text-muted"><CalendarDays size={14} /> {formatDate(train.journeyDate || date)}</p>
        </div>
        <div className="rounded-xl bg-paper p-4">
          <div className="grid gap-3 sm:grid-cols-[1fr_auto] sm:items-end">
            <SelectField label="Travel class" name={`class-${train.trainId}`} value={classCode} onChange={(event) => { setClassCode(event.target.value); setAvailability(null) }}>
              {(train.availableClasses || []).map((item) => <option key={item.classCode} value={item.classCode}>{item.classCode} · {item.className}</option>)}
            </SelectField>
            <Button onClick={checkAvailability} disabled={!classCode || busy} variant="secondary" className="w-full sm:w-auto">
              {busy ? 'Checking…' : 'Check seats & fare'}
            </Button>
          </div>
          {error && <Alert className="mt-3">{error}</Alert>}
          {availability && (
            <div className="mt-4 border-t border-slate-200 pt-4">
              <div className="flex flex-wrap items-end justify-between gap-3">
                <div>
                  <p className="text-xs text-muted">Available seats</p>
                  <p className="mt-0.5 text-xl font-bold text-ink">{availability.availableSeats} <span className="text-xs font-medium text-muted">of {availability.totalSeats}</span></p>
                </div>
                <div className="text-right">
                  <p className="text-xs text-muted">Fare per passenger</p>
                  <p className="mt-0.5 text-xl font-bold text-brand-800">{formatMoney(availability.farePerPassenger, availability.currency)}</p>
                </div>
              </div>
              <Button
                className="mt-4 w-full"
                disabled={availability.availableSeats < 1}
                onClick={() => {
                    const params = new URLSearchParams({ trainId: String(train.trainId), source, destination, date, classCode, fare: String(availability.farePerPassenger), seats: String(availability.availableSeats) })
                    const trainRunId = availability.trainRunId || train.trainRunId
                    if (trainRunId) params.set('trainRunId', String(trainRunId))
                  navigate(`/booking/start?${params.toString()}`)
                }}
              >
                Continue to passenger details <ArrowRight size={16} />
              </Button>
            </div>
          )}
        </div>
      </div>
      <div className="flex justify-end border-t border-slate-100 px-5 py-3 sm:px-6">
        <Link to={`/trains/${train.trainId}`} className="inline-flex items-center gap-1 text-sm font-semibold text-brand-700 hover:text-brand-900">
          Train details <ArrowRight size={15} />
        </Link>
      </div>
    </Card>
  )
}

export function TrainSearchPage() {
  const [searchParams] = useSearchParams()
  const source = searchParams.get('source') || ''
  const destination = searchParams.get('destination') || ''
  const date = searchParams.get('date') || localDateValue()
  const classCode = searchParams.get('classCode') || ''
  const searchKey = JSON.stringify([source, destination, date, classCode])
  const [result, setResult] = useState(null)

  useEffect(() => {
    if (!source || !destination) return
    let active = true
    api.get('/api/trains/search', { params: { source, destination, date, ...(classCode ? { classCode } : {}) } })
      .then((envelope) => { if (active) setResult({ key: searchKey, trains: responseData(envelope) || [], error: '' }) })
      .catch((requestError) => { if (active) setResult({ key: searchKey, trains: [], error: errorMessage(requestError) }) })
    return () => { active = false }
  }, [source, destination, date, classCode, searchKey])

  const loading = Boolean(source && destination) && result?.key !== searchKey
  const trains = result?.key === searchKey ? result.trains : []
  const error = result?.key === searchKey ? result.error : ''

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 lg:py-12">
      <PageHeading eyebrow="Train search" title="Find your way there." description="Search by station code or station ID, then compare available services for your date." />
      <div className="mb-7"><SearchFields compact initial={{ source, destination, date }} /></div>
      {!source || !destination ? (
        <Card><EmptyState title="Start with a route" action={<Link to="/" className="text-sm font-bold text-brand-700">Back to home</Link>}>Enter the source and destination codes above to find train services.</EmptyState></Card>
      ) : loading ? (
        <Card><Loading label="Searching trains…" /></Card>
      ) : error ? (
        <Alert>{error}</Alert>
      ) : trains.length === 0 ? (
        <Card><EmptyState title="No trains found">There are no services for this route and date. Check the station codes and try another date.</EmptyState></Card>
      ) : (
        <div className="space-y-4">
          <p className="text-sm text-muted">{trains.length} {trains.length === 1 ? 'service' : 'services'} found · {formatDate(date)}</p>
          {trains.map((train) => <SearchResultCard key={`${train.trainId}-${train.departureTime}`} train={train} date={date} source={source} destination={destination} />)}
        </div>
      )}
    </div>
  )
}

export function TrainDetailsPage() {
  const { trainId } = useParams()
  const [train, setTrain] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    api.get(`/api/trains/${trainId}`)
      .then((envelope) => { if (active) setTrain(responseData(envelope)) })
      .catch((requestError) => { if (active) setError(errorMessage(requestError)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [trainId])

  if (loading) return <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8"><Card><Loading label="Loading train details…" /></Card></div>
  if (error) return <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8"><Alert>{error}</Alert></div>
  if (!train) return null

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8 lg:py-12">
      <PageHeading eyebrow={`Train ${train.trainNumber}`} title={train.trainName} description={`${train.trainType} service`} action={<span className={`rounded-full px-3 py-1 text-xs font-bold ${train.active ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'}`}>{train.active ? 'Active' : 'Inactive'}</span>} />
      <Card>
        <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
          <h2 className="font-semibold text-ink">Route stops</h2>
          <span className="text-xs text-muted">{train.routeStops?.length || 0} stops</span>
        </div>
        {!train.routeStops?.length ? (
          <EmptyState title="Route details are unavailable">This train has no published route stops in the current data.</EmptyState>
        ) : (
          <ol className="divide-y divide-slate-100">
            {train.routeStops.map((stop) => (
              <li key={`${stop.stationId}-${stop.stopNumber}`} className="grid grid-cols-[34px_1fr_auto] items-center gap-3 px-5 py-4">
                <span className="grid h-8 w-8 place-items-center rounded-full bg-brand-50 text-xs font-bold text-brand-800">{stop.stopNumber}</span>
                <div>
                  <p className="font-semibold text-ink">{stop.stationName}</p>
                  <p className="text-xs text-muted">{stop.stationCode} · Day {stop.dayNumber}</p>
                </div>
                <div className="text-right text-xs text-muted">
                  {stop.arrivalTime && <p><Clock3 size={12} className="mr-1 inline" />Arrive {stop.arrivalTime}</p>}
                  {stop.departureTime && <p className="mt-1">Depart {stop.departureTime}</p>}
                </div>
              </li>
            ))}
          </ol>
        )}
      </Card>
      <div className="mt-5"><Link to="/trains/search" className="text-sm font-semibold text-brand-700 hover:text-brand-900">Search another train</Link></div>
    </div>
  )
}
