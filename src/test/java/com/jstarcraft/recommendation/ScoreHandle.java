package com.jstarcraft.recommendation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Iterator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class ScoreHandle {

    @Test
    public void test() throws Exception {
        String path = "data/";
        char delimiter = ',';
        File inputFile = new File(path + "musical_instruments.csv");
        InputStream inputStream = new FileInputStream(inputFile);
        InputStreamReader reader = new InputStreamReader(inputStream);
        BufferedReader readerBuffer = new BufferedReader(reader);

        File outputFile = new File(path + "musical_instruments.txt");
        FileUtils.deleteQuietly(outputFile);
        outputFile.createNewFile();
        OutputStream outputStream = new FileOutputStream(outputFile);
        OutputStreamWriter writer = new OutputStreamWriter(outputStream);
        BufferedWriter writerBuffer = new BufferedWriter(writer);

        int[] indexes = new int[] { 0, 1, 3, 2 };
        try (CSVParser parser = new CSVParser(readerBuffer, CSVFormat.newFormat(delimiter))) {
            Iterator<CSVRecord> iterator = parser.iterator();
            while (iterator.hasNext()) {
                CSVRecord datas = iterator.next();
                StringBuilder buffer = new StringBuilder();
                for (int index : indexes) {
                    buffer.append(datas.get(index)).append(delimiter);
                }
                writerBuffer.write(buffer.substring(0, buffer.length() - 1));
                writerBuffer.newLine();
            }
        } catch (Exception exception) {
            // TODO 处理日志.
            throw new RuntimeException(exception);
        }

        writerBuffer.flush();
    }

}
