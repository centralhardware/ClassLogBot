package me.centralhardware.znatoki.telegram.statistic.service

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import dev.inmo.kslog.common.error
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.fixedRateTimer
import me.centralhardware.znatoki.telegram.statistic.entity.Student
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StoredField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.*
import org.apache.lucene.search.*
import org.apache.lucene.store.ByteBuffersDirectory

/**
 * Lucene-based student search service.
 * Maintains an in-memory index rebuilt every 60 seconds (and on [refresh]) for fast fuzzy search
 * across student names, supporting partial matches and typo tolerance.
 */
object StudentService {

    /** The index and the students it was built from, swapped atomically on every rebuild. */
    private class Snapshot(val directory: ByteBuffersDirectory, val students: Map<Int, Student>)

    private val snapshot = AtomicReference<Snapshot?>(null)

    fun init() {
        fixedRateTimer(name = "updateLucene", period = 60000L) {
            runCatching { refresh() }.onFailure { KSLog.error("StudentService: index rebuild failed", it) }
        }
    }

    /** Rebuilds the index now, e.g. right after a student was added, so it is searchable immediately. */
    @Synchronized
    fun refresh() {
        val students = StudentMapper.findAll()
        val index = ByteBuffersDirectory()
        IndexWriter(index, IndexWriterConfig(StandardAnalyzer())).use { writer ->
            students.forEach { client ->
                val document = Document()
                document.add(TextField("name", client.name.lowercase(), Field.Store.NO))
                document.add(TextField("lastName", client.lastName.lowercase(), Field.Store.NO))
                document.add(TextField("secondName", client.secondName.lowercase(), Field.Store.NO))
                document.add(StoredField("id", client.id.id))
                writer.addDocument(document)
            }
        }
        snapshot.set(Snapshot(index, students.associateBy { it.id.id }))
    }

    fun getAllActive(): List<Student> {
        val students = StudentMapper.findAll()
        KSLog.info("StudentService.getAllActive(): found ${students.size} students")
        return students
    }

    fun search(fio: String): List<Student> {
        val current = snapshot.get()
        if (current == null) {
            KSLog.info("StudentService.search('$fio'): index not built yet")
            return emptyList()
        }

        return try {
            val combinedQuery = BooleanQuery.Builder()
            fio.lowercase().split(" ").filter { it.isNotBlank() }.forEach { word ->
                listOf("name", "secondName", "lastName").forEach { field ->
                    combinedQuery.add(FuzzyQuery(Term(field, word), 2), BooleanClause.Occur.SHOULD)
                }
            }

            val results = DirectoryReader.open(current.directory).use { reader ->
                val searcher = IndexSearcher(reader)
                val storedFields = searcher.storedFields()
                searcher.search(combinedQuery.build(), 10).scoreDocs
                    .sortedByDescending { it.score }
                    .mapNotNull { current.students[storedFields.document(it.doc).getField("id").numericValue().toInt()] }
            }
            KSLog.info("StudentService.search('$fio'): found ${results.size} students")
            results
        } catch (e: Exception) {
            KSLog.error("StudentService.search('$fio') failed", e)
            emptyList()
        }
    }
}
