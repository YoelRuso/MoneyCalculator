package software.ulpgc.moneycalculator.application.mock;

import software.ulpgc.moneycalculator.architecture.io.CurrencyLoader;
import software.ulpgc.moneycalculator.architecture.model.Currency;

import java.util.List;

public class MockCurrencyLoader implements CurrencyLoader {
    @Override
    public List<Currency> loadAll() {
        return List.of(
                new Currency("USD", "USA"),
                new Currency("EUR", "Europa")
        );
    }
}
