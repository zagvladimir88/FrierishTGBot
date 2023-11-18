--liquibase formatted sql

--changeset zavladimir:1
CREATE TABLE IF NOT EXISTS chats
(
    id      SERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL UNIQUE
);
--rollback DROP TABLE chats


