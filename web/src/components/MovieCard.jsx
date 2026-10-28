export const MovieCard = ({ movie, onSave, onLike }) => {
  return (
    <div className="card">
      <img 
        src={movie.content.posterUrl} 
        alt={movie.content.title} 
        style={{ width: '100%', maxHeight: '300px', objectFit: 'cover', borderRadius: '4px' }} 
      />
      <h3>{movie.content.title} ({movie.content.year})</h3>
      <p>Рейтинг: {movie.content.rating}</p>
      <p style={{ color: 'var(--text-secondary)' }}>{movie.content.overview}</p>
      <p>Жанры: {movie.content.genres.join(', ')}</p>
      <p>Причина: {movie.reason}</p>
      <div style={{ display: 'flex', gap: '8px', marginTop: '12px' }}>
        <button className="btn" onClick={() => onSave(movie)}>Смотреть позже</button>
        <button className="btn" onClick={() => onLike(movie)}>Нравится</button>
      </div>
    </div>
  )
}