/*
 * 2024(C) Alek
 * формирование документа Excel 2003 с таблицей рейтинга
 *
 */

package ae;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class FormaExcel {

  final int[]   f_jInx;  // индексы БД, соответствующие колонке в Excel (-1 нет соответствия)
  final char[]  f_jTyp;  // типы колонок в Excel
  final int     f_Noia; // размер массива шаблона

  /**
   * Конструктор
   * @param Templ строка шаблона соответствия
   */
  FormaExcel(String Templ)
  {
    // подготовка заданного шаблона преобразования в порядковые массивы индексов и типов колонок
    // Шаблон - строка таблицы Excel в порядке следования колонок, колонки разделяются '|'.
    // В колонке шаблона указывается порядковый номер поля БД (нумерация с 0), которое вставляется в
    // данную колонку Excel.
    // Если номер не указан, то данная колонка не заполняется.
    // Если у номера указана буква i, то это целочисленная колонка, если f - действительное число.
    // шаблон колонок "1i | 2 | 3 | 4i | 5 | 6f | 7i | 8i |9"
    final String[] oia = Templ.replaceAll(" ","").split("[|;]");
    final int noir = oia.length;
    int[]  jinx = new int[noir];   // индексы БД, соответствующие колонке в Excel (-1 нет соответствия)
    char[] jtyp = new char[noir];  // типы колонок в Excel
    for(int ixls = 0; ixls < noir; ixls++)  {
      // ixls - колонка в Excel
      String si = oia[ixls];
      String mi = si.replaceAll("[^0-9]", "");
      String mt = si.replaceAll("[0-9]", "");
      if(mi.length() < 1) mi = "-1";  // неподходящее число заменим на -1 тюею игнор и пропуск колонки
      if(mt.length() < 1) mt = "-";   // не целое и не действительное
      jinx[ixls] = Integer.parseInt(mi);  // индекс в шаблоне (индекс в картеже из БД)
      jtyp[ixls] = mt.charAt(0);          // тип колонки (i - целое, f - действительное)
    }
    // запомним результат работы
    this.f_jInx = jinx; // массив индексов
    this.f_jTyp = jtyp; // массив типов
    this.f_Noia = noir; // размер массивов
  }

  /**
   * Изготовить лист отчета по Рейтингу
   * @param arrlst    массив данных
   * @param outDir    выходной каталог
   * @return          имя сформированного файла
   */
  String makeList(ArrayList<String[]> arrlst, String outDir)
  {
    final int Data_base_row = 2;       // базовая строка, для вставки данных
    //final int Date_base_col = 1;       // базовая колонка для вставки данных
    //
    try {
      // получим дату рейтинга
      String[] sdat = arrlst.get(0);
      String dat = sdat[0];   // ячейка с датой рейтинга
      String[] ymd = dat.split("-");
      int yea = Integer.parseInt(ymd[0]);    // Год
      int mon = Integer.parseInt(ymd[1]);    // Месяц
      int day = Integer.parseInt(ymd[2]);    // День
      //
      String resname = "res/" + R.fileNameExcel;
      String s = String.format("%04d%02d%02d_", yea, mon, day);
      String fileName = outDir + R.sep + s + R.fileNameExcel;
      R r = new R();
      if(!r.writeRes2File(resname, fileName)) {
        // System.out.println("?-ERROR-Can't write file: " + fileName);
        System.exit(3);
      }
      //
      FileInputStream inp = new FileInputStream(fileName);
      // получим рабочую книгу Excel
      //Workbook wb = new XSSFWorkbook(inp); // прочитать файл с Excel 2010
      HSSFWorkbook wb = new HSSFWorkbook(inp); // прочитать файл с Excel 2003
      inp.close();
      // Read more: http://www.techartifact.com/blogs/2013/10/update-or-edit-existing-excel-files-in-java-using-apache-poi.html#ixzz4Y23Vf1eR
      // получим первый лист
      HSSFSheet wks = wb.getSheetAt(0); //Access the worksheet, so that we can update / modify it.
      // заполним лист данными за требуемую дату
      int cnt = 0; // кол-во записанных строк
      for(String[] rst: arrlst) {
        Row row = wks.getRow(Data_base_row + cnt);
        if(row == null) {
          row = wks.createRow(Data_base_row + cnt);
        }
        cnt++;
        rst[1] = Integer.toString(cnt); // порядковый номер строки
        setRowVals(row, rst); // записать строку в Excel
      }
      // установить дату на листе
      String strDat1 =String.format("%02d.%02d.%04d", day, mon, yea); // дата рейтинга
      // ячейка даты
      Row row = wks.getRow(0);
      setCellVal(row, 2, strDat1);
      // После заполнения ячеек формулы не пересчитываются, поэтому выполним принудительно
      // перерасчет всех формул на листе
      // http://poi.apache.org/spreadsheet/eval.html#Re-calculating+all+formulas+in+a+Workbook
      // в данной задаче в листе Excel нет формул, поэтому этот код ниже закоментирован
      //// FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
      //// for (Sheet sheet : wb) { for (Row row : sheet) {  for (Cell c : row) { if (c.getCellType() == Cell.CELL_TYPE_FORMULA) { evaluator.evaluateFormulaCell(c); }  }  } }
      //
      // Write the output to a file
      FileOutputStream fileOut = new FileOutputStream(fileName);
      wb.write(fileOut);
      fileOut.close();
      return fileName;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Записать значения в строку Excel из массива строк ответа БД
   * и преобразование некоторых позиций в целое или действительное число, в
   * соответствии с шаблоном
   * @param row   строка Excel, куда делается запись
   * @param rst   массив данных строки для записи
   */
  private void setRowVals(Row row, String[] rst)
  {
    final int nrst = rst.length;
    for(int ixls = 0; ixls < this.f_Noia; ixls++)  {
      // ixls - колонка в Excel
      int j = f_jInx[ixls];  // индекс в шаблоне (индекс в картеже из БД)
      if( j < 0  ||  j >= nrst ) continue;
      // есть число - работаем дальше
      String r = rst[j];
      // индекс в картеже допустимый
      switch (this.f_jTyp[ixls]) {
        case 'i':
          // целочисленная колонка
          try {
            int v = Integer.parseInt(r); // числовое представление
            setCellVal(row, ixls, v);
          } catch (Exception e) {
            System.err.println("Ошибка преобразования целого числа: " + r + " - " + e.getMessage());
          }
          break;

        case 'f':
          // действительная колонка
          try {
            double v = Double.parseDouble(r); // числовое представление
            setCellVal(row, ixls, v);
          } catch (Exception e) {
            System.err.println("Ошибка преобразования действительного числа: " + r + " - " + e.getMessage());
          }
          break;

        default:
          setCellVal(row, ixls, r);
          break;
      }
    }
  }

  /**
   * Установить действительное числовое значение ячейки в заданной строке таблицы
   * @param row   строка
   * @param col   номер колонки
   * @param val   устанавливаемое значения (double)
   * @return      1 - значение установлено, 0 - не установлено
   */
  private boolean setCellVal(Row row, int col, double val)
  {
    try {
      getCell(row, col).setCellValue(val);  // Access the cell
    } catch (Exception e) {
      System.err.println("ошибка здания значения клетке " + col + " value: " + val);
      return false;
    }
    return true;
  }

  /**
   * Установить числовое значение ячейки в заданной строке таблицы
   * @param row   строка
   * @param col   номер колонки
   * @param val   устанавливаемое значения (long)
   * @return      1 - значение установлено, 0 - не установлено
   */
  private boolean setCellVal(Row row, int col, int val)
  {
    try {
      getCell(row, col).setCellValue(val);  // Access the cell
    } catch (Exception e) {
      System.err.println("ошибка здания значения клетке " + col + " value: " + val);
      return false;
    }
    return true;
  }

  /**
   * Установить строковое значение ячейки в заданной строке таблицы
   * @param row   строка
   * @param col   номер колонки
   * @param val   устанавливаемое значения (String)
   * @return      1 - значение установлено, 0 - не установлено
   */
  private boolean setCellVal(Row row, int col, String val)
  {
    try {
      getCell(row, col).setCellValue(val);  // Access the cell
    } catch (Exception e) {
      System.err.println("ошибка здания значения клетке " + col + " value: " + val);
      return false;
    }
    return true;
  }

  /**
   * Получить ячейки в строке в заданной колонке
   * @param row   строка
   * @param col   индекс колонки
   * @return  ячейка (клетка)
   */
  private Cell getCell(Row row, int col)
  {
    Cell c = row.getCell(col);  // Access the cell
    if (c == null) {
      c = row.createCell(col); // создадим ячейку
    }
    return c;
  }


} // END OF CLASS

