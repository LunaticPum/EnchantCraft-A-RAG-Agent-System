ALTER TABLE user_account
    ADD COLUMN search_count INT NOT NULL DEFAULT 0 COMMENT '今日检索次数',
    ADD COLUMN chat_count   INT NOT NULL DEFAULT 0 COMMENT '今日对话次数',
    ADD COLUMN last_reset   DATE COMMENT '配额重置日期';
