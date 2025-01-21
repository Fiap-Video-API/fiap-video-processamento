# fiap-videos-processamento

Projeto desenvolvido em Java Quarkus, para executar listener de mensagearia SQS e processamento de vídeos.
O objetivo é receber um vídeo, processar o mesmo e gerar um .zip com frames do vídeo processado.

## Startando SQS na maquina local para teste
Para executar o SQS local basta utilizar docker compose, para isso execute o comando:

```
docker compose up localstack
```

Após subir o container docker para SQS, basta configurar aws cli, conforme roteiro oficial citado logo abaixo, e depois executar os comandos para criar as filas:

```
aws sqs create-queue --queue-name=processar --profile localstack --endpoint-url=http://localhost:4566
aws sqs create-queue --queue-name=processados --profile localstack --endpoint-url=http://localhost:4566
```

Roteiro oficial utilizado como guia:
https://docs.quarkiverse.io/quarkus-amazon-services/dev/amazon-sqs.html#_provision_sqs_locally_manually

## Pré-requisitos para o SO

É necessário instalar FFMPeg e ZIP no sistema operacional linux, para que os comandos executados pela aplicação funcionem corretamente.

```
sudo apt update
sudo apt install ffmpeg
sudo apt install zip
```

## PUT SQS para processamento de arquivos

Copie o arquivo **resources/arquivos/input.mp4** (vídeo de exemplo) para o diretório especificado em **path.processar** (definido em aplicattion.properties).
Após esse procedimento, você poderá executar o comando abaixo para simular a ação da API responsável por realizar upload do vídeo e publicação na fila de vídeos a serem processados.

```
aws sqs send-message --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/processar --message-body "{\"id\": \"12345\", \"status\": \"AGUARDANDO\", \"pathVideo\": \"input.mp4\", \"pathZip\": \"\"}" --region us-east-1 --profile localstack --endpoint-url=http://localhost:4566

```

## Executando Testes
```
./mvnw test
```

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/fiap-videos-processamento-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
