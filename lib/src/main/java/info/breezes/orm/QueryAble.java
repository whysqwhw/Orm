package info.breezes.orm;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import info.breezes.orm.annotation.Column;
import info.breezes.orm.expressions.Limit;
import info.breezes.orm.expressions.OrderBy;
import info.breezes.orm.expressions.Where;
import info.breezes.orm.translator.IColumnTranslator;
import info.breezes.orm.utils.CursorUtils;
import info.breezes.orm.utils.TableUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Qiao on 2014/5/4.
 */
public class QueryAble<T> implements Iterable<T>, Iterator<T> {
    private Class<T> table;
    private SQLiteDatabase database;
    private ArrayList<Where> wheres;
    private ArrayList<OrderBy> orderBys;
    private ArrayList<String> params;
    private Limit limit;
    private String sql;
    private Cursor cursor;
    private Context mContext;
    private String tableName;

    private ArrayList<FCMap> fcMaps;

    public QueryAble(Class<T> table, SQLiteDatabase database, Context context) {
        this.table = table;
        this.database = database;
        this.mContext = context;
        wheres = new ArrayList<Where>();
        orderBys = new ArrayList<OrderBy>();
        params = new ArrayList<String>();
        fcMaps = new ArrayList<FCMap>();
    }

    public QueryAble<T> where(String column, Object value, String operation) {
        Where where = new Where(column, value, operation);
        if (!this.wheres.contains(where)) {
            this.wheres.add(where);
        }
        return this;
    }

    public QueryAble<T> or(String column, String value, String operation) {
        Where where = new Where(column, value, operation);
        where.condition = "OR";
        if (!this.wheres.contains(where)) {
            this.wheres.add(where);
        }
        return this;
    }

    public QueryAble<T> and(String column, String value, String operation) {
        Where where = new Where(column, value, operation);
        where.condition = "AND";
        if (!this.wheres.contains(where)) {
            this.wheres.add(where);
        }
        return this;
    }

    public QueryAble<T> orderBy(String column, String type) {
        OrderBy orderBy = new OrderBy(column, type);
        if (this.orderBys.contains(orderBy)) {
            this.orderBys.remove(orderBy);
        }
        this.orderBys.add(orderBy);
        return this;
    }

    public QueryAble<T> limit(int start, int count) {
        this.limit = new Limit(start, count);
        return this;
    }

    public QueryAble<T> execute() {
        long st = System.currentTimeMillis();
        if (cursor != null) {
            cursor.close();
        }
        sql = buildSQL();
        if (OrmConfig.Debug) {
            Log.i("ORM QueryAble", sql);
        }
        cursor = database.rawQuery(sql, params.toArray(new String[params.size()]));
        if (mContext != null) {
            cursor.setNotificationUri(mContext.getContentResolver(), Uri.parse("content://orm/" + tableName));
        }
        fcMaps.clear();
        Field fields[] = table.getFields();
        for (Field field : fields) {
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                String columnName = TableUtils.getColumnName(field, column);
                Class<?> fieldType = field.getType();
                FCMap fcMap = new FCMap();
                fcMap.field = field;
                fcMap.translator = OrmConfig.getTranslator(fieldType);
                fcMap.index = cursor.getColumnIndex(columnName);
                if (fcMap.index != -1) {
                    fcMaps.add(fcMap);
                }
            }
        }

        if (OrmConfig.Debug) {
            Log.d("QueryAble", "execute cost:" + (System.currentTimeMillis() - st));
        }
        return this;
    }

    public ArrayList<T> toList() {
        ArrayList<T> arrayList = new ArrayList<T>();
        for (T t : this) {
            arrayList.add(t);
        }
        return arrayList;
    }

    public T first() {
        if (this.iterator().hasNext()) {
            return this.next();
        }
        return null;
    }

    public void close() {
        cursor.close();
        cursor = null;
    }

    public int size() {
        return cursor.getCount();
    }

    public T get(int index) {
        if (cursor.moveToPosition(index)) {
            return readCurrentEntity();
        }
        return null;
    }

    public Cursor getCursor() {
        return this.cursor;
    }

    private String buildSQL() {
        tableName = TableUtils.getTableName(table);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SELECT * FROM ");
        stringBuilder.append(tableName);
        if (wheres.size() > 0) {
            Where where = wheres.get(0);
            stringBuilder.append(" WHERE ");
            stringBuilder.append(where.column);
            stringBuilder.append(where.operation);
            stringBuilder.append("?");
            params.add(where.value.toString());
            for (int i = 1; i < wheres.size(); i++) {
                where = wheres.get(i);
                stringBuilder.append(" ");
                stringBuilder.append(where.condition);
                stringBuilder.append(" ");
                stringBuilder.append(where.column);
                stringBuilder.append(where.operation);
                stringBuilder.append("?");
                params.add(where.value.toString());
            }
        }

        if (orderBys.size() > 0) {
            OrderBy orderBy = orderBys.get(0);
            stringBuilder.append(" ORDER BY ");
            stringBuilder.append(orderBy.column);
            stringBuilder.append(" ");
            stringBuilder.append(orderBy.type);
            for (int i = 1; i < orderBys.size(); i++) {
                orderBy = orderBys.get(i);
                stringBuilder.append(",");
                stringBuilder.append(orderBy.column);
                stringBuilder.append(" ");
                stringBuilder.append(orderBy.type);
            }
        }
        if (limit != null) {
            stringBuilder.append(" LIMIT ");
            stringBuilder.append(limit.start);
            stringBuilder.append(",");
            stringBuilder.append(limit.count);
        }
        return stringBuilder.toString();
    }

    private T readCurrentEntity() {
        return CursorUtils.readCurrentEntity(table, cursor, fcMaps);
    }

    @Override
    public Iterator<T> iterator() {
        if (cursor == null || cursor.isClosed()) {
            execute();
        }
        return this;
    }

    @Override
    public boolean hasNext() {
        return cursor.moveToNext();
    }

    @Override
    public T next() {
        return readCurrentEntity();
    }

    @Override
    public void remove() {
        throw new RuntimeException("QueryAble is Readonly.");
    }

}
