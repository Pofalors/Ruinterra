public class ItemFactory {

    public static Item createById(String itemId) throws Exception {
        if (itemId == null || itemId.trim().isEmpty()) return null;

        itemId = itemId.trim().toLowerCase();

        if (itemId.equals("health_potion")) {
            Item item = new Item("Health Potion");
            item.stackable = true;
            item.healAmount = 20;
            item.price = 50;
            item.loadImage("res/items/health_potion.png");
            return item;
        }

        if (itemId.equals("mana_potion")) {
            Item item = new Item("Mana Potion");
            item.stackable = true;
            item.mpBonus = 20;
            item.price = 40;
            item.loadImage("res/items/potion_blue.png");
            return item;
        }

        if (itemId.equals("iron_sword")) {
            Item item = new Item("Iron Sword");
            item.attackBonus = 5;
            item.price = 200;
            item.loadImage("res/items/iron_sword.png");
            return item;
        }
        
        if (itemId.equals("dagger")) {
            Item item = new Item("Dagger");
            item.attackBonus = 6;
            item.price = 200;
            item.loadImage("res/items/dagger.png");
            return item;
        }

        if (itemId.equals("spear_01")) {
            Item item = new Item("Spear 01");
            item.attackBonus = 8;
            item.price = 200;
            item.loadImage("res/items/spear_01.png");
            return item;
        }

        if (itemId.equals("staff")) {
            Item item = new Item("Staff");
            item.magicBonus = 5;
            item.price = 200;
            item.loadImage("res/items/staff.png");
            return item;
        }

        if (itemId.equals("leather_armor")) {
            Item item = new Item("Leather Armor");
            item.defenseBonus = 3;
            item.price = 150;
            item.loadImage("res/items/leather_armor.png");
            return item;
        }

        if (itemId.equals("leather_boots")) {
            Item item = new Item("Leather Boots");
            item.defenseBonus = 1;
            item.price = 100;
            item.loadImage("res/items/leather_boots.png");
            return item;
        }

        if (itemId.equals("goblin_ear")) {
            Item item = new Item("Goblin Ear");
            item.stackable = true;
            item.loadImage("res/items/goblin_ear.png");
            return item;
        }

        if (itemId.equals("lantern")) {
            Item item = new Item("Lantern");
            item.isKeyItem = true;
            item.loadImage("res/items/lantern.png");
            return item;
        }

        if (itemId.equals("world_map")) {
            Item item = new Item("World Map");
            item.isKeyItem = true;
            item.loadImage("res/items/map.png");
            return item;
        }

        return null;
    }

    public static Item createBySaveName(String itemName) throws Exception {
        if (itemName == null || itemName.trim().isEmpty()) return null;

        itemName = itemName.trim();

        if (itemName.equalsIgnoreCase("Health Potion")) return createById("health_potion");
        if (itemName.equalsIgnoreCase("Mana Potion")) return createById("mana_potion");
        if (itemName.equalsIgnoreCase("Iron Sword")) return createById("iron_sword");
        if (itemName.equalsIgnoreCase("Dagger")) return createById("dagger");
        if (itemName.equalsIgnoreCase("Staff")) return createById("staff");
        if (itemName.equalsIgnoreCase("Spear 01")) return createById("spear_01");
        if (itemName.equalsIgnoreCase("Leather Armor")) return createById("leather_armor");
        if (itemName.equalsIgnoreCase("Leather Boots")) return createById("leather_boots");
        if (itemName.equalsIgnoreCase("Goblin Ear")) return createById("goblin_ear");
        if (itemName.equalsIgnoreCase("Lantern")) return createById("lantern");
        if (itemName.equalsIgnoreCase("World Map")) return createById("world_map");

        return null;
    }
}
