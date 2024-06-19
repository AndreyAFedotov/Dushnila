CREATE TABLE IF NOT EXISTS users
(
    id             BIGSERIAL     NOT NULL UNIQUE,
    tg_id          BIGINT        NOT NULL,
    nick_name      varchar(300)  NOT NULL,
    first_message  timestamp     NOT NULL,
    last_message   timestamp     NOT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE users IS 'Users';
COMMENT ON COLUMN users.id IS 'User ID';
COMMENT ON COLUMN users.tg_id IS 'ID of the Telegram user';
COMMENT ON COLUMN users.nick_name IS 'Nickname of the Telegram user';
COMMENT ON COLUMN users.first_message IS 'First message of user';
COMMENT ON COLUMN users.last_message IS 'Last message of user';