import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class NPC_Guard extends Entity {
    
    public NPC_Guard(GamePanel gp) {
        super(gp);
        
        try {
            // Φόρτωσε τις εικόνες (χρησιμοποίησε τις δικές σου ή placeholder)
            down1 = ImageIO.read(new File("res/npc/merchant_down_1.png"));
            down2 = ImageIO.read(new File("res/npc/merchant_down_2.png"));
            up1 = ImageIO.read(new File("res/npc/merchant_down_1.png")); // Προσωρινά ίδια
            up2 = ImageIO.read(new File("res/npc/merchant_down_2.png")); // Προσωρινά ίδια
            left1 = ImageIO.read(new File("res/npc/merchant_down_1.png")); // Προσωρινά ίδια
            left2 = ImageIO.read(new File("res/npc/merchant_down_2.png")); // Προσωρινά ίδια
            right1 = ImageIO.read(new File("res/npc/merchant_down_1.png")); // Προσωρινά ίδια
            right2 = ImageIO.read(new File("res/npc/merchant_down_2.png")); // Προσωρινά ίδια
            
            currentImage = down1;
            direction = "down";
            speed = 0; // Στατικός!
            solid = true; // Δεν περνάμε μέσα του
            moving = false; // Δεν κινείται
            
            // Θα τον τοποθετήσουμε αργότερα στον constructor του GamePanel
            System.out.println("NPC Guard φορτώθηκε!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void update() {
        // Αν το παιχνίδι δεν είναι σε playState, μην κάνεις update
        if (gp.gameState != gp.playState) {
            // Αλλά συνέχισε το animation
            animCounter++;
            if (animCounter > 10) {
                frame = (frame == 0) ? 1 : 0;
                animCounter = 0;
            }
            
            if (direction.equals("down")) {
                currentImage = (frame == 0) ? down1 : down2;
            } else if (direction.equals("up")) {
                currentImage = (frame == 0) ? up1 : up2;
            } else if (direction.equals("left")) {
                currentImage = (frame == 0) ? left1 : left2;
            } else if (direction.equals("right")) {
                currentImage = (frame == 0) ? right1 : right2;
            }
            return;
        }

        // ---------- ANIMATION (πάντα τρέχει) ----------
        animCounter++;
        if (animCounter > 10) {
            frame = (frame == 0) ? 1 : 0;
            animCounter = 0;
        }
        
        if (direction.equals("down")) {
            currentImage = (frame == 0) ? down1 : down2;
        } else if (direction.equals("up")) {
            currentImage = (frame == 0) ? up1 : up2;
        } else if (direction.equals("left")) {
            currentImage = (frame == 0) ? left1 : left2;
        } else if (direction.equals("right")) {
            currentImage = (frame == 0) ? right1 : right2;
        }
    }
}
