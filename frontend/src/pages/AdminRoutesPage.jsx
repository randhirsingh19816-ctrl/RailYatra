import { useEffect, useState } from 'react'
import { Plus, Save, Trash2 } from 'lucide-react'
import { Alert, Button, Card, EmptyState, Loading, PageHeading, SelectField, TextField } from '../components/Common'
import api, { errorMessage, responseData } from '../lib/api'

function emptyStop(stopNumber) {
  return {
    stationCode: '',
    stopNumber,
    arrivalTime: '',
    departureTime: '',
    dayNumber: 1,
    distanceFromSource: stopNumber === 1 ? '0' : '',
  }
}

export default function AdminRoutesPage() {
  const [trains, setTrains] = useState([])
  const [stations, setStations] = useState([])
  const [trainId, setTrainId] = useState('')
  const [stops, setStops] = useState([])
  const [routeExists, setRouteExists] = useState(false)
  const [loading, setLoading] = useState(true)
  const [routeLoading, setRouteLoading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    let active = true
    Promise.all([
      api.get('/api/admin/trains', { params: { page: 0, size: 100, sortBy: 'trainName', direction: 'asc', active: true } }),
      api.get('/api/admin/stations', { params: { page: 0, size: 100, sortBy: 'stationName', direction: 'asc' } }),
    ])
      .then(([trainEnvelope, stationEnvelope]) => {
        if (!active) return
        setTrains(responseData(trainEnvelope)?.trains || [])
        setStations((responseData(stationEnvelope)?.stations || []).filter((station) => station.active))
      })
      .catch((requestError) => { if (active) setError(errorMessage(requestError)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (!trainId) return
    let active = true
    api.get(`/api/admin/trains/${trainId}/route`)
      .then((envelope) => {
        if (!active) return
        const route = responseData(envelope)
        setStops((route?.stops || []).map((stop) => ({
          stationCode: stop.stationCode,
          stopNumber: stop.stopNumber,
          arrivalTime: stop.arrivalTime || '',
          departureTime: stop.departureTime || '',
          dayNumber: stop.dayNumber,
          distanceFromSource: String(stop.distanceFromSource ?? ''),
        })))
        setRouteExists(true)
      })
      .catch((requestError) => {
        if (!active) return
        if (requestError?.response?.status === 404) {
          setStops([emptyStop(1), emptyStop(2)])
          setRouteExists(false)
          setNotice('No route exists for this train yet. Define at least two stops to create one.')
        } else {
          setError(errorMessage(requestError))
        }
      })
      .finally(() => { if (active) setRouteLoading(false) })
    return () => { active = false }
  }, [trainId])

  function selectTrain(value) {
    setTrainId(value)
    setRouteLoading(Boolean(value))
    setError('')
    setNotice('')
    if (!value) {
      setStops([])
      setRouteExists(false)
    }
  }

  function updateStop(index, field, value) {
    setStops((current) => current.map((stop, itemIndex) => itemIndex === index
      ? { ...stop, [field]: value }
      : stop))
  }

  function addStop() {
    setStops((current) => [...current, emptyStop(current.length + 1)])
  }

  function removeStop(index) {
    setStops((current) => current.filter((_, itemIndex) => itemIndex !== index)
      .map((stop, itemIndex) => ({ ...stop, stopNumber: itemIndex + 1 })))
  }

  async function saveRoute(event) {
    event.preventDefault()
    setError('')
    setNotice('')
    if (stops.length < 2) {
      setError('A route must have at least two stops.')
      return
    }
    setBusy(true)
    const request = {
      stops: stops.map((stop, index) => ({
        stationCode: stop.stationCode.trim().toUpperCase(),
        stopNumber: index + 1,
        arrivalTime: stop.arrivalTime || null,
        departureTime: stop.departureTime || null,
        dayNumber: Number(stop.dayNumber),
        distanceFromSource: stop.distanceFromSource,
      })),
    }
    try {
      const path = `/api/admin/trains/${trainId}/route`
      const envelope = routeExists ? await api.put(path, request) : await api.post(path, request)
      const route = responseData(envelope)
      setRouteExists(true)
      setStops((route?.stops || []).map((stop) => ({
        stationCode: stop.stationCode,
        stopNumber: stop.stopNumber,
        arrivalTime: stop.arrivalTime || '',
        departureTime: stop.departureTime || '',
        dayNumber: stop.dayNumber,
        distanceFromSource: String(stop.distanceFromSource ?? ''),
      })))
      setNotice('Route saved.')
    } catch (requestError) {
      setError(errorMessage(requestError))
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <><PageHeading eyebrow="Admin console" title="Routes" description="Define ordered station stops for each train." /><Card><Loading label="Loading trains and stations…" /></Card></>

  return (
    <>
      <PageHeading eyebrow="Admin console" title="Train routes" description="Define each train’s ordered station stops. The backend preserves existing runs and bookings when a route cannot safely be changed." />
      {error && <Alert className="mb-4">{error}</Alert>}
      {notice && <Alert tone="info" className="mb-4">{notice}</Alert>}
      <Card className="mb-5 p-5 sm:p-6">
        <SelectField label="Select train" name="routeTrain" value={trainId} onChange={(event) => selectTrain(event.target.value)}>
          <option value="">Choose an active train</option>
          {trains.map((train) => <option key={train.id} value={train.id}>{train.trainNumber} · {train.trainName}</option>)}
        </SelectField>
        {!trains.length && <p className="mt-2 text-xs text-muted">No active trains are available. Create or activate a train first.</p>}
      </Card>

      {!trainId ? <Card><EmptyState title="Choose a train">Select an active train to view or define its route stops.</EmptyState></Card> : routeLoading ? <Card><Loading label="Loading route…" /></Card> : (
        <Card className="overflow-hidden">
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 px-5 py-4">
            <div><h2 className="font-display font-bold text-ink">Route stops</h2><p className="mt-1 text-xs text-muted">Use each station code once; distances and stop order must increase.</p></div>
            <Button variant="secondary" onClick={addStop}><Plus size={16} /> Add stop</Button>
          </div>
          {!stations.length && <Alert tone="warning" className="m-4">No active stations were returned. Add active stations before defining a route.</Alert>}
          <form onSubmit={saveRoute}>
            <div className="divide-y divide-slate-100">
              {stops.map((stop, index) => (
                <div key={`${trainId}-${index}`} className="grid gap-3 p-4 sm:grid-cols-2 lg:grid-cols-[1.4fr_0.7fr_0.9fr_0.9fr_0.8fr_auto] lg:items-end">
                  <SelectField label={`Stop ${index + 1} · station`} name={`station-${index}`} required value={stop.stationCode} onChange={(event) => updateStop(index, 'stationCode', event.target.value)}>
                    <option value="">Select station</option>
                    {stations.map((station) => <option key={station.id} value={station.stationCode}>{station.stationCode} · {station.stationName}</option>)}
                  </SelectField>
                  <TextField label="Day" name={`day-${index}`} type="number" min="1" required value={stop.dayNumber} onChange={(event) => updateStop(index, 'dayNumber', event.target.value)} />
                  <TextField label="Arrival" name={`arrival-${index}`} type="time" value={stop.arrivalTime} onChange={(event) => updateStop(index, 'arrivalTime', event.target.value)} hint={index === 0 ? 'Optional at origin' : undefined} />
                  <TextField label="Departure" name={`departure-${index}`} type="time" value={stop.departureTime} onChange={(event) => updateStop(index, 'departureTime', event.target.value)} hint={index === stops.length - 1 ? 'Optional at destination' : undefined} />
                  <TextField label="Distance (km)" name={`distance-${index}`} type="number" min="0" step="0.01" required value={stop.distanceFromSource} onChange={(event) => updateStop(index, 'distanceFromSource', event.target.value)} />
                  <Button type="button" variant="quiet" className="self-end" disabled={stops.length <= 2} aria-label={`Remove stop ${index + 1}`} onClick={() => removeStop(index)}><Trash2 size={16} /></Button>
                </div>
              ))}
            </div>
            <div className="flex flex-col gap-3 border-t border-slate-100 p-4 sm:flex-row sm:items-center sm:justify-between sm:px-5">
              <p className="max-w-xl text-xs leading-5 text-muted">Route edits may be rejected when train runs or historical bookings already depend on the route. Backend validation remains authoritative.</p>
              <Button type="submit" disabled={busy || stations.length === 0 || stops.length < 2}><Save size={16} /> {busy ? 'Saving route…' : routeExists ? 'Save route changes' : 'Create route'}</Button>
            </div>
          </form>
        </Card>
      )}
    </>
  )
}
