CREATE TABLE profiles (
    profile_id integer primary key generated as identity always,
    nickname varchar(32) not null unique
)

CREATE TABLE groups (
    group_id integer primary key generated always as identity,
    name varchar(64) not null,
    invite_code varchar(16) unique
);

CREATE TABLE group_members (
    group_id integer references groups(group_id) on delete cascade,
    profile_id integer references profiles(profile_id) on delete cascade,
    primary key (group_id, profile_id)
);

create table genres(
    gerne_id integer primary key generated as identity always,
    genre_name varchar(30) not null unique
)

CREATE TABLE profile_preferences (
    preference_id integer primary key generated always as identity,
    profile_id integer references profiles(profile_id) on delete cascade,
    genre_id integer references genres(genre_id) on delete cascade,
    weight numeric(3,2) not null check (weight between -1.0 and 1.0),
    unique (profile_id, genre_id)
);
