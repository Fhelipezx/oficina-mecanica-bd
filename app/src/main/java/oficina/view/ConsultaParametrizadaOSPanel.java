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

public class ConsultaParametrizadaOSPanel extends JPanel implements Recarregavel {

    private final JTextField txDataInicio = new JTextField("2026-01-01", 8);
    private final JTextField txDataFim = new JTextField("2026-12-31", 8);
    private final JComboBox<String> cbStatus = new JComboBox<>(new String[]{
        "Todos", "Aberta", "Em andamento", "Aguardando peca", "Concluida", "Cancelada"
    });
    private final JComboBox<Item> cbMecanico = new JComboBox<>();
    private final JTable tabela = new JTable();

    private final JLabel lbTotalOS = new JLabel("0");
    private final JLabel lbTotalHoras = new JLabel("0.0 h");
    private final JLabel lbValorMedio = new JLabel("R$ 0,00");
    private final JLabel lbValorTotal = new JLabel("R$ 0,00");

    public ConsultaParametrizadaOSPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel pFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        pFiltros.setBorder(BorderFactory.createTitledBorder("Filtros"));

        pFiltros.add(new JLabel("Data Início:"));
        pFiltros.add(txDataInicio);

        pFiltros.add(new JLabel("Data Fim:"));
        pFiltros.add(txDataFim);

        pFiltros.add(new JLabel("Status:"));
        pFiltros.add(cbStatus);

        pFiltros.add(new JLabel("Mecânico:"));
        pFiltros.add(cbMecanico);

        JButton bFiltrar = new JButton("Filtrar");
        bFiltrar.setFont(bFiltrar.getFont().deriveFont(Font.BOLD));
        bFiltrar.addActionListener(e -> executarConsulta());
        pFiltros.add(bFiltrar);

        JButton bLimpar = new JButton("Limpar");
        bLimpar.addActionListener(e -> {
            txDataInicio.setText("2026-01-01");
            txDataFim.setText("2026-12-31");
            cbStatus.setSelectedIndex(0);
            if (cbMecanico.getItemCount() > 0) cbMecanico.setSelectedIndex(0);
            executarConsulta();
        });
        pFiltros.add(bLimpar);

        add(pFiltros, BorderLayout.NORTH);

        tabela.setFillsViewportHeight(true);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        JPanel pAgregacoes = new JPanel(new GridLayout(1, 4, 10, 10));
        pAgregacoes.setBorder(BorderFactory.createTitledBorder("Totais e Médias"));

        pAgregacoes.add(criarCardMetrica("Total de Ordens", lbTotalOS));
        pAgregacoes.add(criarCardMetrica("Total de Horas", lbTotalHoras));
        pAgregacoes.add(criarCardMetrica("Média por OS", lbValorMedio));
        pAgregacoes.add(criarCardMetrica("Faturamento Total", lbValorTotal));

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
        carregarMecanicos();
        executarConsulta();
    }

    private void carregarMecanicos() {
        try {
            cbMecanico.removeAllItems();
            cbMecanico.addItem(new Item("0", "Todos os Mecânicos"));
            for (Item item : Db.itens("SELECT id_mecanico, nome FROM mecanico ORDER BY nome")) {
                cbMecanico.addItem(item);
            }
        } catch (SQLException e) {
            Db.mostrarErro(this, e);
        }
    }

    private void executarConsulta() {
        String dtIni = Db.normalizarData(txDataInicio.getText());
        String dtFim = Db.normalizarData(txDataFim.getText());
        String status = (String) cbStatus.getSelectedItem();
        String idMec = Item.id(cbMecanico);

        if (dtIni == null || dtFim == null) {
            Db.aviso(this, "Informe datas válidas (AAAA-MM-DD ou DD/MM/AAAA).");
            return;
        }

        if (dtIni.compareTo(dtFim) > 0) {
            Db.aviso(this, "A data inicial não pode ser posterior à data final.");
            return;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
           .append("  os.id_os AS `OS`, ")
           .append("  DATE_FORMAT(os.data_abertura, '%d/%m/%Y %H:%i') AS `Abertura`, ")
           .append("  os.status AS `Status`, ")
           .append("  c.nome AS `Cliente`, ")
           .append("  CONCAT(v.placa, ' - ', v.marca, ' ', v.modelo) AS `Veículo`, ")
           .append("  m.nome AS `Mecânico Responsável`, ")
           .append("  COUNT(ios.numero_item) AS `Qtd Itens`, ")
           .append("  COALESCE(SUM(ios.quantidade_horas), 0) AS `Horas de Serviço`, ")
           .append("  os.valor_total AS `Valor Total (R$)` ")
           .append("FROM ordem_servico os ")
           .append("JOIN veiculo v ON v.id_veiculo = os.id_veiculo ")
           .append("JOIN cliente c ON c.id_cliente = v.id_cliente ")
           .append("JOIN mecanico m ON m.id_mecanico = os.id_mecanico_responsavel ")
           .append("LEFT JOIN item_ordem_servico ios ON ios.id_os = os.id_os ")
           .append("WHERE os.data_abertura >= ? AND os.data_abertura < DATE_ADD(?, INTERVAL 1 DAY) ");

        List<Object> parametros = new ArrayList<>();
        parametros.add(dtIni);
        parametros.add(dtFim);

        if (status != null && !"Todos".equals(status)) {
            sql.append("  AND os.status = ? ");
            parametros.add(status);
        }

        if (idMec != null && !"0".equals(idMec)) {
            sql.append("  AND os.id_mecanico_responsavel = ? ");
            parametros.add(idMec);
        }

        sql.append("GROUP BY os.id_os, os.data_abertura, os.status, c.nome, v.placa, v.marca, v.modelo, m.nome, os.valor_total ")
           .append("ORDER BY os.data_abertura DESC");

        try {
            DefaultTableModel modelo = Db.modelo(sql.toString(), parametros.toArray());
            tabela.setModel(modelo);

            int countOS = modelo.getRowCount();
            double somaHoras = 0.0;
            double somaValor = 0.0;

            for (int i = 0; i < countOS; i++) {
                try {
                    Object h = modelo.getValueAt(i, 7);
                    if (h != null) somaHoras += Double.parseDouble(h.toString());
                } catch (Exception ex) { }

                try {
                    Object v = modelo.getValueAt(i, 8);
                    if (v != null) somaValor += Double.parseDouble(v.toString());
                } catch (Exception ex) { }
            }

            double mediaValor = countOS > 0 ? (somaValor / countOS) : 0.0;

            lbTotalOS.setText(String.valueOf(countOS));
            lbTotalHoras.setText(String.format("%.1f h", somaHoras));
            lbValorMedio.setText(String.format("R$ %.2f", mediaValor));
            lbValorTotal.setText(String.format("R$ %.2f", somaValor));

        } catch (SQLException e) {
            Db.mostrarErro(this, e);
        }
    }
}
