package com.memorydrawer.card.query.dto;

import java.util.List;

public record DrawerListResponse(List<DrawerItem> drawers) {

	public DrawerListResponse {
		drawers = List.copyOf(drawers);
	}

	public record DrawerItem(int year, long cardCount) {
	}
}
