package com.iceekb.dushnila.jpa.repo;

import com.iceekb.dushnila.jpa.entity.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PointRepo extends JpaRepository<Point, Long> {

    @Query("""
            SELECT p FROM Point p
            WHERE p.channel.id = :id
            ORDER BY p.pointCount DESC
            """)
    List<Point> findPointsForChannelId(Long id);

    @Query("""
            SELECT p FROM Point p
            WHERE p.channel.id = :channelId
            AND p.user.id = :userId
            """)
    Point findPointsForChannelIdAndUserId(Long channelId, Long userId);

    @Query("SELECT EXISTS (SELECT 1 FROM Point p WHERE p.channel.id = :channelId AND p.user.id = :userId)")
    boolean existsByChatIdAndUserId(Long channelId, Long userId);
}
