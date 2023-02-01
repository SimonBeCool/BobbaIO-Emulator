package io.bobba.poc.core.catalogue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bobba.poc.BobbaEnvironment;
import io.bobba.poc.communication.outgoing.catalogue.CatalogueIndexComposer;
import io.bobba.poc.communication.outgoing.catalogue.CataloguePageComposer;
import io.bobba.poc.communication.outgoing.catalogue.CataloguePurchaseErrorComposer;
import io.bobba.poc.communication.outgoing.catalogue.CataloguePurchaseInformationComposer;
import io.bobba.poc.core.items.BaseItem;
import io.bobba.poc.core.users.User;
import io.bobba.poc.threading.ThreadPooling;

public class Catalogue {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPooling.class);
	private Map<Integer, CataloguePage> pages;
	private static int itemIdGenerator = 0;

	public Catalogue() {
		this.pages = new LinkedHashMap<>();
	}
	public void loadFromDb() throws SQLException {
	    String selectPageSQL = "SELECT * FROM catalog_pages";
	    String selectItemSQL = "SELECT * FROM catalog_items WHERE page_id = ?";
	    try (Connection connection = BobbaEnvironment.getGame().getDatabase().getDataSource().getConnection();
	         PreparedStatement selectPageStatement = connection.prepareStatement(selectPageSQL);
	         PreparedStatement selectItemStatement = connection.prepareStatement(selectItemSQL)) {

	        ResultSet pageResultSet = selectPageStatement.executeQuery();
	        pages.clear();
	        while (pageResultSet.next()) {
	            int pageId = pageResultSet.getInt("id");
	            int parentId = pageResultSet.getInt("parent_id");
	            String caption = pageResultSet.getString("caption");
	            int iconColor = pageResultSet.getInt("icon_color");
	            int iconImage = pageResultSet.getInt("icon_image");
	            boolean visible = pageResultSet.getString("visible").equals("1");
	            boolean enabled = pageResultSet.getString("enabled").equals("1");
	            int minRank = pageResultSet.getInt("min_rank");
	            String layout = pageResultSet.getString("page_layout");
	            String headline = pageResultSet.getString("page_headline");
	            String teaser = pageResultSet.getString("page_teaser");
	            String text1 = pageResultSet.getString("page_text1");
	            String text2 = pageResultSet.getString("page_text2");
	            String text3 = pageResultSet.getString("page_text_details");
	            String text4 = pageResultSet.getString("page_text_teaser");

	            List<CatalogueItem> items = new ArrayList<>();
	            selectItemStatement.setInt(1, pageId);
	            ResultSet itemResultSet = selectItemStatement.executeQuery();
	            while (itemResultSet.next()) {
	                int catalogItemId = itemResultSet.getInt("id");
	                String catalogName = itemResultSet.getString("catalog_name");
	                int baseId = itemResultSet.getInt("item_ids");
	                int cost = itemResultSet.getInt("cost_credits");

	                BaseItem base = BobbaEnvironment.getGame().getItemManager().getItem(baseId);
	                if (base != null) {
	                    items.add(new CatalogueItem(catalogItemId, pageId, base, catalogName, cost, 1));
	                } else {
	                    //System.out.println("null base: " + catalogName);
	                }
	            }

	            pages.put(pageId, new CataloguePage(pageId, parentId, caption, visible, enabled, minRank, iconColor,
	                    iconImage, layout, headline, teaser, text1, text2, text3, text4, items));
	        }
	        LOGGER.info("Catalog cache reloaded!");
	    } catch (SQLException e) {
	        throw e;
	    }
	}
	public void initialize() throws SQLException {
		loadFromDb();
	}
	
	public void re_initialize(User user) throws SQLException {
		loadFromDb();
		user.getClient().sendMessage(new CatalogueIndexComposer(new ArrayList<>(pages.values()), user.getRank()));
	}

	public CataloguePage getPage(int pageId) {
		return pages.getOrDefault(pageId, null);
	}

	public void serializeIndex(User user) {
		user.getClient().sendMessage(new CatalogueIndexComposer(new ArrayList<>(pages.values()), user.getRank()));
	}

	public void serializePage(User user, int pageId) {
		CataloguePage page = getPage(pageId);
		if (page != null && page.isEnabled() && page.isVisible() && page.getMinRank() <= user.getRank()) {
			user.getClient().sendMessage(new CataloguePageComposer(page));
		}
	}

	public void handlePurchase(User user, int pageId, int itemId) {
		CataloguePage page = getPage(pageId);
		if (page != null && page.isEnabled() && page.isVisible() && page.getMinRank() <= user.getRank()) {
			CatalogueItem item = page.getItem(itemId);
			if (item != null) {
					if (user.getCredits() < item.getCost()) {
						user.getClient().sendMessage(new CataloguePurchaseErrorComposer());
					} else {
						user.getClient().sendMessage(new CataloguePurchaseInformationComposer(item));
						user.setCredits(user.getCredits() - item.getCost());
						deliverItem(user, item.getBaseItem(), item.getAmount());
					}
			}
		}
	}

	public static int generateItemId() {
		return itemIdGenerator++;
	}

	private void deliverItem(User user, BaseItem item, int amount) {
		for (int i = 0; i < amount; i++) {
			switch (item.getInteractionType()) {
			default:
				user.getInventory().addItem(generateItemId(), item, 0);
			}
		}
	}

	public CatalogueItem findItem(String itemName) {
		for (CataloguePage page : new ArrayList<>(pages.values())) {
			for (CatalogueItem item : page.getItems()) {
				if (item.getName().toLowerCase().equals(itemName.toLowerCase())) {
					return item;
				}
			}
		}
		return null;
	}
}
