-- Projeto Final - Laboratorio de Banco de Dados - Etapa 2 - Tema: Oficina Mecanica
-- Grupo 4: Cleber Gabriel Pereira Passos, Joaquim Manoel Lima Viana Vieira, Felipe Rodrigues Garcia
-- Arquivo 04_gatilhos_auditoria.sql: historico automatico RN15, auditoria (B6) e validacoes RN20/RN16.

USE oficina_mecanica;

-- Gatilho RN15: toda mudanca real de status da OS registra a transicao no banco.
-- Isso tambem cobre updates feitos diretamente no MySQL, fora da aplicacao.
DROP TRIGGER IF EXISTS trg_historico_status_os_au;

DELIMITER $$
CREATE TRIGGER trg_historico_status_os_au
AFTER UPDATE ON ordem_servico
FOR EACH ROW
BEGIN
    IF OLD.status <> NEW.status THEN
        INSERT INTO historico_status_os (id_os, data_hora, status_anterior, status_novo)
        VALUES (NEW.id_os, CURRENT_TIMESTAMP(6), OLD.status, NEW.status);
    END IF;
END$$
DELIMITER ;

CREATE TABLE IF NOT EXISTS auditoria_sistema (
    id_auditoria        INT UNSIGNED AUTO_INCREMENT,
    tabela_afetada      VARCHAR(50)     NOT NULL,
    operacao            ENUM('INSERT','UPDATE','DELETE') NOT NULL,
    id_registro         INT UNSIGNED    NOT NULL,
    usuario_banco       VARCHAR(100)    NOT NULL,
    data_hora           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valores_anteriores  TEXT            NULL,
    valores_posteriores TEXT            NOT NULL,
    motivo_justificativa VARCHAR(255)   NULL,
    CONSTRAINT pk_auditoria_sistema PRIMARY KEY (id_auditoria),
    INDEX idx_auditoria_tabela_reg (tabela_afetada, id_registro),
    INDEX idx_auditoria_data (data_hora)
) ENGINE=InnoDB;

-- Gatilho 1 (B6): Auditoria em ordem_servico
-- Justificativa: Mudancas de status, mecanico responsavel e valor total afetam o faturamento da oficina e a responsabilidade tecnica.
DROP TRIGGER IF EXISTS trg_auditoria_ordem_servico_update;

DELIMITER $$
CREATE TRIGGER trg_auditoria_ordem_servico_update
AFTER UPDATE ON ordem_servico
FOR EACH ROW
BEGIN
    IF (OLD.status <> NEW.status) OR (OLD.valor_total <> NEW.valor_total) OR (OLD.id_mecanico_responsavel <> NEW.id_mecanico_responsavel) THEN
        INSERT INTO auditoria_sistema (
            tabela_afetada,
            operacao,
            id_registro,
            usuario_banco,
            data_hora,
            valores_anteriores,
            valores_posteriores,
            motivo_justificativa
        ) VALUES (
            'ordem_servico',
            'UPDATE',
            OLD.id_os,
            COALESCE(@usuario_aplicacao, USER()),
            NOW(),
            CONCAT('Status: ', OLD.status, ' | ValorTotal: R$', OLD.valor_total, ' | IdMecanico: ', OLD.id_mecanico_responsavel, ' | DataConclusao: ', COALESCE(OLD.data_conclusao, 'NULL')),
            CONCAT('Status: ', NEW.status, ' | ValorTotal: R$', NEW.valor_total, ' | IdMecanico: ', NEW.id_mecanico_responsavel, ' | DataConclusao: ', COALESCE(NEW.data_conclusao, 'NULL')),
            'Alteracao operacional de status, responsavel ou valor total'
        );
    END IF;
END$$
DELIMITER ;

-- Gatilho 2 (B6): Auditoria em peca
-- Justificativa: Alteracoes em precos unitarios ou baixas manuais de estoque representam risco de perdas materiais e inconsistencias financeiras.
DROP TRIGGER IF EXISTS trg_auditoria_peca_update;

