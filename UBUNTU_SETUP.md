# Hướng dẫn Deploy TC/GRS lên Ubuntu 24

Repo: https://github.com/Nhan-Moon-04/TC
Domain: https://nthiennhan.duckdns.org
Chạy với quyền **root** (`root@linux4661:~#`)

---

## 1. Cài đặt công cụ cần thiết

```bash
apt update && apt upgrade -y
apt install -y git curl nginx certbot python3-certbot-nginx
```

---

## 2. Cài Node.js 20 (LTS)

```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt install -y nodejs
node -v
npm -v
```

---

## 3. Cài PM2

```bash
npm install -g pm2
```

---

## 4. Cài PostgreSQL

```bash
apt install -y postgresql postgresql-contrib
systemctl enable postgresql
systemctl start postgresql
```

Tạo database và user:

```bash
sudo -u postgres psql
```

Trong psql chạy:

```sql
CREATE USER tcgrs WITH PASSWORD 'nguyennhan2004';
CREATE DATABASE tcgrs_db OWNER tcgrs;
GRANT ALL PRIVILEGES ON DATABASE tcgrs_db TO tcgrs;
\q
```

---

## 5. Clone source code

```bash
cd /root
git clone https://github.com/Nhan-Moon-04/TC.git
cd TC
```

---

## 6. Cài đặt Backend

```bash
cd /root/TC/backend
npm install
```

Tạo file `.env`:

```bash
cp .env.example .env
nano .env
```

Nội dung `.env`:

```
DATABASE_URL="postgresql://tcgrs:nguyennhan2004@localhost:5432/tcgrs_db"
JWT_SECRET="doi_thanh_chuoi_bi_mat_cua_ban"
JWT_EXPIRES_IN="7d"
PORT=3001
UPLOAD_DIR="./uploads"
FRONTEND_URL="https://nthiennhan.duckdns.org"
```

Build và migrate DB:

```bash
npm run build
npx prisma migrate deploy
npx prisma generate
```

Seed tài khoản admin mặc định (chỉ chạy lần đầu):

```bash
npx tsx src/seed.ts
```

---

## 7. Cài đặt Frontend

```bash
cd /root/TC/frontend
npm install
npm run build
```

---

## 8. Cấu hình Nginx (HTTP tạm, để Certbot lấy SSL)

```bash
nano /etc/nginx/sites-available/tcgrs
```

Dán vào:

```nginx
server {
    listen 80;
    server_name nthiennhan.duckdns.org;

    root /root/TC/frontend/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://localhost:3001;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /uploads/ {
        alias /root/TC/backend/uploads/;
    }
}
```

Kích hoạt:

```bash
ln -s /etc/nginx/sites-available/tcgrs /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx
systemctl enable nginx
```

---

## 9. Cấp SSL (HTTPS port 443) bằng Certbot

```bash
certbot --nginx -d nthiennhan.duckdns.org
```

Certbot sẽ tự động:
- Lấy chứng chỉ SSL miễn phí
- Sửa nginx config thành port 443
- Tự động gia hạn mỗi 90 ngày

Kiểm tra:

```bash
nginx -t && systemctl reload nginx
```

Truy cập thử: https://nthiennhan.duckdns.org

---

## 10. Chạy Backend bằng PM2

```bash
cd /root/TC/backend
pm2 start dist/index.js --name "tcgrs-backend"
```

Lưu và bật tự khởi động khi reboot:

```bash
pm2 save
pm2 startup
```

Chạy lệnh mà PM2 in ra, ví dụ:

```bash
env PATH=$PATH:/usr/bin pm2 startup systemd -u root --hp /root
```

Kiểm tra trạng thái:

```bash
pm2 status
pm2 logs tcgrs-backend
```

---

## 11. Script cập nhật (Git Pull + Rebuild)

```bash
nano /root/update-tc.sh
```

Dán vào:

```bash
#!/bin/bash
set -e

APP_DIR="/root/TC"

echo "=== [1/5] Pull code moi nhat ==="
cd "$APP_DIR"
git pull origin main

echo "=== [2/5] Cap nhat Backend ==="
cd "$APP_DIR/backend"
npm install
npm run build
npx prisma migrate deploy
npx prisma generate

echo "=== [3/5] Cap nhat Frontend ==="
cd "$APP_DIR/frontend"
npm install
npm run build

echo "=== [4/5] Restart Backend ==="
pm2 restart tcgrs-backend

echo "=== [5/5] Reload Nginx ==="
systemctl reload nginx

echo ""
echo "=== HOAN TAT! App da duoc cap nhat ==="
pm2 status
```

Cấp quyền thực thi:

```bash
chmod +x /root/update-tc.sh
```

Mỗi lần muốn cập nhật code mới:

```bash
/root/update-tc.sh
```

---

## Tóm tắt lệnh PM2 thường dùng

| Lệnh | Mô tả |
|------|-------|
| `pm2 status` | Xem trạng thái các app |
| `pm2 logs tcgrs-backend` | Xem log backend |
| `pm2 restart tcgrs-backend` | Khởi động lại backend |
| `pm2 stop tcgrs-backend` | Dừng backend |
| `pm2 save` | Lưu danh sách app (chạy sau mỗi thay đổi) |

---

## Thông tin đăng nhập mặc định

| | |
|---|---|
| Tên đăng nhập | `admin` |
| Mật khẩu | `admin123` |

> Đổi mật khẩu ngay sau khi đăng nhập lần đầu!




ufw allow 80
ufw allow 443
ufw enable
ufw status


Tiến bộ rồi! 500 lỗi thường do nginx không đọc được file trong /root/ vì nginx chạy với user www-data, không có quyền vào thư mục /root.

Sửa nhanh bằng cách cấp quyền:


chmod 755 /root
chmod -R 755 /root/TC/frontend/dist
Rồi reload nginx:


systemctl reload nginx
Thử lại trình duyệt. Nếu vẫn lỗi, xem log để biết chính xác:


tail -20 /var/log/nginx/error.log