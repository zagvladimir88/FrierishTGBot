# Fraerish bot

Telegram-бот: погода, курс НБРБ, поиск картинок, исправление раскладки и ежедневная рассылка курса и погоды.

## Команды

| Команда | Что делает |
|---|---|
| `/start` | справка и подписка на ежедневную рассылку |
| `/stop` | отписка от рассылки |
| `/w Минск` | текущая погода (OpenWeather) |
| `/exchange USD` | официальный курс НБРБ |
| `/g котики` | картинки через SearXNG |
| `/c ghbdtn` | перевод текста, набранного в латинской раскладке (работает и при редактировании сообщения) |

Рассылка: курс в 11:00, погода в 11:30 по Минску (`bot.schedule.*` в `application.yml`).

## Устройство

Одно Spring Boot-приложение. Каждая команда — реализация `CommandHandler`, `UpdateDispatcher` выбирает её по имени команды; новая команда добавляется одним классом-бином.

```
telegram/      long polling, отправка ответов, разбор апдейтов
command/       контракт команд и разбор текста команды
weather/       /w и клиент OpenWeather
exchange/      /exchange и клиент НБРБ
image/         /g, интерфейс ImageSearchClient и реализация для SearXNG
layout/        /c
subscription/  /start, /stop, таблица чатов (H2) и ежедневная рассылка
```

Данные — файл H2 в `${BOT_DATA_DIR}` (в Docker — volume `bot-data`).

## Сборка и тесты

Нужен JDK 21 и Maven 3.9:

```
mvn verify
```

## Запуск

```
cp .env.example .env
docker compose up -d --build
docker compose logs -f bot
```

Переменные в `.env`:

| Переменная | Значение |
|---|---|
| `BOT_TOKEN` | токен от @BotFather |
| `BOT_NAME` | username бота без `@` |
| `OPEN_API_TOKEN` | ключ OpenWeather |
| `SEARXNG_SECRET` | случайная строка, например `openssl rand -hex 32` |

SearXNG наружу не публикуется: бот ходит к нему по внутренней сети compose. Входящие порты не нужны — бот работает через long polling.