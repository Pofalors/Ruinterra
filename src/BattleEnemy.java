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
            boolean loop = animName.equals("idle");
            anim.setAnimation(animName, loop);
        }
    }

    public BufferedImage getCurrentImage() {
        if (anim != null) {
            BufferedImage img = anim.getCurrentImage();
            if (img != null) return img;
        }
        return enemy.currentImage;
    }

    public boolean isAnimationFinished() {
        return anim == null || anim.isFinished();
    }

    public boolean isOnStrikeFrame() {
        return anim != null && anim.isOnStrikeFrame();
    }

    public int getSpriteSize() {
        if (enemy != null) {
            return enemy.spriteSize;
        }
        return 48;
    }
}
