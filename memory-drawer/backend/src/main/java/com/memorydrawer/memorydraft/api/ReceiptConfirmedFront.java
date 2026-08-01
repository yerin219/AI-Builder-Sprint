package com.memorydrawer.memorydraft.api;

import java.util.List;

import com.memorydrawer.receipt.PurchaseItem;

public record ReceiptConfirmedFront(
	String storeName,
	List<PurchaseItem> purchaseItems
) implements ConfirmedFront {
	public ReceiptConfirmedFront {
		purchaseItems = List.copyOf(purchaseItems);
	}
}
