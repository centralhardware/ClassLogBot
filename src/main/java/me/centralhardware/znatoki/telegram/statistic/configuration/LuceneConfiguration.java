package me.centralhardware.znatoki.telegram.statistic.configuration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Pupil;
import me.centralhardware.znatoki.telegram.statistic.mapper.PupilMapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.RAMDirectory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static me.centralhardware.znatoki.telegram.statistic.lucen.Lucene.BIO_FIELD;
import static me.centralhardware.znatoki.telegram.statistic.lucen.Lucene.FIO_FIELD;

@Component
@RequiredArgsConstructor
@Slf4j
public class LuceneConfiguration {

    private final PupilMapper pupilMapper;

    @Getter
    private RAMDirectory memoryIndex;

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public void getAnalyzer() throws IOException {
        log.info("Start update lucene index");

        if (memoryIndex == null){
            memoryIndex = new RAMDirectory();
        }

        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        IndexWriter writter = new IndexWriter(memoryIndex, indexWriterConfig);

        writter.deleteAll();
        pupilMapper.getPupils()
                .forEach(pupil -> {
                    try {
                        Document document = new Document();
                        document.add(new TextField(FIO_FIELD, getFio(pupil) , Field.Store.YES));
                        document.add(new TextField(BIO_FIELD, getBio(pupil), Field.Store.YES));
                        writter.addDocument(document);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        writter.close();
        log.info("Finish update lucene index");
    }

    private String getFio(Pupil pupil){
        return pupil.name().toLowerCase() + " " +
                pupil.secondName().toLowerCase() + " " +
                pupil.lastName().toLowerCase();
    }

    private String getBio(Pupil pupil){
        return String.format("%s класс %s лет", pupil.classNumber(), ChronoUnit.YEARS.between(pupil.dateOfBirth(), LocalDateTime.now()));
    }

}
