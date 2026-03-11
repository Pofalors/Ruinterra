import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.awt.Rectangle;
import javax.imageio.ImageIO;

public class Decoration {
    public BufferedImage image;
    public String name;
    public int worldX, worldY;
    public int width, height;
    public boolean solid;
    public ArrayList<Rectangle> collisionRects; // Για custom collision (π.χ. σπίτια)
    
    // Constructor 1: Απλό decoration με ένα Rectangle collision
    public Decoration(String name, BufferedImage image, int worldX, int worldY, 
                      int width, int height, boolean solid) {
        this.name = name;
        this.image = image;
        this.worldX = worldX;
        this.worldY = worldY;
        this.width = width;
        this.height = height;
        this.solid = solid;
        this.collisionRects = new ArrayList<>();
        
        // Αν είναι solid, πρόσθεσε ολόκληρο το rect
        if (solid) {
            this.collisionRects.add(new Rectangle(worldX, worldY, width, height));
        }
    }
    
    // Constructor 2: Απλό decoration με path εικόνας
    public Decoration(String name, String imagePath, int worldX, int worldY, 
                      int width, int height, boolean solid) {
        this.name = name;
        this.worldX = worldX;
        this.worldY = worldY;
        this.width = width;
        this.height = height;
        this.solid = solid;
        this.collisionRects = new ArrayList<>();
        
        try {
            this.image = ImageIO.read(new File(imagePath));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Αν είναι solid, πρόσθεσε ολόκληρο το rect
        if (solid) {
            this.collisionRects.add(new Rectangle(worldX, worldY, width, height));
        }
    }
    
    // Constructor 3: Για σύνθετο collision (π.χ. σπίτι με πολλά rectangles)
    public Decoration(String name, BufferedImage image, int worldX, int worldY, 
                      int width, int height, ArrayList<Rectangle> collisionRects) {
        this.name = name;
        this.image = image;
        this.worldX = worldX;
        this.worldY = worldY;
        this.width = width;
        this.height = height;
        this.collisionRects = collisionRects;
        this.solid = (collisionRects != null && !collisionRects.isEmpty());
    }
    
    // Constructor 4: Για σύνθετο collision με path εικόνας
    public Decoration(String name, String imagePath, int worldX, int worldY, 
                      int width, int height, ArrayList<Rectangle> collisionRects) {
        this.name = name;
        this.worldX = worldX;
        this.worldY = worldY;
        this.width = width;
        this.height = height;
        this.collisionRects = collisionRects;
        this.solid = (collisionRects != null && !collisionRects.isEmpty());
        
        try {
            this.image = ImageIO.read(new File(imagePath));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
