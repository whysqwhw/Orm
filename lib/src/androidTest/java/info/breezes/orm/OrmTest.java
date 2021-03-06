package info.breezes.orm;

import android.annotation.TargetApi;
import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.test.AndroidTestCase;
import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;
import junit.framework.TestResult;

import java.io.File;
import java.util.ArrayList;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class OrmTest extends AndroidTestCase {

    String TAG = "orm.test";

    TestOrmSQLiteHelper helper;
    boolean over;

    @Override
    public void run(TestResult result) {
        long st = System.currentTimeMillis();
        super.run(result);
        Log.d(TAG, getName() + ":" + (System.currentTimeMillis() - st));
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Log.d(TAG, "setUp");
        OrmConfig.Debug = false;
        helper = new TestOrmSQLiteHelper(getContext(), "test.db", null, 1);
    }

    @SmallTest
    public void testACreateTable() throws Exception {
        SQLiteDatabase database = helper.getCurrentDatabase(true);
        assertNotNull(database);
    }

    @SmallTest
    public void testBInsert500() throws Exception {
        for (int i = 0; i < 500; i++) {
            Employee employee = new Employee();
            helper.insert(employee);
        }
    }

    @SmallTest
    public void testCInsertAll500() throws Exception {
        ArrayList<Employee> employees = new ArrayList<Employee>();
        for (int i = 0; i < 500; i++) {
            Employee employee = new Employee();
            employees.add(employee);
        }
        helper.insertAll(employees.toArray());
    }

    @SmallTest
    public void testDCount() throws Exception {
        int count = helper.query(Employee.class).execute().size();
        Log.d(TAG, "Count:" + count);
    }


    @SmallTest
    public void testESelect500ToList() throws Exception {
        ArrayList<Employee> employees = helper.query(Employee.class).limit(0, 500).execute().toList();
        if (employees.size() < 1) {
            fail("query to list error.");
        } else {
            Log.d(TAG, "ListSize:" + employees.size());
        }
    }


    @SmallTest
    public void testFDeleteAll() throws Exception {
        helper.clear(Employee.class);
        over = true;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void tearDown() throws Exception {
        Log.d(TAG, "tearDown");
        if (over) {
            Log.d(TAG, "Clean.");
            File file = new File(helper.getCurrentDatabase(false).getPath());
            helper.getCurrentDatabase(false).close();
            SQLiteDatabase.deleteDatabase(file);
        }
        super.tearDown();
    }
}