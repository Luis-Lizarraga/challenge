-- ===================================================================
-- DATABASE SCHEMA DEFINITION
-- Description: DDL script for creating tables, constraints, and indexes.
-- Target Engine: H2 Database / PostgreSQL
-- ===================================================================

-- 1. Drop existing tables in reverse dependency order to avoid FK conflicts
DROP TABLE IF EXISTS carrito_items;
DROP TABLE IF EXISTS carritos;
DROP TABLE IF EXISTS descuentos;
DROP TABLE IF EXISTS productos;
DROP TABLE IF EXISTS categorias;
DROP TABLE IF EXISTS usuarios;

-- 2. TABLE: usuarios
CREATE TABLE usuarios (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          nombre VARCHAR(255) NOT NULL UNIQUE,
                          password VARCHAR(255) NOT NULL,
                          email VARCHAR(255) NOT NULL,
                          role VARCHAR(50) NOT NULL
);

-- 3. TABLE: categorias
CREATE TABLE categorias (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            nombre VARCHAR(255) NOT NULL UNIQUE
);

-- 4. TABLE: productos
CREATE TABLE productos (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           codigo VARCHAR(255) NOT NULL UNIQUE,
                           nombre VARCHAR(255) NOT NULL,
                           precio DECIMAL(19, 2) NOT NULL,
                           stock INT NOT NULL DEFAULT 0,
                           categoria_id BIGINT NOT NULL,
                           CONSTRAINT fk_productos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id)
);

-- 5. TABLE: descuentos
CREATE TABLE descuentos (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            categoria_id BIGINT NOT NULL,
                            porcentaje DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
                            activo BOOLEAN NOT NULL DEFAULT FALSE,
                            CONSTRAINT fk_descuentos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id)
);

-- 6. TABLE: carritos
CREATE TABLE carritos (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          usuario_id BIGINT,
                          estado VARCHAR(50) NOT NULL,
                          monto_total DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
                          version BIGINT,
                          razon_fallo VARCHAR(500),
                          CONSTRAINT fk_carritos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- MANUAL INDEX 1: Accelerates fetching all carts associated with a specific user.
-- Not unique, as a user can have multiple carts over time.
CREATE INDEX idx_carritos_usuario ON carritos(usuario_id);

-- 7. TABLE: carrito_items
CREATE TABLE carrito_items (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               carrito_id BIGINT NOT NULL,
                               producto_id BIGINT NOT NULL,
                               cantidad INT NOT NULL DEFAULT 1,
                               precio_unitario DECIMAL(19, 2),
                               monto_descuento DECIMAL(19, 2),
                               CONSTRAINT fk_carrito_items_cart FOREIGN KEY (carrito_id) REFERENCES carritos(id),
                               CONSTRAINT fk_carrito_items_product FOREIGN KEY (producto_id) REFERENCES productos(id),
                               CONSTRAINT uk_cart_items_cart_product UNIQUE (carrito_id, producto_id)
);