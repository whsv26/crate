up:
	docker-compose up -d

down:
	docker-compose down

migrate:
	docker-compose run flyway migrate
