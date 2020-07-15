import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

public class Bot extends TelegramLongPollingBot {
    //TEST-BOT
    public final String BOT_USERNAME = "@scheduleSPbU_test_bot";
    public final String BOT_TOKEN = "1096924723:AAHGxadgGu2jsh1y54cli5LED1bGoVwvfl8";

//    //    Константы
//    public final String BOT_USERNAME = "@scheduleSPbU_bot";
//    public final String BOT_TOKEN = "1065822779:AAEq-5nqUR_g8P4UeVHQMo0lu8BkmvQZ-MI";

    private LocalDate todayIs;
    private String formattedTodayIs;
    //    id текущего чата
    private long chat_id;

    private LocalDate currentDate;
    //    Клавиатуры
    private ArrayList keyboard = new ArrayList<>();
    private ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
    private InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

    //      Адрес, динамически собирающийся
    private StringBuilder url = new StringBuilder();
    //    Объект расписания
    private Schedule schedule = new Schedule();
    //Список с расписаниями на каждый день с привязкой к дате
    private ArrayList<ScheduleWithDate> scheduleWithDateList = new ArrayList<>();
    //Список с программами подготовки
    private ArrayList<String> secondSpecs = new ArrayList<>();
    //Список с годами поступления
    private ArrayList<Element> years = new ArrayList<>();
    //Карта, хранящая пары "Группа - Ссылка"
    private HashMap<String, String> groupLink = new HashMap<>();
    //Список с уровнями обучения
    private ArrayList<String> studyLevelsList = new ArrayList<>();
    private String currentStudyLevel;

    private boolean isFinalUrl;
    private String finalURL;

