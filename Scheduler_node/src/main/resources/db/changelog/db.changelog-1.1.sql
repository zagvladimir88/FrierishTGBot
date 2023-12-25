--liquibase formatted sql

--changeset zavladimir:2
ALTER TABLE chats
    ADD creation_date TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6);

ALTER TABLE chats
    ADD modification_date TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6);

ALTER TABLE chats
    ADD status VARCHAR(25) DEFAULT 'NOT_ACTIVE';

