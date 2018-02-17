package app.model;

import app.ExError;
import fbdbengine.FB_Connection;
import fbdbengine.FB_CustomException;
import fbdbengine.FB_Database;
import fbdbengine.FB_Query;
import util.StringTools;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;

import static app.App.logger;

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

    public void configureDB() throws ExError {
        try {
            db = new FB_Database(false, "127.0.0.1:WebCenter", "SYSDBA", "xxxxxxxx", "UTF-8", false);
        } catch (Exception ex) {
            throw new ExError("Ошибка настройки параметров БД!", ex);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Вспомогательный инструментарий для операций с БД.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /** Интерфейс для вызова обработчика операции с БД. */
    @FunctionalInterface
    interface QFBBeforeTask {
        void run() throws ExError;
    }

    /** Интерфейс для вызова обработчика операции с БД. */
    @FunctionalInterface
    interface QFBTask {
        void run(final FB_Connection con) throws ExError, SQLException, Exception;
    }

    @FunctionalInterface
    interface QFBErrorTask {
        void run(Throwable ex) throws ExError;
    }

    /** Хелпер для операций с БД. */
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

    /** Хелпер для операций с БД. Без обработчика до соединения с БД. */
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
            FB_Query q = con.execute("SELECT DTPROCESS FROM W_REQUEST_PROCESS(?,?,?,?,?)",
                    req.getId(), req.getState().id, req.getResult(), req.getFileName(), req.getFileSize());
            if (!q.next()) throw new ExError("Ошибка сохранения заявки при обработке!");
            req.updateByProcess(q.getLocalDateTime("DTPROCESS"));
            q.closeSafe();
            con.commit();
        });
    }

    public void updateRequestSend(Request req) throws ExError {
        QFB((con) -> {
            FB_Query q = con.execute("SELECT ISENDTRYREMAIN, DTSENDTRY, DTSEND FROM W_REQUEST_SEND(?,?,?)", req.getId(), req.getState().id, req.getResult());
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
}
