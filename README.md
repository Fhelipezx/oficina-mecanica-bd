# Projeto Final — Laboratório de Banco de Dados (GPE17M40053)
## Etapa 1 — Projeto e Construção do Banco (N1)

**Tema:** Oficina Mecânica
**Grupo 4:**
- Cléber Gabriel Pereira Passos
- Joaquim Manoel Lima Viana Vieira
- Felipe Rodrigues Garcia

**Professor:** Samuel Novais Moura Júnior
**SGBD:** MySQL 8.0+ (MySQL Workbench)
**Entrega:** 20/09/2026

## Descrição do domínio

O banco modela a operação de uma oficina mecânica: clientes e seus veículos
(carros ou motos), ordens de serviço conduzidas por mecânicos organizados em
hierarquia, itens de serviço extraídos de um catálogo, peças consumidas por
fornecedor, garantias sobre serviços concluídos e um histórico datado de
mudanças de status de cada ordem de serviço.

## Como reconstruir o banco do zero

Com o MySQL Workbench (ou `mysql` via linha de comando) conectado a uma
instância MySQL 8.0 ou superior, execute os scripts na ordem abaixo:

```bash
mysql -u seu_usuario -p < sql/01_ddl.sql
mysql -u seu_usuario -p < sql/02_carga.sql
mysql -u seu_usuario -p < sql/03_consultas.sql
```

- `01_ddl.sql` cria o banco `oficina_mecanica` do zero e todas as 15 tabelas.
- `02_carga.sql` popula o banco com dados fictícios e realistas (40 clientes,
  40 veículos, 12 mecânicos, 40 peças, 20 serviços de catálogo, 42 ordens de
  serviço, 103 itens de serviço, 118 registros de peças utilizadas, 18
  garantias e 122 eventos de histórico, entre outros).
- `03_consultas.sql` contém as 15 consultas de verificação exigidas.

Todos os três scripts foram testados do início ao fim em uma instância limpa
de MySQL 8.0 antes desta entrega.

## Estrutura do repositório

```
repositorio/
├── README.md
├── docs/
│   ├── relatorio-etapa1.pdf       (artefatos A1 a A5 reunidos)
│   ├── mer-conceitual.pdf / .png  (diagrama ER)
│   ├── mer-conceitual.dot         (arquivo-fonte do diagrama, Graphviz)
│   ├── modelo-logico.pdf
│   └── dicionario-dados.pdf
└── sql/
    ├── 01_ddl.sql
    ├── 02_carga.sql
    └── 03_consultas.sql
```

## Declaração de uso de IA

Ver seção final do `docs/relatorio-etapa1.pdf`.
