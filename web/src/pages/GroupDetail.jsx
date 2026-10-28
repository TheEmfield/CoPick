import { useState, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { groupsApi, recommendationsApi } from '../services/api'
import { MovieCard, VibeModal } from '../components'

export const GroupDetailPage = () => {
  const { id } = useParams()
  const [group, setGroup] = useState(null)
  const [recommendations, setRecommendations] = useState([])
  const [isVibeModalOpen, setIsVibeModalOpen] = useState(false)
  
  const availableGenres = [
    { genreId: 1, genreName: 'action' },
    { genreId: 2, genreName: 'comedy' },
    { genreId: 3, genreName: 'drama' },
    { genreId: 4, genreName: 'scifi' }
  ]

  useEffect(() => {
    loadData()
  }, [id])

  const loadData = async () => {
    const groupData = await groupsApi.getById(id)
    setGroup(groupData)
    const recData = await recommendationsApi.getForGroup(id)
    setRecommendations(recData.items || [])
  }

  const handleRefreshInvite = async () => {
    const updatedGroup = await groupsApi.refreshInviteCode(id)
    setGroup(updatedGroup)
  }

  const handleVibeSubmit = async (vibeData) => {
    const params = {
      mood: vibeData.mood,
      genres: vibeData.genres.join(',')
    }
    const recData = await recommendationsApi.getForGroup(id, params)
    setRecommendations(recData.items || [])
  }

  if (!group) return <div className="container">Загрузка...</div>

  return (
    <div className="container">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <h1>{group.name}</h1>
          <p>Участников: {group.members.length}</p>
        </div>
        <div className="card" style={{ textAlign: 'right' }}>
          <p>Код приглашения: <strong>{group.inviteCode}</strong></p>
          <button className="btn" onClick={handleRefreshInvite}>Обновить код</button>
        </div>
      </div>

      <div className="card">
        <h3>Участники</h3>
        <ul>
          {group.members.map(member => (
            <li key={member.profileId}>{member.nickname} ({member.role})</li>
          ))}
        </ul>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <h2>Доска подборок</h2>
        <button className="btn" onClick={() => setIsVibeModalOpen(true)}>Заказать подборку (Вайб)</button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '16px' }}>
        {recommendations.length > 0 ? (
          recommendations.map((rec, idx) => (
            <MovieCard 
              key={idx} 
              movie={rec} 
              onSave={(m) => console.log('Saved', m)} 
              onLike={(m) => console.log('Liked', m)} 
            />
          ))
        ) : (
          <div className="card" style={{ gridColumn: '1 / -1', textAlign: 'center', color: 'var(--text-secondary)' }}>
            Подборок пока нет. Нажмите "Заказать подборку", чтобы найти что-то новое.
          </div>
        )}
      </div>

      <VibeModal 
        isOpen={isVibeModalOpen} 
        onClose={() => setIsVibeModalOpen(false)} 
        onSubmit={handleVibeSubmit}
        genres={availableGenres}
      />
    </div>
  )
}