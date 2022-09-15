FROM ghcr.io/navikt/poao-baseimages/java:17
COPY /target/aktivitet-arena-acl.jar app.jar
