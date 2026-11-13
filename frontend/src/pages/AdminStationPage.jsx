import { useEffect, useState } from 'react'
import { Building2, Pencil, Plus, Search, X } from 'lucide-react'
import { Alert, Button, Card, EmptyState, Loading, PageHeading, Pagination, StatusBadge, TextField } from '../components/Common'
import api, { errorMessage, responseData } from '../lib/api'

const EMPTY_FORM = { stationName: '', stationCode: '', city: '', state: '' }

export default function AdminStationPage() {
  const [data, setData] = useState(null)
  const [page, setPage] = useState(0)
  const [sortBy, setSortBy] = useState('stationName')
  const [direction, setDirection] = useState('asc')
  const [keyword, setKeyword] = useState('')
  const [activeKeyword, setActiveKeyword] = useState('')
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [reload, setReload] = useState(0)

  useEffect(() => {
    let active = true
    const request = activeKeyword
      ? api.get('/api/admin/stations/search', { params: { keyword: activeKeyword } })
      : api.get('/api/admin/stations', { params: { page, size: 10, sortBy, direction } })
    request
      .then((envelope) => {
        if (!active) return
        const result = responseData(envelope)
        setData(activeKeyword ? { stations: result || [], page: 0, totalPages: 0, totalElements: result?.length || 0 } : result)
      })
      .catch((requestError) => { if (active) setError(errorMessage(requestError)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [page, sortBy, direction, activeKeyword, reload])

  function startEdit(station) {
    setEditingId(station.id)
    setForm({ stationName: station.stationName, stationCode: station.stationCode, city: station.city, state: station.state })
    setNotice('')
    setError('')
  }

  function resetForm() {
    setEditingId(null)
    setForm(EMPTY_FORM)
  }

  function changePage(nextPage) {
    setLoading(true)
    setError('')
    setPage(nextPage)
  }

  async function submit(event) {
    event.preventDefault()
    setError('')
    setNotice('')
    setBusy(true)
    try {
      if (editingId) {
        await api.put(`/api/admin/stations/${editingId}`, form)
        setNotice('Station updated.')
      } else {
        await api.post('/api/admin/stations', form)
        setNotice('Station created.')
      }
      resetForm()
      setLoading(true)
      setReload((value) => value + 1)
    } catch (requestError) {
      setError(errorMessage(requestError))
    } finally {
      setBusy(false)
    }
  }

  async function deactivate(station) {
    if (!window.confirm(`Deactivate ${station.stationName} (${station.stationCode})?`)) return
    setError('')
    setNotice('')
    try {
      await api.patch(`/api/admin/stations/${station.id}/deactivate`)
      setNotice(`${station.stationCode} deactivated.`)
      setLoading(true)
      setReload((value) => value + 1)
    } catch (requestError) {
      setError(errorMessage(requestError))
    }
  }

  function search(event) {
    event.preventDefault()
    setLoading(true)
    setError('')
    setPage(0)
    setActiveKeyword(keyword.trim())
    setReload((value) => value + 1)
  }

  return (
    <>
      <PageHeading eyebrow="Admin console" title="Stations" description="Create, update, search and deactivate stations without removing historical route references." />
      {error && <Alert className="mb-4">{error}</Alert>}
      {notice && <Alert tone="success" className="mb-4">{notice}</Alert>}

      <Card className="mb-6 p-5 sm:p-6">
        <div className="mb-5 flex items-center gap-3">
          <span className="grid h-10 w-10 place-items-center rounded-xl bg-brand-50 text-brand-700"><Building2 size={20} /></span>
          <div><h2 className="font-display text-lg font-bold text-ink">{editingId ? 'Edit station' : 'Add a station'}</h2><p className="text-sm text-muted">Station codes are normalized by the backend.</p></div>
        </div>
        <form onSubmit={submit}>
          <div className="grid gap-4 sm:grid-cols-2">
            <TextField label="Station name" name="stationName" required maxLength={120} value={form.stationName} onChange={(event) => setForm((current) => ({ ...current, stationName: event.target.value }))} placeholder="New Delhi" />
            <TextField label="Station code" name="stationCode" required pattern="[A-Za-z0-9]{2,10}" maxLength={10} value={form.stationCode} onChange={(event) => setForm((current) => ({ ...current, stationCode: event.target.value.toUpperCase() }))} placeholder="NDLS" />
            <TextField label="City" name="city" required maxLength={100} value={form.city} onChange={(event) => setForm((current) => ({ ...current, city: event.target.value }))} placeholder="New Delhi" />
            <TextField label="State" name="state" required maxLength={100} value={form.state} onChange={(event) => setForm((current) => ({ ...current, state: event.target.value }))} placeholder="Delhi" />
          </div>
          <div className="mt-4 flex flex-wrap gap-2">
            <Button type="submit" disabled={busy}><Plus size={16} /> {busy ? 'Saving…' : editingId ? 'Save changes' : 'Create station'}</Button>
            {editingId && <Button variant="secondary" onClick={resetForm}><X size={16} /> Cancel edit</Button>}
          </div>
        </form>
      </Card>

      <Card className="overflow-hidden">
        <div className="flex flex-col gap-3 border-b border-slate-100 p-4 sm:flex-row sm:items-end sm:justify-between sm:p-5">
          <div><h2 className="font-display font-bold text-ink">Station directory</h2><p className="mt-1 text-xs text-muted">{data?.totalElements ?? '—'} total</p></div>
          <form onSubmit={search} className="flex flex-wrap gap-2">
            <TextField label="Search stations" name="stationKeyword" value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="Name, code or city" />
            <Button type="submit" variant="secondary" className="self-end"><Search size={16} /> Search</Button>
            {activeKeyword && <Button variant="quiet" className="self-end" onClick={() => { setLoading(true); setError(''); setKeyword(''); setActiveKeyword(''); setPage(0) }}>Clear</Button>}
          </form>
          {!activeKeyword && (
            <div className="flex gap-2">
              <label className="text-xs text-muted">Sort
                <select className="ml-2 rounded-lg border border-slate-300 bg-white px-2 py-2 text-sm text-ink" value={sortBy} onChange={(event) => { setLoading(true); setError(''); setSortBy(event.target.value); setPage(0) }}>
                  <option value="stationName">Name</option><option value="stationCode">Code</option><option value="city">City</option>
                </select>
              </label>
              <button type="button" className="rounded-lg border border-slate-300 px-3 text-sm font-semibold text-ink" onClick={() => { setLoading(true); setError(''); setDirection((value) => value === 'asc' ? 'desc' : 'asc') }}>{direction.toUpperCase()}</button>
            </div>
          )}
        </div>
        {loading ? <Loading label="Loading stations…" /> : data?.stations?.length ? (
          <>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[700px] text-left text-sm">
                <thead className="bg-slate-50 text-xs uppercase tracking-wide text-muted"><tr><th className="px-5 py-3 font-semibold">Station</th><th className="px-5 py-3 font-semibold">Code</th><th className="px-5 py-3 font-semibold">City / state</th><th className="px-5 py-3 font-semibold">Status</th><th className="px-5 py-3 text-right font-semibold">Actions</th></tr></thead>
                <tbody className="divide-y divide-slate-100">
                  {data.stations.map((station) => (
                    <tr key={station.id} className="hover:bg-slate-50/60">
                      <td className="px-5 py-4 font-semibold text-ink">{station.stationName}<p className="mt-0.5 text-xs font-normal text-muted">ID {station.id}</p></td>
                      <td className="px-5 py-4 font-mono text-xs font-bold text-brand-800">{station.stationCode}</td>
                      <td className="px-5 py-4 text-muted">{station.city}, {station.state}</td>
                      <td className="px-5 py-4"><StatusBadge value={station.active ? 'ACTIVE' : 'INACTIVE'} /></td>
                      <td className="px-5 py-4"><div className="flex justify-end gap-2">
                        <Button variant="secondary" className="min-h-9 px-3 py-1.5" onClick={() => startEdit(station)}><Pencil size={14} /> Edit</Button>
                        {station.active && <Button variant="quiet" className="min-h-9 px-3 py-1.5" onClick={() => deactivate(station)}>Deactivate</Button>}
                      </div></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {!activeKeyword && <Pagination page={data.page} totalPages={data.totalPages} onPageChange={changePage} />}
          </>
        ) : <EmptyState title={activeKeyword ? 'No matching stations' : 'No stations yet'}>{activeKeyword ? 'Try another station name, code or city.' : 'Create a station to start building the route network.'}</EmptyState>}
      </Card>
    </>
  )
}
