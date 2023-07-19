package me.centralhardware.znatoki.telegram.statistic.report;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

public class ExcelReport {

    private final Workbook workbook;
    private Sheet sheet;
    private int rowIndex = 0;

    private final String fio;

    public ExcelReport(String fio){
        this.fio = fio;
        this.workbook = new XSSFWorkbook();
    }

    public void newSheet(String name){
        this.sheet = workbook.createSheet(name);
    }

    protected void writeRow(String...values){
        Row row = sheet.createRow(rowIndex);
        this.sheet.autoSizeColumn(rowIndex);
        int i = 0;
        for (String value : values){
            var cell = row.createCell(i);
            cell.setCellValue(value);
            i = i + 1;
        }
        rowIndex = rowIndex + 1;
    }

    public File create(){
        try {
            var temp = Files.createTempFile(fio, ".xlsx").toFile();
            try (var outputStream = new FileOutputStream(temp)){
                workbook.write(outputStream);
                workbook.close();
            }
            return temp;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
