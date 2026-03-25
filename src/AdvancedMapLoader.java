import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class AdvancedMapLoader {
    public ArrayList<AtlasLayer> atlasLayers = new ArrayList<>();

    public static AdvancedMapData loadAdvancedMap(String metadataPath) {
        String name = "";
        int cols = 0;
        int rows = 0;
        String tilesetName = "";
        String groundFile = "";
        String decorFile = "";
        String collisionFile = "";

        try {
            BufferedReader br = new BufferedReader(new FileReader(metadataPath));
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (!line.contains("=")) continue;

                String[] parts = line.split("=", 2);
                String key = parts[0].trim();
                String value = parts[1].trim();

                switch (key) {
                    case "name":
                        name = value;
                        break;
                    case "cols":
                        cols = Integer.parseInt(value);
                        break;
                    case "rows":
                        rows = Integer.parseInt(value);
                        break;
                    case "tileset":
                        tilesetName = value;
                        break;
                    case "ground":
                        groundFile = value;
                        break;
                    case "decor":
                        decorFile = value;
                        break;
                    case "collision":
                        collisionFile = value;
                        break;
                }
            }

            br.close();

            AdvancedMapData map = new AdvancedMapData(name, cols, rows);
            map.legacy = false;
            map.tilesetName = tilesetName;

            if (!groundFile.isEmpty()) {
                MapLayer groundLayer = loadLayer("ground", "res/maps/" + groundFile, rows, cols);
                map.layers.add(groundLayer);
            }

            if (!decorFile.isEmpty()) {
                MapLayer decorLayer = loadLayer("decor", "res/maps/" + decorFile, rows, cols);
                map.layers.add(decorLayer);
            }

            if (!collisionFile.isEmpty()) {
                MapLayer collisionLayer = loadLayer("collision", "res/maps/" + collisionFile, rows, cols);
                map.layers.add(collisionLayer);
            }

            return map;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static MapLayer loadLayer(String layerName, String filePath, int rows, int cols) {
        MapLayer layer = new MapLayer(layerName, rows, cols);

        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String line;
            int row = 0;

            while ((line = br.readLine()) != null && row < rows) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] numbers = line.split(" ");

                for (int col = 0; col < cols; col++) {
                    if (col < numbers.length) {
                        int val = Integer.parseInt(numbers[col]);

                        if (val == -1) {
                            layer.tiles[row][col] = new LayerTile("", -1);
                        } else {
                            layer.tiles[row][col] = new LayerTile("basic_terrain", val);
                        }
                    } else {
                        layer.tiles[row][col] = new LayerTile("", -1);
                    }
                }

                row++;
            }

            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return layer;
    }
}
