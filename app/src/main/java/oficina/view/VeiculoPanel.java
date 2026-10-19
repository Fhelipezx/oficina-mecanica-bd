package oficina.view;

import oficina.util.Db;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Veículo com especialização Carro/Moto: grava veiculo e a subclasse na mesma transação. */
public class VeiculoPanel extends JPanel implements Recarregavel {

    private static final String[] CABECALHO = {"Cód.", "Placa", "Marca", "Modelo", "Ano", "Cor", "Tipo",
        "Proprietário", "Portas", "Combustível", "Cilindrada"};

    private final JTextField txId = new JTextField(22);
    private final JTextField txPlaca = new JTextField(22);
    private final JTextField txMarca = new JTextField(22);
    private final JTextField txModelo = new JTextField(22);
    private final JTextField txAno = new JTextField(22);
    private final JTextField txCor = new JTextField(22);
    private final JTextField txPortas = new JTextField(22);
    private final JTextField txCilindrada = new JTextField(22);
    private final JComboBox<Item> cbTipo = new JComboBox<>();
    private final JComboBox<Item> cbCliente = new JComboBox<>();
    private final JComboBox<Item> cbComb = new JComboBox<>();
    private final JPanel pCarro = new JPanel(new GridBagLayout());
    private final JPanel pMoto = new JPanel(new GridBagLayout());
    private final JTable grade = new JTable();
    private List<String[]> linhas = new ArrayList<>();
    private String idAtual;
    private String tipoAtual;

    public VeiculoPanel() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        txId.setEditable(false);
        cbTipo.addItem(new Item("Carro", "Carro"));
        cbTipo.addItem(new Item("Moto", "Moto"));
        for (String c : new String[]{"Flex", "Gasolina", "Diesel", "Eletrico", "Hibrido"}) {
            cbComb.addItem(new Item(c, c));
        }

        JPanel form = new JPanel(new GridBagLayout());
        linha(form, 0, "Código", txId);
        linha(form, 1, "Placa *", txPlaca);
        linha(form, 2, "Marca *", txMarca);
        linha(form, 3, "Modelo *", txModelo);
        linha(form, 4, "Ano de fabricação *", txAno);
        linha(form, 5, "Cor", txCor);
        linha(form, 6, "Proprietário *", cbCliente);
        linha(form, 7, "Tipo *", cbTipo);
        linha(pCarro, 0, "Nº de portas *", txPortas);
        linha(pCarro, 1, "Combustível *", cbComb);
        linha(pMoto, 0, "Cilindrada (cc) *", txCilindrada);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 8;
        g.gridwidth = 2;
        g.fill = GridBagConstraints.HORIZONTAL;
        form.add(pCarro, g);
        g.gridy = 9;
        form.add(pMoto, g);
        cbTipo.addActionListener(e -> alternarSubclasse());
        alternarSubclasse();

        JPanel botoes = new JPanel(new GridLayout(2, 2, 6, 6));
        JButton bNovo = new JButton("Novo");
        JButton bSalvar = new JButton("Salvar");
        JButton bExcluir = new JButton("Excluir");
        JButton bAtualizar = new JButton("Atualizar lista");
        bNovo.addActionListener(e -> limpar());
        bSalvar.addActionListener(e -> salvar());
        bExcluir.addActionListener(e -> excluir());
        bAtualizar.addActionListener(e -> recarregar());
        botoes.add(bNovo);
        botoes.add(bSalvar);
        botoes.add(bExcluir);
        botoes.add(bAtualizar);

        JPanel topo = new JPanel(new BorderLayout(0, 12));
        topo.add(form, BorderLayout.NORTH);
        topo.add(botoes, BorderLayout.CENTER);
        JPanel esquerda = new JPanel(new BorderLayout());
        esquerda.add(topo, BorderLayout.NORTH);
        add(esquerda, BorderLayout.WEST);

