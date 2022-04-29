package com.journi.challenge.repositories;

import com.journi.challenge.models.Purchase;
import com.journi.challenge.models.PurchaseStats;
import javax.inject.Named;
import javax.inject.Singleton;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Named
@Singleton
public class PurchasesRepository {

    private final List<Purchase> allPurchases = new ArrayList<>();

    public List<Purchase> list() {
        return allPurchases;
    }

    public void save(Purchase purchase) {
        allPurchases.add(purchase);
    }

    public PurchaseStats getLast30DaysStats() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE.withZone(ZoneId.of("UTC"));

        LocalDateTime start = LocalDate.now().atStartOfDay().minusDays(30);

        List<Purchase> recentPurchases = allPurchases
                .stream()
                .filter(p -> p.getTimestamp().isAfter(start))
                .sorted(Comparator.comparing(Purchase::getTimestamp))
                .collect(Collectors.toList());

        DoubleSummaryStatistics amountStat =
                recentPurchases.stream().mapToDouble(Purchase::getTotalValue).summaryStatistics();
        LocalDateTime from = recentPurchases.isEmpty() ? start : recentPurchases.get(0).getTimestamp();
        LocalDateTime to = recentPurchases.isEmpty() ?
                LocalDate.now().atStartOfDay() :
                recentPurchases.get(recentPurchases.size() - 1).getTimestamp();

        return new PurchaseStats(
                formatter.format(from),
                formatter.format(to),
                amountStat.getCount(),
                amountStat.getSum(),
                amountStat.getAverage(),
                Double.isInfinite(amountStat.getMin()) ? 0 : amountStat.getMin(),
                Double.isInfinite(amountStat.getMax()) ? 0 : amountStat.getMax()
        );
    }
}
