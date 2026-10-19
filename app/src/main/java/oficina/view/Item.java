package oficina.view;

import javax.swing.JComboBox;
import java.util.Objects;

/** Elemento de combo: guarda o id (valor do banco) e o texto mostrado. */
public final class Item {

    public final String id;
    public final String rotulo;
    public final String[] extras;

    public Item(String id, String rotulo, String... extras) {
        this.id = id;
        this.rotulo = rotulo;
        this.extras = extras;
    }

    @Override
    public String toString() {
        return rotulo;
    }

    public static void selecionar(JComboBox<Item> cb, String id) {
        for (int i = 0; i < cb.getItemCount(); i++) {
            if (Objects.equals(cb.getItemAt(i).id, id)) {
                cb.setSelectedIndex(i);
                return;
            }
        }
        if (cb.getItemCount() > 0) {
            cb.setSelectedIndex(0);
        }
    }

    public static Item item(JComboBox<Item> cb) {
        return (Item) cb.getSelectedItem();
    }

    public static String id(JComboBox<Item> cb) {
        Item it = item(cb);
        return it == null ? null : it.id;
    }
}
