# معماری و چرخه کلاس‌های Authorization Server

این سند بر اساس سورس فعلی پروژه نوشته شده و مسیر کامل از درخواست کاربر تا صدور/ابطال توکن را توضیح می‌دهد.

## ۱. نمای کلی سیستم

```
Browser / API Client
        │
        ▼
┌───────────────────┐     form login + captcha      ┌─────────────────────────┐
│  SecurityConfig   │ ───────────────────────────▶ │ PluggableAuthentication │
│  (Order=1)        │                              │ Provider                │
└─────────┬─────────┘                              └───────────┬─────────────┘
          │                                                    │
          │ authorize                                         ▼
          ▼                                          CredentialVerifier
┌───────────────────┐                              (local | remote)
│ Authorization     │
│ Server            │── scope filter ──▶ ScopeAwareOAuth2RequestFactory
│ Endpoints         │── consent UI ────▶ ScopeSelectingUserApprovalHandler
│                   │── JWT issue ─────▶ RevocableTokenServices
└─────────┬─────────┘                              │
          │                                        ▼
          │                              TokenRegistryService (DB)
          ▼
┌───────────────────┐
│ ResourceServer    │◀── Bearer JWT + revoke check
│ /api/**           │
└───────────────────┘
```

## ۲. تنظیمات اجرا

فایل اصلی تنظیمات: `src/main/resources/application.properties`

| کلید | نقش |
|------|-----|
| `auth.login.mode=local\|remote` | منبع احراز هویت فرم و password grant |
| `auth.login.remote.url` | آدرس وب‌سرویس بیرونی لاگین |
| `auth.captcha.enabled` | فعال/غیرفعال کپچا در فرم لاگین |
| `oauth.jwt.signing-key` | کلید امضای JWT |

برای MySQL بخش مربوطه را از کامنت خارج کنید و تنظیمات H2 را غیرفعال کنید.

## ۳. چرخه کلاس‌ها — گام‌به‌گام

### ۳.۱ بالا آمدن Application Context

1. `OauthAuthorizationServerApplication` برنامه را boot می‌کند.
2. `PasswordConfig` → `PasswordEncoder` (Delegating / bcrypt).
3. JPA entityها و Repositoryها ثبت می‌شوند؛ `schema.sql` و `data.sql` دیتابیس را آماده می‌کنند.
4. `SecurityConfig` + `AuthorizationServerConfig` + `ResourceServerConfig` لایه‌های امنیتی را می‌سازند.

### ۳.۲ لاگین مرورگر (با کپچا)

| مرحله | کلاس | کار |
|------|------|-----|
| 1 | `ConsentController.login` | صفحه `login.html` + فلگ `captchaEnabled` |
| 2 | `AuthSupportController.captchaImage` | تولید کد، ذخیره در session، رندر PNG |
| 3 | `CaptchaValidationFilter` | قبل از auth؛ اگر captcha اشتباه → `/login?error=captcha` |
| 4 | `UsernamePasswordAuthenticationFilter` | خواندن username/password |
| 5 | `PluggableAuthenticationProvider` | فراخوانی `CredentialVerifier.verify` |
| 6a | `LocalCredentialVerifier` | تطبیق رمز با جدول `users` |
| 6b | `RemoteCredentialVerifier` | POST JSON به وب‌سرویس بیرونی |
| 7 | `CustomUserDetailsService` | بارگذاری کاربر + scopeها به‌صورت Authority |
| 8 | Session ایجاد می‌شود و کاربر به `/oauth/authorize` برمی‌گردد |

**نکته مهم:** حتی در حالت `remote`، بعد از موفقیت وب‌سرویس بیرونی، پروفایل و scopeها از جدول محلی `users` / `user_scopes` خوانده می‌شود. پس کاربر باید در DB محلی وجود داشته باشد (یا بعداً sync کنید).

### ۳.۳ Authorization Code + انتخاب Scope

| مرحله | کلاس | کار |
|------|------|-----|
| 1 | `/oauth/authorize` | شروع flow؛ درخواست در session ذخیره می‌شود |
| 2 | `ScopeAwareOAuth2RequestFactory` | فیلتر اولیه scopeها |
| 3 | `ScopeSelectingUserApprovalHandler.checkForPreApproval` | همیشه `approved=false` تا صفحه consent بیاید |
| 4 | `ConsentController.consent` | scopeهای قابل انتخاب = تقاطع client ∩ user ∩ DB |
| 5 | کاربر چک‌باکس‌ها را می‌زند و POST می‌کند |
| 6 | `ScopeSelectingUserApprovalHandler.updateAfterApproval` | فقط `scope.X=true`ها را نگه می‌دارد |
| 7 | Authorization code صادر می‌شود |
| 8 | کلاینت با `/oauth/token` کد را عوض می‌کند |

### ۳.۴ صدور JWT و ثبت در رجیستری

| مرحله | کلاس | کار |
|------|------|-----|
| 1 | `RevocableTokenServices.createAccessToken` | ساخت توکن |
| 2 | `CustomTokenEnhancer` | افزودن `user_id`, `full_name`, `email` |
| 3 | `JwtAccessTokenConverter` | امضا + افزودن `jti` و `scope` |
| 4 | `TokenRegistryService.register` | ذخیره رکورد در `oauth_token_registry` |

