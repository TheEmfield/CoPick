import type { ContentItem } from '../types';
import { Star, Bookmark, Heart, Check } from 'lucide-react';

interface Props {
  item: {
    content: ContentItem;
    score: number;
    reason: string;
  };
  savedWatchlist: boolean;
  savedLiked: boolean;
  onToggleSave: (content: ContentItem, list: 'watchlist' | 'liked') => void;
}

export function ContentCard({ item, savedWatchlist, savedLiked, onToggleSave }: Props) {
  return (
    <div className="group flex overflow-hidden rounded-2xl bg-surface border border-app shadow-soft transition-all duration-300 hover:shadow-card hover:-translate-y-0.5">
      <div className="relative w-32 sm:w-36 shrink-0 overflow-hidden bg-surface-2">
        <img
          src={item.content.posterUrl}
          alt={item.content.title}
          loading="lazy"
          className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
        />
        <div className="absolute top-2 left-2 flex items-center gap-1 rounded-lg bg-black/70 px-2 py-1 text-xs font-semibold text-white backdrop-blur-sm">
          <Star className="h-3 w-3 fill-yellow-400 text-yellow-400" />
          {item.content.rating.toFixed(1)}
        </div>
      </div>

      <div className="flex flex-1 flex-col p-4 min-w-0">
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0">
            <h3 className="font-semibold text-app text-sm leading-snug line-clamp-1">
              {item.content.title}
            </h3>
            <span className="text-xs text-muted">{item.content.year}</span>
          </div>
          {item.score > 0 && (
            <span className="shrink-0 rounded-md bg-primary-soft text-primary text-xs font-bold px-2 py-0.5">
              {item.score}
            </span>
          )}
        </div>

        <div className="mt-2 flex flex-wrap gap-1">
          {item.content.genres.slice(0, 4).map((g) => (
            <span
              key={g}
              className="rounded-md bg-surface-2 text-muted px-1.5 py-0.5 text-[10px] font-medium uppercase tracking-wide"
            >
              {g}
            </span>
          ))}
        </div>

        <p className="mt-2.5 text-xs text-muted leading-relaxed line-clamp-3 flex-1">
          {item.content.overview}
        </p>

        <div className="mt-3 flex items-center justify-between">
          <div className="flex items-center gap-1.5 text-[11px] text-muted">
            <Check className="h-3 w-3 text-primary" />
            <span className="line-clamp-1">{item.reason}</span>
          </div>
          <div className="flex gap-1.5 shrink-0">
            <button
              onClick={() => onToggleSave(item.content, 'watchlist')}
              className={`rounded-lg p-1.5 transition-all ${
                savedWatchlist
                  ? 'bg-primary text-white'
                  : 'bg-surface-2 text-muted hover:text-app'
              }`}
              title="Смотреть позже"
            >
              <Bookmark className={`h-3.5 w-3.5 ${savedWatchlist ? 'fill-current' : ''}`} />
            </button>
            <button
              onClick={() => onToggleSave(item.content, 'liked')}
              className={`rounded-lg p-1.5 transition-all ${
                savedLiked
                  ? 'bg-red-500 text-white'
                  : 'bg-surface-2 text-muted hover:text-app'
              }`}
              title="Понравившиеся"
            >
              <Heart className={`h-3.5 w-3.5 ${savedLiked ? 'fill-current' : ''}`} />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
