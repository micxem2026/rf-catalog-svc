-- V1__Create_tables.sql

-- Создание таблицы категорий характеристик
CREATE TABLE IF NOT EXISTS klf_feature_category (
                                      id SERIAL PRIMARY KEY,
                                      name VARCHAR(50) NOT NULL,
                                      created_by VARCHAR(20) NOT NULL,
                                      created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_by VARCHAR(20),
                                      updated_at TIMESTAMPTZ
);

-- Создание таблицы простых характеристик
CREATE TABLE IF NOT EXISTS klf_feature_plain (
                                   id SERIAL PRIMARY KEY,
                                   name VARCHAR(255) NOT NULL,
                                   id_feature_category INTEGER NOT NULL,
                                   created_by VARCHAR(20) NOT NULL,
                                   created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_by VARCHAR(20),
                                   updated_at TIMESTAMPTZ,
                                   CONSTRAINT fk_klf_feature_plain_category FOREIGN KEY (id_feature_category)
                                       REFERENCES klf_feature_category(id)
);

-- Создание таблицы дерева характеристик
CREATE TABLE IF NOT EXISTS klf_feature_tree (
                                  id SERIAL PRIMARY KEY,
                                  id_parent INTEGER,
                                  id_feature_category INTEGER NOT NULL,
                                  id_feature_plain INTEGER NOT NULL,
                                  validity_period daterange NOT NULL DEFAULT '(,)'::daterange,
                                  created_by VARCHAR(20) NOT NULL,
                                  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_by VARCHAR(20),
                                  updated_at TIMESTAMPTZ,
                                  CONSTRAINT fk_klf_feature_tree_parent FOREIGN KEY (id_parent)
                                      REFERENCES klf_feature_tree(id),
                                  CONSTRAINT fk_klf_feature_tree_category FOREIGN KEY (id_feature_category)
                                      REFERENCES klf_feature_category(id),
                                  CONSTRAINT fk_klf_feature_tree_plain FOREIGN KEY (id_feature_plain)
                                      REFERENCES klf_feature_plain(id)
);

-- Создание таблицы кэшированных пользователей
CREATE TABLE IF NOT EXISTS sync__users (
                                     id INTEGER PRIMARY KEY,
                                     username VARCHAR(50) UNIQUE NOT NULL,
                                     display_name VARCHAR(100) NOT NULL,
                                     email VARCHAR(100) UNIQUE NOT NULL,
                                     password_hash VARCHAR(255) NOT NULL,
                                     enabled BOOLEAN DEFAULT true,
                                     account_non_expired BOOLEAN DEFAULT false,
                                     account_non_locked BOOLEAN DEFAULT true,
                                     expiration_date TIMESTAMP,
                                     last_logon TIMESTAMP,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP,
                                     user_type VARCHAR(10) NOT NULL DEFAULT 'USER'::character varying
);
