import { useState } from 'react'

export const VibeModal = ({ isOpen, onClose, onSubmit, genres }) => {
  const [selectedMood, setSelectedMood] = useState('')
  const [selectedGenres, setSelectedGenres] = useState([])

  if (!isOpen) return null

  const handleGenreToggle = (genre) => {
    setSelectedGenres(prev => 
      prev.includes(genre) ? prev.filter(g => g !== genre) : [...prev, genre]
    )
  }

  const handleSubmit = () => {
    onSubmit({ mood: selectedMood, genres: selectedGenres })
    onClose()
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <h2>Настроить вайб</h2>
        
        <h4>Настроение</h4>
        <select 
          className="input" 
          value={selectedMood} 
          onChange={e => setSelectedMood(e.target.value)}
        >
          <option value="">Стандартный алгоритм</option>
          <option value="chill">Расслабленный</option>
          <option value="intense">Напряженный</option>
          <option value="family">Семейный</option>
          <option value="brain">Интеллектуальный</option>
        </select>

        <h4>Акцент на жанры</h4>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', marginBottom: '16px' }}>
          {genres.map(genre => (
            <label key={genre.genreId} style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <input 
                type="checkbox" 
                checked={selectedGenres.includes(genre.genreName)}
                onChange={() => handleGenreToggle(genre.genreName)}
              />
              {genre.genreName}
            </label>
          ))}
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
          <button className="btn" style={{ backgroundColor: 'var(--text-secondary)' }} onClick={onClose}>Отмена</button>
          <button className="btn" onClick={handleSubmit}>Найти</button>
        </div>
      </div>
    </div>
  )
}