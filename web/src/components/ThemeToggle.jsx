import { useContext } from 'react'
import { ThemeContext } from '../context/ThemeContext'

export const ThemeToggle = () => {
  const { theme, toggleTheme } = useContext(ThemeContext)
  return (
    <button className="btn" onClick={toggleTheme}>
      Тема: {theme === 'winter' ? 'Зимняя' : 'Летняя'}
    </button>
  )
}