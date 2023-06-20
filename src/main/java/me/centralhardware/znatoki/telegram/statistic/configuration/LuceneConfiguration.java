package me.centralhardware.znatoki.telegram.statistic.configuration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.Clickhouse;
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
import java.util.concurrent.TimeUnit;

import static me.centralhardware.znatoki.telegram.statistic.lucen.Lucene.FIO_FIELD;

@Component
@RequiredArgsConstructor
public class LuceneConfiguration {

    private final Clickhouse clickhouse;

    @Getter
    private RAMDirectory memoryIndex;

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    public void getAnalyzer() throws IOException {
        RAMDirectory index = new RAMDirectory();
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        IndexWriter writter = new IndexWriter(index, indexWriterConfig);

        clickhouse.getPupils()
                .forEach(pupil -> {
                    try {
                        Document document = new Document();
                        document.add(new TextField(FIO_FIELD, pupil.name().toLowerCase() + " " +
                                pupil.secondName().toLowerCase() + " " +
                                pupil.lastName().toLowerCase(), Field.Store.YES));
                        writter.addDocument(document);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        writter.close();
        this.memoryIndex = index;
    }

}
