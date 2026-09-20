-- Projeto Final - Laboratorio de Banco de Dados - Etapa 1 - Tema: Oficina Mecanica
-- Grupo 4: Cleber Gabriel Pereira Passos, Joaquim Manoel Lima Viana Vieira, Felipe Rodrigues Garcia
-- Arquivo 03_consultas.sql: 15 consultas comentadas (5 Basicas, 5 Juncoes/Agregacao, 5 Avancadas).
USE oficina_mecanica;

-- Categoria: BASICAS

-- C01. Pergunta de negocio: Quais clientes ainda nao informaram e-mail de contato?
-- (projecao + selecao com WHERE + tratamento de NULL)
SELECT id_cliente, nome, telefone
FROM cliente
WHERE email IS NULL;

-- C02. Pergunta de negocio: Quais veiculos tem placa comecando pela letra 'A' ou 'B'?
-- (selecao com LIKE)
SELECT placa, marca, modelo, tipo_veiculo
FROM veiculo
WHERE placa LIKE 'A%' OR placa LIKE 'B%'
ORDER BY placa;

-- C03. Pergunta de negocio: Quais pecas custam entre R$ 50 e R$ 200, ordenadas da mais
-- barata para a mais cara?
-- (selecao com BETWEEN + ordenacao)
SELECT nome, codigo_fabricante, preco_unitario
FROM peca
WHERE preco_unitario BETWEEN 50 AND 200
ORDER BY preco_unitario ASC;

-- C04. Pergunta de negocio: Quais ordens de servico estao em aberto (abertas, em andamento
-- ou aguardando peca), isto e, ainda nao finalizadas?
-- (selecao com IN)
SELECT id_os, id_veiculo, status, data_abertura
FROM ordem_servico
WHERE status IN ('Aberta', 'Em andamento', 'Aguardando peca')
ORDER BY data_abertura;

-- C05. Pergunta de negocio: Qual o cadastro completo dos mecanicos, do mais antigo para o
-- mais recente na empresa?
-- (projecao + ordenacao)
SELECT id_mecanico, nome, data_admissao
FROM mecanico
ORDER BY data_admissao ASC;

-- Categoria: JUNCOES E AGREGACAO

-- C06. Pergunta de negocio: Para cada ordem de servico concluida, qual o cliente, o veiculo
-- e o mecanico responsavel?
-- (juncao com tres tabelas: ordem_servico + veiculo + cliente, mais mecanico)
SELECT os.id_os, c.nome AS cliente, v.placa, v.modelo, m.nome AS mecanico, os.valor_total
FROM ordem_servico os
JOIN veiculo v  ON v.id_veiculo = os.id_veiculo
JOIN cliente c  ON c.id_cliente = v.id_cliente
JOIN mecanico m ON m.id_mecanico = os.id_mecanico_responsavel
WHERE os.status = 'Concluida'
ORDER BY os.id_os;

-- C07. Pergunta de negocio: Quais ordens de servico ainda nao possuem garantia registrada
-- (inclui as que nunca vao ter, por nao terem sido concluidas)?
-- (LEFT JOIN)
SELECT os.id_os, os.status, g.id_garantia
FROM ordem_servico os
LEFT JOIN garantia g ON g.id_os = os.id_os
WHERE g.id_garantia IS NULL
ORDER BY os.id_os;

-- C08. Pergunta de negocio: Qual o faturamento total (soma dos itens de servico) e a
-- quantidade de itens executados por cada mecanico responsavel?
-- (GROUP BY + HAVING)
SELECT m.id_mecanico, m.nome, COUNT(ios.numero_item) AS qtd_itens,
       SUM(ios.valor_cobrado) AS faturamento_total
FROM mecanico m
JOIN ordem_servico os   ON os.id_mecanico_responsavel = m.id_mecanico
JOIN item_ordem_servico ios ON ios.id_os = os.id_os
GROUP BY m.id_mecanico, m.nome
HAVING SUM(ios.valor_cobrado) > 500
ORDER BY faturamento_total DESC;

-- C09. Pergunta de negocio: Quantas ordens de servico cada veiculo ja teve, considerando
-- apenas veiculos com mais de uma OS registrada?
-- (GROUP BY + HAVING)
SELECT v.placa, v.marca, v.modelo, COUNT(os.id_os) AS total_os
FROM veiculo v
JOIN ordem_servico os ON os.id_veiculo = v.id_veiculo
GROUP BY v.id_veiculo, v.placa, v.marca, v.modelo
HAVING COUNT(os.id_os) > 1
ORDER BY total_os DESC;

