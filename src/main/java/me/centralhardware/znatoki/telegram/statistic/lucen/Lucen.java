package me.centralhardware.znatoki.telegram.statistic.lucen;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.Clickhouse;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Pupil;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.LevenshteinAutomata;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class Lucen {


    private final Clickhouse clickhouse;

    private StandardAnalyzer analyzer;
    private Directory memoryIndex;

    @PostConstruct
    public void init() throws IOException {
        memoryIndex = new RAMDirectory();
        analyzer = new StandardAnalyzer();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        IndexWriter writter = new IndexWriter(memoryIndex, indexWriterConfig);

        clickhouse.getPupils()
                .forEach(pupil -> {
                    try {
                        Document document = new Document();
                        document.add(new TextField("fio", pupil.name().toLowerCase() + " " +
                                pupil.secondName().toLowerCase() + " " +
                                pupil.lastName().toLowerCase(), Field.Store.YES));
                        writter.addDocument(document);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        writter.close();
    }

    @SneakyThrows
    public List<String> search(String fio){
        Query fuzzyQuery = new FuzzyQuery(new Term("fio", fio), 2);


        IndexReader indexReader = DirectoryReader.open(memoryIndex);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(fuzzyQuery, 10);
        List<Document> documents = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            documents.add(searcher.doc(scoreDoc.doc));
        }

        return documents.stream()
                .map(it -> it.get("fio"))
                .toList();
    }

}
