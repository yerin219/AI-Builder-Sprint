package com.memorydrawer.memorydraft.api;

import java.time.LocalDate;
import java.util.List;

import com.memorydrawer.receipt.PurchaseItem;

public record ReceiptFrontCandidate(
	LocalDate memoryDate,
	String storeName,
	List<PurchaseItem> purchaseItems
) implements FrontCandidate {
	public ReceiptFrontCandidate {
		purchaseItems = purchaseItems == null ? List.of() : List.copyOf(purchaseItems);
	}
}
