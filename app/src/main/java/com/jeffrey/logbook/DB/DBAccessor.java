package com.jeffrey.logbook.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import com.jeffrey.logbook.Set;

import java.sql.SQLInput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Jeffrey on 5/3/2016.
 */
public class DBAccessor {

    private DBHelper helper;

    private Context c;

    public DBAccessor(Context c) {
        helper = new DBHelper(c);
        this.c = c;
    }

    public void finishWorkout(HashMap<String, List<Set>> map, String date) {
        for(String name : map.keySet()) {
            if(map.get(name).size() == 0){
                Toast.makeText(c, "Cannot submit exercise with no completed sets", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        SQLiteDatabase d = helper.getReadableDatabase();

        String[] projection = {
                helper.WORKOUTS_HEADER_ID
        };
        String[] selectionArgs = {date};

        Cursor c = d.query(helper.WORKOUTS_TABLE_NAME,
                projection,
                helper.WORKOUTS_HEADER_DATE + " = ?",
                selectionArgs,
                null,
                null,
                null
        );

        if(c.getCount() != 0) {
            c.moveToFirst();
            while(true) {
                long id = c.getLong(c.getColumnIndex(helper.WORKOUTS_HEADER_ID));
                d.delete(helper.SETS_TABLE_NAME, helper.WORKOUTS_HEADER_ID + " = ?", new String[]{String.valueOf(id)});
                d.delete(helper.WORKOUTS_TABLE_NAME, helper.WORKOUTS_HEADER_ID + " = ?", new String[]{String.valueOf(id)});
                if(c.isLast()) {
                    break;
                }
                c.moveToNext();
            }

            d.close();
        }

        SQLiteDatabase db = helper.getWritableDatabase();

        for(String exercise : map.keySet()) {
            ContentValues exerciseValues = new ContentValues();
            exerciseValues.put(helper.WORKOUTS_HEADER_NAME, exercise);
            exerciseValues.put(helper.WORKOUTS_HEADER_DATE, date);

            long id = db.insert(helper.WORKOUTS_TABLE_NAME, null, exerciseValues);
            for(Set set : map.get(exercise)) {
                if(set.getReps() != 0) {
                    ContentValues setValues = new ContentValues();
                    setValues.put(helper.SETS_HEADER_WEIGHT, set.getWeight());
                    setValues.put(helper.SETS_HEADER_REPS, set.getReps());
                    setValues.put(helper.WORKOUTS_HEADER_ID, id);
                    db.insert(helper.SETS_TABLE_NAME, null, setValues);
                }
            }
        }
        db.close();
        helper.close();
    }

    public void retrieveWorkout(String date, List<String> exercises, HashMap<String, List<Set>> sets) {
        SQLiteDatabase db = helper.getReadableDatabase();

        String[] projection = {
                helper.WORKOUTS_HEADER_ID,
                helper.WORKOUTS_HEADER_NAME
        };
        String[] selectionArgs = new String[]{date};
        Cursor c = db.query(helper.WORKOUTS_TABLE_NAME,
                projection,
                helper.WORKOUTS_HEADER_DATE + " = ?",
                selectionArgs,
                null,
                null,
                null);
        if(c.getCount() != 0 ) {
            c.moveToFirst();
            while(true) {
                String name = c.getString(c.getColumnIndex(helper.WORKOUTS_HEADER_NAME));
                exercises.add(name);
                long id = c.getLong(c.getColumnIndex(helper.WORKOUTS_HEADER_ID));
                String[] projection2 = {
                        helper.SETS_HEADER_WEIGHT,
                        helper.SETS_HEADER_REPS
                };
                Cursor c2 = db.query(helper.SETS_TABLE_NAME,
                        projection2,
                        helper.WORKOUTS_HEADER_ID + " = ?",
                        new String[]{String.valueOf(id)},
                        null,
                        null,
                        null
                );
                ArrayList<Set> list = new ArrayList<Set>();
                c2.moveToFirst();
                if(c2.getCount() != 0) {
                    while(true) {
                        list.add(new Set(c2.getInt(c2.getColumnIndex(helper.SETS_HEADER_WEIGHT)), c2.getInt(c2.getColumnIndex(helper.SETS_HEADER_REPS))));
                        if(c2.isLast()) {
                            break;
                        }
                        c2.moveToNext();
                    }
                }
                list.add(list.size(), new Set(0, 0));   //dummy
                sets.put(name, list);
                if(c.isLast()) {
                    break;
                }
                c.moveToNext();
            }
        }
        db.close();
    }

    public String[] retrieveHistoricExercises() {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.query(true, helper.WORKOUTS_TABLE_NAME, new String[]{helper.WORKOUTS_HEADER_NAME}, null, null, null, null, null, null);
        if(c.getCount() == 0)
            return new String[] {};
        String[] names  = new String[c.getCount()];
        c.moveToFirst();
        for(int i = 0; i < c.getCount(); i++) {
            names[i] = c.getString(c.getColumnIndex(helper.WORKOUTS_HEADER_NAME));
            c.moveToNext();
        }
        db.close();
        return names;
    }

    public String retrieveAllExercisesAsString(String delimiter) {
        String s = "";

        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor c = db.query(helper.WORKOUTS_TABLE_NAME, new String[]{helper.WORKOUTS_HEADER_ID, helper.WORKOUTS_HEADER_NAME, helper.WORKOUTS_HEADER_DATE}, null, null, null, null, null);
        if(c.getCount() == 0)
            return s;
        c.moveToFirst();
        for(int i = 0; i < c.getCount(); i++) {
            s += (c.getString(c.getColumnIndex(helper.WORKOUTS_HEADER_DATE)) + delimiter + c.getString(c.getColumnIndex(helper.WORKOUTS_HEADER_NAME)) + delimiter);

            Cursor d = db.query(helper.SETS_TABLE_NAME,
                    new String[]{helper.SETS_HEADER_WEIGHT, helper.SETS_HEADER_REPS},
                    helper.WORKOUTS_HEADER_ID + " = ?",
                    new String[]{c.getString(c.getColumnIndex(helper.WORKOUTS_HEADER_ID))},
                    null,
                    null,
                    null);
            d.moveToFirst();
            for(int j = 0; j < d.getCount(); j++) {
                s += (d.getDouble(d.getColumnIndex(helper.SETS_HEADER_WEIGHT)) + delimiter + d.getInt(d.getColumnIndex(helper.SETS_HEADER_REPS)));
                if(j != d.getCount() - 1)
                    s += delimiter;
                d.moveToNext();
            }
            s += "\n";
            c.moveToNext();
        }
        db.close();
        return s;
    }

}
