-- Projeto Final - Laboratorio de Banco de Dados - Etapa 2 - Tema: Oficina Mecanica
-- Grupo 4: Cleber Gabriel Pereira Passos, Joaquim Manoel Lima Viana Vieira, Felipe Rodrigues Garcia
-- SGBD: MySQL 8.0+ (MySQL Workbench)
-- Arquivo 06_otimizacao_indices.sql: Estudo de desempenho de tres consultas criticas (B4).

USE oficina_mecanica;

-- =============================================================================
-- COMO USAR ESTE ESTUDO (leia antes de executar)
-- 1. Execute APENAS depois de 01, 02 e 04. A secao 0 remove os 3 indices experimentais,
--    garantindo que o plano 'ANTES' seja obtido sem a intervencao (script reexecutavel).
-- 2. Para cada consulta ha: EXPLAIN FORMAT=TREE (plano estimado) e EXPLAIN ANALYZE
--    (plano + tempo real medido). Execute cada par ANTES e DEPOIS do CREATE INDEX.
-- 3. Execute o ANALYZE 3 vezes e registre a mediana (a 1a execucao aquece o buffer pool).
-- 4. Registre no relatorio: versao do MySQL (SELECT VERSION();), contagem de linhas
--    das tabelas (SELECT COUNT(*) ...) e a saida REAL. Nao ha ganho garantido: em tabelas
--    pequenas o otimizador pode preferir varredura completa; relate o que de fato aparecer.
-- =============================================================================
SELECT VERSION() AS versao_mysql;
SELECT (SELECT COUNT(*) FROM ordem_servico) AS os,
       (SELECT COUNT(*) FROM peca_utilizada) AS peca_utilizada,
       (SELECT COUNT(*) FROM item_ordem_servico) AS itens;

-- =============================================================================
-- 0. LIMPEZA PREVIA DE INDICES EXPERIMENTAIS (Garante reprodutibilidade do plano ANTES)
-- Compativel com a sintaxe do MySQL 8.0 (verificacao via information_schema)
-- =============================================================================
SET @sql = (SELECT IF(COUNT(*) > 0, 'DROP INDEX idx_os_status_abertura ON ordem_servico', 'DO 0') 
            FROM information_schema.statistics 
            WHERE table_schema = DATABASE() AND table_name = 'ordem_servico' AND index_name = 'idx_os_status_abertura');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) > 0, 'DROP INDEX idx_pecautil_cob ON peca_utilizada', 'DO 0') 
            FROM information_schema.statistics 
            WHERE table_schema = DATABASE() AND table_name = 'peca_utilizada' AND index_name = 'idx_pecautil_cob');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*) > 0, 'DROP INDEX idx_os_mecanico_data ON ordem_servico', 'DO 0') 
            FROM information_schema.statistics 
            WHERE table_schema = DATABASE() AND table_name = 'ordem_servico' AND index_name = 'idx_os_mecanico_data');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- =============================================================================
-- CONSULTA CRITICA 1: Busca de Ordens de Servico por Status e Periodo
-- Pergunta de negocio: "Quais ordens de servico foram abertas a partir de 2026-01-01
-- com status 'Concluida', exibindo cliente, veiculo e valor total?"
-- =============================================================================

-- 1.1 Plano de Execucao ANTES da intervencao:
EXPLAIN FORMAT=TREE
SELECT 
    os.id_os,
    os.status,
    os.data_abertura,
    c.nome AS cliente,
    v.placa,
    os.valor_total
FROM ordem_servico os
JOIN veiculo v ON v.id_veiculo = os.id_veiculo
JOIN cliente c ON c.id_cliente = v.id_cliente
WHERE os.status = 'Concluida'
  AND os.data_abertura >= '2026-01-01'
ORDER BY os.data_abertura DESC;


-- Medicao REAL (tempo de execucao e linhas lidas por no do plano; MySQL >= 8.0.18):
EXPLAIN ANALYZE
SELECT 
    os.id_os,
    os.status,
    os.data_abertura,
    c.nome AS cliente,
    v.placa,
    os.valor_total
FROM ordem_servico os
JOIN veiculo v ON v.id_veiculo = os.id_veiculo
JOIN cliente c ON c.id_cliente = v.id_cliente
WHERE os.status = 'Concluida'
  AND os.data_abertura >= '2026-01-01'
ORDER BY os.data_abertura DESC;

