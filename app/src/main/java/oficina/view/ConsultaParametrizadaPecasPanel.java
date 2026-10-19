package oficina.view;

import oficina.util.Db;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ConsultaParametrizadaPecasPanel extends JPanel implements Recarregavel {

    private final JComboBox<Item> cbFornecedor = new JComboBox<>();
    private final JTextField txPrecoMin = new JTextField("0.00", 6);
    private final JTextField txPrecoMax = new JTextField("5000.00", 6);
    private final JTextField txBuscaNome = new JTextField("", 12);
    private final JTable tabela = new JTable();

    private final JLabel lbTotalPecas = new JLabel("0");
    private final JLabel lbTotalUnidades = new JLabel("0 un");
    private final JLabel lbPrecoMedio = new JLabel("R$ 0,00");
    private final JLabel lbFaturamentoPecas = new JLabel("R$ 0,00");

    public ConsultaParametrizadaPecasPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel pFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        pFiltros.setBorder(BorderFactory.createTitledBorder("Filtros"));

        pFiltros.add(new JLabel("Fornecedor:"));
        pFiltros.add(cbFornecedor);

        pFiltros.add(new JLabel("Preço Mínimo:"));
        pFiltros.add(txPrecoMin);

        pFiltros.add(new JLabel("Preço Máximo:"));
        pFiltros.add(txPrecoMax);

        pFiltros.add(new JLabel("Nome / Código:"));
        pFiltros.add(txBuscaNome);

        JButton bFiltrar = new JButton("Filtrar");
        bFiltrar.setFont(bFiltrar.getFont().deriveFont(Font.BOLD));
        bFiltrar.addActionListener(e -> executarConsulta());
        pFiltros.add(bFiltrar);

        JButton bLimpar = new JButton("Limpar");
        bLimpar.addActionListener(e -> {
            if (cbFornecedor.getItemCount() > 0) cbFornecedor.setSelectedIndex(0);
            txPrecoMin.setText("0.00");
            txPrecoMax.setText("5000.00");
            txBuscaNome.setText("");
            executarConsulta();
        });
        pFiltros.add(bLimpar);

        add(pFiltros, BorderLayout.NORTH);

        tabela.setFillsViewportHeight(true);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        JPanel pAgregacoes = new JPanel(new GridLayout(1, 4, 10, 10));
        pAgregacoes.setBorder(BorderFactory.createTitledBorder("Totais e Médias"));

        pAgregacoes.add(criarCardMetrica("Itens Encontrados", lbTotalPecas));
        pAgregacoes.add(criarCardMetrica("Unidades Consumidas", lbTotalUnidades));
        pAgregacoes.add(criarCardMetrica("Preço Médio", lbPrecoMedio));
        pAgregacoes.add(criarCardMetrica("Faturamento Total", lbFaturamentoPecas));

        add(pAgregacoes, BorderLayout.SOUTH);
    }

    private JPanel criarCardMetrica(String titulo, JLabel valor) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        JLabel lbTitulo = new JLabel(titulo);
        lbTitulo.setFont(new Font("SansSerif", Font.PLAIN, 11));
        valor.setFont(new Font("SansSerif", Font.BOLD, 15));
        card.add(lbTitulo, BorderLayout.NORTH);
        card.add(valor, BorderLayout.CENTER);
        return card;
    }

    @Override
    public void recarregar() {
        carregarFornecedores();
        executarConsulta();
    }

    private void carregarFornecedores() {
        try {
            cbFornecedor.removeAllItems();
            cbFornecedor.addItem(new Item("0", "Todos os Fornecedores"));
            for (Item item : Db.itens("SELECT id_fornecedor, nome FROM fornecedor ORDER BY nome")) {
                cbFornecedor.addItem(item);
            }
        } catch (SQLException e) {
            Db.mostrarErro(this, e);
        }
    }

    private void executarConsulta() {
        String idForn = Item.id(cbFornecedor);
        String precoMin = Db.decimal(txPrecoMin.getText());
        String precoMax = Db.decimal(txPrecoMax.getText());
        String busca = txBuscaNome.getText().trim();

        if (precoMin == null || precoMax == null) {
            Db.aviso(this, "Informe valores numéricos válidos.");
            return;
        }

        if (Double.parseDouble(precoMin) > Double.parseDouble(precoMax)) {
            Db.aviso(this, "O preço mínimo não pode ser maior que o preço máximo.");
            return;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
           .append("  p.id_peca AS `Cód.`, ")
           .append("  p.codigo_fabricante AS `Código Fabricante`, ")
           .append("  p.nome AS `Nome da Peça`, ")
           .append("  f.nome AS `Fornecedor`, ")
           .append("  p.preco_unitario AS `Preço Tabela (R$)`, ")
           .append("  p.estoque_atual AS `Saldo Estoque`, ")
           .append("  COUNT(DISTINCT os.id_os) AS `OS Concluídas`, ")
           .append("  COALESCE(SUM(CASE WHEN os.id_os IS NOT NULL THEN pu.quantidade ELSE 0 END), 0) AS `Qtd Consumo Efetivo`, ")
           .append("  COALESCE(SUM(CASE WHEN os.id_os IS NOT NULL THEN pu.quantidade * pu.preco_aplicado ELSE 0 END), 0.00) AS `Faturamento Realizado (R$)` ")
           .append("FROM peca p ")
           .append("JOIN fornecedor f ON f.id_fornecedor = p.id_fornecedor ")
           .append("LEFT JOIN peca_utilizada pu ON pu.id_peca = p.id_peca ")
           .append("LEFT JOIN ordem_servico os ON os.id_os = pu.id_os AND os.status = 'Concluida' ")
           .append("WHERE p.preco_unitario >= ? AND p.preco_unitario <= ? ");

        List<Object> parametros = new ArrayList<>();
        parametros.add(Double.valueOf(precoMin));
        parametros.add(Double.valueOf(precoMax));

        if (idForn != null && !"0".equals(idForn)) {
            sql.append("  AND p.id_fornecedor = ? ");
            parametros.add(idForn);
        }

        if (!busca.isEmpty()) {
            sql.append("  AND (p.nome LIKE ? OR p.codigo_fabricante LIKE ?) ");
            parametros.add("%" + busca + "%");
            parametros.add("%" + busca + "%");
        }

        sql.append("GROUP BY p.id_peca, p.codigo_fabricante, p.nome, f.nome, p.preco_unitario, p.estoque_atual ")
           .append("ORDER BY `Faturamento Realizado (R$)` DESC, p.nome ASC");

        try {
            DefaultTableModel modelo = Db.modelo(sql.toString(), parametros.toArray());
            tabela.setModel(modelo);

            int countPecas = modelo.getRowCount();
            long somaUnidades = 0;
            double somaPrecoTabela = 0.0;
            double somaFaturamento = 0.0;

            for (int i = 0; i < countPecas; i++) {
                try {
                    Object pTab = modelo.getValueAt(i, 4);
                    if (pTab != null) somaPrecoTabela += Double.parseDouble(pTab.toString());
                } catch (Exception ex) { }

                try {
                    Object u = modelo.getValueAt(i, 7);
                    if (u != null) somaUnidades += Long.parseLong(u.toString());
                } catch (Exception ex) { }

                try {
                    Object f = modelo.getValueAt(i, 8);
                    if (f != null) somaFaturamento += Double.parseDouble(f.toString());
                } catch (Exception ex) { }
            }

            double mediaPreco = countPecas > 0 ? (somaPrecoTabela / countPecas) : 0.0;

            lbTotalPecas.setText(String.valueOf(countPecas));
            lbTotalUnidades.setText(somaUnidades + " un");
            lbPrecoMedio.setText(String.format("R$ %.2f", mediaPreco));
            lbFaturamentoPecas.setText(String.format("R$ %.2f", somaFaturamento));

        } catch (SQLException e) {
            Db.mostrarErro(this, e);
        }
    }
}
