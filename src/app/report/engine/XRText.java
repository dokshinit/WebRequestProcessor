/*
 * Copyright (c) 2015, Aleksey Nikolaevich Dokshin. All right reserved.
 * Contacts: dant.it@gmail.com, dokshin@list.ru.
 */
package app.report.engine;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.base.JRBasePrintText;
import net.sf.jasperreports.engine.base.JRBoxPen;
import net.sf.jasperreports.engine.type.*;
import net.sf.jasperreports.engine.util.JRTextMeasurerUtil;

/**
 * @author Докшин Алексей Николаевич <dant.it@gmail.com>
 */
public class XRText extends XRElement<JRBasePrintText, XRText> {

    private String originalText; // т.к. может портиться при паковке (обрезаться).
    private final JRTextMeasurerUtil measureUtil;

    public XRText(JRDefaultStyleProvider styleprovider,
                  JRTextMeasurerUtil measureutil) {
        super(new JRBasePrintText(styleprovider));
        this.measureUtil = measureutil;
    }

    @Override
    public XRText pack() {
        element.setText(originalText);
        measureUtil.measureTextElement(element);
        return this;
    }

    // Увеличивает высоту вплоть до максимальной, если текст не влезает.
    public XRText expandY(int maxheight) {
        int oldh = height();
        h(maxheight).pack();

        JRLineBox box = element.getLineBox();
        int newh = (int) Math.ceil(element.getTextHeight() + 0.1f); // Поправка на округление.
        int padh = box.getTopPadding() + box.getBottomPadding();
        //int lineh = (int) (box.getTopPen().getLineWidth() + box.getBottomPen().getLineWidth() + 0.5f);
        //logger.infof("EXP CALC: textH=%d padH=%d, oldH=%d newH=%d", newh, padh, oldh, newh + padh);

        h(Math.max(oldh, newh + padh));
        return this;
    }

    // Уменьшает высоту вплоть до минимальной, если текст влезает целиком.
    public XRText shrinkY(int minheight) {
        int oldh = height();
        pack();

        JRLineBox box = element.getLineBox();
        int newh = (int) (element.getTextHeight() + 0.5f);
        int padh = box.getTopPadding() + box.getBottomPadding();
        int lineh = (int) (box.getTopPen().getLineWidth() + box.getBottomPen().getLineWidth() + 0.5f);

        h(Math.min(oldh, Math.max(minheight, newh + padh + lineh)));
        return this;
    }

    public int calcRealWidth() {
        JRLineBox box = element.getLineBox();
        return element.getWidth() +
                (int) Math.round((box.getLeftPen().getLineWidth() + box.getRightPen().getLineWidth()) / 2 + 0.5);
    }

