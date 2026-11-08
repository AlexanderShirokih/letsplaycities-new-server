# Протокол «Сыграем в Города»

Справочник по REST API v1 и игровому протоколу WebSocket. Источник истины — код (`internal/api`, `internal/game`). Дизайн и обоснования — в `docs/superpowers/specs/2026-11-08-go-server-rewrite-design.md`. Эталонные сообщения игрового протокола лежат в [`testdata/protocol/`](../testdata/protocol/).

## REST API v1

### Общие правила

- Пути начинаются с `/v1`. Тела запросов и ответов — JSON с полями в camelCase, время — epoch ms. Исключения: тело `PUT /v1/me/avatar`, `GET /v1/avatars/{id}` и файл словаря.
- Ошибка: HTTP-статус и тело `{"error":{"code":"…","message":"…"}}`. Клиент различает ошибки по `code`, `message` показывает пользователю.
- Тело JSON-запроса — до 64 КБ. Нечитаемый JSON или обязательное поле не задано — 400 `bad_request`.
- Авторизация: `Authorization: Bearer <token>`. Токен выдают `auth/native` и `auth/social`, отзывает `auth/logout`. Без токена или с неизвестным токеном — 401 `unauthorized`. Забаненный пользователь получает 403 `banned` на любой авторизованный запрос и на вход.
- Неизвестный путь под `/v1/` — 404 `not_found`. Известный путь с чужим методом отвечает 405 стандартным текстом `net/http`, не JSON.
- Каждый авторизованный запрос обновляет `lastSeenAt`.

Объект `user`: `{id, name, avatarHash, provider, lastSeenAt}`. `avatarHash` равен `null`, если аватарки нет.

### Вход

| Запрос | Ответ |
|---|---|
| `POST /v1/auth/native {deviceId, name}` | 200 `{token, user}` |
| `POST /v1/auth/social {provider, token, name}` | 200 `{token, user}` |
| `POST /v1/auth/logout` | 204 |

`name` нужен только при создании аккаунта, у существующего игнорируется. Правила имени (вход и `PATCH /v1/me`): 2–64 символа после обрезки пробелов.

`provider`: `google`, `vk`, `ok`, `fb`. Провайдер без ключей выключен.

| Статус | code | Когда |
|---|---|---|
| 400 | `bad_request` | пустой `deviceId`, `provider` или `token`, битый JSON |
| 400 | `bad_name` | имя короче 2 или длиннее 64 символов |
| 400 | `forbidden_name` | слово имени есть в `forbidden_logins` |
| 400 | `provider_disabled` | у провайдера нет ключей, либо провайдер неизвестен |
| 401 | `invalid_provider_token` | провайдер не подтвердил токен |
| 403 | `banned` | аккаунт заблокирован |
| 429 | `too_many_requests` | `auth/native`: больше 10 запросов в минуту с одного IP |
| 502 | `provider_unavailable` | провайдер не ответил |

### Профиль и аватарки

| Запрос | Ответ |
|---|---|
| `GET /v1/me` | 200 `user` |
| `PATCH /v1/me {name?, fcmToken?}` | 200 `user`. Ошибки имени — `bad_name`, `forbidden_name` |
| `PUT /v1/me/avatar` | тело — сырые байты, `Content-Type` `image/png` или `image/jpeg`, до 256 КБ, каждая сторона не больше 512 px. Клиент присылает 256×256. 200 `user` |
| `DELETE /v1/me/avatar` | 204 |
| `GET /v1/users/{id}` | 200 `user` + `friendship` (`none`, `friends`, `incoming`, `outgoing`) + `blocked` (`none`, `byMe`, `me`, `both`) |
| `GET /v1/avatars/{id}?v={hash}` | без авторизации. Байты картинки с `ETag: "<hash>"`. Если `v` равен текущему хешу — `Cache-Control: public, max-age=31536000, immutable`, иначе `no-cache`. `If-None-Match` даёт 304 |

Ошибки: 400 `bad_image` (не PNG и не JPEG, формат не совпадает с `Content-Type` или сторона больше 512 px), 413 `too_large` (больше 256 КБ), 404 `not_found` (нет пользователя или аватарки; нечисловой `{id}` — тоже 404).

### Друзья, блокировки, история

