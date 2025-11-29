package software.ulpgc.moneycalculator.architecture.io;

import software.ulpgc.moneycalculator.architecture.model.Currency;

import java.util.List;

public interface CurrencyLoader {
    List<Currency> loadAll();
}
