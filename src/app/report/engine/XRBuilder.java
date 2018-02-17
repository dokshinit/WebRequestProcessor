/*
 * Copyright (c) 2015, Aleksey Nikolaevich Dokshin. All right reserved.
 * Contacts: dant.it@gmail.com, dokshin@list.ru.
 */
package app.report.engine;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.base.JRBoxPen;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.type.*;
import net.sf.jasperreports.engine.util.JRTextMeasurerUtil;

import java.awt.*;

//import static app.App.logger;

/**
 * @author Докшин Алексей Николаевич <dant.it@gmail.com>
 */
public class XRBuilder {

    public static final JRDesignStyle defaultStyle;
    public static final JRDesignStyle tableStyle;
    public static final JRDesignStyle tableHeadStyle;
    public static final JRDesignStyle tableDetailStyle;
    public static final JRDesignStyle tableSubDetailStyle;
    public static final JRDesignStyle tableGroupStyle;
    public static final JRDesignStyle tableSubGroupStyle;
    public static final JRDesignStyle tableSumaryStyle;
    public static final JRDesignStyle cutLineStyle;

    public static final SimplePrintPageFormat pageFormat_A4_PORTRAIT;
    public static final SimplePrintPageFormat pageFormat_A4_LANDSCAPE;

    static {
        // Стиль по умолчанию (шрифт + параметры экспорта в PDF)
        defaultStyle = new JRDesignStyle();
        defaultStyle.setName("Default");
        defaultStyle.setDefault(true);
        defaultStyle.setFontName("XO Oriel"); // "Liberation Sans"
        defaultStyle.setFontSize(10f);
        defaultStyle.setPdfEncoding("UTF-8");

        // Стиль для таблиц
        tableStyle = new JRDesignStyle();
        tableStyle.setName("Table");
        tableStyle.setParentStyle(defaultStyle);
        tableStyle.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        JRLineBox box = tableStyle.getLineBox();
        box.setLeftPadding(1);
        box.setRightPadding(1);
        JRBoxPen pen = box.getPen();
        pen.setLineStyle(LineStyleEnum.SOLID);
        pen.setLineColor(Color.BLACK);
        pen.setLineWidth(0.4f);

        // Стиль для заголовков колонок в таблицах
        tableHeadStyle = new JRDesignStyle();
        tableHeadStyle.setName("TableHead");
        tableHeadStyle.setParentStyle(tableStyle);
        tableHeadStyle.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        tableHeadStyle.setFontSize(10.5f);
        tableHeadStyle.setBold(true);
        tableHeadStyle.setMode(ModeEnum.OPAQUE);
        tableHeadStyle.setBackcolor(Color.lightGray);

        // Стиль для таблиц
        tableDetailStyle = new JRDesignStyle();
        tableDetailStyle.setName("TableDetail");
        tableDetailStyle.setParentStyle(tableStyle);
        tableDetailStyle.setFontSize(8f);
        tableDetailStyle.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
        box = tableDetailStyle.getLineBox();
        box.setLeftPadding(2);
        box.setRightPadding(2);

        tableSubDetailStyle = new JRDesignStyle();
        tableSubDetailStyle.setName("TableSubDetail");
        tableSubDetailStyle.setParentStyle(tableDetailStyle);
        tableSubDetailStyle.setFontSize(7f);
        box = tableSubDetailStyle.getLineBox();
        box.getTopPen().setLineWidth(0.1f);
        box.getTopPen().setLineColor(Color.gray);
        box.getBottomPen().setLineWidth(0.1f);
        box.getBottomPen().setLineColor(Color.gray);

        // Стиль для группировки
        tableGroupStyle = new JRDesignStyle();
        tableGroupStyle.setName("TableGroup");
        tableGroupStyle.setParentStyle(tableDetailStyle);
        tableGroupStyle.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
        tableGroupStyle.setFontSize(9.5f);
        tableGroupStyle.setBold(true);
        tableGroupStyle.setMode(ModeEnum.OPAQUE);

        // Стиль для вложенной группировки (группировки второго порядка)
        tableSubGroupStyle = new JRDesignStyle();
        tableSubGroupStyle.setName("TableSubGroup");
        tableSubGroupStyle.setParentStyle(tableGroupStyle);
        tableSubGroupStyle.setFontSize(9f);

        // Стиль для таблиц
        tableSumaryStyle = new JRDesignStyle();
        tableSumaryStyle.setName("TableSum");
        tableSumaryStyle.setParentStyle(tableDetailStyle);
        tableSumaryStyle.setFontSize(10.5f);
        tableSumaryStyle.setBold(true);

        // Стиль линии отреза
        cutLineStyle = new JRDesignStyle();
        cutLineStyle.setName("CutLine");
        cutLineStyle.setParentStyle(defaultStyle);
        cutLineStyle.setFontSize(6f);
        cutLineStyle.setBold(false);
        cutLineStyle.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        cutLineStyle.setForecolor(Color.gray);
        box = cutLineStyle.getLineBox();
        box.setPadding(0);
        pen = box.getPen();
        pen.setLineWidth(0);
        pen.setLineStyle(LineStyleEnum.DASHED);
        pen.setLineColor(Color.LIGHT_GRAY);

        // Преопределенные форматы страниц.
        pageFormat_A4_PORTRAIT = new SimplePrintPageFormat();
        pageFormat_A4_PORTRAIT.setOrientation(OrientationEnum.PORTRAIT);
        pageFormat_A4_PORTRAIT.setPageWidth(595);
        pageFormat_A4_PORTRAIT.setPageHeight(842);
        pageFormat_A4_PORTRAIT.setTopMargin(20);
        pageFormat_A4_PORTRAIT.setBottomMargin(20);
        pageFormat_A4_PORTRAIT.setLeftMargin(40);
        pageFormat_A4_PORTRAIT.setRightMargin(20);

        pageFormat_A4_LANDSCAPE = new SimplePrintPageFormat();
        pageFormat_A4_LANDSCAPE.setOrientation(OrientationEnum.LANDSCAPE);
        pageFormat_A4_LANDSCAPE.setPageWidth(842);
        pageFormat_A4_LANDSCAPE.setPageHeight(595);
        pageFormat_A4_LANDSCAPE.setTopMargin(20);
        pageFormat_A4_LANDSCAPE.setBottomMargin(20);
        pageFormat_A4_LANDSCAPE.setLeftMargin(40);
        pageFormat_A4_LANDSCAPE.setRightMargin(20);
    }

