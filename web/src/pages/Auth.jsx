import { useState, useContext } from 'react'
import { AuthContext } from '../context/AuthContext'
import { authApi } from '../services/api'
import { useNavigate } from 'react-router-dom'

export const AuthPage = () => {
  const [isLogin, setIsLogin] = useState(true)
  const [nickname, setNickname] = useState('')
  const [password, setPassword] = useState('')
  const { login } = useContext(AuthContext)
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      const data = isLogin 
        ? await authApi.login(nickname, password)
        : await authApi.register(nickname, password)
      
      if (data.token) {
        login(data.profile, data.token)
        navigate('/groups')
      }
    } catch (error) {
      console.error('Auth error', error)
    }
  }

  return (
    <div className="container" style={{ maxWidth: '400px', marginTop: '100px' }}>
      <div className="card">
        <h2>{isLogin ? 'Вход' : 'Регистрация'}</h2>
        <form onSubmit={handleSubmit}>
          <input 
            className="input" 
            placeholder="Никнейм" 
            value={nickname} 
            onChange={e => setNickname(e.target.value)} 
            required 
          />
          <input 
            className="input" 
            type="password" 
            placeholder="Пароль" 
            value={password} 
            onChange={e => setPassword(e.target.value)} 
            required 
          />
          <button className="btn" style={{ width: '100%' }} type="submit">
            {isLogin ? 'Войти' : 'Зарегистрироваться'}
          </button>
        </form>
        <p style={{ textAlign: 'center', marginTop: '16px', cursor: 'pointer', color: 'var(--accent)' }} onClick={() => setIsLogin(!isLogin)}>
          {isLogin ? 'Нет аккаунта? Регистрация' : 'Уже есть аккаунт? Вход'}
        </p>
      </div>
    </div>
  )
}