# Projeto Final — Laboratório de Banco de Dados (GPE17M40083)
## Do Modelo Conceitual à Aplicação: Um Banco de Dados Completo

**Tema:** Oficina Mecânica  
**Instituição:** Universidade Católica de Brasília (UCB) — Bacharelado em Engenharia de Software  
**Professor:** Samuel Novais Moura Júnior  
**SGBD:** MySQL Server 8.0+ / InnoDB (MySQL Workbench)  
**Linguagem & Interface:** Java (Swing) + JDBC (Maven / Apache NetBeans)  

**Grupo 4:**
- Cléber Gabriel Pereira Passos
- Joaquim Manoel Lima Viana Vieira
- Felipe Rodrigues Garcia

---

## 1. Descrição do Domínio

O sistema modela a operação técnica, financeira e administrativa de uma oficina mecânica completa:
- **Clientes e Veículos:** Proprietários com especialização exclusiva de veículos em carros (portas, combustível) ou motos (cilindrada).
- **Mecânicos e Especialidades:** Relação N:N com atributos de certificação e autorrelacionamento hierárquico de supervisão (RN04/RN20).
- **Ordens de Serviço (OS):** Operações multitabela envolvendo itens de serviço (catálogo), consumo de peças com baixa de estoque, garantias contratuais e histórico datado de transição de status.
- **Segurança, Auditoria e Desempenho:** Controle de concorrência com bloqueios de linha (InnoDB), gatilhos de auditoria, controle de acesso baseado em papéis (RBAC com menor privilégio) e índices B-Tree otimizados com análise EXPLAIN.

---

## 2. Como Reconstruir e Rodar Tudo do Zero

> [!WARNING]
> **Aviso de Integridade:** O script `01_ddl.sql` inicia com o comando `DROP DATABASE IF EXISTS oficina_mecanica;`. Sua execução descarta e recria toda a base de dados. Execute apenas em ambiente de desenvolvimento, homologação ou após backup.

### Opção A — Execução via MySQL Workbench (Interface Gráfica)
1. Conecte-se à sua instância local do MySQL Server 8.0 no **MySQL Workbench**.
2. Abra cada arquivo da pasta `sql/` (menu **File > Open SQL Script...**) e execute o conteúdo na seguinte sequência numérica:
   - `01_ddl.sql` — Criação das 15 tabelas, chaves e restrições CHECK de domínio.
   - `02_carga.sql` — Carga com mais de 400 registros consistentes e recálculo de valores totais.
   - `03_consultas.sql` — Execução das 15 consultas de negócio para validação.
   - `04_gatilhos_auditoria.sql` — Criação da tabela de auditoria, triggers de auditoria (B6), histórico de mudanças de status (RN15) e validações (RN20 e RN16).
   - `05_seguranca_perfis.sql` — Criação das roles (`role_admin`, `role_atendente`, `role_mecanico`), usuários de demonstração, credenciais com hash+sal e usuário técnico `app_oficina`.
   - `06_otimizacao_indices.sql` — Análise de desempenho B4 com planos EXPLAIN antes e depois da criação de índices B-Tree.
   - `07_views_relatorios.sql` — Criação das visões `vw_relatorio_consolidado_os` e `vw_faturamento_mensal`.
3. Depois do script 07, execute `09_verificacao_final.sql` e confirme que todas as linhas retornam **OK** (guarde o print como evidência).
4. Para o teste concorrente: abra `08_experimento_concorrencia.sql` e siga as instruções para executá-lo em **duas abas/conexões simultâneas** (Sessão A e Sessão B), não executando o script de forma linear inteira.

### Opção B — Execução via Linha de Comando (Cliente Terminal `mysql`)
No terminal ou PowerShell, a partir da raiz do repositório:
```bash
mysql -u root -p < sql/01_ddl.sql
mysql -u root -p < sql/02_carga.sql
mysql -u root -p < sql/03_consultas.sql
mysql -u root -p < sql/04_gatilhos_auditoria.sql
mysql -u root -p < sql/05_seguranca_perfis.sql
mysql -u root -p < sql/06_otimizacao_indices.sql
mysql -u root -p < sql/07_views_relatorios.sql
mysql -u root -p < sql/09_verificacao_final.sql
```
> Nesta opção o `root` é usado **somente para instalar o esquema** (é a conta de administração do banco). A aplicação em si conecta com a conta técnica `app_oficina`, sem privilégios de `DROP`/`ALTER`/`CREATE`.

---

### Passo 2: Abrir e Rodar a Aplicação no Apache NetBeans

1. Abra o **Apache NetBeans**.
2. Clique no menu superior em **File > Open Project...** (ou `Ctrl + Shift + O`).
3. Navegue até a pasta `app` (o NetBeans exibirá o ícone de projeto Maven `oficina-app`). Clique em **Open Project**.
4. Clique com o botão direito no projeto na árvore lateral do NetBeans e selecione **Build with Dependencies** (o Maven baixará automaticamente o driver `mysql-connector-j`).
5. Pressione **F6** (ou clique no botão verde **Run Project**).

---

