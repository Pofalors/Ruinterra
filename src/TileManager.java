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
    public int[][][] mapTileNum; // 3D πίνακας [map][row][col]
    ArrayList<String> fileNames = new ArrayList<>();
    ArrayList<String> collisionStatus = new ArrayList<>();
    
    public TileManager(GamePanel gp) {
        this.gp = gp;

        // READ TILE DATA FILE - ΧΡΗΣΙΜΟΠΟΙΩΝΤΑΣ FileReader
        try {
            File file = new File("res/maps/tiledata.txt");
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            
            // GETTING TILE NAMES AND COLLISION DATA FROM THE FILE
            String line;

            while((line = br.readLine()) != null){
                fileNames.add(line);
                collisionStatus.add(br.readLine());
            }
            br.close();
        } catch(IOException e) {
            e.printStackTrace();
        }

        // INITIALIZE THE TILE ARRAY BASED ON THE fileNames size
        tile = new Tile[fileNames.size()];
        getTileImage();
        
        // Get the maxWorldRow & maxWorldCol - ΧΡΗΣΙΜΟΠΟΙΩΝΤΑΣ FileReader
        try {
            File file = new File("res/maps/worldmap.txt");
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            
            String line2 = br.readLine();
            String maxTile[] = line2.split(" ");

            gp.maxWorldCol = maxTile.length;
            gp.maxWorldRow = maxTile.length;
            gp.worldWidth = gp.tileSize * gp.maxWorldCol;
            gp.worldHeight = gp.tileSize * gp.maxWorldRow;
            mapTileNum = new int[gp.maxMaps][gp.maxWorldRow][gp.maxWorldCol];

            br.close();
        } catch (IOException e) {
            System.out.println("Exception!");
        }

        loadMap("res/maps/worldmap.txt", 0); // Overworld
        loadMap("res/maps/dungeon01.txt", 1); // Dungeon
        loadMap("res/maps/interior01.txt", 2); // Merchant house
        loadMap("res/maps/town.txt", 3); // Main Town
        loadMap("res/maps/interior01.txt", 4); // Μεγάλο σπίτι interior
        loadMap("res/maps/interior01.txt", 5); // Μικρό σπίτι interior
    }
    
    public void getTileImage() {

        for(int i = 0; i < fileNames.size(); i++){
            String fileName;
            boolean collision;

            //Get a file name
            fileName = fileNames.get(i);

            //Get a collision Status
            if(collisionStatus.get(i).equals("true")){
                collision = true;
            } else {
                collision = false;
            }
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
    
    public void loadMap(String filePath, int mapIndex) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            
            int row = 0;
            String line;
            
            while ((line = br.readLine()) != null && row < gp.maxWorldRow) {
                String[] numbers = line.split(" ");
                
                for (int col = 0; col < gp.maxWorldCol && col < numbers.length; col++) {
                    int num = Integer.parseInt(numbers[col]);
                    mapTileNum[mapIndex][row][col] = num;
                }
                
                row++;
            }
            
            br.close();
            System.out.println("Χάρτης " + mapIndex + " φορτώθηκε από " + filePath);
            
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Σφάλμα: Δεν βρέθηκε το αρχείο! Φόρτωμα random χάρτη...");
        }
    }
    
    public void draw(Graphics2D g2) {
        int worldCol = 0;
        int worldRow = 0;
        
        while (worldCol < gp.maxWorldCol && worldRow < gp.maxWorldRow) {
            int tileNum = mapTileNum[gp.currentMap][worldRow][worldCol];
            
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
            if (worldCol == gp.maxWorldCol) {
                worldCol = 0;
                worldRow++;
            }
        }
    }
}