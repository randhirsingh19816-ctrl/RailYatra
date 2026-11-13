import { useEffect, useState } from 'react'
import { Pencil, Plus, Search, Train, X } from 'lucide-react'
import { Alert, Button, Card, EmptyState, Loading, PageHeading, Pagination, SelectField, StatusBadge, TextField } from '../components/Common'
import api, { errorMessage, responseData } from '../lib/api'

const TRAIN_TYPES = ['EXPRESS', 'SUPERFAST', 'PASSENGER', 'MAIL', 'RAJDHANI', 'SHATABDI', 'DURONTO', 'OTHER']
const EMPTY_FORM = { trainNumber: '', trainName: '', trainType: 'EXPRESS' }

export default function AdminTrainPage() {
  const [data, setData] = useState(null)
  const [page, setPage] = useState(0)
  const [sortBy, setSortBy] = useState('trainName')
  const [direction, setDirection] = useState('asc')
  const [activeFilter, setActiveFilter] = useState('')
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
    const params = activeKeyword
      ? { keyword: activeKeyword, page, size: 10, sortBy, direction }
      : { page, size: 10, sortBy, direction, ...(activeFilter ? { active: activeFilter === 'true' } : {}) }
    const url = activeKeyword ? '/api/admin/trains/search' : '/api/admin/trains'
    api.get(url, { params })
      .then((envelope) => { if (active) setData(responseData(envelope)) })
      .catch((requestError) => { if (active) setError(errorMessage(requestError)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [page, sortBy, direction, activeFilter, activeKeyword, reload])

  function startEdit(train) {
    setEditingId(train.id)
    setForm({ trainNumber: train.trainNumber, trainName: train.trainName, trainType: train.trainType })
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
        await api.put(`/api/admin/trains/${editingId}`, form)
        setNotice('Train updated.')
      } else {
        await api.post('/api/admin/trains', form)
        setNotice('Train created.')
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

  async function deactivate(train) {
    if (!window.confirm(`Deactivate train ${train.trainNumber} — ${train.trainName}?`)) return
    setError('')
    setNotice('')
    try {
      await api.patch(`/api/admin/trains/${train.id}/deactivate`)
      setNotice(`${train.trainNumber} deactivated.`)
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
      <PageHeading eyebrow="Admin console" title="Trains" description="Manage train identity and operating status. Route, schedule, coach and seat relationships remain separate." />
      {error && <Alert className="mb-4">{error}</Alert>}
      {notice && <Alert tone="success" className="mb-4">{notice}</Alert>}
      <Card className="mb-6 p-5 sm:p-6">
        <div className="mb-5 flex items-center gap-3">
          <span className="grid h-10 w-10 place-items-center rounded-xl bg-brand-50 text-brand-700"><Train size={20} /></span>
          <div><h2 className="font-display text-lg font-bold text-ink">{editingId ? 'Edit train' : 'Add a train'}</h2><p className="text-sm text-muted">Train number, name and type only.</p></div>
        </div>
        <form onSubmit={submit}>
          <div className="grid gap-4 sm:grid-cols-3">
            <TextField label="Train number" name="trainNumber" required maxLength={20} pattern="[A-Za-z0-9-]{1,20}" value={form.trainNumber} onChange={(event) => setForm((current) => ({ ...current, trainNumber: event.target.value.trim() }))} placeholder="12951" />
            <TextField label="Train name" name="trainName" required maxLength={120} value={form.trainName} onChange={(event) => setForm((current) => ({ ...current, trainName: event.target.value }))} placeholder="Mumbai Rajdhani" />
            <SelectField label="Train type" name="trainType" required value={form.trainType} onChange={(event) => setForm((current) => ({ ...current, trainType: event.target.value }))}>
              {TRAIN_TYPES.map((type) => <option key={type} value={type}>{type}</option>)}
            </SelectField>
          </div>
          <div className="mt-4 flex flex-wrap gap-2">
            <Button type="submit" disabled={busy}><Plus size={16} /> {busy ? 'Saving…' : editingId ? 'Save changes' : 'Create train'}</Button>
            {editingId && <Button variant="secondary" onClick={resetForm}><X size={16} /> Cancel edit</Button>}
          </div>
        </form>
      </Card>

      <Card className="overflow-hidden">
        <div className="flex flex-col gap-3 border-b border-slate-100 p-4 sm:flex-row sm:items-end sm:justify-between sm:p-5">
          <div><h2 className="font-display font-bold text-ink">Train directory</h2><p className="mt-1 text-xs text-muted">{data?.totalElements ?? '—'} total</p></div>
          <form onSubmit={search} className="flex flex-wrap gap-2">
            <TextField label="Search trains" name="trainKeyword" value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="Number or name" />
            <Button type="submit" variant="secondary" className="self-end"><Search size={16} /> Search</Button>
            {activeKeyword && <Button variant="quiet" className="self-end" onClick={() => { setLoading(true); setError(''); setKeyword(''); setActiveKeyword(''); setPage(0) }}>Clear</Button>}
          </form>
          {!activeKeyword && (
            <div className="flex flex-wrap gap-2">
              <label className="text-xs text-muted">Status
                <select className="ml-2 rounded-lg border border-slate-300 bg-white px-2 py-2 text-sm text-ink" value={activeFilter} onChange={(event) => { setLoading(true); setError(''); setActiveFilter(event.target.value); setPage(0) }}>
                  <option value="">All</option><option value="true">Active</option><option value="false">Inactive</option>
                </select>
              </label>
              <label className="text-xs text-muted">Sort
                <select className="ml-2 rounded-lg border border-slate-300 bg-white px-2 py-2 text-sm text-ink" value={sortBy} onChange={(event) => { setLoading(true); setError(''); setSortBy(event.target.value); setPage(0) }}>
                  <option value="trainName">Name</option><option value="trainNumber">Number</option><option value="trainType">Type</option>
                </select>
              </label>
              <button type="button" className="rounded-lg border border-slate-300 px-3 text-sm font-semibold text-ink" onClick={() => { setLoading(true); setError(''); setDirection((value) => value === 'asc' ? 'desc' : 'asc') }}>{direction.toUpperCase()}</button>
            </div>
          )}
        </div>
        {loading ? <Loading label="Loading trains…" /> : data?.trains?.length ? (
          <>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[700px] text-left text-sm">
                <thead className="bg-slate-50 text-xs uppercase tracking-wide text-muted"><tr><th className="px-5 py-3 font-semibold">Train</th><th className="px-5 py-3 font-semibold">Number</th><th className="px-5 py-3 font-semibold">Type</th><th className="px-5 py-3 font-semibold">Status</th><th className="px-5 py-3 text-right font-semibold">Actions</th></tr></thead>
                <tbody className="divide-y divide-slate-100">
                  {data.trains.map((train) => (
                    <tr key={train.id} className="hover:bg-slate-50/60">
                      <td className="px-5 py-4 font-semibold text-ink">{train.trainName}<p className="mt-0.5 text-xs font-normal text-muted">ID {train.id}</p></td>
                      <td className="px-5 py-4 font-mono text-xs font-bold text-brand-800">{train.trainNumber}</td>
                      <td className="px-5 py-4 text-muted">{train.trainType}</td>
                      <td className="px-5 py-4"><StatusBadge value={train.active ? 'ACTIVE' : 'INACTIVE'} /></td>
                      <td className="px-5 py-4"><div className="flex justify-end gap-2">
                        <Button variant="secondary" className="min-h-9 px-3 py-1.5" onClick={() => startEdit(train)}><Pencil size={14} /> Edit</Button>
                        {train.active && <Button variant="quiet" className="min-h-9 px-3 py-1.5" onClick={() => deactivate(train)}>Deactivate</Button>}
                      </div></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={data.page} totalPages={data.totalPages} onPageChange={changePage} />
          </>
        ) : <EmptyState title={activeKeyword ? 'No matching trains' : 'No trains yet'}>{activeKeyword ? 'Try another train name or number.' : 'Create a train to begin managing routes and schedules separately.'}</EmptyState>}
      </Card>
    </>
  )
}
