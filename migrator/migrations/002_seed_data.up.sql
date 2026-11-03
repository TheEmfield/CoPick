 INSERT INTO content_types (content_name)
VALUES ('movie')
ON CONFLICT (content_name) DO NOTHING;

INSERT INTO genres (genre_name, content_type)
SELECT 'Боевик', type_id FROM content_types WHERE content_name = 'movie' UNION ALL
SELECT 'Приключения', type_id FROM content_types WHERE content_name = 'movie' UNION ALL
SELECT 'Мультфильм', type_id FROM content_types WHERE content_name = 'movie' UNION ALL
SELECT 'Комедия', type_id FROM content_types WHERE content_name = 'movie' UNION ALL
SELECT 'Криминал', type_id FROM content_types WHERE content_name = 'movie' UNION ALL
SELECT 'Документальный', type_id FROM content_types WHERE content_name = 'movie' UNION ALL
SELECT 'Драма', type_id FROM content_types WHERE content_name = 'movie' UNION ALL
SELECT 'Семейный', type_id FROM content_types WHERE content_name = 'movie' UNION ALL
SELECT 'Фэнтези', type_id FROM content_types WHERE content_name = 'movie' UNION ALL
SELECT 'Исторический', type_id FROM content_types WHERE content_name = 'movie' UNION ALL
SELECT 'Ужасы', type_id FROM content_types WHERE content_name = 'movie' UNION ALL
SELECT 'Мьюзикл', type_id FROM content_types WHERE content_name = 'movie' UNION ALL
SELECT 'Детектив', type_id FROM content_types WHERE content_name = 'movie' UNION ALL
SELECT 'Мелодрама', type_id FROM content_types WHERE content_name = 'movie' UNION ALL
SELECT 'Фантастика', type_id FROM content_types WHERE content_name = 'movie' UNION ALL
SELECT 'Телевизионный фильм', type_id FROM content_types WHERE content_name = 'movie' UNION ALL
SELECT 'Триллер', type_id FROM content_types WHERE content_name = 'movie' UNION ALL
SELECT 'Военный', type_id FROM content_types WHERE content_name = 'movie' UNION ALL
SELECT 'Вестерн', type_id FROM content_types WHERE content_name = 'movie'
ON CONFLICT (genre_name, content_type) DO NOTHING;