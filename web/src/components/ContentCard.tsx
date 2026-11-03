import type { ContentItem } from '../types';
import { Star, Heart } from 'lucide-react';

interface Props {
  item: {
    content: ContentItem;
    score: number;
  };
  liked: boolean;
  onToggleLike: (content: ContentItem) => void;
}

export function ContentCard({ item, liked, onToggleLike }: Props) {
  return (
    <div className="group flex overflow-hidden rounded-2xl bg-surface border border-app shadow-soft transition-all duration-300 hover:shadow-card hover:-translate-y-0.5">
      <div className="relative w-32 sm:w-36 shrink-0 overflow-hidden bg-surface-2">
        <img
          src={item.content.posterUrl}
          alt={item.content.title}
          loading="lazy"
          className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
        />
      </div>

      <div className="flex flex-1 flex-col p-4 min-w-0">
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0">
            <h3 className="font-semibold text-app text-sm leading-snug line-clamp-1">
              {item.content.title}
            </h3>
            <span className="text-xs text-muted">{item.content.year}</span>
          </div>
          <span className="shrink-0 inline-flex items-center gap-1 rounded-md bg-primary-soft text-primary text-xs font-bold px-2 py-0.5">
            <Star className="h-3 w-3 fill-current" />
            {item.content.rating.toFixed(1)}
          </span>
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

        <p className="mt-2.5 text-xs text-muted leading-relaxed line-clamp-5 flex-1">
          {item.content.overview}
        </p>

        <div className="mt-3 flex justify-end">
          <button
            onClick={() => onToggleLike(item.content)}
            className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium transition-all ${
              liked
                ? 'bg-red-500 text-white shadow-soft'
                : 'bg-surface-2 text-muted hover:text-app'
            }`}
          >
            <Heart className={`h-3.5 w-3.5 ${liked ? 'fill-current' : ''}`} />
            {liked ? 'Понравилось' : 'Нравится'}
          </button>
        </div>
      </div>
    </div>
  );
}
