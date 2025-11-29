package software.ulpgc.moneycalculator.application.mock;

import software.ulpgc.moneycalculator.architecture.control.Command;
import software.ulpgc.moneycalculator.architecture.control.ExchangeMoneyCommand;
import software.ulpgc.moneycalculator.architecture.model.Currency;
import software.ulpgc.moneycalculator.architecture.model.Money;
import software.ulpgc.moneycalculator.architecture.ui.CurrencyDialog;
import software.ulpgc.moneycalculator.architecture.ui.MoneyDialog;
import software.ulpgc.moneycalculator.architecture.ui.MoneyDisplay;

import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {
        List<Currency> currencies = new MockCurrencyLoader().loadAll();
        Command command = new ExchangeMoneyCommand(
                moneyDialog(currencies),
                currencyDialog(currencies),
                new MockExchangeRateLoader(),
                moneyDisplay()
        );
        command.execute();
    }

    private static MoneyDisplay moneyDisplay() {
        return System.out::println;
    }

    private static CurrencyDialog currencyDialog(List<Currency> currencies) {
        return () -> currencies.get(1);
    }

    private static MoneyDialog moneyDialog(List<Currency> currencies) {
        return () -> new Money(100, currencies.get(0));
    }
}
