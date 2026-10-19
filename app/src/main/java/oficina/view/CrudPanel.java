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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tela de CRUD para tabelas com chave primária simples (formulário + grade). */
public class CrudPanel extends JPanel implements Recarregavel {

    public interface Validador {
        /** Devolve a mensagem de erro ou null se estiver ok. */
        String validar(Map<String, String> valores, String idAtual);
    }

    private final String tabela;
    private final Campo[] campos;
    private final Validador validador;
    private final JComponent[] entradas;
    private final Map<Integer, Map<String, String>> rotulos = new HashMap<>();
    private final JTable grade = new JTable();
    private final int pkIdx;
    private List<String[]> linhas = new ArrayList<>();
    private String idAtual;

    public CrudPanel(String tabela, Campo[] campos, Validador validador) {
        super(new BorderLayout(10, 10));
        this.tabela = tabela;
        this.campos = campos;
        this.validador = validador;
        this.entradas = new JComponent[campos.length];
        int pk = 0;

        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 3, 3, 3);
        g.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < campos.length; i++) {
            Campo c = campos[i];
            JComponent in;
            if (c.tipo == Campo.Tipo.ENUM) {
                JComboBox<Item> cb = new JComboBox<>();
                for (String o : c.opcoes) {
                    cb.addItem(new Item(o, o));
                }
                in = cb;
            } else if (c.tipo == Campo.Tipo.LOOKUP) {
                in = new JComboBox<Item>();
            } else {
                JTextField tf = new JTextField(22);
                tf.setEditable(!c.pk);
                in = tf;
            }
            entradas[i] = in;
            if (c.pk) {
                pk = i;
            }
            g.gridx = 0;
            g.gridy = i;
            g.weightx = 0;
            form.add(new JLabel(c.rotulo + (c.obrigatorio ? " *" : "")), g);
            g.gridx = 1;
            g.weightx = 1;
            form.add(in, g);
        }
        this.pkIdx = pk;

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

    // ---------- carga de dados ----------

    @Override
    public void recarregar() {
        try {
            for (int i = 0; i < campos.length; i++) {
                if (campos[i].tipo == Campo.Tipo.LOOKUP) {
                    carregarLookup(i);
                }
            }
            listar();
        } catch (SQLException e) {
            Db.mostrarErro(this, e);
        }
    }

    @SuppressWarnings("unchecked")
    private JComboBox<Item> combo(int i) {
        return (JComboBox<Item>) entradas[i];
    }

    private void carregarLookup(int i) throws SQLException {
        JComboBox<Item> cb = combo(i);
        String sel = Item.id(cb);
        cb.removeAllItems();
        Map<String, String> mapa = new HashMap<>();
        if (!campos[i].obrigatorio) {
            cb.addItem(new Item(null, "(nenhum)"));
        }
        for (Item it : Db.itens(campos[i].lookupSql)) {
            cb.addItem(it);
            mapa.put(it.id, it.rotulo);
        }
        rotulos.put(i, mapa);
        Item.selecionar(cb, sel);
    }

    private void listar() throws SQLException {
        List<String> cols = new ArrayList<>();
        String[] titulos = new String[campos.length];
        for (int i = 0; i < campos.length; i++) {
            cols.add(campos[i].coluna);
            titulos[i] = campos[i].rotulo;
        }
        linhas = Db.consultar("SELECT " + String.join(", ", cols) + " FROM " + tabela
                + " ORDER BY " + campos[pkIdx].coluna);
        DefaultTableModel m = new DefaultTableModel(titulos, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        for (String[] l : linhas) {
            Object[] row = new Object[campos.length];
            for (int i = 0; i < campos.length; i++) {
                String v = l[i];
                if (campos[i].tipo == Campo.Tipo.LOOKUP && v != null && rotulos.get(i) != null) {
                    v = rotulos.get(i).getOrDefault(v, v);
                }
                row[i] = v;
            }
            m.addRow(row);
        }
        grade.setModel(m);
        limpar();
    }

    private void carregarLinha(int viewRow) {
        String[] l = linhas.get(grade.convertRowIndexToModel(viewRow));
        idAtual = l[pkIdx];
        for (int i = 0; i < campos.length; i++) {
            if (entradas[i] instanceof JTextField) {
                ((JTextField) entradas[i]).setText(l[i] == null ? "" : l[i]);
            } else {
                Item.selecionar(combo(i), l[i]);
            }
        }
    }

    private void limpar() {
        idAtual = null;
        for (int i = 0; i < campos.length; i++) {
            if (entradas[i] instanceof JTextField) {
                ((JTextField) entradas[i]).setText("");
            } else if (combo(i).getItemCount() > 0) {
                combo(i).setSelectedIndex(0);
            }
        }
        grade.clearSelection();
    }

    // ---------- gravação ----------

    private String ler(int i) {
        if (entradas[i] instanceof JTextField) {
            String s = ((JTextField) entradas[i]).getText().trim();
            return s.isEmpty() ? null : s;
        }
        return Item.id(combo(i));
    }

    private String normalizar(Campo c, String s) {
        switch (c.tipo) {
            case INTEIRO:
                return Db.inteiro(s);
            case DECIMAL:
                return Db.decimal(s);
            case DATA:
                return Db.normalizarData(s);
            case TEXTO:
                if (c.apenasDigitos) {
                    String d = s.replaceAll("\\D", "");
                    return d.isEmpty() ? null : d;
                }
                return s;
            default:
                return s;
        }
    }

    private void salvar() {
        Map<String, String> v = new LinkedHashMap<>();
        for (int i = 0; i < campos.length; i++) {
            Campo c = campos[i];
            if (c.pk) {
                continue;
            }
            String s = ler(i);
            if (s == null && c.obrigatorio) {
                Db.aviso(this, "Preencha o campo: " + c.rotulo);
                return;
            }
            if (s != null) {
                s = normalizar(c, s);
                if (s == null) {
                    Db.aviso(this, "Valor inválido no campo: " + c.rotulo);
                    return;
                }
            }
            v.put(c.coluna, s);
        }
        if (validador != null) {
            String msg = validador.validar(v, idAtual);
            if (msg != null) {
                Db.aviso(this, msg);
                return;
            }
        }
        try {
            if (idAtual == null) {
                inserir(v);
            } else {
                atualizar(v);
            }
            recarregar();
        } catch (SQLException e) {
            Db.mostrarErro(this, e);
        }
    }

    private void inserir(Map<String, String> v) throws SQLException {
        List<String> cols = new ArrayList<>();
        List<Object> par = new ArrayList<>();
        for (Map.Entry<String, String> e : v.entrySet()) {
            if (e.getValue() != null) {
                cols.add(e.getKey());
                par.add(e.getValue());
            }
        }
        String sql = "INSERT INTO " + tabela + " (" + String.join(", ", cols) + ") VALUES ("
                + String.join(", ", Collections.nCopies(cols.size(), "?")) + ")";
        Db.executar(sql, par.toArray());
    }

    private Campo campo(String coluna) {
        for (Campo c : campos) {
            if (c.coluna.equals(coluna)) {
                return c;
            }
        }
        return null;
    }

    private void atualizar(Map<String, String> v) throws SQLException {
        List<String> sets = new ArrayList<>();
        List<Object> par = new ArrayList<>();
        for (Map.Entry<String, String> e : v.entrySet()) {
            if (e.getValue() == null && campo(e.getKey()).padrao) {
                continue;
            }
            sets.add(e.getKey() + " = ?");
            par.add(e.getValue());
        }
        par.add(idAtual);
        Db.executar("UPDATE " + tabela + " SET " + String.join(", ", sets)
                + " WHERE " + campos[pkIdx].coluna + " = ?", par.toArray());
    }

    private void excluir() {
        if (idAtual == null) {
            Db.aviso(this, "Selecione um registro na lista.");
            return;
        }
        int r = JOptionPane.showConfirmDialog(this, "Excluir o registro " + idAtual + "?",
                "Confirmar exclusão", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            Db.executar("DELETE FROM " + tabela + " WHERE " + campos[pkIdx].coluna + " = ?", idAtual);
            recarregar();
        } catch (SQLException e) {
            Db.mostrarErro(this, e);
        }
    }
}
