package oficina.util;

import oficina.view.Item;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Db {

    private Db() { }

    public interface Transacao {
        void executar(Connection c) throws SQLException;
    }

    private static void ligar(PreparedStatement ps, Object[] p) throws SQLException {
        if (p != null) {
            for (int i = 0; i < p.length; i++) {
                ps.setObject(i + 1, p[i]);
            }
        }
    }

    public static int executar(String sql, Object... p) throws SQLException {
        try (Connection c = Conexao.abrir()) {
            return executar(c, sql, p);
        }
    }

    public static int executar(Connection c, String sql, Object... p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ligar(ps, p);
            return ps.executeUpdate();
        }
    }

    public static long inserir(Connection c, String sql, Object... p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ligar(ps, p);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        }
    }

    public static void transacao(Transacao t) throws SQLException {
        try (Connection c = Conexao.abrir()) {
            c.setAutoCommit(false);
            try {
                t.executar(c);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                try {
                    c.rollback();
                } catch (SQLException re) {
                }
                throw e;
            }
        }
    }

    public static List<String[]> consultar(String sql, Object... p) throws SQLException {
        try (Connection c = Conexao.abrir()) {
            return consultar(c, sql, p);
        }
    }

    public static List<String[]> consultar(Connection c, String sql, Object... p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ligar(ps, p);
            try (ResultSet rs = ps.executeQuery()) {
                int n = rs.getMetaData().getColumnCount();
                List<String[]> r = new ArrayList<>();
                while (rs.next()) {
                    String[] l = new String[n];
                    for (int i = 0; i < n; i++) {
                        l[i] = rs.getString(i + 1);
                    }
                    r.add(l);
                }
                return r;
            }
        }
    }

    public static String valor(String sql, Object... p) throws SQLException {
        List<String[]> r = consultar(sql, p);
        return r.isEmpty() ? null : r.get(0)[0];
    }

    public static List<Item> itens(String sql, Object... p) throws SQLException {
        List<Item> r = new ArrayList<>();
        for (String[] l : consultar(sql, p)) {
            r.add(new Item(l[0], l[1], Arrays.copyOfRange(l, 2, l.length)));
        }
        return r;
    }

    public static DefaultTableModel modelo(String sql, Object... p) throws SQLException {
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ligar(ps, p);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                String[] nomes = new String[n];
                for (int i = 0; i < n; i++) {
                    nomes[i] = md.getColumnLabel(i + 1);
                }
                DefaultTableModel m = new DefaultTableModel(nomes, 0) {
                    @Override
                    public boolean isCellEditable(int row, int col) {
                        return false;
                    }
                };
                while (rs.next()) {
                    Object[] linha = new Object[n];
                    for (int i = 0; i < n; i++) {
                        linha[i] = rs.getString(i + 1);
                    }
                    m.addRow(linha);
                }
                return m;
            }
        }
    }

    public static String decimal(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim().replace(',', '.');
        return t.matches("\\d+(\\.\\d+)?") ? t : null;
    }

    public static String inteiro(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.matches("\\d+") ? t : null;
    }

    public static String normalizarData(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        String iso;
        if (t.matches("\\d{4}-\\d{2}-\\d{2}")) {
            iso = t;
        } else if (t.matches("\\d{2}/\\d{2}/\\d{4}")) {
            iso = t.substring(6) + "-" + t.substring(3, 5) + "-" + t.substring(0, 2);
        } else {
            return null;
        }
        try {
            LocalDate.parse(iso);
            return iso;
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static void aviso(Component pai, String msg) {
        JOptionPane.showMessageDialog(pai, msg, "Atenção", JOptionPane.WARNING_MESSAGE);
    }

    public static void informacao(Component pai, String msg, String titulo) {
        JOptionPane.showMessageDialog(pai, msg, titulo, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void mostrarErro(Component pai, Exception e) {
        System.err.println("[DIAGNÓSTICO TÉCNICO] Exceção capturada: " + e.getMessage());

        String msgAmigavel;
        String orientacao = "Por favor, revise os dados informados ou tente novamente.";

        if (e instanceof SQLException) {
            SQLException sqlEx = (SQLException) e;
            int errCode = sqlEx.getErrorCode();
            String sqlState = sqlEx.getSQLState() != null ? sqlEx.getSQLState() : "";
            String raw = sqlEx.getMessage() != null ? sqlEx.getMessage() : "";

            if ("45000".equals(sqlState) || raw.contains("RN") || raw.contains("Estoque insuficiente")) {
                msgAmigavel = raw;
                orientacao = "Ajuste os valores para atender à regra do sistema.";
            } else if (errCode == 1062 || ("23000".equals(sqlState) && (raw.contains("Duplicate") || raw.contains("uq_") || raw.contains("PRIMARY")))) {
                msgAmigavel = "Registro duplicado: já existe um cadastro com este mesmo identificador único (como CPF, CNPJ, Placa ou Código).";
                orientacao = "Verifique se o cadastro já existe ou informe outro valor.";
            } else if (errCode == 1451 || errCode == 1452 || raw.contains("foreign key constraint fails") || raw.contains("fk_")) {
                msgAmigavel = "Restrição de integridade referencial: a operação envolve um registro inexistente ou vinculado a outros dados.";
                orientacao = "Certifique-se de que o registro selecionado está cadastrado e não possui dependências ativas.";
            } else if (errCode == 3819 || raw.contains("Check constraint") || raw.contains("ck_")) {
                msgAmigavel = "Restrição de validação: os valores informados violam regras de domínio do sistema (ex: valores, horas ou quantidades inválidas).";
                orientacao = "Preencha apenas números válidos e maiores que zero.";
            } else if (errCode == 1045 || errCode == 1142 || raw.contains("Access denied") || raw.contains("command denied")) {
                msgAmigavel = "Permissão insuficiente: o usuário ou perfil conectado não possui autorização para esta operação.";
                orientacao = "Solicite autorização a um Administrador do sistema.";
            } else if (sqlState.startsWith("08") || raw.contains("Communications link") || raw.contains("Connection refused")) {
                msgAmigavel = "Falha de comunicação com o banco de dados MySQL.";
                orientacao = "Verifique se o serviço do MySQL Server está em execução na porta 3306.";
            } else {
                msgAmigavel = "Não foi possível concluir a operação solicitada no banco de dados.";
            }
        } else {
            msgAmigavel = e.getMessage() != null ? e.getMessage() : "Ocorreu uma falha operacional.";
        }

        String textoExibicao = msgAmigavel + "\n\n" + orientacao;
        JOptionPane.showMessageDialog(pai, textoExibicao, "Aviso do Sistema", JOptionPane.WARNING_MESSAGE);
    }
}
