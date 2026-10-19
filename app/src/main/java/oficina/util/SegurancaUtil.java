package oficina.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class SegurancaUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private SegurancaUtil() { }

    public static String gerarSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        StringBuilder sb = new StringBuilder();
        for (byte b : salt) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String calcularHash(String senha, String salt) {
        if (senha == null || salt == null) {
            throw new IllegalArgumentException("Senha e sal não podem ser nulos.");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String entrada = senha + salt;
            byte[] digesto = md.digest(entrada.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digesto) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 não disponível.", e);
        }
    }

    public static boolean verificarSenha(String senhaInformada, String saltArmazenado, String hashArmazenado) {
        if (senhaInformada == null || saltArmazenado == null || hashArmazenado == null) {
            return false;
        }
        String hashCalculado = calcularHash(senhaInformada, saltArmazenado);
        return hashCalculado.equalsIgnoreCase(hashArmazenado);
    }

    public static class UsuarioSessao {
        public final int idUsuario;
        public final String login;
        public final String nomeCompleto;
        public final String perfil;

        public UsuarioSessao(int idUsuario, String login, String nomeCompleto, String perfil) {
            this.idUsuario = idUsuario;
            this.login = login;
            this.nomeCompleto = nomeCompleto;
            this.perfil = perfil;
        }

        public boolean isAdmin() {
            return "Administrador".equalsIgnoreCase(perfil);
        }

        public boolean isMecanico() {
            return "Mecanico".equalsIgnoreCase(perfil);
        }

        public boolean isAtendente() {
            return "Atendente".equalsIgnoreCase(perfil);
        }
    }
}
