package me.centralhardware.znatoki.telegram.statistic.clickhouse;

import com.clickhouse.client.*;
import com.clickhouse.data.ClickHouseDataStreamFactory;
import com.clickhouse.data.ClickHouseFormat;
import com.clickhouse.data.ClickHousePipedOutputStream;
import com.clickhouse.data.ClickHouseValue;
import com.clickhouse.data.format.BinaryStreamUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.LogEntry;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Pupil;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
@RequiredArgsConstructor
public class Clickhouse {

    private final ClickHouseNode server;

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

    public List<Pupil> getPupils(){
        try (ClickHouseClient client = openConnection()){
            ClickHouseResponse response = client.read(server)
                    .format(ClickHouseFormat.RowBinaryWithNamesAndTypes)
                    .query("SELECT * FROM pupil WHERE deleted = false").execute().get();

            return response.stream()
                    .map(it -> new Pupil(
                            it.getValue(0).asInteger(),
                            it.getValue(1).asString(),
                            it.getValue(2).asInteger(),
                            it.getValue(3).asString(),
                            it.getValue(4).asDateTime(),
                            it.getValue(5).asDateTime(),
                            it.getValue(6).asDateTime(),
                            it.getValue(7).asString(),
                            it.getValue(8).asString(),
                            it.getValue(9).asString(),
                            it.getValue(10).asString(),
                            it.getValue(11).asString(),
                            it.getValue(12).asString(),
                            it.getValue(13).asDateTime(),
                            it.getValue(14).asString(),
                            it.getValue(15).asString(),
                            it.getValue(16).asString(),
                            it.getValue(17).asString(),
                            it.getValue(18).asString(),
                            it.getValue(19).asString(),
                            it.getValue(20).asString(),
                            it.getValue(21).asString(),
                            it.getValue(22).asString(),
                            it.getValue(23).asInteger(),
                            it.getValue(24).asInteger(),
                            it.getValue(25).asBoolean()
                    ))
                    .toList();

        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public boolean exist(String fio){
        try (ClickHouseClient client = openConnection()){
            ClickHouseResponse response = client.read(server)
                    .format(ClickHouseFormat.RowBinaryWithNamesAndTypes)
                    .query(String.format("SELECT 1 FROM pupil where trim(lowerUTF8(concat(name, ' ', second_name, ' ',  last_name))) = '%s' and deleted = false", fio)).execute().get();

            return response.
                    stream()
                    .findFirst()
                    .map(it -> it.getValue(0))
                    .map(ClickHouseValue::asInteger)
                    .orElse(0) == 1;

        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private ClickHouseClient openConnection(){
        return ClickHouseClient.newInstance(server.getProtocol());
    }

}
