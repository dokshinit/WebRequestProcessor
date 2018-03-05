package app.report;

import app.ExError;
import app.model.*;
import app.report.engine.XRBand;
import app.report.engine.XRText;

import java.time.LocalDate;
import java.util.ArrayList;

import static app.App.model;
import static app.model.Helper.*;
import static app.report.engine.XRBuilder.*;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (02.02.18).
 */
public class ClientTurnoverReport extends BaseReport<ClientTurnoverReport> {

    private Client client;
    //
    private LocalDate dtStart, dtEnd;

    public ClientTurnoverReport(Client client, LocalDate dtstart, LocalDate dtend) throws ExError {
        super(client.getFirm(), "Обороты по клиенту");
        this.client = client;
        this.dtStart = dtstart;
        this.dtEnd = dtend;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void buildBody() throws ExError {
        // Установка строки детализации названия отчёта.
        headerDetailText = client.getTitle() + "\n" + "за период c " + fmtDate8(dtStart) + " по " + fmtDate8(dtEnd);

        // Создаем первую страницу.
        newPage();

        // Генерируем таблицу (заполнение страниц при генерации).
        new SaldoReportTable(true).build();
        sy(3);
        new SaleReportTable().build();
        sy(3);
        new PayReportTable().build();
        sy(3);
        new SaldoReportTable(false).build();
    }

    private class SaldoReportTable extends BaseReportTable<Saldo> {

        protected boolean isStart;

        public SaldoReportTable(boolean isstart) {
            super();
            this.isStart = isstart;
        }

        @Override
        protected void setupCols(int... w) {
            super.setupCols(100, 50, 80, 80);
        }

        @Override
        protected String getNoDataSummaryTitle() {
            return "Сальдо отсутствует";
        }

        @Override
        protected void buildHead() {
            XRBand band = new XRBand().style(tableHeadStyle).cols(colsW).rows(15, 15)
                    .addGX(colsW.length, crText(isStart ? "Сальдо начальное" : "Сальдо конечное").padL(0).left().lw(0f).transparent())
                    .addGX(crText("Тип счета"))
                    .addGX(crText("Н/П"))
                    .addGX(crText("Кол-во, л."))
                    .addGX(crText("Сумма, р."))
                    .pack();
            fill(band);
        }

        @Override
        protected void buildDetail(Saldo it) {
            boolean isoil = it.getAccType() == AccType.KEEP;
            XRBand band = new XRBand().style(tableGroupStyle).cols(colsW).rows(14)
                    .addGX(crText(it.getAccType().getTitle()).left())
                    .addGX(crText(isoil ? it.getOil().getAbbreviation() : "—").center())
                    .addGX(crText(isoil ? fmtN2(it.getSaldo()) : "—"))
                    .addGX(crText(isoil ? "—" : fmtN2(it.getSaldo())))
                    .pack();
            fill(band);
        }

        @Override
        protected void buildNoDataSummary() {
            XRBand band = new XRBand().style(tableSumaryStyle).cols(colsW).rows(15)
                    .addGX(colsW.length, crText(getNoDataSummaryTitle()).left())
                    .pack();
            fill(band);
        }

        @Override
        protected ArrayList<Saldo> loadData(int skip, int limit) throws ExError {
            // Для начальной даты - отнимаем день, т.к. сальдо на конец дня.
            return model.loadClientSaldos(client.getFirm().id, client.getIdd(), client.getIddSub(),
                    isStart ? dtStart.minusDays(1) : dtEnd);
        }
    }

    private class PayReportTable extends BaseReportTable<Pay> {

        protected long allVol, allSum;

        public PayReportTable() {
            super();
            allVol = allSum = 0;
        }

        @Override
        protected void setupCols(int... w) {
            super.setupCols(30, 70, 50, 80, 80, 200);
        }

        @Override
        protected void onDetail(Pay it) {
            allVol += it.getVolume();
            allSum += it.getSumma();
        }

        @Override
        protected String getSummaryTitle() {
            return "Итого по приходу:";
        }

        @Override
        protected String getNoDataSummaryTitle() {
            return "Приход отсутствует";
        }

        @Override
        protected void buildHead() {
            XRBand band = new XRBand().style(tableHeadStyle).cols(colsW).rows(15, 15)
                    .addGX(colsW.length, crText("Приход").padL(0).left().lw(0f).transparent())
                    .addGX(crText("№"))
                    .addGX(crText("Дата"))
                    .addGX(crText("Н/П"))
                    .addGX(crText("Кол-во, л."))
                    .addGX(crText("Сумма, р."))
                    .addGX(crText("Документ"))
                    .pack();
            fill(band);
        }

        @Override
        protected void buildDetail(Pay it) {
            boolean isoil = it.getAccType() == AccType.KEEP;
            XRBand band = new XRBand().style(tableDetailStyle).cols(colsW).rows(13)
                    .addGX(crText("" + (curN + 1)).padR(5))
                    .addGX(crText(fmtDate8(it.getDtw())).center())
                    .addGX(crText(isoil ? it.getOil().getAbbreviation() : "—").center())
                    .addGX(crText(isoil ? fmtN2(it.getVolume()) : "—"))
                    .addGX(crText(isoil ? "—" : fmtN2(it.getSumma())))
                    .addGX(crText(it.getDoc()).padL(5).left())
                    .pack();
            fill(band);
        }

        @Override
        protected void buildSummary() {
            XRBand band = new XRBand().style(tableSumaryStyle).cols(colsW).rows(15)
                    .addGX(3, crText(getSummaryTitle()).left())
                    .addGX(crText(allVol == 0 ? "—" : fmtN2(allVol)))
                    .addGX(crText(allSum == 0 ? "—" : fmtN2(allSum)))
                    .addGX(crText(""))
                    .pack();
            fill(band);
        }

        @Override
        protected void buildNoDataSummary() {
            XRBand band = new XRBand().style(tableSumaryStyle).cols(colsW).rows(14)
                    .addGX(colsW.length, crText(getNoDataSummaryTitle()).left())
                    .pack();
            fill(band);
        }

        @Override
        protected ArrayList<Pay> loadData(int skip, int limit) throws ExError {
            return model.loadClientPays(client.getFirm().id, client.getIdd(), client.getIddSub(), dtStart, dtEnd);
        }
    }

    private static final RGroup<Sale, AccType> SALE_GROUP_ACCTYPE = new RGroup<>(1, "IACCTYPE", Sale::getAccType);
    private static final RGroup<Sale, Oil> SALE_GROUP_OIL = new RGroup<>(2, "IDDOIL", Sale::getOil);

    private class SaleReportTable extends BaseReportTable<Sale> {

        protected long allVol, allSum;
        protected long groupVol, groupSum;
        protected long subgroupVol, subgroupSum;

        public SaleReportTable() {
            super(SALE_GROUP_ACCTYPE, SALE_GROUP_OIL);
            allVol = allSum = groupVol = groupSum = subgroupVol = subgroupSum = 0;
            appendSortString("DBPRICE");
        }

        @Override
        protected void setupCols(int... w) {
            super.setupCols(50, 50, 50, 80, 80);
        }

        @Override
        protected void onGroupChanged() {
            groupVol = groupSum = subgroupVol = subgroupSum = 0;
        }

        @Override
        protected void onSubGroupChanged() {
            subgroupVol = subgroupSum = 0;
        }

        @Override
        protected void onDetail(Sale it) {
            long v = it.getVolume(), s = it.getSumma();
            allVol += v;
            allSum += s;
            groupVol += v;
            groupSum += s;
            subgroupVol += v;
            subgroupSum += s;
        }

        @Override
        protected String getGroupHeadTitle(Sale it) {
            return it.getAccType().getTitle();
        }

        @Override
        protected String getSubGroupHeadTitle(Sale it) {
            return it.getOil().getAbbreviation();
        }

        @Override
        protected String getGroupSummaryTitle(Sale it) {
            return "Итого по виду " + it.getAccType().getTitle() + ": ";
        }

        @Override
        protected String getSubGroupSummaryTitle(Sale it) {
            return "Итого по " + it.getOil().getAbbreviation() + ": ";
        }

        @Override
        protected String getSummaryTitle() {
            return "Итого по расходу:";
        }

        @Override
        protected String getNoDataSummaryTitle() {
            return "Расход отсутствует";
        }

        @Override
        protected void buildHead() {
            XRBand band = new XRBand().style(tableHeadStyle).cols(colsW).rows(15, 15)
                    .addGX(colsW.length, crText("Расход").padL(0).left().lw(0f).transparent())
                    .addGX(crText("Вид Л/С"))
                    .addGX(crText("Н/П"))
                    .addGX(crText("Цена, р."))
                    .addGX(crText("Кол-во, л."))
                    .addGX(crText("Сумма, р."))
                    .pack();
            fill(band);
        }

        @Override
        protected void buildGroupHead(Sale it) {
            XRBand band = new XRBand().style(tableGroupStyle).cols(colsW).rows(14)
                    .addGX(colsW.length, crText(getGroupHeadTitle(it)).left())
                    .pack();
            fill(band);
        }

        @Override
        protected void buildGroupSummary(Sale it) {
            XRBand band = new XRBand().style(tableGroupStyle).cols(colsW).rows(14)
                    .addGX(3, crText(getGroupSummaryTitle(it)).lwT(0f).transparent().right())
                    .addGX(crText(fmtN2(groupVol)))
                    .addGX(crText(fmtN2(groupSum)))
                    .pack();
            fill(band);
        }

        @Override
        protected void buildSubGroupHead(Sale it) {
            if (it.getAccType() == AccType.KEEP) return; // Для о\х не выводим!
            XRBand band = new XRBand().style(tableSubGroupStyle).cols(colsW).rows(13)
                    .addGX(crText("").lwTB(0f, 0f).transparent())
                    .addGX(colsW.length - 1, crText(getSubGroupHeadTitle(it)).left())
                    .pack();
            fill(band);
        }

        @Override
        protected void buildSubGroupSummary(Sale it) {
            if (it.getAccType() == AccType.KEEP) return; // Для о\х не выводим!
            XRBand band = new XRBand().style(tableSubGroupStyle).cols(colsW).rows(13)
                    .addGX(crText("").lwTB(0f, 0f).transparent())
                    .addGX(2, crText(getSubGroupSummaryTitle(it)).lwT(0f).transparent().right())
                    .addGX(crText(fmtN2(subgroupVol)))
                    .addGX(crText(fmtN2(subgroupSum)))
                    .pack();
            fill(band);
        }

        @Override
        protected void buildDetail(Sale it) {
            boolean is = it.getAccType() == AccType.KEEP;
            XRText tx = is ? crText(it.getOil().getAbbreviation()) : crText("").lwTB(0f, 0f).transparent();
            XRBand band = new XRBand().style(tableDetailStyle).cols(colsW).rows(12)
                    .addGX(crText("").lwTB(0f, 0f).transparent())
                    .addGX(tx)
                    .addGX(crText(is ? "---" : fmtN2(it.getPrice())).center())
                    .addGX(crText(fmtN2(it.getVolume())))
                    .addGX(crText(is ? "---" : fmtN2(it.getSumma())))
                    .pack();
            fill(band);
        }

        @Override
        protected void buildSummary() {
            XRBand band = new XRBand().style(tableSumaryStyle).cols(colsW).rows(14)
                    .addGX(3, crText(getSummaryTitle()).left())
                    .addGX(crText(fmtN2(allVol)))
                    .addGX(crText(fmtN2(allSum)))
                    .pack();
            fill(band);
        }

        @Override
        protected void buildNoDataSummary() {
            XRBand band = new XRBand().style(tableSumaryStyle).cols(colsW).rows(14)
                    .addGX(colsW.length, crText(getNoDataSummaryTitle()).left())
                    .pack();
            fill(band);
        }

        @Override
        protected ArrayList<Sale> loadData(int skip, int limit) throws ExError {
            return model.loadClientSales(
                    client.getFirm().id, client.getIdd(), client.getIddSub(), dtStart, dtEnd);
        }
    }
}
