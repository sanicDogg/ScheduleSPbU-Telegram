import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Schedule {
    public Document document;
    public static final String baseURL = "https://timetable.spbu.ru";
    public static final String  ONLINE_LESSON = "С использованием информационно-коммуникационных технологий";

    public Schedule() {
        try {
            connect(baseURL);
        }

        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connect(String url) throws IOException {
        try {
            document = Jsoup.connect(url).validateTLSCertificates(false)
                    .header("Accept-Language", "ru-RU, ru;q=0.9")
                    .get();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Возвращает направления(институты)
    public Elements getInstitutes() {
        try {
            connect(baseURL);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Elements institutes = document.select(".list-group-item a");
//        Убираем 4 последних элемента, не относящихся к направлениям
        for (int i = 0; i < 4; i++) {
            institutes.remove(institutes.last());
        }
        //Убираем колледж и академическую гимназию
        institutes.remove(6);
        institutes.remove(0);

        return institutes;
    }

    //Получаем программы подготовки внутри выбранного института(глобального направления)
    //studyLevel - переменная, которая должна содержать в себе одно слово из выборки:
    // "Магистратура", "Специалитет", "Ординатура" и т.д.
    public Elements getSecondSpecs(String url, String studyLevel) {
        try {
            this.connect(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return document.select(".panel-default:has(.panel-title:contains(" + studyLevel + "))");
    }

    public Elements getStudyLevels(String url) {
        try {
            this.connect(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return document.select(".panel-default .panel-heading");
    }

    //Получение списка групп
    //Вернет словарь "Группа - Ссылка" по заданному url
    public HashMap<String, String> getGroups(String url) {
        try {
            this.connect(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        HashMap<String, String> result = new HashMap<>();
        Elements list = document.select("#studentGroupsForCurrentYear li");

        for (Element e :
                list) {
            Element linkAndGroupName = e.selectFirst("div");
            String link = linkAndGroupName.attr("onclick");
            link = link.substring(22, link.length()-1);
            String group;
            group = linkAndGroupName.text();
            result.put(group, link);
        }

        return result;
    }

    //В day нужно передавать номер дня в расписании(начинается с нуля) на неделю, чтобы получить расписание на день
    public ArrayList<ScheduleWithDate> getSchedule() throws IndexOutOfBoundsException{
        ArrayList<ScheduleWithDate> scheduleWithDateList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        Elements accordion = document.select("#accordion .panel");
        accordion = accordion.select(".panel");

        for (Element element : accordion) {
            sb.setLength(0);
//            Получаем дату
            String stringDate = element.select(".panel-heading h4").text();
            sb.append(stringDate).append("\n");
            //Получаем дату в формате "23 марта"(без дня недели)
            String month = stringDate.substring(getIndexOfLastSpace(stringDate));
            stringDate = stringDate.substring(0, getIndexOfLastSpace(stringDate));
            String dayOfMonth = stringDate.substring(getIndexOfLastSpace(stringDate) + 1);
            stringDate = dayOfMonth + month;

            Elements subjects = element.select("ul li");

            for (Element subject : subjects) {
//                Получаем время
                sb.append(subject.select(".studyevent-datetime").text()).append("\n");
//                    Получаем предмет
                sb.append("<b>").append(subject.select(".studyevent-subject").text()).append("</b>\n");
//          Получаем место проведения занятия
                String location = subject.select(".studyevent-locations").text();

                if (location.equals("")) {
                    sb.append("<i>Местоположения нет на сайте</i>").append("\n");
                } else

                if (location.equals(ONLINE_LESSON)) {
                    sb.append("<i>Место проведения: онлайн</i>").append("\n");
                } else

                sb.append(getLocationAndCabinet(location));

//          Получаем преподавателя
                sb.append(subject.select(".studyevent-educators").text()).append("\n");
                sb.append("\n");
            }
            sb.append("\n");
            scheduleWithDateList.add(new ScheduleWithDate(sb.toString(), stringDate));
        }

        return scheduleWithDateList;
    }

    public StringBuffer getLocationAndCabinet(String location) {
        StringBuffer sb = new StringBuffer();

        // Проверка наличия аудитории
        String comma = location.substring(location.length() - 4, location.length() - 3);
        if (!comma.equals(","))
            return new StringBuffer("Место проведения: " + location).append("\n");

        // Обрезаем 3 цифры с конца, это и есть номер аудитории
        int beginIndex = location.length() - 3;
        String cabinet = location.substring(beginIndex);
        location = location.substring(0, beginIndex - 1);
        //Выделяем курсивом номер аудитории
        sb.append(location);

        sb.append("\n<i>Аудитория: ")
                .append(cabinet).append("</i>").append("\n");
        return sb;
    }

    public int getIndexOfLastSpace(String s){
        //В цикле находим индекс последнего пробела в строке
        int index = 0;
        for (int i = s.length()-1; i >= 0; i--) {
            if (s.charAt(i) == ' ') {
                index = i;
                break;
            }
        }
        return index;
    }
}
