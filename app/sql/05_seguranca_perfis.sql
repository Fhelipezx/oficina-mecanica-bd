-- Projeto Final - Laboratorio de Banco de Dados - Etapa 2 - Tema: Oficina Mecanica
-- Grupo 4: Cleber Gabriel Pereira Passos, Joaquim Manoel Lima Viana Vieira, Felipe Rodrigues Garcia
-- SGBD: MySQL 8.0+ (MySQL Workbench)
-- Arquivo 05_seguranca_perfis.sql: Perfis de acesso, demonstracao de GRANT/REVOKE, senhas com Hash+Sal e Visoes restritivas (B5).

USE oficina_mecanica;

-- -----------------------------------------------------------------------------
-- 1. Tabela de Usuarios da Aplicacao com Hash e Sal de 128 bits (B5)
-- Senhas NUNCA em texto claro. Armazena SHA-256(senha + salt).
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuario_sistema (
    id_usuario      INT UNSIGNED AUTO_INCREMENT,
    login           VARCHAR(50)     NOT NULL,
    nome_completo   VARCHAR(120)    NOT NULL,
    senha_hash      CHAR(64)        NOT NULL COMMENT 'Hash SHA-256 (64 hex)',
    salt            VARCHAR(32)     NOT NULL COMMENT 'Sal aleatorio de 128 bits (32 hex)',
    perfil          ENUM('Administrador','Atendente','Mecanico') NOT NULL,
    ativo           BOOLEAN         NOT NULL DEFAULT TRUE,
    data_criacao    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuario_sistema PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuario_login UNIQUE (login)
) ENGINE=InnoDB;

-- Carga inicial de usuarios do sistema (senhas de demonstracao em laboratorio: 'Admin@123', 'Atend@123', 'Mec@123')
-- Hashes calculados via SHA-256(CONCAT(senha, salt_hex32)):
-- admin:     'Admin@123' + 'a1b2c3d4e5f60718293a4b5c6d7e8f90' -> fd72ccc64a40e0837f525d381c86c51d3cb67e748eb2b574c2de95cfd362bd69
-- atendente: 'Atend@123' + 'f8e7d6c5b4a39281706f5e4d3c2b1a09' -> 3e49461e8267ca08c1e88169f7bb0e9811516ef0212d49230779fb2e59e258cb
-- mecanico:  'Mec@123'   + '1029384756afbecd0192837465fedcba' -> 181e2e354c178f4859ab558c92691d9c6d2c64af11941348f67af5ab891e6229
INSERT INTO usuario_sistema (login, nome_completo, senha_hash, salt, perfil, ativo)
VALUES
('admin', 'Administrador Geral', 'fd72ccc64a40e0837f525d381c86c51d3cb67e748eb2b574c2de95cfd362bd69', 'a1b2c3d4e5f60718293a4b5c6d7e8f90', 'Administrador', TRUE),
('atendente', 'Maria Atendente', '3e49461e8267ca08c1e88169f7bb0e9811516ef0212d49230779fb2e59e258cb', 'f8e7d6c5b4a39281706f5e4d3c2b1a09', 'Atendente', TRUE),
('mecanico', 'Carlos Mecanico Chefe', '181e2e354c178f4859ab558c92691d9c6d2c64af11941348f67af5ab891e6229', '1029384756afbecd0192837465fedcba', 'Mecanico', TRUE)
ON DUPLICATE KEY UPDATE 
    nome_completo = VALUES(nome_completo),
    senha_hash = VALUES(senha_hash),
    salt = VALUES(salt),
    perfil = VALUES(perfil),
    ativo = VALUES(ativo);

-- -----------------------------------------------------------------------------
-- 2. Visoes Restritivas para Atendimento e Mecanicos (B5)
-- -----------------------------------------------------------------------------