### Passo 3: Configuração da Conexão e Credenciais

- **Conexão com o Banco de Dados:**
  - A aplicação verifica se existe o arquivo `db.properties` na pasta `app/`. Se existir e tiver `db.password`, tenta conectar usando essa configuração sem exibir a senha.
  - Para criar o seu arquivo local (ignorado pelo Git via `.gitignore`), copie `db.properties.example` para `db.properties` e preencha a sua senha do MySQL:
    ```properties
    db.host=localhost
    db.port=3306
    db.name=oficina_mecanica
    db.user=app_oficina
    db.password=AppOficina@2026
    ```
  - Se o arquivo não for encontrado ou a configuração não conectar, a aplicação abre uma tela de conexão com `app_oficina` preenchido como usuário inicial; informe a senha local. A aplicação não preenche `root` como padrão e não contém uma senha de banco embutida no código.

- **Arquitetura de acesso (resumo honesto):** a aplicação usa **uma conta técnica** (`app_oficina`, só DML) para todas as conexões; o **perfil** do operador (Administrador, Atendente, Mecânico) é aplicado **na aplicação** (abas visíveis e verificação nas ações). As três *roles* SQL (`role_admin`, `role_atendente`, `role_mecanico`) e os usuários `usr_*` são demonstrados **separadamente no MySQL Workbench** (GRANT/REVOKE, operação permitida e negada). O login do operador é gravado em `@usuario_aplicacao` em cada conexão, e os gatilhos de auditoria registram esse autor (`app:<login>`).
- **Login de Usuário da Aplicação (Hash SHA-256 + Sal - B5):** *(credenciais de demonstração, somente para ambiente local descartável)*
  Ao iniciar, o sistema solicita a autenticação do operador cadastrado em `usuario_sistema`:
  - **Administrador:** Login `admin` | Senha `Admin@123` *(Acesso total)*
  - **Atendente:** Login `atendente` | Senha `Atend@123` *(Atendimento e Ordens)*
  - **Mecânico:** Login `mecanico` | Senha `Mec@123` *(Lançamento operacional)*

---

## 3. Estrutura Completa do Repositório

```
repositorio/
├── .gitignore                     <- Proteção de credenciais e arquivos de build
├── README.md                      <- Guia geral, manual de execução e roteiro de banca
├── app/                           <- Projeto Java completo (Maven para NetBeans)
│   ├── .gitignore
│   ├── README.md
│   ├── db.properties.example      <- Modelo de credenciais fora do versionamento (B2)
│   ├── pom.xml                    <- Configuração Maven e dependência do conector MySQL
│   ├── sql/                       <- Cópia sincronizada dos scripts SQL
│   └── src/
│       └── main/
│           ├── java/oficina/
│           │   ├── Main.java                        <- Ponto de entrada e login com hash
│           │   ├── util/
│           │   │   ├── Conexao.java                 <- Gerenciador JDBC com db.properties (B2)
│           │   │   ├── Db.java                      <- PreparedStatements e mensagens amigáveis (B1/B2)
│           │   │   └── SegurancaUtil.java           <- Criptografia de senhas SHA-256 + Sal (B5)
│           │   └── view/
│           │       ├── JanelaPrincipal.java         <- Janela mestre com abas e controle de sessão
│           │       ├── CrudPanel.java               <- CRUD genérico com validação
│           │       ├── VeiculoPanel.java            <- Especialização Carro/Moto transacional
│           │       ├── OrdemServicoPanel.java       <- Operações na OS e botão de simulação de Rollback (B3)
│           │       ├── ConsultaParametrizadaOSPanel.java    <- Consulta 1: Filtro de OS e agregação (B1)
│           │       ├── ConsultaParametrizadaPecasPanel.java <- Consulta 2: Filtro de Peças e consumo (B1)
│           │       ├── RelatorioConsolidadoPanel.java       <- Relatório Consolidado via VIEW (B1)
│           │       ├── SegurancaAuditoriaPanel.java         <- Logs dos Triggers e prova de SQL Injection (B2/B6)
│           │       └── ConsultasPanel.java          <- Execução das 15 consultas estáticas da Etapa 1
│           └── resources/
│               └── consultas.sql
├── docs/
│   ├── relatorio-etapa1.pdf       <- Relatório formal entregue na Etapa 1
│   ├── relatorio-etapa2.pdf       <- Relatório final consolidado: Etapa 2 + Anexo I (Etapa 1: A1–A5)
│   ├── relatorio-etapa2.html      <- Fonte do relatório da Etapa 2
│   ├── build_relatorio.py         <- Regenera o PDF (insere prints de docs/evidencias/ e anexa a Etapa 1)
│   ├── evidencias/                <- Prints reais das execuções (ver LEIA-ME.txt)
│   ├── mer-conceitual.pdf / .png  <- Diagrama ER (Peter Chen)
│   ├── mer-conceitual.dot         <- Fonte Graphviz do diagrama conceitual
│   ├── modelo-logico.pdf          <- Esquema relacional com decisões de mapeamento
│   └── dicionario-dados.pdf       <- Dicionário de dados completo
└── sql/
    ├── 01_ddl.sql                 <- 15 tabelas, chaves e restrições nomeadas
    ├── 02_carga.sql               <- Carga realista com mais de 400 registros
    ├── 03_consultas.sql           <- 15 consultas comentadas da Etapa 1
    ├── 04_gatilhos_auditoria.sql  <- Auditoria (2 gatilhos B6) + histórico RN15 + gatilhos RN20 e RN16
    ├── 05_seguranca_perfis.sql    <- 3 Roles, usuários com Salt+Hash e visões restritivas (B5)
    ├── 06_otimizacao_indices.sql  <- 3 Consultas críticas, EXPLAIN + EXPLAIN ANALYZE antes/depois e índices (B4)
    ├── 07_views_relatorios.sql    <- Visão consolidada de 7 tabelas e faturamento (B1)
    ├── 08_experimento_concorrencia.sql <- Roteiro de concorrência com 2 sessões e locks (B3) — NÃO executar linearmente
    └── 09_verificacao_final.sql    <- Verificação automática pós-instalação (somente SELECT)
```

