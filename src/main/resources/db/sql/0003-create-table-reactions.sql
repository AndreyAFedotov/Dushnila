CREATE TABLE IF NOT EXISTS reactions
(
    id             BIGSERIAL     NOT NULL UNIQUE,
    channel_id     BIGINT        NOT NULL,
    text_from      varchar(300)  NOT NULL,
    text_to        varchar(300)  NOT NULL,
    user_id        BIGINT        NOT NULL,
    created_on     timestamp     NOT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE reactions IS 'Reactions';
COMMENT ON COLUMN reactions.id IS 'Reaction ID';
COMMENT ON COLUMN reactions.channel_id IS 'Channel ID';
COMMENT ON COLUMN reactions.text_from IS 'Replacement from';
COMMENT ON COLUMN reactions.text_to IS 'Replacement to';
COMMENT ON COLUMN reactions.user_id IS 'User ID';
COMMENT ON COLUMN reactions.created_on IS 'Creation time';

ALTER TABLE reactions ADD CONSTRAINT reactions_channel_id_key FOREIGN KEY (channel_id) REFERENCES channels (id);
ALTER TABLE reactions ADD CONSTRAINT reactions_user_id_key FOREIGN KEY (user_id) REFERENCES users (id);