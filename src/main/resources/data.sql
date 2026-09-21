-- ===================================================================
-- INITIAL DATA INSERTION SCRIPT
-- Description: DML script for seeding test data and stress users.
-- ===================================================================

-- 1. Populate 'categorias' table
INSERT INTO categorias (id, nombre) VALUES
                                        (1, 'PROTEIN'),
                                        (2, 'CREATINE'),
                                        (3, 'VITAMINS'),
                                        (4, 'AMINOACIDS');

-- 2. Populate 'productos' table
INSERT INTO productos (id, codigo, nombre, precio, stock, categoria_id) VALUES
                                                                            (1, 'PROD-001', 'Whey Protein Isolate 1kg', 35.00, 800, 1),
                                                                            (2, 'PROD-002', 'Concentrated Whey 1kg', 25.00, 200, 1),
                                                                            (3, 'PROD-003', 'Creatina Monohidratada 300g', 20.00, 500, 2),
                                                                            (4, 'PROD-004', 'Multvitamínico Alpha 90 caps', 15.00, 100, 3),
                                                                            (5, 'PROD-005', 'BCAA 2:1:1 200g', 18.00, 300, 4);

-- 3. Populate 'descuentos' table
INSERT INTO descuentos (id, categoria_id, porcentaje, activo) VALUES
                                                                  (1, 1, 10.00, TRUE),  -- 10% discount on PROTEIN
                                                                  (2, 2, 15.00, TRUE);  -- 15% discount on CREATINE

-- 4. Seed 'usuarios' table for JMeter stress testing (1001 users)
INSERT INTO usuarios (nombre, email, password, role)
SELECT
    'user_' || X,
    'user_' || X || '@stress.com',
    '$2a$10$IQikAbB3.4xBNI0IxA04kOlEaVevM.gvomkwcfFMRxp9M387b7jQ.',
    'USER'
FROM SYSTEM_RANGE(1, 1001) AS T(X);