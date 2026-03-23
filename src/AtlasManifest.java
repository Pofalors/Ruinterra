import java.util.HashMap;
import java.util.Map;

public class AtlasManifest {
    public String atlasName;
    public int columns;
    public Map<String, Integer> tileIds = new HashMap<>();

    public AtlasManifest(String atlasName, int columns) {
        this.atlasName = atlasName;
        this.columns = columns;
    }

    public void addTile(String name, int col, int row) {
        int index = row * columns + col;
        tileIds.put(name, index);
    }

    public int getTileId(String name) {
        Integer value = tileIds.get(name);
        return value != null ? value : -1;
    }

    public boolean hasTile(String name) {
        return tileIds.containsKey(name);
    }

    public int getRequiredTileId(String name) {
    Integer value = tileIds.get(name);
        if (value == null) {
            throw new RuntimeException("AtlasManifest missing tile: " + name);
        }
        return value;
    }

    public boolean hasTerrain3x3(String prefix) {
        return hasTile(prefix + "_TL") &&
            hasTile(prefix + "_T") &&
            hasTile(prefix + "_TR") &&
            hasTile(prefix + "_L") &&
            hasTile(prefix + "_R") &&
            hasTile(prefix + "_BL") &&
            hasTile(prefix + "_B") &&
            hasTile(prefix + "_BR");
    }

    public boolean hasBottom3x3(String prefix) {
        return hasTile(prefix + "_BTL") &&
            hasTile(prefix + "_BT") &&
            hasTile(prefix + "_BTR") &&
            hasTile(prefix + "_BML") &&
            hasTile(prefix + "_FILL") &&
            hasTile(prefix + "_BMR") &&
            hasTile(prefix + "_BBL") &&
            hasTile(prefix + "_BB") &&
            hasTile(prefix + "_BBR");
    }

    public boolean hasVariant2x2(String prefix) {
        return hasTile(prefix + "_1") &&
            hasTile(prefix + "_2") &&
            hasTile(prefix + "_3") &&
            hasTile(prefix + "_4");
    }
}
