package app.export;

import app.ExError;
import app.model.Client;
import app.model.Helper;
import app.model.Transaction;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Units;
import util.DateTools;

import java.io.OutputStream;
import java.time.LocalDate;
import java.util.ArrayList;

import static app.App.model;

public class Export {

    private Client client;
    private LocalDate dtStart, dtEnd;
    private Integer iddAzs;
    private String iddCard;
    private Integer mode;

    public Export(Client client, LocalDate dtStart, LocalDate dtEnd, Integer iddAzs, String iddCard, Integer mode) {
        this.client = client;
        this.dtStart = dtStart;
        this.dtEnd = dtEnd;
        this.iddAzs = iddAzs;
        this.iddCard = iddCard;
        this.mode = mode;
    }

    private ArrayList<Transaction> loadTransactions(int skip) throws ExError {
        return model.loadClientTransactions(
                client.getFirm().id, client.getIdd(), client.getIddSub(), dtStart, dtEnd, iddAzs, iddCard,
                skip, 10000, "DTSTART,IDDAZS");
    }

    /** Вычисляет размер для POI (в нем 1 единица = 1/20 point). Параметр - миллиметры. */
    private int mmToPt20(double mm) {
        return (int) ((mm * Units.EMU_PER_CENTIMETER * 20.0D) / (10.0D * Units.EMU_PER_POINT));
    }

    /** Вычисляет размер для POI (в нем 1 единица = 1/20 point). Параметр - пикселы. */
    private int pxToPt20(double px) {
        return (int) (px * Units.EMU_PER_PIXEL / Units.EMU_PER_POINT * 20.0D);
    }

    /** Вычисляет размер для POI (в нем 1 единица = 1/20 point). Параметр - поинт. */
    private int ptToPt20(double pt) {
        return (int) (pt * 20.0D);
    }

    /** Вычисляет размер для шарины стообца POI (в нем 1 единица = 1/256 char width). Параметр - миллиметры. */
    private int mmToSz(double mm) {
        return (int) (((mm * Units.EMU_PER_CENTIMETER) / (10.0D * Units.EMU_PER_CHARACTER)) * 256.0D * 0.946D);
        // Добавил коэффициент, т.к. размеры уползали, учет 5px на отступы и бордюр - не помогал!!!
    }

    /** Экспорт транзакций клиента. */
    public void exportTransactions(OutputStream outs) throws ExError {
        try {
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet("transactions");

            HSSFCreationHelper hlp = workbook.getCreationHelper();
            HSSFPalette palette = workbook.getCustomPalette();

            HSSFRow row;
            HSSFCell cell;
            int x = 0, y = 0;

            // Первая строка - информационная.
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 11));
            row = sheet.createRow(y = 0);
            row.setHeightInPoints((int) (2.5 * sheet.getDefaultRowHeightInPoints()));
            cell = row.createCell(x = 0);
            cell.setCellValue("Клиент СТК: " + client.getTitle() + " \n" +
                    "Транзакции отпуска топлива за период с " +
                    Helper.fmtDate8(dtStart) + " по " + Helper.fmtDate8(dtEnd) +
                    (iddAzs == null || iddAzs == 0 ? "" : " на АЗС №" + iddAzs) +
                    (iddCard == null ? "" : " по карте №" + iddCard.substring(3)));
            HSSFCellStyle stT = workbook.createCellStyle();
            HSSFFont font = workbook.createFont();
            font.setBold(true);
            font.setFontHeight((short) ptToPt20(12));
            stT.setFont(font);
            stT.setAlignment(HorizontalAlignment.CENTER);
            stT.setVerticalAlignment(VerticalAlignment.CENTER);
            cell.setCellStyle(stT);

            // Вторая строка - заголовки колонок.
            CellStyle stH = workbook.createCellStyle();
            stH.setAlignment(HorizontalAlignment.CENTER);
            stH.setVerticalAlignment(VerticalAlignment.CENTER);
            stH.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            stH.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            font = workbook.createFont();
            font.setBold(true);
            stH.setFont(font);
            stH.setBorderTop(BorderStyle.THIN);
            stH.setBorderBottom(BorderStyle.THIN);
            stH.setBorderLeft(BorderStyle.THIN);
            stH.setBorderRight(BorderStyle.THIN);

            row = sheet.createRow(++y);
            row.setHeightInPoints((2 * sheet.getDefaultRowHeightInPoints()));

            cell = row.createCell(x = 0);
            cell.setCellValue("ID");
            cell.setCellStyle(stH);
            sheet.setColumnWidth(x, mmToSz(20));

