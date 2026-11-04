import type {
  Genre,
  GroupDetail,
  ContentItem,
  RecommendationItem,
} from './types';

export const MOCK_GENRES: Genre[] = [
  { genreId: 1, genreName: 'action', contentType: 1 },
  { genreId: 2, genreName: 'comedy', contentType: 1 },
  { genreId: 3, genreName: 'drama', contentType: 1 },
  { genreId: 4, genreName: 'thriller', contentType: 1 },
  { genreId: 5, genreName: 'horror', contentType: 1 },
  { genreId: 6, genreName: 'romance', contentType: 1 },
  { genreId: 7, genreName: 'animation', contentType: 1 },
  { genreId: 8, genreName: 'family', contentType: 1 },
  { genreId: 9, genreName: 'documentary', contentType: 1 },
  { genreId: 10, genreName: 'mystery', contentType: 1 },
  { genreId: 11, genreName: 'sci-fi', contentType: 1 },
  { genreId: 12, genreName: 'fantasy', contentType: 1 },
  { genreId: 13, genreName: 'action', contentType: 2 },
  { genreId: 14, genreName: 'drama', contentType: 2 },
  { genreId: 15, genreName: 'comedy', contentType: 2 },
  { genreId: 16, genreName: 'rpg', contentType: 3 },
  { genreId: 17, genreName: 'action', contentType: 3 },
  { genreId: 18, genreName: 'indie', contentType: 3 },
];

const POSTER = (seed: string) =>
  `https://picsum.photos/seed/${encodeURIComponent(seed)}/300/450`;

export const MOCK_CONTENT: ContentItem[] = [
  { externalId: '550', type: 'movie', title: 'Бойцовский клуб', genres: ['drama', 'thriller'], year: 1999, rating: 8.8, posterUrl: POSTER('fight'), overview: 'Офисный клерк встречает загадочного незнакомца и они открывают подпольный клуб.' },
  { externalId: '238', type: 'movie', title: 'Крёстный отец', genres: ['drama'], year: 1972, rating: 9.2, posterUrl: POSTER('godfather'), overview: 'История итало-американской мафиозной семьи.' },
  { externalId: '680', type: 'movie', title: 'Пули над Лас-Вегасом', genres: ['comedy', 'drama'], year: 1995, rating: 7.8, posterUrl: POSTER('pulp'), overview: 'Несколько историй бандитов, боксёра и ядерщика.' },
  { externalId: '155', type: 'movie', title: 'Тёмный рыцарь', genres: ['action', 'thriller'], year: 2008, rating: 9.0, posterUrl: POSTER('dark'), overview: 'Бэтмен против Джокера в Готэме.' },
  { externalId: '13', type: 'movie', title: 'Форрест Гамп', genres: ['drama', 'comedy', 'romance'], year: 1994, rating: 8.8, posterUrl: POSTER('forrest'), overview: 'Жизнеописание человека с большим сердцем.' },
  { externalId: '278', type: 'movie', title: 'Побег из Шоушенка', genres: ['drama'], year: 1994, rating: 9.3, posterUrl: POSTER('shawshank'), overview: 'Заключённый надеется и планирует побег годами.' },
  { externalId: '558', type: 'movie', title: 'Город грехов', genres: ['action', 'thriller', 'horror'], year: 2005, rating: 8.1, posterUrl: POSTER('sin'), overview: 'Неоновый нуар в городе преступности.' },
  { externalId: '9370', type: 'movie', title: 'Зверополис', genres: ['animation', 'family', 'comedy'], year: 2016, rating: 8.0, posterUrl: POSTER('zoo'), overview: 'Лисица и заяц расследуют дело в городе животных.' },
  { externalId: '603', type: 'movie', title: 'Матрица', genres: ['action', 'sci-fi'], year: 1999, rating: 8.7, posterUrl: POSTER('matrix'), overview: 'Хакер узнаёт правду о реальности.' },
  { externalId: '604', type: 'movie', title: 'Властелин колец', genres: ['fantasy', 'action'], year: 2001, rating: 8.9, posterUrl: POSTER('lotr'), overview: 'Братство кольца отправляется в Мордор.' },
  { externalId: '122', type: 'movie', title: 'Властелин духов', genres: ['animation', 'family', 'fantasy'], year: 2001, rating: 8.4, posterUrl: POSTER('spirited'), overview: 'Девочка попадает в мир духов.' },
  { externalId: '129', type: 'movie', title: 'Подарок', genres: ['documentary'], year: 2020, rating: 7.9, posterUrl: POSTER('gift'), overview: 'Документальный портрет современной жизни.' },
  { externalId: '500', type: 'series', title: 'Во все тяжкие', genres: ['drama', 'thriller'], year: 2008, rating: 9.5, posterUrl: POSTER('breaking'), overview: 'Учитель химии становится наркобароном.' },
  { externalId: '501', type: 'series', title: 'Очень странные дела', genres: ['sci-fi', 'mystery'], year: 2016, rating: 8.7, posterUrl: POSTER('stranger'), overview: 'Дети расследуют паранормальное в городке.' },
  { externalId: '502', type: 'series', title: 'Офис', genres: ['comedy'], year: 2005, rating: 9.0, posterUrl: POSTER('office'), overview: 'Будни бумажной компании в документальном стиле.' },
  { externalId: '700', type: 'game', title: 'The Witcher 3', genres: ['rpg', 'action'], year: 2015, rating: 9.3, posterUrl: POSTER('witcher'), overview: 'Ведьмак ищет приёмную дочь в охваченной войной стране.' },
  { externalId: '701', type: 'game', title: 'Hades', genres: ['indie', 'action'], year: 2020, rating: 9.0, posterUrl: POSTER('hades'), overview: 'Сын Аида пытается сбежать из подземного царства.' },
  { externalId: '120', type: 'movie', title: 'Начало', genres: ['action', 'sci-fi', 'mystery'], year: 2010, rating: 8.8, posterUrl: POSTER('inception'), overview: 'Вор проникает в сны, чтобы внедрить идею.' },
  { externalId: '157336', type: 'movie', title: 'Интерстеллар', genres: ["Приключения","Драма","Фантастика"], year: 2014, rating: 8.493, posterUrl: "https://image.tmdb.org/t/p/w500/vReLRjDV9XPhiOSEW7QWow4DXwf.jpg", overview: "Когда засуха, пыльные бури и вымирание растений приводят человечество к продовольственному кризису, коллектив исследователей и учёных отправляется сквозь червоточину в путешествие, чтобы превзойти прежние ограничения для космических путешествий человека и найти планету с подходящими для человечества условиями." },
  { externalId: '124', type: 'movie', title: 'Душа', genres: ['animation', 'family', 'comedy'], year: 2020, rating: 8.1, posterUrl: POSTER('soul'), overview: 'Учитель музыки оказывается в мире душ до рождения.' },
  { externalId: '125', type: 'movie', title: 'Паразиты', genres: ['drama', 'thriller'], year: 2019, rating: 8.6, posterUrl: POSTER('parasite'), overview: 'Семья мошенников проникает в дом богатой семьи.' },
  { externalId: '126', type: 'movie', title: 'Джокер', genres: ['drama', 'thriller'], year: 2019, rating: 8.4, posterUrl: POSTER('joker'), overview: 'Комик-неудачник скатывается в безумие.' },
  { externalId: '127', type: 'movie', title: '1917', genres: ['drama', 'action'], year: 2019, rating: 8.3, posterUrl: POSTER('1917'), overview: 'Два солдата пересекают вражескую территорию с приказом.' },
  { externalId: '503', type: 'series', title: 'Чернобыль', genres: ['drama', 'documentary'], year: 2019, rating: 9.4, posterUrl: POSTER('chernobyl'), overview: 'Хроника атомной катастрофы и её последствий.' },
  { externalId: '504', type: 'series', title: 'Наследники', genres: ['drama', 'comedy'], year: 2018, rating: 8.9, posterUrl: POSTER('succession'), overview: 'Семья медиамагната борется за контроль над империей.' },
  { externalId: '505', type: 'series', title: 'Ведьмак', genres: ['fantasy', 'action'], year: 2019, rating: 8.2, posterUrl: POSTER('witcher-s'), overview: 'Охотник за чудовищами ищет своё место в мире.' },
  { externalId: '702', type: 'game', title: 'Elden Ring', genres: ['rpg', 'action'], year: 2022, rating: 9.6, posterUrl: POSTER('eldenring'), overview: 'Падший герой ищет осколки Элденского кольца.' },
  { externalId: '703', type: 'game', title: 'Stardew Valley', genres: ['indie', 'rpg'], year: 2016, rating: 9.2, posterUrl: POSTER('stardew'), overview: 'Унаследованная ферма становится новой жизнью.' },
];