    //    Метод, выполняющийся при получении сообщений
    @Override
    public void onUpdateReceived(Update update) {
        //Получаем текущую дату
        todayIs = LocalDate.now();
        formattedTodayIs = todayIs.format(DateTimeFormatter.ofPattern("dd MMMM"));

//    ID написавшего пользователя
        update.getUpdateId();
        //Пришел текст, или была нажата кнопка?
        if (update.hasMessage()) {
            chat_id = update.getMessage().getChatId();
            SendMessage sendMessage;
            sendMessage = getMessage(update.getMessage().getText());
            sendMessage.setChatId(chat_id);
            System.out.println("Пришел текст от пользователя " + update.getMessage().getChatId() + "\n"
                    + update.getMessage().getFrom().getUserName() +
                    "\n с содержимым " + update.getMessage().getText());

            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else if (update.hasCallbackQuery()) {
            String response = update.getCallbackQuery().getData();
            Message message = update.getCallbackQuery().getMessage();
            EditMessageText answered = answerCallbackQuery(response, message);

            System.out.println("Пришел callbackQuery от пользователя " + message.getChatId() +
                    "\n сообщение " + message.getMessageId() +
                    "\n с содержимым " + response);

            try {
                execute(answered);
            } catch (TelegramApiException e){
                e.printStackTrace();
            }
        }

        System.out.println("AUU: " + System.getenv("JDBC_DATABASE_URL"));
    }

    //Метод, обрабатывающий нажатия кнопок на inline-клавиатуре
    public EditMessageText answerCallbackQuery(String response, Message message) {
        String s = currentDate.getDayOfWeek()
                .getDisplayName(TextStyle.FULL, new Locale("ru")) + ", "
                + currentDate.format(DateTimeFormatter.ofPattern("dd MMMM"));
        String textSchedule = s + "\nЗанятий не найдено";
        if (response.equals("next")) {
            this.currentDate = this.currentDate.plusDays(1);
            textSchedule = findScheduleAtDay(this.currentDate);
        }

        if (response.equals("prev")) {
            this.currentDate = this.currentDate.minusDays(1);
            textSchedule = findScheduleAtDay(currentDate);
        }
        setInlineKeyboard();
        return editTemplateMessage(textSchedule, message.getMessageId(), true);
    }


    // Метод формирует ответ бота на сообщение пользователя
//      Метод должен возвращать объект SendMessage с текстом
//объект SendMessage создается методом getTemplateMessage()
//    Значаение параметра text у метода отобразит бот
    public SendMessage getMessage(String msg) {
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);
        //specs хранит в себе все направления подгототвки(институты)
        Elements specs = schedule.getInstitutes();

        //Если нажали на группу
        for (Map.Entry<String, String> entry :
                groupLink.entrySet()) {
            if (msg.equals(entry.getKey())) {
                if (checkURL()) {
                    //Здесь хранится конечная ссылка на группу
                    this.isFinalUrl = false;
                    this.finalURL = schedule.baseURL + entry.getValue();
                    try {
                        schedule.connect(this.finalURL);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    setInlineKeyboard();

                    String response = findScheduleAtDay(todayIs);
                    return outTemplateMessage(response
                            + "\nfinalURL: " + finalURL, true, false);
                }
            }
        }

        //Если нажали на год поступления
        for (Element year :
                years) {
            if (msg.equals(year.text())) {
                if (checkURL()) {
                    keyboard.clear();
                    //Очищаем переменную url и вставляем в нее новую ссылку
                    clearURL();
                    url.append(year.select("a").attr("href"));
                    //Карта хранит пары "Группа - Ссылка"
                    groupLink = schedule.getGroups(url.toString());

                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, String> entry :
                            groupLink.entrySet()){
                        //Клавиатура, отображающая группы из карты
                        KeyboardRow keyboardRow1 = new KeyboardRow();
                        keyboardRow1.add((String) entry.getKey());
                        keyboard.add(keyboardRow1);

                        sb.append(entry.getKey()).append("\n");
                        sb.append(entry.getValue()).append("\n");
                    }

                    return outTemplateMessage("Выберите группу", false, true);
                } else return getErrorMessage();
            }
        }

        //Если нажали на программу подготовки
        for (String secondSpec :
                secondSpecs) {
            if (msg.equals(secondSpec)) {
                return outYearOfStudy(secondSpec, currentStudyLevel);
            }
        }

        //Если нажали на магистратуру/бакалавриат и т.д.
        for (String s :
                studyLevelsList) {
            if (msg.equals(s)) {
                if (checkURL()) {
                    currentStudyLevel = s;
                    return outSecondSpec(s);
                }
                else return getErrorMessage();
            }
        }

        //Если нажали на одно из меганаправлений(институт)
        //Тут формируем клавиатуру с уровнями подготовки(бакалавриат, магистратура и т.д.)

        for (Element e :
                specs) {
            if (msg.equals(e.text())) {
                clearVars();
                url.append(e.attr("href"));

                Elements studyLevels = schedule.getStudyLevels(url.toString());
                //Формируем клавиатуру
                for (Element e1 :
                        studyLevels) {
                    KeyboardRow keyboardRow1 = new KeyboardRow();
                    keyboardRow1.add(e1.text());
                    keyboard.add(keyboardRow1);
                    studyLevelsList.add(e1.text());
                }
                replyKeyboardMarkup.setKeyboard(keyboard);

                return outTemplateMessage(e.text() + "\n" + url, false, true);
            }
        }

        //Команда "/start"
        if (msg.equals("/start")) {
            //Очищаем все переменные и подключаемся к корню сайта
            clearVars();
            clearURL();
            studyLevelsList.clear();
            currentStudyLevel = "";
            currentDate = todayIs;

            try {
                schedule.connect(schedule.baseURL);
            } catch (IOException e) {
                e.printStackTrace();
            }
            specs = schedule.getInstitutes();

            //Формируем клавиатуру
            for (int i = 0; i < specs.size(); i=i+2) {
                KeyboardRow keyboardRow1 = new KeyboardRow();
                Element spec1 = specs.get(i);
                keyboardRow1.add(spec1.text());
                //Если количество элементов нечетно
                try {
                    Element spec2 = specs.get(i + 1);
                    keyboardRow1.add(spec2.text());
                } catch (IndexOutOfBoundsException e) {
                    System.out.println("Количество меганаправлений нечетно");
                }
                keyboard.add(keyboardRow1);
            }

            replyKeyboardMarkup.setKeyboard(keyboard);

            return outTemplateMessage("Выберите направление", false, true);
        }

        //Расписание одной группы
        if (msg.equals("/rasp") || msg.equals("р") || msg.equals("расписание") || msg.equals("r")) {
            clearVars();
            clearURL();
            currentDate = todayIs;

            try {
                schedule.connect(schedule.baseURL + "/JOUR/StudentGroupEvents/Primary/249260");
            } catch (IOException e) {
                e.printStackTrace();
            }

            String textSchedule = findScheduleAtDay(todayIs);

            setInlineKeyboard();
            return outTemplateMessage(textSchedule, true, false);
        }

        //Команда "/help"
        if (msg.equals("/help")) {
            return outTemplateMessage("Если вы выбрали не подходящую\nгруппу, попробуйте заново\n/start");
        }

        return getErrorMessage();
    }

