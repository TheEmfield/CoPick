package dto

type DiscoverRequest struct {
	Preferences    []GenrePreference `json:"preferences"`
	ExcludedGenres []string          `json:"excluded_genres"`
}

type GenrePreference struct {
	Genre string `json:"genre"`
	Count int    `json:"count"`
}

type DiscoverResponse struct {
	Movies     []Movie `json:"movies"`
	TotalCount int     `json:"total_count"`
}

type Movie struct {
	ID             int      `json:"id"`
	Title          string   `json:"title"`
	OriginalTitle  string   `json:"original_title"`
	PosterURL      string   `json:"poster_url"`
	BackdropURL    string   `json:"backdrop_url"`
	Rating         float64  `json:"rating"`
	ReleaseYear    int      `json:"release_year"`
	Genres         []string `json:"genres"`
	Overview       string   `json:"overview"`
	RequestedGenre string   `json:"requested_genre"`
}
