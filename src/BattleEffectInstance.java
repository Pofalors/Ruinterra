import java.awt.*;
import java.awt.image.BufferedImage;

public class BattleEffectInstance {
    public String type;
    public int x, y;
    public int timer = 0;
    public int duration = 12;
    public int level = 1;
    public boolean finished = false;

    // future use για sprite sheets
    public BufferedImage[] frames = null;
    public boolean useSpriteFrames = false;

    public BattleEffectInstance(String type, int x, int y, int duration, int level) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.duration = duration;
        this.level = level;
    }

    public void update() {
        timer++;
        if (timer >= duration) {
            finished = true;
        }
    }

    public void draw(Graphics2D g2, GamePanel gp) {
        if (finished) return;

        float progress = (float) timer / Math.max(1, duration);
        float inv = 1.0f - progress;

        switch (type) {
            case "boost_burst":
                drawBoostBurstPlaceholder(g2, progress, inv);
                break;

            case "hit_slash":
                drawHitSlashPlaceholder(g2, progress, inv);
                break;

            case "hit_flash":
                drawHitFlashPlaceholder(g2, gp, progress, inv);
                break;

            default:
                break;
        }
    }

    private void drawBoostBurstPlaceholder(Graphics2D g, float progress, float inv) {
        int impactX = x - 15;
        int impactY = y + 120;

        Color flashColor = new Color(255, 180, 100, Math.max(0, (int)(200 * inv)));

        int flashRadius;
        if (level == 1) {
            flashRadius = 24 + (int)(progress * 30);
        } else if (level == 2) {
            flashRadius = 34 + (int)(progress * 40);
        } else {
            flashRadius = 46 + (int)(progress * 52);
        }

        g.setColor(flashColor);
        g.fillOval(impactX - flashRadius, impactY - flashRadius, flashRadius * 2, flashRadius * 2);

        g.setStroke(new BasicStroke(3f));
        int ringRadius;
        if (level == 1) {
            ringRadius = 28 + (int)(progress * 38);
        } else if (level == 2) {
            ringRadius = 40 + (int)(progress * 50);
        } else {
            ringRadius = 54 + (int)(progress * 64);
        }

        g.setColor(new Color(255, 210, 150, Math.max(0, (int)(170 * inv))));
        g.drawOval(impactX - ringRadius, impactY - ringRadius, ringRadius * 2, ringRadius * 2);

        if (level >= 2) {
            int ringRadius2 = (level == 2)
                    ? 18 + (int)(progress * 32)
                    : 28 + (int)(progress * 44);

            g.setColor(new Color(255, 245, 220, Math.max(0, (int)(120 * inv))));
            g.drawOval(impactX - ringRadius2, impactY - ringRadius2, ringRadius2 * 2, ringRadius2 * 2);
        }

        int sparkCount = 6 + level * 3;
        for (int i = 0; i < sparkCount; i++) {
            double angle = (Math.PI * 2 / sparkCount) * i + (timer * 0.15);
            int dist = 10 + (int)(progress * 50) + (i % 3) * 4;

            int sx = impactX + (int)(Math.cos(angle) * dist);
            int sy = impactY + (int)(Math.sin(angle) * dist);

            int size = (level == 3) ? 5 : 4;
            g.setColor(new Color(255, 255, 255, Math.max(0, (int)(180 * inv))));
            g.fillOval(sx, sy, size, size);
        }
    }

    private void drawHitSlashPlaceholder(Graphics2D g, float progress, float inv) {
        int impactX = x - 15;
        int impactY = y + 120;

        Color mainSlash = new Color(255, 245, 220, Math.max(0, (int)(210 * inv)));
        Color glowSlash = new Color(255, 190, 120, Math.max(0, (int)(130 * inv)));

        int mainLength;
        float mainThickness;

        if (level == 1) {
            mainLength = 55;
            mainThickness = 3f;
        } else if (level == 2) {
            mainLength = 75;
            mainThickness = 5f;
        } else {
            mainLength = 95;
            mainThickness = 7f;
        }

        double angle = -0.65;

        int dx = (int)(Math.cos(angle) * mainLength);
        int dy = (int)(Math.sin(angle) * mainLength);

        int x1 = impactX - dx / 2;
        int y1 = impactY - dy / 2;
        int x2 = impactX + dx / 2;
        int y2 = impactY + dy / 2;

        g.setStroke(new BasicStroke(mainThickness + 4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(glowSlash);
        g.drawLine(x1, y1, x2, y2);

        g.setStroke(new BasicStroke(mainThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(mainSlash);
        g.drawLine(x1, y1, x2, y2);

        int streakCount = 1 + level;
        for (int i = 0; i < streakCount; i++) {
            double offsetFactor = (i - streakCount / 2.0) * 12.0;
            double perpAngle = angle + Math.PI / 2.0;

            int ox = (int)(Math.cos(perpAngle) * offsetFactor);
            int oy = (int)(Math.sin(perpAngle) * offsetFactor);

            int streakLength = (int)(mainLength * (0.45 + i * 0.08));
            int sdx = (int)(Math.cos(angle) * streakLength);
            int sdy = (int)(Math.sin(angle) * streakLength);

            int sx1 = impactX + ox - sdx / 2;
            int sy1 = impactY + oy - sdy / 2;
            int sx2 = impactX + ox + sdx / 2;
            int sy2 = impactY + oy + sdy / 2;

            g.setStroke(new BasicStroke(Math.max(1.5f, mainThickness - 2f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 255, 255, Math.max(0, (int)(120 * inv))));
            g.drawLine(sx1, sy1, sx2, sy2);
        }
    }

    private void drawHitFlashPlaceholder(Graphics2D g, GamePanel gp, float progress, float inv) {
        int alpha;
        if (level == 1) {
            alpha = (int)(70 * inv);
        } else if (level == 2) {
            alpha = (int)(110 * inv);
        } else {
            alpha = (int)(150 * inv);
        }

        alpha = Math.max(0, Math.min(255, alpha));
        g.setColor(new Color(255, 255, 255, alpha));
        g.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
    }
}