        grade.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grade.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && grade.getSelectedRow() >= 0) {
                carregarLinha(grade.getSelectedRow());
            }
        });
        add(new JScrollPane(grade), BorderLayout.CENTER);
    }

    private static void linha(JPanel p, int y, String rotulo, JComponent c) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 3, 3, 3);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy = y;
        g.gridx = 0;
        p.add(new JLabel(rotulo), g);
        g.gridx = 1;
        g.weightx = 1;
        p.add(c, g);
    }

    private void alternarSubclasse() {
        boolean carro = "Carro".equals(Item.id(cbTipo));
        pCarro.setVisible(carro);
        pMoto.setVisible(!carro);
    }

    @Override
    public void recarregar() {
        try {
            String sel = Item.id(cbCliente);
            cbCliente.removeAllItems();
            for (Item it : Db.itens("SELECT id_cliente, CONCAT(nome, ' (', cpf_cnpj, ')') FROM cliente ORDER BY nome")) {
                cbCliente.addItem(it);
            }
            Item.selecionar(cbCliente, sel);

            linhas = Db.consultar(
                "SELECT v.id_veiculo, v.placa, v.marca, v.modelo, v.ano_fabricacao, v.cor, v.tipo_veiculo, "
                + "c.nome, ca.num_portas, ca.tipo_combustivel, mo.cilindrada, v.id_cliente "
                + "FROM veiculo v JOIN cliente c ON c.id_cliente = v.id_cliente "
                + "LEFT JOIN carro ca ON ca.id_veiculo = v.id_veiculo "
                + "LEFT JOIN moto mo ON mo.id_veiculo = v.id_veiculo ORDER BY v.id_veiculo");
            DefaultTableModel m = new DefaultTableModel(CABECALHO, 0) {
                @Override
                public boolean isCellEditable(int r, int c) {
                    return false;
                }
            };
            for (String[] l : linhas) {
                Object[] row = new Object[CABECALHO.length];
                System.arraycopy(l, 0, row, 0, CABECALHO.length);
                m.addRow(row);
            }
            grade.setModel(m);
            limpar();
        } catch (SQLException e) {
            Db.mostrarErro(this, e);
        }
    }

    private void carregarLinha(int viewRow) {
        String[] l = linhas.get(grade.convertRowIndexToModel(viewRow));
        idAtual = l[0];
        tipoAtual = l[6];
        txId.setText(l[0]);
        txPlaca.setText(l[1]);
        txMarca.setText(l[2]);
        txModelo.setText(l[3]);
        txAno.setText(l[4] == null ? "" : l[4]);
        txCor.setText(l[5] == null ? "" : l[5]);
        Item.selecionar(cbTipo, l[6]);
        Item.selecionar(cbCliente, l[11]);
        txPortas.setText(l[8] == null ? "" : l[8]);
        Item.selecionar(cbComb, l[9]);
        txCilindrada.setText(l[10] == null ? "" : l[10]);
    }

    private void limpar() {
        idAtual = null;
        tipoAtual = null;
        for (JTextField t : new JTextField[]{txId, txPlaca, txMarca, txModelo, txAno, txCor, txPortas, txCilindrada}) {
            t.setText("");
        }
        cbTipo.setSelectedIndex(0);
        cbComb.setSelectedIndex(0);
        if (cbCliente.getItemCount() > 0) {
            cbCliente.setSelectedIndex(0);
        }
        grade.clearSelection();
    }

    private void salvar() {
        String placa = txPlaca.getText().trim().toUpperCase();
        String marca = txMarca.getText().trim();
        String modelo = txModelo.getText().trim();
        String ano = Db.inteiro(txAno.getText());
        String cor = txCor.getText().trim().isEmpty() ? null : txCor.getText().trim();
        String tipo = Item.id(cbTipo);
        String cli = Item.id(cbCliente);
        String portas = Db.inteiro(txPortas.getText());
        String comb = Item.id(cbComb);
        String cil = Db.inteiro(txCilindrada.getText());

        if (placa.isEmpty() || marca.isEmpty() || modelo.isEmpty()) {
            Db.aviso(this, "Preencha placa, marca e modelo.");
            return;
        }
        if (ano == null || ano.length() != 4) {
            Db.aviso(this, "Informe o ano de fabricação com 4 dígitos.");
            return;
        }
        if (cli == null) {
            Db.aviso(this, "Escolha o proprietário (cadastre o cliente antes, se necessário).");
            return;
        }
        if ("Carro".equals(tipo) && portas == null) {
            Db.aviso(this, "Informe o número de portas.");
            return;
        }
        if ("Moto".equals(tipo) && cil == null) {
            Db.aviso(this, "Informe a cilindrada.");
            return;
        }

        final String id0 = idAtual;
        final String antigo = tipoAtual;
        try {
            Db.transacao(c -> {
                if (id0 == null) {
                    long novo = Db.inserir(c, "INSERT INTO veiculo (placa, marca, modelo, ano_fabricacao, cor, "
                            + "tipo_veiculo, id_cliente) VALUES (?, ?, ?, ?, ?, ?, ?)",
                            placa, marca, modelo, ano, cor, tipo, cli);
                    inserirSub(c, String.valueOf(novo), tipo, portas, comb, cil);
                } else {
                    Db.executar(c, "UPDATE veiculo SET placa = ?, marca = ?, modelo = ?, ano_fabricacao = ?, "
                            + "cor = ?, tipo_veiculo = ?, id_cliente = ? WHERE id_veiculo = ?",
                            placa, marca, modelo, ano, cor, tipo, cli, id0);
                    if (tipo.equals(antigo)) {
                        if ("Carro".equals(tipo)) {
                            Db.executar(c, "UPDATE carro SET num_portas = ?, tipo_combustivel = ? WHERE id_veiculo = ?",
                                    portas, comb, id0);
                        } else {
                            Db.executar(c, "UPDATE moto SET cilindrada = ? WHERE id_veiculo = ?", cil, id0);
                        }
                    } else {
                        Db.executar(c, "DELETE FROM " + ("Carro".equals(antigo) ? "carro" : "moto")
                                + " WHERE id_veiculo = ?", id0);
                        inserirSub(c, id0, tipo, portas, comb, cil);
                    }
                }
            });
            recarregar();
        } catch (SQLException e) {
            Db.mostrarErro(this, e);
        }
    }

    private static void inserirSub(Connection c, String id, String tipo, String portas, String comb, String cil)
            throws SQLException {
        if ("Carro".equals(tipo)) {
            Db.executar(c, "INSERT INTO carro (id_veiculo, num_portas, tipo_combustivel) VALUES (?, ?, ?)",
                    id, portas, comb);
        } else {
            Db.executar(c, "INSERT INTO moto (id_veiculo, cilindrada) VALUES (?, ?)", id, cil);
        }
    }

    private void excluir() {
        if (idAtual == null) {
            Db.aviso(this, "Selecione um veículo na lista.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Excluir o veículo " + idAtual + "?", "Confirmar exclusão",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            Db.executar("DELETE FROM veiculo WHERE id_veiculo = ?", idAtual);
            recarregar();
        } catch (SQLException e) {
            Db.mostrarErro(this, e);
        }
    }
}
