import { useEffect, useState } from 'react'
import { ArrowRight, CalendarDays, MapPin, Search, Ticket, UserRound, UsersRound } from 'lucide-react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { Alert, Button, Card, EmptyState, Loading, PageHeading, Pagination, SelectField, StatusBadge, TextField } from '../components/Common'
import api, { errorMessage, responseData } from '../lib/api'
import { formatDate, formatDateTime, formatMoney } from '../lib/formatters'

const GENDERS = ['FEMALE', 'MALE', 'OTHER']
const ACTIVE_BOOKING_STATUSES = new Set(['PENDING', 'CONFIRMED'])
const TODAY = new Date().toISOString().slice(0, 10)

function BookingRoute({ sourceStation, destinationStation, journeyDate }) {
  return (
    <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm">
      <span className="inline-flex items-center gap-1.5 font-semibold text-ink"><MapPin size={15} className="text-brand-600" />{sourceStation?.stationCode} · {sourceStation?.stationName}</span>
      <ArrowRight size={15} className="text-slate-400" aria-hidden="true" />
      <span className="inline-flex items-center gap-1.5 font-semibold text-ink">{destinationStation?.stationCode} · {destinationStation?.stationName}</span>
      <span className="inline-flex items-center gap-1.5 text-muted"><CalendarDays size={14} />{formatDate(journeyDate)}</span>
    </div>
  )
}

