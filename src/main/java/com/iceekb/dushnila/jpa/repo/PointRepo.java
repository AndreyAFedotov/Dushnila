package com.iceekb.dushnila.jpa.repo;

import com.iceekb.dushnila.jpa.entity.Point;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PointRepo extends JpaRepository<Point, Long> {

    @Query("""
            SELECT p FROM Point p
            WHERE p.channel.id = :id
            ORDER BY p.pointCount DESC
            """)
    List<Point> findPointsForChannelId(Long id);

    @SuppressWarnings("UnusedReturnValue")
    @Modifying
    @Query(value = """
            INSERT INTO points(channel_id, user_id, point_count)
            VALUES (:channelId, :userId, 1)
            ON CONFLICT (channel_id, user_id)
            DO UPDATE SET point_count = points.point_count + 1
            """, nativeQuery = true)
    int incrementPoint(@Param("channelId") Long channelId, @Param("userId") Long userId);

    @SuppressWarnings("UnusedReturnValue")
    @Modifying
    @Query("DELETE FROM Point p WHERE p.channel.id = :channelId")
    int deleteAllByChannelId(Long channelId);
}
