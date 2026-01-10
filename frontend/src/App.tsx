import { Routes, Route, useParams } from 'react-router-dom'
import Home from './pages/Home'
import Schedule from './pages/Schedule'
import JobDetails from './pages/JobDetails'

function ScheduleWrapper() {
  const { id } = useParams<{ id: string }>();
  return <Schedule solutionId={id} />;
}

function JobDetailsWrapper() {
  const { id } = useParams<{ id: string }>();
  return <JobDetails jobId={id} />;
}

export default function App() {
  return (
    <>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/jobs/:id/schedule" element={<ScheduleWrapper />} />
        <Route path="/jobs/:id" element={<JobDetailsWrapper />} />
      </Routes>
    </>
  )
}