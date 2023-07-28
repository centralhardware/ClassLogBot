package me.centralhardware.znatoki.telegram.statistic.clickhouse.model;

import java.time.LocalDateTime;
import java.util.*;

public class Time {

    private LocalDateTime dateTime;
    private UUID id;
    private Long chatId;
    private String subject;
    private Set<String> fios = new HashSet<>();
    private String fio;
    private Integer amount;
    private String photoId;

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Set<String> getFios() {
        return fios;
    }

    public void setFios(Set<String> fios) {
        this.fios = fios;
    }

    public String getFio() {
        return fio;
    }

    public void setFio(String fio) {
        this.fio = fio;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

}
