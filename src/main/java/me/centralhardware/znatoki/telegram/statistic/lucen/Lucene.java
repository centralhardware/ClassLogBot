package me.centralhardware.znatoki.telegram.statistic.lucen;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class Lucene {

    private final Directory memoryIndex;

    public static final String FIO_FIELD = "fio";

    @SneakyThrows
    public List<String> search(String fio){
        Query fuzzyQuery = new FuzzyQuery(new Term(FIO_FIELD, fio), 2);


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
