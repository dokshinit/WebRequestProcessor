/*
 * Copyright (c) 2015, Aleksey Nikolaevich Dokshin. All right reserved.
 * Contacts: dant.it@gmail.com, dokshin@list.ru.
 */
package app.report.engine;

import net.sf.jasperreports.engine.JRDefaultStyleProvider;
import net.sf.jasperreports.engine.base.JRBasePrintLine;
import net.sf.jasperreports.engine.type.LineDirectionEnum;
import net.sf.jasperreports.engine.type.LineStyleEnum;

/**
 * @author Докшин Алексей Николаевич <dant.it@gmail.com>
 */
public class XRLine extends XRElement<JRBasePrintLine, XRLine> {

    public XRLine(JRDefaultStyleProvider styleprovider) {
        super(new JRBasePrintLine(styleprovider));
    }

    @Override
    public XRLine pack() {
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public XRLine mirror(boolean is) {
        element.setDirection(is ? LineDirectionEnum.BOTTOM_UP : LineDirectionEnum.TOP_DOWN);
        return this;
    }

    public XRLine mirror() {
        return mirror(true);
    }

    public XRLine lineWidth(float w) {
        element.getLinePen().setLineWidth(w);
        return this;
    }

    public XRLine lineStyle(LineStyleEnum st) {
        element.getLinePen().setLineStyle(st);
        return this;
    }
}
