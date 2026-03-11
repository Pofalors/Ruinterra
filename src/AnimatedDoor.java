import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import javax.imageio.ImageIO;

public class AnimatedDoor {
    public ArrayList<BufferedImage> openFrames = new ArrayList<>();
    public ArrayList<BufferedImage> closeFrames = new ArrayList<>();
    public int currentFrame = 0;
    public int frameCounter = 0;
    public final int FRAME_DELAY = 5; // Πόσα frames μέχρι επόμενο καρέ
    public boolean isOpen = false;
    public boolean isAnimating = false;
    public String animationType = ""; // "OPEN" ή "CLOSE"
    
    // Το decoration που θα αλλάζει εικόνα (ολόκληρο το σπίτι)
    public Decoration targetDecoration;
    
    public void setTargetDecoration(Decoration dec) {
        this.targetDecoration = dec;
    }
    
    public void loadFrames(String basePath, String houseName, int frameCount) {
        try {
            openFrames.clear();
            closeFrames.clear();
            
            // Προσπάθησε να φορτώσεις frames για το σπίτι
            // Τα αρχεία είναι π.χ. house_open_1.png, house_open_2.png, house_close_1.png, κλπ.
            for (int i = 0; i < frameCount; i++) {
                // Φόρτωση frames ανοίγματος
                String openPath = basePath + houseName + "_open_" + (i+1) + ".png";
                File openFile = new File(openPath);
                if (openFile.exists()) {
                    BufferedImage openFrame = ImageIO.read(openFile);
                    openFrames.add(openFrame);
                }
                
                // Φόρτωση frames κλεισίματος
                String closePath = basePath + houseName + "_close_" + (i+1) + ".png";
                File closeFile = new File(closePath);
                if (closeFile.exists()) {
                    BufferedImage closeFrame = ImageIO.read(closeFile);
                    closeFrames.add(closeFrame);
                }
            }
            
            System.out.println("Loaded " + openFrames.size() + " open frames and " + 
                              closeFrames.size() + " close frames for house " + houseName);
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error loading house frames for " + houseName);
        }
    }
    
    public void startOpening() {
        if (openFrames.isEmpty()) return;
        
        isAnimating = true;
        animationType = "OPEN";
        currentFrame = 0;
        frameCounter = 0;

        System.out.println("STARTING DOOR ANIMATION - " + new java.util.Date());
        
        // Αμέσως άλλαξε την εικόνα στο πρώτο frame
        if (targetDecoration != null && !openFrames.isEmpty()) {
            targetDecoration.image = openFrames.get(0);
        }
    }
    
    public void startClosing() {
        if (closeFrames.isEmpty()) return;
        
        isAnimating = true;
        animationType = "CLOSE";
        currentFrame = 0;
        frameCounter = 0;
        
        // Αμέσως άλλαξε την εικόνα στο πρώτο frame
        if (targetDecoration != null && !closeFrames.isEmpty()) {
            targetDecoration.image = closeFrames.get(0);
        }
    }
    
    public void update() {
        if (!isAnimating || targetDecoration == null) return;
        
        frameCounter++;
        if (frameCounter >= FRAME_DELAY) {
            frameCounter = 0;
            currentFrame++;

            System.out.println("Animation frame: " + currentFrame);
            
            ArrayList<BufferedImage> currentFrames = animationType.equals("OPEN") ? openFrames : closeFrames;
            
            if (currentFrame < currentFrames.size()) {
                // Ενημέρωσε την εικόνα του σπιτιού
                targetDecoration.image = currentFrames.get(currentFrame);
            } else {
                // Τέλος animation
                isAnimating = false;
                if (animationType.equals("OPEN")) {
                    isOpen = true;
                    // Κράτα την τελευταία εικόνα (ανοιχτή πόρτα)
                    if (!openFrames.isEmpty()) {
                        targetDecoration.image = openFrames.get(openFrames.size() - 1);
                    }
                } else {
                    isOpen = false;
                    // Κράτα την τελευταία εικόνα (κλειστή πόρτα)
                    if (!closeFrames.isEmpty()) {
                        targetDecoration.image = closeFrames.get(closeFrames.size() - 1);
                    }
                }
            }
        }
    }
    
    public boolean isAnimationFinished() {
        ArrayList<BufferedImage> currentFrames = animationType.equals("OPEN") ? openFrames : closeFrames;
        return !isAnimating && currentFrame >= currentFrames.size();
    }
    
    public int getAnimationDuration() {
        int totalFrames = Math.max(openFrames.size(), closeFrames.size());
        return totalFrames * FRAME_DELAY * 16; // 16ms per frame ~ 60fps
    }
}