### ۳.۵ استفاده از API با Bearer Token

| مرحله | کلاس | کار |
|------|------|-----|
| 1 | `ResourceServerConfig` | مسیرهای `/api/**` را با OAuth محافظت می‌کند |
| 2 | `RevocableTokenServices.loadAuthentication` | JWT را می‌خواند |
| 3 | اگر `jti` در رجیستری `revoked=true` باشد → `InvalidTokenException` |
| 4 | کنترلرهایی مثل `ApiController` / `TokenManagementController` اجرا می‌شوند |

### ۳.۶ لیست و ابطال توکن

| API | کلاس | کار |
|-----|------|-----|
| `GET /api/tokens/me` | `TokenManagementController` | لیست توکن‌های کاربر جاری |
| `POST /api/tokens/revoke/{jti}` | همان | ابطال بر اساس jti |
| `POST /api/tokens/revoke` | همان + `ConsumerTokenServices` | ابطال با مقدار خام توکن |
| `POST /api/tokens/revoke-all` | `TokenRegistryService.revokeAllForUser` | ابطال همه توکن‌های کاربر |

## ۴. نقشه پکیج‌ها

```
com.master.oauth
├── OauthAuthorizationServerApplication
├── approval/          # منطق انتخاب و فیلتر scope
│   ├── ScopeAwareOAuth2RequestFactory
│   └── ScopeSelectingUserApprovalHandler
├── auth/              # لاگین pluggable
│   ├── CredentialVerifier
│   ├── LocalCredentialVerifier
│   ├── RemoteCredentialVerifier
│   └── PluggableAuthenticationProvider
├── captcha/           # کپچا تصویری + فیلتر فرم
│   ├── CaptchaService
│   ├── CaptchaValidationFilter
│   └── CaptchaValidationException
├── config/            # Security / OAuth / JWT / Password
├── controller/        # UI + REST
├── entity/            # User, Scope, TokenRegistry
├── repository/
└── service/           # UserDetails, Scope, TokenRegistry
```

## ۵. جداول دیتابیس

| جدول | نقش |
|------|-----|
| `users` | کاربران اختصاصی |
| `scopes` | تعریف scopeها |
| `user_scopes` | scopeهای مجاز هر کاربر |
| `oauth_client_details` | کلاینت‌های OAuth |
| `oauth_approvals` | approvalهای ذخیره‌شده |
| `oauth_token_registry` | رجیستری JWT برای list/revoke |
| `oauth_access_token` / `oauth_refresh_token` | جداول استاندارد Spring (آماده JDBC store) |

## ۶. تعویض لاگین به وب‌سرویس بیرونی

در `application.properties`:

```properties
auth.login.mode=remote
auth.login.remote.url=http://YOUR-AUTH-SERVICE/api/auth/verify
```

قرارداد وب‌سرویس بیرونی:

```http
POST /api/auth/verify
Content-Type: application/json

{"username":"alice","password":"secret"}
```

پاسخ موفق نمونه:

```json
{"authenticated": true}
```

یا `{"success": true}`.

خود این پروژه هم endpoint مشابه `POST /api/auth/verify` دارد تا بتوانید زنجیره سرویس‌ها را تست کنید.

## ۷. آموزش عملی سریع

### ۷.۱ گرفتن توکن (password grant)

```bash
curl -u api-client:password \
  -d "grant_type=password&username=alice&password=password&scope=read profile" \
  http://localhost:8080/oauth/token
```

### ۷.۲ دیدن توکن‌های کاربر

```bash
curl -H "Authorization: Bearer ACCESS_TOKEN" \
  http://localhost:8080/api/tokens/me
```

### ۷.۳ ابطال یک توکن

```bash
curl -X POST -H "Authorization: Bearer ACCESS_TOKEN" \
  http://localhost:8080/api/tokens/revoke/JTI_VALUE
```

بعد از revoke، همان access token دیگر برای `/api/me` معتبر نیست.

### ۷.۴ جریان مرورگر

1. باز کردن `/oauth/authorize?...`
2. لاگین + کپچا
3. انتخاب scope در `/oauth/consent`
4. دریافت `code` و تبدیل به token

## ۹. مستندات API با Redoc

- Spec: `src/main/resources/static/openapi.yaml`
- UI: `src/main/resources/static/docs/index.html` (رندر با [Redoc](https://github.com/Redocly/redoc))
- آدرس: `http://localhost:8080/docs`
- کنترلر هدایت: `DocsController` (`/docs`, `/redoc`, `/swagger`, `/api-docs`)


برای صفحات login/consent از الگوی **Trust & Authority + Soft Glass** استفاده شده:

- تایپوگرافی: Syne (display) + IBM Plex Sans (body)
- پالت: teal حرفه‌ای `#0D7377` روی پس‌زمینه روشن با mesh/orb
- بدون گرادیان بنفش AI، بدون emoji به‌جای آیکون
- motion کوتاه با احترام به `prefers-reduced-motion`
- focus ring و hover قابل‌مشاهده برای دسترس‌پذیری

استایل‌ها در `static/css/app.css` و رفتار کپچا/UI در `static/js/app.js` است.