            cell = row.createCell(++x);
            cell.setCellValue("Дата и время");
            cell.setCellStyle(stH);
            sheet.setColumnWidth(x, mmToSz(40));

            cell = row.createCell(++x);
            cell.setCellValue("АЗС");
            cell.setCellStyle(stH);
            sheet.setColumnWidth(x, mmToSz(15));

            cell = row.createCell(++x);
            cell.setCellValue("ТРК");
            cell.setCellStyle(stH);
            sheet.setColumnWidth(x, mmToSz(15));

            cell = row.createCell(++x);
            cell.setCellValue("Н/П");
            cell.setCellStyle(stH);
            sheet.setColumnWidth(x, mmToSz(15));

            cell = row.createCell(++x);
            cell.setCellValue("Вид Н/П");
            cell.setCellStyle(stH);
            sheet.setColumnWidth(x, mmToSz(20));

            cell = row.createCell(++x);
            cell.setCellValue("Запрошено");
            cell.setCellStyle(stH);
            sheet.setColumnWidth(x, mmToSz(25));

            cell = row.createCell(++x);
            cell.setCellValue("Отпущено");
            cell.setCellStyle(stH);
            sheet.setColumnWidth(x, mmToSz(25));

            cell = row.createCell(++x);
            cell.setCellValue("Цена");
            cell.setCellStyle(stH);
            sheet.setColumnWidth(x, mmToSz(25));

            cell = row.createCell(++x);
            cell.setCellValue("Сумма");
            cell.setCellStyle(stH);
            sheet.setColumnWidth(x, mmToSz(30));

            cell = row.createCell(++x);
            cell.setCellValue("Карта");
            cell.setCellStyle(stH);
            sheet.setColumnWidth(x, mmToSz(25));

            cell = row.createCell(++x);
            cell.setCellValue("Информация по карте");
            cell.setCellStyle(stH);
            sheet.setColumnWidth(x, mmToSz(120));

            CellStyle stDT = workbook.createCellStyle();
            stDT.setDataFormat(hlp.createDataFormat().getFormat("dd.mm.yyyy hh:mm:ss"));
            stDT.setAlignment(HorizontalAlignment.CENTER);

            CellStyle stOIL = workbook.createCellStyle();
            stOIL.setAlignment(HorizontalAlignment.CENTER);

            CellStyle stNUM = workbook.createCellStyle();
            stNUM.setDataFormat(hlp.createDataFormat().getFormat("# ##0.00"));
            stNUM.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle stCARD = workbook.createCellStyle();
            stCARD.setAlignment(HorizontalAlignment.CENTER);

            int skip = 0;
            while (true) {
                ArrayList<Transaction> trans = loadTransactions(skip);
                if (trans.size() == 0) break;
                skip += trans.size();

                for (Transaction t : trans) {

                    row = sheet.createRow(++y);

                    cell = row.createCell(x = 0);
                    cell.setCellValue(t.getIdd());

                    cell = row.createCell(++x);
                    cell.setCellValue(DateTools.asDate(t.getStart()));
                    cell.setCellStyle(stDT);

                    cell = row.createCell(++x);
                    cell.setCellValue(t.getIddAzs());

                    cell = row.createCell(++x);
                    cell.setCellValue(t.getIddTrk());

                    cell = row.createCell(++x);
                    cell.setCellValue(t.getOil().getId());

                    cell = row.createCell(++x);
                    cell.setCellValue(t.getOil().getAbbreviation());
                    cell.setCellStyle(stOIL);

                    cell = row.createCell(++x);
                    cell.setCellValue(t.getVolReq() / 100.0);
                    cell.setCellStyle(stNUM);

                    cell = row.createCell(++x);
                    cell.setCellValue(t.getVolume() / 100.0);
                    cell.setCellStyle(stNUM);

                    cell = row.createCell(++x);
                    cell.setCellValue(t.getPrice() / 100.0);
                    cell.setCellStyle(stNUM);

                    cell = row.createCell(++x);
                    cell.setCellValue(t.getSumma() / 100.0);
                    cell.setCellStyle(stNUM);

                    cell = row.createCell(++x);
                    cell.setCellValue(t.getCardTitle());
                    cell.setCellStyle(stCARD);

                    cell = row.createCell(++x);
                    cell.setCellValue(t.getCardInfo());
                }
            }
            workbook.write(outs);
            outs.flush();

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ExError(ex, "Ошибка операции: %s", ex.getMessage());
        }
    }
}
