import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class User {
    public LocalDate currentDate;
    // Клавиатуры
    public ArrayList<KeyboardRow> keyboard;
    public ReplyKeyboardMarkup replyKeyboardMarkup;
    public InlineKeyboardMarkup inlineKeyboardMarkup;
    // Адрес, динамически собирающийся
    public StringBuilder url;
    public boolean isFinalUrl;
    public String finalURL;
    // Список с расписаниями на каждый день с привязкой к дате
    public ArrayList<ScheduleWithDate> scheduleWithDateList;
    // Список с программами подготовки
    public ArrayList<String> secondSpecs;
    // Карта, хранящая пары "Группа - Ссылка"
    public HashMap<String, String> groupLink;
    // Список с уровнями обучения
    public ArrayList<String> studyLevelsList;
    public String currentStudyLevel;
    // Название группы пользователя
    public String group;

    public final String NEXT_BUTTON_TEXT = "Далее";
    public final String PREV_BUTTON_TEXT = "Назад";
    public short keyboardPage = 0;

    public User() {
        this.keyboard = new ArrayList<>();
        this.replyKeyboardMarkup = new ReplyKeyboardMarkup();

        this.replyKeyboardMarkup.setSelective(true);
        this.replyKeyboardMarkup.setResizeKeyboard(true);
        this.replyKeyboardMarkup.setOneTimeKeyboard(false);

        this.inlineKeyboardMarkup = new InlineKeyboardMarkup();
        this.url = new StringBuilder(Schedule.baseURL);
        this.currentDate = Bot.todayIs;
        this.scheduleWithDateList = new ArrayList<>();
        this.secondSpecs = new ArrayList<>();
        this.groupLink = new HashMap<>();
        this.studyLevelsList = new ArrayList<>();
    }

    public void prepareReplyKeyboardMarkup() {
        final int keyboardSizeLimit = 60;

        int total = this.keyboard == null ? 0 : this.keyboard.size();
        if (total == 0) {
            this.replyKeyboardMarkup.setKeyboard(Collections.emptyList());
            return;
        }

        int totalPages = (int) Math.ceil(total / (double) keyboardSizeLimit);

        if (this.keyboardPage < 0) this.keyboardPage = 0;
        if (this.keyboardPage >= totalPages) this.keyboardPage = (short) (totalPages - 1);

        int from = this.keyboardPage * keyboardSizeLimit;
        int to = Math.min(from + keyboardSizeLimit, total);

        List<KeyboardRow> pageKb = new ArrayList<>(this.keyboard.subList(from, to));

        if (totalPages > 1) {
            KeyboardRow paginateRow = new KeyboardRow();
            if (this.keyboardPage > 0) {
                paginateRow.add(PREV_BUTTON_TEXT);
            }
            if (this.keyboardPage < totalPages - 1) {
                paginateRow.add(NEXT_BUTTON_TEXT);
            }
            if (!paginateRow.isEmpty()) {
                pageKb.add(paginateRow);
            }
        }

        this.replyKeyboardMarkup.setKeyboard(pageKb);
    }
}
