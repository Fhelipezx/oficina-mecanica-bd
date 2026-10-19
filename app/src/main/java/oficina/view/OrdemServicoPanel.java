package oficina.view;

import oficina.util.Db;
import oficina.util.SegurancaUtil.UsuarioSessao;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.SQLException;

public class OrdemServicoPanel extends JPanel implements Recarregavel {

    private interface Acao {
        void run() throws SQLException;
    }

    private static final String[] STATUS = {"Aberta", "Em andamento", "Aguardando peca", "Concluida", "Cancelada"};

    private final UsuarioSessao sessao;
    private final JComboBox<Item> cbVeiculo = new JComboBox<>();
    private final JComboBox<Item> cbMecanico = new JComboBox<>();
    private final JComboBox<Item> cbServico = new JComboBox<>();
    private final JComboBox<Item> cbPeca = new JComboBox<>();
    private final JComboBox<Item> cbStatus = new JComboBox<>();
    private final JTextField txKm = new JTextField(7);
    private final JTextField txHoras = new JTextField(5);
    private final JTextField txValorItem = new JTextField(8);
    private final JTextField txQtd = new JTextField("1", 4);
    private final JTextField txPrecoPeca = new JTextField(8);
    private final JTable tbOS = new JTable();
    private final JTable tbItens = new JTable();
    private final JTable tbPecas = new JTable();
    private final JLabel lbInfo = new JLabel(" ");
    private boolean carregando;

    public OrdemServicoPanel() {
        this(null);
    }

    public OrdemServicoPanel(UsuarioSessao sessao) {
        super(new BorderLayout(6, 6));
        this.sessao = sessao;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        for (String s : STATUS) {
            cbStatus.addItem(new Item(s, s));
        }
        cbVeiculo.setPrototypeDisplayValue(new Item("0", "AAA0A00 - Marca Modelo (Nome do cliente aqui)"));
        cbMecanico.setPrototypeDisplayValue(new Item("0", "Nome do mecânico completo"));
        cbServico.setPrototypeDisplayValue(new Item("0", "Descrição do serviço padrão (R$ 000.00)"));
        cbPeca.setPrototypeDisplayValue(new Item("0", "Nome da peça padrão [estoque 000]"));
        for (JTable t : new JTable[]{tbOS, tbItens, tbPecas}) {
            t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }

        // topo: nova OS
        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        norte.setBorder(BorderFactory.createTitledBorder("Nova ordem de serviço"));
        norte.add(new JLabel("Veículo"));
        norte.add(cbVeiculo);
        norte.add(new JLabel("Mecânico"));
        norte.add(cbMecanico);
        norte.add(new JLabel("KM"));
        norte.add(txKm);
        JButton bAbrir = new JButton("Abrir OS");
        bAbrir.addActionListener(e -> abrirOS());
        norte.add(bAbrir);
        add(norte, BorderLayout.NORTH);

        // lista de OS + status + garantia
        JPanel painelOS = new JPanel(new BorderLayout(4, 4));
        painelOS.setBorder(BorderFactory.createTitledBorder("Ordens de serviço"));
        painelOS.add(new JScrollPane(tbOS), BorderLayout.CENTER);
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        barra.add(new JLabel("Novo status"));
        barra.add(cbStatus);
        JButton bStatus = new JButton("Alterar status");
        JButton bGarantia = new JButton("Registrar garantia");
        JButton bSimularFalha = new JButton("⚠️ Simular Falha (Rollback B3)");
        bStatus.addActionListener(e -> alterarStatus());
        bGarantia.addActionListener(e -> registrarGarantia());
        bSimularFalha.addActionListener(e -> simularFalhaTransacional());
        barra.add(bStatus);
        barra.add(bGarantia);
        barra.add(bSimularFalha);
        barra.add(lbInfo);
        painelOS.add(barra, BorderLayout.SOUTH);

        // itens
        JPanel painelItens = new JPanel(new BorderLayout(4, 4));
        painelItens.setBorder(BorderFactory.createTitledBorder("Itens de serviço da OS selecionada"));
        painelItens.add(new JScrollPane(tbItens), BorderLayout.CENTER);
        JPanel fItens = new JPanel(new GridLayout(2, 1));
        JPanel fi1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        fi1.add(new JLabel("Serviço"));
        fi1.add(cbServico);
        JPanel fi2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        fi2.add(new JLabel("Horas"));
        fi2.add(txHoras);
        fi2.add(new JLabel("Valor (R$)"));
        fi2.add(txValorItem);
        JButton bAddItem = new JButton("Adicionar item");
        JButton bRemItem = new JButton("Remover item");
        bAddItem.addActionListener(e -> adicionarItem());
        bRemItem.addActionListener(e -> removerItem());
        fi2.add(bAddItem);
        fi2.add(bRemItem);
        fItens.add(fi1);
        fItens.add(fi2);
        painelItens.add(fItens, BorderLayout.SOUTH);

        // peças
        JPanel painelPecas = new JPanel(new BorderLayout(4, 4));
        painelPecas.setBorder(BorderFactory.createTitledBorder("Peças usadas no item selecionado"));
        painelPecas.add(new JScrollPane(tbPecas), BorderLayout.CENTER);
        JPanel fPecas = new JPanel(new GridLayout(2, 1));
        JPanel fp1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        fp1.add(new JLabel("Peça"));
        fp1.add(cbPeca);
        JPanel fp2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        fp2.add(new JLabel("Qtd"));
        fp2.add(txQtd);
        fp2.add(new JLabel("Preço (R$)"));
        fp2.add(txPrecoPeca);
        JButton bAddPeca = new JButton("Adicionar peça");
        JButton bRemPeca = new JButton("Remover peça");
        bAddPeca.addActionListener(e -> adicionarPeca());
        bRemPeca.addActionListener(e -> removerPeca());
        fp2.add(bAddPeca);
        fp2.add(bRemPeca);
        fPecas.add(fp1);
        fPecas.add(fp2);
        painelPecas.add(fPecas, BorderLayout.SOUTH);

        JSplitPane baixo = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, painelItens, painelPecas);
        baixo.setResizeWeight(0.5);
        JSplitPane centro = new JSplitPane(JSplitPane.VERTICAL_SPLIT, painelOS, baixo);
        centro.setResizeWeight(0.45);
        add(centro, BorderLayout.CENTER);

