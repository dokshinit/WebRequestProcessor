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
public class ClientCardReport extends BaseReport<ClientCardReport> {

    private Client client;
    //
    private LocalDate dtw;
    private Card.WorkState workState;
    private Mode mode;

    private static final RGroup<Card, Card.WorkState> GROUP_WORKSTATE = new RGroup<>(1, "IBWORK DESC", Card::getWorkState);

    public enum Mode {
        CARD(1, null),
        WORKSTATE_CARD(2, GROUP_WORKSTATE);

        public int id;
        public RGroup<Card, ? extends Comparable> group;

        Mode(int id, RGroup<Card, ? extends Comparable> group) {
            this.id = id;
            this.group = group == null ? RGroup.NO : group;
        }

        public static Mode byId(Integer idd) {
            if (idd == null) return null;
            for (Mode item : values()) if (item.id == idd) return item;
            return null;
        }
    }

    public ClientCardReport(Client client, LocalDate dtw, Card.WorkState workstate, Mode mode) throws ExError {
        super(client.getFirm(), "Карты клиента");
        this.client = client;
        this.dtw = dtw;
        this.workState = workstate;
        this.mode = mode == null ? Mode.CARD : mode;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void buildBody() throws ExError {
        // Установка строки детализации названия отчёта.
        headerDetailText = client.getTitle() + "\n" + "на " + fmtDate8(dtw);
        if (workState != null) headerDetailText += " (" + workState.getTitleForMany() + ")";

        // Создаем первую страницу.
        newPage();
        // Генерируем таблицу (заполнение страниц при генерации).
        new CardsReportTable().build();
    }

    private class CardsReportTable extends BaseReportTable<Card> {

        public CardsReportTable() {
            super(mode.group, RGroup.NO);
            appendSortString("IDD");
        }

        @Override
        protected void setupCols(int... w) {
            super.setupCols(40, 50, /***/40, 40, 120, 120, 80, 40);
            if (workState != null) colsW[1] = 0; // Не выводим второй столбец!
            switch (mode) {
                case CARD:
                    break;
                case WORKSTATE_CARD:
                    changeCols(1, 0);
                    break;
            }
        }

        @Override
        protected String getGroupHeadTitle(Card it) {
            return it.getWorkState().getTitleForMany();
        }

        @Override
        protected String getGroupSummaryTitle(Card it) {
            return "Итого карт ("+it.getWorkState().getTitleForMany()+"): " + (curGroupN + 1);
        }

        @Override
        protected String getSummaryTitle() {
            return "Итого карт: " + (curN + 1);
        }

        @Override
        protected String getNoDataSummaryTitle() {
            return "Карты отсутствуют";
        }

        @Override
        protected void buildHead() {
            XRText[] t = new XRText[]{crText("Номер").fontSize(9f), crText("Состо-\nяние").fontSize(9f)};
            XRBand band = new XRBand().style(tableHeadStyle).cols(colsW).rows(25)
                    .addGX(t[colsO[0]])
                    .addGX(t[colsO[1]])
                    .addGX(crText("Дата\nизмен.").fontSize(9f))
                    .addGX(crText("Дата\nприобр.").fontSize(9f))
                    .addGX(crText("Водитель").fontSize(9f))
                    .addGX(crText("Транспортное\nсредство").fontSize(9f))
                    .addGX(crText("Информация").fontSize(9f))
                    .addGX(crText("Лимит").fontSize(9f))
                    .pack();
            fill(band);
        }

        @Override
        protected void buildGroupHead(Card it) {
            if (workState != null) return; // При отборе по одному состоянию - не выводим!
            XRBand band = new XRBand().style(tableGroupStyle).cols(colsW).rows(12)
                    .addGX(colsW.length, crText(getGroupHeadTitle(it)).left())
                    .pack();
            fill(band);
        }

        @Override
        protected void buildGroupSummary(Card it) {
            if (workState != null) return; // При отборе по одному состоянию - не выводим!
            XRBand band = new XRBand().style(tableGroupStyle).cols(colsW).rows(12)
                    .addGX(colsW.length, crText(getGroupSummaryTitle(it)).left())
                    .pack();
            fill(band);
        }

        @Override
        protected void buildDetail(Card it) {
            String[] t = {it.getIddCard(), it.getWorkState().getTitle()};
            XRText tx1 = hasGroup() ? crText("").lwTB(0f, 0f).transparent() : crText(t[colsO[0]]).center();
            XRText tx2 = hasSubGroup() ? crText("").lwTB(0f, 0f).transparent() : crText(t[colsO[1]]).center();
            XRBand band = new XRBand().style(tableDetailStyle).cols(colsW).rows(10)
                    .addGX(tx1)
                    .addGX(tx2)
                    .addGX(crText(fmtDate6(it.getDtw())).center())
                    .addGX(crText(fmtDate6(it.getDtwEnd())).center())
                    .addGX(crText(it.getDriver()).left())
                    .addGX(crText(it.getCar()).left())
                    .addGX(crText(it.getComment()).left())
                    .addGX(crText(it.getDbDayLimit() == 0 ? "—" : fmtN2_0(it.getDbDayLimit())))
                    .pack();
            fill(band);
        }

        @Override
        protected void buildSummary() {
            XRBand band = new XRBand().style(tableSumaryStyle).cols(colsW).rows(14)
                    .addGX(colsW.length, crText(getSummaryTitle()).left())
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
        protected ArrayList<Card> loadData(int skip, int limit) throws ExError {
            return model.loadClientCards(
                    client.getFirm().id, client.getIdd(), client.getIddSub(), dtw, workState, sortString);
        }
    }

}