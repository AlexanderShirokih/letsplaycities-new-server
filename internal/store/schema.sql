CREATE TABLE users (
    id           INTEGER PRIMARY KEY,
    name         TEXT NOT NULL,
    created_at   INTEGER NOT NULL,
    last_seen_at INTEGER NOT NULL,
    avatar_hash  TEXT,
    fcm_token    TEXT,
    role         INTEGER NOT NULL DEFAULT 1 -- 0 banned, 1 regular, 2 admin
);

CREATE TABLE identities (
    provider TEXT NOT NULL, -- native|google|vk|ok|fb
    uid      TEXT NOT NULL, -- native: HMAC
    user_id  INTEGER NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (provider, uid)
);

CREATE TABLE sessions (
    token_hash   BLOB PRIMARY KEY,
    user_id      INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at   INTEGER NOT NULL,
    last_used_at INTEGER NOT NULL
);

CREATE TABLE friendships (
    sender_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    accepted    INTEGER NOT NULL,
    created_at  INTEGER NOT NULL,
    PRIMARY KEY (sender_id, receiver_id)
);
CREATE INDEX friendships_receiver ON friendships(receiver_id);

CREATE TABLE blocks (
    blocker_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (blocker_id, blocked_id)
);

CREATE TABLE games (
    id           INTEGER PRIMARY KEY,
    starter_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invited_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    started_at   INTEGER NOT NULL,
    duration_sec INTEGER NOT NULL,
    words        INTEGER NOT NULL
);
CREATE INDEX games_starter ON games(starter_id, started_at);
CREATE INDEX games_invited ON games(invited_id, started_at);

CREATE TABLE avatars (
    user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    mime    TEXT NOT NULL,
    data    BLOB NOT NULL
);

CREATE TABLE city_edits (
    id          INTEGER PRIMARY KEY,
    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    old_country INTEGER,
    new_country INTEGER,
    old_name    TEXT,
    new_name    TEXT,
    reason      TEXT,
    verdict     TEXT,
    status      TEXT NOT NULL
);

CREATE TABLE forbidden_logins (
    value TEXT PRIMARY KEY
);

-- One row: hex(HMAC-SHA256(LPS_NATIVE_PEPPER, 'lps-pepper-check')).
CREATE TABLE meta (
    id           INTEGER PRIMARY KEY CHECK (id = 1),
    pepper_check TEXT NOT NULL
);
