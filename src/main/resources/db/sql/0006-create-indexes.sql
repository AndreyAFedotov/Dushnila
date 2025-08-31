-- Индекс для реакций по каналу и исходному тексту
CREATE INDEX IF NOT EXISTS idx_reactions_channel_text_from 
ON reactions(channel_id, text_from);

-- Индекс для игнорируемых слов по каналу и слову
CREATE INDEX IF NOT EXISTS idx_ignores_channel_word 
ON ignores(channel_id, word);

-- Индекс для очков по каналу и пользователю
CREATE INDEX IF NOT EXISTS idx_points_channel_user 
ON points(channel_id, user_id);

-- Индекс для пользователей по Telegram ID
CREATE INDEX IF NOT EXISTS idx_users_tg_id 
ON users(tg_id);

-- Индекс для каналов по Telegram ID
CREATE INDEX IF NOT EXISTS idx_channels_tg_id 
ON channels(tg_id);

-- Индекс для каналов по статусу одобрения
CREATE INDEX IF NOT EXISTS idx_channels_approved 
ON channels(approved);

-- Индекс для очков по количеству (для сортировки статистики)
CREATE INDEX IF NOT EXISTS idx_points_count 
ON points(point_count DESC);


