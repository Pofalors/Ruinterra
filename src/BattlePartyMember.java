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

        try {
            String basePath = "res/player/battle/" + member.name.toLowerCase() + "_";

            SpriteSheet idleSheet = new SpriteSheet(basePath + "idle.png", 64, 64);
            SpriteSheet hurtSheet = new SpriteSheet(basePath + "hurt.png", 64, 64);
            SpriteSheet deathSheet = new SpriteSheet(basePath + "death.png", 64, 64);
            SpriteSheet attackSheet = new SpriteSheet(basePath + "attack.png", 64, 64);

            BufferedImage[] idleFrames = idleSheet.getAllFrames();
            BufferedImage[] hurtFrames = hurtSheet.getAllFrames();
            BufferedImage[] deathFrames = deathSheet.getAllFrames();
            BufferedImage[] attackFrames = attackSheet.getAllFrames();

            anim = new PlayerAnimation(idleFrames, hurtFrames, deathFrames, attackFrames);

        } catch (Exception e) {
            System.out.println("No battle animations for " + member.name + ", using static images");
            e.printStackTrace();
        }
    }

    public void playAnimation(String animName) {
        if (anim != null) {
            boolean loop = animName.equals("idle") || animName.equals("lowHpIdle");
            anim.setAnimation(animName, loop);
        } else {
            currentAnim = animName;
            animFrame = 0;
            animTimer = 20;
        }
    }

    public void update() {
        if (anim != null) {
            anim.update();
        } else {
            if (animTimer > 0) {
                animTimer--;
                if (animTimer <= 0) {
                    if (currentAnim.equals("attack1") || currentAnim.equals("attack2") ||
                        currentAnim.equals("attack3") || currentAnim.equals("hurt")) {
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
        return image;
    }

    public boolean isAnimationFinished() {
        return anim == null || anim.isFinished();
    }

    public boolean isOnStrikeFrame() {
        return anim != null && anim.isOnStrikeFrame();
    }
}