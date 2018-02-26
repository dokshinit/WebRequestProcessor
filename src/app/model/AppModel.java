package app.model;

import app.App;
import app.ExError;
import app.ReportsMailer;
import app.report.BaseReport;
import app.report.ClientCardReport;
import app.report.ClientTransactionReport;
import app.report.ClientTurnoverReport;
import fbdbengine.FB_Connection;
import fbdbengine.FB_CustomException;
import fbdbengine.FB_Database;
import fbdbengine.FB_Query;
import net.sf.jasperreports.engine.JRException;
import util.StringTools;
import xconfig.XConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static app.App.isUI;
import static app.App.logger;
import static app.model.Helper.fmtDT84;
import static util.DateTools.toMillis;
import static util.StringTools.isEmptySafe;

/**
 * Модель приложения (сессии пользователя)
 *
 * @author Aleksey Dokshin <dant.it@gmail.com> (28.11.17).
 */
public class AppModel {

    private FB_Database db;

    public FB_Database db() {
        return db;
    }

    public AppModel() {
    }

    public int redrawInterval;
    public String statePath;
    public ServiceModel procModel, sendModel;

    public void init() throws ExError {
        int p_loadsize, s_loadsize;
        int p_delay, s_delay;
        String base, user, password;

        logger.infof("Загрузка конфигурации...");
        try {
            XConfig cfg = new XConfig();
            cfg.load("app.config");

            base = cfg.getKey("db.host", "127.0.0.1") + ":" + cfg.getKey("db.alias", "WebCenter");
            user = cfg.getKey("db.user", "REQUESTPROCESSOR");
            password = cfg.getKey("db.password", "xxxxxxxx");

            p_loadsize = cfg.getIntKey("processor.loadsize", 10);
            p_delay = cfg.getIntKey("processor.delay", 5000);

            s_loadsize = cfg.getIntKey("sender.loadsize", 10);
            s_delay = cfg.getIntKey("sender.delay", 5000);

            if (isUI) {
                redrawInterval = cfg.getIntKey("ui.redraw", 250);
            } else {
                redrawInterval = cfg.getIntKey("noui.redraw", 5000);
                statePath = cfg.getKey("noui.path", "./state");
            }

        } catch (Exception ex) {
            base = "127.0.0.1:WebCenter";
            user = "REQUESTPROCESSOR";
            password = "xxxxxxxx";
            p_loadsize = s_loadsize = 10;
            p_delay = s_delay = 5000;
            redrawInterval = isUI ? 250 : 5000;
            statePath = "./state";

            logger.infof("Ошибка загрузки конфигурации: %s! Приняты параметры по умолчанию!", ex.getMessage());
        }


        logger.infof("Настройка подключения к БД...");
        try {
            db = new FB_Database(false, base, user, password, "UTF-8", false);
        } catch (Exception ex) {
            throw new ExError("Ошибка настройки параметров БД!", ex);
        }


        procModel = new ServiceModel(ServiceModel.Kind.PROCESSOR, p_loadsize, p_delay);
        sendModel = new ServiceModel(ServiceModel.Kind.SENDER, s_loadsize, s_delay);

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Вспомогательный инструментарий для операций с БД.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Интерфейс для вызова обработчика операции с БД.
     */
    @FunctionalInterface
    interface QFBBeforeTask {
        void run() throws ExError;
    }

    /**
     * Интерфейс для вызова обработчика операции с БД.
     */
    @FunctionalInterface
    interface QFBTask {
        void run(final FB_Connection con) throws ExError, SQLException, Exception;
    }

    @FunctionalInterface
    interface QFBErrorTask {
        void run(Throwable ex) throws ExError;
    }

    /**
     * Хелпер для операций с БД.
     */
    void QFB(FB_Connection con, QFBBeforeTask btask, QFBTask task, QFBErrorTask etask) throws ExError {
        if (btask != null) btask.run();
        boolean isextcon = con != null;
        // Если не внешнее - открываем локальное (будет закрыто автоматически с роллбэк).
        if (!isextcon) {
            try {
                con = db().connect(); // Соединение

            } catch (Exception ex) {
                if (etask != null) etask.run(ex);
                FB_CustomException e = FB_CustomException.parse(ex);
                if (e != null) throw new ExError(ex, "Ошибка подключения к БД: %s", e.name + ": " + e.message);
                logger.error("Ошибка подключения к БД!", ex);
                throw new ExError(ex, "Ошибка подключения к БД! Детальная информация в логе.");
            }
        }
        // Соединение установлено (или уже было открыто - если внешнее).
        try {
            task.run(con);

        } catch (ExError ex) {
            if (etask != null) etask.run(ex);
            logger.error(ex.getMessage());
            throw ex;

        } catch (Exception ex) {
            if (etask != null) etask.run(ex);
            FB_CustomException e = FB_CustomException.parse(ex);
            if (e != null) throw new ExError(ex, "Ошибка операции БД: %s", e.name + ": " + e.message);
            logger.error("Ошибка операции БД!", ex);
            throw new ExError(ex, "Ошибка операции БД! Детальная информация в логе.");

        } finally {
            // Если не внешнее - закрываем с роллбэк (если нужно сохранение данных - это надо сделать в теле задачи).
            if (!isextcon) FB_Connection.closeSafe(con);
        }
    }

    /**
     * Хелпер для операций с БД. Без обработчика до соединения с БД.
     */
    void QFB(FB_Connection con, QFBTask task) throws ExError {
        QFB(con, null, task, null);
    }

    void QFB(QFBTask task) throws ExError {
        QFB(null, null, task, null);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public Client loadClient(int iddclient, int iddsub, LocalDate dtw) throws ExError {
        Client[] res = {null};
        QFB((con) -> {
            FB_Query q = con.execute("SELECT IDDFIRM, IDDCLIENT, IDDSUB, CINN, CNAME, CTITLE, CSUBTITLE, CADDRESS, CEMAIL," +
                    " CPHONE, IBFOND, IBWORK, COILLIMIT, CAZSLIMIT, CCOMMENT, DBCREDIT" +
                    " FROM WP_CLIENT_GET(?,?,?)", iddclient, iddsub, dtw);
            if (!q.next()) throw new ExError("Клиент не найден!");
            res[0] = new Client(q.getInteger("IDDFIRM"), q.getInteger("IDDCLIENT"), q.getInteger("IDDSUB"),
                    q.getString("CINN"), q.getString("CNAME"), q.getString("CTITLE"), q.getString("CSUBTITLE"),
                    q.getString("CADDRESS"), q.getString("CEMAIL"), q.getString("CPHONE"),
                    q.getIntegerAsBoolean("IBFOND"), q.getInteger("IBWORK"),
                    q.getString("COILLIMIT"), q.getString("CAZSLIMIT"), q.getString("CCOMMENT"),
                    q.getLong("DBCREDIT"));
            q.closeSafe();
        });
        return res[0];
    }

    public ArrayList<Request> loadRequests(Request.State state, int maxcount) throws ExError {
        ArrayList<Request> list = new ArrayList<>();
        QFB((con) -> {
            FB_Query q = con.execute("SELECT FIRST " + maxcount
                    + " ID, IDDCLIENT, IDDSUB, DTCREATE, ITYPE, ISUBTYPE, CPARAMSTITLE, CPARAMS, CCOMMENT, ISTATE, DTPROCESS, CFILENAME, "
                    + " IFILESIZE, ISENDTRYREMAIN, DTSENDTRY, DTSEND, CRESULT FROM WP_REQUEST_LIST(?) ORDER BY DTCREATE", state.id);
            while (q.next()) {
                list.add(new Request(
                        q.getInteger("ID"), q.getInteger("IDDCLIENT"), q.getInteger("IDDSUB"), q.getLocalDateTime("DTCREATE"),
                        q.getInteger("ITYPE"), q.getInteger("ISUBTYPE"), q.getString("CPARAMSTITLE"), q.getString("CPARAMS"),
                        q.getString("CCOMMENT"), q.getInteger("ISTATE"),
                        q.getLocalDateTime("DTPROCESS"), q.getString("CFILENAME"), q.getInteger("IFILESIZE"),
                        q.getInteger("ISENDTRYREMAIN"), q.getLocalDateTime("DTSENDTRY"),
                        q.getLocalDateTime("DTSEND"), q.getString("CRESULT")));
            }
            q.closeSafe();
        });
        return list;
    }

    public void updateRequestProcess(Request req) throws ExError {
        QFB((con) -> {
            FB_Query q = con.execute("SELECT DTPROCESS FROM WP_REQUEST_PROCESS(?,?,?,?,?)",
                    req.getId(), req.getState().id, req.getResult(), req.getFileName(), req.getFileSize());
            if (!q.next()) throw new ExError("Ошибка сохранения заявки при обработке!");
            req.updateByProcess(q.getLocalDateTime("DTPROCESS"));
            q.closeSafe();
            con.commit();
        });
    }

    public void updateRequestSend(Request req) throws ExError {
        QFB((con) -> {
            FB_Query q = con.execute("SELECT ISENDTRYREMAIN, DTSENDTRY, DTSEND FROM WP_REQUEST_SEND(?,?,?)", req.getId(), req.getState().id, req.getResult());
            if (!q.next()) throw new ExError("Ошибка сохранения заявки при ответе!");
            req.updateBySend(q.getInteger("ISENDTRYREMAIN"), q.getLocalDateTime("DTSENDTRY"), q.getLocalDateTime("DTSEND"));
            q.closeSafe();
            con.commit();
        });
    }

    public ArrayList<Transaction> loadClientTransactions(Integer iddfirm, Integer iddclient, Integer iddsub, LocalDate dtstart,
                                                         LocalDate dtend, Integer iddazs,
                                                         int offset, int limit, String sort) throws ExError {
        ArrayList<Transaction> list = new ArrayList<>();
        QFB((con) -> {
            FB_Query q = con.execute("SELECT FIRST " + limit + " SKIP " + offset + " "
                            + "DTSTART, DTEND, IDDCARD, CCARD, IDD, IDDAZS, IDDTRK, IDDOIL, IACCTYPE, DBPRICE, DBVOLREQ, DBVOLUME, DBSUMMA "
                            + "FROM WP_REPORT_CLIENTTRANS(?,?,?,NULL, ?,?,?) "
                            + (StringTools.isEmptySafe(sort) ? "" : " ORDER BY " + sort),
                    iddfirm, iddclient, iddsub, dtstart, dtend, iddazs);
            while (q.next()) {
                list.add(new Transaction(
                        q.getLocalDateTime("DTSTART"),
                        q.getLocalDateTime("DTEND"),
                        q.getString("IDDCARD"),
                        q.getString("CCARD"),
                        q.getInteger("IDD"),
                        q.getInteger("IDDAZS"),
                        q.getInteger("IDDTRK"),
                        q.getInteger("IDDOIL"),
                        q.getInteger("IACCTYPE"),
                        q.getLong("DBPRICE"),
                        q.getLong("DBVOLREQ"),
                        q.getLong("DBVOLUME"),
                        q.getLong("DBSUMMA")));
            }
            q.closeSafe();
        });
        return list;
    }

    public ArrayList<Saldo> loadClientSaldos(Integer iddfirm, Integer iddclient, Integer iddsub, LocalDate dtw) throws ExError {
        final ArrayList<Saldo> list = new ArrayList<>();
        QFB((con) -> {
            FB_Query q = con.execute("SELECT IDDACC, IACCTYPE, IDDOIL, DBSALDO "
                            + "FROM WP_REPORT_CLIENTTURNOVER_SALDO(?,?,?,?) ORDER BY IACCTYPE, IDDOIL",
                    iddfirm, iddclient, iddsub, dtw);
            while (q.next()) {
                list.add(new Saldo(q.getInteger("IDDACC"), q.getInteger("IACCTYPE"), q.getInteger("IDDOIL"),
                        q.getLong("DBSALDO")));
            }
            q.closeSafe();
        });
        return list;
    }

    public ArrayList<Pay> loadClientPays(Integer iddfirm, Integer iddclient, Integer iddsub, LocalDate dtstart, LocalDate dtend) throws ExError {
        final ArrayList<Pay> list = new ArrayList<>();
        QFB((con) -> {
            FB_Query q = con.execute("SELECT DTDOC, CDOC, IACCTYPE, IDDOIL, DBVOLUME, DBSUMMA "
                            + "FROM WP_REPORT_CLIENTTURNOVER_PAY(?,?,?,?,?) ORDER BY DTDOC, CDOC",
                    iddfirm, iddclient, iddsub, dtstart, dtend);
            while (q.next()) {
                list.add(new Pay(q.getLocalDate("DTDOC"), q.getString("CDOC"), q.getInteger("IACCTYPE"), q.getInteger("IDDOIL"),
                        q.getLong("DBVOLUME"), q.getLong("DBSUMMA")));
            }
            q.closeSafe();
        });
        return list;
    }

    public ArrayList<Sale> loadClientSales(Integer iddfirm, Integer iddclient, Integer iddsub, LocalDate dtstart, LocalDate dtend) throws ExError {
        final ArrayList<Sale> list = new ArrayList<>();
        QFB((con) -> {
            FB_Query q = con.execute("SELECT IACCTYPE, IDDSUB, IDDOIL, DBPRICE, DBVOLUME, DBSUMMA "
                            + "FROM WP_REPORT_CLIENTTURNOVER_SALE(?,?,?,?,?) ORDER BY IACCTYPE, IDDOIL, DBPRICE",
                    iddfirm, iddclient, iddsub, dtstart, dtend);
            while (q.next()) {
                list.add(new Sale(q.getInteger("IACCTYPE"), q.getInteger("IDDOIL"),
                        q.getLong("DBPRICE"), q.getLong("DBVOLUME"), q.getLong("DBSUMMA")));
            }
            q.closeSafe();
        });
        return list;
    }

    public ArrayList<Card> loadClientCards(Integer iddfirm, Integer iddclient, Integer iddsub, LocalDate dtw, Card.WorkState workstate, String sort) throws ExError {
        final ArrayList<Card> list = new ArrayList<>();
        QFB((con) -> {
            FB_Query q = con.execute("SELECT DTW, DTWEND, IDD, IACCTYPE, IBWORK, DTPAY, CDRIVER, CCAR, DBDAYLIMIT, CCOMMENT "
                            + " FROM WP_REPORT_CLIENTCARD(?,?,?,?,?) "
                            + (StringTools.isEmptySafe(sort) ? "" : " ORDER BY " + sort),
                    iddfirm, iddclient, iddsub, dtw, workstate == null ? null : workstate.id);
            while (q.next()) {
                list.add(new Card(q.getLocalDate("DTW"), q.getLocalDate("DTWEND"), q.getString("IDD"),
                        q.getInteger("IACCTYPE"), q.getInteger("IBWORK"), q.getLocalDate("DTPAY"), q.getString("CDRIVER"),
                        q.getString("CCAR"), q.getLong("DBDAYLIMIT"), q.getString("CCOMMENT")));
            }
            q.closeSafe();
        });
        return list;
    }


    /**
     * Создание каталога, если не существует.
     */
    public static File createDirectoryIfNotExist(String path) throws ExError {
        try {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    throw new ExError("Ошибка создания каталога!");
                }
            }
            return dir;
        } catch (ExError ex) {
            throw ex;
        } catch (Exception e) {
            throw new ExError(e, "Ошибка создания каталога!");
        }
    }

    /**
     * Безопасное удаление файла.
     */
    public static void deleteFileSafe(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                // Если не удалился сразу - пробуем удалить при выходе на случай, если залочен.
                if (!file.delete()) file.deleteOnExit();
            }
        } catch (Exception ignore) {
        }
    }
}
