package oficina.view;

/** Descreve uma coluna para o CRUD genérico. */
public class Campo {

    public enum Tipo { TEXTO, INTEIRO, DECIMAL, DATA, ENUM, LOOKUP }

    public final String coluna;
    public final String rotulo;
    public final Tipo tipo;
    public boolean pk;
    public boolean obrigatorio;
    public boolean padrao;          // coluna tem DEFAULT no banco: pode ficar em branco
    public boolean apenasDigitos;   // remove pontos e traços (CPF/CNPJ)
    public String[] opcoes;         // para ENUM
    public String lookupSql;        // para LOOKUP: SELECT id, rotulo FROM ...

    private Campo(String coluna, String rotulo, Tipo tipo) {
        this.coluna = coluna;
        this.rotulo = rotulo;
        this.tipo = tipo;
    }

    public static Campo pk(String coluna, String rotulo) {
        Campo c = new Campo(coluna, rotulo, Tipo.INTEIRO);
        c.pk = true;
        return c;
    }

    public static Campo texto(String coluna, String rotulo, boolean obrigatorio) {
        return novo(coluna, rotulo, Tipo.TEXTO, obrigatorio);
    }

    public static Campo inteiro(String coluna, String rotulo, boolean obrigatorio) {
        return novo(coluna, rotulo, Tipo.INTEIRO, obrigatorio);
    }

    public static Campo decimal(String coluna, String rotulo, boolean obrigatorio) {
        return novo(coluna, rotulo, Tipo.DECIMAL, obrigatorio);
    }

    public static Campo data(String coluna, String rotulo, boolean obrigatorio) {
        return novo(coluna, rotulo, Tipo.DATA, obrigatorio);
    }

    public static Campo enumerado(String coluna, String rotulo, boolean obrigatorio, String... opcoes) {
        Campo c = novo(coluna, rotulo, Tipo.ENUM, obrigatorio);
        c.opcoes = opcoes;
        return c;
    }

    public static Campo lookup(String coluna, String rotulo, boolean obrigatorio, String sql) {
        Campo c = novo(coluna, rotulo, Tipo.LOOKUP, obrigatorio);
        c.lookupSql = sql;
        return c;
    }

    private static Campo novo(String coluna, String rotulo, Tipo tipo, boolean obrigatorio) {
        Campo c = new Campo(coluna, rotulo, tipo);
        c.obrigatorio = obrigatorio;
        return c;
    }

    public Campo comPadrao() {
        this.padrao = true;
        return this;
    }

    public Campo soDigitos() {
        this.apenasDigitos = true;
        return this;
    }
}
