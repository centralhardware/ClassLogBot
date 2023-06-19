package me.centralhardware.znatoki.telegram.statistic.clickhouse;

import com.clickhouse.client.*;
import com.clickhouse.data.ClickHouseDataStreamFactory;
import com.clickhouse.data.ClickHouseFormat;
import com.clickhouse.data.ClickHousePipedOutputStream;
import com.clickhouse.data.format.BinaryStreamUtils;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.LogEntry;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class Clickhouse {

    private final ClickHouseNode server;

    public Clickhouse(ClickHouseNode server){
        this.server = server;

        createTable();
    }

    private void createTable(){
        try (ClickHouseClient client = openConnection()){
            ClickHouseRequest<?> request = client.read(server);
            request.query("""
                    CREATE TABLE IF NOT EXISTS znatoki_statistic
                    (
                        date_time DateTime,
                        chat_id BIGINT,
                        username Nullable(String),
                        first_name Nullable(String),
                        last_name Nullable(String),
                        lang String,
                        is_premium bool,
                        action String,
                        text VARCHAR(256),
                    )
                    engine = MergeTree
                    ORDER BY date_time
                    """)
                    .execute().get();
            request.query("""
                    CREATE TABLE IF NOT EXISTS znatoki_statistic_time
                    (
                        date_time DateTime,
                        chat_id BIGINT,
                        subject String,
                        fio String,
                        amount INT,
                        photoId String
                    )
                    engine = MergeTree
                    ORDER BY date_time
                    """).execute().get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void insert(LogEntry entry){
        try (ClickHouseClient client = openConnection()){
            ClickHouseRequest.Mutation request =  client
                    .read(server)
                    .write()
                    .table("znatoki_statistic")
                    .format(ClickHouseFormat.RowBinary);

            ClickHouseConfig config = request.getConfig();
            CompletableFuture<ClickHouseResponse> future;

            try (ClickHousePipedOutputStream stream = ClickHouseDataStreamFactory.getInstance()
                    .createPipedOutputStream(config, (Runnable) null)){
                future = request.data(stream.getInputStream()).execute();
                write(stream, entry.dateTime());
                write(stream,entry.chatId());
                writeNullable(stream, entry.username());
                writeNullable(stream, entry.firstName());
                writeNullable(stream, entry.lastName());
                write(stream, entry.lang());
                write(stream, entry.isPremium() != null && entry.isPremium());
                write(stream, entry.action());
                write(stream,entry.text());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try (ClickHouseResponse response = future.get()){
                log.info("{} row inserted in clickhouse", response.getSummary().getWrittenRows());
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public void insert(Time time){
        try (ClickHouseClient client = openConnection()){
            ClickHouseRequest.Mutation request =  client
                    .read(server)
                    .write()
                    .table("znatoki_statistic_time")
                    .format(ClickHouseFormat.RowBinary);

            ClickHouseConfig config = request.getConfig();
            CompletableFuture<ClickHouseResponse> future;

            try (ClickHousePipedOutputStream stream = ClickHouseDataStreamFactory.getInstance()
                    .createPipedOutputStream(config, (Runnable) null)){
                future = request.data(stream.getInputStream()).execute();
                write(stream, time.getDateTime());
                write(stream,time.getChatId());
                write(stream, time.getSubject());
                write(stream, time.getFio());
                write(stream, time.getAmount());
                write(stream, time.getPhotoId());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try (ClickHouseResponse response = future.get()){
                log.info("{} row inserted in clickhouse", response.getSummary().getWrittenRows());
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }

    private void writeNullable(OutputStream stream, Object value) throws IOException {
        if (value == null){
            BinaryStreamUtils.writeNull(stream);
            return;
        }
        BinaryStreamUtils.writeNonNull(stream);
        write(stream, value);
    }

    private void write(OutputStream stream, Object value) throws IOException {
        if (value instanceof String string){
            BinaryStreamUtils.writeString(stream, string);
        } else if (value instanceof UUID uuid){
            BinaryStreamUtils.writeUuid(stream, uuid);
        } else if (value instanceof Integer integer){
            BinaryStreamUtils.writeInt32(stream, integer);
        } else if (value instanceof  Long bigint){
            BinaryStreamUtils.writeInt64(stream, bigint);
        } else if (value instanceof Boolean bool){
            BinaryStreamUtils.writeBoolean(stream, bool);
        } else if (value instanceof LocalDateTime dateTime){
            BinaryStreamUtils.writeDateTime(stream, dateTime, TimeZone.getDefault());
        }
    }

    private ClickHouseClient openConnection(){
        return ClickHouseClient.newInstance(server.getProtocol());
    }

}
