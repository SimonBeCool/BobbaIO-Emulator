package io.bobba.poc.communication.outgoing.catalogue;

import java.util.Comparator;
import java.util.List;

import io.bobba.poc.communication.protocol.ServerMessage;
import io.bobba.poc.communication.protocol.ServerOpCodes;
import io.bobba.poc.core.catalogue.CataloguePage;

public class CatalogueIndexComposer extends ServerMessage {

	public CatalogueIndexComposer(List<CataloguePage> pages, int rank) {
		super(ServerOpCodes.CATALOGUE_INDEX);

		// Sort pages
		pages.sort((p1, p2) -> {
			if (p1.getCaption().equals("Startseite")) {
				return -1;
			} else if (p2.getCaption().equals("Startseite")) {
				return 1;
			} else {
				return p1.getCaption().compareTo(p2.getCaption());
			}
		});

		appendInt(calculateTreeSize(pages, rank, -1));
		for (CataloguePage mainPage : pages) {
			if (mainPage.getParentId() == -1 && mainPage.getMinRank() <= rank) {
				serializePage(mainPage);
				appendInt(calculateTreeSize(pages, rank, mainPage.getId()));
				for (CataloguePage secondPage: pages) {
					if (secondPage.getParentId() == mainPage.getId() && secondPage.getMinRank() <= rank) {
						serializePage(secondPage);
						appendInt(0);
					}
				}
			}
		}
	}

	private void serializePage(CataloguePage page) {
		appendBoolean(page.isVisible());
		appendInt(page.getIconColor());
		appendInt(page.getIconId());
		appendInt(page.getId());
		appendString(page.getCaption());
	}

	private int calculateTreeSize(List<CataloguePage> pages, int rank, int treeId) {
		int i = 0;
		for (CataloguePage page : pages) {
			if (page.getMinRank() <= rank && page.getParentId() == treeId) {
				i++;
			}
		}
		return i;
	}
}
