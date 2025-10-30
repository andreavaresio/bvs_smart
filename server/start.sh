cd ..
docker run --rm -v $(pwd)/server:/app -w /app -p 9988:9988 php:8.2-cli php -S 0.0.0.0:9988

