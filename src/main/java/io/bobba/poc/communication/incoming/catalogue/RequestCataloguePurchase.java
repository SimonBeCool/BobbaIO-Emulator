package io.bobba.poc.communication.incoming.catalogue;

import io.bobba.poc.BobbaEnvironment;
import io.bobba.poc.communication.incoming.IIncomingEvent;
import io.bobba.poc.communication.protocol.ClientMessage;
import io.bobba.poc.core.catalogue.CatalogueItem;
import io.bobba.poc.core.catalogue.CataloguePage;
import io.bobba.poc.core.gameclients.GameClient;
import io.bobba.poc.core.users.User;

public class RequestCataloguePurchase implements IIncomingEvent {

    @Override
    public void handle(GameClient client, ClientMessage request) {
    	User user = client.getUser();
        if (user != null){
        	int pageId = request.popInt();
        	int itemId = request.popInt();
        	
        	CatalogueItem ishc = BobbaEnvironment.getGame().getCatalogue().getPage(pageId).getItem(itemId);
        	switch (ishc.getName()) {
            case "hc_gift_14days":  
            	int club_days = user.getHabboClub() + 14;
    			user.setClubDays(club_days);
            break;
            default:
            	BobbaEnvironment.getGame().getCatalogue().handlePurchase(user, pageId, itemId);
            break;
        }
      }
    }
}