-- 1.2 Criacao do Indice Composto Otimizado:
-- Justificativa: Coloca-se a coluna de igualdade ('status') como primeiro elemento
-- e a coluna de faixa ('data_abertura') como segundo elemento na B-Tree.
-- Hipotese a verificar no plano DEPOIS: o InnoDB navega pelo indice ja na ordem de
-- data_abertura, podendo dispensar o 'Sort' do plano ANTES. So afirme isso se o plano real mostrar.
CREATE INDEX idx_os_status_abertura ON ordem_servico (status, data_abertura DESC);

-- 1.3 Plano de Execucao DEPOIS da intervencao:
EXPLAIN FORMAT=TREE
SELECT 
    os.id_os,
    os.status,
    os.data_abertura,
    c.nome AS cliente,
    v.placa,
    os.valor_total
FROM ordem_servico os
JOIN veiculo v ON v.id_veiculo = os.id_veiculo
JOIN cliente c ON c.id_cliente = v.id_cliente
WHERE os.status = 'Concluida'
  AND os.data_abertura >= '2026-01-01'
ORDER BY os.data_abertura DESC;


-- Medicao REAL (tempo de execucao e linhas lidas por no do plano; MySQL >= 8.0.18):
EXPLAIN ANALYZE
SELECT 
    os.id_os,
    os.status,
    os.data_abertura,
    c.nome AS cliente,
    v.placa,
    os.valor_total
FROM ordem_servico os
JOIN veiculo v ON v.id_veiculo = os.id_veiculo
JOIN cliente c ON c.id_cliente = v.id_cliente
WHERE os.status = 'Concluida'
  AND os.data_abertura >= '2026-01-01'
ORDER BY os.data_abertura DESC;


-- =============================================================================
-- CONSULTA CRITICA 2: Consumo Financeiro de Pecas em OS Concluidas por Fornecedor
-- Pergunta de negocio: "Qual o total financeiro faturado em pecas utilizadas
-- em ordens concluidas por cada fornecedor cadastrado na oficina?"
-- =============================================================================

-- 2.1 Plano de Execucao ANTES da intervencao:
EXPLAIN FORMAT=TREE
SELECT 
    f.nome AS fornecedor,
    p.codigo_fabricante,
    p.nome AS peca,
    SUM(pu.quantidade) AS total_unidades,
    SUM(pu.quantidade * pu.preco_aplicado) AS total_faturado
FROM fornecedor f
JOIN peca p ON p.id_fornecedor = f.id_fornecedor
JOIN peca_utilizada pu ON pu.id_peca = p.id_peca
JOIN ordem_servico os ON os.id_os = pu.id_os
WHERE os.status = 'Concluida'
GROUP BY f.id_fornecedor, p.id_peca
ORDER BY total_faturado DESC;


-- Medicao REAL (tempo de execucao e linhas lidas por no do plano; MySQL >= 8.0.18):
EXPLAIN ANALYZE
SELECT 
    f.nome AS fornecedor,
    p.codigo_fabricante,
    p.nome AS peca,
    SUM(pu.quantidade) AS total_unidades,
    SUM(pu.quantidade * pu.preco_aplicado) AS total_faturado
FROM fornecedor f
JOIN peca p ON p.id_fornecedor = f.id_fornecedor
JOIN peca_utilizada pu ON pu.id_peca = p.id_peca
JOIN ordem_servico os ON os.id_os = pu.id_os
WHERE os.status = 'Concluida'
GROUP BY f.id_fornecedor, p.id_peca
ORDER BY total_faturado DESC;

-- 2.2 Criacao do Indice de Cobertura (Covering Index):
-- Justificativa: Ao indexar (id_peca, id_os, quantidade, preco_aplicado), o MySQL
-- consegue resolver o filtro da OS e o calculo do faturamento diretamente nos nos
-- do indice secundario sem precisar consultar as paginas de dados da tabela base.
CREATE INDEX idx_pecautil_cob ON peca_utilizada (id_peca, id_os, quantidade, preco_aplicado);

-- 2.3 Plano de Execucao DEPOIS da intervencao:
EXPLAIN FORMAT=TREE
SELECT 
    f.nome AS fornecedor,
    p.codigo_fabricante,
    p.nome AS peca,
    SUM(pu.quantidade) AS total_unidades,
    SUM(pu.quantidade * pu.preco_aplicado) AS total_faturado
FROM fornecedor f
JOIN peca p ON p.id_fornecedor = f.id_fornecedor
JOIN peca_utilizada pu ON pu.id_peca = p.id_peca
JOIN ordem_servico os ON os.id_os = pu.id_os
WHERE os.status = 'Concluida'
GROUP BY f.id_fornecedor, p.id_peca
ORDER BY total_faturado DESC;


