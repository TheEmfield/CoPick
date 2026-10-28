import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api';
import type { Group } from '../types';
import { Button } from '../components/Button';
import { Modal } from '../components/Modal';
import { Spinner } from '../components/Spinner';
import { useToast } from '../components/Toast';
import { Users, Plus, LogIn, Crown, User as UserIcon, ArrowRight } from 'lucide-react';

export function GroupsPage() {
  const [groups, setGroups] = useState<Group[]>([]);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [joinOpen, setJoinOpen] = useState(false);
  const navigate = useNavigate();
  const toast = useToast();

  const load = async () => {
    setLoading(true);
    try {
      setGroups(await api.getGroups());
    } catch {
      toast('Не удалось загрузить группы', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-app">Мои группы</h1>
          <p className="mt-1 text-sm text-muted">
            Создайте группу или вступайте по коду приглашения
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" icon={<LogIn className="h-4 w-4" />} onClick={() => setJoinOpen(true)}>
            <span className="hidden sm:inline">Вступить</span>
          </Button>
          <Button icon={<Plus className="h-4 w-4" />} onClick={() => setCreateOpen(true)}>
            <span className="hidden sm:inline">Создать</span>
          </Button>
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner className="text-primary" /></div>
      ) : groups.length === 0 ? (
        <EmptyState onCreate={() => setCreateOpen(true)} onJoin={() => setJoinOpen(true)} />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {groups.map((g) => (
            <button
              key={g.groupId}
              onClick={() => navigate(`/groups/${g.groupId}`)}
              className="group relative overflow-hidden rounded-2xl bg-surface border border-app p-5 text-left shadow-soft transition-all duration-300 hover:shadow-card hover:-translate-y-0.5"
            >
              <div className="flex items-start justify-between">
                <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary-soft text-primary">
                  <Users className="h-5 w-5" />
                </div>
                {g.role === 'owner' ? (
                  <span className="inline-flex items-center gap-1 rounded-lg bg-primary-soft px-2 py-1 text-xs font-medium text-primary">
                    <Crown className="h-3 w-3" /> Владелец
                  </span>
                ) : (
                  <span className="inline-flex items-center gap-1 rounded-lg bg-surface-2 px-2 py-1 text-xs font-medium text-muted">
                    <UserIcon className="h-3 w-3" /> Участник
                  </span>
                )}
              </div>
              <h3 className="mt-4 font-semibold text-app">{g.name}</h3>
              <div className="mt-2 flex items-center gap-3 text-sm text-muted">
                <span>{g.memberCount} участников</span>
                <span className="font-mono text-xs">{g.inviteCode}</span>
              </div>
              <div className="mt-4 flex items-center gap-1 text-sm font-medium text-primary opacity-0 transition-opacity group-hover:opacity-100">
                Открыть <ArrowRight className="h-4 w-4" />
              </div>
            </button>
          ))}
        </div>
      )}

      <CreateGroupModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onCreated={(id) => { setCreateOpen(false); navigate(`/groups/${id}`); }}
      />
      <JoinGroupModal
        open={joinOpen}
        onClose={() => setJoinOpen(false)}
        onJoined={(id) => { setJoinOpen(false); navigate(`/groups/${id}`); }}
      />
    </div>
  );
}

function EmptyState({ onCreate, onJoin }: { onCreate: () => void; onJoin: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border-2 border-dashed border-app bg-surface/50 py-16 text-center">
      <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-primary-soft text-primary">
        <Users className="h-7 w-7" />
      </div>
      <h3 className="text-lg font-semibold text-app">Групп пока нет</h3>
      <p className="mt-1 max-w-xs text-sm text-muted">
        Создайте первую группу и пригласите друзей, чтобы подбирать контент вместе
      </p>
      <div className="mt-5 flex gap-2">
        <Button icon={<Plus className="h-4 w-4" />} onClick={onCreate}>Создать группу</Button>
        <Button variant="secondary" icon={<LogIn className="h-4 w-4" />} onClick={onJoin}>Вступить по коду</Button>
      </div>
    </div>
  );
}

function CreateGroupModal({
  open,
  onClose,
  onCreated,
}: {
  open: boolean;
  onClose: () => void;
  onCreated: (id: number) => void;
}) {
  const [name, setName] = useState('');
  const [age, setAge] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const toast = useToast();

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) { setError('Введите название'); return; }
    setLoading(true);
    setError('');
    try {
      const detail = await api.createGroup(name.trim(), age ? Number(age) : null);
      toast('Группа создана', 'success');
      onCreated(detail.groupId);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Новая группа" size="sm">
      <form onSubmit={submit} className="space-y-4">
        <div>
          <label className="mb-1.5 block text-sm font-medium text-app">Название</label>
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Пятничный кинозал"
            autoFocus
            className="w-full rounded-xl border border-app bg-surface px-4 py-2.5 text-sm text-app focus:outline-none focus:ring-2 focus:ring-app focus:border-primary"
          />
        </div>
        <div>
          <label className="mb-1.5 block text-sm font-medium text-app">Возрастное ограничение</label>
          <input
            type="number"
            value={age}
            onChange={(e) => setAge(e.target.value)}
            placeholder="Без ограничения"
            min={0}
            max={21}
            className="w-full rounded-xl border border-app bg-surface px-4 py-2.5 text-sm text-app focus:outline-none focus:ring-2 focus:ring-app focus:border-primary"
          />
        </div>
        {error && <p className="text-sm text-red-600">{error}</p>}
        <div className="flex justify-end gap-2 pt-2">
          <Button variant="secondary" type="button" onClick={onClose}>Отмена</Button>
          <Button type="submit" loading={loading}>Создать</Button>
        </div>
      </form>
    </Modal>
  );
}

function JoinGroupModal({
  open,
  onClose,
  onJoined,
}: {
  open: boolean;
  onClose: () => void;
  onJoined: (id: number) => void;
}) {
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const toast = useToast();

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!code.trim()) { setError('Введите код'); return; }
    setLoading(true);
    setError('');
    try {
      const detail = await api.joinGroup(code.trim().toUpperCase());
      toast('Вы вступили в группу', 'success');
      onJoined(detail.groupId);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Вступить по коду" size="sm">
      <form onSubmit={submit} className="space-y-4">
        <div>
          <label className="mb-1.5 block text-sm font-medium text-app">Код приглашения</label>
          <input
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
            placeholder="A1B2C3D4"
            autoFocus
            maxLength={8}
            className="w-full rounded-xl border border-app bg-surface px-4 py-2.5 text-center text-lg font-mono tracking-widest text-app focus:outline-none focus:ring-2 focus:ring-app focus:border-primary"
          />
        </div>
        {error && <p className="text-sm text-red-600">{error}</p>}
        <div className="flex justify-end gap-2 pt-2">
          <Button variant="secondary" type="button" onClick={onClose}>Отмена</Button>
          <Button type="submit" loading={loading}>Вступить</Button>
        </div>
      </form>
    </Modal>
  );
}
