import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../api';
import type { GroupDetail, RecommendationItem, SavedItem, ContentItem, Genre } from '../types';
import { Button } from '../components/Button';
import { Modal } from '../components/Modal';
import { Spinner } from '../components/Spinner';
import { ContentCard } from '../components/ContentCard';
import { useToast } from '../components/Toast';
import { useAuth } from '../auth';
import {
  ArrowLeft, Copy, RefreshCw, Crown, UserMinus, LogOut, Trash2,
  Sparkles, Users, Film, Tv, Gamepad2, ChevronDown, Layers, Heart,
} from 'lucide-react';

const TYPE_ICONS: Record<string, typeof Film> = {
  movie: Film,
  series: Tv,
  game: Gamepad2,
};

export function GroupDetailPage() {
  const { id } = useParams<{ id: string }>();
  const groupId = Number(id);
  const navigate = useNavigate();
  const { profile } = useAuth();
  const toast = useToast();

  const [detail, setDetail] = useState<GroupDetail | null>(null);
  const [recs, setRecs] = useState<RecommendationItem[]>([]);
  const [likes, setLikes] = useState<SavedItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [orderOpen, setOrderOpen] = useState(false);

  const isOwner = profile?.profileId === detail?.ownerId;

  const loadLikes = useCallback(async () => {
    try { setLikes(await api.getGroupLikes(groupId)); } catch { /* noop */ }
  }, [groupId]);

  const loadAll = useCallback(async () => {
    setLoading(true);
    try {
      const [d, r] = await Promise.all([
        api.getGroupDetail(groupId),
        api.getRecommendations(groupId, { limit: 20 }),
      ]);
      setDetail(d);
      setRecs(r.items);
      await loadLikes();
    } catch {
      toast('Группа не найдена', 'error');
      navigate('/groups');
    } finally {
      setLoading(false);
    }
  }, [groupId, navigate, toast, loadLikes]);

  useEffect(() => { loadAll(); }, [loadAll]);

  const likedIds = new Set(likes.map((s) => s.content.externalId));

  const toggleLike = async (content: ContentItem) => {
    try {
      const updated = await api.toggleGroupLike(groupId, content);
      setLikes(updated);
    } catch {
      toast('Не удалось обновить', 'error');
    }
  };

  const copyCode = () => {
    if (!detail) return;
    navigator.clipboard.writeText(detail.inviteCode);
    toast('Код скопирован', 'success');
  };

  const refreshCode = async () => {
    try {
      const code = await api.refreshInviteCode(groupId);
      setDetail((d) => (d ? { ...d, inviteCode: code } : d));
      toast('Код обновлён', 'success');
    } catch {
      toast('Не удалось обновить код', 'error');
    }
  };

  const kickMember = async (profileId: number) => {
    try {
      await api.kickMember(groupId, profileId);
      setDetail((d) => d ? { ...d, members: d.members.filter((m) => m.profileId !== profileId) } : d);
      toast('Участник исключён', 'success');
    } catch {
      toast('Не удалось исключить', 'error');
    }
  };

  const leaveGroup = async () => {
    try {
      await api.leaveGroup(groupId);
      toast('Вы покинули группу', 'info');
      navigate('/groups');
    } catch {
      toast('Не удалось покинуть', 'error');
    }
  };

  const deleteGroup = async () => {
    try {
      await api.deleteGroup(groupId);
      toast('Группа удалена', 'info');
      navigate('/groups');
    } catch {
      toast('Не удалось удалить', 'error');
    }
  };

  if (loading || !detail) {
    return (
      <div className="flex justify-center py-20"><Spinner className="text-primary" /></div>
    );
  }

  return (
    <div className="animate-fade-in">
      <button
        onClick={() => navigate('/groups')}
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-muted hover:text-app transition-colors"
      >
        <ArrowLeft className="h-4 w-4" /> Все группы
      </button>

      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-app">{detail.name}</h1>
          {detail.ageRestriction && (
            <span className="mt-1 inline-block rounded-md bg-red-50 text-red-600 px-2 py-0.5 text-xs font-medium">
              {detail.ageRestriction}+
            </span>
          )}
        </div>
        <div className="flex gap-2">
          {isOwner ? (
            <Button variant="danger" icon={<Trash2 className="h-4 w-4" />} onClick={deleteGroup}>
              Удалить группу
            </Button>
          ) : (
            <Button variant="secondary" icon={<LogOut className="h-4 w-4" />} onClick={leaveGroup}>
              Покинуть
            </Button>
          )}
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_280px]">
        <div>
          <div className="mb-5 flex items-center justify-between">
            <div>
              <h2 className="text-xl font-bold text-app">Доска подборок</h2>
              <p className="mt-0.5 text-sm text-muted">
                {recs.length > 0
                  ? 'Текущая подборка для вашей группы'
                  : 'Закажите подборку, чтобы наполнить доску'}
              </p>
            </div>
            <Button icon={<Sparkles className="h-4 w-4" />} onClick={() => setOrderOpen(true)}>
              Собрать подборку
            </Button>
          </div>

          {recs.length > 0 ? (
            <div className="space-y-3">
              {recs.map((item) => (
                <ContentCard
                  key={item.content.externalId}
                  item={item}
                  liked={likedIds.has(item.content.externalId)}
                  onToggleLike={toggleLike}
                />
              ))}
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center rounded-2xl border-2 border-dashed border-app bg-surface/50 py-16 text-center">
              <Layers className="h-10 w-10 text-muted mb-3" />
              <p className="text-muted">Доска пуста — соберите первую подборку</p>
            </div>
          )}
        </div>

        <div className="space-y-6">
          <InviteCard detail={detail} isOwner={isOwner} onCopy={copyCode} onRefresh={refreshCode} />
          <MembersCard
            detail={detail}
            currentProfileId={profile?.profileId}
            isOwner={isOwner}
            onKick={kickMember}
          />
          <Button variant="secondary" className="w-full" icon={<Heart className="h-4 w-4" />} onClick={() => navigate(`/groups/${groupId}/likes`)}>
            Понравившиеся
            {likes.length > 0 && (
              <span className="ml-1.5 rounded-full bg-red-500 text-white text-[10px] font-bold px-1.5 py-0.5">
                {likes.length}
              </span>
            )}
          </Button>
        </div>
      </div>

      <OrderModal
        open={orderOpen}
        onClose={() => setOrderOpen(false)}
        groupId={groupId}
        members={detail.members}
        onResults={(items) => { setRecs(items); setOrderOpen(false); toast('Подборка готова', 'success'); }}
      />
    </div>
  );
}

