package com.memorydrawer.memorydraft.extract;

import java.util.List;

import com.memorydrawer.receipt.PurchaseItem;

public record ExtractedFrontFields(
	String memoryDate,
	String storeName,
	List<PurchaseItem> purchaseItems,
	String eventName,
	String venue,
	String seat
) {
	public ExtractedFrontFields {
		purchaseItems = purchaseItems == null ? List.of() : List.copyOf(purchaseItems);
	}
}
