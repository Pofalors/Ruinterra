import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class AtlasManifestLoader {

    public static AtlasManifest loadManifest(String atlasName, String filePath) {
        int columns = 0;
        AtlasManifest manifest = null;

        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) continue;
                if (line.startsWith("#")) continue;
                if (!line.contains("=")) continue;

                String[] parts = line.split("=", 2);
                String key = parts[0].trim();
                String value = parts[1].trim();

                if (key.equalsIgnoreCase("columns")) {
                    columns = Integer.parseInt(value);
                    manifest = new AtlasManifest(atlasName, columns);
                    continue;
                }

                if (manifest == null) continue;

                String[] coords = value.split(",");
                if (coords.length != 2) continue;

                int col = Integer.parseInt(coords[0].trim());
                int row = Integer.parseInt(coords[1].trim());

                manifest.addTile(key, col, row);
            }

            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return manifest;
    }
}
