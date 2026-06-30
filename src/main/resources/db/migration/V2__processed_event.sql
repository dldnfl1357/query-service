create table if not exists processed_event
(
    event_id     varchar(64) not null,
    consumer     varchar(64) not null,
    processed_at datetime(6) not null,
    primary key (event_id, consumer),
    key idx_processed_at (processed_at)
) engine = InnoDB
  default charset = utf8mb4
  collate = utf8mb4_unicode_ci;
