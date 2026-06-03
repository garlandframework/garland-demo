package org.mtodemo.tests.support.base;

public final class Connections {

    public static final String USER_SERVICE_URL       = "http://localhost:8080";
    public static final String PROJECTION_SERVICE_URL = "http://localhost:8081";

    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin";

    public static final String PG_URL      = "jdbc:postgresql://localhost:5432/userdb?TimeZone=UTC";
    public static final String PG_USERNAME = "user";
    public static final String PG_PASSWORD = "password";

    public static final String KAFKA_BOOTSTRAP_SERVERS   = "localhost:9092";
    public static final String KAFKA_TOPIC_USER_CREATED  = "user.created";
    public static final String KAFKA_TOPIC_USER_UPDATED  = "user.updated";
    public static final String KAFKA_TOPIC_USER_DELETED  = "user.deleted";
    public static final String KAFKA_TOPIC_ORDER_PLACED    = "order.placed";
    public static final String KAFKA_TOPIC_ORDER_CANCELLED = "order.cancelled";

    public static final String MONGO_CONNECTION_STRING = "mongodb://localhost:27017";
    public static final String MONGO_DATABASE          = "projectiondb";

    private Connections() {}
}
