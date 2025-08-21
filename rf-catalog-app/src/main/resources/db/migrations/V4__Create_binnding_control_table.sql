-- Создание таблицы управления биндингами (если еще не создана)
CREATE TABLE IF NOT EXISTS rightsflow.kafka_bindings_control (
      id SERIAL PRIMARY KEY,
      binding_name VARCHAR(255) UNIQUE NOT NULL,
      binding_state VARCHAR(10) NOT NULL DEFAULT 'PAUSE'::character varying,
      created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMPTZ,
      CONSTRAINT chk_binding_state CHECK (binding_state::text = ANY (ARRAY['PAUSE'::character varying, 'RESUME'::character varying]::text[]))
);

-- Добавление триггера для автоматического обновления updated_at
CREATE OR REPLACE FUNCTION rightsflow.update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_kafka_bindings_control_updated_at ON kafka_bindings_control;
CREATE TRIGGER update_kafka_bindings_control_updated_at
    BEFORE UPDATE ON kafka_bindings_control
    FOR EACH ROW EXECUTE FUNCTION rightsflow.update_updated_at_column();

INSERT INTO rightsflow.kafka_bindings_control (binding_name, binding_state)
VALUES
    ('userProcessor-in-0', 'PAUSE')
ON CONFLICT (binding_name) DO NOTHING;