    // Отчёт.
    protected final JasperPrint jasperPrint;
    // Стили.
    protected final JRDefaultStyleProvider defaultStyleProvider;
    // Калькулятор метрик.
    protected final JRTextMeasurerUtil measurerUtil;
    //
    protected PrintPageFormat defaultPageFormat;

    //
    protected int defaultHeaderH, defaultFooterH;

    public XRBuilder() {

        jasperPrint = new JasperPrint();
        // Дефолтные параметры страницы.
        setDefaultPageFormat(pageFormat_A4_PORTRAIT);
        //
        defaultStyleProvider = jasperPrint.getDefaultStyleProvider();
        measurerUtil = JRTextMeasurerUtil.getInstance(DefaultJasperReportsContext.getInstance());

        defaultHeaderH = 0;
        defaultFooterH = 0;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public XRBuilder setDefaultPageFormat(PrintPageFormat pf) {
        defaultPageFormat = pf;
        jasperPrint.setPageWidth(pf.getPageWidth());
        jasperPrint.setPageHeight(pf.getPageHeight());
        jasperPrint.setOrientation(pf.getOrientation());
        jasperPrint.setLeftMargin(pf.getLeftMargin());
        jasperPrint.setTopMargin(pf.getTopMargin());
        jasperPrint.setRightMargin(pf.getRightMargin());
        jasperPrint.setBottomMargin(pf.getBottomMargin());
        // Дефолтная часть с параметрами страницы.
        setPartFormat(0, "default", defaultPageFormat);
        return this;
    }

    public XRBuilder setPartFormat(int pageindex, String name, PrintPageFormat pf) {
        SimplePrintPart pp = new SimplePrintPart();
        pp.setName(name);
        pp.setPageFormat(pf);
        jasperPrint.addPart(pageindex, pp);
        return this;
    }

    public XRBuilder setPartFormat_A4P(int pageindex) {
        return setPartFormat(pageindex, "A4_P", pageFormat_A4_PORTRAIT);
    }

    public XRBuilder setPartFormat_A4L(int pageindex) {
        return setPartFormat(pageindex, "A4_L", pageFormat_A4_LANDSCAPE);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public JasperPrint getPrint() {
        return jasperPrint;
    }

    public XRBuilder defaultColontituls(int head, int foot) {
        defaultHeaderH = head;
        defaultFooterH = foot;
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public int getPageCount() {
        return jasperPrint.getPages().size();
    }

    // Создаётся страница и добавляется в отчёт.
    public XRPage newPage(PrintPageFormat fmt) {
        if (fmt == null) fmt = defaultPageFormat;
        XRPage page = new XRPage(fmt).colontituls(defaultHeaderH, defaultFooterH);
        int n = getPageCount();
        if (n > 0) {
            PrintPageFormat pf = jasperPrint.getParts().getPageFormat(n);
            if (pf != fmt) {
                setPartFormat(n, "page" + (n + 1), fmt);
                //logger.infof("ADD PART: page=%d fmt=%s", n + 1, fmt.toString());
            }
        } else {
            setPartFormat(0, "page1", fmt);
            //logger.infof("ADD PART0: page=%d fmt=%s", n, fmt.toString());
        }
        jasperPrint.addPage(page.getPage());
        return page;
    }

    public XRPage newPage() {
        return newPage(null);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public XRLine crLine() {
        return new XRLine(defaultStyleProvider);
    }

    public XRLine crLine(Color c) {
        return crLine().color(c);
    }

    public XRText crText(String title) {
        return new XRText(defaultStyleProvider, measurerUtil).size(0, 0).text(title);
    }

    public XRText crText(int width, String title) {
        return crText(title).size(width, 0);
    }

    public XRText crText(int width, int height, String title) {
        return crText(title).size(width, height);
    }
}
