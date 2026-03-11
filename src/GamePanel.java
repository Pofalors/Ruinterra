import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Color;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.io.File;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.BasicStroke;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioInputStream;
import java.net.URL;
import java.awt.Toolkit;
import java.awt.RadialGradientPaint;
import java.awt.AlphaComposite;              
import java.awt.GraphicsEnvironment;  
import java.awt.GraphicsDevice;  
import java.awt.Rectangle;
import java.awt.RenderingHints;

public class GamePanel extends JPanel implements Runnable {
    // Ρυθμίσεις οθόνης
    final int originalTileSize = 16; // 16x16 pixels το κάθε πλακάκι (στο πρωτότυπο)
    final int scale = 3; // Για να τα βλέπουμε, τα μεγαλώνουμε 3 φορές
    // Για fullscreen
    private JFrame window;
    private GraphicsDevice device;
    public double scaleX = 1.0;
    public double scaleY = 1.0;
    public boolean isFullScreen = false;

    final int tileSize = originalTileSize * scale; // 48x48 pixels
    final int maxScreenCol = 16; // 16 στήλες από πλακάκια στην οθόνη
    final int maxScreenRow = 12; // 12 σειρές

    final int screenWidth = tileSize * maxScreenCol; // 768 pixels
    final int screenHeight = tileSize * maxScreenRow; // 576 pixels

    // ΝΕΟ: Ρυθμίσεις κόσμου
    public int maxWorldCol;  // 50 στήλες (πολύ μεγαλύτερος κόσμος)
    public int maxWorldRow;  // 50 σειρές
    int worldWidth;
    int worldHeight;

    // Πολλαπλοί χάρτες
    public int currentMap = 0;
    public final int maxMaps = 6;

    // Θέσεις spawn για κάθε χάρτη
    public int[][] spawnPoints = {
        {23 * tileSize, 21 * tileSize}, // Overworld spawn
        {10 * tileSize, 41 * tileSize}, // Dungeon spawn
        {12 * tileSize, 12 * tileSize}, // Merchant spawn
        {4 * tileSize, 22 * tileSize},  // Main town
        {12 * tileSize, 12 * tileSize}, // Big house interior
        {12 * tileSize, 12 * tileSize}   // Small house interior  // Main town
    };

    // SPAWN POINT (ξεκινάμε στη μέση του κόσμου)
    public int worldX = 23 * tileSize;  // Ξεκίνα στη μέση περίπου
    public int worldY = 21 * tileSize;

    // Game loop
    Thread gameThread;

    // tile manager
    TileManager tileM; // Διαχειριστής χάρτη

    // NPC
    public NPC_OldMan oldMan; // Ο NPC μας
    public Entity player; // Ο ήρωας ως Entity
    public ArrayList<ArrayList<Entity>> npcs = new ArrayList<>();

    // Inventory
    public Inventory inventory = new Inventory();
    public boolean showInventory = false; // Να εμφανίζεται το inventory
    public final int inventoryState = 2; // Νέο game state
    //public ArrayList<ItemOnGround> itemsOnGround = new ArrayList<>();
    public ArrayList<ArrayList<ItemOnGround>> itemsOnGround = new ArrayList<>();
    public String itemTooltip = "";
    public int tooltipTimer = 0;

    // ========== ΗΧΟΣ ==========
    public Sound sound = new Sound();

    // ========== ΔΙΑΛΟΓΟΣ ΜΕ "ΓΡΑΨΙΜΟ" ==========
    public String fullDialogue = "";        // Το πλήρες κείμενο
    public String displayedDialogue = "";   // Όσο έχει εμφανιστεί μέχρι τώρα
    public int dialogueCharCounter = 0;     // Μετρητής χαρακτήρων
    public int dialogueSpeed = 1;           // Πόσοι χαρακτήρες ανά frame
    public int dialogueFrameCounter = 0;     // μετρητής frames
    public boolean dialogueTyping = false;  // Αν γράφεται ακόμα

    // === CUSTOM FONTS ===
    public Font maruMonica;
    public Font maruMonicaBold;
    public Font maruMonicaSmall;
    public Font maruMonicaLarge;

    // Σύστημα μέρας/νύχτας
    public int dayTime = 0; // 0=μέρα, 1=βράδυ, 2=νύχτα
    public int timeCounter = 0;
    public final int dayDuration = 9000; // Πόσα frames διαρκεί η μέρα (2.5 λεπτά στα 60fps)
    public final int transitionDuration = 3000; // ΜΙΣΟ λεπτό για τη μετάβαση (αντί για eveningDuration)
    public final int nightDuration = 9000; // 2.5 λεπτά
    // Μουσική μέρας/νύχτας
    public String currentDayMusic = "overworld_day";   // Μουσική μέρας
    public String currentNightMusic = "overworld_night"; // Μουσική νύχτας
    public float targetMusicVolume = 0.3f;  // Στόχος έντασης (0-1)
    public float currentMusicVolume = 0.3f; // Τρέχουσα ένταση
    public boolean isCrossfading = false;   // Αν γίνεται crossfade
    public String nextMusic = "";           // Επόμενη μουσική για crossfade
    public final float CROSSFADE_SPEED = 0.01f;
    // ΜΕΡΑ/ΝΥΧΤΑ
    public float targetDarkness = 0.0f; // Στόχος σκοταδιού
    public float currentDarkness = 0.0f; // Τρέχον σκοτάδι (για smooth transition)
    public float darknessAlpha = 0.0f; // Διαφάνεια σκοταδιού (0-255)
    public boolean hasLantern = false; // Αν ο παίκτης έχει φανάρι
    public Item lantern; // Το αντικείμενο φανάρι
    public int lanternRadius = 5; // Ακτίνα φωτισμού σε tiles

    // MERCHANT
    public ArrayList<Item> merchantItems = new ArrayList<>();
    public int shopOption = 0; // 0=BUY, 1=SELL, 2=EXIT
    public int shopMode = 0; // 0=BUY mode, 1=SELL mode
    public int selectedShopItem = 0;
    public int selectedSellItem = 0;
    public boolean talkingToMerchant = false;

    // DECORATIONS
    public ArrayList<ArrayList<Decoration>> decorations = new ArrayList<>();
    public ArrayList<House> houses = new ArrayList<>();

    // Portals
    public ArrayList<Portal> portals = new ArrayList<>();

    // Game states
    public int gameState = 0;
    public final int playState = 0;
    public final int dialogueState = 1;
    public final int dialogueOptionsState = 3;
    public final int mapState = 5;
    public final int titleState = 6;  
    public final int controlsState = 7;
    public final int pauseState = 8;
    public final int quitConfirmState = 9;
    public final int pauseControlsState = 10;
    public final int shopState = 11;

    // επιλογές διαλόγου
    public String[] dialogueOptions = new String[3];
    public int selectedOption = 0;
    // Διάλογος
    public String currentDialogue = "";

    // ========== ΜΑΧΗ ==========
    public ArrayList<ArrayList<Enemy>> enemies = new ArrayList<>();
    public final int battleState = 4;
    public Enemy currentEnemy;
    public int battleOption = 0; // 0=Attack, 1=Magic, 2=Item, 3=Run
    public String[] battleOptions = {"Attack", "Magic", "Item", "Run"};
    // Random Encounters
    public int encounterStepCounter = 0;
    public int encounterRate = 20; // Κάθε 20 βήματα
    public String currentArea = "overworld"; // "overworld" ή "dungeon"
    // Transition Battle
    public boolean battleStarting = false;
    public int battleFadeAlpha = 0; // 0 = διάφανο, 255 = μαύρο
    public boolean battleFadeIn = false; // Αν γίνεται fade in
    public boolean battleFadeOut = false; // Αν γίνεται fade out
    public final int FADE_SPEED = 3; // Ταχύτητα fade
    public int battleWalkTimer = 0; // Μετρητής για animation περπατήματος
    public int battleWalkFrame = 0; // Frame animation
    public int battleTransitionTimer = 0;
    public final int BATTLE_TRANSITION_TIME = 60; // 1 δευτερόλεπτο στα 60fps
    public boolean battleEntering = false;
    public BufferedImage battleBackground;
    public ArrayList<BattleEnemy> battleEnemies = new ArrayList<>();
    public ArrayList<BattlePlayer> battlePlayers = new ArrayList<>();
    public int groundY = 0; // Το ύψος του εδάφους
    public int groundX = 0; // Η θέση Χ του εδάφους (για κλίση)
    public double groundAngle = 0.17; // 10 μοίρες σε radians (10 * π/180)
    public int groundWidth = screenWidth; // Πλάτος εδάφους
    public int groundHeight = tileSize * 2; // Ύψος εδάφους
    public BufferedImage groundImage; // Η εικόνα του εδάφους

    // ΠΟΖΕΣ
    BufferedImage playerDown1, playerDown2;
    BufferedImage playerUp1, playerUp2;
    BufferedImage playerLeft1, playerLeft2;
    BufferedImage playerRight1, playerRight2;

    BufferedImage currentPlayerImage; // Η τωρινή εικόνα που θα εμφανίζεται

    // TITLE SCREEN
    public int titleCommandNum = 0; // 0=START, 1=SETTINGS, 2=CONTROLS
    public BufferedImage titleLogo; // Το logo του παιχνιδιού
    // SETTINGS
    public int pauseCommandNum = 0; // 0=FULLSCREEN, 1=MUSIC, 2=SOUND, 3=CONTROLS, 4=QUIT
    public String[] pauseOptions = {"FULLSCREEN", "MUSIC VOLUME", "SOUND VOLUME", "CONTROLS", "QUIT GAME"};

    // Για volume control
    public int musicVolume = 30; // 0-100
    public int soundVolume = 70; // 0-100
    public int volumeSettingMode = 0; // 0=κανένα, 1=music, 2=sound
    public int quitConfirmOption = 0; // 0=NO, 1=YES

    int direction = 2; // 0=κάτω, 1=αριστερά, 2=δεξιά, 3=πάνω
    int frame = 0; // 0 ή 1 (δύο καρέ)
    int counter = 0; // Μετρητής για animation

    // Movement
    KeyHandler keyH = new KeyHandler();
    int playerSpeed = 4;

    // Ορίζουμε το μέγεθος του παραθύρου
    public GamePanel(JFrame window) {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true); // Για καλύτερο ζωγράφισμα

        this.addKeyListener(keyH);
        this.setFocusable(true);

        this.window = window;
        this.device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        tileM = new TileManager(this); // Δώσε του access στο GamePanel  
        
        for (int i = 0; i < maxMaps; i++) {
            npcs.add(new ArrayList<>());
            itemsOnGround.add(new ArrayList<>());
        }

        // Δημιούργησε τον player ως Entity
        player = new Entity(this);
        player.worldX = 23 * tileSize;  // ή όποιο spawn point θες
        player.worldY = 21 * tileSize;
        player.speed = 4;
        player.direction = "down";

        // ========== ΝΕΟ: Δημιουργία NPC OldMan και προσθήκη στον χάρτη 0 ==========
        NPC_OldMan oldMan = new NPC_OldMan(this);
        oldMan.worldX = 38 * tileSize;
        oldMan.worldY = 10 * tileSize;
        npcs.get(0).add(oldMan);

        // ========== ΝΕΟ: Δημιουργία NPC Guard και προσθήκη στον χάρτη 0 ==========
        NPC_Guard guard = new NPC_Guard(this);
        guard.worldX = 21 * tileSize; // Συντεταγμένες που θες
        guard.worldY = 19 * tileSize;
        npcs.get(0).add(guard); // Στον ίδιο χάρτη με τον oldMan

        // ========== Δημιουργία NPC Merchant και προσθήκη στον χάρτη 2 ==========

        NPC_Merchant merchant = new NPC_Merchant(this);
        merchant.worldX = 12 * tileSize; // Συντεταγμένες που θες
        merchant.worldY = 7 * tileSize;
        npcs.get(2).add(merchant);

        // ========== Δημιουργία λίστας εχθρών ==========
        for (int i = 0; i < maxMaps; i++) {
            enemies.add(new ArrayList<>());
        }
        // ========== Δημιουργία DECORATIONS ==========
        for (int i = 0; i < maxMaps; i++) {
            decorations.add(new ArrayList<>());
        }

        // Φόρτωση της εικόνας του ήρωα
        try {
            // Φόρτωση όλων των εικόνων
            playerDown1 = ImageIO.read(new File("res/player/Walking sprites/boy_down_1.png"));
            playerDown2 = ImageIO.read(new File("res/player/Walking sprites/boy_down_2.png"));
            playerUp1 = ImageIO.read(new File("res/player/Walking sprites/boy_up_1.png"));
            playerUp2 = ImageIO.read(new File("res/player/Walking sprites/boy_up_2.png"));
            playerLeft1 = ImageIO.read(new File("res/player/Walking sprites/boy_left_1.png"));
            playerLeft2 = ImageIO.read(new File("res/player/Walking sprites/boy_left_2.png"));
            playerRight1 = ImageIO.read(new File("res/player/Walking sprites/boy_right_1.png"));
            playerRight2 = ImageIO.read(new File("res/player/Walking sprites/boy_right_2.png"));
            
            // Ξεκίνα με την πρώτη εικόνα (κάτω)
            currentPlayerImage = playerDown1;
            
            System.out.println("Όλες οι εικόνες φορτώθηκαν επιτυχώς!");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            titleLogo = ImageIO.read(new File("res/title/logo.png")); // Δημιούργησε αυτό το μονοπάτι
            System.out.println("Logo loaded successfully!");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Using text fallback for logo");
        }

