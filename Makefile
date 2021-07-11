rebuild: down docker-build up

docker-build:
	sbt docker

docker-build-and-push:
	sbt dockerBuildAndPush

up:
	docker-compose up -d

down:
	docker-compose down

migrate:
	docker-compose run flyway migrate

clean:
	docker-compose run flyway clean