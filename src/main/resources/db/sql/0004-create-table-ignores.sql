CREATE TABLE IF NOT EXISTS ignores
(
    id             BIGSERIAL     NOT NULL UNIQUE,
    channel_id     BIGINT        NOT NULL,
    word           varchar(300)  NOT NULL,
    user_id        BIGINT        NOT NULL,
    created_on     timestamp     NOT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE ignores IS 'Ignores';
COMMENT ON COLUMN ignores.id IS 'Ignore ID';
COMMENT ON COLUMN ignores.channel_id IS 'Channel ID';
COMMENT ON COLUMN ignores.word IS 'Ignored word';
COMMENT ON COLUMN ignores.user_id IS 'User ID';
COMMENT ON COLUMN ignores.created_on IS 'Creation time';

ALTER TABLE ignores ADD CONSTRAINT ignores_channel_id_key FOREIGN KEY (channel_id) REFERENCES channels (id);
ALTER TABLE ignores ADD CONSTRAINT ignores_user_id_key FOREIGN KEY (user_id) REFERENCES users (id);