-- Medicao REAL (tempo de execucao e linhas lidas por no do plano; MySQL >= 8.0.18):
EXPLAIN ANALYZE
SELECT 
    f.nome AS fornecedor,
    p.codigo_fabricante,
    p.nome AS peca,
    SUM(pu.quantidade) AS total_unidades,
    SUM(pu.quantidade * pu.preco_aplicado) AS total_faturado
FROM fornecedor f
JOIN peca p ON p.id_fornecedor = f.id_fornecedor
JOIN peca_utilizada pu ON pu.id_peca = p.id_peca
JOIN ordem_servico os ON os.id_os = pu.id_os
WHERE os.status = 'Concluida'
GROUP BY f.id_fornecedor, p.id_peca
ORDER BY total_faturado DESC;


-- =============================================================================
-- CONSULTA CRITICA 3: Produtividade e Horas de Trabalho por Mecanico no Ano
-- Pergunta de negocio: "Quantas ordens concluidas e quantas horas de servico cada mecanico
-- executou no ano de 2026?"
-- =============================================================================

-- 3.1 Plano de Execucao ANTES da intervencao:
EXPLAIN FORMAT=TREE
SELECT 
    m.id_mecanico,
    m.nome AS mecanico,
    COUNT(DISTINCT os.id_os) AS total_os,
    COALESCE(SUM(ios.quantidade_horas), 0) AS total_horas
FROM mecanico m
JOIN ordem_servico os ON os.id_mecanico_responsavel = m.id_mecanico
LEFT JOIN item_ordem_servico ios ON ios.id_os = os.id_os
WHERE os.status = 'Concluida'
  AND os.data_abertura >= '2026-01-01'
  AND os.data_abertura < '2027-01-01'
GROUP BY m.id_mecanico
ORDER BY total_horas DESC;


-- Medicao REAL (tempo de execucao e linhas lidas por no do plano; MySQL >= 8.0.18):
EXPLAIN ANALYZE
SELECT 
    m.id_mecanico,
    m.nome AS mecanico,
    COUNT(DISTINCT os.id_os) AS total_os,
    COALESCE(SUM(ios.quantidade_horas), 0) AS total_horas
FROM mecanico m
JOIN ordem_servico os ON os.id_mecanico_responsavel = m.id_mecanico
LEFT JOIN item_ordem_servico ios ON ios.id_os = os.id_os
WHERE os.status = 'Concluida'
  AND os.data_abertura >= '2026-01-01'
  AND os.data_abertura < '2027-01-01'
GROUP BY m.id_mecanico
ORDER BY total_horas DESC;

-- 3.2 Criacao do Indice Composto Mecanico + Status + Data:
-- Justificativa: igualdade em status e faixa em data_abertura, agrupando por mecanico.
-- Hipotese a verificar no plano DEPOIS (o otimizador pode ignorar o indice em tabela pequena).
CREATE INDEX idx_os_mecanico_data ON ordem_servico (id_mecanico_responsavel, status, data_abertura);

-- 3.3 Plano de Execucao DEPOIS da intervencao:
EXPLAIN FORMAT=TREE
SELECT 
    m.id_mecanico,
    m.nome AS mecanico,
    COUNT(DISTINCT os.id_os) AS total_os,
    COALESCE(SUM(ios.quantidade_horas), 0) AS total_horas
FROM mecanico m
JOIN ordem_servico os ON os.id_mecanico_responsavel = m.id_mecanico
LEFT JOIN item_ordem_servico ios ON ios.id_os = os.id_os
WHERE os.status = 'Concluida'
  AND os.data_abertura >= '2026-01-01'
  AND os.data_abertura < '2027-01-01'
GROUP BY m.id_mecanico
ORDER BY total_horas DESC;


-- Medicao REAL (tempo de execucao e linhas lidas por no do plano; MySQL >= 8.0.18):
EXPLAIN ANALYZE
SELECT 
    m.id_mecanico,
    m.nome AS mecanico,
    COUNT(DISTINCT os.id_os) AS total_os,
    COALESCE(SUM(ios.quantidade_horas), 0) AS total_horas
FROM mecanico m
JOIN ordem_servico os ON os.id_mecanico_responsavel = m.id_mecanico
LEFT JOIN item_ordem_servico ios ON ios.id_os = os.id_os
WHERE os.status = 'Concluida'
  AND os.data_abertura >= '2026-01-01'
  AND os.data_abertura < '2027-01-01'
GROUP BY m.id_mecanico
ORDER BY total_horas DESC;
