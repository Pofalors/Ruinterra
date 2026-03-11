import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class NPC_Merchant extends Entity {
    
    public String[] dialogues = new String[20];
    public int dialogueIndex = 0;
    
    public NPC_Merchant(GamePanel gp) {
        super(gp);
        
        direction = "down";
        speed = 0; // Στατικός
        solid = true;
        moving = false;
        
        getImage();
        setDialogue();
    }
    
    public void getImage() {
        try {
            down1 = ImageIO.read(new File("res/npc/merchant_down_1.png"));
            down2 = ImageIO.read(new File("res/npc/merchant_down_2.png"));
            left1 = ImageIO.read(new File("res/npc/merchant_left_1.png"));
            right1 = ImageIO.read(new File("res/npc/merchant_right_1.png"));

            
            currentImage = down1;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void setDialogue() {
        dialogues[0] = "Χαίρεται ταξιδιώτη! Θέλεις να δεις τα εμπορεύματά μου;";
        //dialogues[1] = "Αν ενδιαφέρεσαι για κάτι, μη διστάσεις να με ρωτήσεις.";
    }
    
    public void speak() {
        // Εμφάνισε το επόμενο dialogue
        gp.startDialogue(dialogues[0]);
        dialogueIndex = 0; // Κράτα το στο 0
        gp.talkingToMerchant = true; // Σημάδεψε ότι μιλάμε σε merchant
    }
    
    // Για να γυρίζει ο NPC προς τον παίκτη
    public void setDirectionTowardsPlayer() {
        if (gp.player.worldX < worldX) {
            direction = "left";
            currentImage = left1;
        } else if (gp.player.worldX > worldX) {
            direction = "right";
            currentImage = right1;
        } else if (gp.player.worldY < worldY) {
            direction = "up";
            currentImage = up1;
        } else if (gp.player.worldY > worldY) {
            direction = "down";
            currentImage = down1;
        }
    }
}