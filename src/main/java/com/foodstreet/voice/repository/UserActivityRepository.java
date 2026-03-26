package com.foodstreet.voice.repository;

import com.foodstreet.voice.dto.projection.AudioEngagementProjection;
import com.foodstreet.voice.dto.projection.DailySummaryProjection;
import com.foodstreet.voice.dto.projection.HourlyHeatmapProjection;
import com.foodstreet.voice.dto.projection.PoiRankingProjection;
import com.foodstreet.voice.dto.projection.SessionStatsProjection;
import com.foodstreet.voice.entity.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    List<UserActivity> findByDeviceId(String deviceId);

    List<UserActivity> findByFoodStallId(Long foodStallId);

//active-users
    @Query(value = """
            SELECT COUNT(DISTINCT ua.device_id)
            FROM user_activities ua
            WHERE ua.created_at >= NOW() - (:minutes * INTERVAL '1 minute')
            """, nativeQuery = true)
    Long countActiveUsersInLastMinutes(@Param("minutes") int minutes);

//poi-ranking
    @Query(value = """
        SELECT ua.food_stall_id AS stallId,
            fs.name AS stallName,
            COUNT(*) FILTER (WHERE ua.action_type IN ('PLAY_AUDIO', 'PLAY_AUDIO_MANUAL', 'PLAY_AUDIO_AUTO')) AS plays
        FROM user_activities ua
        JOIN food_stalls fs ON fs.id = ua.food_stall_id
        WHERE ua.created_at >= :fromTime
            AND ua.created_at < :toTime
        GROUP BY ua.food_stall_id, fs.name
        ORDER BY plays DESC, ua.food_stall_id ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<PoiRankingProjection> getPoiRanking(
        @Param("fromTime") LocalDateTime fromTime,
        @Param("toTime") LocalDateTime toTime,
        @Param("limit") int limit);

///hourly-heatmap
    @Query(value = """
        SELECT h.hour_of_day AS hourOfDay,
               COALESCE(x.visits, 0) AS visits
        FROM generate_series(0, 23) AS h(hour_of_day)
        LEFT JOIN (
            SELECT CAST(EXTRACT(HOUR FROM ua.created_at) AS int) AS hour_of_day,
                   COUNT(*) FILTER (WHERE ua.action_type = 'ENTER_REGION') AS visits
            FROM user_activities ua
            WHERE (:stallId IS NULL OR ua.food_stall_id = :stallId)
            GROUP BY CAST(EXTRACT(HOUR FROM ua.created_at) AS int)
        ) AS x ON x.hour_of_day = h.hour_of_day
        ORDER BY h.hour_of_day
        """, nativeQuery = true)
    List<HourlyHeatmapProjection> getHourlyHeatmap(@Param("stallId") Long stallId);

//audio-engagement
    @Query(value = """
        SELECT fs.id AS stallId,
               fs.name AS stallName,
               COUNT(*) FILTER (WHERE ua.action_type IN ('PLAY_AUDIO', 'PLAY_AUDIO_MANUAL', 'PLAY_AUDIO_AUTO')) AS plays
        FROM food_stalls fs
        LEFT JOIN user_activities ua ON ua.food_stall_id = fs.id
        WHERE (:stallId IS NULL OR fs.id = :stallId)
        GROUP BY fs.id, fs.name
        ORDER BY plays DESC, fs.id ASC
        """, nativeQuery = true)
    List<AudioEngagementProjection> getAudioEngagement(@Param("stallId") Long stallId);

//session-stats
    @Query(value = """
        WITH base AS (
            SELECT ua.device_id,
                   ua.food_stall_id,
                   ua.action_type,
                   ua.created_at,
                   DATE(ua.created_at) AS day,
                   CASE
                       WHEN LAG(ua.created_at) OVER (PARTITION BY ua.device_id ORDER BY ua.created_at) IS NULL THEN 1
                       WHEN ua.created_at - LAG(ua.created_at) OVER (PARTITION BY ua.device_id ORDER BY ua.created_at) > INTERVAL '30 minutes' THEN 1
                       ELSE 0
                   END AS is_new_session
            FROM user_activities ua
        ),
        sessionized AS (
            SELECT b.*,
                   SUM(b.is_new_session) OVER (PARTITION BY b.device_id ORDER BY b.created_at) AS session_no
            FROM base b
        ),
        per_session AS (
            SELECT day,
                   device_id,
                   session_no,
                   MIN(created_at) AS session_start,
                   MAX(created_at) AS session_end,
                   COUNT(DISTINCT CASE WHEN action_type = 'ENTER_REGION' THEN food_stall_id END) AS stalls_visited
            FROM sessionized
            GROUP BY day, device_id, session_no
        )
        SELECT day AS day,
               COUNT(*) AS sessions,
             ROUND(CAST(AVG(stalls_visited) AS numeric), 4) AS avgStallsPerSession,
             ROUND(CAST(AVG(EXTRACT(EPOCH FROM (session_end - session_start)) / 60.0) AS numeric), 4) AS avgSessionDurationMinutes
        FROM per_session
        GROUP BY day
        ORDER BY day
        """, nativeQuery = true)
    List<SessionStatsProjection> getSessionStatsByDay();

//daily-summary
    @Query(value = """
        SELECT DATE(ua.created_at) AS day,
               COUNT(DISTINCT ua.device_id) AS users,
               COUNT(*) FILTER (WHERE ua.action_type = 'ENTER_REGION') AS visits,
               COUNT(*) FILTER (WHERE ua.action_type IN ('PLAY_AUDIO', 'PLAY_AUDIO_MANUAL', 'PLAY_AUDIO_AUTO')) AS plays
        FROM user_activities ua
        WHERE ua.created_at >= :fromTime
          AND ua.created_at < :toTime
        GROUP BY DATE(ua.created_at)
        ORDER BY day
        """, nativeQuery = true)
    List<DailySummaryProjection> getDailySummary(
        @Param("fromTime") LocalDateTime fromTime,
        @Param("toTime") LocalDateTime toTime);
}
