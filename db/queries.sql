-- KickStats 조회·집계 쿼리 (MySQL 8.0)
-- 아래 변수를 바꿔 실행합니다. 앱에서는 각 쿼리의 변수를 파라미터로 바꿔 사용합니다.
USE kickstats;
SET NAMES utf8mb4;
SET @season_id = 1;
SET @team_id = 1;
SET @player_id = 15;

-- ===== 4. 팀 순위표 =====

-- 4-1. 시즌 순위표 (승점 > 득실차 > 다득점)
-- 경기 전(점수 NULL) 경기는 제외하고, 아직 경기가 없는 참가 팀도 0으로 표시한다.
WITH result AS (
    SELECT home_team_id AS team_id, home_score AS gf, away_score AS ga
    FROM `match`
    WHERE season_id = @season_id AND home_score IS NOT NULL
    UNION ALL
    SELECT away_team_id, away_score, home_score
    FROM `match`
    WHERE season_id = @season_id AND home_score IS NOT NULL
),
standing AS (
    SELECT t.team_id,
           t.name AS team,
           COUNT(r.team_id) AS played,
           COALESCE(SUM(r.gf > r.ga), 0) AS wins,
           COALESCE(SUM(r.gf = r.ga), 0) AS draws,
           COALESCE(SUM(r.gf < r.ga), 0) AS losses,
           COALESCE(SUM(r.gf), 0) AS goals_for,
           COALESCE(SUM(r.ga), 0) AS goals_against,
           COALESCE(SUM(r.gf) - SUM(r.ga), 0) AS goal_diff,
           COALESCE(SUM(r.gf > r.ga) * 3 + SUM(r.gf = r.ga), 0) AS points
    FROM season_team st
    JOIN team t ON t.team_id = st.team_id
    LEFT JOIN result r ON r.team_id = st.team_id
    WHERE st.season_id = @season_id
    GROUP BY t.team_id, t.name
)
SELECT RANK() OVER (ORDER BY points DESC, goal_diff DESC, goals_for DESC) AS ranking,
       team, played, wins, draws, losses, goals_for, goals_against, goal_diff, points
FROM standing
ORDER BY ranking, team;

-- ===== 5. 선수 개인 순위 =====
-- 소속은 현재 계약(end_date IS NULL) 기준이며, 기록은 시즌 중 이적 전후를 합산한다.

-- 5-1. 득점 순위 (자책골 제외, 동률이면 PK 골이 적은 선수 우선)
SELECT *
FROM (
    SELECT RANK() OVER (ORDER BY COUNT(*) DESC, SUM(g.is_penalty) ASC) AS ranking,
           p.name AS player,
           t.name AS team,
           COUNT(*) AS goals,
           SUM(g.is_penalty) AS penalties
    FROM goal g
    JOIN `match` m ON m.match_id = g.match_id
    JOIN player p ON p.player_id = g.scorer_id
    LEFT JOIN contract c ON c.player_id = p.player_id AND c.end_date IS NULL
    LEFT JOIN team t ON t.team_id = c.team_id
    WHERE m.season_id = @season_id AND NOT g.is_own_goal
    GROUP BY p.player_id, p.name, t.name
) ranked
WHERE ranking <= 10
ORDER BY ranking, player;

-- 5-2. 도움 순위
SELECT *
FROM (
    SELECT RANK() OVER (ORDER BY COUNT(*) DESC) AS ranking,
           p.name AS player,
           t.name AS team,
           COUNT(*) AS assists
    FROM goal g
    JOIN `match` m ON m.match_id = g.match_id
    JOIN player p ON p.player_id = g.assist_player_id
    LEFT JOIN contract c ON c.player_id = p.player_id AND c.end_date IS NULL
    LEFT JOIN team t ON t.team_id = c.team_id
    WHERE m.season_id = @season_id
    GROUP BY p.player_id, p.name, t.name
) ranked
WHERE ranking <= 10
ORDER BY ranking, player;

