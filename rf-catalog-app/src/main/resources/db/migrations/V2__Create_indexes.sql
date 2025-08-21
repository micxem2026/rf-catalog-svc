-- V2__Create_indexes.sql

-- Индексы для klf_feature_plain
CREATE INDEX IF NOT EXISTS idx_klf_feature_plain_category ON klf_feature_plain(id_feature_category);
CREATE INDEX IF NOT EXISTS idx_klf_feature_plain_name ON klf_feature_plain(name);

-- Индексы для klf_feature_tree
CREATE INDEX IF NOT EXISTS idx_klf_feature_tree_parent ON klf_feature_tree(id_parent);
CREATE INDEX IF NOT EXISTS idx_klf_feature_tree_category ON klf_feature_tree(id_feature_category);
CREATE INDEX IF NOT EXISTS idx_klf_feature_tree_plain ON klf_feature_tree(id_feature_plain);
CREATE INDEX IF NOT EXISTS idx_klf_feature_tree_parent_category ON klf_feature_tree(id_parent, id_feature_category);

CREATE UNIQUE INDEX IF NOT EXISTS UNQ_KLF_FEATURE_PLAIN ON KLF_FEATURE_PLAIN(ID_FEATURE_CATEGORY, NAME);
