-- Speed up expense queries by user (most common query pattern)
CREATE INDEX IF NOT EXISTS idx_expenses_user_id
    ON expenses(user_id);

-- Speed up expense filtering by category
CREATE INDEX IF NOT EXISTS idx_expenses_category
    ON expenses(category);

-- Speed up expense date range queries
CREATE INDEX IF NOT EXISTS idx_expenses_expense_date
    ON expenses(expense_date);

-- Speed up budget lookups
CREATE INDEX IF NOT EXISTS idx_budgets_user_id
    ON budgets(user_id);