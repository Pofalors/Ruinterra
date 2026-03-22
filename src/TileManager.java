import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class TileManager {
    GamePanel gp;
    public Tile[] tile;

    ArrayList<String> fileNames = new ArrayList<>();
    ArrayList<String> collisionStatus = new ArrayList<>();

    public ArrayList<MapData> maps = new ArrayList<>();

    public TileManager(GamePanel gp) {
        this.gp = gp;

        loadTileData();
        tile = new Tile[fileNames.size()];
        getTileImage();

        loadAllMaps();

        gp.maxMaps = maps.size();

        // Αρχικό active map = 0
        if (!maps.isEmpty()) {
            applyMapSizeToGamePanel(0);
        }
    }

    private void loadTileData() {
        try {
            File file = new File("res/maps/tiledata.txt");
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);

            String line;
            while ((line = br.readLine()) != null) {
                fileNames.add(line);
                collisionStatus.add(br.readLine());
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadAllMaps() {
        // ===== ΕΔΩ ΔΗΛΩΝΕΙΣ ΟΛΟΥΣ ΤΟΥΣ ΧΑΡΤΕΣ =====
        addMap("Overworld", "res/maps/worldmap.txt");
        addMap("Dungeon01", "res/maps/dungeon01.txt");
        addMap("MerchantHouse", "res/maps/interior01.txt");
        addMap("Town", "res/maps/town.txt");
        addMap("BigHouse", "res/maps/interior01.txt");
        addMap("SmallHouse", "res/maps/interior01.txt");
        // region 1
        addMap("Region1Route01", "res/maps/region1_route01.txt");
        addMap("Region1Town01", "res/maps/region1_town01.txt");
        addMap("Region1Inn01", "res/maps/region1_inn01.txt");
        addMap("Region1Shop01", "res/maps/region1_shop01.txt");
        addMap("Region1Cave01", "res/maps/region1_cave01.txt");
    }

    public void addMap(String name, String filePath) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));

            ArrayList<String[]> rowsList = new ArrayList<>();
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                rowsList.add(line.split(" "));
            }

            br.close();

            if (rowsList.isEmpty()) {
                System.out.println("Κενός χάρτης: " + filePath);
                return;
            }

            int rows = rowsList.size();
            int cols = rowsList.get(0).length;

            MapData map = new MapData(name, filePath, cols, rows);

            for (int row = 0; row < rows; row++) {
                String[] numbers = rowsList.get(row);

                for (int col = 0; col < cols; col++) {
                    if (col < numbers.length) {
                        map.tiles[row][col] = Integer.parseInt(numbers[col]);
                    } else {
                        map.tiles[row][col] = 0;
                    }
                }
            }

            maps.add(map);
            System.out.println("Χάρτης φορτώθηκε: " + name + " (" + cols + "x" + rows + ") από " + filePath);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Σφάλμα φόρτωσης χάρτη: " + filePath);
        }
    }

    public void applyMapSizeToGamePanel(int mapIndex) {
        if (mapIndex < 0 || mapIndex >= maps.size()) return;

        MapData map = maps.get(mapIndex);

        gp.maxWorldCol = map.cols;
        gp.maxWorldRow = map.rows;
        gp.worldWidth = gp.tileSize * gp.maxWorldCol;
        gp.worldHeight = gp.tileSize * gp.maxWorldRow;
    }

    public MapData getCurrentMapData() {
        if (gp.currentMap < 0 || gp.currentMap >= maps.size()) return null;
        return maps.get(gp.currentMap);
    }

    public int getTileNum(int mapIndex, int row, int col) {
        if (mapIndex < 0 || mapIndex >= maps.size()) return 0;

        MapData map = maps.get(mapIndex);
        if (row < 0 || row >= map.rows || col < 0 || col >= map.cols) return 0;

        return map.tiles[row][col];
    }

    public boolean isTileCollision(int mapIndex, int row, int col) {
        int tileNum = getTileNum(mapIndex, row, col);

        if (tileNum < 0 || tileNum >= tile.length || tile[tileNum] == null) {
            return false;
        }

        return tile[tileNum].collision;
    }

    public boolean isValidTile(int mapIndex, int row, int col) {
        if (mapIndex < 0 || mapIndex >= maps.size()) return false;

        MapData map = maps.get(mapIndex);
        return row >= 0 && row < map.rows && col >= 0 && col < map.cols;
    }

    public void getTileImage() {
        for (int i = 0; i < fileNames.size(); i++) {
            String fileName = fileNames.get(i);
            boolean collision = collisionStatus.get(i).equals("true");
            setup(i, fileName, collision);
        }
    }

    public void setup(int index, String imageName, boolean collision) {
        try {
            tile[index] = new Tile();
            tile[index].image = ImageIO.read(new File("res/tiles/" + imageName));
            tile[index].collision = collision;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void draw(Graphics2D g2) {
        MapData currentMapData = getCurrentMapData();
        if (currentMapData == null) return;

        int worldCol = 0;
        int worldRow = 0;

        while (worldCol < currentMapData.cols && worldRow < currentMapData.rows) {
            int tileNum = currentMapData.tiles[worldRow][worldCol];

            int worldX = worldCol * gp.tileSize;
            int worldY = worldRow * gp.tileSize;
            int screenX = worldX - gp.worldX;
            int screenY = worldY - gp.worldY;

            if (worldX + gp.tileSize > gp.worldX &&
                worldX - gp.tileSize < gp.worldX + gp.screenWidth &&
                worldY + gp.tileSize > gp.worldY &&
                worldY - gp.tileSize < gp.worldY + gp.screenHeight) {

                g2.drawImage(tile[tileNum].image, screenX, screenY, gp.tileSize, gp.tileSize, null);
            }

            worldCol++;
            if (worldCol == currentMapData.cols) {
                worldCol = 0;
                worldRow++;
            }
        }
    }
}