    // Отображение года поступления
    // Метод создаст клавиатуру с годами поступления по заданной программе подготовки
    // Метод похож на setSecondSpec()
    // объект html - список из тегов li, внутри которых несколько блоков div, первый из
    // которых содержит название образовательной программы
    public SendMessage outYearOfStudy(String secondSpec, String studyLevel) {
        if (checkURL()) {
            clearVars();

            Elements html = schedule.getSecondSpecs(url.toString(), studyLevel);
            html = html.select("li");
            html.remove(0);
            KeyboardRow keyboardRow1 = new KeyboardRow();

            for (Element e : html) {
                //reply клавиатура
                Element nameOfSecondSpec = e.selectFirst("div");
                //Если нашли подходящую программу подготовки
                if (nameOfSecondSpec.text().equals(secondSpec)) {
                    //divs хранит список из годов поступления
                    Elements divs = e.select("div");
                    //Убираем первый div, хранящий образовательную проргамму
                    divs.remove(0);
                    //Проходимся по списку из годов
                    for (Element e1 :
                            divs) {
                        //Формируем клавиатуру с годами
                        keyboardRow1.add(e1.text());
                        years.add(e1);
                    }
                }
            }
            keyboard.add(keyboardRow1);
            replyKeyboardMarkup.setKeyboard(keyboard);

            return outTemplateMessage("Выберите год поступления", false, true);
        }

        else return getErrorMessage();
    }


    //Отображение специальности внутри института
    // объект html - список из тегов li, внутри которых несколько блоков div, первый из
    // которых содержит название образовательной программы
    public SendMessage outSecondSpec(String studyLevel){
        //Проверка url на пустоту
        if (checkURL()) {
            clearVars();
            //html - Полученный html-код, содержащий список ul с образовательными программами и годами поступления
            Elements html = schedule.getSecondSpecs(url.toString(), studyLevel);
            html = html.select("li");
            html.remove(0);

            for (Element e : html) {
                //reply клавиатура
                e = e.selectFirst("div");
                KeyboardRow keyboardRow1 = new KeyboardRow();
                keyboardRow1.add(e.text());
                keyboard.add(keyboardRow1);

                //Добавляем программу подготовки в глобальный список
                secondSpecs.add(e.text());
            }

            replyKeyboardMarkup.setKeyboard(keyboard);

            return outTemplateMessage("Выберите программу подготовки", false, true);
        } else return getErrorMessage();
    }

    public SendMessage getErrorMessage() {
        return outTemplateMessage("Ошибка, попробуйте /start");
    }

    public void setInlineKeyboard() {
        //Здесь другой способ создания макета кливиатуры, но суть та же
        InlineKeyboardButton btn1 = new InlineKeyboardButton("Предыдущий день");
        InlineKeyboardButton btn2 = new InlineKeyboardButton("Следующий день");
        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        btn1.setCallbackData("prev");
        btn2.setCallbackData("next");
        keyboardButtonsRow1.add(btn1);
        keyboardButtonsRow1.add(btn2);
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(keyboardButtonsRow1);
        inlineKeyboardMarkup.setKeyboard(rowList);
    }