-- C10. Pergunta de negocio: Qual o total de pecas consumidas (em quantidade e valor) por
-- fornecedor?
-- (juncao com tres tabelas + agregacao)
SELECT f.nome AS fornecedor, SUM(pu.quantidade) AS total_pecas_usadas,
       SUM(pu.quantidade * pu.preco_aplicado) AS valor_total_consumido
FROM fornecedor f
JOIN peca p          ON p.id_fornecedor = f.id_fornecedor
JOIN peca_utilizada pu ON pu.id_peca = p.id_peca
GROUP BY f.id_fornecedor, f.nome
ORDER BY valor_total_consumido DESC;

-- Categoria: AVANCADAS

-- C11. Pergunta de negocio: Quais mecanicos ja realizaram algum item de servico cujo valor
-- cobrado supera a media geral de todos os itens da oficina?
-- (subconsulta correlacionada)
SELECT DISTINCT m.nome
FROM mecanico m
WHERE EXISTS (
    SELECT 1
    FROM ordem_servico os
    JOIN item_ordem_servico ios ON ios.id_os = os.id_os
    WHERE os.id_mecanico_responsavel = m.id_mecanico
      AND ios.valor_cobrado > (SELECT AVG(valor_cobrado) FROM item_ordem_servico)
);

-- C12. Pergunta de negocio: Quais clientes possuem pelo menos um veiculo que nunca passou
-- por nenhuma ordem de servico?
-- (EXISTS / NOT EXISTS)
SELECT c.id_cliente, c.nome, v.placa
FROM cliente c
JOIN veiculo v ON v.id_cliente = c.id_cliente
WHERE NOT EXISTS (
    SELECT 1 FROM ordem_servico os WHERE os.id_veiculo = v.id_veiculo
);

-- C13. Pergunta de negocio (nao trivial): Qual e o mecanico com maior faturamento medio por
-- ordem de servico concluida, entre os que concluiram pelo menos duas OS?
-- (subconsulta + agregacao aninhada, responde pergunta de negocio nao trivial)
SELECT nome, qtd_os_concluidas, ROUND(faturamento_total / qtd_os_concluidas, 2) AS media_por_os
FROM (
    SELECT m.id_mecanico, m.nome,
           COUNT(DISTINCT os.id_os) AS qtd_os_concluidas,
           SUM(os.valor_total) AS faturamento_total
    FROM mecanico m
    JOIN ordem_servico os ON os.id_mecanico_responsavel = m.id_mecanico
    WHERE os.status = 'Concluida'
    GROUP BY m.id_mecanico, m.nome
    HAVING COUNT(DISTINCT os.id_os) >= 2
) AS resumo
ORDER BY media_por_os DESC
LIMIT 1;

-- C14. Pergunta de negocio: Quais pecas estao com estoque abaixo da media de estoque de
-- todas as pecas do mesmo fornecedor (possiveis candidatas a reposicao prioritaria)?
-- (subconsulta correlacionada)
SELECT p.nome, p.estoque_atual, p.id_fornecedor
FROM peca p
WHERE p.estoque_atual < (
    SELECT AVG(p2.estoque_atual)
    FROM peca p2
    WHERE p2.id_fornecedor = p.id_fornecedor
)
ORDER BY p.id_fornecedor, p.estoque_atual;

-- C15. Pergunta de negocio (nao trivial): Considerando a hierarquia de mecanicos, quais
-- supervisores tem equipes (subordinados diretos) cujo faturamento somado em itens de
-- servico ultrapassa R$ 1.000?
-- (autorrelacionamento + juncao + agregacao + HAVING)
SELECT sup.nome AS supervisor, COUNT(DISTINCT sub.id_mecanico) AS qtd_subordinados,
       SUM(ios.valor_cobrado) AS faturamento_equipe
FROM mecanico sup
JOIN mecanico sub          ON sub.id_supervisor = sup.id_mecanico
JOIN ordem_servico os       ON os.id_mecanico_responsavel = sub.id_mecanico
JOIN item_ordem_servico ios ON ios.id_os = os.id_os
GROUP BY sup.id_mecanico, sup.nome
HAVING SUM(ios.valor_cobrado) > 1000
ORDER BY faturamento_equipe DESC;

