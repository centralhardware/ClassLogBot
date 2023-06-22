package me.centralhardware.znatoki.telegram.statistic.lucen;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.centralhardware.znatoki.telegram.statistic.configuration.LuceneConfiguration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class Lucene {

    private final LuceneConfiguration luceneConfiguration;

    public static final String FIO_FIELD = "fio";
    public static final String BIO_FIELD = "bio";

    @SneakyThrows
    public List<Pair<String, String>> search(String fio){
        Query fuzzyQuery = new FuzzyQuery(new Term(FIO_FIELD, fio.toLowerCase()), 2);


        IndexReader indexReader = DirectoryReader.open(luceneConfiguration.getMemoryIndex());
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(fuzzyQuery, 10);
        List<Document> documents = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            documents.add(searcher.doc(scoreDoc.doc));
        }

        return documents.stream()
                .map(it -> Pair.of(it.get(FIO_FIELD), it.get(BIO_FIELD)))
                .toList();
    }

}
