import { BrowserRouter, Link, Navigate, Route, Routes } from 'react-router-dom'
import RouteGuard from './auth/RouteGuard'
import AuthProvider from './auth/AuthProvider'
import AppLayout, { AdminLayout } from './components/AppLayout'
import {
  BookingDetailsPage,
  BookingHistoryPage,
  BookingStartPage,
  DashboardPage,
  PnrLookupPage,
  ProfilePage,
} from './pages/BookingPages'
import { LoginPage, RegisterPage } from './pages/AuthPages'
import AdminOverviewPage from './pages/AdminOverviewPage'
import AdminRoutesPage from './pages/AdminRoutesPage'
import AdminStationPage from './pages/AdminStationPage'
import AdminTrainPage from './pages/AdminTrainPage'
import AdminUnavailablePage from './pages/AdminUnavailablePage'
import { HomePage, TrainDetailsPage, TrainSearchPage } from './pages/TrainPages'

function NotFoundPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-20 text-center">
      <p className="text-sm font-bold uppercase tracking-widest text-brand-700">404 · Not found</p>
      <h1 className="mt-3 font-display text-3xl font-bold text-ink">That page has left the station.</h1>
      <p className="mt-3 text-sm text-muted">The address may be incorrect or the page may have moved.</p>
      <Link to="/" className="mt-6 inline-flex min-h-11 items-center rounded-xl bg-brand-700 px-5 py-3 text-sm font-bold text-white hover:bg-brand-900">Return home</Link>
    </div>
  )
}

function AdminOnly({ children }) {
  return <RouteGuard role="ADMIN">{children}</RouteGuard>
}

function SignedIn({ children }) {
  return <RouteGuard>{children}</RouteGuard>
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route element={<AppLayout />}>
            <Route index element={<HomePage />} />
            <Route path="login" element={<LoginPage />} />
            <Route path="register" element={<RegisterPage />} />
            <Route path="trains/search" element={<TrainSearchPage />} />
            <Route path="trains/:trainId" element={<TrainDetailsPage />} />
            <Route path="pnr" element={<PnrLookupPage />} />

            <Route path="dashboard" element={<SignedIn><DashboardPage /></SignedIn>} />
            <Route path="profile" element={<SignedIn><ProfilePage /></SignedIn>} />
            <Route path="bookings" element={<SignedIn><BookingHistoryPage /></SignedIn>} />
            <Route path="bookings/:pnr" element={<SignedIn><BookingDetailsPage /></SignedIn>} />
            <Route path="booking/start" element={<SignedIn><BookingStartPage /></SignedIn>} />

            <Route path="admin" element={<AdminOnly><AdminLayout /></AdminOnly>}>
              <Route index element={<AdminOverviewPage />} />
              <Route path="trains" element={<AdminTrainPage />} />
              <Route path="stations" element={<AdminStationPage />} />
              <Route path="routes" element={<AdminRoutesPage />} />
              <Route path="schedules" element={<AdminUnavailablePage module="schedules" />} />
              <Route path="bookings" element={<AdminUnavailablePage module="bookings" />} />
              <Route path="users" element={<AdminUnavailablePage module="users" />} />
            </Route>
            <Route path="*" element={<NotFoundPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
