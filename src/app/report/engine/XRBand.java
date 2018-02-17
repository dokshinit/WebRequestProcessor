package app.report.engine;

import net.sf.jasperreports.engine.design.JRDesignStyle;

import java.util.ArrayList;

/**
 * Неразрывная группа элементов, которые размещаются только на одной странице. Т.е. если группа не влезает на текущую
 * страницу и происходит перенос на следующую страницу, то переносится вся группа целиком.
 *
 * @author Aleksey Dokshin <dant.it@gmail.com> (31.01.18).
 */
@SuppressWarnings("Duplicates")
public class XRBand {
    /** */
    private int width, height;

    private int curX, curY;
    private int curStepX, curStepY;
    private int curDefaultElementW, curDefaultElementH;
    private int curNextLineY, curOffsetX;
    private JRDesignStyle curStyle;

    private int markX, markY, markStepX, markStepY;

    private final ArrayList<XRElement> elements;

    public ArrayList<XRElement> getElements() {
        return elements;
    }

    public XRBand() {
        elements = new ArrayList<>();
        clear();
    }

    public XRBand clear() {
        elements.clear();
        curX = curY = 0;
        curStepX = curStepY = 0;
        curNextLineY = 0;
        curOffsetX = 0;
        curStyle = null;
        curGridCol = curGridRow = 0;
        mark();
        return this;
    }

    public XRBand pack() {
        width = 0;
        height = 0;
        for (XRElement e : elements) {
            e.pack();
            int w = e.x() + e.width();
            int h = e.y() + e.height();
            if (width < w) width = w;
            if (height < h) height = h;
        }
        return this;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public XRBand style(JRDesignStyle style) {
        curStyle = style;
        return this;
    }

    public XRBand unstyle() {
        curStyle = null;
        return this;
    }

    public XRBand x(int x) {
        curX = x;
        return this;
    }

    public XRBand y(int y) {
        curY = y;
        return this;
    }

    public XRBand xy(int x, int y) {
        curX = x;
        curY = y;
        return this;
    }

    public XRBand sx() {
        curX += curStepX;
        return this;
    }

    public XRBand sy() {
        curY += curStepY;
        return this;
    }

    public XRBand sx(int sx) {
        curX += sx;
        return this;
    }

    public XRBand sy(int sy) {
        curY += sy;
        return this;
    }

    public XRBand defEW(int w) {
        curDefaultElementW = w;
        return this;
    }

    public XRBand defEH(int h) {
        curDefaultElementH = h;
        return this;
    }

    public XRBand nextLine() {
        curY = curNextLineY;
        curX = curOffsetX;
        return this;
    }

    public XRBand startLine() {
        curX = curOffsetX;
        return this;
    }

    public XRBand startLine(int x) {
        curOffsetX = x;
        curX = x;
        return this;
    }

    public XRBand startSY() {
        return startLine().sy();
    }

    public XRBand mark() {
        markX = curX;
        markY = curY;
        markStepX = curStepX;
        markStepY = curStepY;
        return this;
    }

    public XRBand tomark() {
        curX = markX;
        curY = markY;
        curStepX = markStepX;
        curStepY = markStepY;
        return this;
    }

    public XRBand addAbs(XRElement e) {
        elements.add(e);
        return this;
    }

    public XRBand add(XRElement e) {
        e.xy(curX, curY);
        if (e.width() <= 0) e.w(curDefaultElementW);
        if (e.height() <= 0) e.h(curDefaultElementH);
        curStepX = e.width();
        curStepY = e.height();
        if (curStyle != null && !e.hasStyle()) e.style(curStyle);
        elements.add(e);
        curNextLineY = Math.max(curNextLineY, e.y() + e.height());
        return this;
    }

    public XRBand addX(XRElement e) {
        return add(e).sx();
    }

    public XRBand addY(XRElement e) {
        return add(e).sy();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Для построения по сетке.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected int[] gridColX, gridColW, gridRowY, gridRowH;
    protected int curGridCol, curGridRow;

    public XRBand cols(int... ws) {
        gridColW = ws;
        gridColX = new int[ws.length];
        for (int i = 0, x = 0; i < ws.length; i++) {
            gridColX[i] = curX + x;
            x += ws[i];
        }
        curGridCol = curGridRow = 0;
        return this;
    }

    public XRBand rows(int... hs) {
        gridRowH = hs;
        gridRowY = new int[hs.length];
        for (int i = 0, y = 0; i < hs.length; i++) {
            gridRowY[i] = curY + y;
            y += hs[i];
        }
        curGridCol = curGridRow = 0;
        return this;
    }

    public XRBand addG(int nx, int ny, int spanx, int spany, XRElement e) {
        int w = 0, h = 0;
        for (int i = 0; i < spanx; i++) w += gridColW[nx + i];
        for (int i = 0; i < spany; i++) h += gridRowH[ny + i];
        xy(gridColX[nx], gridRowY[ny]);
        if (w > 0 && h > 0) add(e.size(w, h)); // Добавляем только если размер видимый!
        curGridCol = nx;
        curGridRow = ny;
        return this;
    }

    public XRBand addG(int nx, int ny, XRElement e) {
        return addG(nx, ny, 1, 1, e);
    }

    public XRBand addGX(int spanx, int spany, XRElement e) {
        addG(curGridCol, curGridRow, spanx, spany, e);
        curGridCol += spanx;
        if (curGridCol >= gridColX.length) {
            curGridCol = 0;
            curGridRow++;
        }
        if (curGridRow >= gridRowY.length) {
            curGridRow = 0;
        }
        return this;
    }

    public XRBand addGX(int spanx, XRElement e) {
        return addGX(spanx, 1, e);
    }

    public XRBand addGX(XRElement e) {
        return addGX(1, 1, e);
    }
}
