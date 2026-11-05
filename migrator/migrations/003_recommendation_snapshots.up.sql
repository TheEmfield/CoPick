CREATE TABLE recommendation_snapshots (
    snapshot_id integer primary key generated always as identity,
    group_id integer not null references groups(group_id) on delete cascade unique,
    created_at timestamptz default now()
);

CREATE TABLE snapshot_movies (
    snapshot_id integer not null references recommendation_snapshots(snapshot_id) on delete cascade,
    movie_position integer not null,

    title varchar(255) not null,
    original_title varchar(255),
    poster_url varchar(512),
    backdrop_url varchar(512),
    rating numeric(3, 1),
    release_year integer,
    overview text,

    primary key (snapshot_id, movie_position)
);

CREATE TABLE snapshot_movie_genres (
    snapshot_id integer not null,
    movie_position integer not null,
    genre_id integer not null references genres(genre_id) on delete cascade,
    primary key (snapshot_id, movie_position, genre_id),
    foreign key (snapshot_id, movie_position)
        references snapshot_movies(snapshot_id, movie_position)
        on delete cascade
);


CREATE INDEX idx_snapshot_movies_snapshot_id ON snapshot_movies(snapshot_id);
CREATE INDEX idx_snapshot_movie_genres_snapshot_id ON snapshot_movie_genres(snapshot_id, movie_position);