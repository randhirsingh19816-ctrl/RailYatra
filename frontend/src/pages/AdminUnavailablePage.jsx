import { Alert, Card, EmptyState, PageHeading } from '../components/Common'

const MODULES = {
  schedules: {
    title: 'Schedule management',
    explanation: 'The current backend does not expose admin endpoints to create, update or list train schedules or runs.',
  },
  bookings: {
    title: 'All bookings',
    explanation: 'The current backend does not expose an admin endpoint to list or manage all users’ bookings.',
  },
  users: {
    title: 'User management',
    explanation: 'The current backend does not expose an admin endpoint to list or manage user accounts.',
  },
}

export default function AdminUnavailablePage({ module }) {
  const item = MODULES[module]
  return (
    <>
      <PageHeading eyebrow="Admin console" title={item.title} description="This section is intentionally unavailable until the backend exposes the required API." />
      <Alert tone="warning" className="mb-5">{item.explanation} No sample or fabricated data is displayed.</Alert>
      <Card><EmptyState title="Backend API required">This frontend will connect to this module once its backend contract is implemented and verified.</EmptyState></Card>
    </>
  )
}
