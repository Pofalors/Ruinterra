import java.awt.image.BufferedImage;

public class BattlePartyMember {
    public PartyMember member;
    public BufferedImage image;
    public double x, y;
    public double targetX, targetY;
    public int hp, maxHp, mp, maxMp;
    public PlayerAnimation anim;
    public String currentAnim = "idle";
    public int animFrame = 0;
    public int animTimer = 0;
    
    public BattlePartyMember(PartyMember member) {
        this.member = member;
        this.hp = member.hp;
        this.maxHp = member.maxHp;
        this.mp = member.mp;
        this.maxMp = member.maxMp;
        this.image = member.down1;
        
        // Φόρτωσε animations
        try {
            String basePath = "res/player/battle/" + member.name.toLowerCase() + "_";
            System.out.println("Loading animations for " + member.name + " from: " + basePath);
            
            SpriteSheet idleSheet = new SpriteSheet(basePath + "idle.png", 64, 64);
            SpriteSheet hurtSheet = new SpriteSheet(basePath + "hurt.png", 64, 64);
            SpriteSheet deathSheet = new SpriteSheet(basePath + "death.png", 64, 64);
            SpriteSheet attackSheet = new SpriteSheet(basePath + "attack.png", 64, 64);
            
            BufferedImage[] idleFrames = idleSheet.getAllFrames();
            BufferedImage[] hurtFrames = hurtSheet.getAllFrames();
            BufferedImage[] deathFrames = deathSheet.getAllFrames();
            BufferedImage[] attackFrames = attackSheet.getAllFrames();
            
            System.out.println("  - idle: " + idleFrames.length + " frames");
            System.out.println("  - hurt: " + hurtFrames.length + " frames");
            System.out.println("  - death: " + deathFrames.length + " frames");
            System.out.println("  - attack: " + attackFrames.length + " frames");
            
            anim = new PlayerAnimation(idleFrames, hurtFrames, deathFrames, attackFrames);
        } catch (Exception e) {
            System.out.println("No battle animations for " + member.name + ", using static images");
            e.printStackTrace();
        }
    }
    
    public void playAnimation(String animName) {
        if (anim != null) {
            // ΧΡΗΣΙΜΟΠΟΙΗΣΕ ΤΟ PlayerAnimation.playAnimation
            boolean loop = animName.equals("idle");
            anim.setAnimation(animName, loop);
            System.out.println(member.name + " playing animation: " + animName);
        } else {
            // Fallback
            currentAnim = animName;
            animFrame = 0;
            animTimer = 20; // Διάρκεια animation
        }
    }
    
    public void update() {
        if (anim != null) {
            // ΑΦΗΣΕ ΤΟ PlayerAnimation να κάνει τη δουλειά του
            anim.update();
        } else {
            // Fallback animation σύστημα
            if (animTimer > 0) {
                animTimer--;
                if (animTimer <= 0) {
                    if (currentAnim.equals("attack1") || currentAnim.equals("hurt")) {
                        currentAnim = "idle";
                        animFrame = 0;
                    }
                }
            }
        }
    }
    
    public BufferedImage getCurrentImage() {
        if (anim != null) {
            return anim.getCurrentImage();
        }
        
        // Fallback
        if (currentAnim.equals("idle")) {
            return image;
        }
        return image;
    }
}