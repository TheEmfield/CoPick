import { useNavigate } from 'react-router-dom';
import { Button } from '../components/Button';
import {
  Users, Sparkles, Sliders, Film, ArrowRight, Layers, Heart, Bookmark,
} from 'lucide-react';

export function AboutPage() {
  const navigate = useNavigate();

  const features = [
    {
      icon: Users,
      title: 'Группы',
      text: 'Создавайте группы для друзей, семьи или коллег. Приглашайте по коду, управляйте участниками.',
    },
    {
      icon: Sparkles,
      title: 'Умные подборки',
      text: 'Заказывайте подборку фильмов, сериалов или игр под настроение всей компании.',
    },
    {
      icon: Sliders,
      title: 'Предпочтения',
      text: 'Настраивайте отношение к жанрам — от «не нравится» до «обожаю». Алгоритм учтёт вкусы всех участников.',
    },
    {
      icon: Layers,
      title: 'Доска контента',
      text: 'Подборка сохраняется на доске группы. Заходите в любой момент и продолжайте выбирать.',
    },
    {
      icon: Bookmark,
      title: 'Смотреть позже',
      text: 'Сохраняйте заинтересовавший контент в личные списки и возвращайтесь к нему позже.',
    },
    {
      icon: Heart,
      title: 'Темы оформления',
      text: 'Зимняя, летняя, ночная или лесная — выбирайте атмосферу под настроение.',
    },
  ];

  return (
    <div className="animate-fade-in">
      <div className="relative overflow-hidden rounded-3xl bg-surface border border-app p-8 sm:p-12 shadow-card">
        <div className="absolute -top-20 -right-20 h-64 w-64 rounded-full bg-primary/10 blur-3xl" />
        <div className="absolute -bottom-20 -left-20 h-64 w-64 rounded-full bg-accent/10 blur-3xl" />
        <div className="relative max-w-2xl">
          <div className="mb-5 inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-primary text-white shadow-soft">
            <Film className="h-7 w-7" />
          </div>
          <h1 className="text-3xl sm:text-4xl font-bold text-app leading-tight">
            CoPick
          </h1>
          <p className="mt-3 text-lg text-muted leading-relaxed">
            Сервис для совместного подбора фильмов, сериалов и игр.
            Создавайте группы, настраивайте предпочтения и получайте
            подборки, которые нравятся всем.
          </p>
          <div className="mt-6 flex flex-wrap gap-3">
            <Button icon={<ArrowRight className="h-4 w-4" />} onClick={() => navigate('/groups')}>
              Перейти к группам
            </Button>
            <Button variant="secondary" onClick={() => navigate('/profile')}>
              Настроить профиль
            </Button>
          </div>
        </div>
      </div>

      <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {features.map((f) => {
          const Icon = f.icon;
          return (
            <div
              key={f.title}
              className="rounded-2xl bg-surface border border-app p-5 shadow-soft transition-all duration-300 hover:shadow-card hover:-translate-y-0.5"
            >
              <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-xl bg-primary-soft text-primary">
                <Icon className="h-5 w-5" />
              </div>
              <h3 className="font-semibold text-app">{f.title}</h3>
              <p className="mt-1.5 text-sm text-muted leading-relaxed">{f.text}</p>
            </div>
          );
        })}
      </div>

      <div className="mt-8 rounded-2xl bg-surface border border-app p-6 shadow-soft">
        <h2 className="text-lg font-bold text-app mb-4">Как это работает</h2>
        <div className="grid gap-4 sm:grid-cols-3">
          {[
            { step: '1', title: 'Создайте группу', text: 'Пригласите друзей по коду' },
            { step: '2', title: 'Настройте вкусы', text: 'Отметьте жанры в предпочтениях' },
            { step: '3', title: 'Закажите подборку', text: 'Выберите вайб и получите результат' },
          ].map((s) => (
            <div key={s.step} className="flex gap-3">
              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary text-white text-sm font-bold">
                {s.step}
              </div>
              <div>
                <p className="font-medium text-app">{s.title}</p>
                <p className="text-sm text-muted">{s.text}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
