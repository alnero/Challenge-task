package com.journi.challenge.controllers;

import com.journi.challenge.CurrencyConverter;
import com.journi.challenge.models.ProductResponse;
import com.journi.challenge.repositories.ProductsRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ProductsController {

    @Inject
    private ProductsRepository productsRepository;
    @Inject
    private CurrencyConverter currencyConverter;

    @GetMapping("/products")
    public List<ProductResponse> list(@RequestParam(name = "countryCode", defaultValue = "AT") String countryCode) {
        String currencyCode = currencyConverter.getCurrencyForCountryCode(countryCode);
        return productsRepository.list().stream().map(product -> {
            Double eurPrice = product.getPrice();
            Double localCurrencyPrice = currencyConverter.convertEurToCurrency(currencyCode, eurPrice);
            return new ProductResponse(
                    product.getId(),
                    product.getDescription(),
                    localCurrencyPrice,
                    currencyCode
            );
        }).collect(Collectors.toList());
    }
}