        // === FONTS ===
        try {
            // Φόρτωσε το κανονικό font
            maruMonica = Font.createFont(Font.TRUETYPE_FONT, new File("res/font/MaruMonica.ttf")).deriveFont(20f);
            maruMonicaBold = maruMonica.deriveFont(Font.BOLD, 24f);
            maruMonicaSmall = maruMonica.deriveFont(16f);
            maruMonicaLarge = maruMonica.deriveFont(Font.BOLD, 40f);
            
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(maruMonica);
            
            System.out.println("Custom font loaded successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback σε Arial
            maruMonica = new Font("Arial", Font.PLAIN, 20);
            maruMonicaBold = new Font("Arial", Font.BOLD, 24);
            maruMonicaSmall = new Font("Arial", Font.PLAIN, 16);
            maruMonicaLarge = new Font("Arial", Font.BOLD, 40);
        }

        // ========== ΔΗΜΙΟΥΡΓΙΑ ΦΑΝΑΡΙΟΥ ==========
        try {
            lantern = new Item("Lantern");
            lantern.isKeyItem = true;
            lantern.loadImage("res/items/lantern.png");
            inventory.addItem(lantern); // Αυτόματα θα πάει στα key items
            
            System.out.println("Lantern created successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Για δοκιμή, μπορείς να το βάλεις και στο έδαφος:
        try {
            BufferedImage lanternImage = lantern.image;
            itemsOnGround.get(0).add(new ItemOnGround("Lantern", lanternImage, 
                                                15 * tileSize, 10 * tileSize, lantern));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ========== ΠΡΟΦΟΡΤΩΣΗ ΗΧΩΝ ==========
        // Μουσικές
        sound.preloadMusic("overworld_day", "overworld_day.wav");
        sound.preloadMusic("overworld_night", "overworld_night.wav");
        sound.preloadMusic("town_day", "town_day.wav");
        sound.preloadMusic("town_night", "town_night.wav");
        sound.preloadMusic("dungeon", "dungeon_music.wav");
        sound.preloadMusic("battle", "battle_music.wav");
        sound.preloadMusic("merchant_village", "Merchant.wav");
        
        // Ηχητικά εφέ
        sound.preloadSound("type", "type.wav");
        sound.preloadSound("dialogue_start", "dialogue_start.wav");
        sound.preloadSound("enemy_hit", "enemy_hit.wav");
        sound.preloadSound("hitmonster", "hitmonster.wav");
        sound.preloadSound("player_hit", "player_hit.wav");
        sound.preloadSound("level_up", "levelup.wav");
        sound.preloadSound("death", "death.wav");
        sound.preloadSound("run_away", "run_away.wav");
        sound.preloadSound("portal", "portal.wav");
        sound.preloadSound("stairs", "stairs.wav");
        sound.preloadSound("menu_open", "menu_open.wav");
        sound.preloadSound("menu_close", "menu_close.wav");
        sound.preloadSound("menu_select", "menu_select.wav");
        sound.preloadSound("equip", "equip.wav");
        sound.preloadSound("unequip", "unequip.wav");
        sound.preloadSound("enemy_appear", "enemy_appear.wav");
        sound.preloadSound("coin", "coin.wav");
        
        // Ξεκίνα με την μουσική ΤΙΤΛΟΥ
        sound.preloadMusic("title", "title_music.wav");
        sound.playMusic("title");

        // MERCHANT ITEMS FOR SALE
        try {
            Item healthPotion = new Item("Health Potion");
            healthPotion.stackable = true;
            healthPotion.healAmount = 20;
            healthPotion.price = 50;
            healthPotion.loadImage("res/items/health_potion.png");
            merchantItems.add(healthPotion);
            
            Item manaPotion = new Item("Mana Potion");
            manaPotion.stackable = true;
            manaPotion.mpBonus = 20;
            manaPotion.price = 40;
            manaPotion.loadImage("res/items/potion_blue.png");
            merchantItems.add(manaPotion);
            
            Item ironSword = new Item("Iron Sword");
            ironSword.attackBonus = 5;
            ironSword.price = 200;
            ironSword.loadImage("res/items/iron_sword.png");
            merchantItems.add(ironSword);
            
            Item leatherArmor = new Item("Leather Armor");
            leatherArmor.defenseBonus = 3;
            leatherArmor.price = 150;
            leatherArmor.loadImage("res/items/leather_armor.png");
            merchantItems.add(leatherArmor);
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Προσθήκη δειγματικών decorations (στον overworld - χάρτης 0)
        loadHouses();
        try {
            // Statue - θέλουμε 2 tiles ύψος (96 pixels)
            BufferedImage originalStatue = ImageIO.read(new File("res/decorations/statue.png"));
            BufferedImage scaledStatue = scaleDecoration(originalStatue, tileSize * 2); // 96px
            
            decorations.get(3).add(new Decoration("Statue", scaledStatue, 
                                                9 * tileSize, 19 * tileSize, 
                                                scaledStatue.getWidth(), scaledStatue.getHeight(), true));
            
            // Stand - 1.5 tiles ύψος (72 pixels)
            BufferedImage originalStand = ImageIO.read(new File("res/decorations/stand.png"));
            BufferedImage scaledStand = scaleDecoration(originalStand, (int)(tileSize * 1.5));
            
            decorations.get(3).add(new Decoration("Stand", scaledStand, 
                                                18 * tileSize, 19 * tileSize,
                                                scaledStand.getWidth(), scaledStand.getHeight(), true));

            // house - 1.5 tiles ύψος (72 pixels)
            //BufferedImage originalHouse = ImageIO.read(new File("res/decorations/house_1.png"));
            //BufferedImage scaledHouse = scaleDecoration(originalHouse, (int)(tileSize * 6));
            
            //decorations.get(3).add(new Decoration("House", scaledHouse, 
                                                //10 * tileSize, 9 * tileSize,
                                                //scaledHouse.getWidth(), scaledHouse.getHeight(), true));
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ========== Items για Overworld (χάρτης 0) ==========
        try {
            Item healthPotion = new Item("Health Potion");
            healthPotion.stackable = true;
            healthPotion.healAmount = 20;
            healthPotion.price = 50;
            healthPotion.loadImage("res/items/health_potion.png");
            
            BufferedImage potionImage = healthPotion.image;
            itemsOnGround.get(0).add(new ItemOnGround("Health Potion", potionImage, 
                                                25 * tileSize, 21 * tileSize, healthPotion));
            
            Item ironSword = new Item("Iron Sword");
            ironSword.attackBonus = 5;
            ironSword.price = 200;
            ironSword.loadImage("res/items/iron_sword.png");
            
            BufferedImage swordImage = ironSword.image;
            itemsOnGround.get(0).add(new ItemOnGround("Iron Sword", swordImage, 
                                                21 * tileSize, 21 * tileSize, ironSword));

            System.out.println("Items στο έδαφος (Overworld) προστέθηκαν!");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ========== ΝΕΟ: Items για Dungeon (χάρτης 1) ==========
        try {
            Item magicPotion = new Item("Magic Potion");
            magicPotion.stackable = true;
            magicPotion.healAmount = 30;
            magicPotion.loadImage("res/items/potion_blue.png");
            
            BufferedImage potionImage = magicPotion.image;
            itemsOnGround.get(1).add(new ItemOnGround("Magic Potion", potionImage, 
                                                18 * tileSize, 38 * tileSize, magicPotion));
            
            System.out.println("Items στο έδαφος (Dungeon) προστέθηκαν!");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ========== ΝΕΟ: Goblin Ears για το quest ==========
        try {
            Item goblinEar = new Item("Goblin Ear");
            goblinEar.stackable = true;
            goblinEar.amount = 1; // Θα βάλουμε μερικά για δοκιμή            
            goblinEar.loadImage("res/items/goblin_ear.png");
            
            BufferedImage earImage = goblinEar.image;
            
            // Βάλε 5 αυτιά σε διάφορα σημεία (ή ένα με amount=5)
            itemsOnGround.get(0).add(new ItemOnGround("Goblin Ear", earImage, 
                                                18 * tileSize, 40 * tileSize, goblinEar));
            itemsOnGround.get(0).add(new ItemOnGround("Goblin Ear", earImage, 
                                                19 * tileSize, 40 * tileSize, goblinEar));
            itemsOnGround.get(0).add(new ItemOnGround("Goblin Ear", earImage, 
                                                20 * tileSize, 40 * tileSize, goblinEar));
            itemsOnGround.get(0).add(new ItemOnGround("Goblin Ear", earImage, 
                                                21 * tileSize, 40 * tileSize, goblinEar));
            itemsOnGround.get(0).add(new ItemOnGround("Goblin Ear", earImage, 
                                                22 * tileSize, 40 * tileSize, goblinEar));
            
            System.out.println("Goblin Ears προστέθηκαν!");
        } catch (Exception e) {
            e.printStackTrace();
        }

        Item mapItem = new Item("World Map");
        mapItem.isKeyItem = true;
        mapItem.loadImage("res/items/map.png");
        inventory.addItem(mapItem);  // Αυτόματα θα πάει στα key items

        // Δημιούργησε portals
        portals.add(new Portal(0, 12 * tileSize, 8 * tileSize, 1, 10 * tileSize, 41 * tileSize)); // Overworld -> Dungeon
        portals.add(new Portal(1, 9 * tileSize, 41 * tileSize, 0, 11 * tileSize, 9 * tileSize)); // Dungeon -> Overworld
        portals.add(new Portal(0, 10 * tileSize, 39 * tileSize, 2, 12 * tileSize, 12 * tileSize)); // Overworld -> Merchant House
        portals.add(new Portal(2, 12 * tileSize, 13 * tileSize, 0, 10 * tileSize, 40 * tileSize)); // Merchant House -> Overworld
        portals.add(new Portal(0, 41 * tileSize, 7 * tileSize, 3, 4 * tileSize, 22 * tileSize)); // Overworld -> Town
        portals.add(new Portal(3, 3 * tileSize, 22 * tileSize, 0, 41 * tileSize, 8 * tileSize)); // Town -> Overworld

        //ΞΕΚΙΝΑΩ ΜΕ TITLE
        gameState = titleState;
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000 / 60;
        double nextDrawTime = System.nanoTime() + drawInterval;
        
        while (gameThread != null) {
            // ========== ΝΕΟ: Λήψη λιστών για τον τρέχοντα χάρτη ==========
            ArrayList<Entity> currentMapNPCs = npcs.get(currentMap);
            ArrayList<ItemOnGround> currentMapItems = itemsOnGround.get(currentMap);

            // Έλεγξε αν ο παίκτης πατάει Enter μπροστά από NPC
            if (keyH.enterPressed) {
                if (gameState == playState) {
                    // Πρώτα έλεγξε για NPCs (όπως πριν)
                    boolean enteredHouse = false;
        
                    // Υπολόγισε μπροστά από τον παίκτη
                    int checkX = player.worldX;
                    int checkY = player.worldY;
                    
                    switch(direction) {
                        case 0: checkY += tileSize; break; // down
                        case 1: checkX -= tileSize; break; // left
                        case 2: checkX += tileSize; break; // right
                        case 3: checkY -= tileSize; break; // up
                    }
                    
                    // ========== ΠΡΩΤΑ ΕΛΕΓΞΕ ΓΙΑ EXIT ΑΠΟ ΤΟ ΣΠΙΤΙ ==========
                    // Αν είμαστε μέσα σε σπίτι (χάρτης 4 ή 5)
                    if (currentMap == 4 || currentMap == 5) {
                        // Βρες το σπίτι στο οποίο βρισκόμαστε
                        for (House house : houses) {
                            if (house.interiorMap == currentMap) {
                                // Έλεγξε αν ο παίκτης είναι μπροστά από την πόρτα εξόδου
                                // Η έξοδος είναι στο σημείο που μπήκαμε (spawn point)
                                int doorX = house.spawnX;
                                int doorY = house.spawnY;
                                
                                int distanceX = Math.abs(player.worldX - doorX);
                                int distanceY = Math.abs(player.worldY - doorY);
                                
                                if (distanceX < tileSize && distanceY < tileSize) {
                                    System.out.println("Πάτησες Enter για έξοδο από το σπίτι!");
                                    
                                    if (house.hasAnimatedDoor) {
                                        // Ξεκίνα το animation κλεισίματος
                                        house.exitHouse();
                                        playSound("door_close");
                                        
                                        // Δημιούργησε νέο thread για να περιμένει να τελειώσει το animation
                                        Thread animationThread = new Thread(() -> {
                                            try {
                                                System.out.println("Exit thread started, waiting " + house.door.getAnimationDuration() + "ms");
                                                Thread.sleep(house.door.getAnimationDuration());
                                                System.out.println("Exit thread finished waiting, teleporting outside...");
                                                
                                                javax.swing.SwingUtilities.invokeLater(() -> {
                                                    currentMap = house.exteriorMap;
                                                    player.worldX = house.exitX;
                                                    player.worldY = house.exitY;
                                                    System.out.println("Teleported outside to map " + currentMap + " at (" + player.worldX/tileSize + "," + player.worldY/tileSize + ")");
                                                    
                                                    // Επανάφερε την εξωτερική μουσική
                                                    if (currentMap == 3) { // town
                                                        if (dayTime == 0 || dayTime == 3) {
                                                            sound.playMusic("town_day");
                                                        } else {
                                                            sound.playMusic("town_night");
                                                        }
                                                    }
                                                });
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        });
                                        animationThread.start();
                                    } else {
                                        // Αν δεν έχει animation, απλή έξοδος
                                        currentMap = house.exteriorMap;
                                        player.worldX = house.exitX;
                                        player.worldY = house.exitY;
                                        playSound("door_close");
                                        
                                        // Επανάφερε την εξωτερική μουσική
                                        if (currentMap == 3) { // town
                                            if (dayTime == 0 || dayTime == 3) {
                                                sound.playMusic("town_day");
                                            } else {
                                                sound.playMusic("town_night");
                                            }
                                        }
                                    }
                                    
                                    enteredHouse = true;
                                    keyH.enterPressed = false;
                                    break;
                                }
                            }
                        }
                    }
                    
                    // ΜΟΝΟ ΑΝ ΔΕΝ ΕΙΜΑΣΤΕ ΣΕ EXIT, ΕΛΕΓΞΕ ΓΙΑ ΕΙΣΟΔΟ ΣΕ ΣΠΙΤΙ
                    if (!enteredHouse) {
                        // Έλεγξε όλα τα houses
                        for (House house : houses) {
                            // Μόνο αν είμαστε στον σωστό εξωτερικό χάρτη
                            if (currentMap == house.exteriorMap) {
                                // Έλεγξε αν το μπροστινό tile είναι στην περιοχή της πόρτας
                                if (house.doorArea.contains(checkX, checkY)) {
                                    System.out.println("Πάτησες Enter μπροστά από την πόρτα!");
                                    
                                    if (house.hasAnimatedDoor) {
                                        // Ξεκίνα το animation
                                        System.out.println("Ξεκινάω animation πόρτας...");
                                        house.enterHouse();
                                        playSound("door_open");
                                        sound.playMusic("merchant_village");
                                        currentMusicVolume = musicVolume / 100.0f;
                                        sound.setMusicVolume(currentMusicVolume);
                                        
                                        // Δημιούργησε νέο thread για να περιμένει να τελειώσει το animation
                                        Thread animationThread = new Thread(() -> {
                                            try {
                                                System.out.println("Thread started, waiting " + house.door.getAnimationDuration() + "ms");
                                                Thread.sleep(house.door.getAnimationDuration());
                                                System.out.println("Thread finished waiting, teleporting...");
                                                
                                                javax.swing.SwingUtilities.invokeLater(() -> {
                                                    currentMap = house.interiorMap;
                                                    player.worldX = house.spawnX;
                                                    player.worldY = house.spawnY;
                                                    System.out.println("Teleported to map " + currentMap + " at (" + player.worldX/tileSize + "," + player.worldY/tileSize + ")");
                                                });
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        });
                                        animationThread.start();
                                    } else {
                                        // Αν δεν έχει animation, απλή είσοδος
                                        currentMap = house.interiorMap;
                                        player.worldX = house.spawnX;
                                        player.worldY = house.spawnY;
                                        playSound("door_open");
                                        sound.playMusic("merchant_village");
                                        currentMusicVolume = musicVolume / 100.0f;
                                        sound.setMusicVolume(currentMusicVolume);
                                        System.out.println("Μπήκες στο σπίτι (χωρίς animation)");
                                    }
                                    
                                    enteredHouse = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (!enteredHouse) {
                        for (Entity npc : currentMapNPCs) {
                            int distanceX = Math.abs(player.worldX - npc.worldX);
                            int distanceY = Math.abs(player.worldY - npc.worldY);
                            
                            if (distanceX <= tileSize && distanceY <= tileSize) {
                                // ========== ΝΕΟ: Ανάλογα με τον τύπο NPC, διαφορετικός διάλογος ==========
                                if (npc instanceof NPC_OldMan) {
                                    // Διάλογος για τον γέρο
                                    gameState = dialogueState;
                                    startDialogue("Γεια σου, ταξιδιώτη!...\nΚαλώς ήρθες στο πρώτο μου RPG!");
                                    
                                    // Γύρνα τον NPC να κοιτάει τον παίκτη
                                    if (player.worldX < npc.worldX) {
                                        npc.direction = "left";
                                        ((NPC_OldMan) npc).currentImage = ((NPC_OldMan) npc).left1;
                                    }
                                    else if (player.worldX > npc.worldX) {
                                        npc.direction = "right";
                                        ((NPC_OldMan) npc).currentImage = ((NPC_OldMan) npc).right1;
                                    }
                                    else if (player.worldY < npc.worldY) {
                                        npc.direction = "up";
                                        ((NPC_OldMan) npc).currentImage = ((NPC_OldMan) npc).up1;
                                    }
                                    else if (player.worldY > npc.worldY) {
                                        npc.direction = "down";
                                        ((NPC_OldMan) npc).currentImage = ((NPC_OldMan) npc).down1;
                                    }
                                }
                                else if (npc instanceof NPC_Guard) {
                                    boolean hasQuest = false;
                                    boolean questCompleted = false;
                                    
                                    for (Quest q : player.quests) {
                                        if (q.name.equals("Καθάρισμα τεράτων")) {
                                            hasQuest = true;
                                            questCompleted = q.completed;
                                            break;
                                        }
                                    }
                                    
                                    if (!hasQuest) {
                                        gameState = dialogueOptionsState;
                                        startDialogue("Φύλακας: Χρειάζομαι βοήθεια! Τα goblin έχουν γίνει μάστιγα!\nΤι λες;");
                                        dialogueOptions[0] = "(1) Αναλαμβάνω την αποστολή";
                                        dialogueOptions[1] = "(2) Δεν έχω χρόνο τώρα";
                                        dialogueOptions[2] = null;
                                        selectedOption = 0;
                                    } 
                                    else if (!questCompleted) {
                                        gameState = dialogueOptionsState;
                                        startDialogue("Φύλακας: Πώς πάει η αποστολή; Έφερες τα 5 αυτιά;");
                                        dialogueOptions[0] = "(1) Ναι, ορίστε!";
                                        dialogueOptions[1] = "(2) Όχι ακόμα...";
                                        dialogueOptions[2] = null;
                                        selectedOption = 0;
                                    } 
                                    else {
                                        gameState = dialogueState;
                                        startDialogue("Φύλακας: Σε ευχαριστώ για τη βοήθεια! Οι χωρικοί είναι ασφαλείς.");
                                    }
                                    
                                    // Γύρνα τον guard να κοιτάει τον παίκτη
                                    if (player.worldX < npc.worldX) {
                                        npc.direction = "left";
                                        ((NPC_Guard) npc).currentImage = ((NPC_Guard) npc).left1;
                                    }
                                    else if (player.worldX > npc.worldX) {
                                        npc.direction = "right";
                                        ((NPC_Guard) npc).currentImage = ((NPC_Guard) npc).right1;
                                    }
                                    else if (player.worldY < npc.worldY) {
                                        npc.direction = "up";
                                        ((NPC_Guard) npc).currentImage = ((NPC_Guard) npc).up1;
                                    }
                                    else if (player.worldY > npc.worldY) {
                                        npc.direction = "down";
                                        ((NPC_Guard) npc).currentImage = ((NPC_Guard) npc).down1;
                                    }
                                    break;
                                } else if (npc instanceof NPC_Merchant) {
                                    gameState = dialogueState;
                                    ((NPC_Merchant) npc).setDirectionTowardsPlayer();
                                    ((NPC_Merchant) npc).speak();
                                    talkingToMerchant = true;
                                    break;
                                }
                            }
                        }
                        // Αν δεν βρήκε NPC, έλεγξε για τραπέζι (tile 035)
                        if (!talkingToMerchant) {
                            // Υπολόγισε σε ποιο tile είναι ο παίκτης
                            int playerCol = player.worldX / tileSize;
                            int playerRow = player.worldY / tileSize;
                            
                            // Έλεγξε μπροστά από τον παίκτη
                            int checkCol = playerCol;
                            int checkRow = playerRow;
                            
                            switch(direction) {
                                case 0: // down
                                    checkRow++;
                                    break;
                                case 1: // left
                                    checkCol--;
                                    break;
                                case 2: // right
                                    checkCol++;
                                    break;
                                case 3: // up
                                    checkRow--;
                                    break;
                            }
                            
                            // Έλεγξε αν το tile μπροστά είναι τραπέζι (035)
                            if (checkRow >= 0 && checkRow < maxWorldRow && 
                                checkCol >= 0 && checkCol < maxWorldCol) {
                                int tileNum = tileM.mapTileNum[currentMap][checkRow][checkCol];
                                String tileName = tileM.fileNames.get(tileNum);
                                
                                if (tileName.equals("035.png")) { // Το τραπέζι
                                    // Βρες τον merchant στον χάρτη
                                    for (Entity npc : currentMapNPCs) {
                                        if (npc instanceof NPC_Merchant) {
                                            gameState = dialogueState;
                                            ((NPC_Merchant) npc).setDirectionTowardsPlayer();
                                            ((NPC_Merchant) npc).speak();
                                            talkingToMerchant = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    keyH.enterPressed = false;
                } 
                else if (gameState == dialogueState || gameState == dialogueOptionsState) {
                    if (keyH.enterPressed) {
                        if (gameState == dialogueOptionsState) {
                            if (!dialogueTyping) { // Μόνο αν έχει τελειώσει το γράψιμο
                                handleDialogueOption(selectedOption);
                            } else {
                                // Αν γράφεται ακόμα, τελείωσέ το αμέσως
                                displayedDialogue = fullDialogue;
                                dialogueTyping = false;
                                playSound("type");
                            }
                        } else {
                            if (dialogueTyping) {
                                displayedDialogue = fullDialogue;
                                dialogueTyping = false;
                                playSound("type");
                            } else {
                                if (talkingToMerchant) {
                                    gameState = shopState;
                                    talkingToMerchant = false;
                                } else {
                                    gameState = playState;
                                }
                            }
                        }
                        keyH.enterPressed = false;
                    }
                }
            }

            // ========== Μάζεμα items (χρησιμοποιεί currentMapItems) ==========
            if (keyH.ePressed) {
                for (int i = 0; i < currentMapItems.size(); i++) {
                    ItemOnGround item = currentMapItems.get(i);
                    
                    int distanceX = Math.abs(player.worldX - item.worldX);
                    int distanceY = Math.abs(player.worldY - item.worldY);
                    
                    if (distanceX < tileSize && distanceY < tileSize) {
                        if (inventory.addItem(item.item)) {
                            currentMapItems.remove(i);
                            startDialogue("Πήρες: " + item.name + "!");
                            gameState = dialogueState;
                        }
                        break;
                    }
                }
                
                try {
                    Thread.sleep(200);
                    keyH.ePressed = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (keyH.iPressed) {
                if (gameState == playState) {
                    gameState = inventoryState;
                    showInventory = true;
                    playSound("menu_open");
                } else if (gameState == inventoryState) {
                    gameState = playState;
                    showInventory = false;
                    playSound("menu_close");
                }
                
                try {
                    Thread.sleep(200);
                    keyH.iPressed = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (keyH.qPressed) {
                player.quests.clear();
                System.out.println("All quests cleared!");
                try { Thread.sleep(200); } catch (Exception e) {}
                keyH.qPressed = false;
            }

            // ========== ΠΛΟΗΓΗΣΗ ΣΤΟΝ ΔΙΑΛΟΓΟ (εκτός από το enterPressed) ==========
            if (gameState == dialogueOptionsState) {
                if (keyH.upPressed) {
                    selectedOption--;
                    if (selectedOption < 0) selectedOption = 0;
                    try { Thread.sleep(150); } catch (Exception e) {}
                    keyH.upPressed = false;
                }
                if (keyH.downPressed) {
                    selectedOption++;
                    if (selectedOption >= 3) selectedOption = 2;
                    try { Thread.sleep(150); } catch (Exception e) {}
                    keyH.downPressed = false;
                }
            }
            // ========== ΠΛΗΚΤΡΟ Μ (ΧΑΡΤΗΣ) ==========
            if (keyH.mPressed) {
                if (gameState == playState) {
                    // Έλεγξε αν έχει το χάρτη
                    boolean hasMap = false;
                    for (int i = 0; i < inventory.keyItems.length; i++) {
                        if (inventory.keyItems[i] != null && inventory.keyItems[i].name.equals("World Map")) {
                            hasMap = true;
                            break;
                        }
                    }
                    
                    if (hasMap) {
                        gameState = mapState;
                    } else {
                        startDialogue("Δεν έχεις χάρτη ακόμα...");
                        gameState = dialogueState;
                    }
                    keyH.mPressed = false;
                    try { Thread.sleep(200); } catch (Exception e) {}
                } else if (gameState == mapState) {
                    gameState = playState;
                    keyH.mPressed = false;
                    try { Thread.sleep(200); } catch (Exception e) {}
                }
            }

            // ========== SHOP STATE ==========
            if (gameState == shopState) {
                if (keyH.escapePressed) {
                    gameState = playState;
                    talkingToMerchant = false;
                    shopOption = 0; // Reset to BUY
                    selectedShopItem = 0;
                    selectedSellItem = 0;
                    keyH.escapePressed = false;
                    try { Thread.sleep(200); } catch (Exception e) {}
                }

                // Πλοήγηση μεταξύ BUY/SELL/EXIT με πάνω/κάτω
                if (keyH.upPressed) {
                    shopOption--;
                    if (shopOption < 0) shopOption = 2;
                    playSound("menu_select");
                    // Ενημέρωσε το shopMode ανάλογα
                    shopMode = (shopOption == 0) ? 0 : (shopOption == 1) ? 1 : 0;
                    repaint();
                    try { Thread.sleep(150); } catch (Exception e) {}
                    keyH.upPressed = false;
                }
                if (keyH.downPressed) {
                    shopOption++;
                    if (shopOption > 2) shopOption = 0;
                    playSound("menu_select");
                    // Ενημέρωσε το shopMode ανάλογα
                    shopMode = (shopOption == 0) ? 0 : (shopOption == 1) ? 1 : 0;
                    repaint();
                    try { Thread.sleep(150); } catch (Exception e) {}
                    keyH.downPressed = false;
                }
                
                // Μόνο αν ΔΕΝ είμαστε στο EXIT (shopOption != 2), επιτρέπεται πλοήγηση στα items
                if (shopOption != 2) {
                    if (shopOption == 0) { // BUY mode
                        if (keyH.leftPressed) {
                            selectedShopItem--;
                            if (selectedShopItem < 0) selectedShopItem = 0;
                            playSound("menu_select");
                            repaint();
                            try { Thread.sleep(150); } catch (Exception e) {}
                            keyH.leftPressed = false;
                        }
                        if (keyH.rightPressed) {
                            selectedShopItem++;
                            if (selectedShopItem >= merchantItems.size()) selectedShopItem = merchantItems.size() - 1;
                            playSound("menu_select");
                            repaint();
                            try { Thread.sleep(150); } catch (Exception e) {}
                            keyH.rightPressed = false;
                        }
                    } else if (shopOption == 1) { // SELL mode
                        // Υπολόγισε τα προς πώληση items
                        ArrayList<Item> sellableItems = new ArrayList<>();
                        for (int i = 0; i < inventory.storage.length; i++) {
                            if (inventory.storage[i] != null && !inventory.storage[i].isKeyItem) {
                                sellableItems.add(inventory.storage[i]);
                            }
                        }
                        
                        if (sellableItems.size() > 0) {
                            if (keyH.leftPressed) {
                                selectedSellItem--;
                                if (selectedSellItem < 0) selectedSellItem = 0;
                                playSound("menu_select");
                                repaint();
                                try { Thread.sleep(150); } catch (Exception e) {}
                                keyH.leftPressed = false;
                            }
                            if (keyH.rightPressed) {
                                selectedSellItem++;
                                if (selectedSellItem >= sellableItems.size()) selectedSellItem = sellableItems.size() - 1;
                                playSound("menu_select");
                                repaint();
                                try { Thread.sleep(150); } catch (Exception e) {}
                                keyH.rightPressed = false;
                            }
                        }
                    }
                }
                
                // Χειρισμός ENTER - διαφορετικός για κάθε option
                if (keyH.enterPressed) {
                    playSound("menu_select");
                    
                    if (shopOption == 0) { // BUY
                        if (selectedShopItem >= 0 && selectedShopItem < merchantItems.size()) {
                            Item selected = merchantItems.get(selectedShopItem);
                            if (player.gold >= selected.price) {
                                player.gold -= selected.price;
                                // Δημιούργησε αντίγραφο του item
                                Item boughtItem = new Item(selected.name);
                                boughtItem.stackable = selected.stackable;
                                boughtItem.healAmount = selected.healAmount;
                                boughtItem.manaAmount = selected.manaAmount;
                                boughtItem.attackBonus = selected.attackBonus;
                                boughtItem.defenseBonus = selected.defenseBonus;
                                boughtItem.magicBonus = selected.magicBonus;
                                boughtItem.hpBonus = selected.hpBonus;
                                boughtItem.mpBonus = selected.mpBonus;
                                boughtItem.speedBonus = selected.speedBonus;
                                boughtItem.price = selected.price;
                                boughtItem.isKeyItem = selected.isKeyItem;
                                boughtItem.loadImage("res/items/" + selected.name.toLowerCase().replace(" ", "_") + ".png");
                                
                                inventory.addItem(boughtItem);
                                playSound("coin");
                                startDialogue("Αγόρασες " + selected.name + "!");
                                gameState = dialogueState;
                            } else {
                                startDialogue("Δεν έχεις αρκετό χρυσό!");
                                gameState = dialogueState;
                            }
                        }
                    } else if (shopOption == 1) { // SELL
                        ArrayList<Item> sellableItems = new ArrayList<>();
                        for (int i = 0; i < inventory.storage.length; i++) {
                            if (inventory.storage[i] != null && !inventory.storage[i].isKeyItem) {
                                sellableItems.add(inventory.storage[i]);
                            }
                        }
                        
                        if (sellableItems.size() > 0 && selectedSellItem < sellableItems.size()) {
                            Item selected = sellableItems.get(selectedSellItem);
                            int sellPrice = selected.price / 2;
                            
                            // Αφαίρεσε το item από το inventory
                            for (int i = 0; i < inventory.storage.length; i++) {
                                if (inventory.storage[i] == selected) {
                                    if (selected.stackable && selected.amount > 1) {
                                        selected.amount--;
                                    } else {
                                        inventory.storage[i] = null;
                                    }
                                    break;
                                }
                            }
                            
                            player.gold += sellPrice;
                            playSound("coin");
                            startDialogue("Πούλησες " + selected.name + " για " + sellPrice + " χρυσά!");
                            gameState = dialogueState;
                            
                            // Επαναφορά επιλογής
                            selectedSellItem = 0;
                        }
                    } else if (shopOption == 2) { // EXIT
                        gameState = playState;
                        talkingToMerchant = false;
                        shopOption = 0; // Reset to BUY
                        selectedShopItem = 0;
                        selectedSellItem = 0;
                    }
                    
                    keyH.enterPressed = false;
                }
            }

            // ----- INVENTORY NAVIGATION -----
            if (gameState == inventoryState) {
                // Αλλαγή mode με Shift 
                if (keyH.shiftPressed) {
                    inventory.inventoryMode = (inventory.inventoryMode == 0) ? 1 : 0;
                    playSound("menu_select");
                    
                    // Αν πάμε σε equipment mode, επέλεξε το πρώτο slot
                    if (inventory.inventoryMode == 1) {
                        inventory.selectedEquipSlot = 0;
                    } else {
                        inventory.selectedStorageSlot = 0;
                    }
                    
                    try { Thread.sleep(200); } catch (Exception e) {}
                    keyH.shiftPressed = false;
                }               
                if (inventory.inventoryMode == 0) { // Storage mode
                    // Πλοήγηση στο 4x2 grid
                    if (keyH.upPressed) {
                        inventory.selectedStorageSlot -= 4;
                        if (inventory.selectedStorageSlot < 0) inventory.selectedStorageSlot = 0;
                        playSound("menu_select");
                        try { Thread.sleep(150); } catch (Exception e) {}
                        keyH.upPressed = false;
                    }
                    if (keyH.downPressed) {
                        inventory.selectedStorageSlot += 4;
                        if (inventory.selectedStorageSlot >= 8) inventory.selectedStorageSlot = 7;
                        playSound("menu_select");
                        try { Thread.sleep(150); } catch (Exception e) {}
                        keyH.downPressed = false;
                    }
                    if (keyH.leftPressed) {
                        inventory.selectedStorageSlot--;
                        if (inventory.selectedStorageSlot < 0) inventory.selectedStorageSlot = 0;
                        playSound("menu_select");
                        try { Thread.sleep(150); } catch (Exception e) {}
                        keyH.leftPressed = false;
                    }
                    if (keyH.rightPressed) {
                        inventory.selectedStorageSlot++;
                        if (inventory.selectedStorageSlot >= 8) inventory.selectedStorageSlot = 7;
                        playSound("menu_select");
                        try { Thread.sleep(150); } catch (Exception e) {}
                        keyH.rightPressed = false;
                    }
                    
                    // Χρήση item με Enter
                    if (keyH.enterPressed) {
                        Item selected = inventory.storage[inventory.selectedStorageSlot];
                        if (selected != null) {
                            if (selected.healAmount > 0) { // Potion
                                playSound("use_items");
                                player.heal(selected.healAmount);
                                inventory.removeFromStorage(inventory.selectedStorageSlot);
                                startDialogue("Ήπιες " + selected.name + "!");
                                gameState = dialogueState;
                            } else { // Equippable item
                                int targetSlot = -1;
                                
                                if (selected.name.contains("Sword") || selected.name.contains("Blade")) {
                                    targetSlot = 3; // SWORD
                                } else if (selected.name.contains("Shield")) {
                                    targetSlot = 5; // SHIELD
                                } else if (selected.name.contains("Helmet") || selected.name.contains("Hat")) {
                                    targetSlot = 1; // HELMET
                                } else if (selected.name.contains("Chest") || selected.name.contains("Armor")) {
                                    targetSlot = 4; // CHEST
                                } else if (selected.name.contains("Gloves") || selected.name.contains("Gauntlets")) {
                                    targetSlot = 6; // GLOVES
                                } else if (selected.name.contains("Belt")) {
                                    targetSlot = 7; // BELT
                                } else if (selected.name.contains("Boots") || selected.name.contains("Shoes")) {
                                    targetSlot = 8; // BOOTS
                                } else if (selected.name.contains("Ring")) {
                                    targetSlot = 0; // RING
                                } else if (selected.name.contains("Necklace") || selected.name.contains("Amulet")) {
                                    targetSlot = 2; // NECKLACE
                                }
                                
                                if (targetSlot != -1) {
                                    // Πριν εξοπλίσεις, αφαίρεσε τα μπόνους του παλιού item (αν υπάρχει)
                                    Item oldItem = inventory.getEquipSlot(targetSlot);
                                    player.recalcStats();
                                    if (oldItem != null) {
                                        player.attack -= oldItem.attackBonus;
                                        player.defense -= oldItem.defenseBonus;
                                        player.magicAttack -= oldItem.magicBonus;
                                        player.maxHp -= oldItem.hpBonus;
                                        player.maxMp -= oldItem.mpBonus;
                                        player.speed_stat -= oldItem.speedBonus;
                                        
                                        // Αν το maxHp μειώθηκε, προσάρμοσε και το τρέχον hp
                                        if (player.hp > player.maxHp) {
                                            player.hp = player.maxHp;
                                        }
                                        if (player.mp > player.maxMp) {
                                            player.mp = player.maxMp;
                                        }
                                    }
                                    
                                    // Εξόπλισε το νέο item
                                    playSound("equip");
                                    inventory.equipItem(inventory.selectedStorageSlot, targetSlot);
                                    
                                    // Πρόσθεσε τα μπόνους του νέου item
                                    player.attack += selected.attackBonus;
                                    player.defense += selected.defenseBonus;
                                    player.magicAttack += selected.magicBonus;
                                    player.maxHp += selected.hpBonus;
                                    player.maxMp += selected.mpBonus;
                                    player.speed_stat += selected.speedBonus;
                                    
                                    // Αύξησε hp/mp αν χρειάζεται
                                    player.hp += selected.hpBonus;
                                    player.mp += selected.mpBonus;
                                
                                } else {
                                    inventory.inventoryMode = 1;
                                    inventory.selectedEquipSlot = 0;
                                }
                            }
                        }
                        try { Thread.sleep(200); } catch (Exception e) {}
                        keyH.enterPressed = false;
                    }
                }
                else { // Equipment mode
                    // Πλοήγηση στο 3x3 grid
                    if (keyH.upPressed) {
                        inventory.selectedEquipSlot -= 3;
                        if (inventory.selectedEquipSlot < 0) inventory.selectedEquipSlot = 0;
                        playSound("menu_select");
                        try { Thread.sleep(150); } catch (Exception e) {}
                        keyH.upPressed = false;
                    }
                    if (keyH.downPressed) {
                        inventory.selectedEquipSlot += 3;
                        if (inventory.selectedEquipSlot >= 9) inventory.selectedEquipSlot = 8;
                        playSound("menu_select");
                        try { Thread.sleep(150); } catch (Exception e) {}
                        keyH.downPressed = false;
                    }
                    if (keyH.leftPressed) {
                        inventory.selectedEquipSlot--;
                        if (inventory.selectedEquipSlot < 0) inventory.selectedEquipSlot = 0;
                        playSound("menu_select");
                        try { Thread.sleep(150); } catch (Exception e) {}
                        keyH.leftPressed = false;
                    }
                    if (keyH.rightPressed) {
                        inventory.selectedEquipSlot++;
                        if (inventory.selectedEquipSlot >= 9) inventory.selectedEquipSlot = 8;
                        playSound("menu_select");
                        try { Thread.sleep(150); } catch (Exception e) {}
                        keyH.rightPressed = false;
                    }
                    
                    if (keyH.enterPressed) {
                        Item selected = inventory.getEquipSlot(inventory.selectedEquipSlot);
                        if (selected != null) {
                            playSound("unequip");
                            // Αφαίρεσε τα μπόνους
                            player.attack -= selected.attackBonus;
                            player.defense -= selected.defenseBonus;
                            player.magicAttack -= selected.magicBonus;
                            player.maxHp -= selected.hpBonus;
                            player.maxMp -= selected.mpBonus;
                            player.speed_stat -= selected.speedBonus;
                            
                            if (player.hp > player.maxHp) player.hp = player.maxHp;
                            if (player.mp > player.maxMp) player.mp = player.maxMp;
                            
                            inventory.unequipItem(inventory.selectedEquipSlot);
                            player.recalcStats();
                        }
                        try { Thread.sleep(200); } catch (Exception e) {}
                        keyH.enterPressed = false;
                    }
                }
            }
            // ========== BATTLE STATE ==========
            else if (gameState == battleState) {
                if (battleEntering) {
                    battleTransitionTimer++;
                    battleWalkTimer++;
                    
                    // Animation περπατήματος (εναλλαγή καρέ κάθε 10 frames)
                    if (battleWalkTimer > 10) {
                        battleWalkTimer = 0;
                        battleWalkFrame = (battleWalkFrame == 0) ? 1 : 0;
                    }
                    
                    // Κίνηση εχθρών προς τα μέσα
                    for (BattleEnemy be : battleEnemies) {
                        be.x += (be.targetX - be.x) / 10;
                        if (Math.abs(be.x - be.targetX) < 1) be.x = be.targetX;
                    }
                    
                    // Κίνηση παικτών προς τα μέσα ΜΕ ANIMATION
                    for (BattlePlayer bp : battlePlayers) {
                        bp.x -= (bp.x - bp.targetX) / 10;
                        if (Math.abs(bp.x - bp.targetX) < 1) bp.x = bp.targetX;
                        
                        // ΑΛΛΑΞΕ ΕΙΚΟΝΑ ΓΙΑ ANIMATION
                        if (battleWalkFrame == 0) {
                            bp.image = playerLeft1;
                        } else {
                            bp.image = playerLeft2;
                        }
                    }
                    
                    // Όταν τελειώσει το transition
                    if (battleTransitionTimer >= BATTLE_TRANSITION_TIME) {
                        battleEntering = false;
                        // Βεβαιώσου ότι είναι στις σωστές θέσεις
                        for (BattleEnemy be : battleEnemies) be.x = be.targetX;
                        for (BattlePlayer bp : battlePlayers) {
                            bp.x = bp.targetX;
                            bp.image = playerLeft1; // Στατική εικόνα όταν σταματήσει
                        }
                    }
                    
                    repaint();
                } else {
                        // Κανονική λογική μάχης
                        if (keyH.leftPressed) {
                            battleOption--;
                            if (battleOption < 0) battleOption = 3;
                            playSound("menu_select");
                            try { Thread.sleep(150); } catch (Exception e) {}
                            keyH.leftPressed = false;
                            repaint();
                        }
                        if (keyH.rightPressed) {
                            battleOption++;
                            if (battleOption > 3) battleOption = 0;
                            playSound("menu_select");
                            try { Thread.sleep(150); } catch (Exception e) {}
                            keyH.rightPressed = false;
                            repaint();
                        }
                    
                    if (keyH.enterPressed) {
                        playSound("menu_select");

                        if (battleOption == 0) { // Attack
                            playSound("hitmonster");
                            int damage = player.attack - currentEnemy.defense;
                            if (damage < 1) damage = 1;
                            currentEnemy.hp -= damage;

                            // ΜΕΙΩΣΕ ΤΟ HP ΣΤΟ BATTLEENEMY
                            for (BattleEnemy be : battleEnemies) {
                                if (be.enemy == currentEnemy) {
                                    be.hp -= damage;
                                    break;
                                }
                            }
                            
                            currentDialogue = "Έκανες " + damage + " ζημιά!";
                            repaint();
                            try { Thread.sleep(1000); } catch (Exception e) {}
                            
                            if (currentEnemy.hp <= 0) {
                                sound.stopMusic();
                                playSound("levelup");
                                currentEnemy.giveRewards(player);
                                currentDialogue = "Νίκη! Πήρες " + currentEnemy.exp + " EXP!";
                                repaint();
                                try { Thread.sleep(1500); } catch (Exception e) {}
                                gameState = playState;
                                // ========== ΕΠΙΣΤΡΟΦΗ ΣΤΗ ΜΟΥΣΙΚΗ ΤΟΥ ΧΑΡΤΗ ==========
                                if (currentMap == 0) { // Overworld
                                    if (dayTime == 0 || dayTime == 3) { // Μέρα ή ξημέρωμα
                                        sound.playMusic("overworld_day");
                                    } else { // Νύχτα ή βράδυ
                                        sound.playMusic("overworld_night");
                                    }
                                } else { // Dungeon
                                    sound.playMusic("dungeon");
                                }
                                // Βεβαιώσου ότι η ένταση είναι σωστή
                                currentMusicVolume = musicVolume / 100.0f;
                                sound.setMusicVolume(currentMusicVolume);
                                continue;
                            }
                            
                            playSound("enemy_hit");
                            currentDialogue = "Ο εχθρός επιτίθεται!";
                            repaint();
                            try { Thread.sleep(1000); } catch (Exception e) {}
                            
                            int enemyDamage = currentEnemy.attack - player.defense;
                            if (enemyDamage < 1) enemyDamage = 1;
                            player.hp -= enemyDamage;

                            // ΕΝΗΜΕΡΩΣΕ ΤΟ HP ΣΤΟ BATTLEPLAYER
                            for (BattlePlayer bp : battlePlayers) {
                                if (bp.player == player) {
                                    bp.hp = player.hp; // Συγχρόνισε το hp
                                    System.out.println("Player HP updated: " + bp.hp + "/" + bp.maxHp);
                                    break;
                                }
                            }
                            
                            playSound("player_hit");
                            currentDialogue = "Ο εχθρός σου προκάλεσε " + enemyDamage + " ζημιά!";
                            repaint();
                            try { Thread.sleep(1000); } catch (Exception e) {}
                            
                            if (player.hp <= 0) {
                                sound.stopMusic();
                                playSound("death");
                                currentDialogue = "Ηττήθηκες...";
                                repaint();
                                try { Thread.sleep(1500); } catch (Exception e) {}
                                player.worldX = spawnPoints[0][0];
                                player.worldY = spawnPoints[0][1];
                                player.hp = player.maxHp;
                                gameState = playState;

                                // ========== ΕΠΙΣΤΡΟΦΗ ΣΤΗ ΜΟΥΣΙΚΗ ΤΟΥ ΧΑΡΤΗ ==========
                                if (currentMap == 0) { // Overworld
                                    if (dayTime == 0 || dayTime == 3) { // Μέρα ή ξημέρωμα
                                        sound.playMusic("overworld_day");
                                    } else { // Νύχτα ή βράδυ
                                        sound.playMusic("overworld_night");
                                    }
                                } else { // Dungeon
                                    sound.playMusic("dungeon");
                                }
                                // Βεβαιώσου ότι η ένταση είναι σωστή
                                currentMusicVolume = musicVolume / 100.0f;
                                sound.setMusicVolume(currentMusicVolume);
                                continue;
                            }
                            
                        } else if (battleOption == 3) { // Run
                            if (Math.random() < 0.5) {
                                playSound("run_away");
                                currentDialogue = "Απέδρασες!";
                                repaint();
                                try { Thread.sleep(1000); } catch (Exception e) {}
                                gameState = playState;

                                // ========== ΕΠΙΣΤΡΟΦΗ ΣΤΗ ΜΟΥΣΙΚΗ ΤΟΥ ΧΑΡΤΗ ==========
                                if (currentMap == 0) { // Overworld
                                    if (dayTime == 0 || dayTime == 3) { // Μέρα ή ξημέρωμα
                                        sound.playMusic("overworld_day");
                                    } else { // Νύχτα ή βράδυ
                                        sound.playMusic("overworld_night");
                                    }
                                } else { // Dungeon
                                    sound.playMusic("dungeon");
                                }
                                currentMusicVolume = musicVolume / 100.0f;
                                sound.setMusicVolume(currentMusicVolume);
                            } else {
                                playSound("enemy_hit");
                                currentDialogue = "Απέτυχες να αποδράσεις!";
                                repaint();
                                try { Thread.sleep(1000); } catch (Exception e) {}
                            }
                        }
                        keyH.enterPressed = false;
                    }
                }    
            }
            // ========== TITLE STATE ==========
            if (gameState == titleState) {
                if (keyH.upPressed) {
                    titleCommandNum--;
                    if (titleCommandNum < 0) {
                        titleCommandNum = 2;
                    }
                    playSound("menu_select");
                    try { Thread.sleep(150); } catch (Exception e) {}
                    keyH.upPressed = false;
                }
                if (keyH.downPressed) {
                    titleCommandNum++;
                    if (titleCommandNum > 2) {
                        titleCommandNum = 0;
                    }
                    playSound("menu_select");
                    try { Thread.sleep(150); } catch (Exception e) {}
                    keyH.downPressed = false;
                }
                if (keyH.enterPressed) {
                    playSound("menu_select");
                    if (titleCommandNum == 0) { // START GAME
                        gameState = playState;
                        sound.stopMusic();
                        // Ξεκίνα με τη σωστή μουσική ανάλογα με την ώρα
                        if (dayTime == 0 || dayTime == 3) {
                            sound.playMusic("overworld_day");
                        } else {
                            sound.playMusic("overworld_night");
                        }
                        currentMusicVolume = musicVolume / 100.0f;
                        sound.setMusicVolume(currentMusicVolume);
                    } else if (titleCommandNum == 1) { // SETTINGS
                        startDialogue("Settings will be available in a future update!");
                        gameState = dialogueState;
                    } else if (titleCommandNum == 2) { // CONTROLS
                        gameState = controlsState;
                    }
                    keyH.enterPressed = false;
                    try { Thread.sleep(200); } catch (Exception e) {}
                }
            }

            // ΜΕΣΑ στο σύστημα μέρας/νύχτας
            if (gameState == playState) {
                timeCounter++;
                
                // Ενημέρωσε την ώρα και το targetDarkness
                if (dayTime == 0) { // Μέρα
                    targetDarkness = 0.0f;
                    if (timeCounter > dayDuration) {
                        dayTime = 1; // Πήγαινε σε μετάβαση προς νύχτα
                        timeCounter = 0;
                        
                        // Ξεκίνα crossfade προς νυχτερινή μουσική
                        if (currentMap == 0) { // Μόνο για overworld
                            startMusicCrossfade("overworld_night");
                        } else if (currentMap == 3) { // Μόνο για overworld
                            startMusicCrossfade("town_night");
                        }
                    }
                } else if (dayTime == 1) { // Μετάβαση από μέρα σε νύχτα
                    targetDarkness = 150.0f;
                    if (timeCounter > transitionDuration) {
                        dayTime = 2; // Πήγαινε σε νύχτα
                        timeCounter = 0;
                    }
                } else if (dayTime == 2) { // Νύχτα
                    targetDarkness = 150.0f;
                    if (timeCounter > nightDuration) {
                        dayTime = 3; // Πήγαινε σε μετάβαση προς μέρα
                        timeCounter = 0;
                        
                        // Ξεκίνα crossfade προς μεσημεριανή μουσική
                        if (currentMap == 0) { // Μόνο για overworld
                            startMusicCrossfade("overworld_day");
                        } else if (currentMap == 3) { // Μόνο για overworld
                            startMusicCrossfade("town_day");
                        }
                    }
                } else if (dayTime == 3) { // Μετάβαση από νύχτα σε μέρα
                    targetDarkness = 0.0f;
                    if (timeCounter > transitionDuration) {
                        dayTime = 0; // Πήγαινε σε μέρα
                        timeCounter = 0;
                    }
                }
                
                // Smooth transition - σταδιακή προσαρμογή του currentDarkness προς το targetDarkness
                float speed = 0.5f;
                if (currentDarkness < targetDarkness) {
                    currentDarkness = Math.min(targetDarkness, currentDarkness + speed);
                } else if (currentDarkness > targetDarkness) {
                    currentDarkness = Math.max(targetDarkness, currentDarkness - speed);
                }
                
                // Crossfade μουσικής - σταδιακή αλλαγή έντασης
                if (isCrossfading) {
                    // Μείωσε σταδιακά την τρέχουσα μουσική
                    currentMusicVolume = Math.max(0, currentMusicVolume - CROSSFADE_SPEED);
                    sound.setMusicVolume(currentMusicVolume);
                    
                    // Όταν φτάσει στο 0, άλλαξε μουσική και ξεκίνα να την ανεβάζεις
                    if (currentMusicVolume <= 0) {
                        sound.playMusic(nextMusic);
                        // Συνέχισε με την επιθυμητή ένταση (από το musicVolume)
                        targetMusicVolume = musicVolume / 100.0f;
                        isCrossfading = false;
                    }
                } else if (sound.getCurrentMusic().equals(nextMusic) && !isCrossfading) {
                    // Σταδιακή αύξηση της νέας μουσικής μέχρι το target
                    if (currentMusicVolume < targetMusicVolume) {
                        currentMusicVolume = Math.min(targetMusicVolume, currentMusicVolume + CROSSFADE_SPEED);
                        sound.setMusicVolume(currentMusicVolume);
                    }
                }
                
                // Έλεγξε αν ο παίκτης έχει φανάρι
                hasLantern = false;
                for (int i = 0; i < inventory.keyItems.length; i++) {
                    if (inventory.keyItems[i] != null && inventory.keyItems[i].name.equals("Lantern")) {
                        hasLantern = true;
                        break;
                    }
                }
            }

            // ========== CONTROLS STATE ==========
            if (gameState == controlsState) {
                if (keyH.enterPressed || keyH.mPressed || keyH.iPressed) {
                    gameState = titleState;
                    keyH.enterPressed = false;
                    keyH.mPressed = false;
                    keyH.iPressed = false;
                    try { Thread.sleep(200); } catch (Exception e) {}
                }
            }

            // ========== ESCAPE BUTTON ==========
            if (keyH.escapePressed) {
                if (gameState == playState) {
                    gameState = pauseState; // Πήγαινε στο pause menu
                    pauseCommandNum = 0;
                    volumeSettingMode = 0;
                    playSound("menu_open");
                } else if (gameState == pauseState) {
                    gameState = playState; // Γύρνα πίσω στο παιχνίδι
                    playSound("menu_close");
                } else if (gameState == quitConfirmState) {
                    gameState = pauseState; // Γύρνα πίσω στο pause menu
                    playSound("menu_select");
                }
                keyH.escapePressed = false;
                try { Thread.sleep(200); } catch (Exception e) {}
            }

            // ========== PAUSE STATE ==========
            if (gameState == pauseState) {
                if (volumeSettingMode == 0) { // Κανονική πλοήγηση μενού
                    if (keyH.upPressed) {
                        pauseCommandNum--;
                        if (pauseCommandNum < 0) pauseCommandNum = pauseOptions.length - 1;
                        playSound("menu_select");
                        try { Thread.sleep(150); } catch (Exception e) {}
                        keyH.upPressed = false;
                    }
                    if (keyH.downPressed) {
                        pauseCommandNum++;
                        if (pauseCommandNum >= pauseOptions.length) pauseCommandNum = 0;
                        playSound("menu_select");
                        try { Thread.sleep(150); } catch (Exception e) {}
                        keyH.downPressed = false;
                    }
                    if (keyH.enterPressed) {
                        playSound("menu_select");
                        
                        if (pauseCommandNum == 0) { // FULLSCREEN
                            toggleFullScreen();
                        } else if (pauseCommandNum == 1) { // MUSIC VOLUME
                            volumeSettingMode = 1;
                        } else if (pauseCommandNum == 2) { // SOUND VOLUME
                            volumeSettingMode = 2;
                        } else if (pauseCommandNum == 3) { // CONTROLS
                            gameState = pauseControlsState;;
                        } else if (pauseCommandNum == 4) { // QUIT GAME
                            gameState = quitConfirmState;
                            quitConfirmOption = 0; // Default: NO
                        }
                        keyH.enterPressed = false;
                    }
                } else { // Ρύθμιση volume
                    if (keyH.leftPressed) {
                        if (volumeSettingMode == 1) { // Music
                            musicVolume = Math.max(0, musicVolume - 10);
                            currentMusicVolume = musicVolume / 100.0f;
                            sound.setMusicVolume(musicVolume); // Θα το φτιάξουμε
                        } else { // Sound
                            soundVolume = Math.max(0, soundVolume - 10);
                        }
                        playSound("menu_select");
                        try { Thread.sleep(100); } catch (Exception e) {}
                        keyH.leftPressed = false;
                    }
                    if (keyH.rightPressed) {
                        if (volumeSettingMode == 1) { // Music
                            musicVolume = Math.min(100, musicVolume + 10);
                            currentMusicVolume = musicVolume / 100.0f;
                            sound.setMusicVolume(musicVolume);
                        } else { // Sound
                            soundVolume = Math.min(100, soundVolume + 10);
                        }
                        playSound("menu_select");
                        try { Thread.sleep(100); } catch (Exception e) {}
                        keyH.rightPressed = false;
                    }
                    if (keyH.enterPressed || keyH.escapePressed) {
                        volumeSettingMode = 0; // Γύρνα πίσω στο μενού
                        playSound("menu_select");
                        keyH.enterPressed = false;
                        keyH.escapePressed = false;
                    }
                }
            }
            // ========== PAUSE CONTROLS STATE ==========
            if (gameState == pauseControlsState) {
                if (keyH.enterPressed || keyH.escapePressed) {
                    gameState = pauseState; // Γύρνα πίσω στο pause menu
                    keyH.enterPressed = false;
                    keyH.escapePressed = false;
                    try { Thread.sleep(200); } catch (Exception e) {}
                }
            }

            // ========== QUIT CONFIRM STATE ==========
            if (gameState == quitConfirmState) {
                if (keyH.leftPressed || keyH.rightPressed) {
                    quitConfirmOption = (quitConfirmOption == 0) ? 1 : 0;
                    playSound("menu_select");
                    try { Thread.sleep(150); } catch (Exception e) {}
                    keyH.leftPressed = false;
                    keyH.rightPressed = false;
                }
                if (keyH.enterPressed) {
                    playSound("menu_select");
                    if (quitConfirmOption == 0) { // YES
                        System.exit(0); // Κλείσε το παιχνίδι
                    } else { // NO
                        gameState = pauseState; // Γύρνα πίσω
                    }
                    keyH.enterPressed = false;
                }
                if (keyH.escapePressed) {
                    gameState = pauseState; // Γύρνα πίσω
                    keyH.escapePressed = false;
                }
            }
            
            // ========== Ενημέρωση όλων των NPCs του τρέχοντος χάρτη ==========
            if (gameState == playState) {
                for (Entity npc : currentMapNPCs) {
                    if (npc instanceof NPC_OldMan) {
                        ((NPC_OldMan) npc).update();
                    }
                }
            }

            // ========== Κίνηση ήρωα (εκτός από μικρή αλλαγή στα portals) ==========
            // 1. ΕΝΗΜΕΡΩΣΗ: Κουνάμε τον ήρωα και αλλάζουμε κατεύθυνση
            boolean moving = false;

            if (gameState == playState && !battleStarting) {
                for (House house : houses) {
                    house.updateDoor();
                }
                // Δοκιμάζουμε πρώτα την οριζόντια κίνηση (X)
                if (keyH.leftPressed) {
                    direction = 1; // Αριστερά
                    
                    // Δοκίμασε να κουνηθείς αριστερά
                    int nextWorldX = player.worldX - playerSpeed;
                    
                    // Έλεγξε πρώτα με tiles
                    boolean tileCollision = hasWorldCollision(nextWorldX, player.worldY);

                    // Έλεγξε με decorations
                    boolean decorationCollision = false;
                    ArrayList<Decoration> currentMapDecorations = decorations.get(currentMap);
                    for (Decoration dec : currentMapDecorations) {
                        if (dec.solid) {
                            if (checkDecorationCollision(player, dec, nextWorldX, player.worldY)) {
                                decorationCollision = true;
                                break;
                            }
                        }
                    }
                    
                    // Έλεγξε με τον NPC (αν είναι στερεός)
                    boolean npcCollision = false;
                    // Έλεγξε collision ΜΟΝΟ αν το NPC είναι στατικό (solid && !moving)
                    for (Entity npc : currentMapNPCs) {
                        if (npc.solid && !npc.moving) {
                            if (checkEntityCollision(player, npc, nextWorldX, player.worldY)) {
                                npcCollision = true;
                                break;
                            }
                        }
                    }
                    
                    if (!tileCollision && !npcCollision && !decorationCollision) {
                        player.worldX = nextWorldX;
                        moving = true;

                        // ========== ΕΛΕΓΧΟΣ ΠΕΡΙΟΧΗΣ ΜΕ ΒΑΣΗ ΤΟΝ ΧΑΡΤΗ ==========
                        if (currentMap == 0) { // Overworld
                            // Στον overworld, έλεγξε με βάση συντεταγμένες
                            if (player.worldX > 25 * tileSize && player.worldX < 40 * tileSize &&
                                player.worldY > 27 * tileSize && player.worldY < 43 * tileSize) {
                                currentArea = "overworld";
                            }
                            else {
                                currentArea = "safe";
                            }
                        } 
                        else if (currentMap == 1) { // Dungeon
                            if (player.worldX > 15 * tileSize && player.worldX < 45 * tileSize &&
                                player.worldY > 20 * tileSize && player.worldY < 40 * tileSize) {
                                currentArea = "dungeon";
                            }
                            else {
                                currentArea = "safe";
                            }
                        }
                        else if (currentMap == 2) { // Merchant Village
                            // Στο merchant village, καμία μάχη
                            currentArea = "safe";
                        }
                        
                        // ========== RANDOM ENCOUNTER CHECK ==========
                        encounterStepCounter++;
                        if (encounterStepCounter >= encounterRate) {
                            encounterStepCounter = 0;
                            
                            double chance = Math.random();
                            double encounterChance = 0;
                            
                            // Διαφορετικές πιθανότητες ανά περιοχή
                            if (currentArea.equals("overworld")) {
                                encounterChance = 0.2; // 20%
                            } else if (currentArea.equals("dungeon")) {
                                encounterChance = 0.4; // 40%
                            } else {
                                encounterChance = 0; // Ασφαλής περιοχή
                            }
                            
                            if (chance < encounterChance) {
                                startRandomEncounter();
                            }
                        }
                    }
                }
                else if (keyH.rightPressed) {
                    direction = 2; // Δεξιά
                    
                    // Δοκίμασε να κουνηθείς δεξιά
                    int nextWorldX = player.worldX + playerSpeed;
                    
                    // Έλεγξε πρώτα με tiles
                    boolean tileCollision = hasWorldCollision(nextWorldX, player.worldY);

                    // Έλεγξε με decorations
                    boolean decorationCollision = false;
                    ArrayList<Decoration> currentMapDecorations = decorations.get(currentMap);
                    for (Decoration dec : currentMapDecorations) {
                        if (dec.solid) {
                            if (checkDecorationCollision(player, dec, nextWorldX, player.worldY)) {
                                decorationCollision = true;
                                break;
                            }
                        }
                    }
                    
                    // Έλεγξε με τον NPC (αν είναι στερεός)
                    boolean npcCollision = false;
                    // Έλεγξε collision ΜΟΝΟ αν το NPC είναι στατικό (solid && !moving)
                    for (Entity npc : currentMapNPCs) {
                        if (npc.solid && !npc.moving) {
                            if (checkEntityCollision(player, npc, nextWorldX, player.worldY)) {
                                npcCollision = true;
                                break;
                            }
                        }
                    }
                    
                    if (!tileCollision && !npcCollision && !decorationCollision) {
                        player.worldX = nextWorldX;
                        moving = true;

                        // ========== ΕΛΕΓΧΟΣ ΠΕΡΙΟΧΗΣ ΜΕ ΒΑΣΗ ΤΟΝ ΧΑΡΤΗ ==========
                        if (currentMap == 0) { // Overworld
                            // Στον overworld, έλεγξε με βάση συντεταγμένες
                            if (player.worldX > 25 * tileSize && player.worldX < 40 * tileSize &&
                                player.worldY > 27 * tileSize && player.worldY < 43 * tileSize) {
                                currentArea = "overworld";
                            }
                            else {
                                currentArea = "safe";
                            }
                        } 
                        else if (currentMap == 1) { // Dungeon
                            if (player.worldX > 15 * tileSize && player.worldX < 45 * tileSize &&
                                player.worldY > 20 * tileSize && player.worldY < 40 * tileSize) {
                                currentArea = "dungeon";
                            }
                            else {
                                currentArea = "safe";
                            }
                        }
                        else if (currentMap == 2) { // Merchant Village
                            // Στο merchant village, καμία μάχη
                            currentArea = "safe";
                        }
                        
                        // ========== RANDOM ENCOUNTER CHECK ==========
                        encounterStepCounter++;
                        if (encounterStepCounter >= encounterRate) {
                            encounterStepCounter = 0;
                            
                            double chance = Math.random();
                            double encounterChance = 0;
                            
                            // Διαφορετικές πιθανότητες ανά περιοχή
                            if (currentArea.equals("overworld")) {
                                encounterChance = 0.2; // 20%
                            } else if (currentArea.equals("dungeon")) {
                                encounterChance = 0.4; // 40%
                            } else {
                                encounterChance = 0; // Ασφαλής περιοχή
                            }
                            
                            if (chance < encounterChance) {
                                startRandomEncounter();
                            }
                        }
                    }
                }

                // Μετά δοκιμάζουμε την κάθετη κίνηση (Y)
                if (keyH.upPressed) {
                    direction = 3; // Πάνω
                    
                    // Δοκίμασε να κουνηθείς πάνω
                    int nextWorldY = player.worldY - playerSpeed;
                    
                    // Έλεγξε πρώτα με tiles
                    boolean tileCollision = hasWorldCollision(player.worldX, nextWorldY);

                    // Έλεγξε με decorations
                    boolean decorationCollision = false;
                    ArrayList<Decoration> currentMapDecorations = decorations.get(currentMap);
                    for (Decoration dec : currentMapDecorations) {
                        if (dec.solid) {
                            if (checkDecorationCollision(player, dec, player.worldX, nextWorldY)) {
                                decorationCollision = true;
                                break;
                            }
                        }
                    }
                    
                    // Έλεγξε με τον NPC (αν είναι στερεός)
                    boolean npcCollision = false;
                    // Έλεγξε collision ΜΟΝΟ αν το NPC είναι στατικό (solid && !moving)
                    for (Entity npc : currentMapNPCs) {
                        if (npc.solid && !npc.moving) {
                            if (checkEntityCollision(player, npc, player.worldX, nextWorldY)) {
                                npcCollision = true;
                                break;
                            }
                        }
                    }
                    
                    if (!tileCollision && !npcCollision && !decorationCollision) {
                        player.worldY = nextWorldY;
                        moving = true;

                        // ========== ΕΛΕΓΧΟΣ ΠΕΡΙΟΧΗΣ ΜΕ ΒΑΣΗ ΤΟΝ ΧΑΡΤΗ ==========
                        if (currentMap == 0) { // Overworld
                            // Στον overworld, έλεγξε με βάση συντεταγμένες
                            if (player.worldX > 25 * tileSize && player.worldX < 40 * tileSize &&
                                player.worldY > 27 * tileSize && player.worldY < 43 * tileSize) {
                                currentArea = "overworld";
                            }
                            else {
                                currentArea = "safe";
                            }
                        } 
                        else if (currentMap == 1) { // Dungeon
                            if (player.worldX > 15 * tileSize && player.worldX < 45 * tileSize &&
                                player.worldY > 20 * tileSize && player.worldY < 40 * tileSize) {
                                currentArea = "dungeon";
                            }
                            else {
                                currentArea = "safe";
                            }
                        }
                        else if (currentMap == 2) { // Merchant Village
                            // Στο merchant village, καμία μάχη
                            currentArea = "safe";
                        }
                        
                        // ========== RANDOM ENCOUNTER CHECK ==========
                        encounterStepCounter++;
                        if (encounterStepCounter >= encounterRate) {
                            encounterStepCounter = 0;
                            
                            double chance = Math.random();
                            double encounterChance = 0;
                            
                            // Διαφορετικές πιθανότητες ανά περιοχή
                            if (currentArea.equals("overworld")) {
                                encounterChance = 0.2; // 20%
                            } else if (currentArea.equals("dungeon")) {
                                encounterChance = 0.4; // 40%
                            } else {
                                encounterChance = 0; // Ασφαλής περιοχή
                            }
                            
                            if (chance < encounterChance) {
                                startRandomEncounter();
                            }
                        }
                    }
                }
                else if (keyH.downPressed) {
                    direction = 0; // Κάτω
                    
                    // Δοκίμασε να κουνηθείς κάτω
                    int nextWorldY = player.worldY + playerSpeed;
                    
                    // Έλεγξε πρώτα με tiles
                    boolean tileCollision = hasWorldCollision(player.worldX, nextWorldY);

                    // Έλεγξε με decorations
                    boolean decorationCollision = false;
                    ArrayList<Decoration> currentMapDecorations = decorations.get(currentMap);
                    for (Decoration dec : currentMapDecorations) {
                        if (dec.solid) {
                            if (checkDecorationCollision(player, dec, player.worldX, nextWorldY)) {
                                decorationCollision = true;
                                break;
                            }
                        }
                    }
                    
                    // Έλεγξε με τον NPC (αν είναι στερεός)
                    boolean npcCollision = false;
                    // Έλεγξε collision ΜΟΝΟ αν το NPC είναι στατικό (solid && !moving)
                    for (Entity npc : currentMapNPCs) {
                        if (npc.solid && !npc.moving) {
                            if (checkEntityCollision(player, npc, player.worldX, nextWorldY)) {
                                npcCollision = true;
                                break;
                            }
                        }
                    }
                    
                    
                    if (!tileCollision && !npcCollision && !decorationCollision) {
                        player.worldY = nextWorldY;
                        moving = true;

                        // ========== ΕΛΕΓΧΟΣ ΠΕΡΙΟΧΗΣ ΜΕ ΒΑΣΗ ΤΟΝ ΧΑΡΤΗ ==========
                        if (currentMap == 0) { // Overworld
                            // Στον overworld, έλεγξε με βάση συντεταγμένες
                            if (player.worldX > 25 * tileSize && player.worldX < 40 * tileSize &&
                                player.worldY > 27 * tileSize && player.worldY < 43 * tileSize) {
                                currentArea = "overworld";
                            }
                            else {
                                currentArea = "safe";
                            }
                        } 
                        else if (currentMap == 1) { // Dungeon
                            if (player.worldX > 15 * tileSize && player.worldX < 45 * tileSize &&
                                player.worldY > 20 * tileSize && player.worldY < 40 * tileSize) {
                                currentArea = "dungeon";
                            }
                            else {
                                currentArea = "safe";
                            }
                        }
                        else if (currentMap == 2) { // Merchant Village
                            // Στο merchant village, καμία μάχη
                            currentArea = "safe";
                        }
                        
                        // ========== RANDOM ENCOUNTER CHECK ==========
                        encounterStepCounter++;
                        if (encounterStepCounter >= encounterRate) {
                            encounterStepCounter = 0;
                            
                            double chance = Math.random();
                            double encounterChance = 0;
                            
                            // Διαφορετικές πιθανότητες ανά περιοχή
                            if (currentArea.equals("overworld")) {
                                encounterChance = 0.2; // 20%
                            } else if (currentArea.equals("dungeon")) {
                                encounterChance = 0.4; // 40%
                            } else {
                                encounterChance = 0; // Ασφαλής περιοχή
                            }
                            
                            if (chance < encounterChance) {
                                startRandomEncounter();
                            }
                        }
                    }
                }

                // 🚧 ΟΡΙΑ (τα αφήνουμε για εξτρά ασφάλεια)
                if (player.worldX < 0) player.worldX = 0;
                if (player.worldX > worldWidth - tileSize) player.worldX = worldWidth - tileSize;
                if (player.worldY < 0) player.worldY = 0;
                if (player.worldY > worldHeight - tileSize) player.worldY = worldHeight - tileSize;

                // Έλεγξε για portals
                for (Portal portal : portals) {
                    // Μόνο αν είμαστε στον σωστό χάρτη για αυτό το portal
                    if (currentMap == portal.sourceMap) {
                        int distanceX = Math.abs(player.worldX - portal.worldX);
                        int distanceY = Math.abs(player.worldY - portal.worldY);
                        
                        if (distanceX < tileSize && distanceY < tileSize) {
                            currentMap = portal.targetMap;
                            player.worldX = portal.targetX;
                            player.worldY = portal.targetY;
                            
                            // ========== ΑΛΛΑΓΗ ΜΟΥΣΙΚΗΣ ==========
                            if (currentMap == 0) { // Overworld
                                if (dayTime == 0 || dayTime == 3) {
                                    sound.playMusic("overworld_day");
                                } else {
                                    sound.playMusic("overworld_night");
                                }
                            } else if (currentMap == 1) { // Dungeon
                                sound.playMusic("dungeon");
                            } else if (currentMap == 3) { // town
                                if (dayTime == 0 || dayTime == 3) {
                                    sound.playMusic("town_day");
                                } else {
                                    sound.playMusic("town_night");
                                }
                            } else { // Τρίτος χάρτης
                                sound.playMusic("merchant_village");
                            }
                            currentMusicVolume = musicVolume / 100.0f;
                            sound.setMusicVolume(currentMusicVolume);
                            
                            playSound("portal");
                            startDialogue("Ταξίδεψες σε άλλη περιοχή!");
                            gameState = dialogueState;
                            break;
                        }
                    }
                }

                // ========== ΝΕΟ: Έλεγξε αν ο παίκτης ακουμπάει εχθρό ==========
                ArrayList<Enemy> currentMapEnemies = enemies.get(currentMap);
                for (int i = 0; i < currentMapEnemies.size(); i++) {
                    Enemy enemy = currentMapEnemies.get(i);
                    int distanceX = Math.abs(player.worldX - enemy.worldX);
                    int distanceY = Math.abs(player.worldY - enemy.worldY);
                    
                    if (distanceX < tileSize && distanceY < tileSize) {
                        currentEnemy = enemy;
                        gameState = battleState;
                        break;
                    }
                }
            }

            // Ενημέρωσε την κάμερα (worldX, worldY) να ακολουθεί τον παίκτη
            worldX = player.worldX - screenWidth/2 + tileSize/2;
            worldY = player.worldY - screenHeight/2 + tileSize/2;

            // Μην αφήνεις την κάμερα να βγει έξω από τον κόσμο
            if (worldX < 0) worldX = 0;
            if (worldX > worldWidth - screenWidth) worldX = worldWidth - screenWidth;
            if (worldY < 0) worldY = 0;
            if (worldY > worldHeight - screenHeight) worldY = worldHeight - screenHeight;
            
            // 🔄 ANIMATION: Αλλάζουμε εικόνα ανάλογα με την κατεύθυνση
            if (moving) {
                counter++;
                if (counter > 10) { // Ταχύτητα animation
                    // Εναλλάξ μεταξύ 1ου και 2ου καρέ
                    if (frame == 0) {
                        frame = 1;
                    } else {
                        frame = 0;
                    }
                    counter = 0;
                }
            } else {
                frame = 0; // Όταν σταματάει, δείξε το 1ο καρέ
                counter = 0;
            }
            
            // Αποφάσισε ποια εικόνα θα δείξεις με βάση direction και frame
            if (direction == 0) { // Κάτω
                if (frame == 0) {
                    currentPlayerImage = playerDown1;
                } else {
                    currentPlayerImage = playerDown2;
                }
            } else if (direction == 1) { // Αριστερά
                if (frame == 0) {
                    currentPlayerImage = playerLeft1;
                } else {
                    currentPlayerImage = playerLeft2;
                }
            } else if (direction == 2) { // Δεξιά
                if (frame == 0) {
                    currentPlayerImage = playerRight1;
                } else {
                    currentPlayerImage = playerRight2;
                }
            } else if (direction == 3) { // Πάνω
                if (frame == 0) {
                    currentPlayerImage = playerUp1;
                } else {
                    currentPlayerImage = playerUp2;
                }
            }

            // ========== ΔΙΑΛΟΓΟΣ ΜΕ "ΓΡΑΨΙΜΟ" ==========
            if (gameState == dialogueState || gameState == dialogueOptionsState) {
                if (dialogueTyping) {
                    dialogueFrameCounter++;
                    // Γράψε 1 χαρακτήρα κάθε 2 frames (πιο αργά)
                    if (dialogueFrameCounter >= 2) {
                        if (dialogueCharCounter < fullDialogue.length()) {
                            displayedDialogue += fullDialogue.charAt(dialogueCharCounter);
                            dialogueCharCounter++;
                            
                            // Παίξε ήχο γραψίματος (κάθε 2 χαρακτήρες)
                            if (dialogueCharCounter % 2 == 0) {
                                playSound("type");
                            }
                        } else {
                            dialogueTyping = false;
                        }
                        dialogueFrameCounter = 0;
                    }
                }
            }

            // ========== BATTLE FADE EFFECT ==========
            if (battleFadeOut) {
                battleFadeAlpha += FADE_SPEED;
                if (battleFadeAlpha >= 255) {
                    battleFadeAlpha = 255;
                    battleFadeOut = false;
                    
                    // Εδώ ξεκινάει η μάχη!
                    gameState = battleState;
                    battleEntering = true;
                    battleTransitionTimer = 0;
                    battleWalkTimer = 0;
                    battleWalkFrame = 0;
                    battleStarting = false;
                    
                    // Ρύθμισε τους εχθρούς και τους παίκτες
                    setupBattleEntities();
                    
                    // Ξεκίνα fade in
                    battleFadeIn = true;
                }
            }

            if (battleFadeIn) {
                battleFadeAlpha -= FADE_SPEED;
                if (battleFadeAlpha <= 0) {
                    battleFadeAlpha = 0;
                    battleFadeIn = false;
                }
            }
            
            // 2. ΣΧΕΔΙΑΣΗ
            repaint();
            
            // 3. ΣΥΓΧΡΟΝΙΣΜΟΣ (ίδιο όπως πριν)
            try {
                double remainingTime = nextDrawTime - System.nanoTime();
                remainingTime = remainingTime / 1000000;
                if (remainingTime < 0) remainingTime = 0;
                Thread.sleep((long) remainingTime);
                nextDrawTime += drawInterval;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void handleDialogueOption(int option) {
        if (fullDialogue.contains("Φύλακας")) {  // ΑΛΛΑΓΗ: currentDialogue -> fullDialogue
            if (fullDialogue.contains("Χρειάζομαι βοήθεια")) {  // ΑΛΛΑΓΗ
                if (option == 0) {
                    player.quests.removeIf(q -> q.name.equals("Καθάρισμα τεράτων"));
                    
                    Quest newQuest = new Quest("Καθάρισμα τεράτων", "Σκότωσε 5 goblins");
                    newQuest.targetItem = "Goblin Ear";
                    newQuest.requiredAmount = 5;
                    newQuest.active = true;
                    player.quests.add(newQuest);
                    
                    startDialogue("Σε ευχαριστώ! Γύρνα όταν έχεις 5 αυτιά goblin!");
                    gameState = dialogueState;
                } else if (option == 1) {
                    startDialogue("Κρίμα... Αν αλλάξεις γνώμη, είμαι εδώ.");
                    gameState = dialogueState;
                }
            }
            else if (fullDialogue.contains("Πώς πάει η αποστολή")) {  // ΑΛΛΑΓΗ
                if (option == 0) {
                    boolean hasItem = false;
                    for (int i = 0; i < inventory.storage.length; i++) {
                        Item item = inventory.storage[i];
                        if (item != null && item.name.equals("Goblin Ear") && item.amount >= 5) {
                            hasItem = true;
                            item.amount -= 5;
                            if (item.amount == 0) {
                                inventory.storage[i] = null;
                            }
                            break;
                        }
                    }
                    
                    if (hasItem) {
                        player.gold += 100;
                        startDialogue("Μπράβο! Ορίστε 100 χρυσά ως ανταμοιβή!");
                        for (Quest q : player.quests) {
                            if (q.name.equals("Καθάρισμα τεράτων")) {
                                q.completed = true;
                                break;
                            }
                        }
                    } else {
                        startDialogue("Δεν έχεις ακόμα 5 αυτιά goblin... Γύρνα όταν τα μαζέψεις!");
                    }
                    gameState = dialogueState;
                } else {
                    startDialogue("Καλή τύχη! Γύρνα όταν τα μαζέψεις.");
                    gameState = dialogueState;
                }
            }
        }
    }

    public void startDialogue(String text) {
        fullDialogue = text;
        displayedDialogue = "";
        dialogueCharCounter = 0;
        dialogueTyping = true;
        
        // Παίξε ήχο έναρξης διαλόγου (προαιρετικά)
        playSound("dialogue_start");
    }

    public void playSound(String soundName) {
        try {
            // Προσπάθησε πρώτα να παίξει ως sound effect
            sound.playSE(soundName);
        } catch (Exception e) {
            // Αν αποτύχει, δοκίμασε με την παλιά μέθοδο setFile/play
            try {
                sound.setFile(soundName);
                sound.play();
            } catch (Exception ex) {
                // Αγνόησε αν δεν υπάρχει
            }
        }
    }

    public void startMusicCrossfade(String newMusic) {
        if (currentMap != 0 && currentMap != 3) return;
        
        nextMusic = newMusic;
        isCrossfading = true;
    }

    public void toggleFullScreen() {
        try {
            if (isFullScreen) {
                // Έξοδος από fullscreen
                window.dispose();
                window.setUndecorated(false);
                window.setVisible(true);
                device.setFullScreenWindow(null);
                
                // Επανάφερε στο κανονικό μέγεθος
                window.setSize(screenWidth, screenHeight);
                scaleX = 1.0;
                scaleY = 1.0;
                isFullScreen = false;
            } else {
                // Είσοδος σε fullscreen
                window.dispose();
                window.setUndecorated(true);
                window.setVisible(true);
                device.setFullScreenWindow(window);
                
                // Υπολόγισε τα scale factors
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                scaleX = screenSize.getWidth() / screenWidth;
                scaleY = screenSize.getHeight() / screenHeight;
                isFullScreen = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startRandomEncounter() {
        float previousVolume = currentMusicVolume;
        // Επέλεξε τυχαίο τέρας ανάλογα με την περιοχή
        String[] possibleEnemies;
        
        if (currentArea.equals("overworld")) {
            possibleEnemies = new String[]{"Slime", "Red Slime"};
        } else { // dungeon
            possibleEnemies = new String[]{"Bat", "Orc", "Skeleton", "Bat"};
        }
        
        String enemyType = possibleEnemies[(int)(Math.random() * possibleEnemies.length)];
        
        Enemy newEnemy = null;
        
        // Δημιούργησε το κατάλληλο τέρας
        if (enemyType.equals("Slime")) {
            newEnemy = new Enemy_Slime(this);
        } else if (enemyType.equals("Bat")) {
            newEnemy = new Enemy_Bat(this);
        } else if (enemyType.equals("Red Slime")) {
            newEnemy = new Enemy_RedSlime(this);
        } else if (enemyType.equals("Orc")) {
            newEnemy = new Enemy_Orc(this);
        } else if (enemyType.equals("Skeleton")) {
            newEnemy = new Enemy_Skeleton(this);
        }
        
        if (newEnemy != null) {
            // ΞΕΚΙΝΑΕΙ ΜΑΧΗ
            battleStarting = true;
            startBattleWithTransition(newEnemy);
            currentDialogue = "Εμφανίστηκε " + enemyType + "!";

            // ========== ΑΛΛΑΓΗ ΜΟΥΣΙΚΗΣ ==========
            sound.playMusic("battle");
            // Εφάρμοσε την ίδια ένταση στη μάχη
            sound.setMusicVolume(previousVolume);
            playSound("enemy_appear");
        }
    }

    // Νέα μέθοδος για έναρξη μάχης με transition
    public void startBattleWithTransition(Enemy enemy) {
        currentEnemy = enemy;

        // Ξεκίνα fade out
        battleFadeOut = true;
        battleFadeAlpha = 0;
        battleFadeIn = false;

        try {
            groundImage = ImageIO.read(new File("res/battle/ground_grass.png"));
            if (groundImage == null) {
                // Fallback: δημιούργησε gradient ground
                groundImage = createGroundGradient();
            }
        } catch (Exception e) {
            groundImage = createGroundGradient();
        }

        
        // Δημιούργησε τυχαίο background για τη μάχη
        createBattleBackground();
        
        // Ρύθμισε τους εχθρούς και τους παίκτες για οριζόντια μάχη
        //setupBattleEntities();
        
        // Άλλαξε μουσική
        sound.playMusic("battle");
    }

    public BufferedImage createGroundGradient() {
        BufferedImage ground = new BufferedImage(groundWidth, groundHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = ground.createGraphics();
        
        // Gradient από σκούρο σε ανοιχτό πράσινο
        GradientPaint gradient = new GradientPaint(
            0, 0, new Color(34, 139, 34),    // Forest green
            0, groundHeight, new Color(144, 238, 144) // Light green
        );
        
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, groundWidth, groundHeight);
        g2d.dispose();
        
        return ground;
    }

    public void createBattleBackground() {
        // Διάλεξε τυχαίο background ανάλογα με τον χάρτη
        String[] possibleBackgrounds;
        
        if (currentMap == 0) { // Overworld
            possibleBackgrounds = new String[]{"field1"};
        } else if (currentMap == 1) { // Dungeon
            possibleBackgrounds = new String[]{"dungeon1", "dungeon2", "cave"};
        } else {
            possibleBackgrounds = new String[]{"field1"};
        }
        
        String bgName = possibleBackgrounds[(int)(Math.random() * possibleBackgrounds.length)];
        
        try {
            battleBackground = ImageIO.read(new File("res/battle/bg_" + bgName + ".png"));
        } catch (Exception e) {
            // Αν δεν υπάρχει εικόνα, δημιούργησε gradient background
            battleBackground = createGradientBackground();
        }
    }

    public BufferedImage createGradientBackground() {
        BufferedImage bg = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bg.createGraphics();
        
        // Gradient ανάλογα με τον χάρτη
        Color topColor, bottomColor;
        if (currentMap == 0) {
            topColor = new Color(100, 150, 200); // Μπλε ουρανός
            bottomColor = new Color(50, 100, 50); // Πράσινο έδαφος
        } else {
            topColor = new Color(50, 50, 50); // Σκοτεινό
            bottomColor = new Color(100, 50, 50); // Κοκκινωπό
        }
        
        GradientPaint gradient = new GradientPaint(0, 0, topColor, 
                                                0, screenHeight, bottomColor);
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, screenWidth, screenHeight);
        g2d.dispose();
        
        return bg;
    }

    public void setupBattleEntities() {
        battleEnemies.clear();
        battlePlayers.clear();
        
        // Υπολόγισε τη θέση του εδάφους (στο κάτω μέρος της οθόνης)
        groundY = screenHeight - groundHeight - tileSize - 100;
        groundX = 0;
        
        // Δημιούργησε τους εχθρούς
        BattleEnemy be = new BattleEnemy();
        be.enemy = currentEnemy;
        be.hp = currentEnemy.hp;
        be.maxHp = currentEnemy.maxHp;
        
        // Φόρτωσε εικόνα εχθρού
        try {
            String enemyName = currentEnemy.getClass().getSimpleName().replace("Enemy_", "").toLowerCase();
            File rightImage = new File("res/enemy/" + enemyName + "_right_1.png");
            
            if (rightImage.exists()) {
                be.image = ImageIO.read(rightImage);
            } else {
                be.image = currentEnemy.currentImage;
            }
        } catch (Exception e) {
            be.image = currentEnemy.currentImage;
        }
        
        // Ο εχθρός ξεκινάει εκτός οθόνης ΑΡΙΣΤΕΡΑ
        be.x = -tileSize * 4;
        be.y = groundY - tileSize; // Πάνω στο έδαφος
        be.targetX = tileSize * 3;
        be.targetY = groundY - tileSize;
        
        battleEnemies.add(be);
        
        // Δημιούργησε τον παίκτη
        BattlePlayer bp = new BattlePlayer();
        bp.player = player;
        bp.hp = player.hp;
        bp.maxHp = player.maxHp;
        bp.mp = player.mp;
        bp.maxMp = player.maxMp;
        
        // Ο παίκτης ξεκινάει εκτός οθόνης ΔΕΞΙΑ
        bp.x = screenWidth + tileSize * 2;
        bp.y = groundY - tileSize;
        bp.targetX = screenWidth - tileSize * 5;
        bp.targetY = groundY - tileSize;
        
        battlePlayers.add(bp);
    }

    // Ελέγχει αν η επόμενη θέση του ήρωα έχει collision - ΒΕΛΤΙΩΜΕΝΗ ΕΚΔΟΣΗ
    public boolean hasWorldCollision(int nextWorldX, int nextWorldY) {
        int leftX = nextWorldX;
        int rightX = nextWorldX + tileSize - 1;
        int topY = nextWorldY;
        int bottomY = nextWorldY + tileSize - 1;
        
        int topLeftCol = leftX / tileSize;
        int topLeftRow = topY / tileSize;
        int topRightCol = rightX / tileSize;
        int topRightRow = topY / tileSize;
        int bottomLeftCol = leftX / tileSize;
        int bottomLeftRow = bottomY / tileSize;
        int bottomRightCol = rightX / tileSize;
        int bottomRightRow = bottomY / tileSize;
        
        boolean collision = false;
        
        if (isValidWorldTile(topLeftRow, topLeftCol)) {
            collision = tileM.tile[tileM.mapTileNum[currentMap][topLeftRow][topLeftCol]].collision;
        }
        if (!collision && isValidWorldTile(topRightRow, topRightCol)) {
            collision = tileM.tile[tileM.mapTileNum[currentMap][topRightRow][topRightCol]].collision;
        }
        if (!collision && isValidWorldTile(bottomLeftRow, bottomLeftCol)) {
            collision = tileM.tile[tileM.mapTileNum[currentMap][bottomLeftRow][bottomLeftCol]].collision;
        }
        if (!collision && isValidWorldTile(bottomRightRow, bottomRightCol)) {
            collision = tileM.tile[tileM.mapTileNum[currentMap][bottomRightRow][bottomRightCol]].collision;
        }
        
        return collision;
    }

    // Για NPC και άλλες οντότητες (με Entity)
    public boolean checkCollision(Entity entity, int nextX, int nextY) {
        // Ίδιος κώδικας με το hasWorldCollision, αλλά μπορείς απλά να καλέσεις:
        return hasWorldCollision(nextX, nextY); // Αφού προς το παρόν κάνουν το ίδιο
    }

    // Βοηθητική μέθοδος για έλεγχο ορίων
    public boolean isValidWorldTile(int row, int col) {
        return row >= 0 && row < maxWorldRow && col >= 0 && col < maxWorldCol;
    }

    // Έλεγχος σύγκρουσης μεταξύ δύο οντοτήτων (π.χ. ήρωας και NPC)
    public boolean checkEntityCollision(Entity e1, Entity e2, int nextWorldX, int nextWorldY) {
        if (!e2.solid) return false;
        
        // Όρια της e1 στην επόμενη θέση (world coordinates)
        int e1Left = nextWorldX;
        int e1Right = nextWorldX + tileSize;
        int e1Top = nextWorldY;
        int e1Bottom = nextWorldY + tileSize;
        
        // Όρια της e2 στην τωρινή θέση (world coordinates)
        int e2Left = e2.worldX;
        int e2Right = e2.worldX + tileSize;
        int e2Top = e2.worldY;
        int e2Bottom = e2.worldY + tileSize;
        
        if (e1Left < e2Right && e1Right > e2Left && 
            e1Top < e2Bottom && e1Bottom > e2Top) {
            return true;
        }
        
        return false;
    }

    public boolean checkDecorationCollision(Entity e, Decoration d, int nextWorldX, int nextWorldY) {
        if (!d.solid) return false;
        
        // Όρια παίκτη
        Rectangle playerRect = new Rectangle(nextWorldX, nextWorldY, tileSize, tileSize);
        
        // Αν έχει custom collision rectangles, χρησιμοποίησέ τα
        if (d.collisionRects != null && !d.collisionRects.isEmpty()) {
            for (Rectangle rect : d.collisionRects) {
                if (playerRect.intersects(rect)) {
                    return true;
                }
            }
            return false;
        }
        
        // Αλλιώς χρησιμοποίησε ολόκληρο το decoration
        Rectangle decRect = new Rectangle(d.worldX, d.worldY, d.width, d.height);
        return playerRect.intersects(decRect);
    }

    public BufferedImage scaleDecoration(BufferedImage original, int targetHeight) {
        // Υπολόγισε το πλάτος διατηρώντας αναλογία
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        int targetWidth = (originalWidth * targetHeight) / originalHeight;
        
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        
        return scaled;
    }

    public void loadHouses() {
        try {
            // ========== ΜΕΓΑΛΟ ΣΠΙΤΙ (6x6) ==========
            BufferedImage originalHouse = ImageIO.read(new File("res/decorations/house_1.png"));
            BufferedImage scaledHouse = scaleDecoration(originalHouse, tileSize * 6);
            
            // Δημιούργησε τον πίνακα collision 6x6
            // true = δεν περνάς, false = περνάς
            boolean[][] bigHouseCollision = {
                {true,  true,  true,  true,  true,  true,  true,  true,  true},  // Πάνω σειρά
                {true,  true,  true,  true,  true,  true,  true,  true,  true},
                {true,  true,  true,  true,  true,  true,  true,  true,  true},
                {true,  true,  true,  true,  true,  true,  true,  true,  true},
                {false,  false,  false,  true,  true,  true,  true,  false,  false}  // Κάτω σειρά - η πόρτα στη μέση
            };
            
            // Δημιούργησε το decoration (χωρίς collision ακόμα)
            Decoration houseExt = new Decoration("Big House", scaledHouse,
                                                10 * tileSize, 9 * tileSize,
                                                scaledHouse.getWidth(), scaledHouse.getHeight(),
                                                new ArrayList<Rectangle>()); // Κενό collision
            
            decorations.get(3).add(houseExt);
            
            // Δημιούργησε το house object
            Rectangle doorArea = new Rectangle(
                14 * tileSize - tileSize/2,      // 14*48 - 24 = 648 pixels (λίγο πιο αριστερά)
                13 * tileSize - tileSize/2,      // 13*48 - 24 = 600 pixels (λίγο πιο πάνω)
                tileSize * 2,                    // 96 pixels πλάτος (2 tiles)
                tileSize * 2                      // 96 pixels ύψος (2 tiles)
            );

            House bigHouse = new House("Big House", 3, houseExt, doorArea,
                                4,                       // interiorMap = 4
                                12 * tileSize,            // spawnX = 672
                                13 * tileSize,            // spawnY = 624
                                14 * tileSize,            // exitX = 1200
                                14 * tileSize,            // exitY = 1200
                                this);
            
            // Πέρασε τον πίνακα collision
            bigHouse.setCollisionMap(bigHouseCollision);
            
            // ========== Φόρτωσε animated door για το μεγάλο σπίτι ==========
            AnimatedDoor bigHouseDoor = new AnimatedDoor();
            bigHouseDoor.loadFrames("res/decorations/door_big/", "house", 6); // 5 frames
            bigHouse.setAnimatedDoor(bigHouseDoor, 2, 6); // Η πόρτα είναι στο col 2, row 5 (0-based)
            
            houses.add(bigHouse);
            
            // ========== ΜΙΚΡΟ ΣΠΙΤΙ (4x4) ==========
            BufferedImage originalSmallHouse = ImageIO.read(new File("res/decorations/house_2.png"));
            BufferedImage scaledSmallHouse = scaleDecoration(originalSmallHouse, tileSize * 5);
            
            // Πίνακας collision 4x4 - όλα true εκτός από την πόρτα
            boolean[][] smallHouseCollision = {
                {true,  true,  true,  true,  true,  true},  // Πάνω σειρά
                {true,  true,  true,  true,  true,  true},
                {true,  true,  true,  true,  true,  true},
                {true,  true,  true,  true,  true,  true},
                {true,  true,  true,  true,  true,  true},
                {true,  true,  true, true,  true,  true}   // Κάτω σειρά - η πόρτα στη μέση
            };
            
            Decoration smallHouseExt = new Decoration("Small House", scaledSmallHouse,
                                                    29 * tileSize, 9 * tileSize,
                                                    scaledSmallHouse.getWidth(), scaledSmallHouse.getHeight(),
                                                    new ArrayList<Rectangle>());
            
            decorations.get(3).add(smallHouseExt);
            
            Rectangle smallDoorArea = new Rectangle(
                29 * tileSize - tileSize,      // 31*48 - 24 = 1464 pixels
                14 * tileSize - tileSize,      // 14*48 - 24 = 648 pixels
                tileSize * 3,                    // 144 pixels πλάτος
                tileSize * 3                      // 144 pixels ύψος
            );
            
            House smallHouse = new House("Small House", 3, smallHouseExt, smallDoorArea,
                                5,                       // interiorMap = 5
                                12 * tileSize,            // spawnX = 672
                                13 * tileSize,            // spawnY = 624
                                31 * tileSize,            // exitX = 1200
                                14 * tileSize,            // exitY = 1200
                                this);
            
            smallHouse.setCollisionMap(smallHouseCollision);
            
            // Προαιρετικά animated door για το μικρό σπίτι
            AnimatedDoor smallHouseDoor = new AnimatedDoor();
            smallHouseDoor.loadFrames("res/decorations/door_small/", "door", 6);
            smallHouse.setAnimatedDoor(smallHouseDoor, 1, 3);
            
            houses.add(smallHouse);
            
            System.out.println("Houses loaded successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Μέθοδος για έξοδο από το σπίτι (π.χ. όταν πατάς Enter μπροστά από την πόρτα από μέσα)
        public void exitHouse(House house) {
            currentMap = house.exteriorMap;
            player.worldX = house.exitX;
            player.worldY = house.exitY;
            
            // Επανάφερε την εξωτερική μουσική
            if (currentMap == 3) { // town
                if (dayTime == 0 || dayTime == 3) {
                    sound.playMusic("town_day");
                } else {
                    sound.playMusic("town_night");
                }
            }
            
            playSound("door_close");
        }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Μετατροπή Graphics σε Graphics2D (πιο δυνατό)
        Graphics2D g2 = (Graphics2D) g;

        if (isFullScreen) {
            g2.scale(scaleX, scaleY);
        }
        

        if (gameState == titleState) {
            drawTitleScreen(g2);
        } else if (gameState == controlsState) {
            drawControlsScreen(g2, "TITLE");
        }else if (gameState == pauseControlsState) {
            drawControlsScreen(g2, "PAUSE"); 
        } else if (gameState == pauseState) {
            // Πρώτα ζωγράφισε το κανονικό παιχνίδι
            drawGame(g2);
            drawLighting(g2);
            // Μετά ζωγράφισε το pause menu από πάνω
            drawPauseScreen(g2);
        } else if (gameState == quitConfirmState) {
            // Πρώτα ζωγράφισε το κανονικό παιχνίδι
            drawGame(g2);
            drawLighting(g2);
            // Μετά ζωγράφισε το quit confirm
            drawQuitConfirmScreen(g2);
        } else {
            // Κανονικό παιχνίδι
            drawGame(g2);
            drawLighting(g2);

            // ========== Battle Screen ==========
            if (gameState == battleState) {
                drawBattleScreen(g2);
            }

            // Αν είμαστε σε κατάσταση διαλόγου, ζωγράφισε την οθόνη διαλόγου
            if (gameState == dialogueState || gameState == dialogueOptionsState) {
                drawDialogueScreen(g2);
            }

            if (gameState == inventoryState) {
                drawInventory(g2);
            }
            if (gameState == mapState) {
                drawMapScreen(g2);
            }
            if (gameState == shopState) {
                drawShopScreen(g2);
            }
        }
        // ========== ΠΡΟΣΘΕΣΕ ΤΟ FADE EFFECT ΓΙΑ ΟΛΕΣ ΤΙΣ ΚΑΤΑΣΤΑΣΕΙΣ ==========
        // Αυτό ζωγραφίζεται ΠΑΝΤΑ από πάνω, ανεξάρτητα από το gameState
        if (battleFadeOut || battleFadeIn) {
            g2.setColor(new Color(0, 0, 0, battleFadeAlpha));
            g2.fillRect(0, 0, screenWidth, screenHeight);
        }
        
        g2.dispose(); // Καθάρισε (καλή πρακτική)
    }

    // ==== ζωγραφίζεις το παιχνίδι ===== 
    public void drawGame(Graphics2D g2) {
        // 🗺️ ΖΩΓΡΑΦΙΣΕ ΠΡΩΤΑ ΤΟΝ ΧΑΡΤΗ (από κάτω)
        tileM.draw(g2);
        
        // ========== DECORATIONS (πίσω από τον παίκτη) ==========
        ArrayList<Decoration> currentMapDecorations = decorations.get(currentMap);
        for (Decoration dec : currentMapDecorations) {
            int screenX = dec.worldX - worldX;
            int screenY = dec.worldY - worldY;
            
            // Έλεγξε αν είναι ορατό στην οθόνη
            if (screenX + dec.width > 0 && screenX < screenWidth &&
                screenY + dec.height > 0 && screenY < screenHeight) {
                
                // Ζωγράφισε στο ΚΑΝΟΝΙΚΟ του μέγεθος
                g2.drawImage(dec.image, screenX, screenY, dec.width, dec.height, null);
            }
        }
        // Ζωγράφισε items στο έδαφος
        ArrayList<ItemOnGround> currentMapItems = itemsOnGround.get(currentMap);
        for (ItemOnGround item : currentMapItems) {
            int screenX = item.worldX - worldX;
            int screenY = item.worldY - worldY;
            
            if (screenX + tileSize > 0 && screenX < screenWidth &&
                screenY + tileSize > 0 && screenY < screenHeight) {
                g2.drawImage(item.image, screenX, screenY, tileSize, tileSize, null);
            }
        }
        
        // 🧙 ΖΩΓΡΑΦΙΣΕ ΜΕΤΑ ΤΟΝ ΗΡΩΑ (από πάνω)
        if (currentPlayerImage != null) {
            int screenX = player.worldX - worldX;
            int screenY = player.worldY - worldY;
            g2.drawImage(currentPlayerImage, screenX, screenY, tileSize, tileSize, null);
        }
        
        // 👴 NPC
        ArrayList<Entity> currentMapNPCs = npcs.get(currentMap);
        for (Entity npc : currentMapNPCs) {
            int npcScreenX = npc.worldX - worldX;
            int npcScreenY = npc.worldY - worldY;
            g2.drawImage(npc.currentImage, npcScreenX, npcScreenY, tileSize, tileSize, null);
        }
    }

    public void drawPanel(Graphics2D g2, int x, int y, int width, int height, Color bgColor, Color borderColor) {
        // Σκούρο ημιδιάφανο φόντο
        g2.setColor(bgColor);
        g2.fillRoundRect(x, y, width, height, 35, 35);
        
        // Λευκό περίγραμμα
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(x, y, width, height, 35, 35);
    }

    // Overload για default χρώματα (όπως στα dialogue boxes)
    public void drawPanel(Graphics2D g2, int x, int y, int width, int height) {
        drawPanel(g2, x, y, width, height, new Color(0, 0, 0, 220), Color.white);
    }

    public void drawTitleScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0));
        g2.fillRect(0, 0, screenWidth, screenHeight);
        
        if (titleLogo != null) {
            int logoWidth = screenWidth - 200;
            int logoHeight = logoWidth / 2;
            int logoX = screenWidth/2 - logoWidth/2;
            int logoY = 20;
            g2.drawImage(titleLogo, logoX, logoY, logoWidth, logoHeight, null);
        } else {
            g2.setFont(maruMonicaLarge);
            String text = "My RPG";
            int x = getXforCenteredText(text, g2);
            int y = 150;
            
            g2.setColor(Color.gray);
            g2.drawString(text, x + 5, y + 5);
            g2.setColor(Color.white);
            g2.drawString(text, x, y);
        }
        
        g2.setFont(maruMonicaBold);
        String[] options = {"START GAME", "SETTINGS", "CONTROLS"};
        int startY = 300;
        int spacing = 60;
        
        for (int i = 0; i < options.length; i++) {
            int x = getXforCenteredText(options[i], g2) - 10;
            int y = startY + i * spacing;
            
            if (i == titleCommandNum) {
                g2.setColor(Color.yellow);
                g2.drawString(">", x - 40, y);
                g2.drawString(options[i], x, y);
            } else {
                g2.setColor(Color.white);
                g2.drawString(options[i], x, y);
            }
        }
        
        g2.setFont(maruMonicaSmall);
        g2.setColor(Color.gray);
        String controls = "Use ↑/↓ to navigate, Enter to select";
        int x = getXforCenteredText(controls, g2);
        g2.drawString(controls, x, screenHeight - 50);
    }

    public void drawControlsScreen(Graphics2D g2, String source) {
        g2.setColor(new Color(0, 0, 0, 220));
        g2.fillRect(0, 0, screenWidth, screenHeight);
        
        // Τίτλος
        g2.setFont(maruMonicaLarge);
        g2.setColor(Color.yellow);
        String title = "CONTROLS";
        int x = getXforCenteredText(title, g2);
        g2.drawString(title, x, 80);
        
        // Πλαίσιο για τα controls
        drawPanel(g2, 100, 120, screenWidth - 200, screenHeight - 240);
        
        // Controls
        g2.setFont(maruMonica);
        g2.setColor(Color.white);
        
        String[][] controls = {
            {"ARROW KEYS", "Move character"},
            {"ENTER", "Interact with NPCs / Confirm"},
            {"I", "Open/Close Inventory"},
            {"E", "Pick up items"},
            {"M", "Open Map (requires World Map)"},
            {"SHIFT", "Switch between Storage/Equipment"},
            {"ESC", "Open Pause Menu"},
            {"Q", "Clear quests (debug)"}
        };
        
        int startY = 180;
        int spacing = 40;
        
        for (int i = 0; i < controls.length; i++) {
            g2.setColor(Color.yellow);
            g2.drawString(controls[i][0], 150, startY + i * spacing);
            g2.setColor(Color.lightGray);
            g2.drawString(controls[i][1], 350, startY + i * spacing);
        }
        
        // Μάχη controls
        g2.setColor(Color.cyan);
        g2.setFont(maruMonicaBold);
        String battleTitle = "BATTLE CONTROLS";
        x = getXforCenteredText(battleTitle, g2);
        g2.drawString(battleTitle, x, 500);
        
        g2.setFont(maruMonica);
        g2.setColor(Color.white);
        String[] battleControls = {
            "←/→ : Select action",
            "ENTER : Confirm action"
        };
        
        for (int i = 0; i < battleControls.length; i++) {
            x = getXforCenteredText(battleControls[i], g2);
            g2.drawString(battleControls[i], x, 540 + i * 30);
        }
        
        // Οδηγία εξόδου
        g2.setFont(maruMonicaSmall);
        g2.setColor(Color.gray);
        String exit;
        if (source.equals("TITLE")) {
            exit = "Press ENTER or M or I to return to title screen";
        } else {
            exit = "Press ENTER or ESC to return to pause menu";
        }
        x = getXforCenteredText(exit, g2);
        g2.drawString(exit, x, screenHeight - 50);
    }

    // Βοηθητική μέθοδος για κεντράρισμα κειμένου
    public int getXforCenteredText(String text, Graphics2D g2) {
        int length = (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        return screenWidth/2 - length/2;
    }

    // ========== Μέθοδος για μάχη ==========
    public void drawBattleScreen(Graphics2D g2) {
        // Σκούρο φόντο για transition
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, screenWidth, screenHeight);
        
        // Ζωγράφισε το background της μάχης
        if (battleBackground != null) {
            g2.drawImage(battleBackground, 0, 0, screenWidth, screenHeight, null);
        }
        
        // ========== ΖΩΓΡΑΦΙΣΕ ΤΗΝ ΠΛΑΤΦΟΡΜΑ ΜΕ ΕΙΚΟΝΑ ==========
        if (groundImage != null) {
            // Υπολόγισε τις συντεταγμένες της πλατφόρμας
            int groundScreenX = groundX; // Συνήθως 0
            int groundScreenY = groundY; // Αυτό το έχεις υπολογίσει στο setupBattleEntities

            int gw = groundWidth;   // Πλάτος (συνήθως screenWidth)
            int gh = groundHeight;  // Ύψος (π.χ. tileSize * 2)

            // Πόσο θα στενέψει η πάνω πλευρά για προοπτική (τώρα θα είναι αρνητικό για να βγαίνει έξω)
            int topInset = -200; // ΑΡΝΗΤΙΚΗ ΤΙΜΗ για να βγουν οι πάνω γωνίες ΕΞΩ από την οθόνη

            // ΕΠΙΜΗΚΥΝΣΗ: Τα κάτω σημεία βγαίνουν έξω από την οθόνη αριστερά και δεξιά
            int leftExtension = 500;  // Πόσο θα βγει αριστερά
            int rightExtension = 500; // Πόσο θα βγει δεξιά

            // Ορίζουμε τις 4 γωνίες του τραπεζίου (σε screen coordinates)
            int[] xPoints = {
                groundScreenX + topInset,           // Πάνω αριστερά (βγαίνει αριστερά)
                groundScreenX + gw - topInset,      // Πάνω δεξιά (βγαίνει δεξιά)
                groundScreenX + gw + rightExtension, // Κάτω δεξιά (βγαίνει δεξιά)
                groundScreenX - leftExtension        // Κάτω αριστερά (βγαίνει αριστερά)
            };
            int[] yPoints = {
                groundScreenY,                        // Πάνω αριστερά
                groundScreenY,                        // Πάνω δεξιά
                groundScreenY + gh,                   // Κάτω δεξιά
                groundScreenY + gh                     // Κάτω αριστερά
            };

            // Ζωγράφισε την εικόνα του εδάφους ΠΑΝΩ στο τραπέζιο
            // Χρησιμοποιούμε clipping για να "κόψουμε" την εικόνα στο σχήμα του τραπεζίου
            
            // Αποθήκευσε το παλιό clip
            java.awt.Shape oldClip = g2.getClip();
            
            // Δημιούργησε polygon στο σχήμα του τραπεζίου και όρισέ το ως νέο clip
            java.awt.Polygon groundPolygon = new java.awt.Polygon(xPoints, yPoints, 4);
            g2.setClip(groundPolygon);
            
            // Ζωγράφισε την εικόνα να καλύπτει όλη την περιοχή
            // Η εικόνα θα "κοπεί" αυτόματα στο σχήμα του τραπεζίου
            g2.drawImage(groundImage, 
                groundScreenX - leftExtension - 100,  // Ξεκίνα πιο αριστερά (ακόμα πιο έξω)
                groundScreenY - 50,                    // Ξεκίνα πιο πάνω
                gw + leftExtension + rightExtension + 200, // Ακόμα πιο φαρδιά
                gh + 100,                               // Πιο ψηλή
                null);
            
            // Επαναφορά του παλιού clip
            g2.setClip(oldClip);
            
            // Σκιά από κάτω (για να φαίνεται ότι "πατάει" στο έδαφος)
            g2.setColor(new Color(0, 0, 0, 70));
            int[] shadowX = {
                groundScreenX - leftExtension + 20, 
                groundScreenX + gw + rightExtension - 20, 
                groundScreenX + gw + rightExtension, 
                groundScreenX - leftExtension
            };
            int[] shadowY = {
                groundScreenY + gh, 
                groundScreenY + gh, 
                groundScreenY + gh + 15, 
                groundScreenY + gh + 15
            };
            g2.fillPolygon(shadowX, shadowY, 4);

        } else {
            // Fallback: απλό έδαφος
            g2.setColor(new Color(34, 139, 34));
            g2.fillRect(0, screenHeight - tileSize*3, screenWidth, tileSize*2);
        }
        
        // ========== ΖΩΓΡΑΦΙΣΕ ΕΧΘΡΟΥΣ ==========
        for (BattleEnemy be : battleEnemies) {
            int drawX = (int)be.x;
            int drawY = (int)be.y;
            
            // Σκιά
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillOval(drawX + tileSize/2, drawY + tileSize*2 - 5, tileSize*2, tileSize/3);
            
            // Εχθρός
            int spriteSize = tileSize * 2;
            g2.drawImage(be.image, drawX, drawY, spriteSize, spriteSize, null);
            
            // HP Bar
            int barWidth = spriteSize;
            int barHeight = 8;
            int barY = drawY - 15;
            
            g2.setColor(Color.black);
            g2.fillRect(drawX, barY, barWidth, barHeight);
            g2.setColor(Color.green);
            double hpPercentage = (double)be.hp / be.maxHp;
            int hpWidth = (int)(barWidth * hpPercentage);
            g2.fillRect(drawX, barY, hpWidth, barHeight);
            
            g2.setColor(Color.white);
            g2.setFont(new Font("Arial", Font.BOLD, 8));
            String hpText = be.hp + "/" + be.maxHp;
            int textX = drawX + (barWidth/2) - 15;
            g2.drawString(hpText, textX, barY - 2);
        }
        
        // ========== ΖΩΓΡΑΦΙΣΕ ΠΑΙΚΤΕΣ ==========
        for (BattlePlayer bp : battlePlayers) {
            int drawX = (int)bp.x;
            int drawY = (int)bp.y;
            
            // Σκιά
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillOval(drawX + tileSize/2, drawY + tileSize*2 - 5, tileSize*2, tileSize/3);
            
            int spriteSize = tileSize * 2;
            g2.drawImage(bp.image, drawX, drawY, spriteSize, spriteSize, null);
            
            // HP Bar
            int barWidth = spriteSize;
            
            g2.setColor(Color.red);
            g2.fillRect(drawX, drawY - 20, barWidth, 6);
            g2.setColor(Color.green);
            double hpPercentage = (double)bp.hp / bp.maxHp;
            int hpWidth = (int)(barWidth * hpPercentage);
            g2.fillRect(drawX, drawY - 20, hpWidth, 6);
            
            g2.setColor(Color.white);
            g2.setFont(new Font("Arial", Font.BOLD, 8));
            String hpText = bp.hp + "/" + bp.maxHp;
            int textX = drawX + (barWidth/2) - 15;
            g2.drawString(hpText, textX, drawY - 22);
            
            // MP Bar
            g2.setColor(Color.blue);
            g2.fillRect(drawX, drawY - 12, barWidth, 6);
            g2.setColor(Color.cyan);
            double mpPercentage = (double)bp.mp / bp.maxMp;
            int mpWidth = (int)(barWidth * mpPercentage);
            g2.fillRect(drawX, drawY - 12, mpWidth, 6);
            
            g2.setColor(Color.black);
            g2.setFont(new Font("Arial", Font.BOLD, 7));
            String mpText = bp.mp + "/" + bp.maxMp;
            g2.drawString(mpText, textX + 5, drawY - 6);
        }
        
        // Μην εμφανίζεις το μενού κατά τη διάρκεια του transition
        if (!battleEntering) {
            // Μενού επιλογών
            int menuX = 0;
            int menuY = screenHeight - tileSize * 3;
            int menuWidth = screenWidth;
            int menuHeight = tileSize * 3;
            
            drawPanel(g2, menuX, menuY, menuWidth, menuHeight, new Color(0, 0, 0, 220), Color.white);
            
            g2.setFont(maruMonicaBold);
            int optionSpacing = menuWidth / 4;
            
            for (int i = 0; i < battleOptions.length; i++) {
                int optionX = menuX + 30 + (i * optionSpacing);
                
                if (i == battleOption) {
                    g2.setColor(Color.yellow);
                    g2.drawString("▶ " + battleOptions[i], optionX - 15, menuY + 40);
                } else {
                    g2.setColor(Color.white);
                    g2.drawString(battleOptions[i], optionX, menuY + 40);
                }
            }
            
            // Μήνυμα μάχης
            int msgX = screenWidth/2 - 250;
            int msgY = menuY - 50;
            int msgWidth = 500;
            int msgHeight = 40;
            
            drawPanel(g2, msgX, msgY, msgWidth, msgHeight, new Color(0, 0, 0, 180), Color.white);
            g2.setFont(maruMonica);
            g2.setColor(Color.white);
            g2.drawString(currentDialogue, msgX + 20, msgY + 25);
        }
        
        // ========== ΠΡΟΣΘΕΣΕ ΤΟ FADE EFFECT ==========
        if (battleFadeOut || battleFadeIn) {
            g2.setColor(new Color(0, 0, 0, battleFadeAlpha));
            g2.fillRect(0, 0, screenWidth, screenHeight);
        }
    }
    
    public void drawDialogueScreen(Graphics2D g2) {
        int x = tileSize * 2;
        int y = tileSize * 5;
        int width = screenWidth - (tileSize * 4);
        int height = (gameState == dialogueOptionsState) ? tileSize * 7 : tileSize * 4;
        
        drawPanel(g2, x, y, width, height);
        
        g2.setFont(maruMonica);
        g2.setColor(Color.white);
        
        String[] lines = displayedDialogue.split("\n");
        int textY = y + 40;
        for (int i = 0; i < lines.length; i++) {
            g2.drawString(lines[i], x + 20, textY);
            textY += 30;
        }
        
        // Αν τελείωσε το γράψιμο, δείξε το μήνυμα για Enter
        if (!dialogueTyping && gameState == dialogueState) {
            g2.setFont(new Font("Arial", Font.ITALIC, 16));
            g2.drawString("Πάτα Enter για συνέχεια...", x + width - 220, y + height - 20);
        } else if (gameState == dialogueOptionsState) {
            int optionY = textY + 10;
            for (int i = 0; i < dialogueOptions.length; i++) {
                if (dialogueOptions[i] != null) {
                    if (i == selectedOption) {
                        g2.setColor(Color.yellow);
                        g2.fillRect(x + 15, optionY - 18, width - 30, 28);
                        g2.setColor(Color.black);
                    } else {
                        g2.setColor(Color.white);
                    }
                    g2.setFont(new Font("Arial", Font.PLAIN, 18));
                    g2.drawString(dialogueOptions[i], x + 25, optionY);
                    optionY += 30;
                }
            }
            // ========== ΠΡΟΣΘΕΣΕ ΤΟ ΜΗΝΥΜΑ Enter ΚΑΙ ΕΔΩ ==========
            if (!dialogueTyping) {
                g2.setFont(new Font("Arial", Font.ITALIC, 16));
                g2.drawString("Πάτα Enter για συνέχεια...", x + width - 220, y + height - 20);
            }
        } else {
            if (!dialogueTyping) {
            g2.setFont(new Font("Arial", Font.ITALIC, 16));
            g2.drawString("Πάτα Enter για συνέχεια...", x + width - 220, y + height - 20);
        }
        }
    }

    public void drawInventory(Graphics2D g2) {
        // Σκούρο φόντο
        g2.setColor(new Color(0, 0, 0, 220));
        g2.fillRect(0, 0, screenWidth, screenHeight);
        
        int slotSize = 60;
        int slotSpacingX = 7;  // Οριζόντιο spacing = 8
        int slotSpacingY = 20; // Κάθετο spacing = 15
        
        // ========== ΑΡΙΣΤΕΡΑ: STORAGE (4x2) ΜΕ ΕΙΚΟΝΑ ==========
        int storageX = 200;
        int storageY = 50;
        
        // Φόρτωσε και ζωγράφισε την εικόνα για storage
        try {
            BufferedImage storageBg = ImageIO.read(new File("res/gui/storage_bg.png"));
            g2.drawImage(storageBg, storageX - 20, storageY - 30, 300, 200, null);
        } catch (Exception e) {
            // Αν δεν υπάρχει εικόνα, ζωγράφισε απλό πλαίσιο
            g2.setColor(new Color(50, 50, 50, 200));
            g2.fillRoundRect(storageX - 10, storageY - 10, 4 * (slotSize + slotSpacingX) - slotSpacingX + 20, 
                            2 * (slotSize + slotSpacingY) - slotSpacingY + 20, 15, 15);
        }
        
        // Τίτλος
        g2.setColor(Color.white);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString("STORAGE", storageX + 20, storageY - 15);
        
        // Ζωγράφισε τα 8 slots (4x2)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                int index = row * 4 + col;
                int slotX = storageX + col * (slotSize + slotSpacingX);
                int slotY = storageY + row * (slotSize + slotSpacingY);

                
                // Αν υπάρχει item
                if (index < inventory.storage.length && inventory.storage[index] != null) {
                    Item item = inventory.storage[index];
                    
                    // Ζωγράφισε εικόνα item
                    if (item.image != null) {
                        g2.drawImage(item.image, slotX + 5, slotY + 5, slotSize - 10, slotSize - 10, null);
                    }
                    
                    // Ποσότητα (αν stackable)
                    if (item.stackable && item.amount > 1) {
                        g2.setColor(Color.white);
                        g2.setFont(new Font("Arial", Font.BOLD, 10));
                        g2.drawString("x" + item.amount, slotX + 40, slotY + 50);
                    }
                }
                
                // ========== ΝΕΟ HIGHLIGHT: Κίτρινο περίγραμμα ==========
                if (inventory.inventoryMode == 0 && index == inventory.selectedStorageSlot) {
                    g2.setColor(Color.yellow);
                    g2.setStroke(new BasicStroke(4)); // Πιο χοντρό περίγραμμα
                    g2.drawRoundRect(slotX - 2, slotY - 2, slotSize + 2, slotSize +2, 10, 10);
                }

                // ========== TOOLTIP ΓΙΑ ΤΟ ΕΠΙΛΕΓΜΕΝΟ ITEM ==========
                Item hoveredItem = null;

                if (inventory.inventoryMode == 0) { // Storage mode
                    if (inventory.selectedStorageSlot >= 0 && 
                        inventory.selectedStorageSlot < inventory.storage.length) {
                        hoveredItem = inventory.storage[inventory.selectedStorageSlot];
                    }
                } else { // Equipment mode
                    hoveredItem = inventory.getEquipSlot(inventory.selectedEquipSlot);
                }

                if (hoveredItem != null) {
                    drawItemTooltip(g2, hoveredItem);
                }
            }
        }
        
        // ========== ΔΕΞΙΑ: EQUIPMENT (3x3) ΜΕ ΕΙΚΟΝΑ ==========
        int equipX = 500;
        int equipY = 50;
        int slotSizeEquip = 55;
        int slotSpacingEquipX = 17;  // Οριζόντιο spacing = 8
        int slotSpacingEquipY = 17; // Κάθετο spacing = 15
        
        // Φόρτωσε και ζωγράφισε την εικόνα για equipment
        try {
            BufferedImage equipBg = ImageIO.read(new File("res/gui/equipment_bg.png"));
            g2.drawImage(equipBg, equipX - 20, equipY - 30, 250, 250, null);
        } catch (Exception e) {
            // Αν δεν υπάρχει εικόνα, ζωγράφισε απλό πλαίσιο
            g2.setColor(new Color(50, 50, 50, 200));
            g2.fillRoundRect(equipX - 10, equipY - 10, 3 * (slotSizeEquip + slotSpacingEquipX) + 10, 
                            3 * (slotSizeEquip + slotSpacingEquipY) + 10, 15, 15);
        }
        
        // Τίτλος
        g2.setColor(Color.white);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString("EQUIPMENT", equipX + 20, equipY - 15);
        
        // Ζωγράφισε τα 9 slots (3x3)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                int slotX = equipX + col * (slotSizeEquip + slotSpacingEquipX) + 6; // +2 δεξιά
                int slotY = equipY + row * (slotSizeEquip + slotSpacingEquipY) - 3; // -2 πάνω
                
                // Αν υπάρχει item
                Item item = inventory.getEquipSlot(index);
                if (item != null && item.image != null) {
                    g2.drawImage(item.image, slotX + 5, slotY + 5, slotSizeEquip - 10, slotSizeEquip - 10, null);
                }
                
                // ========== ΝΕΟ HIGHLIGHT: Κίτρινο περίγραμμα ==========
                if (inventory.inventoryMode == 1 && index == inventory.selectedEquipSlot) {
                    g2.setColor(Color.yellow);
                    g2.setStroke(new BasicStroke(4));
                    g2.drawRoundRect(slotX - 2, slotY - 2, slotSizeEquip + 2, slotSizeEquip + 2, 10, 10);
                }
            }
        }

        // ========== ΚΕΝΤΡΟ: KEY ITEMS (4x2) ==========
        int keyX = 450;  // Ανάμεσα στα stats και equipment
        int keyY = 350;

        // Φόρτωσε και ζωγράφισε την εικόνα για key items
        try {
            BufferedImage keyBg = ImageIO.read(new File("res/gui/storage_bg.png"));
            g2.drawImage(keyBg, keyX - 20, keyY - 30, 300, 200, null);
        } catch (Exception e) {
            // Fallback
            g2.setColor(new Color(100, 50, 50, 200));
            g2.fillRoundRect(keyX - 10, keyY - 10, 4 * (slotSize + slotSpacingX) - slotSpacingX + 20, 
                            2 * (slotSize + slotSpacingY) - slotSpacingY + 20, 15, 15);
        }

        // Τίτλος
        g2.setColor(Color.white);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString("KEY ITEMS", keyX + 20, keyY - 15);

        // Ζωγράφισε τα 8 slots (4x2)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                int index = row * 4 + col;
                int slotX = keyX + col * (slotSize + slotSpacingX);
                int slotY = keyY + row * (slotSize + slotSpacingY);
                
                // Αν υπάρχει item
                if (index < inventory.keyItems.length && inventory.keyItems[index] != null) {
                    Item item = inventory.keyItems[index];
                    if (item.image != null) {
                        g2.drawImage(item.image, slotX + 5, slotY + 5, slotSize - 10, slotSize - 10, null);
                    }
                }
                
                // Highlight
                if (inventory.inventoryMode == 2 && index == inventory.selectedKeyItemSlot) {
                    g2.setColor(Color.yellow);
                    g2.setStroke(new BasicStroke(4));
                    g2.drawRoundRect(slotX - 2, slotY - 2, slotSize + 2, slotSize +2, 10, 10);
                }
            }
        }
        
        // ========== ΗΡΩΑΣ ΜΕ ΚΑΡΔΙΕΣ ==========
        int heroX = 50;
        int heroY = 50;
        
        // Εικόνα ήρωα
        g2.drawImage(playerDown1, heroX, heroY, tileSize*2, tileSize*2, null);
        
        // 3 καρδιές για HP
        int heartX = heroX - 10;
        int heartY = heroY + 150;
        int heartSize = 25;
        
        try {
            BufferedImage fullHeart = ImageIO.read(new File("res/gui/heart_full.png"));
            BufferedImage emptyHeart = ImageIO.read(new File("res/gui/heart_empty.png"));
            BufferedImage halfHeart = ImageIO.read(new File("res/gui/heart_half.png"));
            
            // Υπολόγισε πόσες καρδιές (κάθε καρδιά = 10 HP)
            int totalHearts = 3;
            int hpPerHeart = player.maxHp / totalHearts;
            
            for (int i = 0; i < totalHearts; i++) {
                int heartHp = player.hp - (i * hpPerHeart);
                if (heartHp >= hpPerHeart) {
                    g2.drawImage(fullHeart, heartX + i * (heartSize + 5), heartY, heartSize, heartSize, null);
                } else if (heartHp > 0) {
                    g2.drawImage(halfHeart, heartX + i * (heartSize + 5), heartY, heartSize, heartSize, null);
                } else {
                    g2.drawImage(emptyHeart, heartX + i * (heartSize + 5), heartY, heartSize, heartSize, null);
                }
            }
        } catch (Exception e) {
            // Αν δεν υπάρχουν εικόνες, ζωγράφισε απλά κυκλάκια
            g2.setColor(Color.red);
            for (int i = 0; i < 3; i++) {
                g2.fillOval(heartX + i * 30, heartY, 20, 20);
            }
        }
        
        // 3 ασπίδες για DEFENCE (πάνω από τις καρδιές)
        int shieldX = heroX - 10;
        int shieldY = heartY - 40;
        
        try {
            BufferedImage shieldFull = ImageIO.read(new File("res/gui/shield_full.png"));
            BufferedImage shieldEmpty = ImageIO.read(new File("res/gui/shield_empty.png"));
            
            // Υπολόγισε πόσες ασπίδες (κάθε ασπίδα = 5 defense)
            int totalShields = 3;
            int defPerShield = 5;
            int playerDefense = player.defense;
            
            for (int i = 0; i < totalShields; i++) {
                if (playerDefense >= (i + 1) * defPerShield) {
                    g2.drawImage(shieldFull, shieldX + i * (heartSize + 5), shieldY, heartSize, heartSize, null);
                } else {
                    g2.drawImage(shieldEmpty, shieldX + i * (heartSize + 5), shieldY, heartSize, heartSize, null);
                }
            }
        } catch (Exception e) {
            // Αν δεν υπάρχουν εικόνες, ζωγράφισε απλά τετράγωνα
            g2.setColor(Color.blue);
            for (int i = 0; i < 3; i++) {
                g2.fillRect(shieldX + i * 30, shieldY, 20, 20);
            }
        }
        
        // ========== STATS ΜΕ ΧΡΩΜΑΤΑ ==========
        int statsX = heroX - 10;
        int statsY = heartY + 60;

        g2.setColor(new Color(50, 50, 50, 200));
        g2.fillRoundRect(statsX - 20, statsY - 20, 150, 250, 15, 15);
        g2.setColor(Color.white);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(statsX - 20, statsY - 20, 150, 250, 15, 15);

        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString("STATS", statsX + 25, statsY);

        g2.setFont(new Font("Arial", Font.PLAIN, 14));
        int statLineY = statsY + 25;

        // Level (πάντα λευκό)
        g2.setColor(Color.white);
        g2.drawString("Level: " + player.level, statsX, statLineY);

        // Attack - Πράσινο αν αυξημένο
        int attackBonus = player.attack - player.baseAttack;
        if (attackBonus > 0) {
            g2.setColor(Color.green);
            g2.drawString("Attack: " + player.attack + " (+" + attackBonus + ")", statsX, statLineY + 20);
        } else {
            g2.setColor(Color.white);
            g2.drawString("Attack: " + player.attack, statsX, statLineY + 20);
        }

        // Defense - Πράσινο αν αυξημένο
        int defenseBonus = player.defense - player.baseDefense;
        if (defenseBonus > 0) {
            g2.setColor(Color.green);
            g2.drawString("Defense: " + player.defense + " (+" + defenseBonus + ")", statsX, statLineY + 40);
        } else {
            g2.setColor(Color.white);
            g2.drawString("Defense: " + player.defense, statsX, statLineY + 40);
        }

        // HP - Κόκκινο (πάντα)
        g2.setColor(Color.red);
        g2.drawString("HP: " + player.hp + "/" + player.maxHp, statsX, statLineY + 60);

        // MP - Μπλε (πάντα)
        g2.setColor(Color.cyan);
        g2.drawString("MP: " + player.mp + "/" + player.maxMp, statsX, statLineY + 80);

        // Magic Attack - Πράσινο αν αυξημένο
        int magicBonus = player.magicAttack - player.baseMagicAttack;
        if (magicBonus > 0) {
            g2.setColor(Color.green);
            g2.drawString("Magic: " + player.magicAttack + " (+" + magicBonus + ")", statsX, statLineY + 100);
        } else {
            g2.setColor(Color.white);
            g2.drawString("Magic: " + player.magicAttack, statsX, statLineY + 100);
        }

        // Speed - Πράσινο αν αυξημένο
        int speedBonus = player.speed_stat - player.baseSpeed;
        if (speedBonus > 0) {
            g2.setColor(Color.green);
            g2.drawString("Speed: " + player.speed_stat + " (+" + speedBonus + ")", statsX, statLineY + 120);
        } else {
            g2.setColor(Color.white);
            g2.drawString("Speed: " + player.speed_stat, statsX, statLineY + 120);
        }

        // EXP - Κίτρινο
        g2.setColor(Color.yellow);
        g2.drawString("EXP: " + player.exp + "/" + player.expToNextLevel, statsX, statLineY + 140);

        // Gold - Χρυσό
        g2.setColor(new Color(255, 215, 0)); // Gold color
        g2.drawString("Gold: " + player.gold, statsX, statLineY + 160);
        
        // ========== ΟΔΗΓΙΕΣ ==========
        g2.setColor(Color.gray);
        g2.setFont(new Font("Arial", Font.ITALIC, 12));
        g2.drawString("SHIFT: Switch between Storage/Equipment", 50, screenHeight - 30);
        g2.drawString("Arrows: Move | Enter: Use/Equip | I: Close", 50, screenHeight - 15);
    }

    public void drawItemTooltip(Graphics2D g2, Item item) {
        int tooltipX = 200;
        int tooltipY = 250;
        int tooltipWidth = 200;
        int tooltipHeight = 120;
        
        drawPanel(g2, tooltipX, tooltipY, tooltipWidth, tooltipHeight);
        
        // ονομα item
        g2.setFont(maruMonicaBold);
        g2.setColor(Color.yellow);
        g2.drawString(item.name, tooltipX + 15, tooltipY + 25);
        
        // Γραμμή διαχωρισμού
        g2.setColor(Color.gray);
        g2.drawLine(tooltipX + 10, tooltipY + 35, tooltipX + tooltipWidth - 10, tooltipY + 35);
        
        // Stats
        g2.setFont(new Font("Arial", Font.PLAIN, 14));
        int statsY = tooltipY + 55;
        int lineHeight = 20;
        
        if (item.healAmount > 0) {
            g2.setColor(Color.green);
            g2.drawString("Heals: " + item.healAmount + " HP", tooltipX + 15, statsY);
            statsY += lineHeight;
        }
        
        if (item.attackBonus > 0) {
            g2.setColor(Color.red);
            g2.drawString("Attack +" + item.attackBonus, tooltipX + 15, statsY);
            statsY += lineHeight;
        }
        
        if (item.defenseBonus > 0) {
            g2.setColor(Color.blue);
            g2.drawString("Defense +" + item.defenseBonus, tooltipX + 15, statsY);
            statsY += lineHeight;
        }
        
        if (item.magicBonus > 0) {
            g2.setColor(Color.cyan);
            g2.drawString("Magic +" + item.magicBonus, tooltipX + 15, statsY);
            statsY += lineHeight;
        }
        
        if (item.hpBonus > 0) {
            g2.setColor(Color.pink);
            g2.drawString("Max HP +" + item.hpBonus, tooltipX + 15, statsY);
            statsY += lineHeight;
        }
        
        if (item.mpBonus > 0) {
            g2.setColor(Color.magenta);
            g2.drawString("Max MP +" + item.mpBonus, tooltipX + 15, statsY);
            statsY += lineHeight;
        }
        
        if (item.speedBonus > 0) {
            g2.setColor(Color.orange);
            g2.drawString("Speed +" + item.speedBonus, tooltipX + 15, statsY);
            statsY += lineHeight;
        }
        
        // Αν δεν έχει stats
        if (item.healAmount == 0 && item.attackBonus == 0 && item.defenseBonus == 0 &&
            item.magicBonus == 0 && item.hpBonus == 0 && item.mpBonus == 0 && item.speedBonus == 0) {
            g2.setColor(Color.lightGray);
            g2.drawString("No special effects", tooltipX + 15, statsY);
        }
    }

    public void drawMapScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 220));
        g2.fillRect(0, 0, screenWidth, screenHeight);
        
        // ========== ΧΑΡΤΗΣ ==========
        // Υπολόγισε το μέγεθος κάθε tile στο minimap
        int mapWidth = screenWidth - 50;
        int mapHeight = screenHeight - 50;
        
        int tileSizeMap = Math.min(mapWidth / maxWorldCol, mapHeight / maxWorldRow);
        tileSizeMap = Math.max(tileSizeMap, 4); // Μίνιμουμ 4 pixels
        
        // Κεντράρισμα του χάρτη
        int totalMapWidth = maxWorldCol * tileSizeMap;
        int totalMapHeight = maxWorldRow * tileSizeMap;
        int mapX = (screenWidth - totalMapWidth) / 2;
        int mapY = (screenHeight - totalMapHeight - 50) / 2;
        
        // Ζωγράφισε τον κόσμο (tiles) - ΧΡΗΣΙΜΟΠΟΙΩΝΤΑΣ ΤΙΣ ΠΡΑΓΜΑΤΙΚΕΣ ΕΙΚΟΝΕΣ
        for (int row = 0; row < maxWorldRow; row++) {
            for (int col = 0; col < maxWorldCol; col++) {
                int tileNum = tileM.mapTileNum[currentMap][row][col];
                
                // Πάρε την εικόνα του tile από τον TileManager
                BufferedImage tileImage = tileM.tile[tileNum].image;
                
                if (tileImage != null) {
                    int x = mapX + col * tileSizeMap;
                    int y = mapY + row * tileSizeMap;
                    
                    // Ζωγράφισε το tile με scale στο μέγεθος του minimap
                    g2.drawImage(tileImage, x, y, tileSizeMap, tileSizeMap, null);
                } else {
                    // Fallback αν δεν υπάρχει εικόνα
                    g2.setColor(Color.darkGray);
                    g2.fillRect(mapX + col * tileSizeMap, mapY + row * tileSizeMap, tileSizeMap, tileSizeMap);
                }
            }
        }
        
        // ========== ΠΑΙΚΤΗΣ (με εικόνα) ==========
        int playerCol = player.worldX / tileSize;
        int playerRow = player.worldY / tileSize;
        int playerMapX = mapX + playerCol * tileSizeMap;
        int playerMapY = mapY + playerRow * tileSizeMap;
        
        // Ζωγράφισε την εικόνα του παίκτη
        if (playerDown1 != null) {
            g2.drawImage(playerDown1, playerMapX, playerMapY, tileSizeMap, tileSizeMap, null);
        } else {
            // Fallback
            g2.setColor(Color.red);
            g2.fillOval(playerMapX, playerMapY, tileSizeMap, tileSizeMap);
        }

        // ========== NPCs (προαιρετικά) ==========
        ArrayList<Entity> currentMapNPCs = npcs.get(currentMap);
        for (Entity npc : currentMapNPCs) {
            int npcCol = npc.worldX / tileSize;
            int npcRow = npc.worldY / tileSize;
            int npcMapX = mapX + npcCol * tileSizeMap;
            int npcMapY = mapY + npcRow * tileSizeMap;
            

            if (npc.currentImage != null) {
                g2.drawImage(npc.down1, npcMapX, npcMapY, tileSizeMap, tileSizeMap, null);
            } else {
                // Fallback
                g2.setColor(Color.cyan);
                g2.fillRect(npcMapX, npcMapY, tileSizeMap, tileSizeMap);
            }
        }
        
        // ========== ΣΥΝΤΕΤΑΓΜΕΝΕΣ ==========
        int coordX = 10;
        int coordY = screenHeight - 80;
        
        g2.setFont(maruMonicaBold);
        g2.setColor(Color.yellow);
        g2.drawString("Συντεταγμένες:", coordX, coordY);
        
        g2.setFont(maruMonica);
        g2.setColor(Color.white);
        g2.drawString("Tile: (" + playerCol + ", " + playerRow + ")", 
                    coordX + 20, coordY + 25);
        g2.drawString("Pixel: (" + player.worldX + ", " + player.worldY + ")", 
                    coordX + 20, coordY + 45);
        
        // ========== ΠΛΗΡΟΦΟΡΙΕΣ ΧΑΡΤΗ ==========
        g2.setFont(maruMonicaSmall);
        g2.drawString("World: " + (currentMap == 0 ? "Overworld" : "Dungeon"), 
                    coordX, coordY + 70);
        
        // ========== ΟΔΗΓΙΕΣ ==========
        g2.setColor(Color.gray);
        g2.setFont(maruMonicaSmall);
        String exit = "Πάτα Μ για να κλείσεις";
        int x = getXforCenteredText(exit, g2);
        g2.drawString(exit, x, screenHeight - 30);
    }

    public void drawPauseScreen(Graphics2D g2) {
        // Ημιδιάφανο μαύρο φόντο
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, screenWidth, screenHeight);
        
        // Πλαίσιο μενού (ΧΡΗΣΙΜΟΠΟΙΩΝΤΑΣ ΤΗ ΝΕΑ ΜΕΘΟΔΟ)
        int frameWidth = 500;
        int frameHeight = 450;
        int frameX = screenWidth/2 - frameWidth/2;
        int frameY = screenHeight/2 - frameHeight/2;
        
        drawPanel(g2, frameX, frameY, frameWidth, frameHeight);
        
        // Τίτλος με custom font
        g2.setFont(maruMonicaLarge);
        g2.setColor(Color.yellow);
        String title = "PAUSE MENU";
        int x = getXforCenteredText(title, g2);
        g2.drawString(title, x, frameY + 60);
        
        if (volumeSettingMode == 0) {
            // Κανονικές επιλογές μενού
            g2.setFont(maruMonicaBold);
            int startY = frameY + 130;
            int spacing = 45;
            
            for (int i = 0; i < pauseOptions.length; i++) {
                String option = pauseOptions[i];
                x = frameX + 80; // Σταθερή θέση για τις επιλογές
                
                if (i == 0) { // FULLSCREEN - με checkbox
                    if (isFullScreen) {
                        g2.drawString("[✓]", x + 120, startY + i * spacing);
                    } else {
                        g2.drawString("[ ]", x + 120, startY + i * spacing);
                    }
                } else if (i == 1 || i == 2) { // MUSIC/SOUND - με ποσοστό
                    int volume = (i == 1) ? musicVolume : soundVolume;
                    option += ": " + volume + "%";
                }
                
                if (i == pauseCommandNum) {
                    g2.setColor(Color.yellow);
                    g2.drawString(">", x - 50, startY + i * spacing);
                    g2.drawString(option, x, startY + i * spacing);
                } else {
                    g2.setColor(Color.white);
                    g2.drawString(option, x, startY + i * spacing);
                }
            }
        } else {
            // Volume controls
            g2.setFont(new Font("Arial", Font.BOLD, 32));
            g2.setColor(Color.cyan);
            String volumeType = (volumeSettingMode == 1) ? "MUSIC VOLUME" : "SOUND VOLUME";
            x = getXforCenteredText(volumeType, g2);
            g2.drawString(volumeType, x, frameY + 150);
            
            // Volume bar
            int barWidth = 300;
            int barHeight = 30;
            int barX = screenWidth/2 - barWidth/2;
            int barY = frameY + 220;
            
            // Background bar
            g2.setColor(Color.darkGray);
            g2.fillRect(barX, barY, barWidth, barHeight);
            
            // Volume level
            g2.setColor(Color.green);
            int volume = (volumeSettingMode == 1) ? musicVolume : soundVolume;
            int fillWidth = (int)(barWidth * (volume / 100.0));
            g2.fillRect(barX, barY, fillWidth, barHeight);
            
            // Border
            g2.setColor(Color.white);
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(barX, barY, barWidth, barHeight);
            
            // Percentage text
            g2.setFont(new Font("Arial", Font.BOLD, 24));
            g2.setColor(Color.white);
            String percentText = volume + "%";
            x = getXforCenteredText(percentText, g2);
            g2.drawString(percentText, x, barY + 50);
            
            // Instructions
            g2.setFont(new Font("Arial", Font.ITALIC, 18));
            g2.setColor(Color.gray);
            String instr = "←/→ to adjust, Enter/Esc to confirm";
            x = getXforCenteredText(instr, g2);
            g2.drawString(instr, x, frameY + 350);
        }
        
        // Οδηγία στο κάτω μέρος
        g2.setFont(maruMonicaSmall);
        g2.setColor(Color.gray);
        String exit = "Press ESC to return to game";
        x = getXforCenteredText(exit, g2);
        g2.drawString(exit, x, frameY + frameHeight - 30);
    }

    public void drawQuitConfirmScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, screenWidth, screenHeight);
        
        int frameWidth = 400;
        int frameHeight = 250;
        int frameX = screenWidth/2 - frameWidth/2;
        int frameY = screenHeight/2 - frameHeight/2;
        
        drawPanel(g2, frameX, frameY, frameWidth, frameHeight);
        
        g2.setFont(maruMonicaBold);
        g2.setColor(Color.yellow);
        String msg = "QUIT GAME?";
        int x = getXforCenteredText(msg, g2);
        g2.drawString(msg, x, frameY + 80);
        
        g2.setFont(maruMonica);
        g2.setColor(Color.white);
        String msg2 = "Are you sure you want to quit?";
        x = getXforCenteredText(msg2, g2);
        g2.drawString(msg2, x, frameY + 130);
        
        String[] options = {"YES", "NO"};
        int startX = frameX + 100;
        int optionY = frameY + 190;
        
        for (int i = 0; i < options.length; i++) {
            if (i == quitConfirmOption) {
                g2.setColor(Color.yellow);
                g2.drawString("> " + options[i], startX + i * 120, optionY);
            } else {
                g2.setColor(Color.white);
                g2.drawString(options[i], startX + i * 120 + 15, optionY);
            }
        }
    }

    public void drawShopScreen(Graphics2D g2) {
        int margin = tileSize; // Μόνο 48 pixels περιθώριο από κάθε πλευρά
        int frameX = margin;
        int frameY = margin;
        int frameWidth = screenWidth - (margin * 2);
        int frameHeight = screenHeight - (margin * 2);
        
        drawPanel(g2, frameX, frameY, frameWidth, frameHeight);
        
        // Τίτλος
        g2.setFont(maruMonicaLarge);
        g2.setColor(Color.yellow);
        String title = "MERCHANT SHOP";
        int x = getXforCenteredText(title, g2);
        g2.drawString(title, x, frameY + 60);
        
        // Χρυσά
        g2.setFont(maruMonicaBold);
        g2.setColor(new Color(255, 215, 0));
        String goldText = "Gold: " + player.gold;
        g2.drawString(goldText, frameX + frameWidth - 150, frameY + 40);
        
        // Πίνακας επιλογών (Buy/Sell/Exit) - VERTICAL now
        String[] shopOptions = {"BUY", "SELL", "EXIT"};
        int optionX = frameX + 50;
        int startY = frameY + 120;
        int spacing = 40;
        
        for (int i = 0; i < shopOptions.length; i++) {
            g2.setFont(maruMonicaBold);
            if (i == shopOption) {
                g2.setColor(Color.yellow);
                g2.drawString("> " + shopOptions[i], optionX, startY + i * spacing);
            } else {
                g2.setColor(Color.white);
                g2.drawString(shopOptions[i], optionX + 15, startY + i * spacing);
            }
        }
        
        // Υπόλοιπο περιεχόμενο ανάλογα με τη λειτουργία
        if (shopOption != 2) {
            if (shopMode == 0) { // BUY mode
                drawBuyScreen(g2, frameX, frameY, frameWidth, frameHeight);
            } else if (shopMode == 1) { // SELL mode
                drawSellScreen(g2, frameX, frameY, frameWidth, frameHeight);
            }
        } else {
            // Μήνυμα επιβεβαίωσης εξόδου (προαιρετικά)
            g2.setFont(maruMonica);
            g2.setColor(Color.white);
            String exitMsg = "Πάτα ENTER για έξοδο!";
            int exitX = getXforCenteredText(exitMsg, g2);
            g2.drawString(exitMsg, exitX, frameY + frameHeight/2);
        }
    }

    public void drawBuyScreen(Graphics2D g2, int frameX, int frameY, int frameWidth, int frameHeight) {
        int slotSize = 60;
        int startX = frameX + 150; // Μετακίνησε δεξιά για να αφήσεις χώρο για το μενού
        int startY = frameY + 150;
        int spacingX = 70;
        int spacingY = 80;
        
        for (int i = 0; i < merchantItems.size() && i < 8; i++) {
            int row = i / 4;
            int col = i % 4;
            int x = startX + col * spacingX;
            int y = startY + row * spacingY;
            
            Item item = merchantItems.get(i);
            
            // Slot
            g2.setColor(new Color(50, 50, 50, 200));
            g2.fillRoundRect(x, y, slotSize, slotSize, 10, 10);
            g2.setColor(Color.white);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(x, y, slotSize, slotSize, 10, 10);
            
            // Item image
            if (item.image != null) {
                g2.drawImage(item.image, x + 5, y + 5, slotSize - 10, slotSize - 10, null);
            }
            
            // Τιμή
            g2.setFont(maruMonicaSmall);
            g2.setColor(new Color(255, 215, 0));
            g2.drawString(item.price + "G", x + 5, y + slotSize + 15);
            
            // Highlight επιλεγμένου
            if (i == selectedShopItem && shopOption == 0) {
                g2.setColor(Color.yellow);
                g2.setStroke(new BasicStroke(4));
                g2.drawRoundRect(x - 2, y - 2, slotSize + 4, slotSize + 4, 12, 12);
            }
        }
        
        // Item details
        if (selectedShopItem >= 0 && selectedShopItem < merchantItems.size() && shopOption == 0) {
            Item selected = merchantItems.get(selectedShopItem);
            drawItemTooltip(g2, selected, frameX + frameWidth - 200, frameY + 150);
        }
        
        // Instructions
        g2.setFont(maruMonicaSmall);
        g2.setColor(Color.gray);
        g2.drawString("↑/↓: Menu | ←/→: Items | Enter: Buy | ESC: Back", 
                    frameX + 50, frameY + frameHeight - 30);
    }

    public void drawSellScreen(Graphics2D g2, int frameX, int frameY, int frameWidth, int frameHeight) {
        int slotSize = 60;
        int startX = frameX + 150; // Μετακίνησε δεξιά για να αφήσεις χώρο για το μενού
        int startY = frameY + 150;
        int spacingX = 70;
        int spacingY = 80;
        
        // Φιλτράρισμα items που μπορούν να πουληθούν (όχι key items)
        ArrayList<Item> sellableItems = new ArrayList<>();
        for (int i = 0; i < inventory.storage.length; i++) {
            if (inventory.storage[i] != null && !inventory.storage[i].isKeyItem) {
                sellableItems.add(inventory.storage[i]);
            }
        }
        
        for (int i = 0; i < sellableItems.size() && i < 8; i++) {
            int row = i / 4;
            int col = i % 4;
            int x = startX + col * spacingX;
            int y = startY + row * spacingY;
            
            Item item = sellableItems.get(i);
            
            // Slot
            g2.setColor(new Color(50, 50, 50, 200));
            g2.fillRoundRect(x, y, slotSize, slotSize, 10, 10);
            g2.setColor(Color.white);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(x, y, slotSize, slotSize, 10, 10);
            
            // Item image
            if (item.image != null) {
                g2.drawImage(item.image, x + 5, y + 5, slotSize - 10, slotSize - 10, null);
            }
            
            // Τιμή πώλησης (μισή τιμή)
            int sellPrice = item.price / 2;
            g2.setFont(maruMonicaSmall);
            g2.setColor(new Color(255, 215, 0));
            g2.drawString(sellPrice + "G", x + 5, y + slotSize + 15);
            
            // Ποσότητα αν stackable
            if (item.stackable && item.amount > 1) {
                g2.setColor(Color.white);
                g2.setFont(maruMonicaSmall);
                g2.drawString("x" + item.amount, x + slotSize - 20, y + slotSize - 5);
            }
            
            // Highlight επιλεγμένου
            if (i == selectedSellItem && shopOption == 1) {
                g2.setColor(Color.yellow);
                g2.setStroke(new BasicStroke(4));
                g2.drawRoundRect(x - 2, y - 2, slotSize + 4, slotSize + 4, 12, 12);
            }
        }
        
        if (selectedSellItem >= 0 && selectedSellItem < sellableItems.size() && shopOption == 1) {
            Item selected = sellableItems.get(selectedSellItem);
            drawItemTooltip(g2, selected, frameX + frameWidth - 200, frameY + 150);
        }
        
        g2.setFont(maruMonicaSmall);
        g2.setColor(Color.gray);
        g2.drawString("↑/↓: Menu | ←/→: Items | Enter: Sell | ESC: Back", 
                    frameX + 50, frameY + frameHeight - 30);
    }

    public void drawItemTooltip(Graphics2D g2, Item item, int x, int y) {
        int width = 200;
        int height = 150;
        
        drawPanel(g2, x, y, width, height);
        
        g2.setFont(maruMonicaBold);
        g2.setColor(Color.yellow);
        g2.drawString(item.name, x + 15, y + 25);
        
        g2.setFont(maruMonicaSmall);
        g2.setColor(Color.white);
        int textY = y + 50;
        
        if (item.healAmount > 0) {
            g2.drawString("Heals: " + item.healAmount + " HP", x + 15, textY);
            textY += 20;
        }
        if (item.attackBonus > 0) {
            g2.drawString("Attack +" + item.attackBonus, x + 15, textY);
            textY += 20;
        }
        if (item.defenseBonus > 0) {
            g2.drawString("Defense +" + item.defenseBonus, x + 15, textY);
            textY += 20;
        }
        if (item.mpBonus > 0) {
            g2.drawString("Mana +" + item.mpBonus, x + 15, textY);
            textY += 20;
        }
        
        g2.setColor(new Color(255, 215, 0));
        g2.drawString("Price: " + item.price + "G", x + 15, y + height - 20);
    }

    public void drawLighting(Graphics2D g2) {
        // ΑΝ ΕΙΜΑΣΤΕ ΣΕ INTERIOR (χάρτες 4,5) ΜΗΝ ΕΦΑΡΜΟΣΕΙΣ ΣΚΟΤΑΔΙ
        if (currentMap == 2 || currentMap == 4 || currentMap == 5) {
            return; // Βγες αμέσως, κανένα εφέ
        }
        // Αν είναι μέρα και δεν έχει αρχίσει ακόμα να σκοτεινιάζει
        if (currentMap == 1) {
            currentDarkness = 150.0f; // Μόνιμο σκοτάδι
            // Το φανάρι λειτουργεί κανονικά
        } else {
            // Στον overworld, κανονικό σύστημα μέρας/νύχτας
            if (currentDarkness <= 0.5f) return;
        }
        
        // Δημιούργησε ένα προσωρινό BufferedImage για το εφέ φωτισμού
        BufferedImage lightBuffer = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D lightG2 = (Graphics2D) lightBuffer.getGraphics();
        
        // Σχεδίασε μαύρο φόντο με την τρέχουσα διαφάνεια
        lightG2.setColor(new Color(0, 0, 0, (int)currentDarkness));
        lightG2.fillRect(0, 0, screenWidth, screenHeight);
        
        // Αν έχει φανάρι, δημιούργησε φωτεινή περιοχή γύρω από τον παίκτη
        if (hasLantern && currentDarkness > 30) {
            int playerScreenX = player.worldX - worldX;
            int playerScreenY = player.worldY - worldY;
            int centerX = playerScreenX + tileSize/2;
            int centerY = playerScreenY + tileSize/2;
            
            // Η ακτίνα εξαρτάται από το πόσο σκοτάδι έχει
            int radius = lanternRadius * tileSize;
            if (currentDarkness > 100) {
                radius = (int)(radius * 1.3f); // Πιο δυνατό φως σε πλήρες σκοτάδι
            }
            
            // Απαλό gradient - το κέντρο είναι εντελώς διάφανο
            float[] dist = {0.0f, 0.3f, 0.7f, 1.0f};
            Color[] colors = {
                new Color(255, 255, 255, 0),      // Εντελώς διάφανο στο κέντρο
                new Color(0, 0, 0, 20),            // Ελάχιστο σκοτάδι
                new Color(0, 0, 0, (int)(currentDarkness * 0.5f)), // Μέτριο σκοτάδι
                new Color(0, 0, 0, (int)currentDarkness) // Κανονικό σκοτάδι στην άκρη
            };
            
            RadialGradientPaint gradient = new RadialGradientPaint(
                centerX, centerY, radius,
                dist, colors
            );
            
            lightG2.setPaint(gradient);
            lightG2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            lightG2.fillRect(0, 0, screenWidth, screenHeight);
        }
        
        lightG2.dispose();
        
        // Σχεδίασε το εφέ πάνω στο παιχνίδι
        g2.drawImage(lightBuffer, 0, 0, null);
    }

    class KeyHandler implements KeyListener {
        public boolean upPressed, downPressed, leftPressed, rightPressed;
        public boolean enterPressed = false; // Νέο πλήκτρο για αλληλεπίδραση (π.χ. με NPC)
        public boolean iPressed = false; // Για inventory
        public boolean ePressed = false; // Για items
        public boolean qPressed = false;
        public boolean shiftPressed = false;
        public boolean mPressed = false;
        public boolean escapePressed = false;

        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();
            
            if (code == KeyEvent.VK_ESCAPE) {
                escapePressed = true;
            }
            if (code == KeyEvent.VK_UP) {
                upPressed = true;
            }
            if (code == KeyEvent.VK_DOWN) {
                downPressed = true;
            }
            if (code == KeyEvent.VK_LEFT) {
                leftPressed = true;
            }
            if (code == KeyEvent.VK_RIGHT) {
                rightPressed = true;
            }
            if (code == KeyEvent.VK_ENTER) {
                enterPressed = true;
            }
            if (code == KeyEvent.VK_I) {
                iPressed = true;
            }
            if (code == KeyEvent.VK_E) {
                ePressed = true;
            }
            if (code == KeyEvent.VK_Q) {
                qPressed = true;
            }
            if (code == KeyEvent.VK_SHIFT) {
                shiftPressed = true;
            }
            if (code == KeyEvent.VK_M) {
                mPressed = true;
            }    
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int code = e.getKeyCode();
            
            if (code == KeyEvent.VK_ESCAPE) {
                escapePressed = false;
            }
            if (code == KeyEvent.VK_UP) {
                upPressed = false;
            }
            if (code == KeyEvent.VK_DOWN) {
                downPressed = false;
            }
            if (code == KeyEvent.VK_LEFT) {
                leftPressed = false;
            }
            if (code == KeyEvent.VK_RIGHT) {
                rightPressed = false;
            }
            if (code == KeyEvent.VK_ENTER) {
                enterPressed = false;
            }
            if (code == KeyEvent.VK_I) {
                iPressed = false;
            } 
            if (code == KeyEvent.VK_E) {
                ePressed = false;
            }
            if (code == KeyEvent.VK_Q) {
                qPressed = false;
            }
            if (code == KeyEvent.VK_SHIFT) {
                shiftPressed = false;
            }
            if (code == KeyEvent.VK_M) {
                mPressed = false;
            }
        }
    }
}