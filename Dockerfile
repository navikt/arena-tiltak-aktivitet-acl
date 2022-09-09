FROM ghcr.io/navikt/poao-baseimages/java:17
COPY /target/arena-aktivitet-acl.jar app.jar
