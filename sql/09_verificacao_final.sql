-- Projeto Final - Laboratorio de Banco de Dados - Etapa 2 - Tema: Oficina Mecanica
-- Grupo 4: Cleber Gabriel Pereira Passos, Joaquim Manoel Lima Viana Vieira, Felipe Rodrigues Garcia
-- Arquivo 09_verificacao_final.sql: verificacao automatica pos-instalacao (somente SELECT; nao altera dados).
-- Execute DEPOIS dos scripts 01 a 07. Cada linha retorna 'OK' ou 'FALHA'.
-- Use a saida desta execucao como evidencia no relatorio (print da grade de resultados).

USE oficina_mecanica;

SELECT VERSION() AS versao_mysql, @@default_storage_engine AS engine_padrao, NOW() AS executado_em;

-- 1. Volume minimo de carga (enunciado, secao 4: >= 40 linhas nas principais e >= 100 na de maior movimento)
SELECT 'cliente >= 40' AS verificacao, COUNT(*) AS valor, IF(COUNT(*) >= 40, 'OK', 'FALHA') AS resultado FROM cliente
UNION ALL SELECT 'veiculo >= 40', COUNT(*), IF(COUNT(*) >= 40, 'OK', 'FALHA') FROM veiculo
UNION ALL SELECT 'ordem_servico >= 40', COUNT(*), IF(COUNT(*) >= 40, 'OK', 'FALHA') FROM ordem_servico
UNION ALL SELECT 'peca_utilizada >= 100 (maior movimento)', COUNT(*), IF(COUNT(*) >= 100, 'OK', 'FALHA') FROM peca_utilizada
UNION ALL SELECT 'historico_status_os >= 100', COUNT(*), IF(COUNT(*) >= 100, 'OK', 'FALHA') FROM historico_status_os;

-- 2. Integridade da especializacao Veiculo -> Carro | Moto (RN02): total e exclusiva
SELECT 'veiculo sem subclasse' AS verificacao, COUNT(*) AS violacoes, IF(COUNT(*) = 0, 'OK', 'FALHA') AS resultado
FROM veiculo v
WHERE NOT EXISTS (SELECT 1 FROM carro c WHERE c.id_veiculo = v.id_veiculo)
  AND NOT EXISTS (SELECT 1 FROM moto m WHERE m.id_veiculo = v.id_veiculo)
UNION ALL
SELECT 'veiculo Carro registrado em moto (ou Moto em carro)', COUNT(*), IF(COUNT(*) = 0, 'OK', 'FALHA')
FROM veiculo v
WHERE (v.tipo_veiculo = 'Carro' AND EXISTS (SELECT 1 FROM moto m WHERE m.id_veiculo = v.id_veiculo))
   OR (v.tipo_veiculo = 'Moto'  AND EXISTS (SELECT 1 FROM carro c WHERE c.id_veiculo = v.id_veiculo))
UNION ALL
SELECT 'veiculo com tipo diferente da subclasse existente', COUNT(*), IF(COUNT(*) = 0, 'OK', 'FALHA')
FROM veiculo v
WHERE (v.tipo_veiculo = 'Carro' AND NOT EXISTS (SELECT 1 FROM carro c WHERE c.id_veiculo = v.id_veiculo))
   OR (v.tipo_veiculo = 'Moto'  AND NOT EXISTS (SELECT 1 FROM moto m WHERE m.id_veiculo = v.id_veiculo));

-- 3. Regras de negocio verificaveis por consulta
SELECT 'RN20: mecanico supervisor de si mesmo' AS verificacao, COUNT(*) AS violacoes, IF(COUNT(*) = 0, 'OK', 'FALHA') AS resultado
FROM mecanico WHERE id_supervisor = id_mecanico
UNION ALL
SELECT 'RN16: garantia em OS nao concluida', COUNT(*), IF(COUNT(*) = 0, 'OK', 'FALHA')
FROM garantia g JOIN ordem_servico os ON os.id_os = g.id_os WHERE os.status <> 'Concluida'
UNION ALL
SELECT 'valor_total <> mao de obra + pecas', COUNT(*), IF(COUNT(*) = 0, 'OK', 'FALHA')
FROM ordem_servico os
WHERE os.valor_total <> COALESCE((SELECT SUM(i.valor_cobrado) FROM item_ordem_servico i WHERE i.id_os = os.id_os), 0)
                      + COALESCE((SELECT SUM(p.quantidade * p.preco_aplicado) FROM peca_utilizada p WHERE p.id_os = os.id_os), 0);

