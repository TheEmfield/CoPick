import { useState, useContext, useEffect } from 'react'
import { AuthContext } from '../context/AuthContext'
import { authApi, preferencesApi } from '../services/api'
import { ThemeToggle } from '../components/ThemeToggle'

export const ProfilePage = () => {
  const { user, logout } = useContext(AuthContext)
  const [nickname, setNickname] = useState(user?.nickname || '')
  const [preferences, setPreferences] = useState([])

  useEffect(() => {
    loadPreferences()
  }, [])

  const loadPreferences = async () => {
    const data = await preferencesApi.getAll()
    setPreferences(data)
  }

  const handleUpdateProfile = async () => {
    await authApi.updateMe({ nickname })
    alert('Профиль обновлен')
  }

  return (
    <div className="container">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <h1>Профиль</h1>
        <div style={{ display: 'flex', gap: '12px' }}>
          <ThemeToggle />
          <button className="btn btn-danger" onClick={logout}>Выйти</button>
        </div>
      </div>

      <div className="card">
        <h3>Настройки аккаунта</h3>
        <input className="input" value={nickname} onChange={e => setNickname(e.target.value)} placeholder="Никнейм" />
        <input className="input" type="password" placeholder="Новый пароль (опционально)" />
        <button className="btn" onClick={handleUpdateProfile}>Сохранить</button>
      </div>

      <div className="card">
        <h3>Мои предпочтения</h3>
        {preferences.map(pref => (
          <div key={pref.genreId} style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--border)' }}>
            <span>{pref.genreName} (Вес: {pref.weight})</span>
            <button className="btn btn-danger" style={{ padding: '4px 8px' }} onClick={async () => {
              await preferencesApi.delete(pref.genreId)
              loadPreferences()
            }}>Удалить</button>
          </div>
        ))}
      </div>
    </div>
  )
}