-- 5-3. 공격포인트 순위 (득점 + 도움, 동률이면 득점이 많은 선수 우선)
WITH point AS (
    SELECT g.scorer_id AS player_id, 1 AS goal, 0 AS assist
    FROM goal g
    JOIN `match` m ON m.match_id = g.match_id
    WHERE m.season_id = @season_id AND NOT g.is_own_goal
    UNION ALL
    SELECT g.assist_player_id, 0, 1
    FROM goal g
    JOIN `match` m ON m.match_id = g.match_id
    WHERE m.season_id = @season_id AND g.assist_player_id IS NOT NULL
)
SELECT *
FROM (
    SELECT RANK() OVER (ORDER BY COUNT(*) DESC, SUM(pt.goal) DESC) AS ranking,
           p.name AS player,
           t.name AS team,
           COUNT(*) AS attack_points,
           SUM(pt.goal) AS goals,
           SUM(pt.assist) AS assists
    FROM point pt
    JOIN player p ON p.player_id = pt.player_id
    LEFT JOIN contract c ON c.player_id = p.player_id AND c.end_date IS NULL
    LEFT JOIN team t ON t.team_id = c.team_id
    GROUP BY p.player_id, p.name, t.name
) ranked
WHERE ranking <= 10
ORDER BY ranking, player;

-- 5-4. 경고 순위 (경고 수 > 퇴장 수)
SELECT *
FROM (
    SELECT RANK() OVER (ORDER BY SUM(cd.card_type = 'YELLOW') DESC,
                                 SUM(cd.card_type = 'RED') DESC) AS ranking,
           p.name AS player,
           t.name AS team,
           SUM(cd.card_type = 'YELLOW') AS yellows,
           SUM(cd.card_type = 'RED') AS reds
    FROM card cd
    JOIN `match` m ON m.match_id = cd.match_id
    JOIN player p ON p.player_id = cd.player_id
    LEFT JOIN contract c ON c.player_id = p.player_id AND c.end_date IS NULL
    LEFT JOIN team t ON t.team_id = c.team_id
    WHERE m.season_id = @season_id
    GROUP BY p.player_id, p.name, t.name
) ranked
WHERE ranking <= 10
ORDER BY ranking, player;

-- ===== 5. 선수별·팀별 기록 =====

-- 5-5. 선수의 시즌 기록 (소속 팀별로 나눠 표시)
SELECT p.name AS player,
       t.name AS team,
       COUNT(*) AS appearances,
       SUM(a.is_starter) AS starts,
       SUM(a.minutes_played) AS minutes,
       SUM((SELECT COUNT(*) FROM goal g
            WHERE g.match_id = a.match_id AND g.scorer_id = a.player_id AND NOT g.is_own_goal)) AS goals,
       SUM((SELECT COUNT(*) FROM goal g
            WHERE g.match_id = a.match_id AND g.assist_player_id = a.player_id)) AS assists,
       SUM((SELECT COUNT(*) FROM card cd
            WHERE cd.match_id = a.match_id AND cd.player_id = a.player_id AND cd.card_type = 'YELLOW')) AS yellows,
       SUM((SELECT COUNT(*) FROM card cd
            WHERE cd.match_id = a.match_id AND cd.player_id = a.player_id AND cd.card_type = 'RED')) AS reds
FROM appearance a
JOIN `match` m ON m.match_id = a.match_id
JOIN player p ON p.player_id = a.player_id
JOIN team t ON t.team_id = a.team_id
WHERE m.season_id = @season_id AND a.player_id = @player_id
GROUP BY p.player_id, p.name, t.team_id, t.name
ORDER BY MIN(m.match_date);

