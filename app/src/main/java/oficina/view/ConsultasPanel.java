package oficina.view;

import oficina.util.Db;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/** Executa as 15 consultas do arquivo 03_consultas.sql (copiado para resources/consultas.sql). */
public class ConsultasPanel extends JPanel {

    private static final class Consulta {
        final String titulo;
        final String sql;

        Consulta(String titulo, String sql) {
            this.titulo = titulo;
            this.sql = sql;
        }

        @Override
        public String toString() {
            return titulo;
        }
    }

    private final JComboBox<Consulta> cb = new JComboBox<>();
    private final JTextArea txSql = new JTextArea(9, 80);
    private final JTable tb = new JTable();
    private final JLabel lbInfo = new JLabel(" ");

    public ConsultasPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        carregarArquivo();

        txSql.setEditable(false);
        txSql.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        cb.addActionListener(e -> {
            Consulta c = (Consulta) cb.getSelectedItem();
            txSql.setText(c == null ? "" : c.sql);
            txSql.setCaretPosition(0);
        });
        JButton bRodar = new JButton("Executar");
        bRodar.addActionListener(e -> executar());

        JPanel norte = new JPanel(new BorderLayout(6, 0));
        norte.add(cb, BorderLayout.CENTER);
        norte.add(bRodar, BorderLayout.EAST);
        add(norte, BorderLayout.NORTH);

        JPanel resultado = new JPanel(new BorderLayout());
        resultado.add(new JScrollPane(tb), BorderLayout.CENTER);
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rodape.add(lbInfo);
        resultado.add(rodape, BorderLayout.SOUTH);
        add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(txSql), resultado), BorderLayout.CENTER);

        if (cb.getItemCount() > 0) {
            cb.setSelectedIndex(0);
        }
    }

    private void carregarArquivo() {
        try (InputStream in = getClass().getResourceAsStream("/consultas.sql")) {
            if (in == null) {
                txSql.setText("Arquivo consultas.sql não encontrado em resources.");
                return;
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String titulo = null;
            StringBuilder sb = new StringBuilder();
            String linha;
            while ((linha = r.readLine()) != null) {
                String t = linha.trim();
                if (t.startsWith("--")) {
                    if (t.matches("^-- C\\d\\d\\..*")) {
                        titulo = t.substring(3).trim();
                        if (titulo.length() > 120) {
                            titulo = titulo.substring(0, 117) + "...";
                        }
                    }
                    continue;
                }
                if (t.isEmpty()) {
                    continue;
                }
                sb.append(linha).append('\n');
                if (t.endsWith(";")) {
                    String sql = sb.toString().trim();
                    sql = sql.substring(0, sql.length() - 1);
                    if (titulo != null && sql.toUpperCase().startsWith("SELECT")) {
                        cb.addItem(new Consulta(titulo, sql));
                    }
                    sb.setLength(0);
                    titulo = null;
                }
            }
        } catch (Exception e) {
            txSql.setText("Erro ao ler consultas: " + e.getMessage());
        }
    }

    private void executar() {
        Consulta c = (Consulta) cb.getSelectedItem();
        if (c == null) {
            return;
        }
        try {
            tb.setModel(Db.modelo(c.sql));
            lbInfo.setText(tb.getRowCount() + " linha(s)");
        } catch (SQLException e) {
            Db.mostrarErro(this, e);
        }
    }
}