-- Visao Restritiva para Atendimento (B5)
-- Oculta dados de custos de fornecedor e auditoria interna
CREATE OR REPLACE VIEW vw_atendimento_cliente_veiculo AS
SELECT 
    c.id_cliente,
    c.nome AS nome_cliente,
    c.telefone,
    c.email,
    v.id_veiculo,
    v.placa,
    v.marca,
    v.modelo,
    v.ano_fabricacao,
    v.tipo_veiculo
FROM cliente c
JOIN veiculo v ON v.id_cliente = c.id_cliente;

-- Visao Restritiva para Mecanicos (B5)
-- Exibe tarefas operacionais sem exibir CPF do cliente, email ou valores financeiros totais
CREATE OR REPLACE VIEW vw_mecanico_painel_operacional AS
SELECT 
    os.id_os,
    os.status,
    os.km_veiculo,
    v.placa,
    v.marca,
    v.modelo,
    m.nome AS mecanico_responsavel,
    sc.descricao AS servico,
    ios.quantidade_horas
FROM ordem_servico os
JOIN veiculo v ON v.id_veiculo = os.id_veiculo
JOIN mecanico m ON m.id_mecanico = os.id_mecanico_responsavel
LEFT JOIN item_ordem_servico ios ON ios.id_os = os.id_os
LEFT JOIN servico_catalogo sc ON sc.id_servico = ios.id_servico
WHERE os.status IN ('Aberta', 'Em andamento', 'Aguardando peca');

-- -----------------------------------------------------------------------------
-- 3. Criacao dos Perfis de Acesso no SGBD (Roles) e Usuarios de Banco (B5)
-- Principio do Menor Privilegio (Least Privilege)
-- -----------------------------------------------------------------------------

-- Criar Roles
DROP ROLE IF EXISTS 'role_admin', 'role_atendente', 'role_mecanico';
CREATE ROLE 'role_admin', 'role_atendente', 'role_mecanico';

-- Permissoes do Administrador: Acesso completo de DML e DDL no schema
GRANT ALL PRIVILEGES ON oficina_mecanica.* TO 'role_admin' WITH GRANT OPTION;

-- Permissoes do Atendente:
-- Focado no atendimento ao cliente, cadastro de veiculos e abertura de ordens de servico.
GRANT SELECT, INSERT, UPDATE ON oficina_mecanica.cliente TO 'role_atendente';
GRANT SELECT, INSERT, UPDATE ON oficina_mecanica.veiculo TO 'role_atendente';
GRANT SELECT, INSERT, UPDATE ON oficina_mecanica.carro TO 'role_atendente';
GRANT SELECT, INSERT, UPDATE ON oficina_mecanica.moto TO 'role_atendente';
-- Leitura e escrita por coluna: valor_total e mecanico_responsavel nao podem ser alterados
-- pelo atendente apos a criacao da OS. A tabela nao possui coluna descricao_problema.
GRANT SELECT (id_os, id_veiculo, id_mecanico_responsavel, data_abertura, data_conclusao, status, km_veiculo) ON oficina_mecanica.ordem_servico TO 'role_atendente';
GRANT INSERT (id_veiculo, id_mecanico_responsavel, km_veiculo) ON oficina_mecanica.ordem_servico TO 'role_atendente';
GRANT UPDATE (status, km_veiculo, data_conclusao) ON oficina_mecanica.ordem_servico TO 'role_atendente';
GRANT SELECT, INSERT, UPDATE ON oficina_mecanica.garantia TO 'role_atendente';
GRANT SELECT ON oficina_mecanica.servico_catalogo TO 'role_atendente';
GRANT SELECT ON oficina_mecanica.peca TO 'role_atendente';
GRANT SELECT ON oficina_mecanica.mecanico TO 'role_atendente';
GRANT SELECT ON oficina_mecanica.vw_atendimento_cliente_veiculo TO 'role_atendente';
GRANT SELECT (id_usuario, login, nome_completo, perfil, ativo) ON oficina_mecanica.usuario_sistema TO 'role_atendente';

