# Master OAuth2 Authorization Server

Authorization Server با **Spring Boot 2.3.0**، کاربران و scope در دیتابیس، صفحه انتخاب scope، کپچا، لاگین pluggable (محلی/وب‌سرویس)، و مدیریت/ابطال توکن.

مستند کامل معماری و چرخه کلاس‌ها: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)

## مستندات API (Redoc)

مستندات OpenAPI با [Redoc](https://github.com/Redocly/redoc) رندر می‌شود:

- UI: [http://localhost:8080/docs](http://localhost:8080/docs)
- Spec: [http://localhost:8080/openapi.yaml](http://localhost:8080/openapi.yaml)

مسیرهای جایگزین: `/redoc` ، `/swagger` ، `/api-docs`

## اجرا

```bash
mvn spring-boot:run
```

آدرس: `http://localhost:8080`  
تنظیمات: `src/main/resources/application.properties`

## قابلیت‌های کلیدی

1. **Consent UI** بعد از لاگین برای انتخاب scope (Thymeleaf)
2. **Scopeهای DB-backed** (`scopes` + `user_scopes`)
3. **کپچا** در فرم لاگین (`/captcha`)
4. **لاگین قابل تعویض**: `auth.login.mode=local|remote`
5. **لیست و revoke توکن**: `/api/tokens/**`
6. **JWT** با رجیستری `oauth_token_registry`

## کاربران نمونه

| Username | Password | Scopes |
|----------|----------|--------|
| admin | password | همه |
| alice | password | read, write, profile, email |
| bob | password | read, profile |

کلاینت‌ها: `web-client` / `api-client` — secret: `password`

## API مدیریت توکن

```bash
# لیست توکن‌های من
curl -H "Authorization: Bearer TOKEN" http://localhost:8080/api/tokens/me

# ابطال با jti
curl -X POST -H "Authorization: Bearer TOKEN" http://localhost:8080/api/tokens/revoke/JTI

# ابطال همه
curl -X POST -H "Authorization: Bearer TOKEN" http://localhost:8080/api/tokens/revoke-all
```

## لاگین از وب‌سرویس بیرونی

```properties
auth.login.mode=remote
auth.login.remote.url=http://localhost:9090/api/auth/verify
```

## CRUD کاربران و Scope

نیاز به Bearer token با scope `write` (مثلاً کاربر `alice` یا `admin`).

```bash
# توکن
TOKEN=$(curl -s -u api-client:password \
  -d "grant_type=password&username=alice&password=password&scope=read write" \
  http://localhost:8089/oauth/token | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

# Users
curl -H "Authorization: Bearer $TOKEN" http://localhost:8089/api/users
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"username":"charlie","password":"password","email":"c@ex.com","scopes":["read","profile"]}' \
  http://localhost:8089/api/users

# Scopes
curl http://localhost:8089/api/scopes
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"reports","displayName":"Reports","description":"Reporting access"}' \
  http://localhost:8089/api/scopes
```

مستندات: http://localhost:8089/docs


```
http://localhost:8080/oauth/authorize?response_type=code&client_id=web-client&redirect_uri=http://localhost:8081/callback&scope=read%20write%20profile%20email&state=xyz
```



اجرا با Maven
cd /home/moradi/IdeaProjects/masterOauth/oauth
mvn spring-boot:run
یا از IntelliJ
کلاس OauthAuthorizationServerApplication را Run کنید.

بعد از بالا آمدن
آدرس	کاربرد
http://localhost:8080/login
لاگین
http://localhost:8080/docs
مستندات Redoc
http://localhost:8080/api/public/health
health
کاربر نمونه: alice / password (کپچا را از تصویر وارد کنید).

اگر پورت 8080 اشغال بود، قبلش:

fuser -k 8080/tcp



کاربران تستی
رمز همه: password

Username	Scopeهای مجاز
admin
همه (read, write, delete, profile, email)
alice
read, write, profile, email
bob
read, profile
کلاینت‌های OAuth
رمز secret همه: password

Client ID	کاربرد
web-client
authorization_code + consent
api-client
password grant / API
نمونه گرفتن توکن:

curl -u api-client:password \
  -d "grant_type=password&username=alice&password=password&scope=read profile" \
  http://localhost:8080/oauth/token

  http://localhost:8087/oauth/authorize?response_type=code&client_id=web-client&redirect_uri=http://localhost:8081/callback&scope=read%20write%20profile%20email&state=demo
# OAuth-Custom
