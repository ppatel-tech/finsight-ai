CREATE TABLE IF NOT EXISTS budgets (
                                       id           BIGSERIAL PRIMARY KEY,
                                       category     VARCHAR(50)    NOT NULL,
                                       limit_amount DECIMAL(10, 2) NOT NULL,
                                       month        INTEGER        NOT NULL,
                                       year         INTEGER        NOT NULL,
                                       created_at   TIMESTAMP,
                                       user_id      BIGINT         NOT NULL,
                                       CONSTRAINT fk_budget_user
                                           FOREIGN KEY (user_id)
                                               REFERENCES users(id)
                                               ON DELETE CASCADE,
                                       CONSTRAINT uk_budget_user_category_month_year
                                           UNIQUE (user_id, category, month, year)
);