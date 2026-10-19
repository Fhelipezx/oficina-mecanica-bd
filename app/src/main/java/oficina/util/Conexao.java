package oficina.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public final class Conexao {

    private static String servidor = "localhost:3306";
    private static String banco = "oficina_mecanica";
    private static String usuario = "app_oficina";
    private static String senha = "";
    private static String url;
    private static boolean carregadoDeArquivo = false;
    // Login do operador autenticado na aplicacao. Gravado em @usuario_aplicacao em TODA conexao aberta,
    // para que os gatilhos de auditoria (B6) registrem o autor real e nao apenas a conta tecnica.
    private static volatile String operador = null;

    private Conexao() { }

    public static boolean carregarDeArquivo() {
        String[] possiveisCaminhos = {
            "db.properties",
            "config.properties",
            "app/db.properties",
            "../db.properties"
        };

        for (String caminho : possiveisCaminhos) {
            File arquivo = new File(caminho);
            if (arquivo.exists() && arquivo.isFile()) {
                try (InputStream in = new FileInputStream(arquivo)) {
                    Properties prop = new Properties();
                    prop.load(in);
                    String host = prop.getProperty("db.host", "localhost");
                    String porta = prop.getProperty("db.port", "3306");
                    String nomeBanco = prop.getProperty("db.name", "oficina_mecanica");
                    String user = prop.getProperty("db.user", "app_oficina");
                    String pass = prop.getProperty("db.password");
                    if (pass == null) {
                        throw new IllegalArgumentException("Configure db.password no arquivo db.properties.");
                    }

                    configurar(host + ":" + porta, nomeBanco, user, pass);
                    testar();
                    carregadoDeArquivo = true;
                    return true;
                } catch (Exception e) {
                }
            }
        }
        return false;
    }

    public static void configurar(String servidorPorta, String usuario, String senha) {
        configurar(servidorPorta, "oficina_mecanica", usuario, senha);
    }

    public static void configurar(String servidorPorta, String nomeBanco, String usuario, String senha) {
        Conexao.servidor = servidorPorta;
        Conexao.banco = nomeBanco;
        Conexao.usuario = usuario;
        Conexao.senha = senha;
        Conexao.url = "jdbc:mysql://" + servidorPorta + "/" + nomeBanco
                + "?useSSL=false&allowPublicKeyRetrieval=true&yearIsDateType=false"
                + "&serverTimezone=America/Sao_Paulo&characterEncoding=UTF-8";
    }

    public static Connection abrir() throws SQLException {
        if (url == null) {
            configurar(servidor, banco, usuario, senha);
        }
        Connection c = DriverManager.getConnection(url, usuario, senha);
        String op = operador;
        if (op != null) {
            try (PreparedStatement ps = c.prepareStatement("SET @usuario_aplicacao = ?")) {
                ps.setString(1, "app:" + op);
                ps.execute();
            } catch (SQLException e) {
                c.close();
                throw e;
            }
        }
        return c;
    }

    public static void definirOperador(String login) {
        operador = login;
    }

    public static void testar() throws SQLException {
        try (Connection c = abrir()) {
        }
    }

    public static String getServidor() { return servidor; }
    public static String getBanco() { return banco; }
    public static String getUsuario() { return usuario; }
    public static boolean isCarregadoDeArquivo() { return carregadoDeArquivo; }
}
