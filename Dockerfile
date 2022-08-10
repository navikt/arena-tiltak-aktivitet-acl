FROM ghcr.io/navikt/poao-baseimages/java:17
COPY /target/arena-tiltak-aktivitet-acl.jar app.jar
