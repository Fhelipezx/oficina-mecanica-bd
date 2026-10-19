-- Projeto Final - Laboratorio de Banco de Dados - Etapa 2 - Tema: Oficina Mecanica
-- Grupo 4: Cleber Gabriel Pereira Passos, Joaquim Manoel Lima Viana Vieira, Felipe Rodrigues Garcia
-- SGBD: MySQL 8.0+ (MySQL Workbench)
-- Arquivo 08_experimento_concorrencia.sql: Roteiro pratico de transacoes, falha provocada e controle de concorrencia (B3).
--
-- ATENCAO IMPORTANTE:
-- Este arquivo contem roteiros manuais e interativos.
-- NAO execute todo o arquivo de uma vez ('Run All').
-- Execute cada bloco individualmente conforme as instrucoes de cada secao.

USE oficina_mecanica;

-- =============================================================================
-- PARTE 1: OPERACAO TRANSACIONAL COM SUCESSO (B3)
-- Atomicidade multitabela: inclusao de item, baixa de estoque e vinculo da peca.
-- =============================================================================

-- 1.1 Estado inicial para conferencia:
SELECT id_peca, nome, estoque_atual FROM peca WHERE id_peca = 1;
SELECT id_os, valor_total FROM ordem_servico WHERE id_os = 1;

-- 1.2 Limpeza preventiva (REEXECUTAVEL): se o teste ja rodou antes, devolve ao estoque o que
--     foi baixado e remove o item 99, voltando ao estado inicial antes de repetir o experimento.
UPDATE peca p
JOIN peca_utilizada pu ON pu.id_peca = p.id_peca AND pu.id_os = 1 AND pu.numero_item = 99
SET p.estoque_atual = p.estoque_atual + pu.quantidade;
DELETE FROM peca_utilizada WHERE id_os = 1 AND numero_item = 99;
DELETE FROM item_ordem_servico WHERE id_os = 1 AND numero_item = 99;

-- 1.3 Execucao do bloco transacional:
START TRANSACTION;

-- Passo 1: Inserir item de servico
INSERT INTO item_ordem_servico (id_os, numero_item, id_servico, quantidade_horas, valor_cobrado)
VALUES (1, 99, 1, 2.00, 160.00);

-- Passo 2: Baixa controlada com validacao de saldo suficiente
UPDATE peca 
SET estoque_atual = estoque_atual - 2 
WHERE id_peca = 1 AND estoque_atual >= 2;
-- CONFIRA: ROW_COUNT() deve ser 1. Se for 0, o saldo era insuficiente: execute ROLLBACK e PARE
-- (nao execute os passos 3 e 4, para nao registrar consumo sem baixa de estoque).
SELECT ROW_COUNT() AS linhas_baixadas;

-- Passo 3: Registrar a peca utilizada
INSERT INTO peca_utilizada (id_os, numero_item, id_peca, quantidade, preco_aplicado)
VALUES (1, 99, 1, 2, 35.90);

-- Passo 4: Recalcular o valor total da OS (mao de obra + pecas)
UPDATE ordem_servico os
SET valor_total = (
    COALESCE((SELECT SUM(valor_cobrado) FROM item_ordem_servico WHERE id_os = os.id_os), 0) +
    COALESCE((SELECT SUM(quantidade * preco_aplicado) FROM peca_utilizada WHERE id_os = os.id_os), 0)
)
WHERE os.id_os = 1;

COMMIT;

-- 1.4 Conferencia apos a transacao confirmada:
SELECT id_peca, nome, estoque_atual FROM peca WHERE id_peca = 1;
SELECT id_os, valor_total FROM ordem_servico WHERE id_os = 1;


-- =============================================================================
-- PARTE 2: OPERACAO TRANSACIONAL COM FALHA PROVOCADA E ROLLBACK (B3)
-- Objetivo: Demonstrar que nenhuma alteracao parcial persiste apos erro.
-- =============================================================================

-- 2.1 Conferencia do estado antes do teste de falha:
SELECT id_peca, estoque_atual FROM peca WHERE id_peca = 1;
SELECT COUNT(*) AS itens_teste_antes FROM item_ordem_servico WHERE id_os = 1 AND numero_item = 98;

-- 2.2 Inicio da transacao:
START TRANSACTION;

-- Passo 1: Inclusao do item temporario
INSERT INTO item_ordem_servico (id_os, numero_item, id_servico, quantidade_horas, valor_cobrado)
VALUES (1, 98, 1, 1.00, 80.00);

-- Passo 2: Baixa preliminar no estoque
UPDATE peca 
SET estoque_atual = estoque_atual - 1 
WHERE id_peca = 1 AND estoque_atual >= 1;

