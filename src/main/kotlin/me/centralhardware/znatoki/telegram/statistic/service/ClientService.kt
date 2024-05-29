package me.centralhardware.znatoki.telegram.statistic.service

import me.centralhardware.znatoki.telegram.statistic.entity.Client
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.TextField
import org.apache.lucene.index.*
import org.apache.lucene.search.FuzzyQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.store.ByteBuffersDirectory
import java.io.IOException
import kotlin.concurrent.fixedRateTimer

object ClientService {

    private var directory: ByteBuffersDirectory? = null


    init {
        fixedRateTimer(name = "updateLucene", period = 1000L) {
            val index = ByteBuffersDirectory()
            val analyzer = StandardAnalyzer()
            val indexWriterConfig = IndexWriterConfig(analyzer)
            val writer = IndexWriter(index, indexWriterConfig)

            ClientMapper.findAll()
                .forEach { client ->
                    try {
                        val document = Document()
                        document.add(
                            TextField(
                                "fio", (client.name.toLowerCase() + " " +
                                        client.secondName.toLowerCase()).toString() + " " +
                                        client.lastName.toLowerCase(), Field.Store.YES
                            )
                        )
                        document.add(TextField("id", client.id.toString(), Field.Store.YES))
                        writer.addDocument(document)
                    } catch (e: IOException) {
                        throw java.lang.RuntimeException(e)
                    }
                }
            writer.close()
            directory = index
        }
    }

    fun search(fio: String): List<Client> {
        val fuzzyQuery: Query =
            FuzzyQuery(Term("fio", fio), 2)

        val indexReader: IndexReader = DirectoryReader.open(directory)
        val searcher = IndexSearcher(indexReader)
        val topDocs = searcher.search(fuzzyQuery, 10)

        return topDocs.scoreDocs
            .map { searcher.doc(it.doc) }
            .mapNotNull { it:Document -> ClientMapper.findById(it["id"].toInt()) }
            .toList()
    }


}