| Запрос | Ответ |
|---|---|
| `GET /v1/friends` | 200 `[{user, status}]`, `status`: `friends`, `incoming`, `outgoing` |
| `PUT /v1/friends/{id}` | 204. Отправляет заявку или принимает встречную |
| `DELETE /v1/friends/{id}` | 204. Отклоняет, отменяет заявку или удаляет друга; идемпотентен |
| `GET /v1/blocks` | 200 `[user]` |
| `PUT /v1/blocks/{id}` | 204. Удаляет дружбу между пользователями |
| `DELETE /v1/blocks/{id}` | 204; идемпотентен |
| `GET /v1/history?with={id}&before={cursor}` | 200 `{games, next}`; `games` — `[{id, opponent: user, isFriend, startedAt, durationSec, words}]`, 50 игр от новых к старым; `next` — курсор или `null`. Нечисловой или отрицательный `with`/`before` — 400 `bad_request` |

Ошибки `PUT /v1/friends/{id}` и `PUT /v1/blocks/{id}`: 400 `self` (на себя), 404 `not_found` (нет пользователя), у друзей ещё 403 `blocked` (между пользователями есть блокировка в любую сторону). `DELETE` не проверяет существование пользователя.

### Заявки на правку городов

| Запрос | Ответ |
|---|---|
| `GET /v1/city-edits` | 200 `[{id, oldCountryCode, newCountryCode, oldName, newName, reason, verdict, status}]`, последние 50 своих |
| `POST /v1/city-edits {oldCountryCode, newCountryCode, oldName, newName, reason}` | 201 созданная заявка со `status: "NEW"` |

Ошибки `POST`: 400 `bad_reason` (`reason` длиннее 64 символов), 429 `too_many_requests` (50 и больше заявок `NEW`).

### Словарь и статика

| Запрос | Ответ |
|---|---|
| `GET /v1/dictionary` | 200 `{version, size, sha256}`; 404 `not_found`, если словарь не загружен |
| `GET /v1/dictionary/{version}.db2` | файл с `Cache-Control: public, max-age=31536000, immutable`; другая версия — 404 `not_found` |
| `GET /privacy-mobile.html`, `GET /legal-mobile.txt` | встроенные страницы |

### Общие коды ошибок

`bad_request`, `bad_name`, `forbidden_name`, `provider_disabled`, `invalid_provider_token`, `provider_unavailable`, `unauthorized`, `banned`, `blocked`, `self`, `bad_image`, `too_large`, `bad_reason`, `too_many_requests`, `not_found`, `internal` (500).

## Игровой протокол (WebSocket)

### Подключение

- `GET /v1/game` с заголовком `Authorization: Bearer <token>` на апгрейде. Ошибки авторизации — обычный HTTP-ответ REST (`unauthorized`, `banned`) до апгрейда.
- Каждое сообщение — текстовый кадр с JSON-объектом `{"type": "…", …}`. Бинарный кадр закрывает соединение (код закрытия 1003, причина `unsupported_frame`). Лимит кадра — 64 КБ.
- Сервер шлёт WS ping каждые `LPS_PING_INTERVAL_SEC`; без ответа за `LPS_PING_TIMEOUT_SEC` соединение закрывается.
- Одно соединение на пользователя. Новое вытесняет старое. Если у пользователя идёт игра, она заканчивается как при разрыве: старое соединение и соперник получают `gameOver{opponentLeft}`, вытесненный игрок проигрывает, игра пишется в историю. Затем старому соединению уходит `error{code: "replaced"}` и закрытие 1001. Новое соединение начинает со свободного состояния.
- Первое сообщение — `hello`. До него любое другое сообщение даёт `error{code: "hello_required"}`.
- Неразборчивый JSON, неизвестный `type` — `error{code: "bad_message"}`, соединение остаётся открытым.
- Разрыв соединения во время игры: сопернику приходит `gameOver{opponentLeft}`.
- Переподключения к идущей игре нет: разрыв и вытеснение завершают игру.

Закрытие по ошибке сервера использует код 1008 с причиной `overloaded` (клиент не успевает читать, очередь отправки — 64 сообщения) или `unsupported_protocol`.

### Сообщения клиента

| type | Поля | Смысл |
|---|---|---|
| `hello` | `protocol` (сейчас 1), `client` | первое сообщение. `client` и `name` сервер игнорирует |
| `find` | — | в очередь случайного подбора (из состояния «свободен» или «ждёт ответа на своё приглашение»; приглашение снимается) |
| `invite` | `userId` | позвать друга |
| `inviteReply` | `userId`, `accept` | ответ на приглашение |
| `cancel` | — | выйти из очереди или отозвать приглашение |
| `word` | `text` | ход |
| `chat` | `text` (до 200 символов) | сообщение сопернику |
| `leave` | — | сдаться |

