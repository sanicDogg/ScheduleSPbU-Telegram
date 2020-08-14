import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;

public class User {
    LocalDate currentDate;
    // Клавиатуры
    public ArrayList keyboard = new ArrayList<>();
    public ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
    public InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
    // Адрес, динамически собирающийся
    public StringBuilder url = new StringBuilder();
    public boolean isFinalUrl;
    public String finalURL;
    // Список с расписаниями на каждый день с привязкой к дате
    public ArrayList<ScheduleWithDate> scheduleWithDateList = new ArrayList<>();
    // Список с программами подготовки
    public ArrayList<String> secondSpecs = new ArrayList<>();
    // Карта, хранящая пары "Группа - Ссылка"
    public HashMap<String, String> groupLink = new HashMap<>();
    // Список с уровнями обучения
    public ArrayList<String> studyLevelsList = new ArrayList<>();
    public String currentStudyLevel;
    // Название группы пользователя
    public String group;

    public User() {
    }
}
