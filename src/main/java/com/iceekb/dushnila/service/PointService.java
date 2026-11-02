package com.iceekb.dushnila.service;

import com.iceekb.dushnila.jpa.repo.PointRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Начисление очков за ошибки.
 * Отдельная короткая транзакция — не держим транзакцию БД
 * во время сетевого запроса к спеллеру.
 */
@Service
@RequiredArgsConstructor
public class PointService {

    private final PointRepo pointRepo;

    @Transactional
    public void addPoint(Long channelId, Long userId) {
        pointRepo.incrementPoint(channelId, userId);
    }
}
