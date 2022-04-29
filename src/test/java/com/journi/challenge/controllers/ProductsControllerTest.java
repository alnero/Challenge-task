package com.journi.challenge.controllers;

import com.journi.challenge.CurrencyConverter;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductsControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CurrencyConverter currencyConverter;

    @Test
    void shouldListProductsWithCurrencyCodeAndConvertedPriceDefault() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", IsEqual.equalTo(4)));
    }

    @Test
    void shouldListProductsWithCurrencyCodeAndConvertedPriceBR() throws Exception {
        mockMvc.perform(get("/products?countryCode=BR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", IsEqual.equalTo(4)));
    }

    @Test
    void shouldListProductsWithCurrencyCodeEURWhenCountryCodeNonSupported() throws Exception {
        mockMvc.perform(get("/products?countryCode=JP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", IsEqual.equalTo(4)))
                .andExpect(jsonPath("$[*].currencyCode", IsNot.not(IsEmptyCollection.empty())))
                .andExpect(jsonPath("$[0].currencyCode", IsEqual.equalTo("EUR")));
    }

    @Test
    void shouldListProductsWithCurrencyCodeAndConvertedCurrencyPrice() throws Exception {
        Set<String> supportedCountries = currencyConverter.getSupportedCountriesCurrency().keySet();
        for (String countryCode : supportedCountries) {
            String currencyCode = currencyConverter.getCurrencyForCountryCode(countryCode);
            mockMvc.perform(get("/products?countryCode=" + countryCode))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()", IsEqual.equalTo(4)))
                    .andExpect(jsonPath("$[*].currencyCode", IsNot.not(IsEmptyCollection.empty())))
                    .andExpect(jsonPath("$[0].currencyCode", IsEqual.equalTo(currencyCode)));
        }
    }
}
