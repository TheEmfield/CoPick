# CoPick Connector Service

Сервис-агрегатор (BFF), предоставляющий унифицированный API для получения данных о фильмах из внешних источников. Берет на себя сложность работы со сторонними API, выполняет маппинг данных, фильтрацию и дедупликацию, возвращая нормализованный формат для основного бэкенда или фронтенда.

## Контракт API

### Эндпоинт получения подборки

- **Метод:** `POST`
- **Путь:** `/discover`
- **Content-Type:** `application/json`

### Структуры данных (DTO)

**DiscoverRequest** — тело входящего запроса.

```go
type DiscoverRequest struct {
    // Массив предпочтений пользователя. Каждый элемент определяет жанр и желаемое количество фильмов для него.
    Preferences []GenrePreference `json:"preferences"`
    
    // Массив жанров, которые необходимо исключить из итоговой выдачи.
    ExcludedGenres []string `json:"excluded_genres"`
}

type GenrePreference struct {
    // Ключ жанра (например, "action", "comedy", "scifi").
    Genre string `json:"genre"`
    
    // Количество фильмов, которое необходимо вернуть для данного жанра.
    Count int `json:"count"`
}

**DiscoverResponse** — тело успешного ответа.

type DiscoverResponse struct {
    // Массив нормализованных объектов фильмов.
    Movies []Movie `json:"movies"`
    
    // Общее количество возвращенных фильмов.
    TotalCount int `json:"total_count"`
}

type Movie struct {
    ID             int      `json:"id"`              // Уникальный идентификатор фильма
    Title          string   `json:"title"`           // Локализованное название
    OriginalTitle  string   `json:"original_title"`  // Оригинальное название
    PosterURL      string   `json:"poster_url"`      // Полный URL постера
    BackdropURL    string   `json:"backdrop_url"`    // Полный URL фонового изображения
    Rating         float64  `json:"rating"`          // Средний рейтинг
    ReleaseYear    int      `json:"release_year"`    // Год выпуска
    Genres         []string `json:"genres"`          // Массив локализованных названий жанров
    Overview       string   `json:"overview"`        // Краткое описание
    RequestedGenre string   `json:"requested_genre"` // Жанр, по которому был найден этот фильм
}

## Использование эндпоинта

Для получения данных основной бэкенд отправляет HTTP POST-запрос на /discover, передавая в теле JSON-структуру DiscoverRequest.
Коннектор самостоятельно обращается к внешнему провайдеру, преобразует относительные пути к изображениям в абсолютные URL, переводит числовые идентификаторы жанров в читаемый вид и возвращает единый агрегированный массив результатов.
В случае ошибки валидации входных данных коннектор возвращает статус 400 Bad Request. При сбоях на стороне внешнего провайдера возвращается 500 Internal Server Error.

## Архитектура и ключевые методы

Сервис построен по паттерну Фасад с четким разделением ответственности. Слой HTTP-обработчиков не имеет прямой зависимости от внешних API и работает исключительно через интерфейс service.Fetcher.
Инициализация

func New(cfg *config.Config, logger *slog.Logger) *Client

Создает главный фасад сервиса и инициализирует всех внутренних клиентов-провайдеров данных. Возвращает экземпляр, готовый к использованию.

## Слой бизнес-логики (Service)

func (c *Client) FetchMovies(ctx context.Context, req dto.DiscoverRequest) (*dto.DiscoverResponse, error)

Основной метод бизнес-логики, реализующий интерфейс service.Fetcher. Итерируется по массиву предпочтений запроса, для каждого жанра обращается к внешнему API с учетом исключений. Выполняет дедупликацию фильмов по ID, преобразует сырые данные (даты, ID жанров, пути к картинкам) в формат DTO и возвращает агрегированный ответ.

## Слой HTTP (Handlers)

func (api *ServiceAPI) GetMovies(ctx echo.Context) error

Обработчик входящего HTTP-запроса. Привязывает JSON-тело к структуре DiscoverRequest, проводит базовую валидацию (проверка на пустой массив предпочтений и корректность значения count), делегирует выполнение методу FetchMovies и маппит возвращаемые ошибки в соответствующие HTTP-статусы.