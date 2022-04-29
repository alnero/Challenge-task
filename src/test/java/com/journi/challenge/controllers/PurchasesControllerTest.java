package com.journi.challenge.controllers;

import com.journi.challenge.CurrencyConverter;
import com.journi.challenge.models.Purchase;
import com.journi.challenge.models.PurchaseStats;
import com.journi.challenge.repositories.PurchasesRepository;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PurchasesControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PurchasesController purchasesController;
    @Autowired
    private PurchasesRepository purchasesRepository;
    @Autowired
    private CurrencyConverter currencyConverter;

    private String getPurchaseJson(String invoiceNumber, String customerName, String dateTime, Double totalValue, String currencyCode, String... productIds) {
        String productIdList = "[\"" + String.join("\",\"", productIds) + "\"]";
        return String.format(Locale.US,"{\"invoiceNumber\":\"%s\",\"customerName\":\"%s\",\"dateTime\":\"%s\",\"productIds\":%s,\"amount\":%.2f,\"currencyCode\":\"%s\"}", invoiceNumber, customerName, dateTime, productIdList, totalValue, currencyCode);
    }

    @Test
    void testPurchaseCurrencyCodeEUR() throws Exception {
        String body = getPurchaseJson("1", "customer 1", "2020-01-01T10:00:00+01:00", 25.34, "EUR", "product1");
        mockMvc.perform(post("/purchases")
                .contentType(MediaType.APPLICATION_JSON).content(body)
        ).andExpect(status().isOk());

        Purchase savedPurchase = purchasesRepository.list().get(purchasesRepository.list().size() - 1);
        assertEquals("customer 1", savedPurchase.getCustomerName());
        assertEquals("1", savedPurchase.getInvoiceNumber());
        assertEquals("2020-01-01T10:00:00", savedPurchase.getTimestamp().format(DateTimeFormatter.ISO_DATE_TIME));
        assertEquals(25.34, savedPurchase.getTotalValue());
    }

    @Test
    void testSupportedPurchaseCurrencyCodes() throws Exception {
        Set<String> supportedCountries = currencyConverter.getSupportedCountriesCurrency().keySet();
        for (String countryCode : supportedCountries) {
            String currencyCode = currencyConverter.getCurrencyForCountryCode(countryCode);
            String body = getPurchaseJson("2", "customer 2", "2020-02-02T20:00:00+01:00", 50.00, currencyCode, "product2");
            mockMvc.perform(post("/purchases")
                    .contentType(MediaType.APPLICATION_JSON).content(body)
            ).andExpect(status().isOk());

            Purchase savedPurchase = purchasesRepository.list().get(purchasesRepository.list().size() - 1);
            Double expectedValue = currencyConverter.convertCurrencyToEur(currencyCode, 50.00);
            assertEquals("customer 2", savedPurchase.getCustomerName());
            assertEquals("2", savedPurchase.getInvoiceNumber());
            assertEquals("2020-02-02T20:00:00", savedPurchase.getTimestamp().format(DateTimeFormatter.ISO_DATE_TIME));
            assertEquals(expectedValue, savedPurchase.getTotalValue());
        }
    }


    @Test
    void testPurchaseStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime firstDate = now.minusDays(20);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE.withZone(ZoneId.of("UTC"));
        // Inside window purchases
        purchasesRepository.save(new Purchase("1", firstDate, Collections.emptyList(), "", 10.0));
        purchasesRepository.save(new Purchase("1", firstDate.plusDays(1), Collections.emptyList(), "", 10.0));
        purchasesRepository.save(new Purchase("1", firstDate.plusDays(2), Collections.emptyList(), "", 10.0));
        purchasesRepository.save(new Purchase("1", firstDate.plusDays(3), Collections.emptyList(), "", 10.0));
        purchasesRepository.save(new Purchase("1", firstDate.plusDays(4), Collections.emptyList(), "", 10.0));
        purchasesRepository.save(new Purchase("1", firstDate.plusDays(5), Collections.emptyList(), "", 10.0));
        purchasesRepository.save(new Purchase("1", firstDate.plusDays(6), Collections.emptyList(), "", 10.0));
        purchasesRepository.save(new Purchase("1", firstDate.plusDays(7), Collections.emptyList(), "", 10.0));
        purchasesRepository.save(new Purchase("1", firstDate.plusDays(8), Collections.emptyList(), "", 10.0));
        purchasesRepository.save(new Purchase("1", firstDate.plusDays(9), Collections.emptyList(), "", 10.0));

        // Outside window purchases
        purchasesRepository.save(new Purchase("1", now.minusDays(31), Collections.emptyList(), "", 10.0));
        purchasesRepository.save(new Purchase("1", now.minusDays(31), Collections.emptyList(), "", 10.0));
        purchasesRepository.save(new Purchase("1", now.minusDays(32), Collections.emptyList(), "", 10.0));
        purchasesRepository.save(new Purchase("1", now.minusDays(33), Collections.emptyList(), "", 10.0));
        purchasesRepository.save(new Purchase("1", now.minusDays(34), Collections.emptyList(), "", 10.0));
        purchasesRepository.save(new Purchase("1", now.minusDays(35), Collections.emptyList(), "", 10.0));

        PurchaseStats purchaseStats = purchasesController.getStats();
        assertEquals(formatter.format(firstDate), purchaseStats.getFrom());
        assertEquals(formatter.format(firstDate.plusDays(9)), purchaseStats.getTo());
        assertEquals(10, purchaseStats.getCountPurchases());
        assertEquals(100.0, purchaseStats.getTotalAmount());
        assertEquals(10.0, purchaseStats.getAvgAmount());
        assertEquals(10.0, purchaseStats.getMinAmount());
        assertEquals(10.0, purchaseStats.getMaxAmount());
    }

    @Test
    void testOutsideWindowPurchaseStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusDays(30);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE.withZone(ZoneId.of("UTC"));

        // Outside window purchases
        purchasesRepository.save(new Purchase("1", now.minusDays(31), Collections.emptyList(), "", 10.0));

        PurchaseStats purchaseStats = purchasesController.getStats();
        assertEquals(formatter.format(from), purchaseStats.getFrom());
        assertEquals(formatter.format(now), purchaseStats.getTo());
        assertEquals(0, purchaseStats.getCountPurchases());
        assertEquals(0.0, purchaseStats.getTotalAmount());
        assertEquals(0.0, purchaseStats.getAvgAmount());
        assertEquals(0.0, purchaseStats.getMinAmount());
        assertEquals(0.0, purchaseStats.getMaxAmount());
    }
}