-- DEMONSTRACAO EXPLICITA DE GRANT SEGUIDO DE REVOKE (Exigencia B5 / Auditoria):
-- Concedemos temporariamente permissao de exclusao em historico ao atendente:
GRANT DELETE ON oficina_mecanica.historico_status_os TO 'role_atendente';
-- E revogamos explicitamente para garantir que atendentes NAO possam apagar historico de status:
REVOKE DELETE ON oficina_mecanica.historico_status_os FROM 'role_atendente';
-- Estado final comprovado: 'role_atendente' NAO possui privilegio DELETE em historico_status_os.

-- Permissoes do Mecanico:
-- Operacao focada nos servicos executados e requisicao de pecas. Nao acessa CPF nem valores financeiros totais da OS.
GRANT SELECT (id_os, data_abertura, data_conclusao, status, km_veiculo, id_veiculo, id_mecanico_responsavel) ON oficina_mecanica.ordem_servico TO 'role_mecanico';
GRANT SELECT, INSERT, UPDATE, DELETE ON oficina_mecanica.item_ordem_servico TO 'role_mecanico';
GRANT SELECT, INSERT, UPDATE, DELETE ON oficina_mecanica.peca_utilizada TO 'role_mecanico';
GRANT SELECT, UPDATE (estoque_atual) ON oficina_mecanica.peca TO 'role_mecanico';
GRANT SELECT ON oficina_mecanica.servico_catalogo TO 'role_mecanico';
GRANT SELECT ON oficina_mecanica.historico_status_os TO 'role_mecanico';
GRANT SELECT ON oficina_mecanica.vw_mecanico_painel_operacional TO 'role_mecanico';
GRANT SELECT (id_usuario, login, nome_completo, perfil, ativo) ON oficina_mecanica.usuario_sistema TO 'role_mecanico';

-- -----------------------------------------------------------------------------
-- 4. Usuario Tecnico da Aplicacao (Least Privilege para conexao JDBC)
-- -----------------------------------------------------------------------------
DROP USER IF EXISTS 'app_oficina'@'localhost';
CREATE USER 'app_oficina'@'localhost' IDENTIFIED BY 'AppOficina@2026';

-- A conta e criada do zero e recebe somente DML neste esquema; nenhum privilegio DDL e concedido.
-- Nao executamos REVOKE de privilegios nunca concedidos, evitando falha desnecessaria na instalacao.
GRANT SELECT, INSERT, UPDATE, DELETE ON oficina_mecanica.* TO 'app_oficina'@'localhost';

-- -----------------------------------------------------------------------------
-- 5. Usuarios de Demonstracao para Avaliacao em Bancada (localhost)
-- -----------------------------------------------------------------------------
DROP USER IF EXISTS 'usr_admin'@'localhost', 'usr_atendente'@'localhost', 'usr_mecanico'@'localhost';

CREATE USER 'usr_admin'@'localhost' IDENTIFIED BY 'AdminPass@2026';
CREATE USER 'usr_atendente'@'localhost' IDENTIFIED BY 'AtendPass@2026';
CREATE USER 'usr_mecanico'@'localhost' IDENTIFIED BY 'MecPass@2026';

-- Vinculacao das roles aos respectivos usuarios
GRANT 'role_admin' TO 'usr_admin'@'localhost';
GRANT 'role_atendente' TO 'usr_atendente'@'localhost';
GRANT 'role_mecanico' TO 'usr_mecanico'@'localhost';

-- Ativacao padrao das roles
SET DEFAULT ROLE 'role_admin' TO 'usr_admin'@'localhost';
SET DEFAULT ROLE 'role_atendente' TO 'usr_atendente'@'localhost';
SET DEFAULT ROLE 'role_mecanico' TO 'usr_mecanico'@'localhost';

FLUSH PRIVILEGES;
