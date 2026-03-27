import java.awt.image.BufferedImage;  // ΣΩΣΤΟ
import java.io.File;
import javax.imageio.ImageIO;

public class Enemy extends Entity {
    protected int hp, maxHp, attack, defense, exp; // protected για πρόσβαση από παιδιά
    public EnemyAnimation anim;
    public String enemyType; // "mushroom", "goblin", "skeleton"
    public int spriteSize; // 48 ή 64
    public String enemyId;
    public int goldReward;
    public String battleBg = "";
    public String groundType = "";
    
    
    public Enemy(GamePanel gp) {
        super(gp);
        solid = true;
        moving = false; // Οι εχθροί δεν κινούνται στο χάρτη
        animCounter = 0;  // <-- ΜΠΟΡΕΙ ΝΑ ΛΕΙΠΕΙ
        frame = 0;        // <-- ΜΠΟΡΕΙ ΝΑ ΛΕΙΠΕΙ
    }
    
    public void update() {
        if (gp.gameState != gp.battleState) {
            // overworld animation μόνο
            animCounter++;
            if (animCounter > 10) {
                frame = (frame == 0) ? 1 : 0;
                animCounter = 0;
            }
            currentImage = (frame == 0) ? down1 : down2;
        }
    }

    // Μέθοδος για επιβράβευση όταν πεθαίνει - τώρα επιστρέφει array {exp, gold}
    public int[] giveRewards(Entity player) {
        // Τυχαίο EXP (10-50)
        int expReward = (int)(Math.random() * 40) + 10;
        // Τυχαίο gold (5-30)
        int goldReward = (int)(Math.random() * 25) + 5;
        
        // Δώσε τα rewards
        player.addExp(expReward);
        player.gold += goldReward;
        
        // Αποθήκευσε το μήνυμα για εμφάνιση
        if (gp != null) {
            gp.battleMessage = "Νίκη! +" + expReward + " EXP, +" + goldReward + " Gold!";
        }
        
        System.out.println("Πήρες " + expReward + " EXP και " + goldReward + " gold!");
        
        return new int[] {expReward, goldReward};
    }

    // Μέθοδος για φόρτωση animations
    public void loadAnimations(String type, int size) {
        this.enemyType = type;
        this.spriteSize = size;
        
        try {
            String basePath = "res/enemy/" + type + "/";
            System.out.println("Trying to load animations from: " + basePath);
            
            // Φόρτωσε τα διαφορετικά sheets
            SpriteSheet idleSheet = new SpriteSheet(basePath + "idle.png", size, size);
            SpriteSheet attackSheet = new SpriteSheet(basePath + "attack.png", size, size);
            SpriteSheet hurtSheet = new SpriteSheet(basePath + "hurt.png", size, size);
            SpriteSheet deathSheet = new SpriteSheet(basePath + "death.png", size, size);
            
            System.out.println("Sheets created successfully");
            
            BufferedImage[] idle = idleSheet.getAllFrames();      // 4 frames
            BufferedImage[] attack = attackSheet.getAllFrames();  // 8 frames
            BufferedImage[] hurt = hurtSheet.getAllFrames();      // 4 frames
            BufferedImage[] death = deathSheet.getAllFrames();    // 4 frames
            
            System.out.println("Frames loaded - idle: " + idle.length + 
                            ", attack: " + attack.length + 
                            ", hurt: " + hurt.length + 
                            ", death: " + death.length);
            
            anim = new EnemyAnimation(idle, attack, hurt, death);
            
            System.out.println("Animation object created successfully for " + type);
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("FAILED to load animations for " + type);
        }
    }
}