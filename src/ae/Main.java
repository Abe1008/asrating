/*
 * Copyright (c) 2019. Aleksey Eremin
 * 03.09.19 11:39
 */
/*
 * Формирование Excel таблицы рейтинга операторов за вчерашнее число, формируемого на основе данных MySql
 *
 */
package ae;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
	      // write your code here
        System.out.println("Данные рейтинга");
        String sqliteDb = null;
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-db":
                        sqliteDb = args[++i];    // имя файла Sqlite, вместо MySql
                        break;

                    case "-?":
                        System.out.println(helpmsg);
                        System.exit(1);
                        break;

                    default:
                        // задан файл рабочей базы данных
                        System.err.println("?-Error-неправильный формат командной строки");
                        System.exit(2);
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("?-Error-" + e.getMessage());
            System.exit(2);
            return;
        }

        if(!prepareWork()) {
            System.out.println("Error prepare work");
            return;
        }
        // вчерашняя дата
        final LocalDateTime dt = LocalDateTime.now().minusHours(24);
        int d = dt.getDayOfMonth();
        int m = dt.getMonthValue();
        int y = dt.getYear();
        String sdat = String.format("%02d.%02d.%04d",d,m,y);
        //
        // откроем БД
        Database db;
        if(sqliteDb == null) {
            System.out.println("Connect MySql");
            db = new DatabaseMysql(R.dbHost, R.dbBase, R.dbUser, R.dbPass);
        } else {
            System.out.println("Connect Sqlite");
            db = new DatabaseSqlite(sqliteDb);
        }
        //
        // создадим объект для формирования отчета Excel
        String   oidx = "1i | 2 | 3 | 4i | 5 | 6f | 7i | 8i | 9";    // список колонок в Excel
        FormaExcel fxls = new FormaExcel(oidx);   // подготовим шаблон
        // сформируем массив данных
        String strDat1 = String.format("%04d-%02d-%02d", y, m, d); // дата рейтинга
        String sql = "SELECT " +
                "'" + strDat1 + "'," +
                "1," +                                          // порядковый номер строки
                "Regions.nam, " +                               // название региона
                "Rating.op_name, " +                            // оператор
                "Rating.id_region, " +                          // номер региона
                "Rating.inn, " +                                // ИНН
                "CONCAT(ROUND(100*not_block/total,3),''), " +   // процент (не ставим знак %)
                "total, " +                                     // всего в реестре
                "not_block, " +                                 // не заблокировано
                // /// "GROUP_CONCAT(note SEPARATOR ' | ') " +         // вышестоящие
                "GROUP_CONCAT(note,'  ') " +         // вышестоящие
                "FROM Rating LEFT JOIN " +
                "(Opers LEFT JOIN opnotes ON (Opers.op_id=opnotes.op_id AND opnotes.tip='Uplink')) " +
                "ON Rating.inn = Opers.op_inn " +
                "LEFT JOIN Regions ON Rating.id_region=Regions.id " +
                "WHERE Rating.dat='" + strDat1 + "' " +
                "GROUP BY pn;";
        ArrayList<String[]> arrlst = db.DlookupArray(sql);

        if(arrlst == null) {
            System.err.println("?-Error-database error");
            System.exit(3);
        }

        System.out.println("Rating for "+ sdat);

        if(arrlst.size() > 0) {
            String outFile = fxls.makeList(arrlst, ".");
            System.out.println("output: " + outFile);
        } else {
            System.err.println("No rating");
        }

    }

    /**
     * Подготовить данные и файлы для работы
     * @return true - подготовлено, false - не готово
     */
    private static boolean prepareWork()
    {
        R r = new R();   // для загрузки ресурсных значений
        r.loadDefault(); // значения по умолчанию
        //
        //System.out.println("work  dir: " + R.workDir);
        //
        // проверим наличие каталогов
        String[] dstr = new String[]{R.workDir};
        for(int i=0; i < dstr.length; i++) {
            String s = dstr[i];
            File f = new File(s);
            if(!f.exists()) {
                System.out.println("?ERROR-Not found dir: " + s);
                return false;
            }
        }
        //
        return true;
    }

    static String helpmsg = "Формирование таблицы рейтинга. ver." + R.Ver + "\n" +
            "java -jar asrating.jar [-db sqlite.db]\n" +
            "  sqlite.db - БД Sqlite с копией данных из MySql\n" +
            "Если параметры не указаны, подключается к MySql.";

} // END OF CLASS
