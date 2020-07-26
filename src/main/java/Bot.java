import com.google.gson.Gson;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.validation.constraints.Null;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Bot extends TelegramLongPollingBot {
//    //TEST-BOT
//    public final String BOT_USERNAME = "@scheduleSPbU_test_bot";
//    public final String BOT_TOKEN = "1096924723:AAHGxadgGu2jsh1y54cli5LED1bGoVwvfl8";

    //    Константы
    public final String BOT_USERNAME = "@scheduleSPbU_bot";
    public final String BOT_TOKEN = System.getenv("BOT_TOKEN");

    // База данных, инициализируется в Main.java
    private Database db = null;
    public void setDb(Database db) {
        this.db = db;
    }
    private LocalDate todayIs;

    //    id текущего чата
    private long chat_id;
    private long prevChat_id=0;
    private String username;
    // Объект расписания
    public Schedule schedule = new Schedule();
    // Список с годами поступления
    public ArrayList<Element> years = new ArrayList<>();

    // Текущий пользователь
    private User user = null;

    // Метод, выполняющийся при получении сообщений
    @Override
    public void onUpdateReceived(Update update) {
        // Получаем текущую дату
        this.todayIs = LocalDate.now();

//    ID написавшего пользователя
        update.getUpdateId();

        // Пришел текст, или была нажата кнопка?
        if (update.hasMessage()) {
            this.chat_id = update.getMessage().getChatId();
            doDatabase(update);
            if (this.prevChat_id != this.chat_id) initUserField();
            SendMessage sendMessage;
            sendMessage = getMessage(update.getMessage().getText());
            sendMessage.setChatId(this.chat_id);
            updateDatabase();

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
            this.chat_id = message.getChatId();
            this.username = "@" + update.getCallbackQuery().getFrom().getUserName() + " " +
                    update.getCallbackQuery().getFrom().getFirstName() + " " +
                    update.getCallbackQuery().getFrom().getLastName();

            initUserField();
            EditMessageText answered = answerCallbackQuery(response, message);
            updateDatabase();

            System.out.println("Пришел callbackQuery от пользователя " + message.getChatId() +
                    "\n сообщение " + message.getMessageId() +
                    "\n с содержимым " + response);

            try {
                execute(answered);
            } catch (TelegramApiException e){
                e.printStackTrace();
            }
        }
        this.prevChat_id = this.chat_id;
    }

    // Метод, обрабатывающий нажатия кнопок на inline-клавиатуре
    public EditMessageText answerCallbackQuery(String response, Message message) {
        String s = this.user.currentDate.getDayOfWeek()
                .getDisplayName(TextStyle.FULL, new Locale("ru")) + ", "
                + this.user.currentDate.format(DateTimeFormatter.ofPattern("dd MMMM"));
        String textSchedule = s + "\nЗанятий не найдено";
        if (response.equals("next")) {
            this.user.currentDate = this.user.currentDate.plusDays(1);
            textSchedule = findScheduleAtDay(this.user.currentDate);
        }

        if (response.equals("prev")) {
            this.user.currentDate = this.user.currentDate.minusDays(1);
            textSchedule = findScheduleAtDay(this.user.currentDate);
        }
        setInlineKeyboard();
        return editTemplateMessage(textSchedule, message.getMessageId(), true);
    }

//      Метод формирует ответ бота на сообщение пользователя
//      Метод должен возвращать объект SendMessage с текстом
//      объект SendMessage создается методом getTemplateMessage()
//      Значаение параметра text у метода отобразит бот

    public SendMessage getMessage(String msg) {
        this.user.replyKeyboardMarkup.setSelective(true);
        this.user.replyKeyboardMarkup.setResizeKeyboard(true);
        this.user.replyKeyboardMarkup.setOneTimeKeyboard(false);
        //specs хранит в себе все направления подгототвки(институты)
        Elements specs = this.schedule.getInstitutes();

        //Если нажали на группу
        for (Map.Entry<String, String> entry :
                this.user.groupLink.entrySet()) {
            if (msg.equals(entry.getKey())) {
                if (checkURL()) {
                    //Здесь хранится конечная ссылка на группу
                    this.user.isFinalUrl = false;
                    this.user.finalURL = this.schedule.baseURL + entry.getValue();
                    try {
                        this.schedule.connect(this.user.finalURL);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    setInlineKeyboard();
                    this.user.group = msg;
                    String response = findScheduleAtDay(todayIs);

                    return outTemplateMessage(response, true, false);
                }
            }
        }

        //Если нажали на год поступления
        for (Element year :
                this.years) {
            if (msg.equals(year.text())) {
                if (checkURL()) {
                    this.user.keyboard.clear();
                    //Очищаем переменную url и вставляем в нее новую ссылку
                    clearURL();
                    this.user.url.append(year.select("a").attr("href"));
                    //Карта хранит пары "Группа - Ссылка"
                    this.user.groupLink = this.schedule.getGroups(this.user.url.toString());

                    for (Map.Entry<String, String> entry :
                            this.user.groupLink.entrySet()){
                        //Клавиатура, отображающая группы из карты
                        KeyboardRow keyboardRow1 = new KeyboardRow();
                        keyboardRow1.add((String) entry.getKey());
                        this.user.keyboard.add(keyboardRow1);
                    }

                    return outTemplateMessage("Выберите группу", false, true);
                } else return getErrorMessage();
            }
        }

        //Если нажали на программу подготовки
        for (String secondSpec :
                this.user.secondSpecs) {
            if (msg.equals(secondSpec)) {
                return outYearOfStudy(secondSpec, this.user.currentStudyLevel);
            }
        }

        //Если нажали на магистратуру/бакалавриат и т.д.
        for (String s :
                this.user.studyLevelsList) {
            if (msg.equals(s)) {
                if (checkURL()) {
                    this.user.currentStudyLevel = s;
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
                this.user.url.append(e.attr("href"));

                Elements studyLevels = this.schedule.getStudyLevels(this.user.url.toString());
                //Формируем клавиатуру
                for (Element e1 :
                        studyLevels) {
                    KeyboardRow keyboardRow1 = new KeyboardRow();
                    keyboardRow1.add(e1.text());
                    this.user.keyboard.add(keyboardRow1);
                    this.user.studyLevelsList.add(e1.text());
                }
                this.user.replyKeyboardMarkup.setKeyboard(this.user.keyboard);

                return outTemplateMessage(e.text() + "\n" + this.user.url, false, true);
            }
        }

        //Команда "/start"
        if (msg.equals("/start")) {
            //Очищаем все переменные и подключаемся к корню сайта
            clearVars();
            clearURL();
            this.user.studyLevelsList.clear();
            this.user.currentStudyLevel = "";
            this.user.currentDate = todayIs;

            try {
                this.schedule.connect(this.schedule.baseURL);
            } catch (IOException e) {
                e.printStackTrace();
            }
            specs = this.schedule.getInstitutes();

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
                this.user.keyboard.add(keyboardRow1);
            }

            this.user.replyKeyboardMarkup.setKeyboard(this.user.keyboard);

            return outTemplateMessage("Выберите направление", false, true);
        }

        //Расписание одной группы
        if (msg.equals("/rasp") || msg.equals("р") || msg.equals("расписание") || msg.equals("r")) {
            clearVars();
            clearURL();
            this.user.currentDate = todayIs;

            try {
                this.schedule.connect(this.schedule.baseURL + "/JOUR/StudentGroupEvents/Primary/249260");
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

    // Метод добавляет пользователя в базу, если его нет; обновляет базу, если
    // пришел овтет от старого пользователя; начинает работать с другим пользователем,
    // если ответ пришел от другого пользователя

    public void doDatabase(Update update) {
            this.username = "@" + update.getMessage().getFrom().getUserName() + " " +
                    update.getMessage().getFrom().getFirstName() + " " +
                    update.getMessage().getFrom().getLastName();
            Gson gson = new Gson();

            try {
                // db.findUser returns list with username_tg and json
                String user = db.findUser(this.chat_id).get(0);
                String json = gson.toJson(this.user);
                // Если пользователя еще нет в базе
                if (user.equals("undefined")) {
                    db.addUser(this.chat_id,
                            this.username, "Unknown", json);
                    // LOGS
                    System.out.println("Added new user in database");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
    }

    public void updateDatabase() {
        Gson gson = new Gson();
        String json = gson.toJson(this.user);
        try {
            db.editUser(this.chat_id, this.username, this.user.group, json);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void initUserField() {
        try {
            Gson gson = new Gson();

            this.user = gson.fromJson(db.findUser(this.chat_id).get(1), User.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Отображение года поступления
    // Метод создаст клавиатуру с годами поступления по заданной программе подготовки
    // Метод похож на setSecondSpec()
    // объект html - список из тегов li, внутри которых несколько блоков div, первый из
    // которых содержит название образовательной программы
    public SendMessage outYearOfStudy(String secondSpec, String studyLevel) {
        if (checkURL()) {
            clearVars();

            Elements html = this.schedule.getSecondSpecs(this.user.url.toString(), studyLevel);
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
                        this.years.add(e1);
                    }
                }
            }
            this.user.keyboard.add(keyboardRow1);
            this.user.replyKeyboardMarkup.setKeyboard(this.user.keyboard);

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
            Elements html = this.schedule.getSecondSpecs(this.user.url.toString(), studyLevel);
            html = html.select("li");
            html.remove(0);

            for (Element e : html) {
                //reply клавиатура
                e = e.selectFirst("div");
                KeyboardRow keyboardRow1 = new KeyboardRow();
                keyboardRow1.add(e.text());
                this.user.keyboard.add(keyboardRow1);

                //Добавляем программу подготовки в глобальный список
                this.user.secondSpecs.add(e.text());
            }

            this.user.replyKeyboardMarkup.setKeyboard(this.user.keyboard);

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
        this.user.inlineKeyboardMarkup.setKeyboard(rowList);
    }

    //Функция нужна для генерации стандартного объекта EditMessageText с клавиатурой inline
    //Вызываем ее, если хотим отредактировать сообщение
    public EditMessageText editTemplateMessage(String text, Integer messageId, Boolean needInlineKeyboard){
        EditMessageText emt = new EditMessageText();
        emt.setText(text);
        emt.setParseMode("HTML");
        emt.setChatId(this.chat_id);
        emt.setMessageId(messageId);
        if (needInlineKeyboard) emt.setReplyMarkup(this.user.inlineKeyboardMarkup);

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
            if (needInlineKeyboard) sm.setReplyMarkup(this.user.inlineKeyboardMarkup);
            if (needReplyKeyboard) sm.setReplyMarkup(this.user.replyKeyboardMarkup);
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

        if (this.user.isFinalUrl) {
            // Обрезаем последние 10 символов с датой
            this.user.finalURL = this.user.finalURL.substring(0, this.user.finalURL.length() - 11);
            // Добавляем новую дату
            this.user.finalURL = this.user.finalURL + "/" + strDate;
        }

        else {
            this.user.finalURL = this.user.finalURL + "/" + strDate;
            this.user.isFinalUrl = true;
        }

        try {
            this.schedule.connect(this.user.finalURL);
        } catch (IOException e) {
            System.out.println("IOException при попытке найти расписание по дате");
            e.printStackTrace();
        }

        this.user.scheduleWithDateList = this.schedule.getSchedule();
        strDate = this.user.currentDate.format(DateTimeFormatter.ofPattern("dd MMMM"));

            for (ScheduleWithDate sched :
                    this.user.scheduleWithDateList) {
                if (sched.getDate().equals(strDate)) {
                return this.user.group + "\n" + sched.getDate() + "\n" + sched.getText();
            }
        }
        String s = this.user.group + "\n" + this.user.currentDate.getDayOfWeek()
                .getDisplayName(TextStyle.FULL, new Locale("ru"))
                + ", " + this.user.currentDate.format(DateTimeFormatter.ofPattern("dd MMMM"));
        return s + "\nЗанятий не найдено";
    }

    //Проверка на пустоту переменной url
    public Boolean checkURL() {
        return !(this.user.url.toString().equals(this.schedule.baseURL))
                && !this.user.url.toString().isEmpty();
    }

    //Очистка переменных
    public void clearVars() {
        this.user.keyboard.clear();
        this.user.secondSpecs.clear();
        this.years.clear();
        this.user.groupLink.clear();
        this.user.group = "";
    }

    public void clearURL() {
        this.user.url.setLength(0);
        this.user.url.append(this.schedule.baseURL);
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