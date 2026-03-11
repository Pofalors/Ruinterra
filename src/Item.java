import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class Item {
    public BufferedImage image;
    public String name;
    public boolean stackable = false;
    public int amount = 1;
    public boolean isKeyItem = false;  // ΝΕΟ: για αντικείμενα που δεν καταναλώνονται
    public String ability = "";        // ΝΕΟ: π.χ. "minimap", "doubleJump", κλπ.
    
    // Για φαγητό/φίλτρα
    public int healAmount = 0;
    public int manaAmount = 0;

    // Για εμπόριο
    public int price = 0;
    
    // Για όπλα και εξοπλισμό
    public int attackBonus = 0;
    public int defenseBonus = 0;      
    public int magicBonus = 0;        
    public int hpBonus = 0;            
    public int mpBonus = 0;            
    public int speedBonus = 0;         
    
    public Item(String name) {
        this.name = name;
    }
    
    // ΝΕΑ ΜΕΘΟΔΟΣ: Φόρτωσε εικόνα για το item
    public void loadImage(String path) {
        try {
            this.image = ImageIO.read(new File(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}