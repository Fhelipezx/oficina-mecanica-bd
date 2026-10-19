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
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.time.LocalDate;

/** Associação N:N Mecânico x Especialidade, com nível e data de certificação. */
public class MecanicoEspecialidadePanel extends JPanel implements Recarregavel {

    private final JComboBox<Item> cbMec = new JComboBox<>();
    private final JComboBox<Item> cbEsp = new JComboBox<>();
    private final JComboBox<Item> cbNivel = new JComboBox<>();
    private final JTextField txData = new JTextField(LocalDate.now().toString(), 10);
    private final JTable tb = new JTable();
    private boolean carregando;

    public MecanicoEspecialidadePanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        for (String n : new String[]{"Basico", "Intermediario", "Avancado"}) {
            cbNivel.addItem(new Item(n, n));
        }
        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        norte.add(new JLabel("Mecânico"));
        norte.add(cbMec);
        add(norte, BorderLayout.NORTH);

        tb.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(tb), BorderLayout.CENTER);

        JPanel sul = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sul.add(new JLabel("Especialidade"));
        sul.add(cbEsp);
        sul.add(new JLabel("Nível"));
        sul.add(cbNivel);
        sul.add(new JLabel("Data (aaaa-mm-dd)"));
        sul.add(txData);
        JButton bAdd = new JButton("Adicionar / atualizar");
        JButton bRem = new JButton("Remover selecionada");
        bAdd.addActionListener(e -> adicionar());
        bRem.addActionListener(e -> remover());
        sul.add(bAdd);
        sul.add(bRem);
        add(sul, BorderLayout.SOUTH);

        cbMec.addActionListener(e -> {
            if (!carregando) {
                listar();
            }
        });
    }

    @Override
    public void recarregar() {
        try {
            carregando = true;
            preencher(cbMec, "SELECT id_mecanico, nome FROM mecanico ORDER BY nome");
            preencher(cbEsp, "SELECT id_especialidade, nome_especialidade FROM especialidade ORDER BY nome_especialidade");
        } catch (SQLException e) {
            Db.mostrarErro(this, e);
        } finally {
            carregando = false;
        }
        listar();
    }

    private void preencher(JComboBox<Item> cb, String sql) throws SQLException {
        String sel = Item.id(cb);
        cb.removeAllItems();
        for (Item it : Db.itens(sql)) {
            cb.addItem(it);
        }
        Item.selecionar(cb, sel);
    }

    private void listar() {
        String mec = Item.id(cbMec);
        if (mec == null) {
            tb.setModel(new DefaultTableModel());
            return;
        }
        try {
            tb.setModel(Db.modelo("SELECT e.id_especialidade AS `Cód.`, e.nome_especialidade AS `Especialidade`, "
                    + "me.nivel_certificacao AS `Nível`, me.data_certificacao AS `Data da certificação` "
                    + "FROM mecanico_especialidade me JOIN especialidade e ON e.id_especialidade = me.id_especialidade "
                    + "WHERE me.id_mecanico = ? ORDER BY e.nome_especialidade", mec));
        } catch (SQLException e) {
            Db.mostrarErro(this, e);
        }
    }

    private void adicionar() {
        String mec = Item.id(cbMec);
        String esp = Item.id(cbEsp);
        String data = Db.normalizarData(txData.getText());
        if (mec == null || esp == null) {
            Db.aviso(this, "Escolha o mecânico e a especialidade.");
            return;
        }
        if (data == null) {
            Db.aviso(this, "Data inválida. Use aaaa-mm-dd ou dd/mm/aaaa.");
            return;
        }
        try {
            Db.executar("INSERT INTO mecanico_especialidade (id_mecanico, id_especialidade, nivel_certificacao, "
                    + "data_certificacao) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                    + "nivel_certificacao = VALUES(nivel_certificacao), data_certificacao = VALUES(data_certificacao)",
                    mec, esp, Item.id(cbNivel), data);
            listar();
        } catch (SQLException e) {
            Db.mostrarErro(this, e);
        }
    }

    private void remover() {
        int r = tb.getSelectedRow();
        String mec = Item.id(cbMec);
        if (r < 0 || mec == null) {
            Db.aviso(this, "Selecione uma especialidade na lista.");
            return;
        }
        try {
            Db.executar("DELETE FROM mecanico_especialidade WHERE id_mecanico = ? AND id_especialidade = ?",
                    mec, tb.getValueAt(r, 0));
            listar();
        } catch (SQLException e) {
            Db.mostrarErro(this, e);
        }
    }
}