export function BookingStartPage() {
  const [params] = useSearchParams()
  const trainId = params.get('trainId')
  const source = params.get('source') || ''
  const destination = params.get('destination') || ''
  const date = params.get('date') || ''
  const classCode = params.get('classCode') || ''
  const navigate = useNavigate()
  const requestKey = JSON.stringify([trainId, source, destination, date, classCode])
  const journeyComplete = Boolean(trainId && source && destination && date && classCode)
  const [result, setResult] = useState(null)
  const [passengers, setPassengers] = useState([{ fullName: '', age: '', gender: 'OTHER' }])
  const [bookingBusy, setBookingBusy] = useState(false)
  const [bookingError, setBookingError] = useState('')

  useEffect(() => {
    if (!journeyComplete) return
    let active = true
    Promise.all([
      api.get(`/api/trains/${trainId}`),
      api.get(`/api/trains/${trainId}/availability`, { params: { source, destination, date, classCode } }),
    ])
      .then(([trainEnvelope, availabilityEnvelope]) => {
        if (!active) return
        setResult({
          key: requestKey,
          train: responseData(trainEnvelope),
          availability: responseData(availabilityEnvelope),
          error: '',
        })
      })
      .catch((requestError) => {
        if (active) setResult({ key: requestKey, train: null, availability: null, error: errorMessage(requestError) })
      })
    return () => { active = false }
  }, [trainId, source, destination, date, classCode, journeyComplete, requestKey])

  const loading = journeyComplete && result?.key !== requestKey
  const train = result?.key === requestKey ? result.train : null
  const availability = result?.key === requestKey ? result.availability : null
  const error = journeyComplete
    ? result?.key === requestKey ? result.error : ''
    : 'Journey details are incomplete. Search for a train and check availability first.'
  const trainRunId = availability?.trainRunId || train?.trainRunId || params.get('trainRunId')
  const hasTrainRunId = Number.isSafeInteger(Number(trainRunId)) && Number(trainRunId) > 0

  function updatePassenger(index, field, value) {
    setPassengers((current) => current.map((passenger, itemIndex) => itemIndex === index
      ? { ...passenger, [field]: value }
      : passenger))
  }

  function addPassenger() {
    setPassengers((current) => current.length < 6
      ? [...current, { fullName: '', age: '', gender: 'OTHER' }]
      : current)
  }

  function removePassenger(index) {
    setPassengers((current) => current.length > 1 ? current.filter((_, itemIndex) => itemIndex !== index) : current)
  }

  async function submitBooking(event) {
    event.preventDefault()
    if (!hasTrainRunId || !availability || availability.availableSeats < passengers.length) return
    const invalidPassenger = passengers.some((passenger) => (
      !passenger.fullName.trim()
      || !Number.isInteger(Number(passenger.age))
      || Number(passenger.age) < 1
      || Number(passenger.age) > 120
    ))
    if (invalidPassenger) {
      setBookingError('Enter a passenger name and an age from 1 to 120 for every passenger.')
      return
    }
    setBookingError('')
    setBookingBusy(true)
    try {
      const envelope = await api.post('/api/bookings', {
        trainRunId: Number(trainRunId),
        sourceStation: source,
        destinationStation: destination,
        journeyDate: date,
        classCode,
        passengers: passengers.map((passenger) => ({
          fullName: passenger.fullName.trim(),
          age: Number(passenger.age),
          gender: passenger.gender,
        })),
      })
      const booking = responseData(envelope)
      navigate(`/bookings/${encodeURIComponent(booking.pnrNumber)}`, { replace: true })
    } catch (requestError) {
      setBookingError(errorMessage(requestError))
    } finally {
      setBookingBusy(false)
    }
  }

  if (loading) return <div className="mx-auto max-w-4xl px-4 py-12 sm:px-6"><Card><Loading label="Refreshing seat availability…" /></Card></div>

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8 lg:py-12">
      <PageHeading eyebrow="Reservation" title="Passenger details" description="Review the live availability and add each passenger to prepare your reservation." />
      {error && <Alert className="mb-5">{error}</Alert>}
      {train && availability && (
        <div className="space-y-5">
          <form onSubmit={submitBooking}>
          <Card className="p-5 sm:p-6">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <p className="text-xs font-bold uppercase tracking-wider text-brand-700">{train.trainNumber} · {train.trainType}</p>
                <h2 className="mt-1 font-display text-xl font-bold text-ink">{train.trainName}</h2>
                <div className="mt-4"><BookingRoute sourceStation={availability.sourceStation} destinationStation={availability.destinationStation} journeyDate={availability.journeyDate} /></div>
              </div>
              <div className="rounded-xl bg-brand-50 px-4 py-3 text-right">
                <p className="text-xs text-muted">{classCode} · {availability.availableSeats} seats available</p>
                <p className="mt-1 text-lg font-bold text-brand-900">{formatMoney(availability.farePerPassenger, availability.currency)} <span className="text-xs font-medium text-muted">/ passenger</span></p>
              </div>
            </div>
          </Card>

          <Card className="p-5 sm:p-6">
            <div className="mb-5 flex items-center justify-between gap-4">
              <div><h2 className="font-display text-lg font-bold text-ink">Passengers</h2><p className="mt-1 text-sm text-muted">Add up to 6 passengers.</p></div>
              <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-bold text-slate-600">{passengers.length} of 6</span>
            </div>
            <div className="space-y-4">
              {passengers.map((passenger, index) => (
                <div key={index} className="rounded-xl border border-slate-200 p-4">
                  <div className="mb-3 flex items-center justify-between">
                    <p className="flex items-center gap-2 text-sm font-bold text-ink"><UserRound size={16} className="text-brand-600" /> Passenger {index + 1}</p>
                    {passengers.length > 1 && <button type="button" onClick={() => removePassenger(index)} className="text-xs font-semibold text-rose-700 hover:underline">Remove</button>}
                  </div>
                  <div className="grid gap-3 sm:grid-cols-3">
                    <TextField label="Full name" name={`passenger-${index}-name`} required maxLength={120} value={passenger.fullName} onChange={(event) => updatePassenger(index, 'fullName', event.target.value)} placeholder="As on ID" className="sm:col-span-1" />
                    <TextField label="Age" name={`passenger-${index}-age`} type="number" min="1" max="120" required value={passenger.age} onChange={(event) => updatePassenger(index, 'age', event.target.value)} placeholder="Age" />
                    <SelectField label="Gender" name={`passenger-${index}-gender`} value={passenger.gender} onChange={(event) => updatePassenger(index, 'gender', event.target.value)}>
                      {GENDERS.map((gender) => <option key={gender} value={gender}>{gender.charAt(0) + gender.slice(1).toLowerCase()}</option>)}
                    </SelectField>
                  </div>
                </div>
              ))}
            </div>
            <Button variant="secondary" className="mt-4" disabled={passengers.length >= 6} onClick={addPassenger}><UsersRound size={16} /> Add passenger</Button>
          </Card>

          {bookingError && <Alert className="mt-5">{bookingError}</Alert>}
          {!hasTrainRunId ? (
            <Alert tone="warning" className="mt-5">
              <strong>Booking is blocked because the response did not include a train-run ID.</strong> The booking endpoint requires <code className="rounded bg-amber-100 px-1 py-0.5">trainRunId</code>. Passenger details are not submitted or saved. Use a backend version that returns the matching run ID in search or availability data.
            </Alert>
          ) : (
            <Alert tone="info" className="mt-5">
              Submitting creates a reservation with the backend’s current payment status. No payment-processing endpoint is available, so this page will not simulate a payment.
            </Alert>
          )}
          <div className="flex flex-col-reverse justify-between gap-3 sm:flex-row">
            <Button variant="secondary" onClick={() => navigate(-1)}>Back to results</Button>
            <Button type="submit" disabled={!hasTrainRunId || bookingBusy || availability.availableSeats < passengers.length}>
              {bookingBusy ? 'Submitting booking…' : 'Create booking'} <ArrowRight size={16} />
            </Button>
          </div>
          </form>
        </div>
      )}
    </div>
  )
}

