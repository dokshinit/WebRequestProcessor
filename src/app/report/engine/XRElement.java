/*
 * Copyright (c) 2015, Aleksey Nikolaevich Dokshin. All right reserved.
 * Contacts: dant.it@gmail.com, dokshin@list.ru.
 */
package app.report.engine;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.type.ModeEnum;

import java.awt.*;

/**
 * @author Докшин Алексей Николаевич <dant.it@gmail.com>
 */
@SuppressWarnings("unchecked")
public abstract class XRElement<E extends JRPrintElement, T extends XRElement<E, T>> {

    @FunctionalInterface
    public interface Actor<T> {
        void run(T src);
    }

    // Элемент отчёта.
    protected final E element;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public E JR() {
        return element;
    }

    protected XRElement(E element) {
        this.element = element;
    }

    // Перерассчитывает размеры\геметрию. Используется текстовым полем.
    public T pack() {
        return (T) this;
    }

    public int x() {
        return element.getX();
    }

    public int y() {
        return element.getY();
    }

    public int width() {
        return element.getWidth();
    }

    public int height() {
        return element.getHeight();
    }

    public T x(int x) {
        element.setX(x);
        return (T) this;
    }

    public T y(int y) {
        element.setY(y);
        return (T) this;
    }

    public T w(int dx) {
        element.setWidth(dx);
        return (T) this;
    }

    public T h(int dy) {
        element.setHeight(dy);
        return (T) this;
    }

    public T xy(int x, int y) {
        element.setX(x);
        element.setY(y);
        return (T) this;
    }

    public T size(int w, int h) {
        element.setWidth(w);
        element.setHeight(h);
        return (T) this;
    }

    public T bound(int x, int y, int w, int h) {
        element.setX(x);
        element.setY(y);
        element.setWidth(w);
        element.setHeight(h);
        return (T) this;
    }

    public T style(JRStyle style) {
        element.setStyle(style);
        return (T) this;
    }

    public boolean hasStyle() {
        return element.getStyle() != null;
    }

    public T mode(ModeEnum mode) {
        element.setMode(mode);
        return (T) this;
    }

    public T opaque() {
        element.setMode(ModeEnum.OPAQUE);
        return (T) this;
    }

    public T transparent() {
        element.setMode(ModeEnum.TRANSPARENT);
        return (T) this;
    }

    public T color(Color c) {
        element.setForecolor(c);
        return (T) this;
    }

    public T bgcolor(Color c) {
        element.setBackcolor(c);
        return (T) this;
    }

    public T key(String key, String value) {
        element.getPropertiesMap().setProperty(key, value);
        return (T) this;
    }

    public String key(String key) {
        return element.getPropertiesMap().getProperty(key);
    }
}
