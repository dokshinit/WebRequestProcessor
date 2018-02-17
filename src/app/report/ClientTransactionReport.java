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
public class ClientTransactionReport extends BaseReport<ClientTransactionReport> {

    private Client client;
    private LocalDate dtStart, dtEnd;
    private Integer iddAzs;
    private Mode mode;

    private static final RGroup<Transaction, String> GROUP_CARD = new RGroup<>(1, "IDDCARD", Transaction::getCard);
    private static final RGroup<Transaction, Oil> GROUP_OIL = new RGroup<>(2, "IDDOIL", Transaction::getOil);

    public enum Mode {
        TIME(1, null, null),
        CARD_TIME(2, GROUP_CARD, null),
        OIL_TIME(3, GROUP_OIL, null),
        CARD_OIL_TIME(4, GROUP_CARD, GROUP_OIL),
        OIL_CARD_TIME(5, GROUP_OIL, GROUP_CARD);

        public int id;
        public RGroup<Transaction, ? extends Comparable> group, subgroup;

        Mode(int id, RGroup<Transaction, ? extends Comparable> group, RGroup<Transaction, ? extends Comparable> subgroup) {
            this.id = id;
            this.group = group == null ? RGroup.NO : group;
            this.subgroup = subgroup == null ? RGroup.NO : subgroup;
        }

        public static Mode byId(Integer idd) {
            if (idd == null) return null;
            for (Mode item : values()) if (item.id == idd) return item;
            return null;
        }
    }


