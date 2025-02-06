# O build da aplicação ocorre em duas etapas, ou seja, multi stage. 
# Primeiro, utilizamos uma imagem alpine para instalar zip e ffmpeg
FROM alpine:latest AS builder
RUN apk add --no-cache zip ffmpeg

# Criar um diretório para armazenar as dependências dinâmicas
RUN mkdir /deps

# Identificar e copiar dependências do ffmpeg e zip
RUN ldd /usr/bin/ffmpeg | awk '{ if (match($3, "/")) { print $3 } }' | xargs -I '{}' cp -v '{}' /deps/ \
    && ldd /usr/bin/zip | awk '{ if (match($3, "/")) { print $3 } }' | xargs -I '{}' cp -v '{}' /deps/


# Agora sim, utilizamos uma imagem oficial quarkus micro para executar o build nativo
FROM quay.io/quarkus/quarkus-micro-image:2.0

WORKDIR /work/

# Copiar binários do ffmpeg e zip instalados no stage anterior
COPY --from=builder /usr/bin/zip /usr/bin/zip
COPY --from=builder /usr/bin/ffmpeg /usr/bin/ffmpeg

# Copiar dependências dinâmicas identificadas no estágio anterior
COPY --from=builder /deps/* /lib/

# Copiar aplicação nativa
COPY --chown=1001:root target/*-runner /work/application

# Expor porta 8080
EXPOSE 8080
USER 1001

# Definir ponto de entrada
ENTRYPOINT ["./application", "-Dquarkus.http.host=0.0.0.0"]
