import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Locale;

public class Main {

    public static void main(String[] args) throws URISyntaxException, SQLException, TelegramApiException {
        Locale.setDefault(new Locale("ru"));

        TelegramBotsApi telegram = new TelegramBotsApi(DefaultBotSession.class);

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