function BookingCard({ booking }) {
  return (
    <Link to={`/bookings/${encodeURIComponent(booking.pnrNumber)}`} className="block rounded-2xl border border-slate-200 bg-white p-5 shadow-card transition hover:-translate-y-0.5 hover:border-brand-200 sm:p-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-xs font-bold uppercase tracking-wider text-brand-700">PNR {booking.pnrNumber}</p>
          <h2 className="mt-1 font-display text-lg font-bold text-ink">{booking.trainName}</h2>
          <p className="mt-1 text-sm text-muted">{booking.trainNumber}</p>
        </div>
        <StatusBadge value={booking.bookingStatus} />
      </div>
      <div className="mt-5">
        <BookingRoute sourceStation={booking.sourceStation} destinationStation={booking.destinationStation} journeyDate={booking.journeyDate} />
      </div>
      <div className="mt-5 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-4 text-sm">
        <span className="inline-flex items-center gap-1.5 text-muted"><UsersRound size={15} /> {booking.passengerCount} passenger{booking.passengerCount === 1 ? '' : 's'}</span>
        <span className="font-bold text-ink">{formatMoney(booking.totalFare)}</span>
        <span className="inline-flex items-center gap-1 font-semibold text-brand-700">Details <ArrowRight size={15} /></span>
      </div>
    </Link>
  )
}

