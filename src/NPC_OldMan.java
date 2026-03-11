import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class NPC_OldMan extends Entity {
    // Μετρητές
    int actionCounter = 0;    // Μετρητής για την τρέχουσα ενέργεια
    int idleCounter = 0;      // Μετρητής για όταν στέκεται
    
    // Καταστάσεις NPC
    String state = "idle";    // "idle" ή "walking"
    
    // Περιοχή κίνησης
    int startX;
    int startY;
    int moveRange = 3 * gp.tileSize; // 3 tiles περιοχή
    
    public NPC_OldMan(GamePanel gp) {
        super(gp);
        
        try {
            down1 = ImageIO.read(new File("res/npc/oldman_down_1.png"));
            down2 = ImageIO.read(new File("res/npc/oldman_down_2.png"));
            up1 = ImageIO.read(new File("res/npc/oldman_up_1.png"));
            up2 = ImageIO.read(new File("res/npc/oldman_up_2.png"));
            left1 = ImageIO.read(new File("res/npc/oldman_left_1.png"));
            left2 = ImageIO.read(new File("res/npc/oldman_left_2.png"));
            right1 = ImageIO.read(new File("res/npc/oldman_right_1.png"));
            right2 = ImageIO.read(new File("res/npc/oldman_right_2.png"));
            
            currentImage = down1;
            direction = "down";
            speed = 1;
            solid = true; // Είναι στερεός (τον ακουμπάμε)
            moving = true; // Κινείται!
            
            // Αρχική θέση
            worldX = 38 * gp.tileSize;
            worldY = 10 * gp.tileSize;
            startX = worldX;
            startY = worldY;
            
            System.out.println("NPC OldMan φορτώθηκε!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void update() {
        // Αν το παιχνίδι δεν είναι σε playState, μην κάνεις κανονικό update
        if (gp.gameState != gp.playState) {
            // Αλλά συνέχισε το animation κανονικά (για να ανασαίνει)
            animCounter++;
            if (animCounter > 10) {
                frame = (frame == 0) ? 1 : 0;
                animCounter = 0;
            }
            
            // Διάλεξε εικόνα με βάση την τρέχουσα direction (που ήδη άλλαξε από το Enter)
            if (direction.equals("down")) {
                currentImage = (frame == 0) ? down1 : down2;
            } else if (direction.equals("up")) {
                currentImage = (frame == 0) ? up1 : up2;
            } else if (direction.equals("left")) {
                currentImage = (frame == 0) ? left1 : left2;
            } else if (direction.equals("right")) {
                currentImage = (frame == 0) ? right1 : right2;
            }
            
            return; // Βγες, μην κάνεις κίνηση
        }

        // ---------- ΛΟΓΙΚΗ ΣΥΜΠΕΡΙΦΟΡΑΣ ----------
        actionCounter++;
        
        if (state.equals("idle")) {
            // Στέκεται ακίνητος
            idleCounter++;
            
            // Κάθε 2 δευτερόλεπτα περίπου (60 FPS * 2 = 120 frames)
            if (idleCounter > 120) {
                // Αποφάσισε τι θα κάνει: 70% περπάτημα, 30% απλά κοιτά αλλού
                int random = (int)(Math.random() * 10);
                
                if (random < 7) { // 70% πιθανότητα να περπατήσει
                    state = "walking";
                    // Επέλεξε τυχαία κατεύθυνση
                    int dir = (int)(Math.random() * 4);
                    if (dir == 0) direction = "up";
                    else if (dir == 1) direction = "down";
                    else if (dir == 2) direction = "left";
                    else direction = "right";
                    
                    actionCounter = 0; // Ξεκίνα να μετράς πόσο περπατάει
                } else { // 30% απλά κοιτά αλλού χωρίς να περπατήσει
                    int dir = (int)(Math.random() * 4);
                    if (dir == 0) direction = "up";
                    else if (dir == 1) direction = "down";
                    else if (dir == 2) direction = "left";
                    else direction = "right";
                }
                
                idleCounter = 0;
            }
        } 
        else if (state.equals("walking")) {
            // Περπατάει για λίγο (30 frames = μισό δευτερόλεπτο περίπου)
            if (actionCounter < 30) {
                // ---------- ΚΙΝΗΣΗ ----------
                int nextWorldX = worldX;
                int nextWorldY = worldY;
                
                if (direction.equals("up")) nextWorldY = worldY - speed;
                else if (direction.equals("down")) nextWorldY = worldY + speed;
                else if (direction.equals("left")) nextWorldX = worldX - speed;
                else if (direction.equals("right")) nextWorldX = worldX + speed;
                
                // Έλεγξε όρια περιοχής
                boolean inBounds = true;
                if (nextWorldX < startX - moveRange || nextWorldX > startX + moveRange ||
                    nextWorldY < startY - moveRange || nextWorldY > startY + moveRange) {
                    inBounds = false;
                }
                
                // Έλεγξε collision με tiles
                boolean tileCollision = gp.checkCollision(this, nextWorldX, nextWorldY);

                // Έλεγξε collision με τον παίκτη (αν ο παίκτης είναι στερεός)
                boolean playerCollision = false;
                // Αν το NPC είναι στατικό, έλεγξε collision με τον παίκτη
                if (!moving) {
                    playerCollision = gp.checkEntityCollision(this, gp.player, nextWorldX, nextWorldY);
                }

                // Αν είναι μέσα στα όρια ΚΑΙ δεν έχει tile collision ΚΑΙ δεν έχει player collision
                if (inBounds && !tileCollision && !playerCollision) {
                    worldX = nextWorldX;
                    worldY = nextWorldY;
                } else {
                    // Αν δεν μπορεί να πάει, σταμάτα και ξεκίνα idle
                    state = "idle";
                    idleCounter = 80; // Για να μην ξαναρχίσει αμέσως
                }
            } else {
                // Τέλος περπατήματος, γύρνα σε idle
                state = "idle";
                idleCounter = 0;
            }
        }
        
        // ---------- ANIMATION (πάντα τρέχει) ----------
        animCounter++;
        if (animCounter > 10) {
            frame = (frame == 0) ? 1 : 0;
            animCounter = 0;
        }
        
        // Διάλεξε εικόνα (ακόμα και όταν στέκεται, κάνει animation)
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
