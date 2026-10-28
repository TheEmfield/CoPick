import { useState, useEffect } from 'react'
import { groupsApi } from '../services/api'
import { useNavigate } from 'react-router-dom'

export const GroupsPage = () => {
  const [groups, setGroups] = useState([])
  const [newGroupName, setNewGroupName] = useState('')
  const [joinCode, setJoinCode] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    loadGroups()
  }, [])

  const loadGroups = async () => {
    const data = await groupsApi.getAll()
    setGroups(data)
  }

  const handleCreateGroup = async () => {
    await groupsApi.create({ name: newGroupName, ageRestriction: 18 })
    setNewGroupName('')
    loadGroups()
  }

  const handleJoinGroup = async () => {
    await groupsApi.join(joinCode)
    setJoinCode('')
    loadGroups()
  }

  return (
    <div className="container">
      <h1>Мои группы</h1>
      
      <div className="card">
        <h3>Создать новую группу</h3>
        <div style={{ display: 'flex', gap: '8px' }}>
          <input 
            className="input" 
            style={{ marginBottom: 0 }} 
            placeholder="Название группы" 
            value={newGroupName} 
            onChange={e => setNewGroupName(e.target.value)} 
          />
          <button className="btn" onClick={handleCreateGroup}>Создать</button>
        </div>
      </div>

      <div className="card">
        <h3>Вступить по коду</h3>
        <div style={{ display: 'flex', gap: '8px' }}>
          <input 
            className="input" 
            style={{ marginBottom: 0 }} 
            placeholder="Код приглашения" 
            value={joinCode} 
            onChange={e => setJoinCode(e.target.value)} 
          />
          <button className="btn" onClick={handleJoinGroup}>Вступить</button>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '16px' }}>
        {groups.map(group => (
          <div key={group.groupId} className="card" style={{ cursor: 'pointer' }} onClick={() => navigate(`/groups/${group.groupId}`)}>
            <h3>{group.name}</h3>
            <p>Участников: {group.memberCount}</p>
            <p>Роль: {group.role === 'owner' ? 'Владелец' : 'Участник'}</p>
          </div>
        ))}
      </div>
    </div>
  )
}