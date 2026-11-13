import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { Building2, LayoutDashboard, Menu, Search, ShieldCheck, Ticket, Train, UserRound, X } from 'lucide-react'
import useAuth from '../auth/useAuth'

const CURRENT_YEAR = new Date().getFullYear()

const publicLinks = [
  { to: '/trains/search', label: 'Find a train', icon: Search },
  { to: '/pnr', label: 'PNR status', icon: Ticket },
]

function HeaderLink({ to, children, onClick }) {
  return (
    <NavLink
      to={to}
      onClick={onClick}
      className={({ isActive }) => `rounded-lg px-3 py-2 text-sm font-medium transition ${isActive ? 'bg-white/10 text-white' : 'text-white/75 hover:bg-white/10 hover:text-white'}`}
    >
      {children}
    </NavLink>
  )
}

export default function AppLayout() {
  const { user, isAuthenticated, logout } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const navigate = useNavigate()
  const closeMenu = () => setMenuOpen(false)

  function signOut() {
    logout()
    closeMenu()
    navigate('/')
  }

  return (
    <div className="min-h-screen bg-paper">
      <header className="sticky top-0 z-30 border-b border-white/10 bg-ink text-white shadow-lg shadow-slate-900/10">
        <div className="mx-auto flex h-[68px] max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          <Link to="/" className="flex items-center gap-2.5" onClick={closeMenu} aria-label="Trackline home">
            <span className="grid h-9 w-9 place-items-center rounded-xl bg-accent text-ink">
              <Train size={21} strokeWidth={2.3} aria-hidden="true" />
            </span>
            <span className="font-display text-lg font-bold tracking-tight">trackline<span className="text-accent">.</span></span>
          </Link>

          <nav className="hidden items-center gap-1 md:flex" aria-label="Main navigation">
            {publicLinks.map(({ to, label }) => <HeaderLink key={to} to={to}>{label}</HeaderLink>)}
            {isAuthenticated && <HeaderLink to="/dashboard">My journeys</HeaderLink>}
            {user?.role === 'ADMIN' && <HeaderLink to="/admin"><span className="inline-flex items-center gap-1.5"><ShieldCheck size={15} /> Admin</span></HeaderLink>}
          </nav>

          <div className="hidden items-center gap-3 md:flex">
            {isAuthenticated ? (
              <>
                <Link to="/profile" className="flex items-center gap-2 text-sm text-white/80 hover:text-white">
                  <UserRound size={17} aria-hidden="true" /> {user?.fullName?.split(' ')[0] || 'Account'}
                </Link>
                <button onClick={signOut} className="rounded-lg border border-white/20 px-3 py-2 text-sm font-semibold text-white/85 transition hover:border-white/50 hover:text-white">
                  Sign out
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="rounded-lg px-3 py-2 text-sm font-semibold text-white/85 hover:text-white">Sign in</Link>
                <Link to="/register" className="rounded-lg bg-accent px-4 py-2.5 text-sm font-bold text-ink transition hover:bg-amber-300">Create account</Link>
              </>
            )}
          </div>

          <button
            type="button"
            className="grid h-10 w-10 place-items-center rounded-lg border border-white/15 text-white md:hidden"
            aria-label={menuOpen ? 'Close navigation menu' : 'Open navigation menu'}
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((open) => !open)}
          >
            {menuOpen ? <X size={21} /> : <Menu size={21} />}
          </button>
        </div>
        {menuOpen && (
          <nav className="border-t border-white/10 px-4 py-3 md:hidden" aria-label="Mobile navigation">
            <div className="mx-auto flex max-w-7xl flex-col gap-1">
              {publicLinks.map(({ to, label }) => <HeaderLink key={to} to={to} onClick={closeMenu}>{label}</HeaderLink>)}
              {isAuthenticated && <HeaderLink to="/dashboard" onClick={closeMenu}>My journeys</HeaderLink>}
              {user?.role === 'ADMIN' && <HeaderLink to="/admin" onClick={closeMenu}>Admin console</HeaderLink>}
              <div className="mt-2 flex gap-2 border-t border-white/10 pt-3">
                {isAuthenticated ? (
                  <>
                    <Link to="/profile" onClick={closeMenu} className="flex-1 rounded-lg px-3 py-2 text-sm text-white/80">Profile</Link>
                    <button onClick={signOut} className="flex-1 rounded-lg bg-white/10 px-3 py-2 text-sm font-semibold">Sign out</button>
                  </>
                ) : (
                  <>
                    <Link to="/login" onClick={closeMenu} className="flex-1 rounded-lg px-3 py-2 text-sm text-white/80">Sign in</Link>
                    <Link to="/register" onClick={closeMenu} className="flex-1 rounded-lg bg-accent px-3 py-2 text-center text-sm font-bold text-ink">Join now</Link>
                  </>
                )}
              </div>
            </div>
          </nav>
        )}
      </header>

      <main className="min-h-[calc(100vh-150px)]">
        <Outlet />
      </main>

      <footer className="border-t border-slate-200 bg-white">
        <div className="mx-auto flex max-w-7xl flex-col gap-2 px-4 py-6 text-xs text-muted sm:flex-row sm:items-center sm:justify-between sm:px-6 lg:px-8">
          <p>© {CURRENT_YEAR} Trackline. Your journey, made simpler.</p>
          <p>Train details and availability are provided by the reservation service.</p>
        </div>
      </footer>
    </div>
  )
}

const adminLinks = [
  { to: '/admin', label: 'Overview', icon: LayoutDashboard, end: true },
  { to: '/admin/trains', label: 'Trains', icon: Train },
  { to: '/admin/stations', label: 'Stations', icon: Building2 },
  { to: '/admin/routes', label: 'Routes', icon: Ticket },
  { to: '/admin/schedules', label: 'Schedules', icon: Ticket },
  { to: '/admin/bookings', label: 'Bookings', icon: Ticket },
  { to: '/admin/users', label: 'Users', icon: UserRound },
]

export function AdminLayout() {
  return (
    <div className="mx-auto grid max-w-7xl gap-6 px-4 py-6 sm:px-6 lg:grid-cols-[220px_minmax(0,1fr)] lg:px-8 lg:py-9">
      <aside className="admin-scrollbar overflow-x-auto lg:overflow-visible">
        <div className="flex gap-2 lg:sticky lg:top-24 lg:flex-col lg:gap-1">
          <p className="hidden px-3 pb-3 text-xs font-bold uppercase tracking-[0.18em] text-muted lg:block">Administration</p>
          {adminLinks.map(({ to, label, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) => `inline-flex shrink-0 items-center gap-2.5 rounded-xl px-3.5 py-2.5 text-sm font-semibold transition ${isActive ? 'bg-brand-700 text-white shadow-md shadow-brand-900/10' : 'bg-white text-muted hover:bg-brand-50 hover:text-brand-800 lg:bg-transparent'}`}
            >
              <Icon size={17} aria-hidden="true" /> {label}
            </NavLink>
          ))}
        </div>
      </aside>
      <section className="min-w-0"><Outlet /></section>
    </div>
  )
}
