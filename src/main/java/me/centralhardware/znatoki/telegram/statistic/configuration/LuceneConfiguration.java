package me.centralhardware.znatoki.telegram.statistic.configuration;

import me.centralhardware.znatoki.telegram.statistic.clickhouse.Clickhouse;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.RAMDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

import static me.centralhardware.znatoki.telegram.statistic.lucen.Lucene.FIO_FIELD;

@Configuration
public class LuceneConfiguration {


    @Bean
    public RAMDirectory getMemoryIndex(){
        return new RAMDirectory();
    }

    @Bean
    public StandardAnalyzer getAnalyzer(RAMDirectory memoryIndex, Clickhouse clickhouse) throws IOException {
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        IndexWriter writter = new IndexWriter(memoryIndex, indexWriterConfig);

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
        return analyzer;
    }

}