DELIMITER $$
CREATE TRIGGER trg_auditoria_peca_update
AFTER UPDATE ON peca
FOR EACH ROW
BEGIN
    IF (OLD.preco_unitario <> NEW.preco_unitario) OR (OLD.estoque_atual <> NEW.estoque_atual) THEN
        INSERT INTO auditoria_sistema (
            tabela_afetada,
            operacao,
            id_registro,
            usuario_banco,
            data_hora,
            valores_anteriores,
            valores_posteriores,
            motivo_justificativa
        ) VALUES (
            'peca',
            'UPDATE',
            OLD.id_peca,
            COALESCE(@usuario_aplicacao, USER()),
            NOW(),
            CONCAT('Peca: ', OLD.nome, ' | Preco: R$', OLD.preco_unitario, ' | Estoque: ', OLD.estoque_atual),
            CONCAT('Peca: ', NEW.nome, ' | Preco: R$', NEW.preco_unitario, ' | Estoque: ', NEW.estoque_atual),
            'Alteracao de saldo de estoque ou reajuste de preco'
        );
    END IF;
END$$
DELIMITER ;

-- Gatilho 3 (RN20): Mecanico nao pode supervisionar a si mesmo
DROP TRIGGER IF EXISTS trg_mecanico_supervisor_bi;

DELIMITER $$
CREATE TRIGGER trg_mecanico_supervisor_bi
BEFORE INSERT ON mecanico
FOR EACH ROW
BEGIN
    IF NEW.id_supervisor IS NOT NULL AND NEW.id_mecanico IS NOT NULL AND NEW.id_supervisor = NEW.id_mecanico THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RN20 violada: um mecanico nao pode ser supervisor de si mesmo.';
    END IF;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trg_mecanico_supervisor_bu;

DELIMITER $$
CREATE TRIGGER trg_mecanico_supervisor_bu
BEFORE UPDATE ON mecanico
FOR EACH ROW
BEGIN
    IF NEW.id_supervisor IS NOT NULL AND NEW.id_supervisor = NEW.id_mecanico THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RN20 violada: um mecanico nao pode ser supervisor de si mesmo.';
    END IF;
END$$
DELIMITER ;

-- Gatilho 4 (RN16): Garantia emitida exclusivamente para OS concluida
DROP TRIGGER IF EXISTS trg_garantia_os_concluida_bi;

DELIMITER $$
CREATE TRIGGER trg_garantia_os_concluida_bi
BEFORE INSERT ON garantia
FOR EACH ROW
BEGIN
    DECLARE v_status VARCHAR(20);
    SELECT status INTO v_status FROM ordem_servico WHERE id_os = NEW.id_os;
    IF v_status IS NULL OR v_status <> 'Concluida' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RN16 violada: garantia so pode ser associada a ordem de servico concluida.';
    END IF;
END$$
DELIMITER ;
-- Gatilho 5 (RN16): impede UPDATE que mova garantia para OS nao concluida
DROP TRIGGER IF EXISTS trg_garantia_os_concluida_bu;

DELIMITER $$
CREATE TRIGGER trg_garantia_os_concluida_bu
BEFORE UPDATE ON garantia
FOR EACH ROW
BEGIN
    DECLARE v_status VARCHAR(20);
    SELECT status INTO v_status FROM ordem_servico WHERE id_os = NEW.id_os;
    IF v_status IS NULL OR v_status <> 'Concluida' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RN16 violada: garantia so pode ser associada a ordem de servico concluida.';
    END IF;
END$$
DELIMITER ;

-- Gatilho 6 (RN16): impede tirar a OS de Concluida enquanto existir garantia
DROP TRIGGER IF EXISTS trg_os_status_garantia_bu;

DELIMITER $$
CREATE TRIGGER trg_os_status_garantia_bu
BEFORE UPDATE ON ordem_servico
FOR EACH ROW
BEGIN
    IF NEW.status <> 'Concluida' AND NEW.status <> OLD.status
       AND EXISTS (SELECT 1 FROM garantia WHERE id_os = NEW.id_os) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RN16 violada: OS com garantia deve permanecer Concluida.';
    END IF;
END$$
DELIMITER ;