-- Projeto Final - Laboratorio de Banco de Dados - Etapa 1 - Tema: Oficina Mecanica
-- Grupo 4: Cleber Gabriel Pereira Passos, Joaquim Manoel Lima Viana Vieira, Felipe Rodrigues Garcia
-- SGBD: MySQL 8.0+ (MySQL Workbench)
-- Arquivo 01_ddl.sql: cria o banco e todas as tabelas. Executavel do inicio ao fim em base limpa.

DROP DATABASE IF EXISTS oficina_mecanica;
CREATE DATABASE oficina_mecanica
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE oficina_mecanica;

-- Cliente (RN01: proprietario unico do veiculo; RN18: CPF/CNPJ unico)
CREATE TABLE cliente (
    id_cliente      INT UNSIGNED AUTO_INCREMENT,
    nome            VARCHAR(120)    NOT NULL,
    cpf_cnpj        VARCHAR(14)     NOT NULL COMMENT 'RN18 - documento unico',
    telefone        VARCHAR(20)     NULL,
    email           VARCHAR(120)    NULL COMMENT 'RN19 - formato validado por CHECK',
    endereco        VARCHAR(200)    NULL,
    data_cadastro   DATE            NOT NULL DEFAULT (CURRENT_DATE),
    CONSTRAINT pk_cliente PRIMARY KEY (id_cliente),
    CONSTRAINT uq_cliente_cpf_cnpj UNIQUE (cpf_cnpj),
    CONSTRAINT ck_cliente_email CHECK (email IS NULL OR email LIKE '%_@__%.__%')
) ENGINE=InnoDB;

CREATE INDEX idx_cliente_nome ON cliente (nome);

-- Mecanico (RN04: autorrelacionamento hierarquico; RN20: nao supervisiona a si mesmo)
CREATE TABLE mecanico (
    id_mecanico     INT UNSIGNED AUTO_INCREMENT,
    nome            VARCHAR(120)    NOT NULL,
    cpf             VARCHAR(11)     NOT NULL COMMENT 'RN18 - CPF unico',
    data_admissao   DATE            NOT NULL,
    id_supervisor   INT UNSIGNED    NULL COMMENT 'RN04 - NULL = lider de equipe',
    CONSTRAINT pk_mecanico PRIMARY KEY (id_mecanico),
    CONSTRAINT uq_mecanico_cpf UNIQUE (cpf),
    CONSTRAINT fk_mecanico_supervisor FOREIGN KEY (id_supervisor)
        REFERENCES mecanico (id_mecanico)
        ON DELETE SET NULL ON UPDATE CASCADE
    -- RN20 nao pode ser expressa como CHECK aqui: a coluna id_supervisor participa
    -- da acao referencial ON DELETE SET NULL, o que o MySQL 8 proibe (erro 3823).
    -- Sera garantida pela aplicacao na Etapa 2.
) ENGINE=InnoDB;

CREATE INDEX idx_mecanico_supervisor ON mecanico (id_supervisor);

-- Especialidade
CREATE TABLE especialidade (
    id_especialidade    INT UNSIGNED AUTO_INCREMENT,
    nome_especialidade  VARCHAR(80) NOT NULL,
    CONSTRAINT pk_especialidade PRIMARY KEY (id_especialidade),
    CONSTRAINT uq_especialidade_nome UNIQUE (nome_especialidade)
) ENGINE=InnoDB;

