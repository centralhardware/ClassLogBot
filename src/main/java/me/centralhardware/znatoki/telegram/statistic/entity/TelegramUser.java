package me.centralhardware.znatoki.telegram.statistic.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.centralhardware.znatoki.telegram.statistic.entity.Enum.Role;

import java.util.Set;

@Entity
@Table
@NoArgsConstructor
public class TelegramUser {

    @Id
    @Getter
    @Setter
    private Long id;
    @Getter
    @Setter
    private String username;
    @Getter
    @Setter
    private String firstName;
    @Getter
    @Setter
    private String lastName;
    @Enumerated(EnumType.STRING)
    @Getter
    @Setter
    private Role role;
    @Column(name = "authorizeInGoogle", columnDefinition = "boolean default false")
    @Getter
    @Setter
    private boolean authorizeInGoogle;

    @OneToMany(mappedBy = "created_by")
    private Set<Pupil> create;

    @OneToMany(mappedBy = "updateBy")
    private Set<Pupil> update;

    @OneToMany(mappedBy = "updateBy")
    private Set<Session> sessions;

    @OneToMany(mappedBy = "updateBy")
    private Set<Session> updates;


    public TelegramUser(Long id, String username, String firstName, String lastName, Role role) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    @Override
    public String toString() {
        if (username == null) {
            return String.format("*%s %s*", firstName, lastName);
        } else {
            return String.format("[%s](https://t.me/%s)", username, username);
        }
    }

    public boolean hasReadRight() {
        if (role == null) return false;
        return role != Role.UNAUTHORIZED;
    }

    public boolean hasWriteRight() {
        if (role == null) return false;
        return role == Role.READ_WRITE || role == Role.ADMIN;
    }
}
