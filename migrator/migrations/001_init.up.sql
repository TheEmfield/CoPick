CREATE TABLE content_types (
    type_id integer primary key generated always as identity,
    content_name varchar(16) not null unique
);

CREATE TABLE profiles (
    profile_id integer primary key generated always as identity,
    nickname varchar(32) not null unique,
    password_hash varchar(255),
    created_at timestamptz default now()
);

CREATE TABLE groups (
    group_id integer primary key generated always as identity,
    name varchar(64) not null,
    invite_code varchar(16) unique,
    age_restriction smallint check (age_restriction between 0 and 21),
    created_at timestamptz default now()
);

CREATE TABLE group_members (
    group_id integer references groups(group_id) on delete cascade,
    profile_id integer references profiles(profile_id) on delete cascade,
    role varchar(16) not null default 'member',
    joined_at timestamptz default now(),
    primary key (group_id, profile_id)
);

CREATE TABLE genres (
    genre_id integer primary key generated always as identity,
    genre_name varchar(30) not null,
    content_type integer not null references content_types(type_id) on delete cascade,
    unique (genre_name, content_type)
);

CREATE TABLE profile_preferences (
    profile_id integer references profiles(profile_id) on delete cascade,
    genre_id integer references genres(genre_id) on delete cascade,
    weight numeric(3,2) not null check (weight between -1 and 1),
    updated_at timestamptz default now(),
    primary key (profile_id, genre_id)
);

CREATE TABLE preference_history (
    history_id integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY, 
    profile_id integer NOT NULL,
    genre_id integer NOT NULL, 
    old_weight numeric(3,2) check (old_weight between -1 and 1), 
    new_weight numeric(3,2) check (new_weight between -1 and 1),
    changed_at timestamptz DEFAULT NOW()
);