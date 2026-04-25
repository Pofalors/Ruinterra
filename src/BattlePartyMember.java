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
            SpriteSheet attack0Sheet = new SpriteSheet(basePath + "attack.png", 64, 64);
            SpriteSheet runLeftSheet = new SpriteSheet(basePath + "run_left.png", 64, 64);
            SpriteSheet runRightSheet = new SpriteSheet(basePath + "run_right.png", 64, 64);

            BufferedImage[] idleFrames = idleSheet.getAllFrames();
            BufferedImage[] hurtFrames = hurtSheet.getAllFrames();
            BufferedImage[] deathFrames = deathSheet.getAllFrames();
            BufferedImage[] attack0Frames = attack0Sheet.getAllFrames();
            BufferedImage[] runLeftFrames = runLeftSheet.getAllFrames();
            BufferedImage[] runRightFrames = runRightSheet.getAllFrames();
            BufferedImage[] attack1Frames = null;
            BufferedImage[] attack2Frames = null;
            BufferedImage[] attack3Frames = null;

            try {
                SpriteSheet attack1Sheet = new SpriteSheet(basePath + "attack2.png", 64, 64);
                attack1Frames = attack1Sheet.getAllFrames();
            } catch (Exception e) {
                System.out.println(basePath + "attack2.png not found, using fallback");
                attack1Frames = attack0Frames;
            }

            try {
                SpriteSheet attack2Sheet = new SpriteSheet(basePath + "attack3.png", 64, 64);
                attack2Frames = attack2Sheet.getAllFrames();
            } catch (Exception e) {
                System.out.println(basePath + "attack3.png not found, using fallback");
                attack2Frames = attack1Frames != null ? attack1Frames : attack0Frames;
            }

            try {
                SpriteSheet attack3Sheet = new SpriteSheet(basePath + "attack4.png", 64, 64);
                attack3Frames = attack3Sheet.getAllFrames();
            } catch (Exception e) {
                System.out.println(basePath + "attack4.png not found, using fallback");
                attack3Frames = attack2Frames != null ? attack2Frames : 
                            (attack1Frames != null ? attack1Frames : attack0Frames);
            }


            anim = new PlayerAnimation(idleFrames, hurtFrames, deathFrames, attack0Frames);
            anim.attack1 = attack0Frames;
            anim.attack2 = attack1Frames;
            anim.attack3 = attack2Frames;
            anim.attack4 = attack3Frames;
            anim.runLeft = runLeftFrames;
            anim.runRight = runRightFrames;

        } catch (Exception e) {
            System.out.println("No battle animations for " + member.name + ", using static images");
            e.printStackTrace();
        }
    }

    public void playAnimation(String animName) {
        if (anim != null) {
            boolean loop =
                    animName.equals("idle") ||
                    animName.equals("lowHpIdle") ||
                    animName.equals("run_left") ||
                    animName.equals("run_right");

            anim.setAnimation(animName, loop);
        } else {
            currentAnim = animName;
            animFrame = 0;
            animTimer = 20;
        }
    }

    public int getCurrentFrameIndex() {
        if (anim != null) {
            return anim.getCurrentFrameIndex();
        }
        return 0;
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