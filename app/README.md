# Oficina Mecânica — Interface Gráfica Java (Swing + JDBC + Maven)

Aplicação desenvolvida para o Projeto Final da disciplina **Laboratório de Banco de Dados (GPE17M40083)** — Universidade Católica de Brasília (UCB).

---

## 1. Pré-requisitos do Banco de Dados
> [!WARNING]
> O script `01_ddl.sql` executa `DROP DATABASE IF EXISTS oficina_mecanica;`, apagando e recriando todo o banco de dados. Execute apenas em ambiente de desenvolvimento/teste!

Para que a aplicação funcione integralmente (incluindo autenticação por perfil, visões consolidadas e auditoria), **todos os scripts de 01 a 07 (e, para conferência, o 09) devem ser executados previamente no MySQL Server 8.0**:

- **Opção A (MySQL Workbench):** Abra cada arquivo da pasta `sql/` (ou `app/sql/`) em uma aba do editor e execute (ícone do raio ou `Ctrl+Shift+Enter`) na seguinte ordem:
  1. `01_ddl.sql` (Criação do esquema, tabelas e restrições)
  2. `02_carga.sql` (Povoamento com mais de 400 registros realistas)
  3. `03_consultas.sql` (15 consultas de negócio)
  4. `04_gatilhos_auditoria.sql` (Tabela de auditoria, triggers B6, histórico automático RN15 e triggers RN20/RN16)
  5. `05_seguranca_perfis.sql` (Criação de roles, usuários de banco, hash+sal e usuário `app_oficina`)
  6. `06_otimizacao_indices.sql` (Índices B-Tree com estudo de execução EXPLAIN)
  7. `07_views_relatorios.sql` (Visão consolidada de OS e visão de faturamento)
  8. `09_verificacao_final.sql` (Conferência automática; todas as linhas devem retornar OK)
  *Nota sobre `08_experimento_concorrencia.sql`: roteiro interativo para execução manual em duas abas/conexões concorrentes para observar bloqueios.*

- **Opção B (Linha de Comando mysql):**
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
  *(o `root` só instala o esquema; a aplicação conecta como `app_oficina`)*

---

## 2. Como Abrir e Executar no Apache NetBeans

1. Abra o **Apache NetBeans**.
2. Acesse **File > Open Project...** e selecione a pasta `app`.
3. Clique com o botão direito no projeto `oficina-app` > **Build with Dependencies** (o Maven baixará o `mysql-connector-j` 8.4.0 automaticamente).
4. Pressione **F6** (ou clique no botão **Run Project**).

---

## 3. Configuração de Conexão JDBC e Perfis de Acesso

### Conexão JDBC (Conta técnica sem privilégios DDL)
A aplicação busca automaticamente o arquivo `db.properties` na pasta `app/`. Se não existir, utilize o modelo `db.properties.example`:
```properties
db.host=localhost
db.port=3306
db.name=oficina_mecanica
db.user=app_oficina
db.password=AppOficina@2026
```
*(A senha `AppOficina@2026` é uma credencial demonstrativa somente para o laboratório local, criada pelo script 05. Mantenha `db.properties` fora do Git; em ambiente real, troque a senha e não use `root` na aplicação.)* Se não houver um arquivo válido, a tela manual inicia com `app_oficina` como usuário e solicita a senha; a senha não está embutida no código Java.

### Autenticação na Aplicação (Tabela `usuario_sistema` - Hash SHA-256 + Sal)
Ao abrir a aplicação, informe as credenciais de demonstração de acordo com o perfil a ser testado:
- **Administrador:** Login `admin` | Senha `Admin@123` *(Acesso total a todas as 13 abas)*
- **Atendente:** Login `atendente` | Senha `Atend@123` *(Clientes, Veículos, Abertura de OS e Consultas)*
- **Mecânico:** Login `mecanico` | Senha `Mec@123` *(Ordens operacionais, Lançamento de itens/peças e Catálogos)*

---

## 4. Funcionalidades da Aplicação
- **CRUDs Completos:** Clientes, Veículos (com subclasses Carro/Moto transacionadas atomicamente), Mecânicos, Especialidades, Fornecedores, Peças e Serviços.
- **Ordens de Serviço:** Abertura, transição de status com registro automático do histórico por gatilho no banco (RN15), lançamento de itens com cálculo de mão de obra e dedução automática de estoque com recálculo consistente do `valor_total` (mão de obra + peças).
- **Controle de Acesso por Perfil (RBAC na aplicação):** abas visíveis conforme o perfil e verificação nas ações (ex.: mecânico não abre/cancela OS nem emite garantia; só o administrador acessa Segurança e Auditoria). A conexão ao MySQL usa a conta técnica `app_oficina`; as roles SQL são demonstradas à parte no Workbench.
- **Auditoria com autor:** o login do operador é enviado a cada conexão (`SET @usuario_aplicacao = ?`) e gravado pelos gatilhos como `app:<login>`.
- **Demonstração Transacional Multitabela (B3):** Botão interativo para simular falha no meio de transação com verificação prévia e posterior de integridade e acionamento de `c.rollback()`.
- **Consultas Parametrizadas & Relatórios:** Filtros dinâmicos com limite superior seguro (`< DATE_ADD(...)`) e integração direta com a view consolidada `vw_relatorio_consolidado_os`.
- **Tratamento Amigável de Erros:** Captura e mapeamento de códigos SQL (1062, 1451/1452, 3819, 1045) sem expor a exceção bruta ao operador (o detalhe técnico vai somente para o log do console).