---

## 4. Roteiro Cronometrado para a Apresentação ao Vivo (15 Minutos)

| Momento | Tempo | Roteiro Prático de Demonstração na Apresentação |
|---|---|---|
| **Contexto** | 2 min | Apresentar o domínio de Oficina Mecânica, as principais entidades e a evolução da Etapa 1 para a Etapa 2. |
| **CRUDs e Consultas** | 4 min | 1. Mostrar o cadastro de veículo na aba *Veículos* (gravação atômica da superclasse e subclasse Carro/Moto).<br>2. Abrir a aba *Consulta 1 (OS & Faturamento)* e filtrar por período e status, mostrando a agregação em tempo real nos cards.<br>3. Abrir a aba *Relatório Consolidado (VIEW)* e demonstrar a integração de 7 tabelas vindas da <code>vw_relatorio_consolidado_os</code>. |
| **Transação** | 3 min | 1. Na aba *Ordens de Serviço*, incluir um item e uma peça com sucesso (mostrando baixa de estoque e recálculo do total).<br>2. Clicar no botão **"⚠️ Simular Falha (Rollback B3)"**: demonstrar a exceção de restrição CHECK provocada no meio do fluxo e o ROLLBACK imediato mantendo o banco 100% íntegro. |
| **Restrições em Ação** | 2 min | 1. Demonstrar o tratamento de erro amigável (tentar cadastrar cliente com CPF duplicado).<br>2. Na aba *Segurança*, testar o payload de SQL Injection <code>' OR '1'='1</code>, comprovando que a consulta parametrizada neutraliza o ataque. |
| **Desempenho e Acesso** | 3 min | 1. No MySQL Workbench, exibir a árvore do `EXPLAIN` antes e depois do índice `idx_os_status_abertura` (registrar o que o plano real mostrar; o ganho depende do volume de dados).<br>2. Demonstrar o login sob o perfil `admin` e sob o perfil `atendente` (menor privilégio). |
| **Arguição** | 1 min | Responder aos questionamentos técnicos do professor Samuel. |

---

## 5. Como Enviar o Projeto ao Professor no AVA

O edital exige a entrega no AVA de:
1. **Link do Repositório Git:**
   - Crie um repositório no GitHub (ou GitLab), inicialize o git na pasta `repositorio` (com `git init`, `git add .`, `git commit -m "Entrega final Etapa 2"`, `git push`) e certifique-se de que o repositório está **Público** para acesso do professor.
2. **Relatório Técnico em PDF:**
   - Antes do envio, salve os prints reais em `docs/evidencias/` (nomes em `LEIA-ME.txt`) e rode `python3 docs/build_relatorio.py` para atualizar o PDF.
   - Faça o upload do arquivo `docs/relatorio-etapa2.pdf` diretamente na atividade do AVA.
3. **Backup em ZIP (se solicitado):**
   - Caso o AVA solicite envio do arquivo compactado, envie o conteúdo da pasta `repositorio` compactado em `.zip` (já com o `oficina-app.zip` removido).

---

## 6. Declaração de Uso de Inteligência Artificial

Em atendimento ao item 9 do edital: a equipe utilizou assistentes de IA generativa como apoio (estruturação do projeto Maven, geração de sal/hash, revisão de código e scripts, auditoria técnica do repositório e redação/formatação do relatório). Na revisão final de 18/10/2026 foi usado o assistente **Claude (Anthropic)** para auditar o repositório contra o enunciado, corrigir código e scripts e reescrever o relatório da Etapa 2.

**Como o resultado foi verificado:** o código Java foi compilado (`javac`, JDK 21) sem erros; os scripts SQL são verificados pela equipe no MySQL 8.0 com o script `09_verificacao_final.sql` e pelos roteiros dos scripts 06 e 08; o que depende de execução no MySQL (planos EXPLAIN, tempos, concorrência, logins reais por perfil) está registrado no relatório **somente com as evidências efetivamente obtidas**. A seção 11 do relatório detalha o que foi implementado e o que foi testado.
