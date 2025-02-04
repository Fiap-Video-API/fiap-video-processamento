#!/bin/sh

echo "Criando filas SQS no LocalStack..."

aws sqs create-queue --queue-name=processar --profile localstack --endpoint-url=http://localhost:4566
aws sqs create-queue --queue-name=processados --profile localstack --endpoint-url=http://localhost:4566

echo "Filas criadas com sucesso!"
