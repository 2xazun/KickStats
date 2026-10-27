-- KickStats 초기 스키마 (MySQL 8.0)
DROP DATABASE IF EXISTS kickstats;
CREATE DATABASE kickstats DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE kickstats;

CREATE TABLE league (
    league_id   INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    country     VARCHAR(50) NOT NULL
);

CREATE TABLE season (
    season_id   INT AUTO_INCREMENT PRIMARY KEY,
    league_id   INT NOT NULL,
    year        VARCHAR(20) NOT NULL,          -- 예: 2025, 2025-26
    start_date  DATE,
    end_date    DATE,
    UNIQUE (league_id, year),
    FOREIGN KEY (league_id) REFERENCES league(league_id) ON DELETE CASCADE
);

CREATE TABLE stadium (
    stadium_id  INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    city        VARCHAR(50),
    capacity    INT
);

CREATE TABLE team (
    team_id         INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(50) NOT NULL UNIQUE,
    city            VARCHAR(50),
    home_stadium_id INT,
    founded_year    SMALLINT,
    FOREIGN KEY (home_stadium_id) REFERENCES stadium(stadium_id) ON DELETE SET NULL
);

-- 시즌별 참가 팀
CREATE TABLE season_team (
    season_id   INT NOT NULL,
    team_id     INT NOT NULL,
    PRIMARY KEY (season_id, team_id),
    FOREIGN KEY (season_id) REFERENCES season(season_id) ON DELETE CASCADE,
    FOREIGN KEY (team_id)   REFERENCES team(team_id)     ON DELETE CASCADE
);

CREATE TABLE player (
    player_id   INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    position    ENUM('GK','DF','MF','FW') NOT NULL,
    nationality VARCHAR(50),
    birth_date  DATE
);

-- 계약(소속/등번호/이적 이력). end_date가 NULL이면 현재 소속
CREATE TABLE contract (
    contract_id INT AUTO_INCREMENT PRIMARY KEY,
    player_id   INT NOT NULL,
    team_id     INT NOT NULL,
    back_number TINYINT UNSIGNED,
    start_date  DATE NOT NULL,
    end_date    DATE,
    FOREIGN KEY (player_id) REFERENCES player(player_id) ON DELETE CASCADE,
    FOREIGN KEY (team_id)   REFERENCES team(team_id)     ON DELETE CASCADE,
    CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE TABLE `match` (
    match_id     INT AUTO_INCREMENT PRIMARY KEY,
    season_id    INT NOT NULL,
    home_team_id INT NOT NULL,
    away_team_id INT NOT NULL,
    stadium_id   INT,
    match_date   DATETIME NOT NULL,
    round        SMALLINT,
    home_score   TINYINT UNSIGNED,             -- NULL이면 경기 전
    away_score   TINYINT UNSIGNED,
    FOREIGN KEY (season_id)    REFERENCES season(season_id) ON DELETE CASCADE,
    FOREIGN KEY (home_team_id) REFERENCES team(team_id),
    FOREIGN KEY (away_team_id) REFERENCES team(team_id),
    FOREIGN KEY (stadium_id)   REFERENCES stadium(stadium_id) ON DELETE SET NULL,
    CHECK (home_team_id <> away_team_id)
);

-- 출전 기록
CREATE TABLE appearance (
    match_id       INT NOT NULL,
    player_id      INT NOT NULL,
    team_id        INT NOT NULL,
    is_starter     BOOLEAN NOT NULL,
    minutes_played TINYINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (match_id, player_id),
    FOREIGN KEY (match_id)  REFERENCES `match`(match_id) ON DELETE CASCADE,
    FOREIGN KEY (player_id) REFERENCES player(player_id),
    FOREIGN KEY (team_id)   REFERENCES team(team_id)
);

CREATE TABLE goal (
    goal_id          INT AUTO_INCREMENT PRIMARY KEY,
    match_id         INT NOT NULL,
    team_id          INT NOT NULL,             -- 득점이 인정된 팀 (자책골 대비)
    scorer_id        INT NOT NULL,
    assist_player_id INT,
    minute           TINYINT UNSIGNED NOT NULL,
    is_penalty       BOOLEAN NOT NULL DEFAULT FALSE,
    is_own_goal      BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (match_id)         REFERENCES `match`(match_id) ON DELETE CASCADE,
    FOREIGN KEY (team_id)          REFERENCES team(team_id),
    FOREIGN KEY (scorer_id)        REFERENCES player(player_id),
    FOREIGN KEY (assist_player_id) REFERENCES player(player_id)
);

CREATE TABLE card (
    card_id    INT AUTO_INCREMENT PRIMARY KEY,
    match_id   INT NOT NULL,
    player_id  INT NOT NULL,
    card_type  ENUM('YELLOW','RED') NOT NULL,
    minute     TINYINT UNSIGNED NOT NULL,
    FOREIGN KEY (match_id)  REFERENCES `match`(match_id) ON DELETE CASCADE,
    FOREIGN KEY (player_id) REFERENCES player(player_id)
);
