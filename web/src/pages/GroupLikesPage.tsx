import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../api';
import type { SavedItem, ContentItem, GroupDetail } from '../types';
import { Button } from '../components/Button';
import { Spinner } from '../components/Spinner';
import { ContentCard } from '../components/ContentCard';
import { useToast } from '../components/Toast';
import { ArrowLeft, Heart, Layers } from 'lucide-react';

export function GroupLikesPage() {
  const { id } = useParams<{ id: string }>();
  const groupId = Number(id);
  const navigate = useNavigate();
  const toast = useToast();

  const [detail, setDetail] = useState<GroupDetail | null>(null);
  const [likes, setLikes] = useState<SavedItem[]>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [d, l] = await Promise.all([
        api.getGroupDetail(groupId),
        api.getGroupLikes(groupId),
      ]);
      setDetail(d);
      setLikes(l);
    } catch (err) {
      toast(err instanceof Error ? err.message : 'Группа не найдена', 'error');
      navigate('/groups');
    } finally {
      setLoading(false);
    }
  }, [groupId, navigate, toast]);

  useEffect(() => { load(); }, [load]);

  const likedIds = new Set(likes.map((s) => s.content.externalId));

  const toggleLike = async (content: ContentItem) => {
    try {
      const updated = await api.toggleGroupLike(groupId, content);
      setLikes(updated);
    } catch {
      toast('Не удалось обновить', 'error');
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
        onClick={() => navigate(`/groups/${groupId}`)}
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-muted hover:text-app transition-colors"
      >
        <ArrowLeft className="h-4 w-4" /> Назад в группу
      </button>

      <div className="mb-6 flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-red-50 text-red-500">
          <Heart className="h-5 w-5 fill-current" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-app">Понравившиеся</h1>
          <p className="text-sm text-muted">
            {detail.name} — {likes.length > 0 ? `${likes.length} фильмов отмечено` : 'пока пусто'}
          </p>
        </div>
      </div>

      {likes.length > 0 ? (
        <div className="space-y-3">
          {likes.map((item) => (
            <ContentCard
              key={item.content.externalId}
              item={{ content: item.content, score: 0 }}
              liked={likedIds.has(item.content.externalId)}
              onToggleLike={toggleLike}
            />
          ))}
        </div>
      ) : (
        <div className="flex flex-col items-center justify-center rounded-2xl border-2 border-dashed border-app bg-surface/50 py-16 text-center">
          <Layers className="h-10 w-10 text-muted mb-3" />
          <p className="text-muted">Пока никто не отметил понравившиеся фильмы</p>
          <p className="mt-1 text-xs text-muted">Нажмите «Нравится» на карточке фильма на доске подборок</p>
          <Button
            variant="secondary"
            className="mt-4"
            icon={<ArrowLeft className="h-4 w-4" />}
            onClick={() => navigate(`/groups/${groupId}`)}
          >
            Вернуться к доске
          </Button>
        </div>
      )}
    </div>
  );
}