-- 4. Seguranca de senhas (B5): salt de 32 hex (128 bits), hash SHA-256 de 64 hex, salts distintos
SELECT 'usuarios com salt <> 32 ou hash <> 64 caracteres' AS verificacao, COUNT(*) AS violacoes, IF(COUNT(*) = 0, 'OK', 'FALHA') AS resultado
FROM usuario_sistema WHERE CHAR_LENGTH(salt) <> 32 OR CHAR_LENGTH(senha_hash) <> 64
UNION ALL
SELECT 'salts repetidos entre usuarios', COUNT(*), IF(COUNT(*) = 0, 'OK', 'FALHA')
FROM (SELECT salt FROM usuario_sistema GROUP BY salt HAVING COUNT(*) > 1) t
UNION ALL
SELECT 'usuarios de demonstracao (esperado 3)', COUNT(*), IF(COUNT(*) = 3, 'OK', 'FALHA')
FROM usuario_sistema WHERE login IN ('admin', 'atendente', 'mecanico');

-- 5. Convencao de nomes das restricoes (A6): uq_, fk_, ck_ (a PK do MySQL e sempre chamada PRIMARY)
SELECT 'restricoes UNIQUE fora do prefixo uq_' AS verificacao, COUNT(*) AS violacoes, IF(COUNT(*) = 0, 'OK', 'FALHA') AS resultado
FROM information_schema.table_constraints
WHERE constraint_schema = DATABASE() AND constraint_type = 'UNIQUE' AND constraint_name NOT LIKE 'uq\_%'
UNION ALL
SELECT 'restricoes FOREIGN KEY fora do prefixo fk_', COUNT(*), IF(COUNT(*) = 0, 'OK', 'FALHA')
FROM information_schema.table_constraints
WHERE constraint_schema = DATABASE() AND constraint_type = 'FOREIGN KEY' AND constraint_name NOT LIKE 'fk\_%'
UNION ALL
SELECT 'restricoes CHECK fora do prefixo ck_', COUNT(*), IF(COUNT(*) = 0, 'OK', 'FALHA')
FROM information_schema.table_constraints
WHERE constraint_schema = DATABASE() AND constraint_type = 'CHECK' AND constraint_name NOT LIKE 'ck\_%';

-- 6. Objetos esperados (Etapa 2)
SELECT 'views (esperado 4: consolidado, faturamento, atendimento, mecanico)' AS verificacao, COUNT(*) AS valor,
       IF(COUNT(*) = 4, 'OK', 'FALHA') AS resultado
FROM information_schema.views WHERE table_schema = DATABASE()
UNION ALL
SELECT 'gatilhos (esperado 6: 2 auditoria + 2 RN20 + 1 RN16 + 1 RN15)', COUNT(*), IF(COUNT(*) = 6, 'OK', 'FALHA')
FROM information_schema.triggers WHERE trigger_schema = DATABASE();

-- 7. Lista das chaves estrangeiras com as acoes ON DELETE / ON UPDATE (conferir que nenhuma e 'NO ACTION' por omissao)
SELECT constraint_name AS fk, table_name AS tabela, referenced_table_name AS referencia,
       delete_rule AS on_delete, update_rule AS on_update
FROM information_schema.referential_constraints
WHERE constraint_schema = DATABASE()
ORDER BY table_name, constraint_name;

-- 8. Privilegios efetivos (executar como administrador do banco e guardar a saida como evidencia de B5):
-- SHOW GRANTS FOR 'role_atendente';
-- SHOW GRANTS FOR 'role_mecanico';
-- SHOW GRANTS FOR 'app_oficina'@'localhost';
