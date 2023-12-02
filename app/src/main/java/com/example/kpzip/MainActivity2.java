package com.example.kpzip;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.PathUtils;

import android.Manifest;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.compress.compressors.CompressorException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileStore;
import java.util.ArrayList;

import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;


import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MainActivity2 extends AppCompatActivity {
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> fileInfoList;
    private ArrayList<String> filePaths;
    public String newFilePath;
    private TextView textview;

    String fileName;
    long fileSize;

    File zipFile;
    File file2;

    public String nameoFile;

    boolean checkLongClick = false;

    private static final int REQUEST_CODE_PICK_FILE = 1;
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 2;
    private static final String TAG = "MainActivity2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        listView = findViewById(R.id.listview);
        String zipFilePath = MainActivity.zipPath;
        textview = findViewById(R.id.textName);
        textview.setText(MainActivity.zipNameExp);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        List<FileItem> fileList = getFileListFromArchive(zipFilePath);

        List<String> fileInfos = new ArrayList<>();
        for (FileItem item : fileList) {
            String info = "Имя: " + item.getFileName() + ", Размер: " + item.getFileSize() + " байт";
            System.out.println(info);
            fileInfos.add(info);
        }

        ArrayAdapter<String> filesAdapter = new ArrayAdapter<>(this, R.layout.listivew_layout,
                fileInfos);
        listView.setAdapter(filesAdapter);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                checkLongClick = true;

                String fileInfo = (String)parent.getItemAtPosition(position);

                int startIndex = fileInfo.indexOf("Имя: ") + "Имя: ".length();
                int endIndex = fileInfo.indexOf(", Размер:");

                nameoFile = fileInfo.substring(startIndex, endIndex);

                int color = Color.TRANSPARENT;
                Drawable background = view.getBackground();
                if (background instanceof ColorDrawable)
                    color = ((ColorDrawable) background).getColor();

                if (color == Color.WHITE) {
                    view.setBackgroundColor(Color.LTGRAY);
                } else {
                    view.setBackgroundColor(Color.WHITE);
                }

                return false;
            }
        });

    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            String fileName = getFileName(fileUri);
            if (fileName != null) {
                try {
                    File archiveFile = new File(MainActivity.zipPath);  // Создание архивного файла
                    addFileToArchive(fileUri, fileName, archiveFile);  // Добавление файла в архив
                    Toast.makeText(this, "Добавлен файл: " + fileName, Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Log.e(TAG, "Error adding the file to the archive", e);
                    Toast.makeText(this, "Ошибка при добавлении файла в архив", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String getFileName(Uri uri) {
        String fileName = null;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1) {
                fileName = cursor.getString(nameIndex);
            }
            cursor.close();
        }
        return fileName;
    }

    private void addFileToArchive(Uri fileUri, String fileName, File archiveFile) throws IOException {
        File tempFile = new File(Environment.getExternalStorageDirectory(), MainActivity.zipNameExp);

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempFile));
        ZipFile zipFile = new ZipFile(archiveFile);

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry e = entries.nextElement();
            zos.putNextEntry(e);
            if (!e.isDirectory()) {
                copy(zipFile.getInputStream(e), zos);
            }
            zos.closeEntry();
        }

        InputStream inputStream = getContentResolver().openInputStream(fileUri);
        zos.putNextEntry(new ZipEntry(fileName));
        byte[] buffer = new byte[4096];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            zos.write(buffer, 0, length);
        }
        zos.closeEntry();
        inputStream.close();

        zos.close();

        archiveFile.delete();
        tempFile.renameTo(archiveFile);
    }

    public void unzip() {
        String unzipLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/unziplocation/";

        File dir = new File(unzipLocation);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try (FileInputStream fin = new FileInputStream(MainActivity.zipPath);
             ZipInputStream zin = new ZipInputStream(fin)) {

            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {
                String path = unzipLocation + ze.getName();
                Log.d("Decompress", "Extracting: " + path);


                if (ze.isDirectory()) {
                    File unzipDir = new File(path);
                    if (!unzipDir.isDirectory()) {
                        unzipDir.mkdirs();
                        Log.d("Decompress", "Creating directory: " + path);
                    }
                } else {

                    File outputFile = new File(path);
                    File outputDir = new File(outputFile.getParent());
                    if (!outputDir.exists()) {
                        outputDir.mkdirs();
                        Log.d("Decompress", "Creating directory: " + outputDir);
                    }


                    try (FileOutputStream fout = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[8192];
                        int count;


                        while ((count = zin.read(buffer)) != -1) {
                            fout.write(buffer, 0, count);
                        }
                        Log.d("Decompress", "File extracted: " + path);
                    }
                    zin.closeEntry();
                }
            }
        } catch (Exception e) {
            Log.e("Decompress", "unzip", e);
        }
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_second, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.add_menu) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    String packageURI = String.valueOf(Uri.fromParts("package", getPackageName(), null));
                    intent.setData(Uri.parse(packageURI));
                    startActivity(intent);
                } else {
                    openFilePicker();
                }
            } else if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            } else {
                openFilePicker();
            }
        } else if (id == R.id.del_menu) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    String packageURI = String.valueOf(Uri.fromParts("package", getPackageName(), null));
                    intent.setData(Uri.parse(packageURI));
                    startActivity(intent);
                } else {
                    try {
                        removeFileFromZip();
                        System.out.println("Файл успешно удален из архива.");
                    } catch (IOException e) {
                        System.err.println("Ошибка при удалении файла из архива: " + e.getMessage());
                    }
                }
            } else if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            } else {
                try {
                    removeFileFromZip();
                    System.out.println("Файл успешно удален из архива.");
                } catch (IOException e) {
                    System.err.println("Ошибка при удалении файла из архива: " + e.getMessage());
                }
            }
        } else if (id == R.id.exit_menu) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    String packageURI = String.valueOf(Uri.fromParts("package", getPackageName(), null));
                    intent.setData(Uri.parse(packageURI));
                    startActivity(intent);
                } else {
                    unzip();
                    System.out.println("Файл успешно удален из архива.");
                }
            } else if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            } else {
                unzip();
                System.out.println("Файл успешно удален из архива.");
            }
        } else {
            System.out.println("Ошибка");
        }

        return super.onOptionsItemSelected(item);
    }

    public List<FileItem> getFileListFromArchive(String filePath) {
        List<FileItem> fileList = new ArrayList<>();

        try {
            ZipFile zip = new ZipFile(filePath);
            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String fileName = entry.getName();
                long fileSize = entry.getSize();
                FileItem file = new FileItem(fileName, fileSize);
                fileList.add(file);
            }
            zip.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileList;
    }

    public void removeFileFromZip() throws IOException {

        String archivePath = MainActivity.zipPath;


        String fileName = nameoFile;


        File tempFile = new File(archivePath + ".temp");
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(tempFile));


        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(archivePath));


        for (ZipEntry entry = zipInputStream.getNextEntry(); entry != null; entry = zipInputStream.getNextEntry()) {

            if (!entry.getName().equals(fileName)) {
                zipOutputStream.putNextEntry(new ZipEntry(entry.getName()));


                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                    zipOutputStream.write(buffer, 0, bytesRead);
                }
            }
        }


        zipInputStream.close();
        zipOutputStream.close();


        File originalFile = new File(archivePath);
        tempFile.renameTo(originalFile);
    }

    public static class FileItem {
        private String fileName;
        private long fileSize;

        public FileItem(String fileName, long fileSize) {
            this.fileName = fileName;
            this.fileSize = fileSize;
        }

        public String getFileName() {
            return fileName;
        }

        public long getFileSize() {
            return fileSize;
        }
    }
}