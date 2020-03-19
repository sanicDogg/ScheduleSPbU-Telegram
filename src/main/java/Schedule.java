import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;

public class Schedule {
    private Document document;
    public final String baseURL = "https://timetable.spbu.ru";

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
            document = Jsoup.connect(url)
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

        HashMap<String, String> result = new HashMap();
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
    public String getSchedule(int day) throws IndexOutOfBoundsException{
        StringBuilder sb = new StringBuilder();
        Elements accordion = document.select("#accordion .panel");
        accordion = accordion.select(".panel");

        Element element = accordion.get(day);
//            Получаем дату
        sb.append(element.select(".panel-heading h4").text()).append("\n");

        Elements subjects = element.select("ul li");
        for (Element subject : subjects) {
//                Получаем время
            sb.append(subject.select(".studyevent-datetime").text()).append("\n");
//                    Получаем предмет
            sb.append("<b>").append(subject.select(".studyevent-subject").text()).append("</b>\n");
//                    Получаем место проведения занятия
            String location = subject.select(".studyevent-locations").text();
            sb.append(location).append("\n");

//            String cabinet = location.substring(location.length() - 3);
//            location = location.substring(0, location.length() - 4);
//            //Выделяем курсивом номер аудитории
//            sb.append(location).append("\n <i>Аудитория: ")
//                    .append(cabinet).append("</i>")
//                    .append("\n");
//          Получаем преподавателя
            sb.append(subject.select(".studyevent-educators").text()).append("\n");
            sb.append("\n");
        }
        sb.append("\n");

        return sb.toString();
    }

    //Расписание на неделю
    public String getSchedule() {
        StringBuilder sb = new StringBuilder();
        Elements accordion = document.select("#accordion .panel");
        accordion = accordion.select(".panel");

        for (Element element : accordion) {
            //            Получаем дату
            sb.append(element.select(".panel-heading h4").text()).append("\n");

            Elements subjects = element.select("ul li");
            for (Element subject : subjects) {
//                Получаем время
                sb.append(subject.select(".studyevent-datetime").text()).append("\n");
//                    Получаем предмет
                sb.append(subject.select(".studyevent-subject").text()).append("\n");
                //                Получаем время
                sb.append(subject.select(".studyevent-datetime").text()).append("\n");
//                    Получаем место проведения занятия
                sb.append(subject.select(".studyevent-locations").text()).append("\n");

                //Получаем преподавателя
                sb.append(subject.select(".studyevent-educators").text()).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
