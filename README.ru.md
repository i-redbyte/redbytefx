[English](README.md) · **Русский**

# RedByteFX

Пишите **шейдеры под Android** на **Kotlin**, компилируйте в **AGSL** и подключайте их к **Jetpack Compose** — с типизированными uniform-ами, понятными локальными именами и `effect.agslSource()`, когда нужно увидеть, что именно уходит на GPU.

---

## Почему это приятно использовать

- **Код шейдера остаётся похожим на математику** — `sin`, `mix`, `float2`, `sample()`, `let(...)`, `fn(...)` без ручной склейки строк.
- **Предсказуемый AGSL** — DSL остаётся близким к генерируемому AGSL, проще отлаживать.
- **Дружба с Compose** — `rememberFxController`, `bindFloat` / `bindTime`, `Modifier.redbyteFx(...)` для живого UI.
- **Глубина на выбор** — только **core** для полного контроля или **stdlib** (маски, градиенты, шум, SDF и др.), когда нужна скорость.

---

## Требования

- **Android API 33+** (используемый путь AGSL / `RuntimeShader` рассчитан на современные API).
- **Kotlin** и актуальный **Android Gradle Plugin** (версии см. в `gradle/libs.versions.toml` репозитория).
- **Jetpack Compose**, если подключаете `:redbytefx-compose` (Compose BOM передаётся через этот артефакт).

---

## Подключение RedByteFX **1.0.0**

### 1. Maven-репозиторий GitHub Packages

[Создайте токен](https://github.com/settings/tokens) с правом **`read:packages`**. Не коммитьте его — храните в `~/.gradle/gradle.properties` или в секретах CI.

В **`settings.gradle.kts`** (внутри `dependencyResolutionManagement { repositories { ... } }`):

```kotlin
maven {
    url = uri("https://maven.pkg.github.com/i-redbyte/redbytefx")
    credentials {
        username = providers.gradleProperty("gpr.user").orNull
            ?: System.getenv("GITHUB_ACTOR")
            ?: ""
        password = providers.gradleProperty("gpr.token").orNull
            ?: System.getenv("GITHUB_TOKEN")
            ?: ""
    }
}
```

В **`gradle.properties`** (только у вас локально):

```properties
gpr.user=ВАШ_ЛОГИН_GITHUB
gpr.token=ВАШ_ТОКЕН
```

*(В GitHub Actions для этого репозитория `GITHUB_TOKEN` подставляется автоматически.)*

### 2. Зависимости

```kotlin
dependencies {
    implementation("com.github.i-redbyte:redbytefx-core:1.0.0")
    implementation("com.github.i-redbyte:redbytefx-compose:1.0.0")
    implementation("com.github.i-redbyte:redbytefx-stdlib:1.0.0")
}
```

| Артефакт | Что даёт |
|----------|----------|
| `redbytefx-core` | DSL, компилятор, uniform-ы, `redbytefx { }`, вывод AGSL |
| `redbytefx-compose` | `FxController`, `Modifier.redbyteFx`, привязки |
| `redbytefx-stdlib` | Готовые шейдерные «рецепты» (по желанию, но очень удобно) |

Можно подключить только **`redbytefx-compose`** и **`redbytefx-stdlib`** — **`redbytefx-core`** придёт транзитивно. Три строки выше — явный и удобный для копирования вариант.

---

## Мини-пример

Добавьте стандартные импорты Compose и `ru.redbyte.redbytefx.*` (включая `grayscale`, `sample`, `mix`).

```kotlin
val grayscaleDemo = run {
    var amount: FxParam.Float? = null
    val effect = redbytefx {
        val amountUniform by autoUniformFloat(0f)
        amount = amountUniform
        val base = let(sample(), "base")
        mix(base, grayscale(base), amountUniform)
    }
    effect to amount!!
}

@Composable
fun HelloFx(amount: Float) {
    val fx = rememberFxController(grayscaleDemo.first)
    fx.bindFloat(grayscaleDemo.second, amount)
    Text(
        text = "RedByteFX",
        modifier = Modifier.redbyteFx(fx)
    )
}
```

Держите **один контроллер на цель рендеринга**, привязывайте только uniform-ы **этого** скомпилированного эффекта; при странностях смотрите `agslSource()`.

---

## Модули в этом репозитории

| Gradle-модуль | Назначение |
|---------------|------------|
| `:redbytefx-core` | Язык, компилятор, мост к рантайму |
| `:redbytefx-compose` | Интеграция с Compose |
| `:redbytefx-stdlib` | Высокоуровневые помощники |
| `:sample` | Демо-приложение (исходники только в репозитории, не как библиотека) |

---

## Сборка и проверка у себя

```bash
./gradlew qualityCheck
```

Опубликовать три библиотеки в **локальный Maven** (без токена GitHub):

```bash
./gradlew :redbytefx-core:publishAllPublicationsToMavenLocalRepository \
  :redbytefx-compose:publishAllPublicationsToMavenLocalRepository \
  :redbytefx-stdlib:publishAllPublicationsToMavenLocalRepository
```

---

## Участие в проекте — вам рады

Исправление опечатки, уточнение текста ошибки, новый тест или небольшой хелпер в stdlib — **спасибо**, что вы здесь.

- **Сначала обсудим** крупные изменения — откройте issue, чтобы согласовать направление до огромного PR.
- **Держите PR узкими** — так их быстрее ревьюят.
- Перед пушем запустите **`./gradlew qualityCheck`**.
- Изменения **публичного API** сопровождайте обновлением **KotlinDoc** в том же коммите.

Нам важны ясность, уважение к ревьюерам и код, который понятен следующему читателю. Если это про вас — будем рады совместной работе.

---

## Лицензия

Распространяется под **MIT** — текст в [LICENSE](LICENSE).
