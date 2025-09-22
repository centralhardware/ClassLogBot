package me.centralhardware.znatoki.telegram.statistic.service

import java.io.IOException
import kotlin.concurrent.fixedRateTimer
import me.centralhardware.znatoki.telegram.statistic.entity.Student
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.TextField
import org.apache.lucene.index.*
import org.apache.lucene.search.*
import org.apache.lucene.store.ByteBuffersDirectory

object ClientService {

    private var directory: ByteBuffersDirectory? = null

    fun init() {
        fixedRateTimer(name = "updateLucene", period = 60000L) {
            val index = ByteBuffersDirectory()
            val analyzer = StandardAnalyzer()
            val indexWriterConfig = IndexWriterConfig(analyzer)
            val writer = IndexWriter(index, indexWriterConfig)

            StudentMapper.findAll().forEach { client ->
                try {
                    val document = Document()
                    document.add(TextField("name", client.name.lowercase(), Field.Store.YES))
                    document.add(
                        TextField("lastName", client.lastName.lowercase(), Field.Store.YES)
                    )
                    document.add(
                        TextField("secondName", client.secondName.lowercase(), Field.Store.YES)
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

    fun search(fio: String): List<Student> {
        // Создаем поисковые запросы для каждого слова по каждому полю (имя, фамилия и отчество)
        val queries =
            fio.split(" ").flatMap { word ->
                listOf(
                    FuzzyQuery(Term("name", word), 2),
                    FuzzyQuery(Term("secondName", word), 2),
                    FuzzyQuery(Term("lastName", word), 2),
                )
            }

        // Конструируем BooleanQuery, объединяя все запросы
        val combinedQuery = BooleanQuery.Builder()
        queries.forEach { query -> combinedQuery.add(query, BooleanClause.Occur.SHOULD) }

        val indexReader: IndexReader = DirectoryReader.open(directory)
        val searcher = IndexSearcher(indexReader)
        val topDocs = searcher.search(combinedQuery.build(), 10)
        return topDocs.scoreDocs
            .sortedBy { it.score }
            .reversed()
            .map { searcher.storedFields().document(it.doc) }
            .mapNotNull { it: Document -> StudentMapper.findById(it["id"].toInt()) }
            .toList()
    }
}
