package app.report;

import app.ExError;
import app.model.Firm;
import app.report.engine.*;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.PrintPageFormat;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.OutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import util.CommonTools;

import java.awt.*;
import java.io.File;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

//import static app.App.logger;
import static app.model.Helper.*;
import static app.report.engine.XRBuilder.*;
import static util.StringTools.*;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (01.02.18).
 */
@SuppressWarnings("unchecked")
public abstract class BaseReport<T extends BaseReport<T>> {

    protected XRBuilder xb;

    protected Firm firm;
    protected String headerTitleText, headerDetailText;
    //
    protected XRPage curPage = null;
    protected ArrayList<XRPage> pages = new ArrayList<>();

    public BaseReport(Firm firm, String title, String detail) {
        this.xb = new XRBuilder();
        this.firm = firm;
        this.headerTitleText = title;
        this.headerDetailText = detail;
    }

    public BaseReport(Firm firm, String title) {
        this(firm, title, null);
    }

    public BaseReport(Firm firm) {
        this(firm, null, null);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public JasperPrint getPrint() {
        return xb.getPrint();
    }

    protected XRLine crLine() {
        return xb.crLine();
    }

    protected XRLine crLine(Color c) {
        return xb.crLine(c);
    }

    protected XRText crText(String title) {
        return xb.crText(title);
    }

    protected XRText crText(int width, String title) {
        return xb.crText(width, title);
    }

    protected XRText crText(int width, int height, String title) {
        return xb.crText(width, height, title);
    }

    public int getPagesCount() {
        return pages.size();
    }

    protected boolean isFirstPage() {
        return getPagesCount() == 1;
    }


    protected Runnable runBeforeNewPage, runOnNewPage;

    protected void fireBeforeNewPage() {
        if (runBeforeNewPage != null) runBeforeNewPage.run();
    }

    protected void fireOnNewPage() {
        if (runOnNewPage != null) runOnNewPage.run();
    }

    protected T newPage(PrintPageFormat fmt) {
        fireBeforeNewPage();

        curPage = xb.newPage(fmt);
        pages.add(curPage);
        //logger.infof("NEW PAGE (%d)", getPagesCount());

        // Т.к. header и footer не пересекаются - можем заполнять независимо.
        buildHeader();
        buildFooter();

        // После сбрасываем координаты в начало (перед постройкой тела).
        curPage.xy(0, 0);

        fireOnNewPage();

        return (T) this;
    }

    protected T newPage() {
        return newPage(null);
    }

    protected T x(int x) {
        curPage.x(x);
        return (T) this;
    }

    protected T y(int y) {
        curPage.y(y);
        return (T) this;
    }

    protected T xy(int x, int y) {
        curPage.xy(x, y);
        return (T) this;
    }

    protected T sx(int dx) {
        curPage.x(curPage.x() + dx);
        return (T) this;
    }

    protected T sy(int dy) {
        curPage.y(curPage.y() + dy);
        return (T) this;
    }

    protected T fill(XRBand band) {
        if (curPage.isHeightFit(band)) {
            curPage.fill(band);
        } else {
            // Переход на новую страницу.
            newPage(curPage.getPageFormat()); // Повторяем формат!
            curPage.fill(band); // Если бенд больше полной доступной высоты пустой страницы - сами себе дураки!
        }
        return (T) this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    protected void buildHeader() {
        if (!isFirstPage()) return; // Только на первой странице.

        String info = firm.getAddress();
        if (!firm.getContacts().isEmpty()) info += "\n" + firm.getContacts();
        String bank = (firm.getInn().isEmpty() ? "" : ("ИНН " + firm.getInn() + ", ")) + firm.getBank();

        boolean istitle = false;
        int w = curPage.bodyW();
        XRBand band = new XRBand().style(defaultStyle)
                .addX(xb.crText(160, 20, firm.getTitle()).fontSize(14f).bold().left().middle())
                .addX(crText(w - 160 - 70, 20, info).fontSize(7f).left().middle().pad(5, 1, 0, 0))
                .addX(crText(70, 20, fmtDT86(LocalDateTime.now())).fontSize(6f).right().top().color(Color.GRAY))
                .nextLine()
                .addY(crText(w, 9, bank).pad(2, 2, 0, 0).fontSize(6f).left().middle().opaque().bgcolor(new Color(0xE0E0E0)).expandY(20))
                .addY(crLine().size(w, 1));
        if (headerTitleText != null && !headerDetailText.isEmpty()) {
            istitle = true;
            band.addY(crText(w, 20, headerTitleText).fontSize(14f).bold().center().middle().expandY(50));
        }
        if (headerDetailText != null && !headerDetailText.isEmpty()) {
            istitle = true;
            band.addY(crText(w, 15, headerDetailText).fontSize(12f).bold().center().middle().expandY(100));
        }
        if (istitle) band.sy(2).addY(crLine().size(w, 1));
        band.pack();
        curPage.header(band.height() + 3).xy(0, 0).fillHeader(band);
    }

    protected Color footerPageColor = new Color(0x666666);

    protected String getFooterNote() {
        String s = headerTitleText == null ? "" : headerTitleText;
        if (!isEmptySafe(headerDetailText)) s += (s.isEmpty() ? "" : ": ") + headerDetailText;
        return s.replace("\n", " ");
    }

    protected void buildFooter() {
        XRText t = crText(curPage.bodyW() - 85, 15, getFooterNote()).padT(3).fontSize(6f).italic().left().bottom().expandY(30);
        XRText pn = crText(85, t.height(), "").padT(3).fontSize(8f).right().bottom().color(footerPageColor);
        XRBand band = new XRBand().style(defaultStyle).addX(t).addX(pn).pack();

        curPage.footer(band.height()).xy(0, 0).fillFooter(band);
        curPage.pageNumField(pn);
    }

    protected String pageNumFmtString = "стр. %d / %d";

    protected void updatePageNums() {
        int n = pages.size();
        for (int i = 0; i < n; i++) {
            XRText t = pages.get(i).pageNumField();
            if (t != null) t.text(pageNumFmtString, i + 1, n).pack();
        }
    }

    public T build() throws ExError {
        buildBody();
        updatePageNums();
        return (T) this;
    }

    protected abstract void buildBody() throws ExError;

    public void exportToPDF(OutputStreamExporterOutput outputStreamExporter) throws JRException {
        JRPdfExporter exporterPDF = new JRPdfExporter();
        exporterPDF.setExporterInput(new SimpleExporterInput(getPrint()));
        exporterPDF.setExporterOutput(outputStreamExporter);
        exporterPDF.exportReport();
    }

    public void exportToPDF(String filename) throws JRException {
        exportToPDF(new SimpleOutputStreamExporterOutput(filename));
    }

    public void exportToPDF(File file) throws JRException {
        exportToPDF(new SimpleOutputStreamExporterOutput(file));
    }

    public void exportToPDF(OutputStream outputStream) throws JRException {
        exportToPDF(new SimpleOutputStreamExporterOutput(outputStream));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static class RGroup<T, V extends Comparable> {

        public static final RGroup NO = new RGroup(0, null, null);

        public final int id;
        public String sortString;
        public Function<T, V> cmpSupplier;

        public RGroup(int id, String sortString, Function<T, V> cmpSupplier) {
            this.id = id;
            this.sortString = sortString;
            this.cmpSupplier = cmpSupplier;
        }

        public boolean hasGroup() {
            return id > 0 && cmpSupplier != null;
        }

        public boolean isChanged(T obj1, T obj2) {
            return hasGroup() && CommonTools.compareNullForward(cmpSupplier.apply(obj1), cmpSupplier.apply(obj2)) != 0;
        }

        public V value(T obj) {
            return hasGroup() ? cmpSupplier.apply(obj) : null;
        }

        public String valueAsString(T obj) {
            V value = value(obj);
            return value == null ? "" : value.toString();
        }
    }

    /** Стандартная таблица, имеющая до двух группировок. */
    protected class BaseReportTable<TT> {

        protected final RGroup<TT, ? extends Comparable> group, subgroup;
        protected int[] colsO;
        protected int[] colsW;
        protected ArrayList<TT> itemList;
        protected TT lastIt, curIt;
        protected String sortString;
        // Аккумуляторы для строк.
        public int curN, curGroupN, curSubGgroupN;

        public BaseReportTable(RGroup<TT, ? extends Comparable> group, RGroup<TT, ? extends Comparable> subgroup) {
            this.group = group;
            this.subgroup = subgroup;
            this.colsO = null;
            this.colsW = null;
            this.itemList = new ArrayList<>();
            this.lastIt = null;
            this.curIt = null;

            this.sortString = "";
            if (!isEmptySafe(group.sortString)) {
                appendSortString(group.sortString);
                if (!isEmptySafe(subgroup.sortString)) appendSortString(subgroup.sortString);
            }

            setupCols(); // по умолчанию пустая
        }

        public BaseReportTable(RGroup<TT, ? extends Comparable> group) {
            this(group, RGroup.NO);
        }

        public BaseReportTable() {
            this(RGroup.NO, RGroup.NO);
        }

        protected void appendSortString(String str) {
            if (sortString == null || sortString.isEmpty()) {
                sortString = str;
            } else {
                sortString += ", " + str;
            }
        }

        protected void setupCols(int... w) {
            colsW = w;
            colsO = new int[colsW.length];
            for (int i = 0; i < colsO.length; i++) colsO[i] = i;
        }

        protected void changeCols(int... nums) {
            int[] w = Arrays.copyOf(colsW, colsW.length);
            for (int i = 0; i < nums.length; i++) {
                if (nums[i] < 0) continue;
                colsO[i] = nums[i];
                colsW[i] = w[nums[i]];
            }
        }

        protected boolean hasGroup() {
            return group.hasGroup();
        }

        protected boolean hasSubGroup() {
            return subgroup.hasGroup();
        }

        protected boolean isGroupChanged() {
            return group.isChanged(lastIt, curIt);
        }

        protected boolean isSubGroupChanged() {
            return subgroup.isChanged(lastIt, curIt);
        }

        protected void onGroupChanged() { // аккумуляторы например
        }

        protected void onSubGroupChanged() { // аккумуляторы например
        }

        protected void onDetail(TT it) { // аккумуляторы например
        }

        protected String getGroupHeadTitle(TT it) {
            return hasGroup() ? group.valueAsString(it) : "";
        }

        protected String getGroupSummaryTitle(TT it) {
            return hasGroup() ? "Итого по " + group.valueAsString(it) + ": " : "Итого: ";
        }

        protected String getSubGroupHeadTitle(TT it) {
            return hasSubGroup() ? subgroup.valueAsString(it) : "";
        }

        protected String getSubGroupSummaryTitle(TT it) {
            return hasSubGroup() ? "Итого по " + subgroup.valueAsString(it) + ": " : "Итого: ";
        }

        protected String getSummaryTitle() {
            return "Итого по отчёту: ";
        }

        protected String getNoDataSummaryTitle() {
            return "Данные отсутствуют";
        }


        protected void buildHead() {
        }

        protected void buildGroupHead(TT it) {
        }

        protected void buildGroupSummary(TT it) {
        }

        protected void buildSubGroupHead(TT it) {
        }

        protected void buildSubGroupSummary(TT it) {
        }

        protected void buildDetail(TT it) {
        }

        protected void buildSummary() {
        }

        protected void buildNoDataSummary() {
        }

        protected ArrayList<TT> loadData(int skip, int limit) throws ExError {
            return new ArrayList<>();
        }

        protected void build() throws ExError {
            buildHead(); // Начальный заголовок.

            Runnable saveOnNewPage = runOnNewPage;
            runOnNewPage = this::buildHead; // Заголовок на каждую ноыую страницу таблицы.
            int limit = 5000;
            lastIt = null;
            curN = curGroupN = curSubGgroupN = 0;
            int loaded = 0;
            while (true) {
                //logger.infof("LOAD...");
                itemList = loadData(loaded, limit);
                //logger.infof("LOADED = %d", itemList.size());
                if (!itemList.isEmpty()) {
                    loaded += itemList.size();
                    for (TT it : itemList) {
                        curIt = it;
                        if (lastIt != null) {
                            // Проверка на завершение группы и открытие новой.
                            if (hasGroup()) {
                                if (isGroupChanged()) {
                                    if (hasSubGroup()) buildSubGroupSummary(lastIt);
                                    buildGroupSummary(lastIt);
                                    onGroupChanged();
                                    curGroupN = curSubGgroupN = 0;
                                    buildGroupHead(curIt);
                                    if (hasSubGroup()) buildSubGroupHead(curIt);
                                } else {
                                    if (hasSubGroup()) {
                                        if (isSubGroupChanged()) {
                                            buildSubGroupSummary(lastIt);
                                            onSubGroupChanged();
                                            curSubGgroupN = 0;
                                            buildSubGroupHead(curIt);
                                        }
                                    }
                                }
                            }
                        } else {
                            if (hasGroup()) buildGroupHead(curIt);
                            if (hasSubGroup()) buildSubGroupHead(curIt);
                        }
                        // Добавление строки.
                        buildDetail(curIt);

                        onDetail(curIt);

                        lastIt = curIt;
                        curN++;
                        curGroupN++;
                        curSubGgroupN++;
                    }
                    if (itemList.size() == limit) continue;
                    // Если считано меньше лимита, то значит всё считали.

                }

                // Завершение
                if (lastIt != null) {
                    // Закрываем группы.
                    if (hasSubGroup()) buildSubGroupSummary(lastIt);
                    if (hasGroup()) buildGroupSummary(lastIt);
                    buildSummary();
                } else {
                    buildNoDataSummary();
                }
                break; // Прерываем.
            }

            runOnNewPage = saveOnNewPage;
        }
    }
}
