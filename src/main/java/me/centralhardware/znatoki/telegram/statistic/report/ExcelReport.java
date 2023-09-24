package me.centralhardware.znatoki.telegram.statistic.report;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExcelReport {

    private final Workbook workbook;
    private Sheet sheet;
    private int rowIndex = 0;

    protected final String fio;
    protected final Long service;
    protected final String serviceName;
    protected final LocalDateTime date;

    public ExcelReport(String fio, Long service, String serviceName, LocalDateTime date){
        this.fio = fio;
        this.service = service;
        this.serviceName = serviceName;
        this.date = date;
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

    protected void writeTitle(String title, Integer columnNumbers){
        Row row = sheet.createRow(rowIndex);
        this.sheet.autoSizeColumn(rowIndex);
        var cell = row.createCell(0);
        cell.setCellValue(title);
        CellStyle cellStyle = cell.getCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, columnNumbers));
        rowIndex++;
    }

    public File create(){
        try {
            var temp = Files.createFile(Path.of(fio + " - " + serviceName + " " + date.format(DateTimeFormatter.ofPattern("MMMM")) + " " +
                    date.getYear() + ".xlsx")).toFile();

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