-- Mecanico_Especialidade (RN05: N:N com atributo proprio)
CREATE TABLE mecanico_especialidade (
    id_mecanico         INT UNSIGNED NOT NULL,
    id_especialidade    INT UNSIGNED NOT NULL,
    nivel_certificacao  ENUM('Basico','Intermediario','Avancado') NOT NULL DEFAULT 'Basico',
    data_certificacao   DATE NOT NULL,
    CONSTRAINT pk_mecanico_especialidade PRIMARY KEY (id_mecanico, id_especialidade),
    CONSTRAINT fk_mecesp_mecanico FOREIGN KEY (id_mecanico)
        REFERENCES mecanico (id_mecanico)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_mecesp_especialidade FOREIGN KEY (id_especialidade)
        REFERENCES especialidade (id_especialidade)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Veiculo, superclasse (RN01: proprietario; RN02: especializacao total/exclusiva; RN03: placa unica)
CREATE TABLE veiculo (
    id_veiculo      INT UNSIGNED AUTO_INCREMENT,
    placa           VARCHAR(8)      NOT NULL COMMENT 'RN03 - unica',
    marca           VARCHAR(50)     NOT NULL,
    modelo          VARCHAR(50)     NOT NULL,
    ano_fabricacao  YEAR            NOT NULL,
    cor             VARCHAR(30)     NULL,
    tipo_veiculo    ENUM('Carro','Moto') NOT NULL COMMENT 'RN02 - discriminador da especializacao',
    id_cliente      INT UNSIGNED    NOT NULL,
    CONSTRAINT pk_veiculo PRIMARY KEY (id_veiculo),
    CONSTRAINT uq_veiculo_placa UNIQUE (placa),
    CONSTRAINT fk_veiculo_cliente FOREIGN KEY (id_cliente)
        REFERENCES cliente (id_cliente)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_veiculo_cliente ON veiculo (id_cliente);

-- Carro e Moto, subclasses de Veiculo (RN02 - estrategia: tabela por subclasse)
CREATE TABLE carro (
    id_veiculo          INT UNSIGNED NOT NULL,
    num_portas          TINYINT UNSIGNED NOT NULL,
    tipo_combustivel    ENUM('Flex','Gasolina','Diesel','Eletrico','Hibrido') NOT NULL,
    CONSTRAINT pk_carro PRIMARY KEY (id_veiculo),
    CONSTRAINT fk_carro_veiculo FOREIGN KEY (id_veiculo)
        REFERENCES veiculo (id_veiculo)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE moto (
    id_veiculo      INT UNSIGNED NOT NULL,
    cilindrada      SMALLINT UNSIGNED NOT NULL,
    CONSTRAINT pk_moto PRIMARY KEY (id_veiculo),
    CONSTRAINT fk_moto_veiculo FOREIGN KEY (id_veiculo)
        REFERENCES veiculo (id_veiculo)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Fornecedor
CREATE TABLE fornecedor (
    id_fornecedor   INT UNSIGNED AUTO_INCREMENT,
    nome            VARCHAR(120) NOT NULL,
    cnpj            VARCHAR(14)  NOT NULL,
    telefone        VARCHAR(20)  NULL,
    CONSTRAINT pk_fornecedor PRIMARY KEY (id_fornecedor),
    CONSTRAINT uq_fornecedor_cnpj UNIQUE (cnpj)
) ENGINE=InnoDB;

-- Peca (RN08: fornecedor unico por peca; RN09: estoque nao-negativo)
CREATE TABLE peca (
    id_peca             INT UNSIGNED AUTO_INCREMENT,
    nome                VARCHAR(120)    NOT NULL,
    codigo_fabricante   VARCHAR(40)     NOT NULL,
    preco_unitario      DECIMAL(10,2)   NOT NULL,
    estoque_atual       INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT 'RN09 - garantido pelo tipo UNSIGNED',
    id_fornecedor       INT UNSIGNED    NOT NULL,
    CONSTRAINT pk_peca PRIMARY KEY (id_peca),
    CONSTRAINT uq_peca_codigo_fabricante UNIQUE (codigo_fabricante),
    CONSTRAINT fk_peca_fornecedor FOREIGN KEY (id_fornecedor)
        REFERENCES fornecedor (id_fornecedor)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_peca_preco CHECK (preco_unitario > 0)
) ENGINE=InnoDB;

CREATE INDEX idx_peca_fornecedor ON peca (id_fornecedor);

-- Servico_Catalogo
CREATE TABLE servico_catalogo (
    id_servico          INT UNSIGNED AUTO_INCREMENT,
    descricao           VARCHAR(150)    NOT NULL,
    valor_padrao        DECIMAL(10,2)   NOT NULL,
    tempo_estimado_horas DECIMAL(4,2)   NOT NULL,
    CONSTRAINT pk_servico_catalogo PRIMARY KEY (id_servico),
    CONSTRAINT ck_servico_valor CHECK (valor_padrao >= 0)
) ENGINE=InnoDB;

-- Ordem_Servico (RN06: veiculo e mecanico obrigatorios;
-- RN07: data_conclusao preenchida SE E SOMENTE SE status for final -
-- o CHECK cobre as duas direcoes da equivalencia, nao so o "somente se")
CREATE TABLE ordem_servico (
    id_os                   INT UNSIGNED AUTO_INCREMENT,
    id_veiculo              INT UNSIGNED NOT NULL,
    id_mecanico_responsavel INT UNSIGNED NOT NULL,
    data_abertura           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_conclusao          DATETIME NULL COMMENT 'RN07',
    status                  ENUM('Aberta','Em andamento','Aguardando peca','Concluida','Cancelada') NOT NULL DEFAULT 'Aberta',
    km_veiculo              INT UNSIGNED NOT NULL,
    valor_total             DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT pk_ordem_servico PRIMARY KEY (id_os),
    CONSTRAINT fk_os_veiculo FOREIGN KEY (id_veiculo)
        REFERENCES veiculo (id_veiculo)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_os_mecanico FOREIGN KEY (id_mecanico_responsavel)
        REFERENCES mecanico (id_mecanico)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_os_data_conclusao CHECK (
        (status IN ('Concluida','Cancelada') AND data_conclusao IS NOT NULL)
        OR (status NOT IN ('Concluida','Cancelada') AND data_conclusao IS NULL)
    )
) ENGINE=InnoDB;

CREATE INDEX idx_os_veiculo ON ordem_servico (id_veiculo);
CREATE INDEX idx_os_mecanico ON ordem_servico (id_mecanico_responsavel);
CREATE INDEX idx_os_status ON ordem_servico (status);

-- Item_Ordem_Servico, entidade fraca (RN10: identificacao por dependencia; RN11, RN13)
CREATE TABLE item_ordem_servico (
    id_os           INT UNSIGNED NOT NULL,
    numero_item     SMALLINT UNSIGNED NOT NULL COMMENT 'RN10 - unico apenas dentro da OS',
    id_servico      INT UNSIGNED NOT NULL,
    quantidade_horas DECIMAL(4,2) NOT NULL,
    valor_cobrado   DECIMAL(10,2) NOT NULL,
    CONSTRAINT pk_item_ordem_servico PRIMARY KEY (id_os, numero_item),
    CONSTRAINT fk_item_os FOREIGN KEY (id_os)
        REFERENCES ordem_servico (id_os)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_item_servico FOREIGN KEY (id_servico)
        REFERENCES servico_catalogo (id_servico)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_item_valor CHECK (valor_cobrado >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_item_servico ON item_ordem_servico (id_servico);

-- Peca_Utilizada (RN12: N:N com atributo proprio; RN14: quantidade > 0)
CREATE TABLE peca_utilizada (
    id_os           INT UNSIGNED NOT NULL,
    numero_item     SMALLINT UNSIGNED NOT NULL,
    id_peca         INT UNSIGNED NOT NULL,
    quantidade      INT UNSIGNED NOT NULL,
    preco_aplicado  DECIMAL(10,2) NOT NULL,
    CONSTRAINT pk_peca_utilizada PRIMARY KEY (id_os, numero_item, id_peca),
    CONSTRAINT fk_pecautil_item FOREIGN KEY (id_os, numero_item)
        REFERENCES item_ordem_servico (id_os, numero_item)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_pecautil_peca FOREIGN KEY (id_peca)
        REFERENCES peca (id_peca)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_pecautil_quantidade CHECK (quantidade > 0)
) ENGINE=InnoDB;

CREATE INDEX idx_pecautil_peca ON peca_utilizada (id_peca);

-- Garantia (RN16: uma por OS concluida; RN17: data_fim > data_inicio)
CREATE TABLE garantia (
    id_garantia         INT UNSIGNED AUTO_INCREMENT,
    id_os               INT UNSIGNED NOT NULL,
    data_inicio         DATE NOT NULL,
    data_fim            DATE NOT NULL,
    status_garantia     ENUM('Vigente','Expirada','Acionada') NOT NULL DEFAULT 'Vigente',
    descricao_cobertura VARCHAR(200) NULL,
    CONSTRAINT pk_garantia PRIMARY KEY (id_garantia),
    CONSTRAINT uq_garantia_os UNIQUE (id_os),
    CONSTRAINT fk_garantia_os FOREIGN KEY (id_os)
        REFERENCES ordem_servico (id_os)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_garantia_datas CHECK (data_fim > data_inicio)
) ENGINE=InnoDB;

-- Historico_Status_OS, entidade fraca / historico datado (RN15)
CREATE TABLE historico_status_os (
    id_os           INT UNSIGNED NOT NULL,
    data_hora       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status_anterior ENUM('Aberta','Em andamento','Aguardando peca','Concluida','Cancelada') NULL,
    status_novo     ENUM('Aberta','Em andamento','Aguardando peca','Concluida','Cancelada') NOT NULL,
    CONSTRAINT pk_historico_status_os PRIMARY KEY (id_os, data_hora),
    CONSTRAINT fk_historico_os FOREIGN KEY (id_os)
        REFERENCES ordem_servico (id_os)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;