    //Функция нужна для генерации стандартного объекта EditMessageText с клавиатурой inline
    //Вызываем ее, если хотим отредактировать сообщение
    public EditMessageText editTemplateMessage(String text, Integer messageId, Boolean needInlineKeyboard){
        EditMessageText emt = new EditMessageText();
        emt.setText(text);
        emt.setParseMode("HTML");
        emt.setChatId(chat_id);
        emt.setMessageId(messageId);
        if (needInlineKeyboard) emt.setReplyMarkup(inlineKeyboardMarkup);

        return emt;
    }

    public EditMessageText editTemplateMessage(String text, Integer messageId){
        EditMessageText emt = new EditMessageText();
        emt.setText(text);
        emt.setParseMode("HTML");
        emt.setChatId(chat_id);
        emt.setMessageId(messageId);

        return emt;
    }

    //Только один из двух флагов может принять значение "true"
    //Функция нужна для генерации стандартного объекта SendMessage с клавиатурой inline или reply
    public SendMessage outTemplateMessage(String text, Boolean needInlineKeyboard, Boolean needReplyKeyboard){
        SendMessage sm = new SendMessage();
        sm.setText(text);
        sm.setChatId(chat_id);
        sm.setParseMode("HTML");
        if (!(needInlineKeyboard && needReplyKeyboard)) {
            if (needInlineKeyboard) sm.setReplyMarkup(inlineKeyboardMarkup);
            if (needReplyKeyboard) sm.setReplyMarkup(replyKeyboardMarkup);
        }
        return sm;
    }

    public SendMessage outTemplateMessage(String text){
        SendMessage sm = new SendMessage();
        sm.setText(text);
        sm.setParseMode("HTML");
        sm.setChatId(chat_id);

        return sm;
    }

    //Находит расписание в заданный день. Дата передается в формате "22 марта"
    public String findScheduleAtDay(LocalDate date) {
        // День недели числом
        int dayOfWeek = date.getDayOfWeek().getValue();
        // В date теперь понедельник нужной недели
        date = date.minusDays(dayOfWeek - 1);
        // Нужная дата для генерации правильной ссылки
        String strDate = date.getYear() + "-";
        // Дописываем нули, если числа месяца и дня меньше 10
        if (date.getMonthValue() < 10)
            strDate += "0";
        strDate += date.getMonthValue() + "-";
        if (date.getDayOfMonth() < 10)
            strDate += "0";
        strDate += date.getDayOfMonth();

        // DEBUG
        System.out.println("strDate: " + "\t" + strDate);

        // Подключаемся по новому URL

        if (this.isFinalUrl) {
            // Обрезаем последние 10 символов с датой
            this.finalURL = this.finalURL.substring(0, this.finalURL.length() - 11);
            // Добавляем новую дату
            this.finalURL = this.finalURL + "/" + strDate;
        }

        else {
            this.finalURL = this.finalURL + "/" + strDate;
            this.isFinalUrl = true;
        }

        try {
            schedule.connect(finalURL);
        } catch (IOException e) {
            System.out.println("IOException при попытке найти расписание по дате");
            e.printStackTrace();
        }

        this.scheduleWithDateList = schedule.getSchedule();
        strDate = currentDate.format(DateTimeFormatter.ofPattern("dd MMMM"));

        for (ScheduleWithDate sched :
                this.scheduleWithDateList) {
            if (sched.getDate().equals(strDate)) {
                return sched.getText();
            }
        }
        String s = this.currentDate.getDayOfWeek()
                .getDisplayName(TextStyle.FULL, new Locale("ru"))
                + ", " + this.currentDate.format(DateTimeFormatter.ofPattern("dd MMMM"));
        return s + "\nЗанятий не найдено";
    }

    //Проверка на пустоту переменной url
    public Boolean checkURL() {
        return !(url.toString().equals(schedule.baseURL)) && !url.toString().isEmpty();
    }

    //Очистка переменных
    public void clearVars() {
        keyboard.clear();
        secondSpecs.clear();
        years.clear();
        groupLink.clear();
    }

    public void clearURL() {
        url.setLength(0);
        url.append(schedule.baseURL);
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }
}