# Гайд по миграциям (Liquibase)

Эта страница объясняет, как добавлять и применять миграции БД в проекте.

## Что используем

- Liquibase с master‑файлом `src/main/resources/db/changelog/db.changelog-master.xml`.
- Миграции храним в `src/main/resources/db/changelog/YYYY/MM/*.xml`.

## Как добавить новую миграцию

1. Создайте файл в папке по дате, например: `src/main/resources/db/changelog/2025/10/01-02-new-feature.xml`.
2. Подключите его в `db.changelog-master.xml` через `<include file="db/changelog/2025/10/01-02-new-feature.xml"/>`.
3. Внутри файла добавьте `databaseChangeLog` и нужные `changeSet`.
4. Если пишете precondition на SQL — используйте `SELECT COUNT(*)` вместо выборки одной строки. Это безопаснее при
   пустых результатах.

## Как запустить миграции локально

- При запуске приложения миграции применяются автоматически (см. `spring.liquibase.enabled`).
- В интеграционных тестах мы также включаем Liquibase, чтобы схема совпадала с продом.

## Рекомендации

- Избегайте ручного `ddl-auto` в проде. Управляйте схемой только через Liquibase.
- Пишите обратимые изменения там, где это возможно (`rollback`).
- Давайте понятные `id` и `author` в `changeSet`, например `id="create-orders-table" author="<имя>"`.