    public ClientTransactionReport(Client client, LocalDate dtstart, LocalDate dtend, Integer iddazs, Mode mode) throws ExError {
        super(client.getFirm(), "Транзакции по картам клиента");
        this.client = client;
        this.dtStart = dtstart;
        this.dtEnd = dtend;
        this.iddAzs = iddazs;
        this.mode = mode == null ? Mode.CARD_OIL_TIME : mode;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void buildBody() throws ExError {
        // Установка строки детализации названия отчёта.
        headerDetailText = client.getTitle() + "\n" + "за период c " + fmtDate8(dtStart) + " по " + fmtDate8(dtEnd);
        if (iddAzs != null) headerDetailText += " на АЗС №" + iddAzs;

        // Создаем первую страницу.
        newPage();
        // Генерируем таблицу (заполнение страниц при генерации).
        new TransReportTable().build();
    }

    private class TransReportTable extends BaseReportTable<Transaction> {

        protected long allVol, allSum;
        protected long groupVol, groupSum;
        protected long subgroupVol, subgroupSum;

        public TransReportTable() {
            super(mode.group, mode.subgroup);
            allVol = allSum = groupVol = groupSum = subgroupVol = subgroupSum = 0;
            appendSortString("DTSTART");
        }

        @Override
        protected void setupCols(int... w) {
            super.setupCols(90, 60, 50, 35, 35, 55, 70, 80);
            switch (mode) {
                case TIME:
                    break;
                case CARD_TIME:
                    changeCols(1, 0, 2);
                    break;
                case CARD_OIL_TIME:
                    changeCols(1, 2, 0);
                    break;
                case OIL_TIME:
                    changeCols(2, 0, 1);
                    break;
                case OIL_CARD_TIME:
                    changeCols(2, 1, 0);
                    break;
            }
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
        protected void onDetail(Transaction it) {
            long v = it.getVolume(), s = it.getSumma();
            allVol += v;
            allSum += s;
            groupVol += v;
            groupSum += s;
            subgroupVol += v;
            subgroupSum += s;
        }

        @Override
        protected String getGroupHeadTitle(Transaction it) {
            if (group == GROUP_CARD) return it.getCard() + ": " + it.getCardInfo();
            if (group == GROUP_OIL) return it.getOil().getAbbreviation();
            return "";
        }

        @Override
        protected String getSubGroupHeadTitle(Transaction it) {
            if (subgroup == GROUP_CARD) return it.getCard() + ": " + it.getCardInfo();
            if (subgroup == GROUP_OIL) return it.getOil().getAbbreviation();
            return "";
        }

        @Override
        protected String getGroupSummaryTitle(Transaction it) {
            if (group == GROUP_CARD) return "Итого по карте " + it.getCard() + ": ";
            if (group == GROUP_OIL) return "Итого по " + it.getOil().getAbbreviation() + ": ";
            return "";
        }

        @Override
        protected String getSubGroupSummaryTitle(Transaction it) {
            if (subgroup == GROUP_CARD) return "Итого по карте " + it.getCard() + ": ";
            if (subgroup == GROUP_OIL) return "Итого по " + it.getOil().getAbbreviation() + ": ";
            return "";
        }

        @Override
        protected String getSummaryTitle() {
            return "Итого по транзакциям:";
        }

        @Override
        protected String getNoDataSummaryTitle() {
            return "Транзакции отсутствуют";
        }

        @Override
        protected void buildHead() {
            String[] t = {"Дата", "Карта", "Н/П"};
            XRBand band = new XRBand().style(tableHeadStyle).cols(colsW).rows(15)
                    .addGX(crText(t[colsO[0]]))
                    .addGX(crText(t[colsO[1]]))
                    .addGX(crText(t[colsO[2]]))
                    .addGX(crText("АЗС"))
                    .addGX(crText("ТРК"))
                    .addGX(crText("Цена, р."))
                    .addGX(crText("Кол-во, л."))
                    .addGX(crText("Сумма, р."))
                    .pack();
            fill(band);
        }

        @Override
        protected void buildGroupHead(Transaction it) {
            XRBand band = new XRBand().style(tableGroupStyle).cols(colsW).rows(12)
                    .addGX(colsW.length, crText(getGroupHeadTitle(it)).left())
                    .pack();
            fill(band);
        }

        @Override
        protected void buildGroupSummary(Transaction it) {
            XRBand band = new XRBand().style(tableGroupStyle).cols(colsW).rows(12)
                    .addGX(6, crText(getGroupSummaryTitle(it)).lwT(0f).transparent().right())
                    .addGX(crText(fmtN2(groupVol)))
                    .addGX(crText(fmtN2(groupSum)))
                    .pack();
            fill(band);
        }

        @Override
        protected void buildSubGroupHead(Transaction it) {
            XRBand band = new XRBand().style(tableSubGroupStyle).cols(colsW).rows(11)
                    .addGX(crText("").lwTB(0f, 0f).transparent())
                    .addGX(colsW.length - 1, crText(getSubGroupHeadTitle(it)).left())
                    .pack();
            fill(band);
        }

        @Override
        protected void buildSubGroupSummary(Transaction it) {
            XRBand band = new XRBand().style(tableSubGroupStyle).cols(colsW).rows(11)
                    .addGX(crText("").lwTB(0f, 0f).transparent())
                    .addGX(5, crText(getSubGroupSummaryTitle(it)).lwT(0f).transparent().right())
                    .addGX(crText(fmtN2(subgroupVol)))
                    .addGX(crText(fmtN2(subgroupSum)))
                    .pack();
            fill(band);
        }

        @Override
        protected void buildDetail(Transaction it) {
            String[] t = {fmtDT86(it.getStart()), it.getCard(), it.getOil().getAbbreviation()};
            XRText tx1 = hasGroup() ? crText("").lwTB(0f, 0f).transparent() : crText(t[colsO[0]]).center();
            XRText tx2 = hasSubGroup() ? crText("").lwTB(0f, 0f).transparent() : crText(t[colsO[1]]).center();
            XRBand band = new XRBand().style(tableDetailStyle).cols(colsW).rows(10)
                    .addGX(tx1)
                    .addGX(tx2)
                    .addGX(crText(t[colsO[2]]).center())
                    .addGX(crText("" + it.getIddAzs()).center())
                    .addGX(crText("" + it.getIddTrk()).center())
                    .addGX(crText(fmtN2(it.getPrice())).center())
                    .addGX(crText(fmtN2(it.getVolume())))
                    .addGX(crText(fmtN2(it.getSumma())))
                    .pack();
            fill(band);
        }

        @Override
        protected void buildSummary() {
            XRBand band = new XRBand().style(tableSumaryStyle).cols(colsW).rows(14)
                    .addGX(6, crText(getSummaryTitle()).left())
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
        protected ArrayList<Transaction> loadData(int skip, int limit) throws ExError {
            return model.loadClientTransactions(
                    client.getFirm().id, client.getIdd(), client.getIddSub(), dtStart, dtEnd, iddAzs,
                    skip, limit, sortString);
        }
    }

}