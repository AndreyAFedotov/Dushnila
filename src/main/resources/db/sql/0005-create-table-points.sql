CREATE TABLE IF NOT EXISTS points
(
    id             BIGSERIAL     NOT NULL UNIQUE,
    channel_id     BIGINT        NOT NULL,
    user_id        BIGINT        NOT NULL,
    point_count    BIGINT        NOT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE points IS 'Points';
COMMENT ON COLUMN points.id IS 'Point ID';
COMMENT ON COLUMN points.channel_id IS 'Channel ID';
COMMENT ON COLUMN points.user_id IS 'User ID';
COMMENT ON COLUMN points.point_count IS 'Count of user points on chanel';

ALTER TABLE points ADD CONSTRAINT points_channel_id_key FOREIGN KEY (channel_id) REFERENCES channels (id);
ALTER TABLE points ADD CONSTRAINT points_user_id_key FOREIGN KEY (user_id) REFERENCES users (id);