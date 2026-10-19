-- Projeto Final - Laboratorio de Banco de Dados - Etapa 2 - Tema: Oficina Mecanica
-- Grupo 4: Cleber Gabriel Pereira Passos, Joaquim Manoel Lima Viana Vieira, Felipe Rodrigues Garcia
-- SGBD: MySQL 8.0+ (MySQL Workbench)
-- Arquivo 07_views_relatorios.sql: Visoes consolidadas para relatorios e interface (B1 e B5).

USE oficina_mecanica;

-- -----------------------------------------------------------------------------
-- Visao 1: Relatorio Consolidado de Ordens de Servico (B1)
-- Consolida em uma unica visao todas as informacoes operacionais, financeiras
-- e de garantia de cada ordem de servico, integrando 7 tabelas do esquema.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_relatorio_consolidado_os AS
SELECT 
    os.id_os AS id_os,
    os.status AS status_os,
    os.data_abertura AS data_abertura,
    os.data_conclusao AS data_conclusao,
    c.nome AS cliente_nome,
    c.telefone AS cliente_telefone,
    v.placa AS veiculo_placa,
    CONCAT(v.marca, ' ', v.modelo) AS veiculo_modelo,
    v.tipo_veiculo AS tipo_veiculo,
    m.nome AS mecanico_responsavel,
    COALESCE(itens.qtd_itens, 0) AS qtd_itens_servico,
    COALESCE(itens.total_horas, 0) AS total_horas_trabalhadas,
    COALESCE(itens.valor_mao_obra, 0.00) AS valor_mao_obra,
    COALESCE(pecas.qtd_pecas, 0) AS total_pecas_utilizadas,
    COALESCE(pecas.valor_pecas, 0.00) AS valor_pecas,
    (COALESCE(itens.valor_mao_obra, 0.00) + COALESCE(pecas.valor_pecas, 0.00)) AS valor_total_calculado,
    os.valor_total AS valor_total_os,
    COALESCE(g.status_garantia, 'Sem Garantia') AS status_garantia,
    g.data_fim AS garantia_validade
FROM ordem_servico os
JOIN veiculo v ON v.id_veiculo = os.id_veiculo
JOIN cliente c ON c.id_cliente = v.id_cliente
JOIN mecanico m ON m.id_mecanico = os.id_mecanico_responsavel
LEFT JOIN (
    SELECT 
        id_os,
        COUNT(numero_item) AS qtd_itens,
        SUM(quantidade_horas) AS total_horas,
        SUM(valor_cobrado) AS valor_mao_obra
    FROM item_ordem_servico
    GROUP BY id_os
) itens ON itens.id_os = os.id_os
LEFT JOIN (
    SELECT 
        id_os,
        SUM(quantidade) AS qtd_pecas,
        SUM(quantidade * preco_aplicado) AS valor_pecas
    FROM peca_utilizada
    GROUP BY id_os
) pecas ON pecas.id_os = os.id_os
LEFT JOIN garantia g ON g.id_os = os.id_os;

-- -----------------------------------------------------------------------------
-- Visao 2: Faturamento Consolidado Mensal da Oficina
-- Analise gerencial agregada por mes/ano de faturamento, separando servicos e pecas.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_faturamento_mensal AS
SELECT 
    DATE_FORMAT(os.data_conclusao, '%Y-%m') AS mes_ano,
    COUNT(DISTINCT os.id_os) AS total_os_concluidas,
    COALESCE(SUM(itens.valor_mao_obra), 0.00) AS total_faturado_mao_obra,
    COALESCE(SUM(p.total_pecas_valor), 0.00) AS total_faturado_pecas,
    SUM(os.valor_total) AS receita_bruta_total
FROM ordem_servico os
LEFT JOIN (
    SELECT id_os, SUM(valor_cobrado) AS valor_mao_obra
    FROM item_ordem_servico
    GROUP BY id_os
) itens ON itens.id_os = os.id_os
LEFT JOIN (
    SELECT id_os, SUM(quantidade * preco_aplicado) AS total_pecas_valor
    FROM peca_utilizada
    GROUP BY id_os
) p ON p.id_os = os.id_os
WHERE os.status = 'Concluida' AND os.data_conclusao IS NOT NULL
GROUP BY DATE_FORMAT(os.data_conclusao, '%Y-%m')
ORDER BY mes_ano DESC;