-- Passo 3: FALHA INTENCIONAL! (Viola restricao CHECK ck_pecautil_quantidade: quantidade > 0)
-- Execute SOMENTE esta linha (selecione-a). O motor retornara ERRO 3819 (CHECK violado):
INSERT INTO peca_utilizada (id_os, numero_item, id_peca, quantidade, preco_aplicado)
VALUES (1, 98, 1, 0, 35.90);

-- Passo 4: Apos o erro retornado acima, execute o comando ROLLBACK na mesma conexao:
ROLLBACK;

-- 2.3 Conferencia da integridade absoluta (tanto estoque quanto item revertidos):
SELECT id_peca, estoque_atual FROM peca WHERE id_peca = 1;
SELECT COUNT(*) AS itens_teste_depois FROM item_ordem_servico WHERE id_os = 1 AND numero_item = 98;


-- =============================================================================
-- PARTE 3: EXPERIMENTO DE CONCORRENCIA COM DUAS SESSOES INDEPENDENTES (B3)
-- Nivel de Isolamento: REPEATABLE READ (Padrao InnoDB)
--
-- Roteiro de execucao:
-- 1. Abra DUAS conexoes/abas no MySQL Workbench (ou dois terminais mysql).
-- 2. Na Aba 1 (Sessao A), execute o Bloco A1.
-- 3. Na Aba 2 (Sessao B), execute o Bloco B1 e observe o estado de espera (Lock Wait).
-- 4. Na Aba 1 (Sessao A), execute o Bloco A2 (COMMIT).
-- 5. Observe que a Aba 2 e liberada imediatamente. Em seguida, comite a Sessao B.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- [SESSAO A - ABA 1] - Bloco A1
-- -----------------------------------------------------------------------------
USE oficina_mecanica;
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;
START TRANSACTION;

-- Trava a linha da peca 1 com bloqueio exclusivo (X-Lock / Row-Level Lock)
SELECT id_peca, nome, estoque_atual 
FROM peca 
WHERE id_peca = 1 
FOR UPDATE;

-- Atualiza 2 unidades na Sessao A:
UPDATE peca SET estoque_atual = estoque_atual - 2 WHERE id_peca = 1;

-- Registre o nivel de isolamento em vigor nesta sessao:
SELECT @@transaction_isolation AS isolamento_sessao_A;
-- (ATENCAO: NAO execute o COMMIT agora. Va para a Aba 2 / Sessao B).


-- -----------------------------------------------------------------------------
-- [SESSAO B - ABA 2] - Bloco B1 (Executar enquanto a Sessao A estiver aberta)
-- -----------------------------------------------------------------------------
USE oficina_mecanica;
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;
START TRANSACTION;

-- Tentativa de atualizar a MESMA linha da peca 1 bloqueada pela Sessao A:
UPDATE peca SET estoque_atual = estoque_atual - 1 WHERE id_peca = 1;

-- COMPORTAMENTO ESPERADO (confirme e registre o que VOCE observou):
-- O cursor da Aba 2 fica aguardando o bloqueio de linha (row lock) da Sessao A.
-- Enquanto B espera, numa TERCEIRA aba (ou na Sessao A) e possivel ver a espera com:
--   SELECT trx_id, trx_state, trx_started, trx_wait_started
--     FROM information_schema.innodb_trx;
--   SELECT * FROM performance_schema.data_lock_waits;
-- Se B esperar mais que innodb_lock_wait_timeout (padrao 50 s) ocorre ERRO 1205 (Lock wait timeout):
-- SELECT @@innodb_lock_wait_timeout;


-- -----------------------------------------------------------------------------
-- [SESSAO A - ABA 1] - Bloco A2 (Liberacao do bloqueio)
-- -----------------------------------------------------------------------------
COMMIT;

-- COMPORTAMENTO:
-- Imediatamente apos o COMMIT da Sessao A, a Sessao B retoma a execucao
-- e conclui seu UPDATE sobre o valor ja confirmado por A (nenhuma das duas baixas e perdida).
-- Confira na verificacao final: estoque inicial - 2 (sessao A) - 1 (sessao B).


-- -----------------------------------------------------------------------------
-- [SESSAO B - ABA 2] - Bloco B2 (Confirmacao final da Sessao B)
-- -----------------------------------------------------------------------------
COMMIT;


-- -----------------------------------------------------------------------------
-- [VERIFICACAO FINAL DO ESTOQUE] - Executar em qualquer aba
-- -----------------------------------------------------------------------------
SELECT id_peca, nome, estoque_atual FROM peca WHERE id_peca = 1;
