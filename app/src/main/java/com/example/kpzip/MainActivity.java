package com.example.kpzip;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipOutputStream;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> fileInfoList;
    private ArrayList<String> filePaths;
    private List<File> zipFiles = new ArrayList<>();

    public static String zipPath;
    public static String zipNameExp;

    private static final int PERMISSION_REQUEST_CODE = 100;

    public ArrayList<String> deleteList;

    public boolean checkLongClick = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                String packageURI = String.valueOf(Uri.fromParts("package", getPackageName(), null));
                intent.setData(Uri.parse(packageURI));
                startActivity(intent);
            } else {
                createZipList();
            }
        } else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            createZipList();
        }

        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeupdate);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateList();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    public void updateList() {
        zipFiles.clear();
        createZipList();
    }

    public void createZipList() {
        File path = Environment.getExternalStorageDirectory();

        zipList(path);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());

        List<String> zipFilesInformation = new ArrayList<>();
        for (File file : zipFiles) {
            String modificationDate = dateFormat.format(new Date(file.lastModified()));
            zipFilesInformation.add("Имя: " + file.getName() + "\nПуть: " + file.getAbsolutePath() + "\nРазмер: " + file.length() + " байт" + "\nДата изменения: " + modificationDate);
        }

        deleteList = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.listivew_layout, zipFilesInformation);

        listView = findViewById(R.id.listview);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setAdapter(adapter);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                checkLongClick = true;

                String fileInfo = (String)parent.getItemAtPosition(position);
                String[] fileInfoParts = fileInfo.split("\n");

                String path = fileInfoParts[1].split(": ")[1];

                int color = Color.TRANSPARENT;
                Drawable background = view.getBackground();
                if (background instanceof ColorDrawable)
                    color = ((ColorDrawable) background).getColor();

                if (color == Color.WHITE) {
                    deleteList.remove(path);
                    view.setBackgroundColor(Color.LTGRAY);
                } else {
                    view.setBackgroundColor(Color.WHITE);
                    deleteList.add(path);
                }


                return false;
            }
        });


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!checkLongClick) {
                    String fileInfo = (String)parent.getItemAtPosition(position);
                    String[] fileInfoParts = fileInfo.split("\n");

                    String name = fileInfoParts[0].split(": ")[1];
                    String path = fileInfoParts[1].split(": ")[1];

                    zipNameExp = name;
                    zipPath = path;

                    Intent intent = new Intent(view.getContext(), MainActivity2.class);

                    view.getContext().startActivity(intent);
                }
            }
        });
    }

    public void deleteZIP() {
        File file;

        for (String path : deleteList) {
            file = new File(path);
            file.delete();
        }

        deleteList.clear();
        checkLongClick = false;
        updateList();
    }

    public void zipList(File directory) {
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".zip")) {
                    zipFiles.add(file);
                } else if (file.isDirectory()) {
                    zipList(file);
                }
            }
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.create_menu) {
            createButton();
        } else if (id == R.id.del_menu) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    String packageURI = String.valueOf(Uri.fromParts("package", getPackageName(), null));
                    intent.setData(Uri.parse(packageURI));
                    startActivity(intent);
                } else {
                    deleteZIP();
                }
            } else if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            } else {
                deleteZIP();
            }
        } else if (id == R.id.exit_menu) {
            System.out.println("Выход");
        } else {
            System.out.println("Ошибка");
        }

        return super.onOptionsItemSelected(item);
    }

    public void createButton() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Введите имя нового архива");

        final EditText input = new EditText(this);
        dialogBuilder.setView(input);

        dialogBuilder.setPositiveButton("Создать", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String zipName = input.getText().toString();
                createZIP(zipName);
                updateList();
            }
        });

        dialogBuilder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    public void createZIP(String zipName) {
        File zipFilePath = new File(Environment.getExternalStorageDirectory(), "/Download/" + zipName + ".zip");

        try {
            FileOutputStream fos = new FileOutputStream(zipFilePath);
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            zipOut.close();

            System.out.println("Пустой архив успешно создан: " + zipFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}