    public int calcRealHeight() {
        JRLineBox box = element.getLineBox();
        return element.getHeight() +
                (int) Math.round((box.getTopPen().getLineWidth() + box.getBottomPen().getLineWidth()) / 2 + 0.5);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public JRLineBox box() {
        return element.getLineBox();
    }

    public JRBoxPen pen() {
        return box().getPen();
    }

    public XRText lw(Float w) {
        box().getPen().setLineWidth(w);
        return this;
    }

    public XRText lwL(Float w) {
        box().getLeftPen().setLineWidth(w);
        return this;
    }

    public XRText lwR(Float w) {
        box().getRightPen().setLineWidth(w);
        return this;
    }

    public XRText lwLR(Float l, Float r) {
        box().getLeftPen().setLineWidth(l);
        box().getRightPen().setLineWidth(r);
        return this;
    }

    public XRText lwT(Float w) {
        box().getTopPen().setLineWidth(w);
        return this;
    }
    public XRText lwB(Float w) {
        box().getBottomPen().setLineWidth(w);
        return this;
    }

    public XRText lwTB(Float t, Float b) {
        box().getTopPen().setLineWidth(t);
        box().getBottomPen().setLineWidth(b);
        return this;
    }

    public XRText lw(Float l, Float r, Float t, Float b) {
        if (l == null || l >= 0) box().getLeftPen().setLineWidth(l);
        if (r == null || r >= 0) box().getRightPen().setLineWidth(r);
        if (t == null || t >= 0) box().getTopPen().setLineWidth(t);
        if (b == null || b >= 0) box().getBottomPen().setLineWidth(b);
        return this;
    }

    public XRText line(Actor<JRBoxPen> run) {
        if (run != null) run.run(pen());
        return this;
    }

    public XRText line(Actor<JRBoxPen> left, Actor<JRBoxPen> right, Actor<JRBoxPen> top, Actor<JRBoxPen> bottom) {
        if (left != null) left.run(box().getLeftPen());
        if (right != null) right.run(box().getRightPen());
        if (top != null) top.run(box().getTopPen());
        if (bottom != null) bottom.run(box().getBottomPen());
        return this;
    }

    public XRText lineT(Actor<JRBoxPen> top) {
        return line(null, null, top, null);
    }

    public XRText lineB(Actor<JRBoxPen> bottom) {
        return line(null, null, null, bottom);
    }

    public XRText lineTB(Actor<JRBoxPen> topbottom) {
        return line(null, null, topbottom, topbottom);
    }

    public XRText lineTB(Actor<JRBoxPen> top, Actor<JRBoxPen> bottom) {
        return line(null, null, top, bottom);
    }

    public XRText lineWidth(float width) {
        pen().setLineWidth(width);
        return this;
    }

    public XRText lineWidthL(float width) {
        box().getLeftPen().setLineWidth(width);
        return this;
    }

    public XRText lineWidthR(float width) {
        box().getRightPen().setLineWidth(width);
        return this;
    }

    public XRText lineWidthT(float width) {
        box().getTopPen().setLineWidth(width);
        return this;
    }

    public XRText lineWidthB(float width) {
        box().getBottomPen().setLineWidth(width);
        return this;
    }

    public XRText box(Actor<JRLineBox> run) {
        if (run != null) run.run(box());
        return this;
    }

    public XRText pad(int p) {
        box().setPadding(p);
        return this;
    }

    public XRText pad(int l, int r, int t, int b) {
        JRLineBox box = box();
        if (l >= 0) box.setLeftPadding(l);
        if (r >= 0) box.setRightPadding(r);
        if (t >= 0) box.setTopPadding(t);
        if (b >= 0) box.setBottomPadding(b);
        return this;
    }


    public XRText padL(int p) {
        box().setLeftPadding(p);
        return this;
    }

    public XRText padR(int p) {
        box().setRightPadding(p);
        return this;
    }

    public XRText padT(int p) {
        box().setTopPadding(p);
        return this;
    }

    public XRText padB(int p) {
        box().setBottomPadding(p);
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public XRText anchor(String name) {
        element.setAnchorName(name);
        return this;
    }

    public XRText linkRef(String ref) {
        element.setHyperlinkType(HyperlinkTypeEnum.REFERENCE);
        element.setHyperlinkReference(ref);
        return this;
    }

    public XRText linkAnchor(String ref, String anchor) {
        if (ref != null && !ref.isEmpty()) {
            element.setHyperlinkType(HyperlinkTypeEnum.REMOTE_ANCHOR);
            element.setHyperlinkReference(ref);
        } else {
            element.setHyperlinkType(HyperlinkTypeEnum.LOCAL_ANCHOR);
        }
        element.setHyperlinkAnchor(anchor);
        return this;
    }

    public XRText linkAnchor(String anchor) {
        return linkAnchor(null, anchor);
    }

    public XRText rotate(RotationEnum type) {
        element.setRotation(type);
        return this;
    }

    public XRText rotateRight() {
        return rotate(RotationEnum.RIGHT);
    }

    public XRText rotateLeft() {
        return rotate(RotationEnum.LEFT);
    }

    public XRText rotateUpsideDown() {
        return rotate(RotationEnum.UPSIDE_DOWN);
    }

    public XRText rotateNone() {
        return rotate(RotationEnum.NONE);
    }

    public XRText text(String fmt, Object... params) {
        originalText = params.length > 0 ? String.format(fmt, params) : fmt;
        element.setText(originalText);
        return this;
    }

    public XRText textHeight(float h) {
        element.setTextHeight(h);
        return this;
    }

    public XRText font(String font) {
        element.setFontName(font);
        return this;
    }

    public XRText bold(boolean is) {
        element.setBold(is);
        return this;
    }

    public XRText bold() {
        return bold(true);
    }

    public XRText italic(boolean is) {
        element.setItalic(is);
        return this;
    }

    public XRText italic() {
        return italic(true);
    }

    public XRText underline(boolean is) {
        element.setUnderline(is);
        return this;
    }

    public XRText strikeThrough(boolean is) {
        element.setStrikeThrough(is);
        return this;
    }

    public XRText fontSize(Float fsize) {
        element.setFontSize(fsize);
        return this;
    }

    public XRText pdfFontName(String font) {
        element.setPdfFontName(font);
        return this;
    }

    public XRText pdfEncoding(String enc) {
        element.setPdfEncoding(enc);
        return this;
    }

    public XRText pdfEmbedded(boolean is) {
        element.setPdfEmbedded(is);
        return this;
    }

    public XRText alignX(HorizontalTextAlignEnum horizontalAlignment) {
        element.setHorizontalTextAlign(horizontalAlignment);
        return this;
    }

    public XRText left() {
        element.setHorizontalTextAlign(HorizontalTextAlignEnum.LEFT);
        return this;
    }

    public XRText center() {
        element.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        return this;
    }

    public XRText centerIf(boolean is) {
        if (is) element.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        return this;
    }

    public XRText right() {
        element.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
        return this;
    }

    public XRText alignY(VerticalTextAlignEnum verticalAlignment) {
        element.setVerticalTextAlign(verticalAlignment);
        return this;
    }

    public XRText top() {
        element.setVerticalTextAlign(VerticalTextAlignEnum.TOP);
        return this;
    }

    public XRText middle() {
        element.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        return this;
    }

    public XRText bottom() {
        element.setVerticalTextAlign(VerticalTextAlignEnum.BOTTOM);
        return this;
    }

    public XRText alignL() {
        return left();
    }

    public XRText alignLT() {
        return left().top();
    }

    public XRText alignLM() {
        return left().middle();
    }

    public XRText alignLB() {
        return left().bottom();
    }

    public XRText alignC() {
        return center();
    }

    public XRText alignCT() {
        return center().top();
    }

    public XRText alignCM() {
        return center().middle();
    }

    public XRText alignCB() {
        return center().bottom();
    }

    public XRText alignR() {
        return right();
    }

    public XRText alignRT() {
        return right().top();
    }

    public XRText alignRM() {
        return right().middle();
    }

    public XRText alignRB() {
        return right().bottom();
    }

    public XRText alignT() {
        return top();
    }

    public XRText alignM() {
        return middle();
    }

    public XRText alignB() {
        return bottom();
    }
}
