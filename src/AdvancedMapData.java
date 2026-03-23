import java.util.ArrayList;

public class AdvancedMapData {
    public String name;
    public int cols;
    public int rows;
    public String tilesetName;
    public boolean legacy = true;

    // legacy support
    public int[][] legacyTiles;

    // advanced support
    public ArrayList<MapLayer> layers = new ArrayList<>();

    public AdvancedMapData(String name, int cols, int rows) {
        this.name = name;
        this.cols = cols;
        this.rows = rows;
        this.legacyTiles = new int[rows][cols];
    }

    public MapLayer getLayer(String layerName) {
        for (MapLayer layer : layers) {
            if (layer.name.equalsIgnoreCase(layerName)) {
                return layer;
            }
        }
        return null;
    }
}