        tbOS.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                osSelecionada();
            }
        });
        tbItens.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                acao(this::listarPecas);
            }
        });
        cbServico.addActionListener(e -> {
            if (!carregando) {
                prefillServico();
            }
        });
        cbPeca.addActionListener(e -> {
            if (!carregando) {
                prefillPeca();
            }
        });
    }

    // ---------- utilidades ----------

    private void acao(Acao a) {
        try {
            a.run();
        } catch (SQLException e) {
            Db.mostrarErro(this, e);
        }
    }

    private void aviso(String m) {
        Db.aviso(this, m);
    }

    private String osId() {
        int r = tbOS.getSelectedRow();
        return r < 0 ? null : String.valueOf(tbOS.getValueAt(r, 0));
    }

    private String osStatus() {
        int r = tbOS.getSelectedRow();
        return r < 0 ? null : String.valueOf(tbOS.getValueAt(r, 5));
    }

    private String osGarantia() {
        int r = tbOS.getSelectedRow();
        Object v = r < 0 ? null : tbOS.getValueAt(r, 10);
        return v == null ? null : v.toString();
    }

    private String itemNum() {
        int r = tbItens.getSelectedRow();
        return r < 0 ? null : String.valueOf(tbItens.getValueAt(r, 0));
    }

    private static boolean finalizada(String status) {
        return "Concluida".equals(status) || "Cancelada".equals(status);
    }

    /** Valida OS selecionada e editável; devolve o id ou null (já avisando o usuário). */
    private String osEditavel() {
        String os = osId();
        if (os == null) {
            aviso("Selecione uma ordem de serviço.");
            return null;
        }
        if (finalizada(osStatus())) {
            aviso("A OS está " + osStatus() + " e não aceita mais alterações de itens ou peças.");
            return null;
        }
        return os;
    }

    // ---------- carga ----------

    @Override
    public void recarregar() {
        acao(() -> {
            carregarCombos();
            listarOS(osId());
        });
    }

    private void carregarCombos() throws SQLException {
        carregando = true;
        try {
            preencher(cbVeiculo, "SELECT v.id_veiculo, CONCAT(v.placa, ' - ', v.marca, ' ', v.modelo, ' (', c.nome, ')') "
                    + "FROM veiculo v JOIN cliente c ON c.id_cliente = v.id_cliente ORDER BY v.placa");
            preencher(cbMecanico, "SELECT id_mecanico, nome FROM mecanico ORDER BY nome");
            preencher(cbServico, "SELECT id_servico, CONCAT(descricao, ' (R$ ', valor_padrao, ')'), valor_padrao, "
                    + "tempo_estimado_horas FROM servico_catalogo ORDER BY descricao");
            preencher(cbPeca, "SELECT id_peca, CONCAT(nome, ' [estoque ', estoque_atual, ']'), preco_unitario "
                    + "FROM peca ORDER BY nome");
        } finally {
            carregando = false;
        }
        prefillServico();
        prefillPeca();
    }

    private void preencher(JComboBox<Item> cb, String sql) throws SQLException {
        String sel = Item.id(cb);
        cb.removeAllItems();
        for (Item it : Db.itens(sql)) {
            cb.addItem(it);
        }
        Item.selecionar(cb, sel);
    }

    private void prefillServico() {
        Item it = Item.item(cbServico);
        if (it != null && it.extras.length >= 2) {
            txValorItem.setText(it.extras[0]);
            txHoras.setText(it.extras[1]);
        }
    }

    private void prefillPeca() {
        Item it = Item.item(cbPeca);
        if (it != null && it.extras.length >= 1) {
            txPrecoPeca.setText(it.extras[0]);
        }
    }

    private void listarOS(String manter) throws SQLException {
        DefaultTableModel m = Db.modelo("SELECT os.id_os AS `OS`, v.placa AS `Placa`, "
                + "CONCAT(v.marca, ' ', v.modelo) AS `Veículo`, c.nome AS `Cliente`, m.nome AS `Mecânico`, "
                + "os.status AS `Status`, os.data_abertura AS `Abertura`, os.data_conclusao AS `Conclusão`, "
                + "os.km_veiculo AS `KM`, os.valor_total AS `Mão de obra (R$)`, g.status_garantia AS `Garantia` "
                + "FROM ordem_servico os JOIN veiculo v ON v.id_veiculo = os.id_veiculo "
                + "JOIN cliente c ON c.id_cliente = v.id_cliente "
                + "JOIN mecanico m ON m.id_mecanico = os.id_mecanico_responsavel "
                + "LEFT JOIN garantia g ON g.id_os = os.id_os ORDER BY os.id_os DESC");
        tbOS.setModel(m);
        if (manter != null) {
            for (int i = 0; i < m.getRowCount(); i++) {
                if (manter.equals(String.valueOf(m.getValueAt(i, 0)))) {
                    tbOS.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }
    }

    private void osSelecionada() {
        String os = osId();
        if (os == null) {
            tbItens.setModel(new DefaultTableModel());
            tbPecas.setModel(new DefaultTableModel());
            lbInfo.setText(" ");
            return;
        }
        Item.selecionar(cbStatus, osStatus());
        String g = osGarantia();
        lbInfo.setText(g == null ? "Sem garantia." : "Garantia: " + g);
        acao(() -> listarItens(os));
    }

    private void listarItens(String os) throws SQLException {
        tbItens.setModel(Db.modelo("SELECT i.numero_item AS `Item`, s.descricao AS `Serviço`, "
                + "i.quantidade_horas AS `Horas`, i.valor_cobrado AS `Valor cobrado (R$)` "
                + "FROM item_ordem_servico i JOIN servico_catalogo s ON s.id_servico = i.id_servico "
                + "WHERE i.id_os = ? ORDER BY i.numero_item", os));
    }

    private void listarPecas() throws SQLException {
        String os = osId();
        String it = itemNum();
        if (os == null || it == null) {
            tbPecas.setModel(new DefaultTableModel());
            return;
        }
        tbPecas.setModel(Db.modelo("SELECT pu.id_peca AS `Cód. peça`, p.nome AS `Peça`, pu.quantidade AS `Qtd`, "
                + "pu.preco_aplicado AS `Preço aplicado (R$)`, pu.quantidade * pu.preco_aplicado AS `Subtotal (R$)` "
                + "FROM peca_utilizada pu JOIN peca p ON p.id_peca = pu.id_peca "
                + "WHERE pu.id_os = ? AND pu.numero_item = ? ORDER BY p.nome", os, it));
    }

    // ---------- ações ----------

    private void abrirOS() {
        if (sessao != null && sessao.isMecanico()) {
            aviso("Abertura de nova ordem de serviço é restrita aos perfis de Atendimento e Administração.");
            return;
        }
        String v = Item.id(cbVeiculo);
        String m = Item.id(cbMecanico);
        String km = Db.inteiro(txKm.getText());
        if (v == null || m == null) {
            aviso("Escolha o veículo e o mecânico.");
            return;
        }
        if (km == null) {
            aviso("Informe a quilometragem do veículo (somente números).");
            return;
        }
        acao(() -> {
            final String[] novo = new String[1];
            Db.transacao(c -> {
                long id = Db.inserir(c, "INSERT INTO ordem_servico (id_veiculo, id_mecanico_responsavel, km_veiculo) "
                        + "VALUES (?, ?, ?)", v, m, km);
                Db.executar(c, "INSERT INTO historico_status_os (id_os, status_anterior, status_novo) "
                        + "VALUES (?, NULL, 'Aberta')", id);
                novo[0] = String.valueOf(id);
            });
            txKm.setText("");
            listarOS(novo[0]);
        });
    }

    private void alterarStatus() {
        String os = osId();
        if (os == null) {
            aviso("Selecione uma ordem de serviço.");
            return;
        }
        String atual = osStatus();
        String novo = Item.id(cbStatus);
        if (sessao != null && sessao.isMecanico() && "Cancelada".equals(novo)) {
            aviso("Cancelamento de ordem de serviço é restrito aos perfis de Atendimento e Administração.");
            return;
        }
        if (finalizada(atual)) {
            aviso("A OS está " + atual + " e não pode mais mudar de status.");
            return;
        }
        if (novo.equals(atual)) {
            aviso("A OS já está com esse status.");
            return;
        }
        acao(() -> {
            if ("Concluida".equals(novo) && "0".equals(Db.valor(
                    "SELECT COUNT(*) FROM item_ordem_servico WHERE id_os = ?", os))) {
                aviso("Uma OS concluída precisa ter ao menos um item de serviço.");
                return;
            }
            Db.transacao(c -> {
                Db.executar(c, "UPDATE ordem_servico SET status = ?"
                        + (finalizada(novo) ? ", data_conclusao = NOW()" : "") + " WHERE id_os = ?", novo, os);
                // RN15: o gatilho trg_historico_status_os_au registra a transicao no banco.
                // Nao inserir aqui para evitar duplicidade e cobrir tambem alteracoes SQL diretas.
                if ("Cancelada".equals(novo)) {
                    Db.executar(c, "UPDATE peca p JOIN (SELECT id_peca, SUM(quantidade) AS q FROM peca_utilizada "
                            + "WHERE id_os = ? GROUP BY id_peca) x ON x.id_peca = p.id_peca "
                            + "SET p.estoque_atual = p.estoque_atual + x.q", os);
                }
            });
            carregarCombos();
            listarOS(os);
        });
    }

    private void registrarGarantia() {
        if (sessao != null && sessao.isMecanico()) {
            aviso("Emissão de garantia é restrita aos perfis de Atendimento e Administração.");
            return;
        }
        String os = osId();
        if (os == null) {
            aviso("Selecione uma ordem de serviço.");
            return;
        }
        if (!"Concluida".equals(osStatus())) {
            aviso("Somente OS concluída pode ter garantia (RN16).");
            return;
        }
        if (osGarantia() != null) {
            aviso("Esta OS já tem garantia registrada.");
            return;
        }
        String dias = JOptionPane.showInputDialog(this, "Prazo da garantia em dias:", "90");
        if (dias == null) {
            return;
        }
        String d = Db.inteiro(dias);
        if (d == null || Integer.parseInt(d) <= 0) {
            aviso("Informe um número de dias maior que zero.");
            return;
        }
        String cob = JOptionPane.showInputDialog(this, "Descrição da cobertura (opcional):");
        if (cob != null && cob.trim().isEmpty()) {
            cob = null;
        }
        final String cobertura = cob;
        acao(() -> {
            Db.executar("INSERT INTO garantia (id_os, data_inicio, data_fim, descricao_cobertura) "
                    + "VALUES (?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL ? DAY), ?)",
                    os, Integer.valueOf(d), cobertura);
            listarOS(os);
        });
    }

    private static void recalcularTotal(Connection c, String os) throws SQLException {
        Db.executar(c, "UPDATE ordem_servico SET valor_total = ("
                + "  COALESCE((SELECT SUM(valor_cobrado) FROM item_ordem_servico WHERE id_os = ?), 0) + "
                + "  COALESCE((SELECT SUM(quantidade * preco_aplicado) FROM peca_utilizada WHERE id_os = ?), 0)"
                + ") WHERE id_os = ?", os, os, os);
    }

    private void adicionarItem() {
        String os = osEditavel();
        if (os == null) {
            return;
        }
        String srv = Item.id(cbServico);
        String horas = Db.decimal(txHoras.getText());
        String valor = Db.decimal(txValorItem.getText());
        if (srv == null) {
            aviso("Escolha o serviço.");
            return;
        }
        if (horas == null || valor == null) {
            aviso("Horas e valor devem ser numéricos.");
            return;
        }
        acao(() -> {
            Db.transacao(c -> {
                Db.consultar(c, "SELECT id_os FROM ordem_servico WHERE id_os = ? FOR UPDATE", os);
                String prox = Db.consultar(c, "SELECT COALESCE(MAX(numero_item), 0) + 1 FROM item_ordem_servico "
                        + "WHERE id_os = ?", os).get(0)[0];
                Db.executar(c, "INSERT INTO item_ordem_servico (id_os, numero_item, id_servico, quantidade_horas, "
                        + "valor_cobrado) VALUES (?, ?, ?, ?, ?)", os, prox, srv, horas, valor);
                recalcularTotal(c, os);
            });
            listarOS(os);
            listarItens(os);
        });
    }

    private void removerItem() {
        String os = osEditavel();
        if (os == null) {
            return;
        }
        String it = itemNum();
        if (it == null) {
            aviso("Selecione um item na lista.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Remover o item " + it + " e devolver suas peças ao estoque?",
                "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        acao(() -> {
            Db.transacao(c -> {
                Db.executar(c, "UPDATE peca p JOIN (SELECT id_peca, SUM(quantidade) AS q FROM peca_utilizada "
                        + "WHERE id_os = ? AND numero_item = ? GROUP BY id_peca) x ON x.id_peca = p.id_peca "
                        + "SET p.estoque_atual = p.estoque_atual + x.q", os, it);
                Db.executar(c, "DELETE FROM item_ordem_servico WHERE id_os = ? AND numero_item = ?", os, it);
                recalcularTotal(c, os);
            });
            carregarCombos();
            listarOS(os);
        });
    }

    private void adicionarPeca() {
        String os = osEditavel();
        if (os == null) {
            return;
        }
        String it = itemNum();
        if (it == null) {
            aviso("Selecione o item de serviço que consumiu a peça.");
            return;
        }
        String peca = Item.id(cbPeca);
        String qtd = Db.inteiro(txQtd.getText());
        String preco = Db.decimal(txPrecoPeca.getText());
        if (peca == null) {
            aviso("Escolha a peça.");
            return;
        }
        if (qtd == null || Integer.parseInt(qtd) <= 0) {
            aviso("A quantidade deve ser um número maior que zero (RN14).");
            return;
        }
        if (preco == null) {
            aviso("Informe o preço aplicado.");
            return;
        }
        acao(() -> {
            Db.transacao(c -> {
                int n = Db.executar(c, "UPDATE peca SET estoque_atual = estoque_atual - ? "
                        + "WHERE id_peca = ? AND estoque_atual >= ?", qtd, peca, qtd);
                if (n == 0) {
                    throw new SQLException("Estoque insuficiente para essa quantidade (RN09).");
                }
                Db.executar(c, "INSERT INTO peca_utilizada (id_os, numero_item, id_peca, quantidade, preco_aplicado) "
                        + "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE quantidade = quantidade + VALUES(quantidade)",
                        os, it, peca, qtd, preco);
                recalcularTotal(c, os);
            });
            carregarCombos();
            listarOS(os);
            listarPecas();
        });
    }

    private void removerPeca() {
        String os = osEditavel();
        if (os == null) {
            return;
        }
        String it = itemNum();
        int r = tbPecas.getSelectedRow();
        if (it == null || r < 0) {
            aviso("Selecione a peça na lista de peças do item.");
            return;
        }
        Object peca = tbPecas.getValueAt(r, 0);
        Object qtd = tbPecas.getValueAt(r, 2);
        acao(() -> {
            Db.transacao(c -> {
                Db.executar(c, "UPDATE peca SET estoque_atual = estoque_atual + ? WHERE id_peca = ?", qtd, peca);
                Db.executar(c, "DELETE FROM peca_utilizada WHERE id_os = ? AND numero_item = ? AND id_peca = ?",
                        os, it, peca);
                recalcularTotal(c, os);
            });
            carregarCombos();
            listarOS(os);
            listarPecas();
        });
    }

    private void simularFalhaTransacional() {
        if (sessao != null && !sessao.isAdmin()) {
            aviso("A demonstração de teste transacional é restrita ao perfil Administrador.");
            return;
        }
        String os = osEditavel();
        if (os == null) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Deseja iniciar a demonstração de Falha Transacional e Rollback (B3)?\n\n"
                + "Passo 1: Baixa preliminar de 1 unidade no estoque da Peça 1 dentro da transação.\n"
                + "Passo 2: Inclusão de um item temporário de R$ 999.00 na OS " + os + ".\n"
                + "Passo 3: Violação intencional de integridade (CHECK quantidade = 0 na tabela peca_utilizada).\n"
                + "Passo 4: O driver acionará c.rollback(), desfazendo TANTO a baixa de estoque QUANTO o item temporário!",
                "Simular Falha na Transação (Rollback B3)", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            int itensAntes = Integer.parseInt(Db.valor("SELECT COUNT(*) FROM item_ordem_servico WHERE id_os = ?", os));
            int estoqueAntes = Integer.parseInt(Db.valor("SELECT estoque_atual FROM peca WHERE id_peca = 1"));

            try {
                Db.transacao(c -> {
                    int n = Db.executar(c, "UPDATE peca SET estoque_atual = estoque_atual - 1 WHERE id_peca = 1 AND estoque_atual >= 1");
                    if (n == 0) {
                        throw new SQLException("Estoque insuficiente na peça 1 para o teste.");
                    }

                    Db.consultar(c, "SELECT id_os FROM ordem_servico WHERE id_os = ? FOR UPDATE", os);
                    String prox = Db.consultar(c, "SELECT COALESCE(MAX(numero_item), 0) + 1 FROM item_ordem_servico WHERE id_os = ?", os).get(0)[0];
                    Db.executar(c, "INSERT INTO item_ordem_servico (id_os, numero_item, id_servico, quantidade_horas, valor_cobrado) "
                            + "VALUES (?, ?, 1, 1.0, 999.00)", os, prox);

                    Db.executar(c, "INSERT INTO peca_utilizada (id_os, numero_item, id_peca, quantidade, preco_aplicado) "
                            + "VALUES (?, ?, 1, 0, 50.00)", os, prox);
                });
            } catch (SQLException e) {
                int itensDepois = Integer.parseInt(Db.valor("SELECT COUNT(*) FROM item_ordem_servico WHERE id_os = ?", os));
                int estoqueDepois = Integer.parseInt(Db.valor("SELECT estoque_atual FROM peca WHERE id_peca = 1"));

                JOptionPane.showMessageDialog(this,
                        "DEMONSTRAÇÃO DE ATOMICIDADE MULTITABELA E ROLLBACK (B3)\n\n"
                        + "1. Operações iniciadas na mesma transação:\n"
                        + "   • Tentativa de baixa de estoque na tabela 'peca'\n"
                        + "   • Tentativa de inserção de item na tabela 'item_ordem_servico'\n\n"
                        + "2. Falha provocada intencionalmente:\n"
                        + "   • Violação de restrição CHECK ao inserir quantidade zero em 'peca_utilizada'\n\n"
                        + "3. Reversão automática via JDBC:\n"
                        + "   • O método transacao disparou ROLLBACK imediato\n\n"
                        + "4. Conferência de integridade do banco de dados:\n"
                        + "   • Saldo em estoque da Peça 1: Antes = " + estoqueAntes + " | Depois = " + estoqueDepois + " (Inalterado)\n"
                        + "   • Quantidade de itens da OS: Antes = " + itensAntes + " | Depois = " + itensDepois + " (Inalterado)\n\n"
                        + "Nenhuma alteração parcial persistiu no banco de dados.",
                        "Transação Revertida com Sucesso (B3)", JOptionPane.INFORMATION_MESSAGE);

                acao(() -> {
                    listarOS(os);
                    listarItens(os);
                    carregarCombos();
                });
            }
        } catch (SQLException ex) {
            Db.mostrarErro(this, ex);
        }
    }
}
