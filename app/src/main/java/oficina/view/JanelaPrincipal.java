package oficina.view;

import oficina.util.Conexao;
import oficina.util.SegurancaUtil.UsuarioSessao;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;

public class JanelaPrincipal extends JFrame {

    private final UsuarioSessao sessao;

    public JanelaPrincipal(UsuarioSessao sessao) {
        super("Oficina Mecânica - " + (sessao != null ? sessao.nomeCompleto + " (" + sessao.perfil + ")" : "Convidado"));
        this.sessao = sessao;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1300, 800);
        setLocationRelativeTo(null);

        JTabbedPane abas = new JTabbedPane();

        boolean admin = sessao == null || sessao.isAdmin();
        boolean atendente = sessao != null && sessao.isAtendente();
        boolean mecanico = sessao != null && sessao.isMecanico();

        // 1. Ordens de Servico: operacional para todos os perfis autenticados
        abas.addTab("Ordens de Serviço", new OrdemServicoPanel(sessao));

        // 2. Abas do Atendimento e Administrador
        if (admin || atendente) {
            abas.addTab("Veículos", new VeiculoPanel());
            abas.addTab("Clientes", clientes());
            abas.addTab("Consulta de OS", new ConsultaParametrizadaOSPanel());
            abas.addTab("Consulta de Peças", new ConsultaParametrizadaPecasPanel());
            abas.addTab("Relatório Consolidado", new RelatorioConsolidadoPanel());
        }

        // 3. Abas do Mecânico e Administrador (consulta de catálogo e especialidades)
        if (admin || mecanico) {
            abas.addTab("Peças", pecas());
            abas.addTab("Serviços", servicos());
            abas.addTab("Especialidades do Mecânico", new MecanicoEspecialidadePanel());
        }

        // 4. Abas restritas ao Administrador (gestão de equipe, segurança e ferramentas SQL)
        if (admin) {
            abas.addTab("Mecânicos", mecanicos());
            abas.addTab("Especialidades", especialidades());
            abas.addTab("Fornecedores", fornecedores());
            abas.addTab("Segurança e Auditoria", new SegurancaAuditoriaPanel());
            abas.addTab("Consultas SQL", new ConsultasPanel());
        }

        abas.addChangeListener(e -> recarregar(abas.getSelectedComponent()));

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        rodape.setBorder(BorderFactory.createEtchedBorder());
        JLabel lbStatus = new JLabel("Servidor: " + Conexao.getServidor()
                + " | Banco: " + Conexao.getBanco()
                + " | Usuário: " + (sessao != null ? sessao.login + " (" + sessao.perfil + ")" : "root"));
        lbStatus.setFont(new Font("SansSerif", Font.PLAIN, 11));
        rodape.add(lbStatus);

        add(abas, BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);

        recarregar(abas.getSelectedComponent());
    }

    private static void recarregar(Component c) {
        if (c instanceof Recarregavel) {
            ((Recarregavel) c).recarregar();
        }
    }

    private static CrudPanel clientes() {
        return new CrudPanel("cliente", new Campo[]{
            Campo.pk("id_cliente", "Código"),
            Campo.texto("nome", "Nome", true),
            Campo.texto("cpf_cnpj", "CPF/CNPJ", true).soDigitos(),
            Campo.texto("telefone", "Telefone", false),
            Campo.texto("email", "E-mail", false),
            Campo.texto("endereco", "Endereço", false),
            Campo.data("data_cadastro", "Data de cadastro", false).comPadrao()
        }, null);
    }

    private static CrudPanel mecanicos() {
        return new CrudPanel("mecanico", new Campo[]{
            Campo.pk("id_mecanico", "Código"),
            Campo.texto("nome", "Nome", true),
            Campo.texto("cpf", "CPF", true).soDigitos(),
            Campo.data("data_admissao", "Data de admissão", true),
            Campo.lookup("id_supervisor", "Supervisor", false,
                "SELECT id_mecanico, nome FROM mecanico ORDER BY nome")
        }, (v, id) -> id != null && id.equals(v.get("id_supervisor"))
                ? "Um mecânico não pode ser o próprio supervisor (RN20)." : null);
    }

    private static CrudPanel especialidades() {
        return new CrudPanel("especialidade", new Campo[]{
            Campo.pk("id_especialidade", "Código"),
            Campo.texto("nome_especialidade", "Nome", true)
        }, null);
    }

    private static CrudPanel fornecedores() {
        return new CrudPanel("fornecedor", new Campo[]{
            Campo.pk("id_fornecedor", "Código"),
            Campo.texto("nome", "Nome", true),
            Campo.texto("cnpj", "CNPJ", true).soDigitos(),
            Campo.texto("telefone", "Telefone", false)
        }, null);
    }

    private static CrudPanel pecas() {
        return new CrudPanel("peca", new Campo[]{
            Campo.pk("id_peca", "Código"),
            Campo.texto("nome", "Nome", true),
            Campo.texto("codigo_fabricante", "Código do fabricante", true),
            Campo.decimal("preco_unitario", "Preço unitário (R$)", true),
            Campo.inteiro("estoque_atual", "Estoque atual", false).comPadrao(),
            Campo.lookup("id_fornecedor", "Fornecedor", true,
                "SELECT id_fornecedor, nome FROM fornecedor ORDER BY nome")
        }, null);
    }

    private static CrudPanel servicos() {
        return new CrudPanel("servico_catalogo", new Campo[]{
            Campo.pk("id_servico", "Código"),
            Campo.texto("descricao", "Descrição", true),
            Campo.decimal("valor_padrao", "Valor padrão (R$)", true),
            Campo.decimal("tempo_estimado_horas", "Tempo estimado (h)", true)
        }, null);
    }
}
