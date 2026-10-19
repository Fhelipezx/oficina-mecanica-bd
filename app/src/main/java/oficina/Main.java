package oficina;

import oficina.util.Conexao;
import oficina.util.Db;
import oficina.util.SegurancaUtil;
import oficina.util.SegurancaUtil.UsuarioSessao;
import oficina.view.JanelaPrincipal;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
            }

            if (!conectarBanco()) {
                System.exit(0);
            }

            UsuarioSessao sessao = autenticarUsuario();
            if (sessao == null) {
                System.exit(0);
            }

            Conexao.definirOperador(sessao.login);
            new JanelaPrincipal(sessao).setVisible(true);
        });
    }

    private static boolean conectarBanco() {
        if (Conexao.carregarDeArquivo()) {
            return true;
        }

        JTextField servidor = new JTextField("localhost:3306");
        JTextField usuario = new JTextField("app_oficina");
        JPasswordField senha = new JPasswordField();

        JPanel p = new JPanel(new GridLayout(3, 2, 6, 6));
        p.add(new JLabel("Servidor:"));
        p.add(servidor);
        p.add(new JLabel("Usuário:"));
        p.add(usuario);
        p.add(new JLabel("Senha:"));
        p.add(senha);

        while (true) {
            int r = JOptionPane.showConfirmDialog(null, p,
                    "Conectar ao MySQL (oficina_mecanica)",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (r != JOptionPane.OK_OPTION) {
                return false;
            }

            Conexao.configurar(servidor.getText().trim(), usuario.getText().trim(), new String(senha.getPassword()));
            try {
                Conexao.testar();
                return true;
            } catch (SQLException e) {
                Db.mostrarErro(null, e);
            }
        }
    }

    private static UsuarioSessao autenticarUsuario() {
        try {
            Db.consultar("SELECT 1 FROM usuario_sistema LIMIT 1");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Não foi possível consultar a tabela de usuários (usuario_sistema).\n\n"
                    + "Certifique-se de que todos os scripts (01 a 07) foram executados no banco de dados\n"
                    + "e que a conexão possui permissão de leitura.\n\n"
                    + "O acesso foi bloqueado por segurança.",
                    "Falha de Configuração do Banco", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        JTextField txLogin = new JTextField();
        JPasswordField txSenha = new JPasswordField();

        JPanel p = new JPanel(new BorderLayout(8, 8));
        JPanel campos = new JPanel(new GridLayout(2, 2, 6, 6));
        campos.add(new JLabel("Login:"));
        campos.add(txLogin);
        campos.add(new JLabel("Senha:"));
        campos.add(txSenha);
        p.add(campos, BorderLayout.CENTER);

        JLabel dica = new JLabel("<html><small>Acesso restrito por perfil (Administrador, Atendente ou Mecânico).<br>"
                + "Credenciais de demonstração documentadas no README do projeto.</small></html>");
        dica.setForeground(new Color(90, 90, 90));
        dica.setFont(new Font("SansSerif", Font.PLAIN, 10));
        p.add(dica, BorderLayout.SOUTH);

        while (true) {
            int r = JOptionPane.showConfirmDialog(null, p,
                    "Autenticação de Usuário",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (r != JOptionPane.OK_OPTION) {
                return null;
            }

            String login = txLogin.getText().trim();
            String senha = new String(txSenha.getPassword());

            if (login.isEmpty() || senha.isEmpty()) {
                Db.aviso(null, "Preencha o login e a senha.");
                continue;
            }

            try {
                List<String[]> usuarios = Db.consultar(
                        "SELECT id_usuario, nome_completo, senha_hash, salt, perfil, ativo "
                        + "FROM usuario_sistema WHERE login = ?", login);

                if (usuarios.isEmpty()) {
                    Db.aviso(null, "Login ou senha inválidos.");
                    continue;
                }

                String[] u = usuarios.get(0);
                int id = Integer.parseInt(u[0]);
                String nome = u[1];
                String hashArmazenado = u[2];
                String saltArmazenado = u[3];
                String perfil = u[4];
                boolean ativo = "1".equals(u[5]);

                if (!ativo) {
                    Db.aviso(null, "Usuário desativado pelo administrador.");
                    continue;
                }

                if (SegurancaUtil.verificarSenha(senha, saltArmazenado, hashArmazenado)) {
                    return new UsuarioSessao(id, login, nome, perfil);
                } else {
                    Db.aviso(null, "Login ou senha inválidos.");
                }

            } catch (SQLException e) {
                Db.mostrarErro(null, e);
            }
        }
    }
}
