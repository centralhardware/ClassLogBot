package me.centralhardware.znatoki.telegram.statistic.redis;

public enum Role {

    UNAUTHORIZED,
    READ,
    READ_WRITE,
    ADMIN;

    public static Role of(String role){
        switch (role){
            case "admin" -> {
                return ADMIN;
            }
            case "rw" -> {
                return READ_WRITE;
            }
            case "r" -> {
                return READ;
            }
            default -> {
                return UNAUTHORIZED;
            }
        }
    }

}
