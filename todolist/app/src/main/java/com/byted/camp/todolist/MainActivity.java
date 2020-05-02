package com.byted.camp.todolist;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.byted.camp.todolist.beans.Note;
import com.byted.camp.todolist.beans.State;
import com.byted.camp.todolist.db.TodoContract;
import com.byted.camp.todolist.db.TodoDbHelper;
import com.byted.camp.todolist.operation.activity.DatabaseActivity;
import com.byted.camp.todolist.operation.activity.DebugActivity;
import com.byted.camp.todolist.operation.activity.SettingActivity;
import com.byted.camp.todolist.ui.NoteListAdapter;
import com.byted.camp.todolist.db.TodoContract.TodoEntry;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final int REQUEST_CODE_ADD = 1002;

    private RecyclerView recyclerView;
    private NoteListAdapter notesAdapter;

    private TodoDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(
                        new Intent(MainActivity.this, NoteActivity.class),
                        REQUEST_CODE_ADD);
            }
        });

        recyclerView = findViewById(R.id.list_todo);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false));
        recyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        notesAdapter = new NoteListAdapter(new NoteOperator() {
            @Override
            public void deleteNote(Note note) {
                MainActivity.this.deleteNote(note);
                notesAdapter.refresh(loadNotesFromDatabase());
//                notesAdapter.notifyDataSetChanged();
            }

            @Override
            public void updateNote(Note note) {
                MainActivity.this.updateNode(note);
                notesAdapter.refresh(loadNotesFromDatabase());
//                notesAdapter.notifyDataSetChanged();
            }
        });
        recyclerView.setAdapter(notesAdapter);

        dbHelper = new TodoDbHelper(this);
        notesAdapter.refresh(loadNotesFromDatabase());

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingActivity.class));
                return true;
            case R.id.action_debug:
                startActivity(new Intent(this, DebugActivity.class));
                return true;
            case R.id.action_database:
                startActivity(new Intent(this, DatabaseActivity.class));
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD
                && resultCode == Activity.RESULT_OK) {
            notesAdapter.refresh(loadNotesFromDatabase());
        }
    }

    private List<Note> loadNotesFromDatabase() {
        // TODO 从数据库中查询数据，并转换成 JavaBeans
        List<Note> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {
                BaseColumns._ID,
                TodoEntry.COLUMN_CONTENT,
                TodoEntry.COLUMN_DATE,
                TodoEntry.COLUMN_STATE,
                TodoEntry.COLUMN_PRIORITY
        };
        String sortOrder = TodoEntry.COLUMN_PRIORITY + " ASC";
        Cursor cursor = db.query(
                TodoEntry.TABLE_NAME,       // The table to query
                projection,             // The array of columns to return (pass null to get all)
                null,              // The columns for the WHERE clause
                null,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                sortOrder               // The sort order
        );
        Log.i(TAG, "perfrom query data:");
        while (cursor.moveToNext()) {
            long itemId = cursor.getLong(cursor.getColumnIndexOrThrow(TodoEntry._ID));
            String content = cursor.getString(cursor.getColumnIndex(TodoEntry.COLUMN_CONTENT));
            String date_str = cursor.getString(cursor.getColumnIndex(TodoEntry.COLUMN_DATE));
            int state_num = cursor.getInt(cursor.getColumnIndex(TodoEntry.COLUMN_STATE));
            int priority = cursor.getInt(cursor.getColumnIndex(TodoEntry.COLUMN_PRIORITY));
            Note note = new Note(itemId);
            note.setContent(content);
            try {
                String format = "E, dd MMM yyyy HH:mm:ss";
                SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.ENGLISH);
                Date date = dateFormat.parse(date_str);
                note.setDate(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            note.setState(State.from(state_num));
            note.setPrivority(priority);
            list.add(note);
        }
        cursor.close();
        return list;
//        return null;
    }

    private void deleteNote(Note note) {
        // TODO 删除数据
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = TodoEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(note.id)};
        int deletedRows = db.delete(TodoEntry.TABLE_NAME, selection, selectionArgs);
        Log.i(TAG, "delete: " + deletedRows);
    }


    private void updateNode(Note note) {
        // 更新数据
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = TodoEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(note.id)};

        State state = note.getState();
        ContentValues values = new ContentValues();

        if (state == State.TODO) {
            values.put(TodoEntry.COLUMN_STATE, 0);
            Log.i(TAG, "state: 0");
        } else {
            values.put(TodoEntry.COLUMN_STATE, 1);
            Log.i(TAG, "state: 1");
        }
        int count = db.update(
                TodoEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
        Log.i(TAG, "perform update data, result:" + count);
    }
}
