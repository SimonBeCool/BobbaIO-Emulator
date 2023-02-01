package io.bobba.poc.communication.incoming.catalogue;

import io.bobba.poc.BobbaEnvironment;
import io.bobba.poc.communication.incoming.IIncomingEvent;
import io.bobba.poc.communication.outgoing.catalogue.CataloguePurchaseErrorComposer;
import io.bobba.poc.communication.outgoing.catalogue.CataloguePurchaseInformationComposer;
import io.bobba.poc.communication.protocol.ClientMessage;
import io.bobba.poc.core.catalogue.CatalogueItem;
import io.bobba.poc.core.catalogue.CataloguePage;
import io.bobba.poc.core.gameclients.GameClient;
import io.bobba.poc.core.users.User;

public class RequestCataloguePurchase implements IIncomingEvent {

    @Override
    public void handle(GameClient client, ClientMessage request) {
    	User user = client.getUser();
    	if (user != null) {
    	int pageId = request.popInt();
    	int itemId = request.popInt();
    	
    	CatalogueItem item = BobbaEnvironment.getGame().getCatalogue().getPage(pageId).getItem(itemId);
    	    switch (item.getName()) {
    	        case "hc_gift_14days":
    	            if (user.getCredits() < item.getCost()) {
    	                user.getClient().sendMessage(new CataloguePurchaseErrorComposer());
    	            } else {
    	                user.getClient().sendMessage(new CataloguePurchaseInformationComposer(item));
    	                user.setCredits(user.getCredits() - item.getCost());
    	                
    	                int clubDays = user.getHabboClub() + 14;
    	                user.setClubDays(clubDays);
    	            }
    	        break;
    	        case "hc_gift_31days":
    	            if (user.getCredits() < item.getCost()) {
    	                user.getClient().sendMessage(new CataloguePurchaseErrorComposer());
    	            } else {
    	                user.getClient().sendMessage(new CataloguePurchaseInformationComposer(item));
    	                user.setCredits(user.getCredits() - item.getCost());
    	                
    	                int clubDays = user.getHabboClub() + 31;
    	                user.setClubDays(clubDays);
    	            }
    	        break;
    	        default:
    	            BobbaEnvironment.getGame().getCatalogue().handlePurchase(user, pageId, itemId);
    	        break;
    	    }
    	}
    }
}
