-- Indexes to speed up global search queries

CREATE INDEX IF NOT EXISTS idx_plots_plot_name ON plots(plot_name);
CREATE INDEX IF NOT EXISTS idx_seasons_season_name ON seasons(season_name);
CREATE INDEX IF NOT EXISTS idx_tasks_title ON tasks(title);
CREATE INDEX IF NOT EXISTS idx_expenses_item_name ON expenses(item_name);
CREATE INDEX IF NOT EXISTS idx_documents_title ON documents(title);
CREATE INDEX IF NOT EXISTS idx_farms_farm_name ON farms(farm_name);
CREATE INDEX IF NOT EXISTS idx_users_user_name ON users(user_name);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_full_name ON users(full_name);
