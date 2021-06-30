up:
	docker-compose up -d

down:
	docker-compose down

migrate:
	docker-compose run flyway migrate

clean:
	docker-compose run flyway clean