function InviteCard({
  detail,
  isOwner,
  onCopy,
  onRefresh,
}: {
  detail: GroupDetail;
  isOwner: boolean;
  onCopy: () => void;
  onRefresh: () => void;
}) {
  return (
    <div className="rounded-2xl bg-surface border border-app p-5 shadow-soft">
      <h3 className="mb-3 text-sm font-semibold text-app">Код приглашения</h3>
      <div className="rounded-xl bg-primary-soft px-4 py-3 text-center">
        <span className="text-xl font-mono font-bold tracking-[0.3em] text-primary">
          {detail.inviteCode}
        </span>
      </div>
      <div className="mt-3 flex gap-2">
        <Button variant="secondary" className="flex-1" icon={<Copy className="h-4 w-4" />} onClick={onCopy}>
          Копировать
        </Button>
        {isOwner && (
          <Button variant="secondary" icon={<RefreshCw className="h-4 w-4" />} onClick={onRefresh}>
            <span className="sr-only">Обновить</span>
          </Button>
        )}
      </div>
    </div>
  );
}

function MembersCard({
  detail,
  currentProfileId,
  isOwner,
  onKick,
}: {
  detail: GroupDetail;
  currentProfileId?: number;
  isOwner: boolean;
  onKick: (profileId: number) => void;
}) {
  return (
    <div className="rounded-2xl bg-surface border border-app p-5 shadow-soft">
      <div className="mb-3 flex items-center gap-2">
        <Users className="h-4 w-4 text-muted" />
        <h3 className="text-sm font-semibold text-app">Участники</h3>
        <span className="ml-auto text-xs text-muted">{detail.members.length}</span>
      </div>
      <div className="space-y-1">
        {detail.members.map((m) => (
          <div
            key={m.profileId}
            className="flex items-center gap-2.5 rounded-xl px-2.5 py-2 hover:bg-surface-2 transition-colors"
          >
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary-soft text-primary text-xs font-semibold">
              {m.nickname.slice(0, 2).toUpperCase()}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-app truncate">{m.nickname}</p>
              {m.role === 'owner' && (
                <span className="inline-flex items-center gap-1 text-[10px] text-primary">
                  <Crown className="h-2.5 w-2.5" /> Владелец
                </span>
              )}
            </div>
            {isOwner && m.profileId !== currentProfileId && (
              <button
                onClick={() => onKick(m.profileId)}
                className="rounded-lg p-1.5 text-muted hover:bg-red-50 hover:text-red-600 transition-colors"
                title="Исключить"
              >
                <UserMinus className="h-3.5 w-3.5" />
              </button>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

function OrderModal({
  open,
  onClose,
  groupId,
  members,
  onResults,
}: {
  open: boolean;
  onClose: () => void;
  groupId: number;
  members: GroupDetail['members'];
  onResults: (items: RecommendationItem[]) => void;
}) {
  const [types, setTypes] = useState<Set<string>>(new Set(['movie', 'series', 'game']));
  const [genres, setGenres] = useState<Genre[]>([]);
  const [selectedGenres, setSelectedGenres] = useState<Set<string>>(new Set());
  const [selectedMembers, setSelectedMembers] = useState<Set<number>>(new Set());
  const [loading, setLoading] = useState(false);
  const [advanced, setAdvanced] = useState(false);

  useEffect(() => {
    if (open && genres.length === 0) {
      api.getGenres().then(setGenres).catch(() => {});
    }
  }, [open, genres.length]);

  const toggleType = (t: string) => {
    setTypes((prev) => {
      const next = new Set(prev);
      next.has(t) ? next.delete(t) : next.add(t);
      return next;
    });
  };

  const toggleGenre = (name: string) => {
    setSelectedGenres((prev) => {
      const next = new Set(prev);
      next.has(name) ? next.delete(name) : next.add(name);
      return next;
    });
  };

  const toggleMember = (id: number) => {
    setSelectedMembers((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const submit = async () => {
    setLoading(true);
    try {
      const res = await api.getRecommendations(groupId, {
        types: Array.from(types),
        limit: 20,
        genreFilter: Array.from(selectedGenres),
        profileIds: selectedMembers.size > 0 ? Array.from(selectedMembers) : undefined,
      });
      onResults(res.items);
    } catch {
      /* noop */
    } finally {
      setLoading(false);
    }
  };

  const availableGenres = genres.filter((g) => {
    if (types.size === 3 || types.size === 0) return true;
    if (types.has('movie') && g.contentType === 1) return true;
    if (types.has('series') && g.contentType === 2) return true;
    if (types.has('game') && g.contentType === 3) return true;
    return false;
  });
  const genreNames = Array.from(new Set(availableGenres.map((g) => g.genreName))).sort();

  return (
    <Modal open={open} onClose={onClose} title="Собрать подборку" size="lg">
      <div className="space-y-5">
        <Section title="Тип контента">
          <div className="grid grid-cols-3 gap-2">
            {(['movie', 'series', 'game'] as const).map((t) => {
              const Icon = TYPE_ICONS[t];
              const labels = { movie: 'Фильмы', series: 'Сериалы', game: 'Игры' };
              return (
                <button
                  key={t}
                  onClick={() => toggleType(t)}
                  className={`flex flex-col items-center gap-2 rounded-xl border-2 px-3 py-3 transition-all ${
                    types.has(t)
                      ? 'border-primary bg-primary-soft text-primary'
                      : 'border-app bg-surface text-muted hover:border-primary/50'
                  }`}
                >
                  <Icon className="h-5 w-5" />
                  <span className="text-xs font-medium">{labels[t]}</span>
                </button>
              );
            })}
          </div>
        </Section>

        <Section title="Жанры для акцента">
          <p className="mb-2.5 text-xs text-muted">
            Выберите жанры, на которые люди сейчас настроены. Можно несколько или ни одного.
          </p>
          <div className="flex flex-wrap gap-2">
            {genreNames.map((name) => (
              <button
                key={name}
                onClick={() => toggleGenre(name)}
                className={`rounded-lg px-3 py-1.5 text-sm capitalize transition-all ${
                  selectedGenres.has(name)
                    ? 'bg-primary text-white shadow-soft'
                    : 'bg-surface-2 text-muted hover:text-app'
                }`}
              >
                {name}
              </button>
            ))}
          </div>
        </Section>

        <div>
          <button
            onClick={() => setAdvanced((v) => !v)}
            className="flex items-center gap-1.5 text-sm font-medium text-muted hover:text-app transition-colors"
          >
            <ChevronDown className={`h-4 w-4 transition-transform ${advanced ? 'rotate-180' : ''}`} />
            Дополнительно
          </button>
          {advanced && (
            <div className="mt-3 space-y-4 animate-fade-in">
              <Section title="Учитывать участников">
                <div className="flex flex-wrap gap-2">
                  <button
                    onClick={() => setSelectedMembers(new Set())}
                    className={`rounded-lg px-3 py-1.5 text-sm transition-all ${
                      selectedMembers.size === 0 ? 'bg-primary text-white' : 'bg-surface-2 text-muted hover:text-app'
                    }`}
                  >
                    Все участники
                  </button>
                  {members.map((m) => (
                    <button
                      key={m.profileId}
                      onClick={() => toggleMember(m.profileId)}
                      className={`rounded-lg px-3 py-1.5 text-sm transition-all ${
                        selectedMembers.has(m.profileId) ? 'bg-primary text-white' : 'bg-surface-2 text-muted hover:text-app'
                      }`}
                    >
                      {m.nickname}
                    </button>
                  ))}
                </div>
              </Section>
            </div>
          )}
        </div>

        <div className="flex justify-end gap-2 border-t border-app pt-4">
          <Button variant="secondary" onClick={onClose}>Отмена</Button>
          <Button icon={<Sparkles className="h-4 w-4" />} loading={loading} onClick={submit}>
            Подобрать
          </Button>
        </div>
      </div>
    </Modal>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div>
      <label className="mb-2 block text-sm font-semibold text-app">{title}</label>
      {children}
    </div>
  );
}