-- 5-6. 팀 소속 선수들의 시즌 기록 (해당 팀에서 뛴 경기만 집계)
SELECT p.name AS player,
       p.position,
       COUNT(*) AS appearances,
       SUM(a.is_starter) AS starts,
       SUM(a.minutes_played) AS minutes,
       SUM((SELECT COUNT(*) FROM goal g
            WHERE g.match_id = a.match_id AND g.scorer_id = a.player_id AND NOT g.is_own_goal)) AS goals,
       SUM((SELECT COUNT(*) FROM goal g
            WHERE g.match_id = a.match_id AND g.assist_player_id = a.player_id)) AS assists
FROM appearance a
JOIN `match` m ON m.match_id = a.match_id
JOIN player p ON p.player_id = a.player_id
WHERE m.season_id = @season_id AND a.team_id = @team_id
GROUP BY p.player_id, p.name, p.position
ORDER BY goals DESC, assists DESC, minutes DESC, player;

-- 5-7. 선수의 이적 이력
SELECT p.name AS player, t.name AS team, c.back_number, c.start_date, c.end_date
FROM contract c
JOIN player p ON p.player_id = c.player_id
JOIN team t ON t.team_id = c.team_id
WHERE c.player_id = @player_id
ORDER BY c.start_date;

-- ===== 6. 통계 분석 =====

-- 6-1. 시즌 전체 홈·원정 승률 비교
SELECT COUNT(*) AS played,
       SUM(home_score > away_score) AS home_wins,
       SUM(home_score = away_score) AS draws,
       SUM(home_score < away_score) AS away_wins,
       ROUND(AVG(home_score > away_score) * 100, 1) AS home_win_pct,
       ROUND(AVG(home_score = away_score) * 100, 1) AS draw_pct,
       ROUND(AVG(home_score < away_score) * 100, 1) AS away_win_pct
FROM `match`
WHERE season_id = @season_id AND home_score IS NOT NULL;

-- 6-2. 팀별 홈·원정 승률 비교
SELECT t.name AS team,
       SUM(m.home_team_id = t.team_id) AS home_played,
       SUM(m.home_team_id = t.team_id AND m.home_score > m.away_score) AS home_wins,
       ROUND(100 * SUM(m.home_team_id = t.team_id AND m.home_score > m.away_score)
             / NULLIF(SUM(m.home_team_id = t.team_id), 0), 1) AS home_win_pct,
       SUM(m.away_team_id = t.team_id) AS away_played,
       SUM(m.away_team_id = t.team_id AND m.away_score > m.home_score) AS away_wins,
       ROUND(100 * SUM(m.away_team_id = t.team_id AND m.away_score > m.home_score)
             / NULLIF(SUM(m.away_team_id = t.team_id), 0), 1) AS away_win_pct
FROM team t
JOIN `match` m ON t.team_id IN (m.home_team_id, m.away_team_id)
WHERE m.season_id = @season_id AND m.home_score IS NOT NULL
GROUP BY t.team_id, t.name
ORDER BY t.name;

-- 6-3. 팀별 경기당 평균 득점·실점
SELECT t.name AS team,
       COUNT(*) AS played,
       ROUND(AVG(IF(m.home_team_id = t.team_id, m.home_score, m.away_score)), 2) AS avg_goals_for,
       ROUND(AVG(IF(m.home_team_id = t.team_id, m.away_score, m.home_score)), 2) AS avg_goals_against
FROM team t
JOIN `match` m ON t.team_id IN (m.home_team_id, m.away_team_id)
WHERE m.season_id = @season_id AND m.home_score IS NOT NULL
GROUP BY t.team_id, t.name
ORDER BY avg_goals_for DESC, t.name;

-- 6-4. 경기장별 경기 수 (예정 경기 포함)
SELECT s.name AS stadium,
       s.city,
       COUNT(m.match_id) AS total_matches,
       COUNT(m.home_score) AS played_matches
FROM stadium s
LEFT JOIN `match` m ON m.stadium_id = s.stadium_id AND m.season_id = @season_id
GROUP BY s.stadium_id, s.name, s.city
ORDER BY total_matches DESC, s.name;
