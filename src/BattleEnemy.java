import java.awt.image.BufferedImage;

public class BattleEnemy {
    public Enemy enemy;
    public double x, y;
    public double targetX, targetY;
    public int hp;
    public int maxHp; 
    public BufferedImage image;
    public EnemyAnimation anim;

   public BattleEnemy(Enemy enemy) {
        this.enemy = enemy;
        this.hp = enemy.hp;
        this.maxHp = enemy.maxHp;
        
        // Πάρε το animation από τον enemy
        if (enemy.anim != null) {
            this.anim = enemy.anim;
        }
    }
    
    public void update() {
        if (anim != null) {
            anim.update();
        }
    }
    
    public void playAnimation(String animName) {
        if (anim != null) {
            boolean loop = animName.equals("idle"); // Μόνο το idle κάνει loop
            anim.setAnimation(animName, loop);
        }
    }
    
    public BufferedImage getCurrentImage() {
        if (anim != null) {
            BufferedImage img = anim.getCurrentImage();
            if (img == null) {
                System.out.println("WARNING: anim.getCurrentImage() returned null");
                return enemy.currentImage;
            }
            return img;
        }
        System.out.println("WARNING: anim is null, using enemy.currentImage");
        return enemy.currentImage;
    }
    
    public int getSpriteSize() {
        if (enemy != null) {
            return enemy.spriteSize;  // <-- Βεβαιώσου ότι το spriteSize έχει τιμή
        }
        return 48; // fallback
    }
}
