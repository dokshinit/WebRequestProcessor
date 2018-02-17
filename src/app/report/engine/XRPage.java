package app.report.engine;

import net.sf.jasperreports.engine.PrintPageFormat;
import net.sf.jasperreports.engine.base.JRBasePrintPage;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (01.02.18).
 */
public class XRPage {

    protected final PrintPageFormat format;
    protected final JRBasePrintPage page;

    // Положение доступной области текущей страницы на листе.
    protected int width, height;
    // Координаты областей колонтитулов в координатах страницы.
    protected int headerY, headerH;
    protected int footerY, footerH;
    // Размеры доступной области текущей страницы (колонтитулы в нее не входят!).
    protected int bodyX, bodyY, bodyW, bodyH;

    // Координаты курсора.
    protected int curX, curY;

    protected XRText pageNumText;

    public XRPage(PrintPageFormat pf) {
        page = new JRBasePrintPage();
        format = pf;
        width = pf.getPageWidth();
        height = pf.getPageHeight();
        bodyX = pf.getLeftMargin();
        bodyY = pf.getTopMargin();
        bodyW = width - pf.getLeftMargin() - pf.getRightMargin();
        bodyH = height - pf.getTopMargin() - pf.getBottomMargin();
        pageNumText = null;

        colontituls(0, 0).xy(0, 0); // перерассчитывается также и доступное поле тела отчёта.
    }

    public JRBasePrintPage getPage() {
        return page;
    }

    public PrintPageFormat getPageFormat() {
        return format;
    }

    public int x() {
        return curX;
    }

    public int y() {
        return curY;
    }

    public int bodyX() {
        return bodyX;
    }

    public int bodyY() {
        return bodyY;
    }

    public int bodyW() {
        return bodyW;
    }

    public int bodyH() {
        return bodyH;
    }

    public int headerY() {
        return headerY;
    }

    public int headerH() {
        return headerH;
    }

    public int footerY() {
        return footerY;
    }

    public int footerH() {
        return footerH;
    }

    public XRText pageNumField() {
        return pageNumText;
    }

    public XRText pageNumField(XRText text) {
        return pageNumText = text;
    }

    public XRPage x(int x) {
        curX = x;
        return this;
    }

    public XRPage y(int y) {
        curY = y;
        return this;
    }

    public XRPage xy(int x, int y) {
        curX = x;
        curY = y;
        return this;
    }

    public XRPage colontituls(int head, int foot) {
        headerH = head;
        footerH = foot;
        headerY = format.getTopMargin();
        footerY = height - format.getBottomMargin() - footerH;
        bodyY = headerY + headerH;
        bodyH = footerY - bodyY;
        return this;
    }

    public XRPage header(int h) {
        return colontituls(h, footerH);
    }

    public XRPage footer(int h) {
        return colontituls(headerH, h);
    }

    public boolean isHeightFit(XRBand band) {
        return bodyH - curY >= band.height();
    }

    public int remainingHeight() {
        return bodyH - curY;
    }

    public static final String PAGE_NUM_KEY = "xPageNum";

    public XRPage fill(XRBand band, int startx, int starty) {
        for (XRElement e : band.getElements()) {
            e.xy(startx + e.x(), starty + e.y());
            if (e.key(PAGE_NUM_KEY) != null && e instanceof XRText) pageNumText = (XRText) e;
            page.addElement(e.JR());
        }
        curY += band.height();
        band.clear(); // Очищаем бэнд - т.к. элементы добавлены на страницу и не могут быть добавлены еще раз (изменить координаты).
        return this;
    }

    public XRPage fill(XRBand band) {
        return fill(band, bodyX + curX, bodyY + curY);
    }

    public XRPage fillHeader(XRBand band) {
        return fill(band, bodyX + curX, headerY + curY);
    }

    public XRPage fillFooter(XRBand band) {
        return fill(band, bodyX + curX, footerY + curY);
    }
}
