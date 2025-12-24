-- Дедупликация и уникальные индексы для стабильной работы под высокой нагрузкой.
-- Цели:
-- 1) points: гарантировать одну строку на (channel_id, user_id) и атомарные инкременты через ON CONFLICT
-- 2) ignores: не допускать дублей слов на канал (case-insensitive)
-- 3) reactions: не допускать дублей text_from на канал (case-insensitive)

-- ===== points: дедупликация (merge) =====
-- Суммируем point_count по (channel_id, user_id) и оставляем одну запись.
WITH dup AS (
    SELECT
        MIN(id) AS keep_id,
        channel_id,
        user_id,
        SUM(point_count) AS sum_points,
        COUNT(*) AS cnt
    FROM points
    GROUP BY channel_id, user_id
    HAVING COUNT(*) > 1
)
UPDATE points p
SET point_count = d.sum_points
FROM dup d
WHERE p.id = d.keep_id;

DELETE FROM points p
USING (
    SELECT
        id,
        MIN(id) OVER (PARTITION BY channel_id, user_id) AS keep_id
    FROM points
) x
WHERE p.id = x.id
  AND p.id <> x.keep_id;

-- ===== ignores: дедупликация (case-insensitive) =====
DELETE FROM ignores a
USING ignores b
WHERE a.id > b.id
  AND a.channel_id = b.channel_id
  AND lower(a.word) = lower(b.word);

-- ===== reactions: дедупликация (case-insensitive) =====
-- Оставляем самую "новую" запись (по created_on, затем по id) на (channel_id, lower(text_from)).
DELETE FROM reactions a
USING reactions b
WHERE a.channel_id = b.channel_id
  AND lower(a.text_from) = lower(b.text_from)
  AND (
      a.created_on < b.created_on
      OR (a.created_on = b.created_on AND a.id < b.id)
  );

-- ===== Уникальные индексы =====
CREATE UNIQUE INDEX IF NOT EXISTS ux_points_channel_user
    ON points(channel_id, user_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ignores_channel_word_ci
    ON ignores(channel_id, lower(word));

CREATE UNIQUE INDEX IF NOT EXISTS ux_reactions_channel_text_from_ci
    ON reactions(channel_id, lower(text_from));


