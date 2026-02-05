-- 1. CRIAR POSIÇÃO PARA O ALUNO (Onde ele será pego/deixado)
INSERT INTO geo_position_table (id, latitude, longitude)
VALUES ('f47ac10b-58cc-4372-a567-0e02b2c3d479', -23.5555, -46.6395);

-- 2. CRIAR USUÁRIOS (Motorista e Aluno)
INSERT INTO user_table (id, name, email, password, status)
VALUES ('d10a2030-e2b1-4f1e-a5e2-123456789012', 'Motorista Teste', 'driver@test.com', '123', 'ACTIVE');
INSERT INTO user_table (id, name, email, password, status)
VALUES ('e24270d7-fdf1-46d5-a362-43c2f34d13f7', 'Aluno Teste', 'student@test.com', '123', 'ACTIVE');

-- 3. VINCULAR ÀS TABELAS ESPECÍFICAS
INSERT INTO driver_table (id, area_of_activity, total_trips)
VALUES ('d10a2030-e2b1-4f1e-a5e2-123456789012', 'São Paulo', 0);
INSERT INTO student_table (id, course, institution_type)
VALUES ('e24270d7-fdf1-46d5-a362-43c2f34d13f7', 'Engenharia', 'UNIVERSITY');

-- 4. CRIAR A VIAGEM (Usando o UUID que você já está testando no Insomnia)
INSERT INTO travels_data (id, driver_id, travel_status, origin_latitude, origin_longitude, final_latitude, final_longitude)
VALUES ('8b21fd8a-49b8-43e1-b6b6-04731b95aca7', 'd10a2030-e2b1-4f1e-a5e2-123456789012', 'ON_ROAD', -23.5500, -46.6333, -23.5600, -46.6400);

-- 5. VINCULAR O ALUNO À VIAGEM COM A POSIÇÃO (A peça que faltava!)
INSERT INTO student_travel (id, travel_id, student_id, position_id, embark)
VALUES (random_uuid(), '8b21fd8a-49b8-43e1-b6b6-04731b95aca7', 'e24270d7-fdf1-46d5-a362-43c2f34d13f7', 'f47ac10b-58cc-4372-a567-0e02b2c3d479', true);