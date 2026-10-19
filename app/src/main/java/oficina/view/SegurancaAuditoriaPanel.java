package oficina.view;

import oficina.util.Db;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.SQLException;

public class SegurancaAuditoriaPanel extends JPanel implements Recarregavel {

    private final JTable tbAuditoria = new JTable();
    private final JTextField txPayload = new JTextField("' OR '1'='1", 25);
    private final JTextArea txLogSql = new JTextArea(8, 70);
    private final JTable tbResultadoInjecao = new JTable();
    private final JTable tbUsuarios = new JTable();

    public SegurancaAuditoriaPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTabbedPane abasInternas = new JTabbedPane();
        abasInternas.addTab("Gatilhos e Auditoria", criarAbaAuditoria());
        abasInternas.addTab("Teste de SQL Injection", criarAbaInjecao());
        abasInternas.addTab("Usuários e Senhas (Hash + Sal)", criarAbaUsuarios());

        add(abasInternas, BorderLayout.CENTER);
    }

    private JPanel criarAbaAuditoria() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("Registros de auditoria gerados por gatilhos (tabela auditoria_sistema):"));

        JButton bAtualizar = new JButton("Atualizar");
        bAtualizar.addActionListener(e -> carregarAuditoria());
        topo.add(bAtualizar);

        p.add(topo, BorderLayout.NORTH);
        p.add(new JScrollPane(tbAuditoria), BorderLayout.CENTER);
        return p;
    }

    private JPanel criarAbaInjecao() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        topo.setBorder(BorderFactory.createTitledBorder("Teste de Injeção de SQL"));
        topo.add(new JLabel("Entrada:"));
        topo.add(txPayload);

        JButton bTestar = new JButton("Executar Consulta Parametrizada");
        bTestar.setFont(bTestar.getFont().deriveFont(Font.BOLD));
        bTestar.addActionListener(e -> testarInjecao());
        topo.add(bTestar);

        p.add(topo, BorderLayout.NORTH);

        txLogSql.setEditable(false);
        txLogSql.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        txLogSql.setText("Consulta base executada com PreparedStatement:\n"
                + "   SELECT id_cliente, nome, cpf_cnpj, telefone, email FROM cliente WHERE nome LIKE ?\n\n"
                + "Digite uma entrada no campo acima e clique em 'Executar Consulta Parametrizada' para verificar a resistência.");

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(txLogSql),
                new JScrollPane(tbResultadoInjecao));
        split.setResizeWeight(0.35);

        p.add(split, BorderLayout.CENTER);
        return p;
    }

    private JPanel criarAbaUsuarios() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("Controle de Usuários e Perfis da Aplicação (usuario_sistema):"));

        JButton bAtualizar = new JButton("Atualizar");
        bAtualizar.addActionListener(e -> carregarUsuarios());
        topo.add(bAtualizar);

        p.add(topo, BorderLayout.NORTH);
        p.add(new JScrollPane(tbUsuarios), BorderLayout.CENTER);

        JLabel lbInfoSeguranca = new JLabel("<html><small><b>Política de Segurança:</b> "
                + "Senhas armazenadas com SHA-256 e Sal aleatório individual de 128 bits (32 caracteres hexadecimais). "
                + "Por proteção contra vazamento, os hashes e sais são mantidos restritos ao banco de dados.</small></html>");
        lbInfoSeguranca.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        p.add(lbInfoSeguranca, BorderLayout.SOUTH);

        return p;
    }

    @Override
    public void recarregar() {
        carregarAuditoria();
        carregarUsuarios();
    }

    private void carregarAuditoria() {
        try {
            DefaultTableModel m = Db.modelo("SELECT id_auditoria AS `ID`, tabela_afetada AS `Tabela`, "
                    + "operacao AS `Operação`, id_registro AS `ID Registro`, usuario_banco AS `Usuário Banco`, "
                    + "DATE_FORMAT(data_hora, '%d/%m/%Y %H:%i:%s') AS `Data/Hora`, "
                    + "valores_anteriores AS `Valores Anteriores`, valores_posteriores AS `Valores Posteriores` "
                    + "FROM auditoria_sistema ORDER BY id_auditoria DESC");
            tbAuditoria.setModel(m);
        } catch (SQLException e) {
            Db.mostrarErro(this, e);
        }
    }

    private void testarInjecao() {
        String payload = txPayload.getText();
        String sql = "SELECT id_cliente AS `Cód.`, nome AS `Nome do Cliente`, cpf_cnpj AS `Documento`, "
                + "telefone AS `Telefone`, email AS `E-mail` FROM cliente WHERE nome LIKE ? LIMIT 50";

        txLogSql.setText("SQL preparado: " + sql + "\n"
                + "Parâmetro vinculado: \"%" + payload + "%\"\n"
                + "Comportamento: A entrada foi tratada como literal e não alterou a estrutura da query.\n");

        try {
            DefaultTableModel m = Db.modelo(sql, "%" + payload + "%");
            tbResultadoInjecao.setModel(m);
            txLogSql.append("Linhas retornadas: " + m.getRowCount());
        } catch (SQLException e) {
            Db.mostrarErro(this, e);
        }
    }

    private void carregarUsuarios() {
        try {
            DefaultTableModel m = Db.modelo("SELECT id_usuario AS `ID`, login AS `Login`, nome_completo AS `Nome`, "
                    + "perfil AS `Perfil`, IF(ativo = 1, 'Sim', 'Não') AS `Ativo`, "
                    + "DATE_FORMAT(data_criacao, '%d/%m/%Y %H:%i') AS `Data Criação` "
                    + "FROM usuario_sistema ORDER BY id_usuario");
            tbUsuarios.setModel(m);
        } catch (SQLException e) {
            Db.mostrarErro(this, e);
        }
    }
}
