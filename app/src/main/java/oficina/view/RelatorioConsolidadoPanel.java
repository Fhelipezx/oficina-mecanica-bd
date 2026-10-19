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

public class RelatorioConsolidadoPanel extends JPanel implements Recarregavel {

    private final JComboBox<String> cbStatus = new JComboBox<>(new String[]{
        "Todos", "Aberta", "Em andamento", "Aguardando peca", "Concluida", "Cancelada"
    });
    private final JComboBox<String> cbGarantia = new JComboBox<>(new String[]{
        "Todas", "Vigente", "Expirada", "Acionada", "Sem Garantia"
    });
    private final JTextField txBusca = new JTextField(15);
    private final JTable tabela = new JTable();

    private final JLabel lbQtdOS = new JLabel("0");
    private final JLabel lbTotalHoras = new JLabel("0.0 h");
    private final JLabel lbTotalPecasValor = new JLabel("R$ 0,00");
    private final JLabel lbReceitaTotal = new JLabel("R$ 0,00");

    public RelatorioConsolidadoPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel pFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        pFiltros.setBorder(BorderFactory.createTitledBorder("Filtros do Relatório"));

        pFiltros.add(new JLabel("Status:"));
        pFiltros.add(cbStatus);

        pFiltros.add(new JLabel("Garantia:"));
        pFiltros.add(cbGarantia);

        pFiltros.add(new JLabel("Cliente / Placa:"));
        pFiltros.add(txBusca);

        JButton bFiltrar = new JButton("Filtrar");
        bFiltrar.setFont(bFiltrar.getFont().deriveFont(Font.BOLD));
        bFiltrar.addActionListener(e -> carregarRelatorio());
        pFiltros.add(bFiltrar);

        JButton bLimpar = new JButton("Limpar");
        bLimpar.addActionListener(e -> {
            cbStatus.setSelectedIndex(0);
            cbGarantia.setSelectedIndex(0);
            txBusca.setText("");
            carregarRelatorio();
        });
        pFiltros.add(bLimpar);

        add(pFiltros, BorderLayout.NORTH);

        tabela.setFillsViewportHeight(true);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        JPanel pCards = new JPanel(new GridLayout(1, 4, 10, 10));
        pCards.setBorder(BorderFactory.createTitledBorder("Totais Consolidados"));

        pCards.add(criarCard("Total de Ordens", lbQtdOS));
        pCards.add(criarCard("Horas Trabalhadas", lbTotalHoras));
        pCards.add(criarCard("Total em Peças", lbTotalPecasValor));
        pCards.add(criarCard("Valor Total das OS Exibidas", lbReceitaTotal));

        add(pCards, BorderLayout.SOUTH);
    }

    private JPanel criarCard(String titulo, JLabel valor) {
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
        carregarRelatorio();
    }

    private void carregarRelatorio() {
        String status = (String) cbStatus.getSelectedItem();
        String garantia = (String) cbGarantia.getSelectedItem();
        String busca = txBusca.getText().trim();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
           .append("  id_os AS `OS`, ")
           .append("  status_os AS `Status`, ")
           .append("  DATE_FORMAT(data_abertura, '%d/%m/%Y %H:%i') AS `Abertura`, ")
           .append("  DATE_FORMAT(data_conclusao, '%d/%m/%Y %H:%i') AS `Conclusão`, ")
           .append("  cliente_nome AS `Cliente`, ")
           .append("  cliente_telefone AS `Telefone`, ")
           .append("  veiculo_placa AS `Placa`, ")
           .append("  veiculo_modelo AS `Veículo`, ")
           .append("  tipo_veiculo AS `Tipo`, ")
           .append("  mecanico_responsavel AS `Mecânico`, ")
           .append("  qtd_itens_servico AS `Itens`, ")
           .append("  total_horas_trabalhadas AS `Horas`, ")
           .append("  valor_mao_obra AS `Mão de Obra (R$)`, ")
           .append("  total_pecas_utilizadas AS `Peças Qtd`, ")
           .append("  valor_pecas AS `Peças Total (R$)`, ")
           .append("  valor_total_calculado AS `Valor Total (R$)`, ")
           .append("  status_garantia AS `Garantia`, ")
           .append("  DATE_FORMAT(garantia_validade, '%d/%m/%Y') AS `Validade Garantia` ")
           .append("FROM vw_relatorio_consolidado_os ")
           .append("WHERE 1=1 ");

        List<Object> par = new ArrayList<>();

        if (status != null && !"Todos".equals(status)) {
            sql.append(" AND status_os = ? ");
            par.add(status);
        }

        if (garantia != null && !"Todas".equals(garantia)) {
            sql.append(" AND status_garantia = ? ");
            par.add(garantia);
        }

        if (!busca.isEmpty()) {
            sql.append(" AND (cliente_nome LIKE ? OR veiculo_placa LIKE ?) ");
            par.add("%" + busca + "%");
            par.add("%" + busca + "%");
        }

        sql.append("ORDER BY id_os DESC");

        try {
            DefaultTableModel modelo = Db.modelo(sql.toString(), par.toArray());
            tabela.setModel(modelo);

            int totalRows = modelo.getRowCount();
            double somaHoras = 0.0;
            double somaPecas = 0.0;
            double somaTotal = 0.0;
            double receitaConcluida = 0.0;

            for (int i = 0; i < totalRows; i++) {
                try {
                    Object h = modelo.getValueAt(i, 11);
                    if (h != null) somaHoras += Double.parseDouble(h.toString());
                } catch (Exception ex) { }

                try {
                    Object p = modelo.getValueAt(i, 14);
                    if (p != null) somaPecas += Double.parseDouble(p.toString());
                } catch (Exception ex) { }

                try {
                    Object t = modelo.getValueAt(i, 15);
                    if (t != null) {
                        double val = Double.parseDouble(t.toString());
                        somaTotal += val;
                        Object st = modelo.getValueAt(i, 1);
                        if ("Concluida".equals(st)) {
                            receitaConcluida += val;
                        }
                    }
                } catch (Exception ex) { }
            }

            lbQtdOS.setText(String.valueOf(totalRows));
            lbTotalHoras.setText(String.format("%.1f h", somaHoras));
            lbTotalPecasValor.setText(String.format("R$ %.2f", somaPecas));
            lbReceitaTotal.setText(String.format("R$ %.2f", somaTotal));
            lbReceitaTotal.setToolTipText(String.format("Receita Efetiva (OS Concluídas): R$ %.2f", receitaConcluida));

        } catch (SQLException e) {
            Db.mostrarErro(this, e);
        }
    }
}
