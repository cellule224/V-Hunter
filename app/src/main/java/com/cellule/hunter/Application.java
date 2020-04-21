package com.cellule.hunter;

import org.greenrobot.greendao.database.Database;

public class Application extends android.app.Application {

    private DaoSession daoSession;

    @Override
    public void onCreate() {
        super.onCreate();

        // Dev SQLite database
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "notes-db");
        Database db = helper.getWritableDb();


        daoSession = new DaoMaster(db).newSession();
    }

    public DaoSession getDaoSession() {
        return daoSession;
    }
}
