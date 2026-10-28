import { Routes, Route, Navigate } from 'react-router-dom'
import { useContext } from 'react'
import { AuthContext } from './context/AuthContext'
import AuthPage from './pages/Auth'
import ProfilePage from './pages/Profile'
import GroupsPage from './pages/Groups'
import GroupDetailPage from './pages/GroupDetail'

const ProtectedRoute = ({ children }) => {
  const { user } = useContext(AuthContext)
  if (!user) return <Navigate to="/auth" replace />
  return children
}

function App() {
  return (
    <Routes>
      <Route path="/auth" element={<AuthPage />} />
      <Route path="/" element={<Navigate to="/groups" replace />} />
      <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
      <Route path="/groups" element={<ProtectedRoute><GroupsPage /></ProtectedRoute>} />
      <Route path="/groups/:id" element={<ProtectedRoute><GroupDetailPage /></ProtectedRoute>} />
    </Routes>
  )
}

export default App