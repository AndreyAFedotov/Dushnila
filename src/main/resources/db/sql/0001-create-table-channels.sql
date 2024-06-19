CREATE TABLE IF NOT EXISTS channels
(
    id             BIGSERIAL     NOT NULL UNIQUE,
    tg_id          BIGINT        NOT NULL,
    chat_name      varchar(300)  NOT NULL,
    first_message  timestamp     NOT NULL,
    last_message   timestamp     NOT NULL,
    message_count  BIGINT        NOT NULL,
    approved       char(1)       NOT NULL,
    PRIMARY KEY (id)
    );

COMMENT ON TABLE channels IS 'Channels';
COMMENT ON COLUMN channels.id IS 'Channel ID';
COMMENT ON COLUMN channels.tg_id IS 'ID of the Telegram channel';
COMMENT ON COLUMN channels.chat_name IS 'Name of the Telegram channel';
COMMENT ON COLUMN channels.first_message IS 'First message read in the channel';
COMMENT ON COLUMN channels.last_message IS 'Last message read in the channel';
COMMENT ON COLUMN channels.message_count IS 'Number of messages on the channel';
COMMENT ON COLUMN channels.approved IS 'Channel is approved for operation
Enum:
A - Approved
R - Rejected
W - Waiting';