# Alterações aplicadas ao repositório final

Data da revisão: 18/10/2026

Este arquivo descreve as alterações feitas nesta revisão do `repositorio_FINAL.zip`. Ele registra correções de código/documentação, mas **não substitui a execução de testes no MySQL**.

## 1. Permissões SQL de `role_atendente`

Arquivos: `sql/05_seguranca_perfis.sql` e `app/sql/05_seguranca_perfis.sql`.

- Removida a referência à coluna `descricao_problema`, que não existe em `ordem_servico` no DDL do projeto.
- Separados os privilégios `SELECT`, `INSERT` e `UPDATE` em permissões por coluna para `ordem_servico`.
- A role de atendimento não recebe permissão para alterar `valor_total` nem para alterar o mecânico responsável depois que a OS foi criada.
- Removido o `REVOKE` de privilégios DDL que nunca tinham sido concedidos à conta técnica recém-criada `app_oficina`. A conta recebe somente os privilégios DML explicitamente concedidos pelo script.

## 2. Histórico de status (RN15) garantido pelo banco

Arquivos: `sql/04_gatilhos_auditoria.sql`, `app/sql/04_gatilhos_auditoria.sql` e `app/src/main/java/oficina/view/OrdemServicoPanel.java`.

- Adicionado o gatilho `trg_historico_status_os_au`, que grava `status_anterior` e `status_novo` em `historico_status_os` sempre que o status da OS realmente muda.
- Removida a inserção manual de histórico da ação de alteração de status na aplicação, evitando dois registros para uma só transição.
- Mantido o registro inicial `NULL -> Aberta` durante a criação de uma OS na aplicação. A carga inicial do banco também mantém os registros definidos em `02_carga.sql`.

## 3. Identificação do usuário em auditoria

Arquivos: `sql/04_gatilhos_auditoria.sql` e `app/sql/04_gatilhos_auditoria.sql`.

- O fallback da variável `@usuario_aplicacao` passou de `CURRENT_USER()` para `USER()`. Nos gatilhos MySQL, `CURRENT_USER()` pode representar o definidor do gatilho; `USER()` identifica a conta da conexão que executou a operação.
- A documentação deixa explícito que `app:<login>` é uma identificação lógica fornecida pela aplicação e não uma prova criptográfica da identidade humana.

## 4. Conexão JDBC e credenciais

Arquivos: `app/src/main/java/oficina/Main.java`, `app/src/main/java/oficina/util/Conexao.java`, `app/db.properties.example` e READMEs.

- O formulário manual de conexão não sugere mais `root`; começa com `app_oficina`.
- Removida do código Java a senha padrão da conta técnica. O valor deve vir de `db.properties` ou ser digitado na tela de conexão.
- Atualizados os READMEs sobre o fluxo de conexão e a versão do conector MySQL Maven (8.4.0).
- A declaração de uso de IA do relatório agora inclui esta assistência do ChatGPT (OpenAI) junto à ferramenta já declarada pela equipe.

## 5. Verificação e relatório

- Atualizado `09_verificacao_final.sql` para esperar 6 gatilhos: 2 de auditoria, 2 para RN20, 1 para RN16 e 1 para RN15.
- As cópias dos nove scripts SQL em `sql/` e `app/sql/` foram comparadas e estão idênticas.
- Atualizados `docs/relatorio-etapa2.html` e `docs/relatorio-etapa2.pdf` para refletir as alterações e manter os itens de evidência ausentes marcados como pendentes.

## 6. Verificações efetuadas nesta revisão

- Compilação de todos os arquivos Java com `javac` usando compatibilidade com Java 11: concluída, com um aviso de configuração do caminho de módulos do compilador.
- Sincronização das cópias dos scripts SQL: confirmada por comparação byte a byte.
- Pesquisa de referências antigas ao privilégio inválido e à contagem anterior de gatilhos: concluída.
- Geração e inspeção visual do PDF atualizado: concluídas.

## 7. Testes ainda necessários no ambiente da equipe

Não havia um servidor MySQL instalado/disponível neste ambiente, e o Maven também não estava disponível. Portanto, **não foi possível afirmar que os scripts executam sem erros em MySQL nem que a aplicação conecta e funciona de ponta a ponta**.

Antes da entrega, a equipe ainda deve:

1. Executar os scripts na ordem documentada em uma base de laboratório MySQL 8.0+.
2. Confirmar que todas as verificações de `09_verificacao_final.sql` retornam `OK`.
3. Testar com logins reais `usr_atendente` e `usr_mecanico` as operações permitidas e negadas, incluindo o `GRANT`/`REVOKE`.
4. Alterar o status de uma OS pela aplicação e por SQL direto; confirmar que cada mudança gera exatamente um registro de histórico.
5. Executar as demonstrações exigidas pelo enunciado (transação/rollback, concorrência, SQL Injection e `EXPLAIN ANALYZE`) e anexar apenas capturas reais em `docs/evidencias/`.
6. Regenerar `docs/relatorio-etapa2.pdf` depois de adicionar as evidências reais.

Nenhuma captura de teste foi inventada ou marcada como concluída nesta revisão.
