import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Locale;

public class Main {

    public static void main(String[] args) throws URISyntaxException, SQLException {
        Locale.setDefault(new Locale("ru"));

        ApiContextInitializer.init();
        TelegramBotsApi telegram = new TelegramBotsApi();

        Bot bot = new Bot();
        bot.setDb(new Database());
        bot.checkTime();

        try {
            telegram.registerBot(bot);
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
    }
}