export function DashboardPage() {
  const { user } = useAuth()
  const [bookings, setBookings] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    api.get('/api/bookings/my-bookings', { params: { page: 0, size: 5 } })
      .then((envelope) => { if (active) setBookings(responseData(envelope)?.bookings || []) })
      .catch((requestError) => { if (active) setError(errorMessage(requestError)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  const upcoming = bookings.filter((booking) => booking.journeyDate >= TODAY && booking.bookingStatus !== 'CANCELLED').length

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 lg:py-12">
      <PageHeading eyebrow="Your account" title={`Good to see you, ${user?.fullName?.split(' ')[0] || 'traveller'}.`} description="Your reservation details and recent journeys, all in one place." action={<Link to="/trains/search" className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-brand-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-900">Find a train <ArrowRight size={16} /></Link>} />
      <div className="mb-8 grid gap-4 sm:grid-cols-3">
        <Card className="p-5"><p className="text-sm text-muted">Recent reservations</p><p className="mt-2 text-3xl font-bold text-ink">{loading ? '—' : bookings.length}</p></Card>
        <Card className="p-5"><p className="text-sm text-muted">Upcoming journeys</p><p className="mt-2 text-3xl font-bold text-ink">{loading ? '—' : upcoming}</p></Card>
        <Card className="p-5"><p className="text-sm text-muted">Signed in as</p><p className="mt-2 truncate text-base font-bold text-ink">{user?.email}</p></Card>
      </div>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="font-display text-xl font-bold text-ink">Recent bookings</h2>
        <Link to="/bookings" className="text-sm font-bold text-brand-700 hover:text-brand-900">View all</Link>
      </div>
      {error ? <Alert>{error}</Alert> : loading ? <Card><Loading label="Loading your bookings…" /></Card> : bookings.length ? <div className="space-y-4">{bookings.map((booking) => <BookingCard key={booking.pnrNumber} booking={booking} />)}</div> : <Card><EmptyState title="No journeys yet" action={<Link to="/trains/search" className="text-sm font-bold text-brand-700">Search trains</Link>}>When you have a reservation, it will appear here.</EmptyState></Card>}
    </div>
  )
}

export function BookingHistoryPage() {
  const [page, setPage] = useState(0)
  const [result, setResult] = useState(null)

  useEffect(() => {
    let active = true
    api.get('/api/bookings/my-bookings', { params: { page, size: 10 } })
      .then((envelope) => { if (active) setResult({ page, data: responseData(envelope), error: '' }) })
      .catch((requestError) => { if (active) setResult({ page, data: null, error: errorMessage(requestError) }) })
    return () => { active = false }
  }, [page])

  const loading = !result || result.page !== page
  const data = result?.page === page ? result.data : null
  const error = result?.page === page ? result.error : ''

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8 lg:py-12">
      <PageHeading eyebrow="Your account" title="Booking history" description="Review your reservations, passenger counts and current status." action={<Link to="/pnr" className="text-sm font-bold text-brand-700 hover:text-brand-900">Look up a PNR</Link>} />
      {error ? <Alert>{error}</Alert> : loading ? <Card><Loading label="Loading booking history…" /></Card> : data?.bookings?.length ? (
        <div className="space-y-4">
          {data.bookings.map((booking) => <BookingCard key={booking.pnrNumber} booking={booking} />)}
          <Card><Pagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} /></Card>
        </div>
      ) : <Card><EmptyState title="No bookings found">Your booking history will show here once you have a reservation.</EmptyState></Card>}
    </div>
  )
}

export function PnrLookupPage() {
  const navigate = useNavigate()
  const [pnr, setPnr] = useState('')
  const [error, setError] = useState('')

  function submit(event) {
    event.preventDefault()
    const value = pnr.trim()
    if (!value) {
      setError('Enter a PNR number to continue.')
      return
    }
    setError('')
    navigate(`/bookings/${encodeURIComponent(value.toUpperCase())}`)
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-12 sm:px-6 lg:px-8 lg:py-20">
      <Card className="p-6 sm:p-9">
        <div className="mb-7 grid h-12 w-12 place-items-center rounded-xl bg-brand-50 text-brand-700"><Ticket size={23} /></div>
        <PageHeading eyebrow="Reservation lookup" title="Check your PNR status." description="Enter your PNR to view its booking and passenger details. Sign in with the account that owns the reservation." />
        <form onSubmit={submit} className="grid gap-3 sm:grid-cols-[1fr_auto] sm:items-end">
          <TextField label="PNR number" name="pnr" required maxLength={64} value={pnr} onChange={(event) => setPnr(event.target.value)} placeholder="Enter your PNR" />
          <Button type="submit"><Search size={16} /> Look up PNR</Button>
        </form>
        {error && <Alert className="mt-4">{error}</Alert>}
      </Card>
    </div>
  )
}

export function BookingDetailsPage() {
  const { pnr } = useParams()
  const [booking, setBooking] = useState(null)
  const [cancellation, setCancellation] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [cancelBusy, setCancelBusy] = useState(false)
  const [confirmCancel, setConfirmCancel] = useState(false)

  async function loadBooking() {
    const result = responseData(await api.get(`/api/bookings/${encodeURIComponent(pnr)}`))
    setBooking(result)
  }

  useEffect(() => {
    let active = true
    api.get(`/api/bookings/${encodeURIComponent(pnr)}`)
      .then((envelope) => { if (active) setBooking(responseData(envelope)) })
      .catch((requestError) => { if (active) setError(errorMessage(requestError)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [pnr])

  async function cancelBooking() {
    setCancelBusy(true)
    setError('')
    try {
      setCancellation(responseData(await api.post(`/api/bookings/${encodeURIComponent(pnr)}/cancel`)))
      await loadBooking()
      setConfirmCancel(false)
    } catch (requestError) {
      setError(errorMessage(requestError))
    } finally {
      setCancelBusy(false)
    }
  }

  if (loading) return <div className="mx-auto max-w-4xl px-4 py-12 sm:px-6"><Card><Loading label="Retrieving PNR details…" /></Card></div>
  if (error && !booking) return <div className="mx-auto max-w-4xl px-4 py-12 sm:px-6"><Alert>{error}</Alert><Link to="/pnr" className="mt-4 inline-block text-sm font-semibold text-brand-700">Try another PNR</Link></div>
  if (!booking) return null

  const canCancel = ACTIVE_BOOKING_STATUSES.has(booking.bookingStatus)

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8 lg:py-12">
      <PageHeading eyebrow="Booking details" title={`PNR ${booking.pnrNumber}`} description={`${booking.trainNumber} · ${booking.trainName}`} action={<StatusBadge value={booking.bookingStatus} />} />
      {error && <Alert className="mb-4">{error}</Alert>}
      {cancellation && (
        <Alert tone="warning" className="mb-5">
          Cancellation status: <strong>{cancellation.refundStatus}</strong>. Refund amount: <strong>{formatMoney(cancellation.refundAmount)}</strong>. A refund marked REQUESTED is pending and has not been processed.
        </Alert>
      )}
      <Card className="p-5 sm:p-7">
        <BookingRoute sourceStation={booking.sourceStation} destinationStation={booking.destinationStation} journeyDate={booking.journeyDate} />
        <div className="mt-6 grid gap-4 border-y border-slate-100 py-5 sm:grid-cols-3">
          <div><p className="text-xs text-muted">Booking status</p><div className="mt-1"><StatusBadge value={booking.bookingStatus} /></div></div>
          <div><p className="text-xs text-muted">Payment status</p><div className="mt-1"><StatusBadge value={booking.paymentStatus} /></div></div>
          <div><p className="text-xs text-muted">Total fare</p><p className="mt-1 font-bold text-ink">{formatMoney(booking.totalFare, booking.currency)}</p></div>
          <div><p className="text-xs text-muted">Class</p><p className="mt-1 font-semibold text-ink">{booking.classCode}</p></div>
          <div><p className="text-xs text-muted">Fare per passenger</p><p className="mt-1 font-semibold text-ink">{formatMoney(booking.farePerPassenger, booking.currency)}</p></div>
          <div><p className="text-xs text-muted">Booked on</p><p className="mt-1 font-semibold text-ink">{formatDateTime(booking.bookingDate)}</p></div>
        </div>
        <div className="py-5">
          <h2 className="mb-3 flex items-center gap-2 font-display font-bold text-ink"><UsersRound size={18} className="text-brand-600" /> Passenger details</h2>
          <div className="divide-y divide-slate-100">
            {(booking.passengers || []).map((passenger, index) => (
              <div key={`${passenger.fullName}-${index}`} className="flex flex-wrap items-center justify-between gap-3 py-3 text-sm">
                <div><p className="font-semibold text-ink">{passenger.fullName}</p><p className="mt-0.5 text-xs text-muted">{passenger.age} years · {passenger.gender}</p></div>
                <div className="text-right text-xs text-muted"><p>{passenger.coachNumber ? `Coach ${passenger.coachNumber}` : 'Seat pending'}</p><p>{passenger.seatNumber ? `Seat ${passenger.seatNumber}` : ''}</p></div>
              </div>
            ))}
          </div>
        </div>
        {canCancel && (
          <div className="border-t border-slate-100 pt-5">
            {!confirmCancel ? (
              <Button variant="danger" onClick={() => setConfirmCancel(true)}>Request cancellation</Button>
            ) : (
              <div className="rounded-xl border border-amber-200 bg-amber-50 p-4">
                <p className="text-sm font-semibold text-amber-950">Cancel this booking?</p>
                <p className="mt-1 text-sm leading-6 text-amber-900">The backend will determine eligibility and refund amount. Any refund request may remain pending; this does not mean funds have been returned.</p>
                <div className="mt-4 flex flex-wrap gap-2">
                  <Button variant="danger" disabled={cancelBusy} onClick={cancelBooking}>{cancelBusy ? 'Submitting…' : 'Confirm cancellation'}</Button>
                  <Button variant="secondary" disabled={cancelBusy} onClick={() => setConfirmCancel(false)}>Keep booking</Button>
                </div>
              </div>
            )}
          </div>
        )}
      </Card>
      <Link to="/bookings" className="mt-5 inline-flex items-center gap-1 text-sm font-semibold text-brand-700 hover:text-brand-900">Back to booking history <ArrowRight size={14} /></Link>
    </div>
  )
}

export function ProfilePage() {
  const { user } = useAuth()
  return (
    <div className="mx-auto max-w-3xl px-4 py-8 sm:px-6 lg:px-8 lg:py-12">
      <PageHeading eyebrow="Your account" title="Profile" description="Your account information returned when you signed in." />
      <Card className="divide-y divide-slate-100">
        {[
          ['Full name', user?.fullName],
          ['Email address', user?.email],
          ['Phone number', user?.phoneNumber],
          ['Account role', user?.role],
        ].map(([label, value]) => <div key={label} className="flex flex-wrap justify-between gap-2 px-5 py-4"><span className="text-sm text-muted">{label}</span><span className="text-sm font-semibold text-ink">{value || '—'}</span></div>)}
      </Card>
      <Alert tone="info" className="mt-4">
        Profile editing is not available because the backend does not currently expose the requested user profile endpoint. The information shown is from your sign-in response.
      </Alert>
    </div>
  )
}
