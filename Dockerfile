FROM busybox:1.36.1-uclibc as busybox
FROM gcr.io/distroless/java21-debian12:nonroot

COPY --from=busybox /bin/sh /bin/sh
COPY --from=busybox /bin/printenv /bin/printenv

WORKDIR /app
COPY /target/aktivitet-arena-acl.jar app.jar
ENV TZ="Europe/Oslo"
EXPOSE 8080
CMD ["app.jar"]