export function buildRecommendations(opts: {
  types?: string[];
  limit?: number;
  genreFilter?: string[];
  profileIds?: number[];
}): RecommendationItem[] {
  let pool = MOCK_CONTENT;
  if (opts.types && opts.types.length > 0) {
    pool = pool.filter((c) => opts.types!.includes(c.type));
  }
  if (opts.genreFilter && opts.genreFilter.length > 0) {
    pool = pool.filter((c) =>
      c.genres.some((g) => opts.genreFilter!.includes(g))
    );
  }
  const reasons = [
    'Высокий рейтинг, нравится большинству',
    'Совпадает с предпочтениями участников',
    'Подходит под выбранный вайб',
    'Популярно в этой группе',
    'Нравится 3 из 4 участников',
  ];
  const limit = opts.limit ?? 10;
  return pool.slice(0, limit).map((content, i) => ({
    content,
    score: Math.round((9.5 - i * 0.3) * 10) / 10,
    reason: reasons[i % reasons.length],
  }));
}

export const MOCK_GROUP_DETAIL: Record<number, GroupDetail> = {
  1: {
    groupId: 1,
    name: 'Пятничный кинозал',
    inviteCode: 'A1B2C3D4',
    ageRestriction: 18,
    ownerId: 1,
    createdAt: '2026-10-21T12:00:00Z',
    members: [
      { profileId: 1, nickname: 'Alex', role: 'owner' },
      { profileId: 2, nickname: 'Masha', role: 'member' },
      { profileId: 3, nickname: 'Igor', role: 'member' },
      { profileId: 4, nickname: 'Lena', role: 'member' },
    ],
  },
  2: {
    groupId: 2,
    name: 'Семейный вечер',
    inviteCode: 'XYZ12345',
    ageRestriction: null,
    ownerId: 1,
    createdAt: '2026-10-15T18:30:00Z',
    members: [
      { profileId: 1, nickname: 'Alex', role: 'owner' },
      { profileId: 5, nickname: 'Mom', role: 'member' },
      { profileId: 6, nickname: 'Dad', role: 'member' },
    ],
  },
};

export function makeInviteCode(): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  return Array.from({ length: 8 }, () =>
    chars[Math.floor(Math.random() * chars.length)]
  ).join('');
}
