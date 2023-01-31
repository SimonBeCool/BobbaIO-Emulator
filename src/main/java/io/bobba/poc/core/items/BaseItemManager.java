package io.bobba.poc.core.items;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.bobba.poc.BobbaEnvironment;

public class BaseItemManager {
	public static int baseItemId = 0;

	private Map<Integer, BaseItem> items;

	public BaseItemManager() {
		this.items = new HashMap<>();
	}

	private void loadFromDb() throws SQLException {
        String query = "SELECT * FROM furniture";

        try (
            Connection connection = BobbaEnvironment.getGame().getDatabase().getDataSource().getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query)
        ) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String itemName = resultSet.getString("item_name");
                String type = resultSet.getString("type");
                int x = resultSet.getInt("width");
                int y = resultSet.getInt("length");
                double z = resultSet.getDouble("stack_height");
                boolean canStack = resultSet.getString("can_stack").equals("1");
                boolean canSit = resultSet.getString("can_sit").equals("1");
                boolean canWalk = resultSet.getString("is_walkable").equals("1");
                int spriteId = resultSet.getInt("sprite_id");
                String interaction = resultSet.getString("interaction_type");
                int states = resultSet.getInt("interaction_modes_count");

                if (type.equals("s")) {
                    addRoomItem(id, spriteId, x, y, z, itemName, states, canStack, canWalk, canSit, Arrays.asList(0, 2, 4, 6), interaction);
                } else if (type.equals("i")) {
                    addWallItem(id, spriteId, itemName, states, interaction);
                }
            }
        }
    }

	public void initialize() throws SQLException {
		loadFromDb();
	}
	
	public void re_initialize() throws SQLException {
		loadFromDb();
	}

	public BaseItem addRoomItem(int id, int baseId, int x, int y, double z, String itemName, int states,
			boolean stackable, boolean walkable, boolean seat, List<Integer> directions, String interaction) {
		if (!items.containsKey(id)) {
			items.put(id, new BaseItem(id, ItemType.RoomItem, baseId, x, y, z, itemName, states, stackable, walkable,
					seat, directions, InteractionType.valueOf(interaction.toUpperCase())));
		}
		return items.get(id);
	}

	public BaseItem addWallItem(int id, int baseId, String itemName, int states, String interaction) {
		if (!items.containsKey(id)) {
			items.put(id, new BaseItem(id, ItemType.WallItem, baseId, 0, 0, 0, itemName, states, false, false, false,
					Arrays.asList(2, 4), InteractionType.valueOf(interaction.toUpperCase())));
		}
		return items.get(id);
	}

	public BaseItem getItem(int id) {
		return items.getOrDefault(id, null);
	}

	public List<BaseItem> getItems() {
		return new ArrayList<BaseItem>(items.values());
	}

	public BaseItem findItem(String itemName) {
		for (BaseItem item : items.values()) {
			if (itemName.toLowerCase().equals(item.getItemName().toLowerCase())) {
				return item;
			}
		}
		return null;
	}
	
	public BaseItem findItemByBaseId(int baseid) {
		for (BaseItem item : items.values()) {
			if (baseid == item.getBaseId()) {
				return item;
			}
		}
		return null;
	}
}