### Сообщения сервера

| type | Поля |
|---|---|
| `welcome` | `user` |
| `queued` | — |
| `inviteStatus` | `userId`, `status`: `sent`, `declined`, `busy`, `offline`, `notFriends`, `blocked`, `expired` |
| `match` | `gameId`, `opponent: {id, name, avatarHash, friendship}`, `youStart`, `moveTimeSec` |
| `word` | `by`, `text`, `status`: `accepted`, `alreadyUsed`, `unknown`, `wrongLetter`, `notYourTurn` |
| `chat` | `by`, `text` |
| `gameOver` | `reason` (`timeout`, `opponentLeft`, `surrender`), `loser`, `words`, `durationSec` |
| `error` | `code` |

`opponent.friendship`: `none`, `friends`, `incoming`, `outgoing`. В `welcome.user` поля `provider` и `lastSeenAt` присутствуют, когда они не пустые.

Отклонённое слово получает только автор. Принятое получают оба игрока, ход переходит к сопернику, таймер перезапускается.

### Коды `error`

| code | Когда |
|---|---|
| `bad_message` | неразборчивый кадр, неизвестный `type`, `chat` длиннее 200 символов |
| `hello_required` | любое сообщение до `hello` |
| `invalid_state` | сообщение не подходит состоянию: повторный `hello`, `find` или `invite` в игре или очереди, `word`, `chat` и `leave` вне игры, `cancel` без очереди и приглашения, `inviteReply{accept: true}` во время игры; также сбой хранилища при обработке |
| `unsupported_protocol` | `hello` с `protocol`, отличным от 1; следом закрытие 1008 |
| `replaced` | соединение вытеснено новым |

### Правила

- **Подбор.** FIFO. Пара не создаётся при блокировке в любую сторону, такой игрок ждёт следующего. Начинает тот, кто встал в очередь раньше. С `LPS_BOTS_ENABLED` при пустой очереди через `LPS_BOTS_WAIT_SEC` игроку подбирается бот.
- **Приглашение.** Проверки по порядку: `blocked`, `notFriends`, `busy`; нет FCM-токена у приглашённого или пуш не ушёл — `offline`; иначе `sent` и FCM-пуш `{type: "invite", fromId, fromName}`. Приглашение живёт `LPS_INVITE_TTL_SEC`, затем приглашающий получает `expired`. Приглашение снимается по `cancel`, отключению приглашающего и началу им другой игры. Ответ на снятое приглашение — `inviteStatus{userId: <приглашающий>, status: expired}`. `accept: true` создаёт игру, первым ходит приглашающий; `accept: false` — приглашающий получает `declined`.
- **Ход.** Слово нормализуется как в клиенте (`formatCity`): обрезка пробелов, нижний регистр, `ё` → `е`, повторы `-` схлопываются. Порядок проверок: `notYourTurn`, `wrongLetter` (первая буква против последней значимой буквы предыдущего слова; значимая — последняя, кроме `ь ъ ы ё`), `alreadyUsed`, `unknown` (нет в словаре).
- **Таймер.** `LPS_MOVE_TIME_SEC` на ход, считает сервер. По истечении проигрывает тот, чей ход: `gameOver{timeout}`.
- **История.** При `gameOver` игра пишется в `games`, если принято хотя бы одно слово; запись происходит до отправки `gameOver`.
- **Блокировка во время игры.** Игра идёт, `chat` между этими игроками не доставляется.

### Примеры

Клиент: [hello](../testdata/protocol/client-hello.json), [find](../testdata/protocol/client-find.json), [invite](../testdata/protocol/client-invite.json), [inviteReply](../testdata/protocol/client-inviteReply.json), [cancel](../testdata/protocol/client-cancel.json), [word](../testdata/protocol/client-word.json), [chat](../testdata/protocol/client-chat.json), [leave](../testdata/protocol/client-leave.json).

Сервер: [welcome](../testdata/protocol/server-welcome.json), [queued](../testdata/protocol/server-queued.json), [inviteStatus](../testdata/protocol/server-inviteStatus.json), [match](../testdata/protocol/server-match.json), [word](../testdata/protocol/server-word.json), [chat](../testdata/protocol/server-chat.json), [gameOver](../testdata/protocol/server-gameOver.json), [error](../testdata/protocol/server-error.json).
