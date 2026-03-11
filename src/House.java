import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class House {
    public String name;
    public Decoration exterior;
    public int exteriorMap;
    public int interiorMap;
    public Rectangle doorArea;
    public int spawnX, spawnY;
    public int exitX, exitY;
    
    // Για την animated πόρτα (ολόκληρο το σπίτι)
    public AnimatedDoor door;
    public boolean hasAnimatedDoor = false;
    public int doorTileX, doorTileY; // Η θέση της πόρτας στο tile grid (για reference)
    
    // Για custom collision (πίνακας boolean)
    public boolean[][] collisionMap; // true = δεν περνάς, false = περνάς
    
    // Reference στο GamePanel για υπολογισμούς
    public GamePanel gp;
    
    public House(String name, int exteriorMap, Decoration exterior, Rectangle doorArea,
                 int interiorMap, int spawnX, int spawnY, int exitX, int exitY, GamePanel gp) {
        this.name = name;
        this.exteriorMap = exteriorMap;
        this.exterior = exterior;
        this.doorArea = doorArea;
        this.interiorMap = interiorMap;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.exitX = exitX;
        this.exitY = exitY;
        this.gp = gp;
    }
    
    // Μέθοδος για ορισμό custom collision (π.χ. 6x6 πίνακας)
    public void setCollisionMap(boolean[][] collisionMap) {
        this.collisionMap = collisionMap;
        updateDecorationCollision();
    }
    
    // Ανανέωσε τα collision rectangles του decoration με βάση τον πίνακα
    private void updateDecorationCollision() {
        if (exterior == null || gp == null) return;
        
        ArrayList<Rectangle> customRects = new ArrayList<>();
        
        int tileCols = exterior.width / gp.tileSize;
        int tileRows = exterior.height / gp.tileSize;
        
        for (int row = 0; row < tileRows; row++) {
            for (int col = 0; col < tileCols; col++) {
                if (collisionMap != null && row < collisionMap.length && col < collisionMap[0].length) {
                    if (collisionMap[row][col]) {
                        // Αν το tile αυτό έχει collision, πρόσθεσε rectangle
                        int x = exterior.worldX + col * gp.tileSize;
                        int y = exterior.worldY + row * gp.tileSize;
                        customRects.add(new Rectangle(x, y, gp.tileSize, gp.tileSize));
                    }
                }
            }
        }
        
        // Αν δεν ορίστηκε collision map, κάνε όλο το decoration solid
        if (customRects.isEmpty() && collisionMap == null) {
            customRects.add(new Rectangle(exterior.worldX, exterior.worldY, 
                                          exterior.width, exterior.height));
        }
        
        // Αντικατέστησε τα collision rectangles
        exterior.collisionRects = customRects;
        exterior.solid = !customRects.isEmpty();
    }
    
    public void setAnimatedDoor(AnimatedDoor door, int doorTileX, int doorTileY) {
        this.door = door;
        this.doorTileX = doorTileX;
        this.doorTileY = doorTileY;
        this.hasAnimatedDoor = true;
        
        // Σημαντικό: Το εξωτερικό decoration θα αλλάζει εικόνα από το animation
        door.setTargetDecoration(exterior);
    }
    
    public void updateDoor() {
        if (hasAnimatedDoor) {
            door.update();
        }
    }
    
    // Μέθοδος για είσοδο στο σπίτι (ξεκινά το animation)
    public void enterHouse() {
        if (hasAnimatedDoor) {
            door.startOpening();
        }
    }
    
    // Μέθοδος για έξοδο από το σπίτι (ξεκινά το animation)
    public void exitHouse() {
        if (hasAnimatedDoor) {
            door.startClosing();
        }
    }
    
    // Έλεγχος αν η πόρτα είναι ανοιχτή (για να μπορεί να μπει)
    public boolean isDoorOpen() {
        return !hasAnimatedDoor || door.isOpen;
    }
    
    // Έλεγχος αν η πόρτα είναι κλειστή
    public boolean isDoorClosed() {
        return !hasAnimatedDoor || !door.isOpen;
    }
}